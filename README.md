# Calm Corner

A simple, calm Android app with tools that help kids and young people regulate big feelings.

**Working name.** "Calm Corner" is a placeholder. Check the Play Store for clashes before you settle on a name.

## What's in the prototype

| Tool | What it does |
|---|---|
| 🟦 **Box Breathing** | A dot travels around a square: in, hold, out, hold. |
| 🖐️ **Five Finger Breathing** | A drawn hand. The child traces up each finger breathing in and down breathing out, following the dot. |
| 🎈 **Balloon Breathing** | Belly breathing with a balloon that inflates and deflates. The out-breath is longer than the in-breath. |
| 👀 **5-4-3-2-1 Senses** | Grounding. Tap a circle for each thing you see, touch, hear, smell and taste. |
| 💪 **Squeeze & Relax** | Muscle relaxation for kids: fists like squeezing a lemon, turtle shoulders, elephant tummy and more. |

Other features:
- **Feelings check-in** on the home screen (Angry, Worried, Sad, Too much, Wiggly, Calm). It suggests the tools that suit that feeling.
- **Speed selector** (Quick 3s / Steady 4s / Slow 5s). Younger children often find 4 seconds too long.
- **Gentle vibration** at each phase change, so a child can follow along with eyes closed.
- The screen stays on during an exercise. Light and dark themes.
- **No internet permission, no accounts, no ads, no tracking.** This makes the Play Store "Designed for Families" review much simpler.

## Tech stack

- **Kotlin + Jetpack Compose** (Google's modern native Android UI toolkit)
- Single activity, no backend, no third-party libraries beyond AndroidX
- minSdk 26 (Android 8.0+), targetSdk 36

## Get it on a phone

### Option A: download the APK from GitHub (no setup)
1. Every push builds the app in the **Actions** tab of this GitHub repo (workflow "Build APK").
2. Open the latest green run and download the **calm-corner-debug-apk** artifact (a zip file).
3. Unzip it and copy `app-debug.apk` to the Android phone (Google Drive, email or a USB cable).
4. Open it on the phone. Android asks you to allow "install unknown apps" for that source. Allow it and install.

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
