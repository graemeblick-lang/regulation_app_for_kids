# Calm Corner

A simple, calm Android app with tools that help kids and young people regulate big feelings.

**Working name.** "Calm Corner" is a placeholder. Check the Play Store for clashes before you settle on a name.

## What's in the prototype

| Tool | What it does |
|---|---|
| 🟦 **Box Breathing** | A dot travels around a square: in, hold, out, hold. |
| 🖐️ **Finger Breathing** | A dot traces the outline of a hand: up a finger to breathe in, down to breathe out. |
| 🎈 **Balloon Breathing** | Belly breathing with a balloon that inflates and deflates. The child picks the balloon colour (remembered). The out-breath is longer than the in-breath. |
| 👀 **Five Senses** | Grounding. Tap a circle for each thing you see, touch, hear, smell and taste. |
| 💪 **Squeeze and Relax** | Muscle relaxation. A drawn body lights up the part to squeeze (hands, arms, shoulders, face, tummy, feet) and shows it tense, then relaxed. |

Other features:
- **Colour check-in** on the home screen: Blue (sad, tired), Green (calm, ready), Yellow (worried, wiggly), Red (very angry or scared). It suggests tools for that state. The app deliberately avoids trademarked programme names.
- **Speed selector** (🐇 Fast 3s / 🚶 Medium 4s / 🐢 Slow 5s). Younger children often find 4 seconds too long.
- **Gentle vibration** at each phase change, so a child can follow along with eyes closed.
- The screen stays on during an exercise. Light and dark themes.
- **No internet permission, no accounts, no ads, no tracking.** This makes the Play Store "Designed for Families" review much simpler.

## Tech stack

- **Kotlin + Jetpack Compose** (Google's modern native Android UI toolkit)
- Single activity, no backend, no third-party libraries beyond AndroidX
- minSdk 26 (Android 8.0+), targetSdk 36

## Get it on a phone

### Option A: download the APK straight onto the phone (no setup)
1. On the Android phone, open this link in Chrome (sign in to GitHub first if the repo is private):
   **https://github.com/graemeblick-lang/regulation_app_for_kids/releases/latest/download/CalmCorner.apk**
2. Open the downloaded file. Android asks you to allow "install unknown apps" for Chrome. Allow it and tap **Install**.
3. If Play Protect warns about an unknown developer, tap **More details → Install anyway**. This happens with every self-built test app.
4. If Play Protect blocks it with no "Install anyway" option: open the **Play Store** app → tap your profile picture → **Play Protect** → ⚙️ settings → turn off **Scan apps with Play Protect**. Install the app, then **turn it back on**.

> Builds made before 20 Nov 2026 (evening) were signed with a random key. If you have one of those installed, **uninstall it once** before installing a newer build. From then on, updates install over the top.

A new build is published automatically on every push (see the **Releases** section of the repo page).

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
