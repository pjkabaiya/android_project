# Android App Skeleton

This folder is now a Firebase-ready Android (Java) project scaffold. It includes the Google services Gradle plugin, Firebase BoM, Google Maps, and a `google-services.json` file in the app module.

What to open

- `settings.gradle.kts`
- `build.gradle.kts`
- `app/build.gradle.kts`

What is configured

- Project-level Google services plugin: `com.google.gms.google-services` version `4.4.4`
- App-level Google services plugin
- Firebase BoM `34.14.0`
- Firebase Auth and Analytics
- Google Maps and Location dependencies
- Package name matched to the Firebase config: `matatu_system.A1`

Run it in Android Studio

1. Open the `android-app` folder as a project.
2. Sync Gradle.
3. Build or run the `app` module.

