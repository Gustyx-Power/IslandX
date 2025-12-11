# CapsuleEdge

<div align="center">

![CapsuleEdge](https://img.shields.io/badge/CapsuleEdge-Dynamic%20Island-4CAF50?style=for-the-badge&logo=android&logoColor=white)
![Android](https://img.shields.io/badge/Android-8.0+-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)

**Dynamic Island Experience for Android**

*Transform your notification bar into an interactive, animated capsule*

</div>

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🔔 **Smart Notifications** | Display notifications in an elegant capsule with tap-to-open and auto-expand support |
| 🎵 **Media Control** | Control music with play/pause, skip, and view album art from the Dynamic Island |
| 🔋 **Charging Animation** | Beautiful charging indicator with battery percentage and animated lightning bolt |
| 📶 **Bluetooth Status** | See connected Bluetooth devices when pairing with headphones or speakers |
| 🔕 **Ringer Mode** | Quick visual feedback when switching between silent, vibrate, and normal modes |
| ⚙️ **Customizable** | Adjust position, size, and scale to perfectly fit your device's notch or camera |
| 🌙 **Hide in Landscape** | Automatically hide when playing games or watching videos in landscape mode |

---

## 📱 Screenshots

<div align="center">

| Idle State | Media Playing | Notification |
|:----------:|:-------------:|:------------:|
| ⬛ Capsule | 🎵 Now Playing | 🔔 Alert |

</div>

---

## 📋 Requirements

- ✅ Android 8.0 (Oreo) or higher
- ✅ Device with punch-hole/notch camera (recommended)
- ✅ Accessibility service enabled

---

## 🔐 Required Permissions

| Permission | Purpose |
|------------|---------|
| 📱 **Display Over Other Apps** | Show the Dynamic Island overlay on screen |
| ♿ **Accessibility Service** | Read notifications and display them in the Island |
| 🔔 **Notification Access** | Show persistent service notification |
| 🔋 **Battery Optimization** | Keep the Island running in background |
| 🎵 **Media Listener** | Detect music playback from apps like Spotify |

---

## 🛠️ Tech Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose + Material 3
- **Architecture:** MVVM with Repository Pattern
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 36 (Android 16)
- **Dependencies:**
  - AndroidX Lifecycle & ViewModel
  - Navigation Compose
  - DataStore Preferences
  - Coil for image loading
  - Kotlin Coroutines

---

## 🚀 Installation

1. Download the latest APK from Release
2. Enable "Install from Unknown Sources" if prompted
3. Install the APK
4. Open CapsuleEdge and grant all required permissions
5. Tap "Start CapsuleEdge" to activate the Dynamic Island

---

## ⚙️ Configuration

You can customize the Dynamic Island from the app:

| Setting | Range | Description |
|---------|-------|-------------|
| **Horizontal Offset** | -100 to 100 | Move the island left/right |
| **Vertical Offset** | -30 to 80 | Move the island up/down |
| **Capsule Width** | 80 to 200dp | Adjust the collapsed width |
| **Scale** | 0.5x to 1.5x | Scale the entire island |
| **Hide in Landscape** | On/Off | Auto-hide when in landscape mode |

---


---
</div>
