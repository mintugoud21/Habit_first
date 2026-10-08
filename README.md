# 📱 Habits First (Android)

> **"Do the thing, then scroll. Earn your apps back."**

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026--35)-brightgreen.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2F%20Repository-blue.svg)]()
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Local--First-success.svg)]()
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

An open-source Android implementation of **Habits First**, built on the **Premack Principle** (Grandma's Rule: high-probability behaviors like social media scrolling reinforce low-probability behaviors like working out or reading). Distracting apps remain locked until your daily habits are completed.

---

## 🌟 Key Features

- **🛡️ Premack App Blocker Engine**:
  - Leverages Android `AccessibilityService` (`HabitBlockerAccessibilityService`) to detect and intercept target apps (Instagram, TikTok, YouTube, Reddit, X/Twitter, games, etc.).
  - Instantly presents a non-intrusive **Habits First Lock Screen** (`BlockedOverlayActivity`).

- **🔒 Interactive Lock Screen Overlay**:
  - Displays remaining habits required to unlock the app in real time.
  - One-tap quick launchers for built-in timers (Meditation, Pomodoro, Workout) or manual habit check-offs.
  - Emergency 1-minute snooze button (can be toggled/disabled).
  - Safe "Go Home" redirection.

- **🏦 Payment & Banking Apps Safe Mode**:
  - Automatically exempts 100+ UPI, banking, and payment applications (Google Pay, PhonePe, Paytm, BHIM, Chase, Bank of America, Wells Fargo, SBI, HDFC, ICICI, etc.) from accessibility interception.
  - Dynamically restricts `serviceInfo.packageNames` to only non-financial apps to eliminate security warnings.
  - One-tap "Pause Blocker for Payments (10 Min)" option in Settings.

- **🎨 Day & Night Mode Support**:
  - Fully adaptive Light and Dark themes (`values` and `values-night`).
  - High-contrast text rendering and edge-to-edge support with safe window insets (no status bar, navigation bar, or camera cutout overlap on Android 15 / API 35).

- **⚙️ Full Habit Customization & Visual Badges**:
  - Customize habit title, description, daily target value, unit, and habit tracking type.
  - **Visual Icon Picker**: Choose from icons for Steps, Dumbbell/Workout, Meditation, Book/Reading, Pomodoro, Water, Sleep, Coding, Journaling, Health, etc.
  - **Color Accent Picker**: Choose from vibrant emerald, cyan, violet, rose, amber, and indigo badges.
  - Reorder, edit, delete, or restore default habits with one tap.

- **🤖 Automated Habit Tracking**:
  - **Hardware Step Sensor (`StepSensorHelper`)**: Uses `Sensor.TYPE_STEP_COUNTER` to auto-track daily walking toward your goal (e.g. 10,000 steps).
  - **Productive App Usage Tracking (`UsageTrackerHelper`)**: Automatically tracks time spent inside educational or productive apps (Duolingo, Kindle, Anki, etc.) using `UsageStatsManager`.
  - **Built-in Timers (`TimerActivity`)**: Built-in countdown timers for Meditation, Workout, and Pomodoro focus sessions with vibration feedback.

- **📊 "A Year of Habits" Consistency Heatmap**:
  - GitHub-style contribution grid visualization displaying daily completion rates and streak streaks across weeks and months.

- **📱 Home Screen Widget & Quick Settings Tile**:
  - **4x2 App Widget** (`HabitsAppWidgetProvider`): Displays live daily progress, streaks, and pending habits on your home screen.
  - **Quick Settings Status Tile** (`HabitStatusTileService`): View unlocked/locked status right from your notification shade.

- **🔒 100% Local-First & Privacy Respecting**:
  - No account registration required.
  - No analytics, tracking, or cloud sync.
  - Zero subscription fees.
  - Complete **JSON Backup & Restore** with clipboard export/import.

---

## 🏗️ Project Architecture

```
app/src/main/java/com/habitsfirst/app/
├── data/
│   ├── model/           # Habit, BlockRule, HabitLog, HabitType data models
│   └── repository/      # HabitRepository (SharedPreferences JSON storage, backup/restore)
├── service/
│   ├── HabitBlockerAccessibilityService.kt # App blocking interception engine
│   ├── StepSensorHelper.kt                 # Hardware step counter listener
│   └── UsageTrackerHelper.kt               # App usage tracking helper
├── ui/
│   ├── MainActivity.kt                     # 4-tab dashboard (Habits, Blocks, Streaks, Settings)
│   ├── BlockedOverlayActivity.kt           # Fullscreen Premack lock screen
│   ├── TimerActivity.kt                    # Habit focus countdown timer
│   ├── adapters/                           # HabitAdapter, SettingsHabitAdapter, BlockRuleAdapter
│   └── views/                              # CircularProgressView, ContributionHeatmapView
└── util/
    ├── PaymentSafetyHelper.kt              # Banking/UPI safety exemption whitelist
    ├── NotificationHelper.kt               # Daily reminder notification channels
    ├── DailyReminderReceiver.kt            # Morning & evening reminder alarms
    └── BootReceiver.kt                     # Boot completed alarm rescheduler
```

---

## 📱 How to Install on Your Android Phone

### Option A: Sideload APK directly (Recommended)
1. Download the pre-built APK from the root of this project: `HabitsFirst-v1.0.0.apk` (or from [Releases](../../releases)).
2. Transfer or download the APK onto your Android device.
3. Open the file on your device. When prompted, enable **"Allow from this source"** in Android Settings.
4. Tap **Install** and open **Habits First**.

### Option B: Install via ADB (Developer Mode)
If your phone is connected to your computer with USB Debugging enabled:
```bash
adb install -r HabitsFirst-v1.0.0.apk
```

---

## 🔐 Required Permissions Setup

To enable app blocking and automated habit detection:

1. **Accessibility Service**:
   - Go to **Settings > Accessibility > Installed Apps / Downloaded Apps**.
   - Tap **Habits First** and toggle **ON**.
   - *(On Android 13+, if "Restricted setting" appears, go to App Info > tap the 3 dots in the top-right > tap "Allow restricted settings", then enable Accessibility).*
2. **Usage Access**:
   - Required for automated productive app tracking (e.g. reading/studying).
   - Go to **Settings > Apps > Special app access > Usage access > Habits First > Allow**.
3. **Notifications** (Android 13+):
   - Allow notifications when prompted to receive daily morning motivation and evening streak protection alerts.

---

## 🛠️ Building From Source

### Prerequisites
- JDK 17 or higher
- Android SDK (API 35 / Build-Tools 35.0.0)
- Gradle 8.14+

### Build Debug APK:
```bash
./gradlew assembleDebug
```
The output APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

### Run Unit Tests:
```bash
./gradlew testDebugUnitTest
```

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
