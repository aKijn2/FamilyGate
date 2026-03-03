package com.example.familygate.parent;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.familygate.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

/**
 * Launcher Activity for the parent-flavor APK.
 *
 * The parent can:
 *  1. Enter PocketBase credentials and a child device ID, then save them.
 *  2. Tap "Load Apps" to see all apps installed on THIS device.
 *  3. Tap "Fetch Rules" to pull the current rules for the child device from
 *     PocketBase and pre-fill the blocked toggles.
 *  4. Flip switches to block / unblock apps.
 *  5. Tap "Push Rules to PocketBase" to send the full rule set to the backend,
 *     where the child device will pick it up within 15 minutes.
 */
public class ParentMainActivity extends AppCompatActivity {

    private ParentViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_main);

        viewModel = new ViewModelProvider(this).get(ParentViewModel.class);

        // -----------------------------------------------------------------------
        // View references
        // -----------------------------------------------------------------------
        EditText inputUrl           = findViewById(R.id.input_pb_url);
        EditText inputEmail         = findViewById(R.id.input_parent_email);
        EditText inputPassword      = findViewById(R.id.input_parent_password);
        EditText inputChildDeviceId = findViewById(R.id.input_child_device_id);

        MaterialButton buttonSaveConfig  = findViewById(R.id.button_save_config);
        MaterialButton buttonLoadApps    = findViewById(R.id.button_load_apps);
        MaterialButton buttonFetchRules  = findViewById(R.id.button_fetch_rules);
        MaterialButton buttonPushRules   = findViewById(R.id.button_push_rules);

        LinearProgressIndicator progressBar = findViewById(R.id.progress_bar);
        TextView textStatus = findViewById(R.id.text_status);

        RecyclerView recycler = findViewById(R.id.recycler_parent_apps);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setNestedScrollingEnabled(false);

        ParentAppAdapter adapter = new ParentAppAdapter(
                (packageName, blocked) -> viewModel.toggleBlocked(packageName, blocked));
        recycler.setAdapter(adapter);

        // -----------------------------------------------------------------------
        // Pre-fill saved config
        // -----------------------------------------------------------------------
        inputUrl.setText(viewModel.getSavedUrl());
        inputEmail.setText(viewModel.getSavedEmail());
        inputPassword.setText(viewModel.getSavedPassword());
        inputChildDeviceId.setText(viewModel.getSavedChildDeviceId());

        // -----------------------------------------------------------------------
        // Button actions
        // -----------------------------------------------------------------------
        buttonSaveConfig.setOnClickListener(v -> {
            viewModel.saveConfig(
                    inputUrl.getText().toString().trim(),
                    inputEmail.getText().toString().trim(),
                    inputPassword.getText().toString().trim(),
                    inputChildDeviceId.getText().toString().trim()
            );
            Toast.makeText(this, "Config saved", Toast.LENGTH_SHORT).show();
        });

        buttonLoadApps.setOnClickListener(v -> viewModel.loadInstalledApps());

        buttonFetchRules.setOnClickListener(v -> {
            String childId = inputChildDeviceId.getText().toString().trim();
            if (childId.isEmpty()) {
                Toast.makeText(this, R.string.child_device_id_required, Toast.LENGTH_SHORT).show();
                return;
            }
            // Persist the ID before fetching so PocketBase calls use the latest value
            viewModel.saveConfig(
                    inputUrl.getText().toString().trim(),
                    inputEmail.getText().toString().trim(),
                    inputPassword.getText().toString().trim(),
                    childId
            );
            viewModel.fetchRulesFromPocketBase(childId);
        });

        buttonPushRules.setOnClickListener(v -> {
            String childId = inputChildDeviceId.getText().toString().trim();
            if (childId.isEmpty()) {
                Toast.makeText(this, R.string.child_device_id_required, Toast.LENGTH_SHORT).show();
                return;
            }
            // Persist latest config before pushing
            viewModel.saveConfig(
                    inputUrl.getText().toString().trim(),
                    inputEmail.getText().toString().trim(),
                    inputPassword.getText().toString().trim(),
                    childId
            );
            viewModel.pushRulesToPocketBase(childId);
        });

        // -----------------------------------------------------------------------
        // Observe LiveData
        // -----------------------------------------------------------------------
        viewModel.getApps().observe(this, adapter::submitList);

        viewModel.getStatusMessage().observe(this, textStatus::setText);

        viewModel.getLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            buttonLoadApps.setEnabled(!isLoading);
            buttonFetchRules.setEnabled(!isLoading);
            buttonPushRules.setEnabled(!isLoading);
        });
    }
}
