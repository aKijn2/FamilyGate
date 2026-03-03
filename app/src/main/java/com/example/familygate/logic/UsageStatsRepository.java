package com.example.familygate.logic;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Process;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsageStatsRepository {
    public boolean hasUsageAccess(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    public Map<String, Long> getMinutesUsedToday(Context context) {
        Map<String, Long> minutesByPackage = new HashMap<>();
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);

        long endTime = System.currentTimeMillis();
        long startTime = endTime - 24L * 60L * 60L * 1000L;
        List<UsageStats> usageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);

        if (usageStats == null) {
            return minutesByPackage;
        }

        for (UsageStats stat : usageStats) {
            long minutes = stat.getTotalTimeInForeground() / (60 * 1000);
            minutesByPackage.put(stat.getPackageName(), minutes);
        }
        return minutesByPackage;
    }
}
