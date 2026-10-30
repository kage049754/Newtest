# DroidFlow

Offline-first Android routine launcher built with Kotlin, Jetpack Compose and Material 3.

## Current features
- Material You dynamic colors on Android 12+ and fallback palette on older devices.
- Create, edit, search, enable/disable, duplicate, run and delete named flows.
- Multi-step routines with up to 20 supported actions, launched one at a time with user confirmation between steps.
- Reorder routine steps with up/down controls and remove individual steps before saving.
- Templates for focus, connectivity, battery, display, Bluetooth and time settings.
- Persistent local activity history (latest 100 events).
- Export flow definitions as JSON through Android's share sheet.
- No account, cloud service, analytics, ads or paid API.

## Android behavior and privacy
Flows are user-triggered. Android protects many system controls; DroidFlow opens the relevant settings page rather than silently changing protected settings. It does not use an Accessibility Service, notification listener, or background execution to bypass restrictions. Flow definitions and history stay in app-private local storage.

## Build
Run `gradle --no-daemon assembleDebug`. APK: `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions builds and uploads a debug APK on pushes to `main`, pull requests and manual dispatch. Artifacts are retained for 14 days.

## Roadmap
- [x] Offline flow storage and templates
- [x] Edit, duplicate, search, enable/disable and delete
- [x] Persistent activity history
- [x] JSON export
- [x] Validated JSON import and restore (legacy single-action backups remain supported)
- [x] Multi-step flows with supported action sequences
- [ ] Optional user-approved scheduled reminders
- [ ] Home-screen shortcuts/widgets
- [ ] Automated UI and repository tests

Scheduled triggers and background actions will be implemented only where Android permits them and with explicit user consent.
