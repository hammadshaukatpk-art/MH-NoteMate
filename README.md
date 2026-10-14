# MH NoteMate — Android Notes, Tasks & Reminders

**Developer credit:** MH Developers  
**Package ID:** `com.mhdevelopers.notemate`  
**Version:** 1.0.0 (versionCode 1)  
**Language:** Java; Android native views  
**Minimum Android:** 8.0 (API 26)  
**Target Android:** API 36  
**Storage:** Local SQLite; no account or internet required

## Included features

- Branded splash screen and blue/white dashboard inspired by the supplied four-screen reference.
- Dashboard with Notes, Tasks, Reminders, Completed and today's schedule.
- Create, edit, search, pin and delete colorful notes; category filters (Personal, Work, Study, Other, Important).
- Create tasks, mark them complete, and browse a calendar by date.
- Set optional due date/time for any note or task.
- Optional advance alert: 5, 10, 15, 30, 60 minutes, or 1 day before; plus due-time alarm.
- Daily and weekly repeating alarms.
- Notifications with alarm sound for due events; reminder alerts before the event.
- Alarms rescheduled after phone reboot, app update, and clock/time-zone change.
- Settings screen for notification and precise-alarm permissions.
- 'MH Developers' footer credit and About section.
- Corrected PNG launcher icons and 512×512 Play Store artwork in `design/`.

## Open in Android Studio

1. Install a current **Android Studio**, Android SDK **Platform 36**, Android SDK Build Tools and JDK 17 or later.
2. Extract the project ZIP and select **Open** in Android Studio. Select the **MHNoteMate** folder containing `settings.gradle`.
3. Let Android Studio download the Android Gradle Plugin 8.13.2 and Gradle 8.13. The first sync requires internet access.
4. If the IDE complains about a missing official Gradle wrapper JAR, use the included `gradlew.bat` (Windows) or `./gradlew` (macOS/Linux) from a terminal to download and run Gradle, or configure Android Studio to use a locally installed Gradle 8.13. The included bootstrap scripts do not require a wrapper JAR.
5. Connect an Android phone with USB debugging enabled, or start an emulator. Press **Run**.

## Build an installable APK

In Android Studio: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

Or from the project folder:

- **Windows:** `gradlew.bat assembleDebug`
- **macOS/Linux:** `./gradlew assembleDebug`

After a successful build, the debug APK is located at:
`app/build/outputs/apk/debug/app-debug.apk`

The debug APK is for testing. It is **not** the signed release package for Play Store distribution.

## Create a signed Play Store AAB

1. Android Studio: **Build > Generate Signed Bundle / APK**.
2. Select **Android App Bundle**, then **Create new** signing key (or select your existing keystore).
3. Select the **release** build variant, complete the wizard, and keep your keystore and passwords safe.
4. Upload the signed `.aab` to the Google Play Console after testing, filling the Data safety declaration and completing the required store information and testing steps.
5. Upload `design/MHNoteMate_PlayStore_512.png` as the 512×512 store icon; the app launcher icon is already in the project.

The package ID must be unique on Google Play. If `com.mhdevelopers.notemate` is already registered by another publisher, update `applicationId` and `namespace` and the Java package before uploading.

## Android permissions and alarm behavior

- **Notifications:** Android 13+ asks users to allow notifications. If denied, the note still saves but cannot display alerts.
- **Precise alarms:** Android 12+ may require enabling **Alarms & reminders** for MH NoteMate in Android settings. Without this, reminders use inexact alarms and may be delayed by battery optimization.
- **Reboot:** Boot permission is used to restore scheduled reminders after the phone restarts.
- **Do Not Disturb / silent mode:** Device settings can suppress sound or delay notification delivery. This app does not bypass these controls.
- **No internet permission** is requested. Notes remain on the phone. Device backups are disabled in the manifest.

## Recommended manual QA before publishing

1. Add a note, close the app, reopen and verify the note persists.
2. Edit, pin, search, filter, and delete notes.
3. Add a task, complete it and verify the dashboard count changes.
4. Schedule a note 10 minutes from now, with a 5-minute early alert. Test both the advance notification and due-time alarm sound on a physical phone.
5. Verify daily/weekly repeats, notification permission denial, precise-alarm permission denial, reboot, and device time-zone change.
6. Test layout on small and large Android screens, including Android 15/16 edge-to-edge layouts.
7. Verify release signing, package ID, app content disclosures and Play Console policies before publication.

## Project structure

```
MHNoteMate/
  app/src/main/AndroidManifest.xml
  app/src/main/java/com/mhdevelopers/notemate/
    MainActivity.java       # Splash, dashboard, notes, tasks, calendar, settings, editor
    NotesDb.java           # SQLite persistence
    AlarmHelper.java       # Schedule and cancel alerts
    ReminderReceiver.java  # Alarm notification and recurrence
    BootReceiver.java      # Reschedule after reboot/clock change
  app/src/main/res/mipmap-*/ic_launcher.png
  design/
    MHNoteMate_Icon_Transparent_512.png
    MHNoteMate_PlayStore_512.png
    Reference_UI_4_Screens.png
```

## Important delivery note

This package contains a full editable Android Studio source project and PNG assets. The Android SDK/Gradle distribution is not installed in the file-generation environment, so no APK or AAB was compiled or tested here. Build, test, and sign the app using Android Studio before Play Store upload. The reference image is a design target; the app implements a close native Android interpretation, not pixel-for-pixel identical screenshots.
