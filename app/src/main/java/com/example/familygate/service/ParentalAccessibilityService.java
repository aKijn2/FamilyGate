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

        // Never interfere with our own UI.
        if (getPackageName().equals(packageName)) {
            return;
        }

        if (controlManager.shouldBlockApp(packageName, ruleStore)) {
            // Only launch BlockedAppActivity when it is not already on screen.
            // This prevents a rapid-fire loop while the block screen is visible,
            // but immediately re-blocks the moment the child dismisses it and
            // the blocked app returns to the foreground.
            if (!BlockedAppActivity.isVisible) {
                Intent blockIntent = new Intent(this, BlockedAppActivity.class);
                blockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                blockIntent.putExtra("blocked_package", packageName);
                startActivity(blockIntent);
            }
        }
    }

    @Override
    public void onInterrupt() {
    }
}
