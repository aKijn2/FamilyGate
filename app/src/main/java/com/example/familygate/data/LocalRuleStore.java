package com.example.familygate.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalRuleStore {
    private static final String PREFS = "family_gate_prefs";
    private static final String KEY_RULES = "rules";
    private static final String KEY_PB_URL = "pocketbase_url";
    private static final String KEY_PB_EMAIL = "pocketbase_email";
    private static final String KEY_PB_PASSWORD = "pocketbase_password";
    private static final String KEY_CHILD_DEVICE_ID = "child_device_id";

    private final SharedPreferences prefs;

    public LocalRuleStore(Context context) {
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void savePocketBaseConfig(String url, String email, String password) {
        prefs.edit()
                .putString(KEY_PB_URL, url)
                .putString(KEY_PB_EMAIL, email)
                .putString(KEY_PB_PASSWORD, password)
                .apply();
    }

    public String getPocketBaseUrl() {
        return prefs.getString(KEY_PB_URL, "");
    }

    public String getPocketBaseEmail() {
        return prefs.getString(KEY_PB_EMAIL, "");
    }

    public String getPocketBasePassword() {
        return prefs.getString(KEY_PB_PASSWORD, "");
    }

    public void saveChildDeviceId(String childDeviceId) {
        prefs.edit().putString(KEY_CHILD_DEVICE_ID, childDeviceId).apply();
    }

    public String getChildDeviceId() {
        return prefs.getString(KEY_CHILD_DEVICE_ID, "");
    }

    public void saveRules(List<AppRule> rules) {
        JSONArray jsonArray = new JSONArray();
        for (AppRule rule : rules) {
            JSONObject item = new JSONObject();
            try {
                item.put("packageName", rule.getPackageName());
                item.put("appName", rule.getAppName());
                item.put("blocked", rule.isBlocked());
                item.put("dailyLimitMinutes", rule.getDailyLimitMinutes());
                jsonArray.put(item);
            } catch (JSONException ignored) {
            }
        }
        prefs.edit().putString(KEY_RULES, jsonArray.toString()).apply();
    }

    public List<AppRule> getRules() {
        List<AppRule> rules = new ArrayList<>();
        String raw = prefs.getString(KEY_RULES, "[]");
        try {
            JSONArray jsonArray = new JSONArray(raw);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject item = jsonArray.getJSONObject(i);
                rules.add(new AppRule(
                        item.optString("packageName"),
                        item.optString("appName"),
                        item.optBoolean("blocked", false),
                        item.optInt("dailyLimitMinutes", 0)
                ));
            }
        } catch (JSONException ignored) {
        }
        return rules;
    }

    public void updateBlockState(String packageName, boolean blocked) {
        List<AppRule> current = getRules();
        List<AppRule> updated = new ArrayList<>();
        boolean found = false;

        for (AppRule rule : current) {
            if (rule.getPackageName().equals(packageName)) {
                updated.add(new AppRule(rule.getPackageName(), rule.getAppName(), blocked, rule.getDailyLimitMinutes()));
                found = true;
            } else {
                updated.add(rule);
            }
        }

        if (!found) {
            updated.add(new AppRule(packageName, packageName, blocked, 0));
        }

        saveRules(updated);
    }

    public Map<String, AppRule> getRuleMap() {
        Map<String, AppRule> map = new HashMap<>();
        for (AppRule rule : getRules()) {
            map.put(rule.getPackageName(), rule);
        }
        return map;
    }
}
