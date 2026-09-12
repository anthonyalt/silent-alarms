# Silent Alarms

## Stack decisions

- Native Android app in Kotlin; package and application ID: `nz.silentalarms.app`.
- Minimum SDK 26, compile SDK 36, target SDK 36.
- Jetpack Compose UI using Material 3 Expressive (`MaterialExpressiveTheme`).
  Material 3 is explicitly pinned to `1.5.0-alpha12` because stable `1.4.0`
  excludes Expressive APIs. Keep experimental opt-ins local to the code using them.
- Gradle Kotlin DSL, checked-in Gradle 8.13 wrapper, Android Gradle Plugin 8.13.2,
  JDK 17, and Kotlin/Compose compiler plugin 2.2.21.
- Keep dependency and plugin versions in `gradle/libs.versions.toml`. Align Compose
  libraries with its BOM, with the documented Material 3 Expressive override.
- Room 2.8.4 stores alarms, with KSP code generation and exported schemas in
  `app/schemas`. Commit schema changes and write migrations when changing the
  schema; do not use destructive migration fallbacks.
- Preferences DataStore 1.2.0 stores settings. Reuse the single top-level
  `Context.settingsDataStore` delegate instead of creating competing stores.

## Data conventions

`Alarm` contains `id`, `label`, `hour`, `minute`, `daysOfWeek`,
`vibrationPatternId`, nullable `soundUri`, `isEnabled`, and `createdAt`.
IDs are auto-generated `Long`s. Time uses a local 24-hour hour/minute pair.
`daysOfWeek` is a `Set<DayOfWeek>` stored as a seven-bit integer, with Monday at
bit 0 and Sunday at bit 6; an empty set represents a non-repeating alarm.
`createdAt` is Unix epoch milliseconds. Vibration pattern IDs and sound URI
strings are references for future features, not implementations of playback.
Use Room suspend functions and flows; do not allow main-thread database queries.

## Scope and conventions

The app supports local alarm creation/editing, stored-alarm list rendering,
enable/disable toggling, and swipe-to-delete with undo. Room and DataStore are
available through `SilentAlarmsApplication`; alarm UI state is owned by a
ViewModel exposed as `StateFlow`. Enabled alarms use `AlarmManager.setAlarmClock`,
reschedule after reboot, and fire through a `specialUse` foreground service with
a full-screen lock-screen activity, repeating vibration, nine-minute snooze, and
five-minute auto-dismiss. Settings UI and sound playback are not implemented
yet. Do not add other roadmap features unless the current task explicitly
requests them. See `docs/ROADMAP.md` for later sessions.

Use string resources for user-visible text, accessible button labels, Compose
edge-to-edge insets, and light/dark themes. Keep changes scoped and avoid adding
frameworks or dependencies without a concrete need.

## Build and delivery

Use JDK 17 and an Android SDK with platform 36 and build-tools 35.0.0.
Set `ANDROID_HOME` or an untracked `local.properties` containing `sdk.dir`.
Run `./gradlew assembleDebug` before opening a PR; fix failures and retry.
Run `./gradlew testDebugUnitTest` for storage-model/converter changes.
The debug APK is `app/build/outputs/apk/debug/app-debug.apk`.

`.github/workflows/build.yml` builds and tests pushes to `main`, uploads the APK,
and publishes it as a debug prerelease. These APKs are development builds, not
production-signed releases.

## Testing limitations

Robolectric and Compose UI tests run under `./gradlew testDebugUnitTest` without
an emulator. Pull requests also run `connectedDebugAndroidTest` on an API 36
x86_64 emulator. Emulators do not provide haptic hardware, so vibration and
haptic behavior cannot be verified in CI. Manually verify alarm vibration,
including behavior while the phone is silent, on a physical Android device.
