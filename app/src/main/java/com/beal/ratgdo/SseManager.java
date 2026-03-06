package com.beal.ratgdo;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SseManager {

    public interface SseListener {
        void onConnected();
        void onDoorState(String state, String operation);
        void onDisconnected(String reason);
    }

    private static final int RECONNECT_DELAY_MS = 3000;

    private final Context context;
    private final SseListener listener;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String baseUrl;
    private boolean running = false;
    private Future<?> currentTask;

    public SseManager(Context context, SseListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void start() {
        running = true;
        scheduleConnect();
    }

    public void stop() {
        running = false;
        if (currentTask != null) {
            currentTask.cancel(true);
        }
    }

    private void scheduleConnect() {
        if (!running) return;
        currentTask = executor.submit(this::connectLoop);
    }

    private void connectLoop() {
        // First check WiFi before attempting connection
        if (!isOnWifi()) {
            listener.onDisconnected("Not connected to WiFi");
            waitThenReconnect();
            return;
        }

        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + "/events");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(0); // No read timeout - we want to stay connected
            conn.connect();

            if (conn.getResponseCode() != 200) {
                listener.onDisconnected("Could not reach RATGDO - is it powered on and on the same network? Check IP in Settings.");
                waitThenReconnect();
                return;
            }

            // Connected - now parse the SSE stream
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));

            String line;
            String currentEvent = null;
            boolean hasPinged = false;

            while (running && (line = reader.readLine()) != null) {

                if (line.startsWith("event:")) {
                    currentEvent = line.substring(6).trim();

                } else if (line.startsWith("data:")) {
                    String data = line.substring(5).trim();

                    if ("ping".equals(currentEvent) && !hasPinged) {
                        // First ping confirms we're talking to an ESPHome device
                        hasPinged = true;
                        listener.onConnected();
                    }

                    if ("state".equals(currentEvent) && hasPinged && !data.isEmpty()) {
                        JSONObject json = new JSONObject(data);
                        String nameId = json.optString("name_id", "");

                        if ("cover/Door".equals(nameId)) {
                            String state = json.optString("state", "");
                            String operation = json.optString("current_operation", "IDLE");
                            listener.onDoorState(state, operation);
                        }
                    }
                }
                // Blank line = end of SSE event, reset current event type
                else if (line.isEmpty()) {
                    currentEvent = null;
                }
            }

            // Stream ended
            listener.onDisconnected("Connection lost");
            waitThenReconnect();

        } catch (Exception e) {
            if (running) {
                listener.onDisconnected("Could not reach RATGDO - is it powered on and on the same network? Check IP in Settings.");
                waitThenReconnect();
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void waitThenReconnect() {
        if (!running) return;
        try {
            Thread.sleep(RECONNECT_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        scheduleConnect();
    }

    private boolean isOnWifi() {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        if (caps == null) return false;
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }
}
