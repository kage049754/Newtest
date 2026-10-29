# Last 1% — Endless Survival

**Last 1%** is a portrait neon-arcade survival game for Android. Move your energy core around a sci-fi arena, collect energy, dodge red shard hazards, and survive as long as you can. There is no fixed 45-second finish line: the run ends when a hazard hits you.

## Current gameplay
- Native Android Java game; the core game works offline.
- Drag anywhere in the arena to move the energy core.
- Endless survival run. Stage 1 begins immediately; every 60 seconds advances the stage number.
- Hazard speed and spawn frequency increase continuously with survival time. Difficulty has practical caps to avoid making the game impossible or overwhelming low-end phones.
- Cyan energy pickups add score. Red shard hazards end a run on contact.
- A shield upgrade blocks one hit per run; coin-boost upgrades increase energy score.
- Coins, best score, and upgrades save locally on the device.
- Run-end results show total survival time, score, and earned coins.
- Optional rewarded ad offers bonus coins after a run; no ad is shown during active gameplay.

## Visual design
- Dark sci-fi arena with a neon grid and border.
- Glowing energy core and cyan collectible orbs.
- Angular red shard hazards with glow.
- Custom gradient action buttons and a compact arcade HUD.

## Audio
- Original ambient synth music is generated in code, so the project does not depend on copyrighted music or external audio files.
- Short sound cues play for energy collection, shield activation, stage changes, and run-ending collisions.
- Audio can be muted from the in-game audio control. Music pauses when the app leaves the foreground.
- Audio is synthesized and intentionally lightweight; a future polish pass can replace or supplement it with professionally mixed original/licensed tracks and adjustable separate music/SFX volume controls.

## Progression and upgrades
- **Shield:** 50 coins; blocks one hit each run.
- **Coin boost:** levels cost 100, 200, and 350 coins; increases energy-orb score.
- **Run rewards:** coins are earned based on score and survival time.
- **Rewarded bonus:** bonus coins are granted only after the ad SDK confirms the reward callback.

## Build and download
GitHub Actions runs `gradle --no-daemon assembleDebug`, verifies that the APK exists and is non-empty, calculates its SHA-256 hash, and uploads the `Last1Percent-debug-apk` artifact. Open **Actions**, select the latest completed successful **Android Debug APK** run, and download the artifact. CI confirms compilation and artifact creation; it does not prove physical-device installation, performance, or audio quality.

## Roadmap
- [x] Native Android project and portrait touch controls
- [x] Endless survival with stage progression and time-based difficulty
- [x] Neon arena, glowing core, energy pickups, and shard hazards
- [x] Local score/coin persistence, shield, and coin-boost upgrades
- [x] Procedural ambient synth and gameplay sound cues
- [x] Rewarded-only AdMob test integration (no gameplay interruptions)
- [x] GitHub Actions build, APK verification, and artifact upload
- [ ] Install and play-test on a physical Android phone; tune difficulty from real play sessions
- [ ] Add pause menu, audio sliders, and more hazard patterns
- [ ] Add original visual effects, unlockable core skins, and stage-specific arena themes
- [ ] Add privacy/consent handling and privacy policy before public release
- [ ] Configure real AdMob IDs only after the AdMob app/ad units are ready
- [ ] Release signing, store listing, policy review, and closed testing

## Technology
- Java, Android SDK, Gradle 8.9, JDK 17
- Google Mobile Ads SDK (test IDs only)
- GitHub Actions on Ubuntu
- Android AudioTrack/ToneGenerator for synthesized audio

## Monetization and release notes
The repository uses Google's official AdMob test App ID and rewarded test unit ID only. Test ads do not generate revenue. Before release, configure your own AdMob app and ad units, implement privacy/consent requirements relevant to users and regions, publish a privacy policy, and follow current Google Play and AdMob policies. Never click your own live ads. Real monetization depends on AdMob approval, valid traffic, distribution, and policy compliance.

This is an active prototype. A successful CI build is not a claim of device testing or production readiness.
