package com.example.familygate.ui;

import android.app.Application;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.familygate.data.AppRule;
import com.example.familygate.data.AppUsage;
import com.example.familygate.data.LocalRuleStore;
import com.example.familygate.data.PocketBaseClient;
import com.example.familygate.logic.ParentalControlManager;
import com.example.familygate.logic.UsageStatsRepository;

import java.util.ArrayList;
import java.util.List;

public class MainViewModel extends AndroidViewModel {
    private final MutableLiveData<List<AppUsage>> appUsages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>("");

    private final LocalRuleStore localRuleStore;
    private final ParentalControlManager controlManager;
    private final PocketBaseClient pocketBaseClient;
    private final UsageStatsRepository usageStatsRepository;

    public MainViewModel(@NonNull Application application) {
        super(application);
        this.localRuleStore = new LocalRuleStore(application);
        this.controlManager = new ParentalControlManager();
        this.pocketBaseClient = new PocketBaseClient();
        this.usageStatsRepository = new UsageStatsRepository();
    }

    public LiveData<List<AppUsage>> getAppUsages() {
        return appUsages;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public String getSavedUrl() {
        return localRuleStore.getPocketBaseUrl();
    }

    public String getSavedEmail() {
        return localRuleStore.getPocketBaseEmail();
    }

    public String getSavedPassword() {
        return localRuleStore.getPocketBasePassword();
    }

    public boolean hasUsageAccess() {
        return usageStatsRepository.hasUsageAccess(getApplication());
    }

    public boolean isAccessibilityEnabled() {
        String enabledServices = Settings.Secure.getString(getApplication().getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabledServices != null && enabledServices.contains(getApplication().getPackageName());
    }

    public String getSavedChildDeviceId() {
        return localRuleStore.getChildDeviceId();
    }

    public void savePocketBaseConfig(String url, String email, String password, String childDeviceId) {
        localRuleStore.savePocketBaseConfig(url, email, password);
        localRuleStore.saveChildDeviceId(childDeviceId);
        statusMessage.setValue("PocketBase config saved");
    }

    public void refreshDashboard() {
        new Thread(() -> {
            List<AppUsage> usage = controlManager.buildDashboardData(getApplication(), localRuleStore);
            appUsages.postValue(usage);
        }).start();
    }

    public void toggleBlocked(String packageName, boolean blocked) {
        localRuleStore.updateBlockState(packageName, blocked);
        refreshDashboard();
    }

    public boolean isParentLoggedIn() {
        return !localRuleStore.getAuthToken().isEmpty();
    }

    public void logout() {
        localRuleStore.clearAuthToken();
    }

    public void syncRulesFromPocketBase(String childDeviceId) {
        String url = localRuleStore.getPocketBaseUrl();
        String email = localRuleStore.getPocketBaseEmail();
        String password = localRuleStore.getPocketBasePassword();

        if (url.isEmpty() || email.isEmpty() || password.isEmpty()) {
            statusMessage.setValue("Configure PocketBase URL, email and password first");
            return;
        }

        statusMessage.setValue("Signing in to PocketBase...");
        pocketBaseClient.signIn(url, email, password, new PocketBaseClient.ResultCallback<String>() {
            @Override
            public void onSuccess(String token) {
                statusMessage.postValue("Syncing rules...");
                pocketBaseClient.fetchRules(url, token, childDeviceId, new PocketBaseClient.ResultCallback<List<AppRule>>() {
                    @Override
                    public void onSuccess(List<AppRule> value) {
                        localRuleStore.saveRules(value);
                        statusMessage.postValue("Rules synced: " + value.size());
                        refreshDashboard();
                    }

                    @Override
                    public void onError(String message) {
                        statusMessage.postValue("Sync failed: " + message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                statusMessage.postValue("Sign-in failed: " + message);
            }
        });
    }
}
