# Silent Alarms roadmap

The app provides a buildable Android app, Material 3 Expressive alarm list and
add/edit UI, a Room alarm schema with persisted create/edit/delete/toggle flows,
exact AlarmManager scheduling (`setAlarmClock`) with Doze exemption and reboot
rescheduling (`BOOT_COMPLETED`), a Preferences DataStore entry point, and debug
APK delivery.

## Later sessions

- [x] Alarm creation/editing, stored-alarm list rendering, enable/disable, and
  deletion.
- [ ] Settings UI backed by DataStore.
- [x] Exact scheduling through `AlarmManager` with `USE_EXACT_ALARM`, including
  one-shot and repeating alarms, cancellation, and time/time-zone changes.
- [ ] A foreground service for ringing/vibration with a **5 minute auto-dismiss**,
  explicit dismissal, and appropriate notification/service permissions.
- [ ] A full-screen intent alarm screen, with lock-screen behavior and full-screen
  intent permission/eligibility handling.
- [ ] Built-in and custom vibration patterns using
  `VibrationEffect.createWaveform`, including pattern selection and preview.
- [ ] A built-in sound library with sound selection and preview.
- [ ] User sound import through `ACTION_OPEN_DOCUMENT`, with persistable URI read
  access and handling for unavailable or removed documents.
- [x] Reboot rescheduling of enabled alarms, including the required boot receiver.
- [ ] Battery optimisation guidance screens explaining relevant device settings
  without claiming to bypass platform restrictions.

Implement permissions and lifecycle handling with the feature that needs them,
not pre-emptively in the bootstrap.
