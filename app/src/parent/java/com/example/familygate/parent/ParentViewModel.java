package com.example.familygate.parent;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.familygate.data.AppRule;
import com.example.familygate.data.LocalRuleStore;
import com.example.familygate.data.PocketBaseClient;
import com.example.familygate.logic.AppDiscoveryRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ViewModel for the parent APK.
 *
 * Responsibilities:
 *  - Load installed apps from the device.
 *  - Fetch existing rules from PocketBase so current block states are shown.
 *  - Allow the parent to toggle per-app blocking.
 *  - Push the full rule set back to PocketBase.
 */
public class ParentViewModel extends AndroidViewModel {

    // ---------------------------------------------------------------------------
    // Data model
    // ---------------------------------------------------------------------------

    /** Mutable value object — `blocked` is updated directly by the adapter. */
    public static class ParentAppInfo {
        public final String packageName;
        public final String appName;
        public boolean blocked;

        public ParentAppInfo(String packageName, String appName, boolean blocked) {
            this.packageName = packageName;
            this.appName = appName;
            this.blocked = blocked;
        }
    }

    // ---------------------------------------------------------------------------
    // LiveData
    // ---------------------------------------------------------------------------

    private final MutableLiveData<List<ParentAppInfo>> apps = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    // ---------------------------------------------------------------------------
    // Dependencies
    // ---------------------------------------------------------------------------

    private final LocalRuleStore localRuleStore;
    private final PocketBaseClient pocketBaseClient;
    private final AppDiscoveryRepository appDiscoveryRepository;

    public ParentViewModel(@NonNull Application application) {
        super(application);
        this.localRuleStore = new LocalRuleStore(application);
        this.pocketBaseClient = new PocketBaseClient();
        this.appDiscoveryRepository = new AppDiscoveryRepository();
    }

    // ---------------------------------------------------------------------------
    // Exposed LiveData
    // ---------------------------------------------------------------------------

    public LiveData<List<ParentAppInfo>> getApps() { return apps; }
    public LiveData<String> getStatusMessage() { return statusMessage; }
    public LiveData<Boolean> getLoading() { return loading; }

    // ---------------------------------------------------------------------------
    // Credential helpers
    // ---------------------------------------------------------------------------

    public String getSavedUrl()      { return localRuleStore.getPocketBaseUrl(); }
    public String getSavedEmail()    { return localRuleStore.getPocketBaseEmail(); }
    public String getSavedPassword() { return localRuleStore.getPocketBasePassword(); }
    public String getSavedChildDeviceId() { return localRuleStore.getChildDeviceId(); }

    public void saveConfig(String url, String email, String password, String childDeviceId) {
        localRuleStore.savePocketBaseConfig(url, email, password);
        localRuleStore.saveChildDeviceId(childDeviceId);
        statusMessage.setValue("Config saved");
    }

    // ---------------------------------------------------------------------------
    // Load apps installed on THIS device
    // ---------------------------------------------------------------------------

    /**
     * Populates the list with all launchable apps found on the device.
     * Existing blocked states from any previously fetched rules are preserved.
     */
    public void loadInstalledApps() {
        loading.setValue(true);
        statusMessage.setValue("Loading installed apps…");

        new Thread(() -> {
            List<AppDiscoveryRepository.InstalledApp> installed =
                    appDiscoveryRepository.getLaunchableApps(getApplication());

            // Build a quick lookup of any already-known blocked states
            Map<String, Boolean> knownBlocked = new HashMap<>();
            List<ParentAppInfo> current = apps.getValue();
            if (current != null) {
                for (ParentAppInfo info : current) {
                    knownBlocked.put(info.packageName, info.blocked);
                }
            }

            List<ParentAppInfo> result = new ArrayList<>();
            for (AppDiscoveryRepository.InstalledApp app : installed) {
                boolean blocked = knownBlocked.getOrDefault(app.packageName, false);
                result.add(new ParentAppInfo(app.packageName, app.appName, blocked));
            }

            apps.postValue(result);
            loading.postValue(false);
            statusMessage.postValue(result.size() + " apps loaded — toggle to block, then tap Push");
        }).start();
    }

    // ---------------------------------------------------------------------------
    // Fetch rules from PocketBase (downloads current state to pre-fill toggles)
    // ---------------------------------------------------------------------------

