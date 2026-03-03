package com.example.familygate.logic;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppDiscoveryRepository {
    public static class InstalledApp {
        public final String packageName;
        public final String appName;

        public InstalledApp(String packageName, String appName) {
            this.packageName = packageName;
            this.appName = appName;
        }
    }

    public List<InstalledApp> getLaunchableApps(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN, null);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(launcherIntent, 0);

        List<InstalledApp> apps = new ArrayList<>();
        for (ResolveInfo resolveInfo : resolveInfos) {
            ApplicationInfo appInfo = resolveInfo.activityInfo.applicationInfo;
            String label = packageManager.getApplicationLabel(appInfo).toString();
            apps.add(new InstalledApp(resolveInfo.activityInfo.packageName, label));
        }

        Collections.sort(apps, Comparator.comparing(a -> a.appName.toLowerCase()));
        return apps;
    }
}
