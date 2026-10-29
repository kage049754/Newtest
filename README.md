# AdQuest — Star Catch

AdQuest is a small, offline-playable Android casual game prototype. Catch falling stars, avoid bombs, and beat your best score. The project is intentionally separate from all other game repositories.

## Current prototype
- Native Android Java app (no paid engine or API required).
- 30-second rounds, touch controls, score, coins, and local best-score saving.
- Google Mobile Ads SDK configured with Google's **official test IDs** only.
- Rewarded-ad button is offered after a round; reward is granted only after the SDK reports a completed reward.
- No ad interrupts an active round. A small banner may appear on the menu/result screen.
- GitHub Actions builds a debug APK and uploads it as an artifact.

## Build
Push to GitHub or run **Actions → Android Debug APK → Run workflow**. Download the `AdQuest-debug-apk` artifact from a completed successful workflow run. A successful CI build verifies compilation, not installation or gameplay on a physical phone.

## AdMob setup before release
1. Create an AdMob account and register the Android app using package ID `com.kage049754.adquest`.
2. Create a rewarded ad unit and, if desired, a banner ad unit.
3. Replace the sample test App ID in `app/src/main/AndroidManifest.xml` and test ad unit IDs in `MainActivity.java` only when ready to test with your own app configuration.
4. Keep test ads enabled during development. Never click your own live ads.
5. Publish a privacy policy, complete Google's required consent/privacy flows for your audience and regions, and configure AdMob payment, identity, and tax details as Google requires.
6. Review Google Play's current policies and complete testing before production release.

The sample IDs in this repository are Google test IDs. They do not generate real earnings. AdMob approval, account setup, app distribution, eligible traffic, and policy compliance are required before monetization can pay out.

## Roadmap
- [x] Basic playable touch-based casual game
- [x] Save best score locally
- [x] Test-only rewarded ad integration
- [x] GitHub Actions APK build and artifact
- [ ] Install and test on a real Android phone
- [ ] Add sound, accessibility settings, and improved visual polish
- [ ] Add privacy/consent implementation appropriate to release regions
- [ ] Replace test IDs only after AdMob app/ad units are configured
- [ ] Release signing, store listing, policy review, and closed testing

## Tech
- Android Java, Android SDK, Gradle
- Google Mobile Ads SDK with test IDs
- GitHub Actions / Ubuntu runner

## License
Prototype code is provided for personal learning and development. Review dependency licenses before publishing.
