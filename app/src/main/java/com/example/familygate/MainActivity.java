package com.example.familygate;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.familygate.data.SyncWorker;
import com.example.familygate.ui.AppUsageAdapter;
import com.example.familygate.ui.MainViewModel;
import com.google.android.material.button.MaterialButton;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private MainViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        EditText inputUrl = findViewById(R.id.input_pb_url);
        EditText inputEmail = findViewById(R.id.input_parent_email);
        EditText inputPassword = findViewById(R.id.input_parent_password);
        EditText inputChildDeviceId = findViewById(R.id.input_child_device_id);
        TextView textStatus = findViewById(R.id.text_status);

        inputUrl.setText(viewModel.getSavedUrl());
        inputEmail.setText(viewModel.getSavedEmail());
        inputPassword.setText(viewModel.getSavedPassword());
        inputChildDeviceId.setText(viewModel.getSavedChildDeviceId());

        MaterialButton buttonSaveConfig = findViewById(R.id.button_save_config);
        MaterialButton buttonSync = findViewById(R.id.button_sync_rules);
        MaterialButton buttonUsagePermission = findViewById(R.id.button_usage_permission);
        MaterialButton buttonAccessibilityPermission = findViewById(R.id.button_accessibility_permission);
        MaterialButton buttonRefresh = findViewById(R.id.button_refresh);

        RecyclerView recycler = findViewById(R.id.recycler_apps);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        AppUsageAdapter adapter = new AppUsageAdapter((packageName, blocked) -> viewModel.toggleBlocked(packageName, blocked));
        recycler.setAdapter(adapter);

        buttonSaveConfig.setOnClickListener(v -> {
            String childDeviceId = inputChildDeviceId.getText().toString().trim();
            viewModel.savePocketBaseConfig(
                    inputUrl.getText().toString().trim(),
                    inputEmail.getText().toString().trim(),
                    inputPassword.getText().toString().trim(),
                    childDeviceId
            );
            schedulePeriodicSync();
        });

        buttonSync.setOnClickListener(v -> {
            String childDeviceId = inputChildDeviceId.getText().toString().trim();
            if (childDeviceId.isEmpty()) {
                Toast.makeText(this, R.string.child_device_required, Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.syncRulesFromPocketBase(childDeviceId);
        });

        buttonUsagePermission.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
        });

        buttonAccessibilityPermission.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        buttonRefresh.setOnClickListener(v -> viewModel.refreshDashboard());

        viewModel.getAppUsages().observe(this, adapter::submitList);
        viewModel.getStatusMessage().observe(this, textStatus::setText);

        updatePermissionIndicators();
        viewModel.refreshDashboard();

        // Re-register periodic sync in case the app was updated or work was cancelled
        if (!viewModel.getSavedUrl().isEmpty()) {
            schedulePeriodicSync();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionIndicators();
    }

    private void updatePermissionIndicators() {
        TextView usageState = findViewById(R.id.text_usage_permission_state);
        TextView accessibilityState = findViewById(R.id.text_accessibility_permission_state);

        usageState.setText(viewModel.hasUsageAccess() ? R.string.permission_enabled : R.string.permission_disabled);
        accessibilityState.setText(viewModel.isAccessibilityEnabled() ? R.string.permission_enabled : R.string.permission_disabled);
    }

    private void schedulePeriodicSync() {
        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                SyncWorker.class, 15, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "familygate_periodic_sync",
                ExistingPeriodicWorkPolicy.UPDATE,
                syncRequest);
    }
}