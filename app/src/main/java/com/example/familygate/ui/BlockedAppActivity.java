package com.example.familygate.ui;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.familygate.R;
import com.google.android.material.button.MaterialButton;

public class BlockedAppActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_app);

        String blockedPackage = getIntent().getStringExtra("blocked_package");
        TextView subtitle = findViewById(R.id.text_blocked_subtitle);
        subtitle.setText(getString(R.string.blocked_subtitle, blockedPackage == null ? "" : blockedPackage));

        MaterialButton button = findViewById(R.id.button_go_back);
        button.setOnClickListener(v -> finish());
    }
}
