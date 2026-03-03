package com.example.familygate.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PocketBaseClient {
    public interface ResultCallback<T> {
        void onSuccess(T value);

        void onError(String message);
    }

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();

    private String normalizeBaseUrl(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        // Strip trailing slash
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        // Strip PocketBase admin UI path (/_/ or /_) if accidentally pasted
        if (trimmed.endsWith("/_")) {
            trimmed = trimmed.substring(0, trimmed.length() - 2);
        }
        // Strip trailing slash again after stripping admin path
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    public void signIn(String baseUrl, String email, String password, ResultCallback<String> callback) {
        new Thread(() -> {
            try {
                String normalizedUrl = normalizeBaseUrl(baseUrl);
                JSONObject payload = new JSONObject();
                payload.put("identity", email);
                payload.put("password", password);

                Request request = new Request.Builder()
                        .url(normalizedUrl + "/api/collections/users/auth-with-password")
                        .post(RequestBody.create(payload.toString(), JSON))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = "";
                        if (response.body() != null) {
                            try { errorBody = " - " + response.body().string(); } catch (Exception ignored) {}
                        }
                        callback.onError("PocketBase login failed: " + response.code() + errorBody);
                        return;
                    }
                    if (response.body() == null) {
                        callback.onError("PocketBase login failed: empty response");
                        return;
                    }

                    JSONObject json = new JSONObject(response.body().string());
                    String token = json.optString("token", "");
                    if (token.isEmpty()) {
                        callback.onError("No token returned by PocketBase");
                        return;
                    }
                    callback.onSuccess(token);
                }
            } catch (IOException | JSONException exception) {
                callback.onError(exception.getMessage());
            }
        }).start();
    }

    public void fetchRules(String baseUrl, String authToken, String childDeviceId, ResultCallback<List<AppRule>> callback) {
        new Thread(() -> {
            try {
                String normalizedUrl = normalizeBaseUrl(baseUrl);
                String path = normalizedUrl + "/api/collections/app_rules/records?perPage=200&filter=(childDeviceId='" + childDeviceId + "')";

                Request request = new Request.Builder()
                        .url(path)
                        .addHeader("Authorization", "Bearer " + authToken)
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        callback.onError("Failed to fetch rules: " + response.code());
                        return;
                    }

                    JSONObject payload = new JSONObject(response.body().string());
                    JSONArray items = payload.optJSONArray("items");
                    List<AppRule> result = new ArrayList<>();
                    if (items != null) {
                        for (int i = 0; i < items.length(); i++) {
                            JSONObject item = items.getJSONObject(i);
                            result.add(new AppRule(
                                    item.optString("packageName"),
                                    item.optString("appName", item.optString("packageName")),
                                    item.optBoolean("blocked", false),
                                    item.optInt("dailyLimitMinutes", 0)
                            ));
                        }
                    }
                    callback.onSuccess(result);
                }
            } catch (IOException | JSONException exception) {
                callback.onError(exception.getMessage());
            }
        }).start();
    }

    /** Fetches a map of packageName → PocketBase record ID for all rules of a child device. */
    public void fetchRuleIdMap(String baseUrl, String authToken, String childDeviceId,
                               ResultCallback<Map<String, String>> callback) {
        new Thread(() -> {
            try {
                String normalizedUrl = normalizeBaseUrl(baseUrl);
                String path = normalizedUrl + "/api/collections/app_rules/records?perPage=200&filter=(childDeviceId='" + childDeviceId + "')";

                Request request = new Request.Builder()
                        .url(path)
                        .addHeader("Authorization", "Bearer " + authToken)
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        callback.onError("Failed to fetch rule IDs: " + response.code());
                        return;
                    }
                    JSONObject payload = new JSONObject(response.body().string());
                    JSONArray items = payload.optJSONArray("items");
                    Map<String, String> idMap = new HashMap<>();
                    if (items != null) {
                        for (int i = 0; i < items.length(); i++) {
                            JSONObject item = items.getJSONObject(i);
                            String pkg = item.optString("packageName");
                            String id = item.optString("id");
                            if (!pkg.isEmpty() && !id.isEmpty()) {
                                idMap.put(pkg, id);
                            }
                        }
                    }
                    callback.onSuccess(idMap);
                }
            } catch (IOException | JSONException e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    /** Creates a new app_rules record in PocketBase. */
    public void createRule(String baseUrl, String authToken, String childDeviceId, AppRule rule,
                           ResultCallback<String> callback) {
        new Thread(() -> {
            try {
                String normalizedUrl = normalizeBaseUrl(baseUrl);
                JSONObject body = new JSONObject();
                body.put("childDeviceId", childDeviceId);
                body.put("packageName", rule.getPackageName());
                body.put("appName", rule.getAppName());
                body.put("blocked", rule.isBlocked());
                body.put("dailyLimitMinutes", rule.getDailyLimitMinutes());

                Request request = new Request.Builder()
                        .url(normalizedUrl + "/api/collections/app_rules/records")
                        .addHeader("Authorization", "Bearer " + authToken)
                        .post(RequestBody.create(body.toString(), JSON))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String err = response.body() != null ? response.body().string() : "";
                        callback.onError("Create failed " + response.code() + ": " + err);
                    } else {
                        callback.onSuccess("created");
                    }
                }
            } catch (IOException | JSONException e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    /** Updates an existing app_rules record in PocketBase. */
    public void updateRule(String baseUrl, String authToken, String recordId, AppRule rule,
                           ResultCallback<String> callback) {
        new Thread(() -> {
            try {
                String normalizedUrl = normalizeBaseUrl(baseUrl);
                JSONObject body = new JSONObject();
                body.put("appName", rule.getAppName());
                body.put("blocked", rule.isBlocked());
                body.put("dailyLimitMinutes", rule.getDailyLimitMinutes());

                Request request = new Request.Builder()
                        .url(normalizedUrl + "/api/collections/app_rules/records/" + recordId)
                        .addHeader("Authorization", "Bearer " + authToken)
                        .patch(RequestBody.create(body.toString(), JSON))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String err = response.body() != null ? response.body().string() : "";
                        callback.onError("Update failed " + response.code() + ": " + err);
                    } else {
                        callback.onSuccess("updated");
                    }
                }
            } catch (IOException | JSONException e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    /**
     * Pushes a list of rules to PocketBase for a given child device.
     * Fetches existing record IDs first, then creates or updates each rule in sequence.
     * The callback reports progress as "X/Y done" and final success/error.
     */
    public void pushRules(String baseUrl, String authToken, String childDeviceId,
                          List<AppRule> rules, ResultCallback<String> callback) {
        fetchRuleIdMap(baseUrl, authToken, childDeviceId, new ResultCallback<Map<String, String>>() {
            @Override
            public void onSuccess(Map<String, String> idMap) {
                new Thread(() -> {
                    int total = rules.size();
                    int done = 0;
                    List<String> errors = new ArrayList<>();

                    for (AppRule rule : rules) {
                        String existingId = idMap.get(rule.getPackageName());
                        boolean[] finished = {false};
                        String[] lastError = {null};

                        Object lock = new Object();

                        if (existingId != null) {
                            updateRule(baseUrl, authToken, existingId, rule, new ResultCallback<String>() {
                                @Override
                                public void onSuccess(String value) {
                                    synchronized (lock) { finished[0] = true; lock.notifyAll(); }
                                }
                                @Override
                                public void onError(String message) {
                                    synchronized (lock) { lastError[0] = message; finished[0] = true; lock.notifyAll(); }
                                }
                            });
                        } else {
                            createRule(baseUrl, authToken, childDeviceId, rule, new ResultCallback<String>() {
                                @Override
                                public void onSuccess(String value) {
                                    synchronized (lock) { finished[0] = true; lock.notifyAll(); }
                                }
                                @Override
                                public void onError(String message) {
                                    synchronized (lock) { lastError[0] = message; finished[0] = true; lock.notifyAll(); }
                                }
                            });
                        }

                        // Wait for the async call to finish
                        synchronized (lock) {
                            while (!finished[0]) {
                                try { lock.wait(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                            }
                        }

                        done++;
                        if (lastError[0] != null) {
                            errors.add(rule.getPackageName() + ": " + lastError[0]);
                        }
                    }

                    if (errors.isEmpty()) {
                        callback.onSuccess("Pushed " + done + "/" + total + " rules successfully");
                    } else {
                        callback.onError(errors.size() + " errors:\n" + String.join("\n", errors));
                    }
                }).start();
            }

            @Override
            public void onError(String message) {
                callback.onError("Could not fetch existing rules: " + message);
            }
        });
    }
}
