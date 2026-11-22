@echo off
rem PC TuneUp launcher - double-click me. Keep this file next to PCTuneUp.ps1.
if not exist "%~dp0PCTuneUp.ps1" (
  echo PCTuneUp.ps1 must be in the same folder as this file.
  pause
  exit /b 1
)
start "" powershell.exe -NoProfile -ExecutionPolicy Bypass -STA -WindowStyle Hidden -File "%~dp0PCTuneUp.ps1"
