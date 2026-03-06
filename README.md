# RATGDO Android Controller

An Android app for controlling a [RATGDO](https://paulwiggles.com/ratgdo/) garage door opener over your local WiFi network. Built around the ESPHome Web API, it uses a persistent Server-Sent Events (SSE) connection for real-time door state updates without polling.

---

## Hardware Requirements

- A RATGDO device flashed with [ESPHome](https://esphome.io/) firmware with the Web API enabled
- The RATGDO must be on the same local WiFi network as your phone (the app will check for this and walk you on target with error messages)

---

## Features

- **Real-time door state** — live Open / Closed / Opening / Closing status via SSE stream
- **Smart button states** — Open and Close buttons enable/disable contextually based on door movement
- **Network validation** — verifies WiFi connectivity and confirms the RATGDO is reachable before enabling controls
- **Auto-reconnect** — automatically re-establishes the SSE connection on disconnect or network interruption
- **Persistent settings** — device IP or hostname saved via SharedPreferences, survives app restarts
- **Light/dark mode** — adapts to system theme

---

## Setup

1. Install the app on your Android device (API 26+)
2. Ensure your phone is connected to the same WiFi network as your RATGDO
3. Tap the **gear icon** in the top right
4. Enter your RATGDO's IP address or hostname (e.g. `192.168.0.13`)
5. Tap **Save** — the app will connect automatically and display the current door state

---

## Architecture

The app connects to the ESPHome Web API's `/events` endpoint and keeps a persistent SSE stream open for the lifetime of the app's foreground session.

- `SseManager` — runs on a background thread, parses the SSE stream line by line, and fires callbacks to `MainActivity` on connection, door state changes, and disconnection
- `MainActivity` — implements `SseManager.SseListener` and updates the UI on the main thread via `runOnUiThread()`
- `SettingsActivity` — single screen for configuring the device IP, persisted via `SharedPreferences`
- `RatgdoConstants` — shared constants class for SharedPreferences keys

The SSE stream is stopped in `onPause()` and restarted in `onResume()` to avoid background battery drain.

---

## Roadmap

- [ ] Prompt to open WiFi settings when not connected
- [ ] App icon
- [ ] Home screen widget

---

## Building

Clone the repo and open in Android Studio. No API keys or external dependencies required — the app communicates directly with your RATGDO over the local network.

```
git clone git@github.com:In-disBEALief/ratgdo-android.git
```

Minimum SDK: API 26 (Android 8.0)  
Target SDK: API 34  
Language: Java  
Build system: Gradle (Groovy DSL)

---

## Related Projects

- [RATGDO](https://paulwiggles.com/ratgdo/) — the hardware this app is built for
- [ESPHome](https://esphome.io/) — the firmware powering the Web API
