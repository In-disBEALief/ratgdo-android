# RATGDO Android Controller

An Android app for controlling a [RATGDO](https://ratcloud.llc/) garage door opener over your local Wi-Fi network. A modest comfort improvement over directly accessing the ESPHome Web UI directly from the phone's browser. 

Built around the ESPHome Web API, it uses a persistent Server-Sent Events (SSE) connection for real-time door state updates without polling. 

---

## Hardware Requirements

- A RATGDO device flashed with [ESPHome](https://esphome.io/) firmware with the Web API enabled
- The RATGDO must be on the same local Wi-Fi network as your phone (the app will check for this and walk you on target with error messages)

---

## Features

- **Real-time door state** — live Open / Closed / Opening / Closing status via SSE stream
- **Smart button states** — Open and Close buttons enable/disable contextually based on door movement
- **Network validation** — verifies Wi-Fi connectivity and confirms the RATGDO is reachable before enabling controls
- **Auto-reconnect** — automatically re-establishes the SSE connection on disconnect or network interruption
- **Persistent settings** — device IP or hostname saved via SharedPreferences, survives app restarts
- **Light/dark mode** — adapts to system theme

---

## Download
[Download v1.0.0 APK](https://github.com/In-disBEALief/ratgdo-android/releases/download/v1.0.0/ratgdo-android-v1.0.0.apk)

---

## Setup

1. (Prerequisite) Install and configure your RATGDO device on a local Wi-Fi network
2. Install the app on your Android device (Android API 26+ supported, see *Releases* for the latest)
3. Ensure your phone is connected to the same Wi-Fi network as your RATGDO
4. Tap the **gear icon** in the top right
5. Enter your RATGDO's IP address or hostname
6. Tap **Save** — the app will connect automatically and display the current door state
7. Open and close your garage door at your leisure

---

## Architecture

The app connects to the ESPHome Web API's `/events` endpoint and keeps a persistent SSE stream open for the lifetime of the app's foreground session.

- `SseManager` — runs on a background thread, parses the SSE stream and fires callbacks to `MainActivity` on connection, door state changes, and disconnection
- `MainActivity` — implements `SseManager.SseListener` and updates the UI on the main thread via `runOnUiThread()`
- `SettingsActivity` — single screen for configuring the device IP, persisted via `SharedPreferences`
- `RatgdoConstants` — shared constants class for SharedPreferences keys

The SSE stream is stopped in `onPause()` and restarted in `onResume()` to avoid background battery drain.

And that's it!

---

## Roadmap

- [ ] Prompt to open WiFi settings when not connected

---

## Building it yourself (if you want to make your own customizations)

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

- [RATGDO LLC](https://ratcloud.llc/) / [RATGDO Github](https://github.com/PaulWieland/ratgdo) — the hardware this app is built for
- [ESPHome](https://esphome.io/) — the firmware powering the Web API

![RATGDO App Icon](app/src/main/ic_launcher-playstore.png)
