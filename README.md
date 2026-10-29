# Last 1% — LibGDX Endless Survival

**Last 1%** is a free, offline-first Android arcade survival game built with **LibGDX 1.13.1** and Java. Guide a glowing energy core, collect cyan energy, dodge red shards, and survive an endless stream of increasingly difficult hazards.

## Current LibGDX rebuild

- **Engine:** LibGDX Android backend with Java 17; no paid tools, assets, or services required.
- **Game presentation:** sci-fi arena grid, layered neon panels, glowing energy core, cyan collectibles, red shard hazards, particle bursts, score HUD, and arcade-style panels/buttons.
- **Screens:** base menu, active gameplay, pause overlay, core upgrade shop, and run-results screen.
- **Endless progression:** difficulty and spawn frequency increase over time; stage advances every 45 seconds. There is no fixed timer-based win screen.
- **Controls:** drag on the playfield to steer the core; tap pause to resume or end a run.
- **Progression:** energy pickups increase score; shield absorbs one collision per run; amplifier upgrades improve pickup score. Coins, best score, and amplifier level persist locally.
- **Privacy/offline:** gameplay uses no account, server, or network connection. The earlier AdMob test integration is removed from this LibGDX rebuild.
- **Assets:** all current visuals are drawn at runtime with LibGDX primitives, so the APK needs no external art downloads. Original sprite sheets, richer animation, and synthesized/recorded audio are future polish items.

## Build and verify

GitHub Actions runs `gradle --no-daemon assembleDebug`, verifies that the APK exists and is non-empty, prints its SHA-256, and uploads the `Last1Percent-debug-apk` artifact.

1. Open [GitHub Actions](https://github.com/kage049754/Newtest/actions).
2. Choose the latest **Android Debug APK** run that has completed successfully.
3. Download the `Last1Percent-debug-apk` artifact and extract the APK.

CI verifies compilation and artifact creation, not physical-device installation, gameplay balance, or device-specific performance.

## Technology

- Java 17
- LibGDX 1.13.1
- Android SDK / Gradle 8.9
- GitHub Actions

## Roadmap

- [x] Migrate rendering and game loop to LibGDX Android backend
- [x] Add arcade-style base, gameplay HUD, pause, shop, and results screens
- [x] Add endless hazards, pickups, collision, particles, and time-based difficulty
- [x] Persist coins, best score, and amplifier upgrades locally
- [ ] Physical Android play-test and tune controls/difficulty
- [ ] Add original sprite assets, richer enemy patterns, and animated transitions
- [ ] Add adjustable original music and sound effects
- [ ] Add unlockable core skins and stage-specific arenas
- [ ] Release signing, accessibility review, and store testing

This is an active prototype. Do not treat a successful CI build as proof of device testing or production readiness.
