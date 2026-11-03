# Android Reminder Application

A fully featured, native Android reminder application built with **Jetpack Compose**, **Material 3**, **Room Database**, **Firebase Authentication**, **AlarmManager**, and **Clean Architecture**.

---

## Features

- **Authentication:** Mobile OTP verification and Google Sign-In via Credential Manager.
- **Home Screen Dashboard:** Welcome greeting, categorized reminders (Today, Upcoming, Completed), and quick management actions.
- **Reminder CRUD:** Create, read, update, and delete reminders with mandatory/optional fields (Name, Description, Start Date, Time, Repeat Frequency, Repeat Until, Alarm Sound, Snooze).
- **Recurrence Engine:** Supports Daily, Weekly, Monthly, and Yearly recurrences with custom repeat days and auto-scheduling of the next occurrence.
- **Alarm & Notification Engine:** Alarm-style alerts, exact alarm scheduling via `AlarmManager`, notification delivery, and interactive Snooze/Dismiss actions.
- **Device Restart Recovery:** Automatic alarm restoration on device boot via `BootReceiver`.
- **Offline Functionality:** Complete offline CRUD and local notification triggers powered by Room SQLite database.
- **Permissions Handling:** Notification and exact alarm permission checks and recovery flows.

---

## Technology Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose, Material 3
- **Local Storage:** Room Database (SQLite)
- **Authentication:** Firebase Authentication & Google Credential Manager
- **Alarm Scheduling:** Android AlarmManager
- **Notifications:** NotificationManager
- **Dependency Injection:** Hilt
- **Concurrency:** Kotlin Coroutines & Flow

---

## Architecture

The project follows Clean Architecture principles:
- **Presentation Layer:** Jetpack Compose screens and ViewModels (`HomeViewModel`, `AddEditReminderViewModel`, etc.).
- **Domain Layer:** Business models (`Reminder`), repositories, and recurrence calculation utilities.
- **Data Layer:** Room entities, DAOs, converters, and repository implementations (`ReminderRepositoryImpl`, `AuthRepositoryImpl`).

---

## Setup & Execution

1. **Prerequisites:**
   - Android Studio Arctic Fox or newer.
   - JDK 17.
   - Android SDK (compileSdk 35, minSdk 26).
   - Connected Android Device or Emulator.

2. **Firebase Configuration:**
   - Add your valid `google-services.json` file to the `app/` directory.

3. **Build & Run via Gradle:**
   ```powershell
   .\gradlew clean
   .\gradlew :app:assembleDebug
   ```

4. **Install APK:**
   ```powershell
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```

---

## Verification & Testing

- Run unit tests:
  ```powershell
  .\gradlew test
  ```
- Run instrumentation tests:
  ```powershell
  .\gradlew :app:connectedDebugAndroidTest
  ```
