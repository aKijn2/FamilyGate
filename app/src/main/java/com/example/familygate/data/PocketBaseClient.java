package com.example.familygate.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
}
