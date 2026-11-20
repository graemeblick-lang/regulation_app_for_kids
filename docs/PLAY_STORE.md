# Publishing to the app stores

## 1. Before you publish
- [ ] **Test with your daughter**, and if you can with a few other families. Watch where they get stuck and ask which tools they actually reach for.
- [ ] **Pick the final name** and check the Play Store for clashes.
- [ ] **Change the `applicationId`** in `app/build.gradle.kts` from `com.calmcorner.app` to something you own, e.g. `com.yourname.calmcorner`. You can never change it after the first upload.
- [ ] **Get a proper app icon.** The prototype has a simple vector placeholder.
- [ ] Consider having a child psychologist, OT or school counsellor review the wording. This also helps your store listing.

## 2. Google Play
1. **Create a developer account** at <https://play.google.com/console>. There is a one-off $25 fee and ID verification.
   - With a *personal* account, Google requires a **closed test with at least 12 testers for 14 days** before you can publish to production. Line up friends and family early.
2. **Create an upload key** (Android Studio → Build → Generate Signed App Bundle). Back up the keystore file and password somewhere safe. Never commit them to git (`.gitignore` already blocks `*.jks`).
3. **Build a release bundle**: `./gradlew bundleRelease` gives you `app/build/outputs/bundle/release/app-release.aab`.
4. In Play Console, fill in:
   - **Target audience**: include under-13 age groups. This opts you into the **Families Policy**, which this app is built to meet: no ads, no data collection, no network access.
   - **Data safety form**: "No data collected" and "No data shared".
   - **Privacy policy URL**: required for every app, especially kids' apps. A starting draft is in [PRIVACY_POLICY.md](PRIVACY_POLICY.md). Host it somewhere public, e.g. GitHub Pages.
   - **Content rating questionnaire**: should come out as "Everyone".
   - **Store listing**: short and full descriptions, at least 2 phone screenshots, a 512×512 icon and a 1024×500 feature graphic.
   - **Health claims**: describe it as "calming and self-regulation techniques". Don't claim it treats anxiety, ADHD or other conditions. Medical claims trigger extra review.
5. Upload to **Internal testing**, then **Closed testing** (12 testers, 14 days), then **Production**.

## 3. Apple App Store (later)
This prototype is Android-only (Kotlin). For iPhone you would either:
- **Port to Compose Multiplatform or Flutter**, so one codebase builds for both platforms. The app is small (around 1,000 lines), so a port is quick. Or
- Write a native SwiftUI version.

Either way you need a Mac with Xcode and an Apple Developer account ($99 a year), and the app goes in the "Kids" category. Apple's Kids rules are similar: no third-party analytics or ads.

## 4. Ideas for after the prototype
- A customisable **"my calm plan"**: the child picks their favourite tools, and a grown-up adds personal coping ideas.
- Optional soft **sounds or voice guidance** ("breathe in…") for younger kids or kids who find reading hard.
- A **feelings thermometer** (1–5) before and after, to show the child it helped (stored only on the device).
- More tools: "hot chocolate breathing", star breathing, butterfly hug, a calm-down countdown, positive self-talk cards.
- A home-screen **widget** for one-tap box breathing.
- Accessibility: larger text options, a reduced-motion mode, TalkBack testing.
