# PC TuneUp: offline fan and performance checker for Windows

A **portable** Windows tool. Nothing to install, and it runs from any folder or USB stick. It works **offline**: it contains no network code and never connects to the internet. It works out **why your fan is loud**, checks your PC's overall performance, explains each problem in plain English and can apply the fixes it recommends.

## How to use

1. Get **`PCTuneUp.exe`** from this repository's Releases page (or build it, see below). Put it anywhere: Desktop, USB stick, etc. It is the only file you need.
2. Double-click **`PCTuneUp.exe`**. Click **Yes** when Windows asks for administrator permission. If you click **No**, the tool still runs, but only scans (it can't apply tune-ups).
   - The first time, Windows SmartScreen may say *"Windows protected your PC"* because the file isn't signed by a known publisher. Click **More info** > **Run anyway**.
   - Without the `.exe`: put `Start PC TuneUp.cmd` and `PCTuneUp.ps1` in the same folder and double-click the `.cmd` file. It works the same way.
3. For the best diagnosis, close your usual programs, wait a minute, then scan **while the fan is loud**.
4. **Tab 1 (Diagnosis):** click **Scan my PC**. It takes about 15–30 seconds. You get:
   - **"Why is the fan loud?"**: the likely causes, most important first
   - a **health score** out of 100
   - a list of findings, each with a recommendation (click a finding to see more)
5. **Tab 2 (Processes):** what is using the processor, graphics and memory, with a note on what each program is. You can end a program or lower its priority. Windows system processes are protected.
6. **Tab 3 (Startup programs):** turn off programs that start automatically when you sign in. This uses the same switch as Task Manager, so you can turn them back on at any time.
7. **Tab 4 (Tune-Up):** the fixes recommended by the scan are already ticked. Check them, then click **Apply ticked tune-ups**. **Undo my tune-ups** puts the settings back.
8. **Save HTML report** saves a full report you can open in any browser, offline.

## What it checks

| Area | What it looks at |
|---|---|
| Processor | Average load over 5 seconds, which programs use it, and turbo boost while the PC is barely busy |
| Graphics | GPU load for each program (Windows 10 1709 and later) |
| Cooling | Temperature sensors (if your PC exposes them), and **thermal throttling events** in the Windows event log (the PC slowing itself down because it's too hot) |
| Power | Power plan (High performance or Ultimate make more heat), and the minimum and maximum processor state |
| Memory | RAM in use, and how much is installed |
| Storage | Free space, SSD or hard drive, drive health, disk errors, how busy the disk is, temporary files |
| Startup | Programs that start automatically, with advice on which ones are safe to turn off |
| Windows | Days since the last restart, restarts waiting for updates, unexpected shutdowns, hardware (WHEA) errors |
| Security | Busy programs running from Temp or Downloads folders (a common sign of crypto-mining malware), Defender status |
| Battery | Battery wear and whether it's charging (laptops) |

The scan only **reads** information. Nothing changes until you apply a tune-up.

## Tune-ups

| Tune-up | Effect | Can it be undone? |
|---|---|---|
| Create a System Restore point | Safety snapshot of Windows, made first | n/a |
| Switch to the Balanced power plan | The processor slows down when idle, so it runs cooler and quieter | Yes |
| **Cooler & quieter CPU (cap at 99%)** | Turns off turbo boost and lets the processor idle down to 5%. This usually makes the **biggest difference to fan noise**. Heavy tasks run about 10–25% slower | Yes |
| Clean temporary files | Deletes temporary files older than 24 hours (skips files in use and never follows links) | No (safe) |
| Empty the Recycle Bin | Frees disk space | No |
| Optimise drives | TRIM for SSDs, defragmentation for hard drives | n/a |
| Clean up old Windows Update files | Runs DISM `StartComponentCleanup`, which works offline | No (safe) |
| Check and repair system files | Runs `sfc /scannow` | n/a |
| Microsoft Defender quick scan | Scans with the virus definitions already on the PC | n/a |

The longer tasks (the last four) run in a separate window so you can watch their progress.

## Limitations

- **Software can't set the fan speed directly.** On almost every PC the fan is controlled by the BIOS/firmware. Windows has no standard way for a program to change it. This tool makes the fan quieter by **reducing heat**: less processor load, sensible power settings and less background work. It also tells you where your PC maker's fan settings are (Dell, Lenovo, HP, ASUS, Acer, MSI and others).
- **Temperature readings** are only available if your PC exposes a sensor to Windows. Many don't, and on some PCs the sensor measures the motherboard rather than the processor. When there's no sensor, the tool relies on load, throttling events and power settings instead.
- **Dust can't be found by software.** If no software cause is found, the tool tells you so. A loud fan with low load usually means dust, blocked vents, a worn fan, or dried thermal paste on an older PC.
- Antivirus definitions can't be updated, because the tool works offline.

## Where it saves things

In a `PCTuneUp-Data` folder next to `PCTuneUp.exe` (or next to the script), so everything stays on the USB stick. If that folder can't be written to, it uses `%LOCALAPPDATA%\PCTuneUp` instead. That folder holds:

- `PC-Report-*.html`: saved reports
- `tuneup-log.txt`: a log of every change made
- `undo.json`: the settings needed to undo tune-ups

## Requirements

Windows 10 or 11. It uses Windows PowerShell 5.1, which is built into Windows, so there is nothing to install.

## How the .exe works

`PCTuneUp.exe` is a small launcher (about 130 KB) with `PCTuneUp.ps1` built into it. When you start it, it copies the script to `%LOCALAPPDATA%\PCTuneUp\app` and runs it with the Windows PowerShell that comes with Windows. It uses the .NET Framework 4.x already included in Windows 10/11, so there is nothing to install, and it makes no network connections. The launcher source is in `launcher/Launcher.cs`.

## Building the .exe

- **On Windows:** `powershell -ExecutionPolicy Bypass -File .\build.ps1`. This checks the script for syntax errors, then compiles with the C# compiler built into Windows. The result is `dist\PCTuneUp.exe`.
- **On Linux/macOS:** install Mono (`mono-mcs`, `mono-devel`) and run `./build.sh`.
- **Automatically:** GitHub Actions (`.github/workflows/build.yml`) builds the `.exe` on every push and attaches it to the run as an artifact. Pushing a tag such as `v1.0.0` publishes a Release with `PCTuneUp.exe` attached.

## Repository layout

| Path | What it is |
|---|---|
| `PCTuneUp.ps1` | The whole application (PowerShell + Windows Forms) |
| `Start PC TuneUp.cmd` | Launcher to use when you don't have the `.exe` |
| `launcher/` | `.exe` launcher source, icon and manifest |
| `build.ps1`, `build.sh` | Build scripts |