    /**
     * Signs into PocketBase, downloads the rules for the given child device,
     * and merges them with the app list so blocked toggles reflect reality.
     */
    public void fetchRulesFromPocketBase(String childDeviceId) {
        String url = localRuleStore.getPocketBaseUrl();
        String email = localRuleStore.getPocketBaseEmail();
        String password = localRuleStore.getPocketBasePassword();

        if (url.isEmpty() || email.isEmpty() || password.isEmpty()) {
            statusMessage.setValue("Save PocketBase config first");
            return;
        }
        if (childDeviceId.isEmpty()) {
            statusMessage.setValue("Enter a Child Device ID first");
            return;
        }

        loading.setValue(true);
        statusMessage.setValue("Signing in to PocketBase…");

        pocketBaseClient.signIn(url, email, password, new PocketBaseClient.ResultCallback<String>() {
            @Override
            public void onSuccess(String token) {
                statusMessage.postValue("Downloading rules…");
                pocketBaseClient.fetchRules(url, token, childDeviceId,
                        new PocketBaseClient.ResultCallback<List<AppRule>>() {
                            @Override
                            public void onSuccess(List<AppRule> rules) {
                                // Build map: packageName → blocked
                                Map<String, Boolean> blockedMap = new HashMap<>();
                                for (AppRule r : rules) {
                                    blockedMap.put(r.getPackageName(), r.isBlocked());
                                }

                                // Merge into current list or start fresh from PocketBase data
                                List<ParentAppInfo> current = apps.getValue();
                                if (current != null && !current.isEmpty()) {
                                    for (ParentAppInfo info : current) {
                                        if (blockedMap.containsKey(info.packageName)) {
                                            info.blocked = Boolean.TRUE.equals(blockedMap.get(info.packageName));
                                        }
                                    }
                                    apps.postValue(new ArrayList<>(current)); // trigger redraw
                                } else {
                                    // No apps loaded yet — show what PocketBase knows about
                                    List<ParentAppInfo> fromPb = new ArrayList<>();
                                    for (AppRule r : rules) {
                                        fromPb.add(new ParentAppInfo(r.getPackageName(), r.getAppName(), r.isBlocked()));
                                    }
                                    apps.postValue(fromPb);
                                }

                                loading.postValue(false);
                                statusMessage.postValue("Fetched " + rules.size() + " rules from PocketBase");
                            }

                            @Override
                            public void onError(String message) {
                                loading.postValue(false);
                                statusMessage.postValue("Fetch failed: " + message);
                            }
                        });
            }

            @Override
            public void onError(String message) {
                loading.postValue(false);
                statusMessage.postValue("Sign-in failed: " + message);
            }
        });
    }

    // ---------------------------------------------------------------------------
    // Toggle — called by the adapter; no network call needed here
    // ---------------------------------------------------------------------------

    /** Called by the adapter when the parent flips a switch. State lives in the list items. */
    public void toggleBlocked(String packageName, boolean blocked) {
        // The adapter already mutates item.blocked directly; nothing extra needed.
        // This hook exists so the Activity can react if needed (e.g. show a count badge).
    }

    // ---------------------------------------------------------------------------
    // Push rules to PocketBase
    // ---------------------------------------------------------------------------

    /**
     * Signs in to PocketBase, then creates or updates one record per app in the list.
     * Only pushes apps where the parent has explicitly set blocked=true, plus any
     * existing PocketBase records that need to be updated.
     */
    public void pushRulesToPocketBase(String childDeviceId) {
        String url = localRuleStore.getPocketBaseUrl();
        String email = localRuleStore.getPocketBaseEmail();
        String password = localRuleStore.getPocketBasePassword();

        if (url.isEmpty() || email.isEmpty() || password.isEmpty()) {
            statusMessage.setValue("Save PocketBase config first");
            return;
        }
        if (childDeviceId.isEmpty()) {
            statusMessage.setValue("Enter a Child Device ID first");
            return;
        }

        List<ParentAppInfo> snapshot = apps.getValue();
        if (snapshot == null || snapshot.isEmpty()) {
            statusMessage.setValue("Load apps first");
            return;
        }

        // Build AppRule list from the current toggle states
        List<AppRule> rulesToPush = new ArrayList<>();
        for (ParentAppInfo info : snapshot) {
            rulesToPush.add(new AppRule(info.packageName, info.appName, info.blocked, 0));
        }

        loading.setValue(true);
        statusMessage.setValue("Signing in to PocketBase…");

        pocketBaseClient.signIn(url, email, password, new PocketBaseClient.ResultCallback<String>() {
            @Override
            public void onSuccess(String token) {
                statusMessage.postValue("Pushing " + rulesToPush.size() + " rules…");
                pocketBaseClient.pushRules(url, token, childDeviceId, rulesToPush,
                        new PocketBaseClient.ResultCallback<String>() {
                            @Override
                            public void onSuccess(String value) {
                                loading.postValue(false);
                                statusMessage.postValue(value);
                            }

                            @Override
                            public void onError(String message) {
                                loading.postValue(false);
                                statusMessage.postValue("Push error: " + message);
                            }
                        });
            }

            @Override
            public void onError(String message) {
                loading.postValue(false);
                statusMessage.postValue("Sign-in failed: " + message);
            }
        });
    }
}
