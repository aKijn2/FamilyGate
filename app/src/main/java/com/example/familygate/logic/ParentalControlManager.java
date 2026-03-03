package com.example.familygate.logic;

import android.content.Context;

import com.example.familygate.data.AppRule;
import com.example.familygate.data.AppUsage;
import com.example.familygate.data.LocalRuleStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ParentalControlManager {
    private final AppDiscoveryRepository discoveryRepository = new AppDiscoveryRepository();
    private final UsageStatsRepository usageStatsRepository = new UsageStatsRepository();

    public List<AppUsage> buildDashboardData(Context context, LocalRuleStore ruleStore) {
        Map<String, AppRule> rules = ruleStore.getRuleMap();
        Map<String, Long> usage = usageStatsRepository.getMinutesUsedToday(context);

        List<AppDiscoveryRepository.InstalledApp> installedApps = discoveryRepository.getLaunchableApps(context);
        List<AppUsage> appUsages = new ArrayList<>();

        for (AppDiscoveryRepository.InstalledApp app : installedApps) {
            AppRule rule = rules.get(app.packageName);
            long minutes = usage.getOrDefault(app.packageName, 0L);
            boolean blocked = rule != null && rule.isBlocked();
            int limit = rule != null ? rule.getDailyLimitMinutes() : 0;
            boolean overLimit = limit > 0 && minutes >= limit;
            appUsages.add(new AppUsage(app.packageName, app.appName, minutes, blocked, overLimit));
        }
        return appUsages;
    }

    public boolean shouldBlockApp(String packageName, LocalRuleStore ruleStore) {
        Map<String, AppRule> rules = ruleStore.getRuleMap();
        AppRule rule = rules.get(packageName);
        return rule != null && rule.isBlocked();
    }
}
