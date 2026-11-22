# Builds dist\PCTuneUp.exe on Windows using the C# compiler that ships with Windows (.NET Framework 4.x).
# Usage:  powershell -ExecutionPolicy Bypass -File .\build.ps1
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot

# 1. Check the PowerShell script for syntax errors
$tokens = $null; $errors = $null
[System.Management.Automation.Language.Parser]::ParseFile((Join-Path $root 'PCTuneUp.ps1'), [ref]$tokens, [ref]$errors) | Out-Null
if ($errors.Count -gt 0) { $errors | ForEach-Object { Write-Host "$($_.Extent.StartLineNumber): $($_.Message)" -ForegroundColor Red }; throw 'PCTuneUp.ps1 has syntax errors.' }
Write-Host 'PCTuneUp.ps1: no syntax errors.' -ForegroundColor Green

# 2. Compile the launcher with the script embedded
$csc = Join-Path $env:WINDIR 'Microsoft.NET\Framework64\v4.0.30319\csc.exe'
if (-not (Test-Path $csc)) { $csc = Join-Path $env:WINDIR 'Microsoft.NET\Framework\v4.0.30319\csc.exe' }
New-Item -ItemType Directory -Force -Path (Join-Path $root 'dist') | Out-Null
& $csc /nologo /optimize+ /target:winexe /platform:anycpu `
    "/out:$root\dist\PCTuneUp.exe" "/win32icon:$root\launcher\icon.ico" "/win32manifest:$root\launcher\app.manifest" `
    "/resource:$root\PCTuneUp.ps1,PCTuneUp.ps1" /r:System.Windows.Forms.dll "$root\launcher\Launcher.cs"
if ($LASTEXITCODE -ne 0) { throw 'Compile failed.' }
Write-Host "Built $root\dist\PCTuneUp.exe" -ForegroundColor Green
