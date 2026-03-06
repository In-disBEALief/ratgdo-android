package com.beal.ratgdo;

import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements SseManager.SseListener {

    private Button buttonOpen;
    private Button buttonClose;
    private TextView textViewConnection;
    private TextView textViewDoor;

    private SseManager sseManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonOpen = findViewById(R.id.buttonOpen);
        buttonClose = findViewById(R.id.buttonClose);
        textViewConnection = findViewById(R.id.textViewConnection);
        textViewDoor = findViewById(R.id.textViewDoor);

        buttonOpen.setEnabled(false);
        buttonClose.setEnabled(false);

        buttonOpen.setOnClickListener(v -> sendCommand("open"));
        buttonClose.setOnClickListener(v -> sendCommand("close"));

        sseManager = new SseManager(this, this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences(RatgdoConstants.PREF_FILE, MODE_PRIVATE);
        String gdoIP = prefs.getString(RatgdoConstants.PREF_KEY_IP, "");

        if(gdoIP.isEmpty()){
            textViewConnection.setText("No device IP set - tap the gear icon to configure");
            buttonOpen.setEnabled(false);
            buttonClose.setEnabled(false);
            return;
        }

        String baseUrl = "http://" + gdoIP;
        sseManager.setBaseUrl(baseUrl);
        sseManager.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sseManager.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- SseListener callbacks ---

    @Override
    public void onConnected() {
        runOnUiThread(() -> {
            textViewConnection.setText("Connected");
            textViewDoor.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onDoorState(String state, String operation) {
        runOnUiThread(() -> {
            textViewConnection.setText("Connected");
            switch (operation) {
                case "OPENING":
                    textViewDoor.setText("Opening...");
                    buttonOpen.setEnabled(false);
                    buttonClose.setEnabled(true);
                    break;
                case "CLOSING":
                    textViewDoor.setText("Closing...");
                    buttonOpen.setEnabled(true);
                    buttonClose.setEnabled(false);
                    break;
                default: // IDLE
                    if ("OPEN".equals(state)) {
                        textViewDoor.setText("Open - Ready");
                    } else {
                        textViewDoor.setText("Closed - Ready");
                    }
                    buttonOpen.setEnabled(true);
                    buttonClose.setEnabled(true);
                    break;
            }
        });
    }

    @Override
    public void onDisconnected(String reason) {
        runOnUiThread(() -> {
            textViewConnection.setText(reason);
            textViewDoor.setVisibility(View.INVISIBLE);
            buttonOpen.setEnabled(false);
            buttonClose.setEnabled(false);
        });
    }

    // --- Commands ---

    private void sendCommand(String action) {
        SharedPreferences prefs = getSharedPreferences(RatgdoConstants.PREF_FILE, MODE_PRIVATE);
        String gdoIP = prefs.getString(RatgdoConstants.PREF_KEY_IP, "");
        if(gdoIP.isEmpty()) return;
        String baseUrl = "http://" + gdoIP;

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(baseUrl + "/cover/Door/" + action);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setFixedLengthStreamingMode(0);
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.connect();
                conn.getResponseCode();
            } catch (Exception e) {
                runOnUiThread(() ->
                        textViewConnection.setText("Command error: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }
}