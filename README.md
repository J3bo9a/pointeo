# Pointeo — Android project

This folder is a complete Android Studio / Gradle project. The Pointeo app runs inside it (offline, no server).
Sharing uses Android's own share sheet, saving writes to Downloads/Pointeo.

## Get the APK without installing anything (GitHub)
1. Create a free repository on github.com and upload the contents of this folder (keep the `.github` folder).
2. Open the **Actions** tab → "Build APK" → wait about 3 minutes.
3. Open the finished run → download **Pointeo-apk** → unzip → `app-debug.apk`.
4. Send the APK to the phone, open it, and allow "Install unknown apps" when asked.

## Or with Android Studio
Open this folder, then Build → Build APK(s). The file is in `app/build/outputs/apk/debug/`.

Requires Android 10 or later. The debug APK is signed with a debug key: fine for your own phones. For the Play Store you need a release signing key.
