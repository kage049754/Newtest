# DroidFlow

DroidFlow is an offline-first Android routine and shortcut manager built with Kotlin, Jetpack Compose, and Material 3 dynamic color.

## Current features
- Material You interface with system dynamic colors on Android 12+ and a light fallback on older versions.
- Home dashboard and quick actions for Wi-Fi, Bluetooth, display, battery, and Android settings.
- Create, enable, disable, run, and delete personal flows.
- Six reusable routine templates for study, connectivity, battery, wind-down, Bluetooth, and time settings.
- Local persistence of flow definitions; no account or server required.
- Settings shortcuts for app permissions, notifications, battery, and system settings.
- Explicit user-triggered actions; no hidden device control or accessibility automation.

## Android limitations
DroidFlow opens relevant Android settings pages rather than silently changing protected system settings. Device version and manufacturer determine which settings pages are available. Scheduled reminders and multi-step background triggers are planned for later phases using supported Android APIs.

## Build
GitHub Actions builds a debug APK on every push to `main`, verifies that the APK exists and is non-empty, calculates its SHA-256, and uploads it as an artifact.

Open **Actions → DroidFlow Android APK → latest successful run → Artifacts** to download the APK.

Requirements: JDK 17, Gradle 8.9, Android SDK platform 35. No paid services or API keys are required.

## Roadmap
1. Core Material You dashboard, editable flows, templates, local persistence, and quick actions.
2. Add reliable reminder scheduling, notification controls, and persistent run history.
3. Add user-defined trigger conditions and reusable multi-step routines.
4. Add import/export, backup/restore, and automated UI tests.
5. Verify behavior on real Android devices and document OEM-specific limitations.

DroidFlow is a local utility, not a game. It does not use AccessibilityService to automate arbitrary third-party apps.
