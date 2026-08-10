<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Cite Circle

An academic social platform for researchers and students, featuring publishing,
citations, and communities.

This is a native **Android** app (Kotlin + Jetpack Compose). There is no iOS or
web build in this repository.

View your app in AI Studio: https://ai.studio/apps/d8379e77-32a4-4a3e-b0eb-a0fd0149357e

## Requirements

- An Android phone running **Android 7.0 (API 24)** or newer
- [Android Studio](https://developer.android.com/studio) (recommended), or a
  standalone [Android SDK](https://developer.android.com/tools) plus **JDK 17+**
  for command-line builds
- A [Gemini API key](https://aistudio.google.com/apikey) — optional; only the
  AI chat screen needs it

The Gradle wrapper is checked in, so you do not need Gradle installed. It pins
Gradle 9.1.0 to match the Android Gradle Plugin version in
`gradle/libs.versions.toml`.

## Install on your phone

### Option A — run directly from Android Studio (easiest)

1. **Open the project.** Android Studio → *Open* → select this directory. Let
   the Gradle sync finish.
2. **Add your Gemini API key** (optional). Create a file named `.env` in the
   project root:
   ```
   GEMINI_API_KEY=your_actual_key_here
   ```
   `.env` is gitignored, so your key never gets committed. If you skip this,
   the app builds and runs normally — only the AI chat screen will fail, since
   the build falls back to the placeholder value in `.env.example`.
3. **Turn on USB debugging on the phone.** Settings → *About phone* → tap
   *Build number* seven times, then Settings → *Developer options* → enable
   **USB debugging**.
4. **Connect the phone** by USB and tap *Allow* on the "Allow USB debugging?"
   prompt.
5. **Select your device** in the toolbar dropdown and press **Run** (▶).
   Android Studio builds, installs, and launches the app.

### Option B — build an APK and sideload it

From the project root:

```bash
# Point at your Android SDK if it isn't already set
export ANDROID_HOME="$HOME/Android/Sdk"   # macOS: ~/Library/Android/sdk

./gradlew assembleDebug                   # Windows: gradlew.bat assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Copy it to the phone (USB, Google Drive, email), open it with the phone's file
manager, and allow **install from unknown sources** when prompted.

With USB debugging enabled you can also install it straight over the cable:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Debug builds are signed with the standard auto-generated debug keystore
(`~/.android/debug.keystore`) that the Android SDK creates on first use. Nothing
extra to configure, and no keystore is checked into this repository.

## Known limitation: Firebase is not configured

`app/google-services.json` is a **placeholder** (project id `dummy-project`, api
key `dummy-api-key`). The app launches and runs fine on this config — Firebase
is not initialized at startup, and the app's local data uses a Room database on
the device.

Anything that talks to Firebase — **sign-in and Firestore sync** — will fail
until you supply a real config:

1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
2. Add an Android app with package name `com.aistudio.folio.wzpx`
3. Download the generated `google-services.json` and replace `app/google-services.json`

## Release builds

The `release` build type is wired for Play Store upload signing and expects a
keystore that is **not** in this repository:

```bash
export KEYSTORE_PATH=/path/to/my-upload-key.jks   # defaults to ./my-upload-key.jks
export STORE_PASSWORD=...
export KEY_PASSWORD=...

./gradlew assembleRelease
```

The key alias is `upload`. For simply installing on your own phone, use the
debug build above — a release build is not required.

If you have already published this app in AI Studio, please
[request an upload key reset](https://support.google.com/googleplay/android-developer/answer/9842756#zippy=%2Crequest-an-upload-key-reset)
in the Google Play Console.
