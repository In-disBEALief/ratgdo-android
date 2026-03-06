package com.beal.ratgdo;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        EditText editTextIp = findViewById(R.id.editTextIp);
        Button buttonSave = findViewById(R.id.buttonSave);

        // Load existing value if set, so user can see and edit current setting
        SharedPreferences prefs = getSharedPreferences(RatgdoConstants.PREF_FILE, MODE_PRIVATE);
        String currentIp = prefs.getString(RatgdoConstants.PREF_KEY_IP, "");
        if (!currentIp.isEmpty()) {
            editTextIp.setText(currentIp);
        }

        buttonSave.setOnClickListener(v -> {
            String ip = editTextIp.getText().toString().trim();
            if (!ip.isEmpty()) {
                prefs.edit()
                        .putString(RatgdoConstants.PREF_KEY_IP, ip)
                        .apply();
                finish(); // Return to MainActivity
            } else {
                editTextIp.setError("Please enter an IP address or hostname");
            }
        });
    }
}