# Last 1% — Survival Challenge

**Last 1%** is a fast, one-life survival arcade game for Android. Move your energy core left and right, dodge falling hazards, collect energy orbs, and survive as long as possible. Each run is short, retries are instant, and coins unlock future cosmetic and gameplay progression.

## Gameplay
- Portrait, touch-controlled survival arcade.
- One-hit run: collide with a red hazard and the run ends.
- Collect cyan energy orbs for score and coins.
- Survive for up to 45 seconds; the hazard spawn rate and speed increase over time.
- Save best score and coin balance locally on the device.
- Optional rewarded test ad after a run; no ad interrupts active gameplay.
- Google Mobile Ads uses official test IDs only until a real AdMob setup is deliberately configured.

## Build and download
GitHub Actions runs `gradle assembleDebug`, checks that the APK exists and is non-empty, calculates its SHA-256 hash, and uploads the `Last1Percent-debug-apk` artifact. Open **Actions**, choose the latest completed successful **Android Debug APK** run, then download the artifact. CI confirms compilation and artifact creation, not real-phone gameplay.

## Roadmap
- [x] Native Android prototype and touch movement
- [x] One-hit survival, hazards, collectible orbs, escalating difficulty
- [x] Local best-score and coin persistence
- [x] Test-only rewarded ad integration
- [x] GitHub Actions APK verification and artifact upload
- [ ] Install and play-test on a physical Android phone
- [ ] Add sound, haptics, pause/retry polish, and accessible settings
- [ ] Add unlockable skins and balanced progression
- [ ] Add region-appropriate privacy/consent flow before release
- [ ] Configure real AdMob app/ad units only after account setup
- [ ] Release signing, store listing, policy review, and closed testing

## Technology
- Native Android Java, Android SDK, Gradle
- Google Mobile Ads SDK (test IDs only)
- GitHub Actions

## AdMob and release notes
The app currently uses Google's sample App ID and test ad unit IDs. Test ads do not earn revenue. Before release, configure your own AdMob app and ad units, implement required privacy/consent handling, publish a privacy policy, and follow current Google Play and AdMob policies. Never click your own live ads.

This is an early prototype, not a claim of a production-ready or device-tested release.
