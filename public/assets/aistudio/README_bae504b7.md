# Evonith CCI Management - Android

## Option A - Install now as an Android app (PWA, no APK)
1. Upload everything inside the `www` folder to an HTTPS web address (plant intranet with HTTPS, or any HTTPS host).
2. On the Android phone open that address in Chrome.
3. Tap the menu (three dots) -> "Install app" / "Add to Home screen".
It then opens full-screen with its own icon and works offline.

## Option B - Build a real APK (needs a PC with Android Studio + internet)
1. Install Node.js 18+ and Android Studio.
2. In this folder run:
   npm install
   npx cap add android
   npx cap sync android
   npx cap open android
3. In Android Studio: Build > Build Bundle(s)/APK(s) > Build APK(s).
4. Copy app-debug.apk to phones and install (allow "install unknown apps").
For distribution use Build > Generate Signed Bundle / APK.

## Logo
Sign in as Admin > Users tab > Company logo to upload the Evonith logo (stored per phone).
To embed it permanently, paste a data:image/png;base64,... string into DEFAULT_LOGO in www/index.html and replace www/icon-192.png and icon-512.png with the logo.

## Important
- Data and user accounts are stored on each phone only (not shared between phones). Use Reports > Backup regularly.
- Passwords are stored unencrypted in the phone browser storage: prototype only. Use a secure server before official use.
