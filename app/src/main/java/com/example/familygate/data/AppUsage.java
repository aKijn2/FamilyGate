package com.example.familygate.data;

public class AppUsage {
    private final String packageName;
    private final String appName;
    private final long minutesToday;
    private final boolean blocked;
    private final boolean overLimit;

    public AppUsage(String packageName, String appName, long minutesToday, boolean blocked, boolean overLimit) {
        this.packageName = packageName;
        this.appName = appName;
        this.minutesToday = minutesToday;
        this.blocked = blocked;
        this.overLimit = overLimit;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getAppName() {
        return appName;
    }

    public long getMinutesToday() {
        return minutesToday;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public boolean isOverLimit() {
        return overLimit;
    }
}
