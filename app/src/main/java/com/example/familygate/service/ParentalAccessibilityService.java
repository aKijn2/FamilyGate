package com.example.familygate.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

import com.example.familygate.data.LocalRuleStore;
import com.example.familygate.logic.ParentalControlManager;
import com.example.familygate.ui.BlockedAppActivity;

public class ParentalAccessibilityService extends AccessibilityService {
    private final ParentalControlManager controlManager = new ParentalControlManager();
    private LocalRuleStore ruleStore;
    private String lastBlockedPackage = "";

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        ruleStore = new LocalRuleStore(getApplicationContext());
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null || ruleStore == null) {
            return;
        }

        String packageName = event.getPackageName().toString();
        if (getPackageName().equals(packageName)) {
            return;
        }

        if (controlManager.shouldBlockApp(packageName, ruleStore)) {
            if (packageName.equals(lastBlockedPackage)) {
                return;
            }
            lastBlockedPackage = packageName;
            Intent blockIntent = new Intent(this, BlockedAppActivity.class);
            blockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            blockIntent.putExtra("blocked_package", packageName);
            startActivity(blockIntent);
        } else {
            lastBlockedPackage = "";
        }
    }

    @Override
    public void onInterrupt() {
    }
}
