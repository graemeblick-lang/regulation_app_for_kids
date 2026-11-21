# Calm Corner

A simple, calm Android app with tools that help kids and young people regulate big feelings.

**Working name.** "Calm Corner" is a placeholder. Check the Play Store for clashes before you settle on a name.

## What's in the app

**Check-in loop:** pick a colour (Blue, Green, Yellow or Red), get suggested tools, do one, then answer **"How do you feel now?"**. Every finished tool earns a ⭐ for effort. If she isn't calm yet, it suggests another tool (and on Red, talking to a grown-up).

| Tool | What it does |
|---|---|
| 🟦 **Box Breathing** | A dot travels around a square: in, hold, out, hold. |
| 🖐️ **Finger Breathing** | A dot traces the outline of a hand: up a finger to breathe in, down to breathe out. |
| 🎈 **Balloon Breathing** | A balloon inflates and deflates. She picks its colour. The out-breath is longer than the in-breath. |
| 👀 **Five Senses** | Grounding. Tap a circle for each thing you see, touch, hear, smell and taste. |
| 💪 **Squeeze and Relax** | A drawn body lights up each part to squeeze, then relax. The face step zooms in on a big face to copy. |
| ✨ **Calm Jar** | Shake the phone (or tap Shake) and watch glitter slowly settle. |
| 🔵 **Bubble Pop** | A grid of bubbles to pop slowly, like a pop-it toy. |
| 🥁 **Calm Drum** | One big drum for one hand. It picks up her own steady beat, joins in at her speed, slowly slows down, then ends with breathing in time with the drum. No score; always ends slow. |
| 💓 **Heartbeat Drum** | A slow, soft "lub-dub" drum to breathe with (in for 2 beats, out for 4). Good at bedtime. |
| 🙌 **Body Moves** | Calming "heavy work": wall push, hand press, chair push, big hug and big stretch, 5 times each, with a drawn figure to copy. |
| ♾️ **Trace and Breathe** | Trace an infinity loop or a star with a finger, following a dot at breathing pace. The dot glows when her finger is on it. |
| 🌧️ **Calm Sounds** | Rain, waves or a soft hum made live by the app (no recordings), for 5, 10 or 20 minutes, fading out at the end. |
| ⏳ **Calm Timer** | 1, 2 or 5 minutes of quiet rest with a slowly pulsing circle. |

**My calm plan:** she stars ☆ her favourite tools, and a grown-up adds her own calm ideas with a photo (e.g. "Cuddle the dog"). These show first on the home screen. A **calm picture** (her safe place, pet or person) appears when she picks Red.

**Body clues:** "How does Red feel in my body?" shows a figure with the body areas lit up, plus simple words (e.g. "My face feels hot").

**Also:**
- **Voice guidance** (phone's built-in offline text-to-speech) and a soft **pop sound** in Bubble Pop. One 🔊/🔇 button on every exercise silences all sound, and her choice is remembered (e.g. quiet on the bus).
- Gentle **vibration** at each step (can be turned off).
- **Remembers** her breathing speed for each tool, her balloon colour and her timer length.
- **Breaths per session** (3, 5, 10 or keep going), ending with "All done ⭐".
- **Home-screen widget** ("Calm now"): one tap opens her most-used tool.
- **Grown-ups area**: press and hold ⚙️ on the home screen for 3 seconds. It shows this week's summary (exercises, how often she felt calmer afterwards, most helpful tool) and recent history with before/after colours. Also: **share a weekly summary PDF** (for school or a therapist), **school mode** (silent, vibration only, hides Body Moves and Calm Sounds), **daily reminders** (before school, after school, bedtime), **reduce movement**, and **bigger text and buttons**. Settings: **choice of voice** (10 voices: friendly woman or man in British, American and Australian accents, Indian woman, girl, boy, deep and calm; each with a preview), sound, vibration, breaths per session, Bubble Pop movement (still, calm or lively), Calm Drum ending speed and length, theme (match phone, light or dark), show or hide tools, clear history.
- **No internet permission, no accounts, no ads, no tracking.** All history stays on the phone.

## Wording rules

- Short, simple words. Many users find reading hard, especially when upset.
- **Literal instructions only, with no metaphors** (e.g. "Breathe in slowly. Your tummy gets big.", not "Fill your tummy like a balloon"). Kids who take words literally can follow them.
- An emoji or picture next to key words, as a reading aid.

## Tech stack

- **Kotlin + Jetpack Compose** (Google's modern native Android UI toolkit)
- Single activity, no backend, no third-party libraries beyond AndroidX
- minSdk 26 (Android 8.0+), targetSdk 36

## Get it on a phone

### Option A: download the APK straight onto the phone (no setup)
1. On the Android phone, open this link in Chrome (sign in to GitHub first if the repo is private):
   **https://github.com/graemeblick-lang/regulation_app_for_kids/releases/latest/download/CalmCorner.apk**
   (always the newest test build: https://github.com/graemeblick-lang/regulation_app_for_kids/releases/download/latest-build/CalmCorner.apk)
2. Open the downloaded file. Android asks you to allow "install unknown apps" for Chrome. Allow it and tap **Install**.
3. If Play Protect warns about an unknown developer, tap **More details → Install anyway**. This happens with every self-built test app.
4. If Play Protect blocks it with no "Install anyway" option: open the **Play Store** app → tap your profile picture → **Play Protect** → ⚙️ settings → turn off **Scan apps with Play Protect**. Install the app, then **turn it back on**.

> Builds made before 20 Nov 2026 (evening) were signed with a random key. If you have one of those installed, **uninstall it once** before installing a newer build. From then on, updates install over the top.

A new build is published automatically on every push (see the **Releases** section of the repo page).

**Numbered versions:** each tagged version (e.g. **v0.3.0**) also has its own permanent release with release notes. If a newer build has a problem, install an older numbered version from the Releases page.

## Branches and releases
- `main` is the stable version. New work happens on a separate branch and is merged into `main` with a pull request.
- To publish a numbered version: update `versionCode`/`versionName` in `app/build.gradle.kts`, add `release-notes/vX.Y.Z.md`, then create the release on GitHub (Releases → Draft a new release → new tag `vX.Y.Z` on `main`). **Untick "Set as the latest release"** so the usual download link keeps giving the newest test build. The build attaches the APK automatically.

### Running the tests
`./gradlew testDebugUnitTest` (also runs on every push).

### Option B: Android Studio (to make changes)
1. Install [Android Studio](https://developer.android.com/studio).
2. **File → Open** this folder and wait for Gradle sync to finish.
3. Plug in the phone with USB debugging on, or start an emulator, then press ▶ Run.

## Project layout

```
app/src/main/java/com/calmcorner/app/
├── MainActivity.kt
├── data/Tools.kt                 # tool list + feelings → suggestions
└── ui/
    ├── CalmApp.kt                # simple navigation between screens
    ├── theme/Theme.kt            # colours & text styles
    ├── components/
    │   ├── PhaseTimer.kt         # shared timer for all breathing/squeeze exercises
    │   └── Common.kt             # scaffold, buttons, speed picker, haptics
    └── screens/                  # one file per tool
```

To add a new tool: add an entry to `Tool` in `data/Tools.kt`, create a screen in `ui/screens/`, and add one line in `CalmApp.kt`.

See [docs/PLAY_STORE.md](docs/PLAY_STORE.md) for the route to publishing.
