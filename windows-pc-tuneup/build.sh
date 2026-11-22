#!/usr/bin/env bash
# Builds dist/PCTuneUp.exe on Linux/macOS with Mono (apt-get install mono-mcs mono-devel).
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p dist
mcs -nologo -optimize+ -target:winexe -platform:anycpu -sdk:4.5 \
    -out:dist/PCTuneUp.exe -win32icon:launcher/icon.ico \
    -resource:PCTuneUp.ps1,PCTuneUp.ps1 -r:System.Windows.Forms.dll launcher/Launcher.cs
echo "Built dist/PCTuneUp.exe"
