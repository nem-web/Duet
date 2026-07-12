# Duet - Secure Direct Distribution & Update System Guide

This guide details how to build, sign, configure, and securely distribute the **Duet** application outside of the Google Play Store (direct APK distribution/sideloading).

---

## 📖 Table of Contents
1. [Build Instructions (Debug vs. Release)](#1-build-instructions-debug-vs-release)
2. [Keystore Generation & Signing Config](#2-keystore-generation--signing-config)
3. [Firebase Update System Configuration](#3-firebase-update-system-configuration)
4. [How to Publish and Deploy Updates](#4-how-to-publish-and-deploy-updates)
5. [Security & Permissions Explained](#5-security--permissions-explained)

---

## 1. Build Instructions (Debug vs. Release)

Gradle tasks are pre-configured to build both debug and release variants. Always execute tasks using the `gradle` executable directly (avoiding `./gradlew` which might not be supported in some container environments).

### Build Debug APK
This compiles the application with the debug signing key, enabling logcats and debug features.
```bash
gradle :app:assembleDebug
```
The compiled debug APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Build Release APK
This compiles the highly optimized, release-ready binary, signed with your secure developer upload key.
```bash
gradle :app:assembleRelease
```
The compiled release APK will be located at:
`app/build/outputs/apk/release/app-release.apk`

---

## 2. Keystore Generation & Signing Config

The project is structured to sign release builds using a production-ready keystore. For security, credentials are read dynamically from system environment variables rather than being hardcoded.

### A. How to Generate a Private Keystore
Use the standard Java JDK `keytool` command in your terminal to generate a developer keystore:

```bash
keytool -genkey -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

*   **Keystore Filename:** `my-upload-key.jks`
*   **Key Alias:** `upload`
*   **Validity:** `10000` days (approx. 27 years)

### B. Where to Place the Keystore
Place the generated `my-upload-key.jks` file in the **root directory** of your project (where `settings.gradle.kts` lives).

### C. Supplying Signing Credentials
During the release build process, the signing configuration reads these values from environment variables. Set them in your environment or build container:

| Variable Name | Description | Example Value |
| :--- | :--- | :--- |
| `KEYSTORE_PATH` | Path to the `.jks` file (defaults to root) | `/path/to/my-upload-key.jks` |
| `STORE_PASSWORD` | The password of your keystore | `my_secure_store_pass` |
| `KEY_PASSWORD` | The password of your key alias | `my_secure_key_pass` |
| `KEY_ALIAS` | The alias of your signing key | `upload` |

On Linux/macOS:
```bash
export STORE_PASSWORD="your_store_password"
export KEY_PASSWORD="your_key_password"
gradle :app:assembleRelease
```

---

## 3. Firebase Update System Configuration

Duet includes a custom update manager that bypasses Google Play and retrieves the latest version information directly from Firebase Firestore. It compares the current version code with the latest online version, shows optional or mandatory update prompts, downloads the APK using the system `DownloadManager`, and installs it via `FileProvider`.

### A. Create the Firestore Structure
1. Go to your [Firebase Console](https://console.firebase.google.com/).
2. Navigate to **Firestore Database** and ensure it is enabled.
3. Create a new collection named **`app_updates`**.
4. In this collection, create a document with the exact ID **`latest`**.

### B. Document Schema
Add the following fields with their respective data types to the `latest` document:

```json
{
  "versionCode": 2,
  "versionName": "1.1.0",
  "apkUrl": "https://example.com/downloads/duet-update-v1.1.apk",
  "isMandatory": false,
  "releaseNotes": "• Customized period notifications added for both partners\n• Water intake tracking stability fixes\n• Fluid transition animations in relationship settings"
}
```

*   **`versionCode`** (Integer/Long): The release number of the newest APK. If this number is higher than the `versionCode` in the installed app (defined in `app/build.gradle.kts`), the update prompt will trigger.
*   **`versionName`** (String): The user-visible name of the release.
*   **`apkUrl`** (String): The direct, secure (`https://`) download link to the compiled APK.
*   **`isMandatory`** (Boolean): If set to `true`, the update is mandatory. The update overlay becomes persistent, blocking the main UI, and the "Not Now" dismiss button is hidden.
*   **`releaseNotes`** (String): Bulleted list or text describing what's new.

---

## 4. How to Publish and Deploy Updates

When releasing a new update to your users, follow these steps:

1.  **Increment App Version:** Increase `versionCode` and `versionName` inside `app/build.gradle.kts` (e.g., from `versionCode = 1` to `versionCode = 2`).
2.  **Compile & Sign:** Build your release APK using the signing steps outlined in Section 2.
3.  **Host the APK:** Upload the signed `app-release.apk` to a secure public storage bucket (such as Firebase Storage, AWS S3, GitHub Releases, or a secure private web server). Note down the direct HTTPS download URL.
4.  **Publish on Firestore:** Update the `latest` document under the `app_updates` collection in Firestore with the new `versionCode`, `versionName`, direct `apkUrl`, `isMandatory` flag, and `releaseNotes`.
5.  **Instant Delivery:** The next time users open the application, they will instantly see the Material 3 "New Update Available!" prompt.

---

## 5. Security & Permissions Explained

To ensure full transparency and device safety, the app utilizes only the minimum required set of permissions. No security mechanisms are bypassed.

### Required Permissions in `AndroidManifest.xml`

*   **`android.permission.INTERNET`**: Required to communicate with Firebase Auth, Firestore Database, and download the update APK.
*   **`android.permission.ACCESS_NETWORK_STATE`**: Used to detect network connectivity, ensuring graceful fallbacks when offline.
*   **`android.permission.REQUEST_INSTALL_PACKAGES`**: Required starting from Android 8.0 (API 26) to request package installations. When the APK is downloaded, this permission allows launching the native installer. If the user hasn't granted "Install unknown apps" to Duet, the app securely prompts them to enable it in system settings.
*   **`android.permission.POST_NOTIFICATIONS`**: Allows posting local reminders, water intake reminders, and cycle trackers to the status bar.

### Secure File Sharing via `FileProvider`
To comply with Android's strict storage security rules, the downloaded update APK is saved directly to the app's secure internal or external files directory. It is shared with the OS Package Installer via the secure `FileProvider` (`androidx.core.content.FileProvider`), which grants temporary, read-only content URI permissions. This avoids requesting broad storage permissions (such as `WRITE_EXTERNAL_STORAGE` or `MANAGE_EXTERNAL_STORAGE`).
