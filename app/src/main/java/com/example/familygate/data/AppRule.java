package com.example.familygate.data;

public class AppRule {
    private final String packageName;
    private final String appName;
    private final boolean blocked;
    private final int dailyLimitMinutes;

    public AppRule(String packageName, String appName, boolean blocked, int dailyLimitMinutes) {
        this.packageName = packageName;
        this.appName = appName;
        this.blocked = blocked;
        this.dailyLimitMinutes = dailyLimitMinutes;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getAppName() {
        return appName;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public int getDailyLimitMinutes() {
        return dailyLimitMinutes;
    }
}
