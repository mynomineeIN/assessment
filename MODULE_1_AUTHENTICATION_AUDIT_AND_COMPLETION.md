# Authentication and Firebase Setup

## Sign-in and registration

- **Mobile OTP:** users enter a phone number, request and verify an OTP, then continue to the home screen. Firebase Authentication automatically creates an account for a number on its first successful verification and signs in returning users.
- **Google:** users can choose an account through Credential Manager and authenticate with Firebase Authentication.
- **User profile:** after successful authentication, the app writes the Firebase UID, phone number, display name, email, photo URL, provider, and timestamps to `users/{uid}` in Cloud Firestore.
- **Reminders:** reminder documents are scoped to `users/{uid}/reminders/{cloudId}`. Room remains the offline source of truth; writes queue for Firestore, and remote reminders are restored after login.

## Firebase configuration

The Android application ID is `com.mynominee.notify` in project `notify-68514`, matching the current `app/google-services.json`. The file has no OAuth clients yet, so Google Sign-In will remain unavailable until the Web OAuth client is configured and the updated file is downloaded.

In Firebase Console:

1. Enable **Phone** and **Google** under Authentication → Sign-in method.
2. Configure SHA-1/SHA-256 signing fingerprints for Google sign-in.
3. Create Cloud Firestore and publish the repository's `firestore.rules`.

Do not add Firebase service-account credentials to the Android app. See [README.md](./README.md) for build and run steps.

## Verification

The debug unit-test suite passes with the upgraded Android/Kotlin toolchain. Google Services resource generation and APK assembly remain to be verified after the matching `google-services.json` is installed.
