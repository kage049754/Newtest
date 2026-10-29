# Last 1% — Survival Challenge

**Last 1%** is a portrait survival arcade game for Android. Drag your energy core around the arena, dodge falling red hazards, collect cyan energy, and see how long you can survive. Each run is quick, the hazards get faster, and coins carry over between runs.

## Current game
- Native Android Java; core gameplay works offline.
- Drag anywhere in the play area to move the core.
- Red hazards end a run on contact. A permanent shield upgrade blocks one hit per run.
- Cyan energy orbs add score; the coin boost upgrade increases their value.
- Survive up to 45 seconds while spawn rate and hazard speed increase.
- Best score, coins, and upgrades save locally on the device.
- Optional rewarded ad offers bonus coins after a run. No ad is shown during active gameplay.
- Uses Google's official AdMob test App ID and rewarded test unit ID only.

## Progression
- **Shield:** 50 coins; blocks one hit each run.
- **Coin boost:** levels cost 100, 200, and 350 coins; increases energy-orb score.
- **Run rewards:** coins are earned based on score and survival time.
- **Rewarded bonus:** bonus coins are granted only after the ad SDK confirms the reward callback.

## Build and download
GitHub Actions runs gradle assembleDebug, checks that the APK exists and is non-empty, calculates its SHA-256 hash, and uploads the Last1Percent-debug-apk artifact. Open **Actions**, select the latest completed successful **Android Debug APK** run, and download the artifact. CI confirms compilation and artifact creation; it does not prove physical-device installation or gameplay.

## Roadmap
- [x] Native Android project and portrait touch controls
- [x] Survival loop, collision detection, collectible energy, escalating difficulty
- [x] Local score/coin persistence
- [x] Shield and coin-boost upgrades
- [x] Rewarded-only AdMob test integration (no gameplay interruptions)
- [x] GitHub Actions build, APK verification, and artifact upload
- [ ] Install and play-test on a physical Android phone
- [ ] Add sound, optional haptics, and settings
- [ ] Add cosmetic unlocks and additional hazard patterns
- [ ] Add privacy/consent handling and privacy policy before public release
- [ ] Configure real AdMob IDs only after the AdMob app/ad units are ready
- [ ] Release signing, store listing, policy review, and closed testing

## Technology
- Java, Android SDK, Gradle 8.9, JDK 17
- Google Mobile Ads SDK (test IDs only)
- GitHub Actions on Ubuntu

## Monetization and release notes
Test ads do not generate revenue. Before release, configure your own AdMob app and ad units, implement the privacy/consent requirements relevant to your users and regions, publish a privacy policy, and follow current Google Play and AdMob policies. Never click your own live ads. Real monetization depends on AdMob approval, valid traffic, distribution, and policy compliance.

This is an active prototype. A successful CI build is not a claim of device testing or production readiness.
