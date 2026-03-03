package com.example.familygate.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.familygate.R;
import com.google.android.material.button.MaterialButton;

public class BlockedAppActivity extends AppCompatActivity {

    /**
     * True while this activity is in the foreground.
     * Read by ParentalAccessibilityService to avoid duplicate screen launches
     * while still re-blocking the moment the child dismisses this screen.
     */
    public static volatile boolean isVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_app);

        String blockedPackage = getIntent().getStringExtra("blocked_package");
        TextView subtitle = findViewById(R.id.text_blocked_subtitle);
        subtitle.setText(getString(R.string.blocked_subtitle, blockedPackage == null ? "" : blockedPackage));

        // "Go back" sends the child to the HOME screen, not back into the blocked app.
        MaterialButton button = findViewById(R.id.button_go_back);
        button.setOnClickListener(v -> goHome());

        // Hardware Back key also goes home instead of returning to the blocked app.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goHome();
            }
        });
    }

    @Override
    protected void onResume() { super.onResume(); isVisible = true;  }

    @Override
    protected void onPause()  { super.onPause();  isVisible = false; }

    private void goHome() {
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(home);
        finish();
    }
}
