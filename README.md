# Notify

A fully featured, native Android reminder application built with **Jetpack Compose**, **Material 3**, **Room Database**, **Firebase Authentication**, **AlarmManager**, and **Clean Architecture**.

---

## Features

- **Authentication:** Phone OTP (with automatic new-account registration) and Google Sign-In using Firebase Authentication.
- **Cloud backup:** Authenticated user profiles and reminders are stored in Firestore under each user's UID. Room remains available for offline use.
- **Alarm Dashboard:** A calm, interactive schedule with next-alarm highlights, active/completed counts, Today/Upcoming/Paused filters, and quick edit, pause, and delete actions.
- **Reminder CRUD:** Create, read, update, and delete reminders with mandatory/optional fields (Name, Description, Start Date, Time, Repeat Frequency, Repeat Until, Alarm Sound, Snooze).
- **Recurrence Engine:** Supports Daily, Weekly, Monthly, and Yearly recurrences with custom repeat days and auto-scheduling of the next occurrence.
- **Alarm & Notification Engine:** Alarm-style alerts, exact alarm scheduling via `AlarmManager`, notification delivery, and interactive Snooze/Dismiss actions.
- **Device Restart Recovery:** Automatic alarm restoration on device boot via `BootReceiver`.
- **Offline Functionality:** Reminder CRUD and local notification triggers use Room SQLite. Firestore queues local writes while offline and cloud reminders are restored after sign-in.
- **Permissions Handling:** Notification and exact alarm permission checks and recovery flows.

---

## Technology Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose, Material 3
- **Local Storage:** Room Database (SQLite)
- **Authentication:** Firebase Authentication (phone OTP and Google) and Cloud Firestore
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
   - The file must be downloaded for Firebase project `notify-68514` and Android package `com.mynominee.notify`. It supplies the API key and OAuth client IDs required by Firebase and Google Sign-In.
   - Enable **Phone** in Firebase Console → Authentication → Sign-in method.
   - Enable **Google** in Firebase Console → Authentication → Sign-in method and configure the app's signing certificate fingerprints.
   - Create a Cloud Firestore database and publish the root `firestore.rules` rules.

3. **Build & Run via Gradle:**
   ```powershell
   .\gradlew clean
   .\gradlew :app:assembleDebug
   ```

4. **Install APK:**
   ```powershell
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```

Phone OTP requires a real Android device or a Firebase-configured test phone number. The Firebase Android configuration file is project-specific; do not put service-account keys in the app.

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
