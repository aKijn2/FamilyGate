package com.example.familygate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.familygate.data.LocalRuleStore;
import com.example.familygate.data.PocketBaseClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

/**
 * Launcher activity — only the parent can get past this screen.
 *
 * Flow:
 *  • If a valid auth token is already stored → skip straight to MainActivity.
 *  • Otherwise show a login form (PocketBase URL, email, password, child device ID).
 *  • On successful PocketBase authentication the token is saved and MainActivity opens.
 *
 * The child cannot reach MainActivity (which contains all block-toggle controls)
 * without knowing the parent's password.
 */
public class LoginActivity extends AppCompatActivity {

    private LocalRuleStore store;
    private PocketBaseClient pbClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        store = new LocalRuleStore(this);
        pbClient = new PocketBaseClient();

        // If the parent is already authenticated, jump straight to the dashboard.
        if (!store.getAuthToken().isEmpty()) {
            openDashboard();
            return;
        }

        setContentView(R.layout.activity_login);

        EditText inputUrl      = findViewById(R.id.login_input_url);
        EditText inputEmail    = findViewById(R.id.login_input_email);
        EditText inputPassword = findViewById(R.id.login_input_password);
        EditText inputDeviceId = findViewById(R.id.login_input_device_id);
        TextView textStatus    = findViewById(R.id.login_text_status);
        MaterialButton btnLogin = findViewById(R.id.login_button);
        LinearProgressIndicator progress = findViewById(R.id.login_progress);

        // Pre-fill any previously saved values so the parent doesn't retype them every time.
        inputUrl.setText(store.getPocketBaseUrl());
        inputEmail.setText(store.getPocketBaseEmail());
        inputDeviceId.setText(store.getChildDeviceId());

        btnLogin.setOnClickListener(v -> {
            String url      = inputUrl.getText().toString().trim();
            String email    = inputEmail.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();
            String deviceId = inputDeviceId.getText().toString().trim();

            if (url.isEmpty() || email.isEmpty() || password.isEmpty() || deviceId.isEmpty()) {
                textStatus.setText(R.string.login_fill_all_fields);
                return;
            }

            // Disable UI while authenticating
            setUiEnabled(false, btnLogin, progress);
            textStatus.setText(R.string.login_signing_in);

            pbClient.signIn(url, email, password, new PocketBaseClient.ResultCallback<String>() {
                @Override
                public void onSuccess(String token) {
                    // Persist everything BEFORE opening MainActivity
                    store.savePocketBaseConfig(url, email, password);
                    store.saveChildDeviceId(deviceId);
                    store.saveAuthToken(token);
                    openDashboard();
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        textStatus.setText(getString(R.string.login_failed, message));
                        setUiEnabled(true, btnLogin, progress);
                    });
                }
            });
        });
    }

    private void openDashboard() {
        startActivity(new Intent(this, MainActivity.class));
        finish(); // remove LoginActivity from back stack so Back doesn't return here
    }

    private void setUiEnabled(boolean enabled, MaterialButton btn, LinearProgressIndicator progress) {
        btn.setEnabled(enabled);
        progress.setVisibility(enabled ? View.GONE : View.VISIBLE);
    }
}
