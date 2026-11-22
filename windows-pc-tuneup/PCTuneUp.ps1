<#
    PC TuneUp - portable, offline Windows fan & performance doctor
    ----------------------------------------------------------------
    * Portable: no installation. Run "Start PC TuneUp.cmd" from any folder or USB stick.
    * Offline:  this script contains NO network code. It never downloads, uploads or
                "phones home". Everything is read from your own PC.
    * Safe:     scanning only reads information. Nothing is changed until you tick a
                tune-up and press Apply. Settings changes are recorded so they can be undone.

    Requires Windows 10 or 11 with Windows PowerShell 5.1 (built in).
#>
param(
    [switch]$Limited,    # do not ask for administrator rights
    [string]$DataRoot    # folder to keep reports/undo data in (the .exe passes its own folder)
)

Set-StrictMode -Off
$ErrorActionPreference = 'Continue'

$script:IsAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
$script:SelfPath = if ($PSCommandPath) { $PSCommandPath } else { $MyInvocation.MyCommand.Path }

# Ask Windows for administrator rights (needed for tune-ups and some checks).
# If the user clicks "No", carry on in limited (scan-only) mode.
if (-not $script:IsAdmin -and -not $Limited -and $script:SelfPath) {
    $elevArgs = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-STA', '-File', "`"$script:SelfPath`"")
    if ($DataRoot) { $elevArgs += @('-DataRoot', "`"$DataRoot`"") }
    try {
        Start-Process -FilePath 'powershell.exe' -Verb RunAs -WindowStyle Hidden -ErrorAction Stop `
            -ArgumentList $elevArgs
        exit
    } catch { }
}

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
[System.Windows.Forms.Application]::EnableVisualStyles()

# Show any unexpected start-up error in a message box (the console window is hidden)
trap {
    [System.Windows.Forms.MessageBox]::Show("PC TuneUp hit an unexpected error:`n`n$($_.Exception.Message)`n`n$($_.InvocationInfo.PositionMessage)", 'PC TuneUp', 'OK', 'Error') | Out-Null
    exit 1
}

$script:AppName = 'PC TuneUp'
$script:Version = '1.0.0'
$script:Root    = if ($script:SelfPath) { Split-Path -Parent $script:SelfPath } else { [AppDomain]::CurrentDomain.BaseDirectory }   # (BaseDirectory: when wrapped as .exe)

# Known Windows power plan GUIDs
$script:GuidBalanced = '381b4222-f694-41f0-9685-ff5bb260df2e'
$script:GuidHighPerf = '8c5e7fda-e8bf-4a96-9a85-a6e23a8c635c'
$script:GuidSaver    = 'a1841308-3541-4fab-bc81-f71556f20b4a'
$script:GuidUltimate = 'e9a42b02-d5df-448d-aa00-03f14749eb61'

# ---------------------------------------------------------------------------
#  Data folder (next to the tool so it stays portable; falls back to LocalAppData
#  if the tool is on read-only media)
# ---------------------------------------------------------------------------
function Get-DataFolder {
    $base = if ($DataRoot -and (Test-Path -LiteralPath $DataRoot)) { $DataRoot } else { $script:Root }
    $d = Join-Path $base 'PCTuneUp-Data'
    try {
        if (-not (Test-Path -LiteralPath $d)) { New-Item -ItemType Directory -Path $d -Force -ErrorAction Stop | Out-Null }
        $probe = Join-Path $d '.write-test'
        [IO.File]::WriteAllText($probe, 'x')
        Remove-Item -LiteralPath $probe -Force
        return $d
    } catch { }
    $d = Join-Path $env:LOCALAPPDATA 'PCTuneUp'
    if (-not (Test-Path -LiteralPath $d)) { New-Item -ItemType Directory -Path $d -Force | Out-Null }
    return $d
}
$script:DataDir  = Get-DataFolder
$script:UndoPath = Join-Path $script:DataDir 'undo.json'
$script:LogPath  = Join-Path $script:DataDir 'tuneup-log.txt'

# ---------------------------------------------------------------------------
#  Small helpers
# ---------------------------------------------------------------------------
function Wait-UI([int]$Milliseconds) {
    $sw = [Diagnostics.Stopwatch]::StartNew()
    while ($sw.ElapsedMilliseconds -lt $Milliseconds) {
        [System.Windows.Forms.Application]::DoEvents()
        Start-Sleep -Milliseconds 40
    }
}

function Format-Bytes([double]$Bytes) {
    if ($Bytes -ge 1TB) { return ('{0:N1} TB' -f ($Bytes / 1TB)) }
    if ($Bytes -ge 1GB) { return ('{0:N1} GB' -f ($Bytes / 1GB)) }
    if ($Bytes -ge 1MB) { return ('{0:N0} MB' -f ($Bytes / 1MB)) }
    if ($Bytes -ge 1KB) { return ('{0:N0} KB' -f ($Bytes / 1KB)) }
    return ('{0:N0} B' -f $Bytes)
}

function HtmlEnc([string]$s) { return [System.Net.WebUtility]::HtmlEncode($s) }

function Write-TuneLog([string]$Text) {
    $line = '{0:yyyy-MM-dd HH:mm:ss}  {1}' -f (Get-Date), $Text
    try { Add-Content -LiteralPath $script:LogPath -Value $line -ErrorAction Stop } catch { }
    if ($script:TuneLogBox) {
        $script:TuneLogBox.AppendText($line + [Environment]::NewLine)
        [System.Windows.Forms.Application]::DoEvents()
    }
}

function Read-Undo {
    $u = @{ PowerScheme = $null; CpuCap = $null; Startup = @() }
    if (Test-Path -LiteralPath $script:UndoPath) {
        try {
            $j = Get-Content -LiteralPath $script:UndoPath -Raw | ConvertFrom-Json
            if ($j.PowerScheme) { $u.PowerScheme = [string]$j.PowerScheme }
            if ($j.CpuCap) {
                $u.CpuCap = @{ Scheme = [string]$j.CpuCap.Scheme; AcMax = [int]$j.CpuCap.AcMax; DcMax = [int]$j.CpuCap.DcMax; AcMin = [int]$j.CpuCap.AcMin; DcMin = [int]$j.CpuCap.DcMin }
            }
            if ($j.Startup) {
                $u.Startup = @($j.Startup | ForEach-Object { @{ ApprovedKey = [string]$_.ApprovedKey; ValueName = [string]$_.ValueName; Name = [string]$_.Name } })
            }
        } catch { }
    }
    return $u
}

function Save-Undo($u) {
    try { ($u | ConvertTo-Json -Depth 5) | Set-Content -LiteralPath $script:UndoPath -Encoding UTF8 -ErrorAction Stop } catch { Write-TuneLog "Could not save undo information: $($_.Exception.Message)" }
}

# ---------------------------------------------------------------------------
#  Knowledge base: common processes and what they mean
# ---------------------------------------------------------------------------
$browserNote = 'Web browser. Many tabs, video, games or extensions make browsers the most common cause of a loud fan. Close tabs you are not using, remove extensions you do not need, and turn on the browser''s Efficiency / Memory Saver mode.'
$defenderNote = 'Microsoft Defender antivirus is scanning. This is usually temporary - let it finish. Do NOT turn Defender off.'
$searchNote = 'Windows Search is indexing your files. Normal after copying lots of files. If it never stops, rebuild the index: Settings > Privacy & security > Searching Windows > Advanced indexing options > Advanced > Rebuild.'
$updateNote = 'Windows Update is installing or tidying up updates. Let it finish, then restart the PC.'

$script:KnownProcesses = @{
    'msmpeng'               = $defenderNote
    'mpdefendercoreservice' = $defenderNote
    'mpcmdrun'              = $defenderNote
    'nissrv'                = 'Microsoft Defender network inspection. Normally light.'
    'searchindexer'         = $searchNote
    'searchprotocolhost'    = $searchNote
    'searchfilterhost'      = $searchNote
    'tiworker'              = $updateNote
    'trustedinstaller'      = $updateNote
    'mousocoreworker'       = $updateNote
    'usoclient'             = $updateNote
    'wuauclt'               = $updateNote
    'compattelrunner'       = 'Windows compatibility check (telemetry). Runs now and then and stops by itself.'
    'wmiprvse'              = 'WMI Provider Host answers requests for system information - this scan uses it briefly. If it stays busy afterwards, another program (often a vendor utility) is polling it constantly.'
    'svchost'               = 'Windows service host (several Windows services share it). In Task Manager > Details, right-click it > "Go to service(s)" to see which service is busy.'
    'dwm'                   = 'Desktop Window Manager draws the screen. High use usually means an out-of-date graphics driver, several/high-refresh monitors or heavy animations.'
    'system'                = 'Windows kernel and drivers. Sustained high use usually means a driver problem or very heavy disk activity.'
    'memory compression'    = 'Windows is compressing memory because RAM is nearly full. Close programs or add more RAM.'
    'audiodg'               = 'Windows audio engine. High use is usually caused by sound "enhancements" - turn them off in Sound settings.'
    'chrome'                = $browserNote
    'msedge'                = $browserNote
    'firefox'               = $browserNote
    'opera'                 = $browserNote
    'brave'                 = $browserNote
    'vivaldi'               = $browserNote
    'msedgewebview2'        = 'Embedded web page used inside apps such as Widgets, Teams, Outlook or Office.'
    'teams'                 = 'Microsoft Teams. Video calls and background blur/effects are very CPU/GPU heavy.'
    'ms-teams'              = 'Microsoft Teams. Video calls and background blur/effects are very CPU/GPU heavy.'
    'zoom'                  = 'Zoom. Video calls and virtual backgrounds are very CPU/GPU heavy.'
    'discord'               = 'Discord. Streaming, video and animated content use a lot of CPU/GPU. Turn off hardware acceleration if it misbehaves.'
    'onedrive'              = 'OneDrive is syncing files. If it is busy all the time, pause syncing for a while.'
    'dropbox'               = 'Dropbox is syncing files.'
    'googledrivefs'         = 'Google Drive is syncing files.'
    'explorer'              = 'Windows Explorer / desktop. Sustained high use can be caused by a folder with thousands of thumbnails or a faulty shell add-on.'
    'runtimebroker'         = 'Manages permissions for Store apps. Spikes are usually caused by one particular app.'
    'spoolsv'               = 'Print spooler. A stuck print job can keep it busy - clear the print queue.'
    'widgets'               = 'Windows Widgets. Can be switched off in Taskbar settings.'
    'steam'                 = 'Steam (game launcher). Downloads/updates games in the background.'
    'steamwebhelper'        = 'Steam''s built-in browser. Switch off animated avatars and GPU acceleration in Steam settings if heavy.'
    'epicgameslauncher'     = 'Epic Games Launcher. Updates games in the background.'
    'nvcontainer'           = 'NVIDIA driver service.'
    'nvdisplay.container'   = 'NVIDIA display driver service.'
    'lsass'                 = 'Windows security / sign-in. Should be light; sustained high use can indicate a problem - run a Defender scan.'
}

# Processes that must never be ended from this tool
$script:ProtectedProcesses = @('system','idle','registry','smss','csrss','wininit','winlogon','services','lsass','lsaiso','svchost',
    'dwm','explorer','fontdrvhost','memory compression','sihost','ctfmon','msmpeng','mpdefendercoreservice','securityhealthservice',
    'spoolsv','audiodg','wudfhost','conhost','dllhost','taskhostw','runtimebroker','startmenuexperiencehost','searchhost','textinputhost')

# ---------------------------------------------------------------------------
#  Measurement functions (all read-only)
# ---------------------------------------------------------------------------
function Get-ProcSnapshot {
    $h = @{}
    foreach ($p in [Diagnostics.Process]::GetProcesses()) {
        $cpu = $null
        try { $cpu = $p.TotalProcessorTime.TotalMilliseconds } catch { }
        $h[$p.Id] = @{ Name = $p.ProcessName; Cpu = $cpu; Mem = $p.WorkingSet64 }
    }
    return $h
}

function Get-GpuUsage {
    # Per-process GPU use (Windows 10 1709+). Returns $null if not available.
    try { $engines = Get-CimInstance -ClassName Win32_PerfFormattedData_GPUPerformanceCounters_GPUEngine -ErrorAction Stop } catch { return $null }
    $perPidType = @{}
    $perType = @{}
    foreach ($e in $engines) {
        if ($e.Name -match 'pid_(\d+)_.*engtype_(.+)$') {
            $procId = [int]$matches[1]; $type = $matches[2]; $u = [double]$e.UtilizationPercentage
            $k = "$procId|$type"
            if ($perPidType.ContainsKey($k)) { $perPidType[$k] += $u } else { $perPidType[$k] = $u }
            if ($perType.ContainsKey($type)) { $perType[$type] += $u } else { $perType[$type] = $u }
        }
    }
    $perPid = @{}
    foreach ($k in $perPidType.Keys) {
        $procId = [int]($k.Split('|')[0])
        $v = [math]::Min(100, $perPidType[$k])
        if (-not $perPid.ContainsKey($procId) -or $perPid[$procId] -lt $v) { $perPid[$procId] = $v }
    }
    $overall = 0
    foreach ($v in $perType.Values) { if ($v -gt $overall) { $overall = $v } }
    return @{ PerPid = $perPid; Overall = [math]::Min(100, $overall) }
}

function Get-Temperatures {
    $list = New-Object System.Collections.ArrayList
    try {
        $tz = Get-CimInstance -Namespace 'root/wmi' -ClassName MSAcpi_ThermalZoneTemperature -ErrorAction Stop
        foreach ($t in $tz) {
            $c = [math]::Round(($t.CurrentTemperature / 10) - 273.15, 1)
            if ($c -gt 5 -and $c -lt 125) { [void]$list.Add([pscustomobject]@{ Source = [string]$t.InstanceName; Celsius = $c }) }
        }
    } catch { }
    if ($list.Count -eq 0) {
        try {
            $tz = Get-CimInstance -ClassName Win32_PerfFormattedData_Counters_ThermalZoneInformation -ErrorAction Stop
            foreach ($t in $tz) {
                $k = if ($t.HighPrecisionTemperature) { $t.HighPrecisionTemperature / 10 } else { [double]$t.Temperature }
                $c = [math]::Round($k - 273.15, 1)
                if ($c -gt 5 -and $c -lt 125) { [void]$list.Add([pscustomobject]@{ Source = [string]$t.Name; Celsius = $c }) }
            }
        } catch { }
    }
    return $list
}

function Get-PowerSettingValues([string]$Setting, [string]$Scheme = 'SCHEME_CURRENT') {
    $lines = & powercfg.exe /query $Scheme SUB_PROCESSOR $Setting 2>$null
    $vals = @()
    foreach ($l in $lines) { if ($l -match ':\s*(0x[0-9a-fA-F]+)\s*$') { $vals += [Convert]::ToInt32($matches[1], 16) } }
    if ($vals.Count -ge 2) { return @{ AC = $vals[$vals.Count - 2]; DC = $vals[$vals.Count - 1] } }
    return $null
}

function Get-PowerInfo {
    $out = (& powercfg.exe /getactivescheme 2>$null) -join ' '
    $guid = $null; $name = ''
    if ($out -match '([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})') { $guid = $matches[1].ToLower() }
    if ($out -match '\(([^)]+)\)') { $name = $matches[1] }
    $kind = 'Other'
    if ($guid -eq $script:GuidBalanced) { $kind = 'Balanced' }
    elseif ($guid -eq $script:GuidHighPerf -or $name -match 'High performance') { $kind = 'HighPerformance' }
    elseif ($guid -eq $script:GuidUltimate -or $name -match 'Ultimate') { $kind = 'Ultimate' }
    elseif ($guid -eq $script:GuidSaver) { $kind = 'PowerSaver' }
    return @{ Guid = $guid; Name = $name; Kind = $kind; Max = (Get-PowerSettingValues 'PROCTHROTTLEMAX'); Min = (Get-PowerSettingValues 'PROCTHROTTLEMIN') }
}

function Get-FolderStats([string]$Path, [datetime]$OlderThan, [int]$MaxItems = 300000) {
    # Walks a folder WITHOUT following junctions/symlinks.
    $total = [double]0; $old = [double]0; $files = 0
    if (-not (Test-Path -LiteralPath $Path)) { return @{ Size = 0; OldSize = 0; Files = 0 } }
    $stack = New-Object System.Collections.Stack
    $stack.Push((New-Object IO.DirectoryInfo $Path))
    while ($stack.Count -gt 0 -and $files -lt $MaxItems) {
        $dir = $stack.Pop()
        try { $entries = $dir.GetFileSystemInfos() } catch { continue }
        foreach ($e in $entries) {
            if ($e.Attributes -band [IO.FileAttributes]::ReparsePoint) { continue }
            if ($e -is [IO.DirectoryInfo]) { $stack.Push($e) }
            else {
                $files++; $total += $e.Length
                if ($e.LastWriteTime -lt $OlderThan) { $old += $e.Length }
            }
        }
    }
    return @{ Size = $total; OldSize = $old; Files = $files }
}

# ---------------------------------------------------------------------------
#  Startup programs (same on/off switch Task Manager uses - fully reversible)
# ---------------------------------------------------------------------------
function Test-StartupEnabled([string]$ApprovedKey, [string]$ValueName) {
    try {
        $k = Get-Item -LiteralPath $ApprovedKey -ErrorAction Stop
        $v = $k.GetValue($ValueName)
        if ($v -is [byte[]] -and $v.Length -gt 0) { return (($v[0] -band 1) -eq 0) }
    } catch { }
    return $true
}

function Set-StartupState([string]$ApprovedKey, [string]$ValueName, [bool]$Enable) {
    if (-not (Test-Path -LiteralPath $ApprovedKey)) { New-Item -Path $ApprovedKey -Force | Out-Null }
    $bytes = New-Object byte[] 12
    if ($Enable) { $bytes[0] = 2 }
    else {
        $bytes[0] = 3
        $ft = [BitConverter]::GetBytes([DateTime]::Now.ToFileTime())
        [Array]::Copy($ft, 0, $bytes, 4, 8)
    }
    New-ItemProperty -LiteralPath $ApprovedKey -Name $ValueName -PropertyType Binary -Value $bytes -Force -ErrorAction Stop | Out-Null
}

function Get-StartupAdvice([string]$Name, [string]$Command) {
    $s = ("$Name $Command").ToLower()
    if ($s -match 'securityhealth|defender|antivirus|avast|avg|norton|mcafee|bitdefender|kaspersky|eset|malwarebytes|sophos|webroot|trend ?micro') { return 'Keep (security)' }
    if ($s -match 'realtek|rtkaud|rthdvcpl|synaptics|elan|etdctrl|alps|igfx|intel|nvidia|nvcontainer|amd|radeon|waves|maxxaudio|dolby|conexant|bluetooth|touchpad|hotkey|thinkpad|ibmpmsvc|asus.*(atk|acpi)|hp.*(hotkey|quicklaunch)|dell.*(touchpad|audio)') { return 'Keep (hardware / driver)' }
    if ($s -match 'logitech|lghub|razer|synapse|corsair|icue|steelseries|nzxt|armoury|msi center|dragon center') { return 'Optional (device software)' }
    if ($s -match 'onedrive|teams|spotify|discord|steam|epicgames|eadesktop|origin|ubisoft|battle\.net|skype|zoom|dropbox|google ?drive|googledrivefs|adobe|acrobat|creative cloud|ccxprocess|itunes|whatsapp|telegram|slack|msedge|chrome|opera|brave|utorrent|qbittorrent|ccleaner|jusched|java|wallpaper|cortana|copilot') { return 'Usually safe to disable' }
    return 'Review - disable if you do not need it at sign-in'
}

function Get-StartupItems {
    $items = New-Object System.Collections.ArrayList
    $regDefs = @(
        @{ Key = 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Run'; Approved = 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Explorer\StartupApproved\Run'; Source = 'Registry - this user' },
        @{ Key = 'HKLM:\Software\Microsoft\Windows\CurrentVersion\Run'; Approved = 'HKLM:\Software\Microsoft\Windows\CurrentVersion\Explorer\StartupApproved\Run'; Source = 'Registry - all users' },
        @{ Key = 'HKLM:\Software\WOW6432Node\Microsoft\Windows\CurrentVersion\Run'; Approved = 'HKLM:\Software\Microsoft\Windows\CurrentVersion\Explorer\StartupApproved\Run32'; Source = 'Registry - all users (32-bit)' }
    )
    foreach ($d in $regDefs) {
        try {
            $k = Get-Item -LiteralPath $d.Key -ErrorAction Stop
            foreach ($vn in $k.GetValueNames()) {
                if ([string]::IsNullOrEmpty($vn)) { continue }
                $cmd = [string]$k.GetValue($vn)
                [void]$items.Add([pscustomobject]@{
                    Name = $vn; Command = $cmd; Source = $d.Source; ApprovedKey = $d.Approved; ValueName = $vn
                    Enabled = (Test-StartupEnabled $d.Approved $vn); Advice = (Get-StartupAdvice $vn $cmd)
                })
            }
        } catch { }
    }
    $folderDefs = @(
        @{ Path = [Environment]::GetFolderPath('Startup'); Approved = 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Explorer\StartupApproved\StartupFolder'; Source = 'Startup folder - this user' },
        @{ Path = [Environment]::GetFolderPath('CommonStartup'); Approved = 'HKLM:\Software\Microsoft\Windows\CurrentVersion\Explorer\StartupApproved\StartupFolder'; Source = 'Startup folder - all users' }
    )
    foreach ($d in $folderDefs) {
        if (-not $d.Path -or -not (Test-Path -LiteralPath $d.Path)) { continue }
        foreach ($f in (Get-ChildItem -LiteralPath $d.Path -File -Force -ErrorAction SilentlyContinue)) {
            if ($f.Name -ieq 'desktop.ini') { continue }
            [void]$items.Add([pscustomobject]@{
                Name = [IO.Path]::GetFileNameWithoutExtension($f.Name); Command = $f.FullName; Source = $d.Source; ApprovedKey = $d.Approved; ValueName = $f.Name
                Enabled = (Test-StartupEnabled $d.Approved $f.Name); Advice = (Get-StartupAdvice $f.Name $f.FullName)
            })
        }
    }
    return $items
}

# ---------------------------------------------------------------------------
#  Process sampling (CPU %, GPU %, memory) over a few seconds
# ---------------------------------------------------------------------------
function Measure-Processes([int]$Seconds = 5, [scriptblock]$OnTick) {
    $cores = [Environment]::ProcessorCount
    $null = Get-GpuUsage   # prime the formatted counters
    try { $null = Get-CimInstance Win32_PerfFormattedData_PerfDisk_PhysicalDisk -Filter "Name='_Total'" -ErrorAction Stop } catch { }
    $before = Get-ProcSnapshot
    $sw = [Diagnostics.Stopwatch]::StartNew()
    $loads = New-Object System.Collections.Generic.List[double]
    $perfs = New-Object System.Collections.Generic.List[double]
    $freqMHz = $null
    for ($i = 0; $i -lt $Seconds; $i++) {
        try {
            $l = (Get-CimInstance Win32_Processor -ErrorAction Stop | Measure-Object -Property LoadPercentage -Average).Average
            if ($null -ne $l) { $loads.Add([double]$l) }
        } catch { }
        try {
            $pi = Get-CimInstance Win32_PerfFormattedData_Counters_ProcessorInformation -Filter "Name='_Total'" -ErrorAction Stop
            if ($pi) { $perfs.Add([double]$pi.PercentProcessorPerformance); if ($pi.ProcessorFrequency) { $freqMHz = [int]$pi.ProcessorFrequency } }
        } catch { }
        if ($OnTick) { & $OnTick $i $Seconds }
        $target = ($i + 1) * 1000
        $remaining = $target - $sw.ElapsedMilliseconds
        if ($remaining -gt 0) { Wait-UI $remaining }
    }
    $elapsedMs = [double]$sw.ElapsedMilliseconds
    $after = Get-ProcSnapshot
    $gpu = Get-GpuUsage
    $diskIdle = $null
    try { $diskIdle = [double](Get-CimInstance Win32_PerfFormattedData_PerfDisk_PhysicalDisk -Filter "Name='_Total'" -ErrorAction Stop).PercentIdleTime } catch { }

    $paths = @{}
    try { foreach ($w in (Get-CimInstance Win32_Process -Property ProcessId, ExecutablePath -ErrorAction Stop)) { $paths[[int]$w.ProcessId] = [string]$w.ExecutablePath } } catch { }

    $rows = New-Object System.Collections.ArrayList
    $sumCpu = 0.0
    foreach ($procId in $after.Keys) {
        if ($procId -eq 0) { continue }
        $a = $after[$procId]; $b = $before[$procId]
        $cpu = $null
        if ($b -and $null -ne $a.Cpu -and $null -ne $b.Cpu) {
            $cpu = [math]::Max(0, ($a.Cpu - $b.Cpu) / ($elapsedMs * $cores) * 100)
            $sumCpu += $cpu
        }
        $g = 0
        if ($gpu -and $gpu.PerPid.ContainsKey($procId)) { $g = $gpu.PerPid[$procId] }
        $nameLower = $a.Name.ToLower()
        $note = ''
        if ($script:KnownProcesses.ContainsKey($nameLower)) { $note = $script:KnownProcesses[$nameLower] }
        if ($procId -eq $PID) { $note = 'This tool (PC TuneUp) - only busy while scanning.' }
        [void]$rows.Add([pscustomobject]@{
            Name = $a.Name; Id = $procId; Cpu = $cpu; Gpu = $g; MemBytes = [double]$a.Mem
            Path = $paths[$procId]; Note = $note; IsSelf = ($procId -eq $PID)
        })
    }
    $avgLoad = if ($loads.Count -gt 0) { ($loads | Measure-Object -Average).Average } else { $null }
    $total = if ($null -ne $avgLoad) { [math]::Max($avgLoad, [math]::Min(100, $sumCpu)) } else { [math]::Min(100, $sumCpu) }
    $avgPerf = if ($perfs.Count -gt 0) { ($perfs | Measure-Object -Average).Average } else { $null }
    return @{
        Rows = $rows; TotalCpu = [math]::Round($total, 1); GpuOverall = $(if ($gpu) { [math]::Round($gpu.Overall, 1) } else { $null })
        PerfPercent = $avgPerf; FreqMHz = $freqMHz; DiskIdle = $diskIdle; Seconds = [math]::Round($elapsedMs / 1000, 1)
    }
}

# ---------------------------------------------------------------------------
#  The full scan
# ---------------------------------------------------------------------------
$script:Findings = New-Object System.Collections.ArrayList
$script:Scan = $null

function Add-Finding {
    param([string]$Severity, [string]$Area, [string]$Title, [string]$Detail, [string]$Recommendation, [string[]]$Fix = @())
    [void]$script:Findings.Add([pscustomobject]@{ Severity = $Severity; Area = $Area; Title = $Title; Detail = $Detail; Recommendation = $Recommendation; Fix = $Fix })
}

function Get-VendorCoolingTip([string]$Manufacturer) {
    $m = $Manufacturer.ToLower()
    if ($m -match 'dell')      { return 'Dell: open "Dell Optimizer", "MyDell" or "Dell Power Manager" and choose the Quiet or Cool thermal mode. In BIOS (F2) check Thermal Management.' }
    if ($m -match 'lenovo')    { return 'Lenovo: open "Lenovo Vantage" > Power and choose Intelligent Cooling / Quiet mode (on many models Fn+Q cycles fan modes).' }
    if ($m -match 'hp|hewlett') { return 'HP: open "HP Command Center" or "OMEN Gaming Hub" and choose Quiet/Cool. In BIOS (F10) turn off "Fan Always On" if present.' }
    if ($m -match 'asus')      { return 'ASUS: open "MyASUS" or "Armoury Crate" > Fan Profile and choose Whisper / Silent mode (on many models Fn+F5 cycles fan modes).' }
    if ($m -match 'acer')      { return 'Acer: open "Acer Care Center", "NitroSense" or "PredatorSense" and set fan control to Auto / Quiet.' }
    if ($m -match 'msi|micro-star') { return 'MSI: open "MSI Center" > User Scenario and choose Silent or Balanced.' }
    if ($m -match 'microsoft') { return 'Surface: use the Power mode slider (Settings > System > Power & battery) - "Best power efficiency" or "Balanced" runs cooler.' }
    if ($m -match 'samsung')   { return 'Samsung: open "Samsung Settings" > Performance mode and choose Quiet / Silent.' }
    if ($m -match 'razer')     { return 'Razer: open "Razer Synapse" > Performance and choose Balanced / Silent fan mode.' }
    if ($m -match 'gigabyte|aorus') { return 'Gigabyte: open "Gigabyte Control Center" and choose Quiet / Power Saving.' }
    return 'Check your PC maker''s control app or BIOS/UEFI setup for a "Quiet", "Silent" or "Balanced" fan profile, and make sure it is not set to "Full speed" or "Performance".'
}

function Invoke-SystemScan([scriptblock]$Progress) {
    $script:Findings.Clear()
    $s = @{}

    & $Progress 2 'Reading system information...'
    $cs   = Get-CimInstance Win32_ComputerSystem -ErrorAction SilentlyContinue
    $os   = Get-CimInstance Win32_OperatingSystem -ErrorAction SilentlyContinue
    $cpus = @(Get-CimInstance Win32_Processor -ErrorAction SilentlyContinue)
    $encl = Get-CimInstance Win32_SystemEnclosure -ErrorAction SilentlyContinue
    $bats = @(Get-CimInstance Win32_Battery -ErrorAction SilentlyContinue)
    $gpus = @(Get-CimInstance Win32_VideoController -ErrorAction SilentlyContinue)
    $chassisLaptop = @($encl.ChassisTypes | Where-Object { $_ -in 8, 9, 10, 11, 12, 14, 18, 21, 30, 31, 32 }).Count -gt 0
    $s.IsLaptop     = ($bats.Count -gt 0) -or $chassisLaptop
    $s.Manufacturer = [string]$cs.Manufacturer
    $s.Model        = [string]$cs.Model
    $s.OS           = '{0} (build {1})' -f $os.Caption, $os.BuildNumber
    $s.CpuName      = ($cpus | Select-Object -First 1).Name -replace '\s+', ' '
    $s.Cores        = ($cpus | Measure-Object -Property NumberOfCores -Sum).Sum
    $s.Threads      = [Environment]::ProcessorCount
    $s.MaxClock     = ($cpus | Select-Object -First 1).MaxClockSpeed
    $s.GpuNames     = ($gpus | ForEach-Object { $_.Name }) -join '; '
    $s.RamBytes     = [double]$cs.TotalPhysicalMemory
    $s.LastBoot     = $os.LastBootUpTime
    $s.Uptime       = (Get-Date) - $os.LastBootUpTime
    $s.Computer     = $env:COMPUTERNAME

    & $Progress 8 'Measuring what is using the processor and graphics (5 seconds)...'
    $m = Measure-Processes -Seconds 5 -OnTick { param($i, $n) & $Progress (8 + [int](40 * ($i + 1) / $n)) ("Measuring activity... {0}/{1} s" -f ($i + 1), $n) }
    $s.Measure = $m

    & $Progress 50 'Checking temperatures and fans...'
    $s.Temps = @(Get-Temperatures)
    $s.Fans  = @()
    try { $s.Fans = @(Get-CimInstance Win32_Fan -ErrorAction Stop | Where-Object { $_.DesiredSpeed -or $_.ActiveCooling }) } catch { }

    & $Progress 55 'Checking power settings...'
    $s.Power = Get-PowerInfo

    & $Progress 60 'Checking memory...'
    $s.RamTotalKB = [double]$os.TotalVisibleMemorySize
    $s.RamFreeKB  = [double]$os.FreePhysicalMemory
    $s.RamUsedPct = if ($s.RamTotalKB -gt 0) { [math]::Round(100 * (1 - $s.RamFreeKB / $s.RamTotalKB), 0) } else { 0 }
    $s.CommitPct  = if ($os.TotalVirtualMemorySize -gt 0) { [math]::Round(100 * (1 - $os.FreeVirtualMemory / $os.TotalVirtualMemorySize), 0) } else { 0 }

    & $Progress 64 'Checking drives...'
    $s.Volumes = @(Get-CimInstance Win32_LogicalDisk -Filter 'DriveType=3' -ErrorAction SilentlyContinue)
    $s.SystemDisk = $null
    $s.PhysicalDisks = @()
    try {
        $s.PhysicalDisks = @(Get-PhysicalDisk -ErrorAction Stop)
        $sysLetter = $env:SystemDrive.TrimEnd(':')
        $diskNum = (Get-Partition -DriveLetter $sysLetter -ErrorAction Stop | Select-Object -First 1).DiskNumber
        $s.SystemDisk = $s.PhysicalDisks | Where-Object { [string]$_.DeviceId -eq [string]$diskNum } | Select-Object -First 1
    } catch { }

    & $Progress 70 'Measuring temporary files...'
    $cutoff = (Get-Date).AddHours(-24)
    $t1 = Get-FolderStats $env:TEMP $cutoff
    $t2 = Get-FolderStats (Join-Path $env:WINDIR 'Temp') $cutoff
    $s.TempOldBytes = $t1.OldSize + $t2.OldSize
    $s.TempBytes    = $t1.Size + $t2.Size

    & $Progress 76 'Checking startup programs...'
    $s.Startup = @(Get-StartupItems)

    & $Progress 82 'Reading the Windows event log (throttling, crashes, disk errors)...'
    $since7 = (Get-Date).AddDays(-7); $since30 = (Get-Date).AddDays(-30)
    $s.ThrottleEvents = @(Get-WinEvent -FilterHashtable @{ LogName = 'System'; ProviderName = 'Microsoft-Windows-Kernel-Processor-Power'; Id = 37; StartTime = $since7 } -MaxEvents 500 -ErrorAction SilentlyContinue)
    $s.CrashEvents    = @(Get-WinEvent -FilterHashtable @{ LogName = 'System'; ProviderName = 'Microsoft-Windows-Kernel-Power'; Id = 41; StartTime = $since30 } -MaxEvents 100 -ErrorAction SilentlyContinue)
    $s.WheaEvents     = @(Get-WinEvent -FilterHashtable @{ LogName = 'System'; ProviderName = 'Microsoft-Windows-WHEA-Logger'; StartTime = $since30 } -MaxEvents 100 -ErrorAction SilentlyContinue)
    $s.DiskEvents     = @(Get-WinEvent -FilterHashtable @{ LogName = 'System'; ProviderName = 'disk'; Id = 7, 11, 51, 153; StartTime = $since30 } -MaxEvents 100 -ErrorAction SilentlyContinue)

    & $Progress 88 'Checking battery, security and pending restarts...'
    $s.OnBattery = $false; $s.BatteryWear = $null; $s.BatteryCharge = $null
    if ($bats.Count -gt 0) {
        $s.OnBattery = ($bats[0].BatteryStatus -eq 1)
        $s.BatteryCharge = $bats[0].EstimatedChargeRemaining
        try {
            $design = (Get-CimInstance -Namespace 'root/wmi' -ClassName BatteryStaticData -ErrorAction Stop | Select-Object -First 1).DesignedCapacity
            $full   = (Get-CimInstance -Namespace 'root/wmi' -ClassName BatteryFullChargedCapacity -ErrorAction Stop | Select-Object -First 1).FullChargedCapacity
            if ($design -gt 0 -and $full -gt 0) { $s.BatteryWear = [math]::Round(100 * (1 - $full / $design), 0) }
        } catch { }
    }
    $s.Defender = $null
    try { $s.Defender = Get-MpComputerStatus -ErrorAction Stop } catch { }
    $s.RebootPending = (Test-Path 'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Component Based Servicing\RebootPending') -or
                       (Test-Path 'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\WindowsUpdate\Auto Update\RebootRequired')
    $s.ProcessCount = @($m.Rows).Count

    & $Progress 94 'Analysing results...'
    $script:Scan = $s
    Invoke-Analysis $s
    & $Progress 100 'Scan complete.'
}

# ---------------------------------------------------------------------------
#  Analysis: turn measurements into findings + a plain-English fan verdict
# ---------------------------------------------------------------------------
function Invoke-Analysis($s) {
    $m = $s.Measure
    $rows = @($m.Rows)
    $topCpu = @($rows | Where-Object { $null -ne $_.Cpu -and -not $_.IsSelf } | Sort-Object Cpu -Descending | Select-Object -First 8)
    $topGpu = @($rows | Where-Object { $_.Gpu -gt 0 -and -not $_.IsSelf } | Sort-Object Gpu -Descending | Select-Object -First 5)
    $maxTemp = $null
    if ($s.Temps.Count -gt 0) { $maxTemp = ($s.Temps | Measure-Object -Property Celsius -Maximum).Maximum }
    $s.MaxTemp = $maxTemp
    $reasons = New-Object System.Collections.ArrayList   # fan verdict reasons, most important first

    # ---- Cooling / thermal ----
    $throttleCount = $s.ThrottleEvents.Count
    if ($throttleCount -gt 0) {
        $sev = if ($throttleCount -ge 20) { 'Critical' } else { 'Warning' }
        Add-Finding $sev 'Cooling' "Processor was slowed down by firmware $throttleCount time(s) in the last 7 days" `
            'Windows logged "processor speed is being limited by system firmware" (Kernel-Processor-Power event 37). This happens when the PC gets too hot (thermal throttling) or, on laptops, when running on battery or a weak charger.' `
            ("Clean dust out of the fan and vents (short bursts of compressed air with the PC off and the fan held still), keep vents clear (use a hard flat surface, not a bed or lap), and use the original charger. If the PC is several years old, renewing the thermal paste on the processor makes a big difference. " + (Get-VendorCoolingTip $s.Manufacturer))
        [void]$reasons.Add("COOLING IS STRUGGLING: Windows logged $throttleCount processor slow-down event(s) this week. The PC is getting hot enough that it has to slow itself down. Most common causes: dust clogging the fan/heatsink, blocked vents, or dried thermal paste.")
    }
    if ($null -ne $maxTemp) {
        if ($maxTemp -ge 90) {
            Add-Finding 'Critical' 'Cooling' ("Very high temperature: {0:N0} °C" -f $maxTemp) 'A thermal zone sensor reports a very high temperature. At this level the fan runs at full speed and the processor throttles.' ('Shut down and let it cool, then clean dust from the vents and fan. Check that all fans spin. ' + (Get-VendorCoolingTip $s.Manufacturer)) @('cpucap', 'balanced')
            [void]$reasons.Add(("RUNNING HOT: a temperature sensor reads {0:N0} °C - the fan has to run flat out." -f $maxTemp))
        } elseif ($maxTemp -ge 80) {
            Add-Finding 'Warning' 'Cooling' ("High temperature: {0:N0} °C" -f $maxTemp) 'A thermal zone sensor reports a high temperature, which makes the fan run fast.' ('Reduce load, keep vents clear and clean out dust. ' + (Get-VendorCoolingTip $s.Manufacturer)) @('cpucap')
            [void]$reasons.Add(("WARM: a temperature sensor reads {0:N0} °C." -f $maxTemp))
        } else {
            Add-Finding 'Good' 'Cooling' ("Temperature looks OK: {0:N0} °C" -f $maxTemp) ('Sensor(s): ' + (($s.Temps | ForEach-Object { '{0} = {1:N0} °C' -f $_.Source, $_.Celsius }) -join '; ') + '. Note: on many PCs this ACPI sensor is the motherboard/chassis, not the processor core itself.') ''
        }
    } else {
        Add-Finding 'Info' 'Cooling' 'Temperature sensors are not exposed to Windows on this PC' 'Many PCs only let the BIOS/manufacturer software read the processor temperature and fan speed. The diagnosis therefore uses load, throttling events and power settings instead.' ('The BIOS/UEFI setup screen usually shows CPU temperature and fan speed. ' + (Get-VendorCoolingTip $s.Manufacturer))
    }

    # ---- CPU load ----
    $cpu = $m.TotalCpu
    $culpritText = ($topCpu | Where-Object { $_.Cpu -ge 3 } | Select-Object -First 4 | ForEach-Object { '{0} ({1:N0}%)' -f $_.Name, $_.Cpu }) -join ', '
    if ($cpu -ge 60) {
        Add-Finding 'Critical' 'Processor' ("Processor is very busy: {0:N0}% average" -f $cpu) ("Measured over {0} seconds. Biggest users: {1}." -f $m.Seconds, $culpritText) 'See the Processes tab. Close or end programs you do not need, and check the notes for each one.' @('balanced', 'cpucap')
        [void]$reasons.Add(("HEAVY PROCESSOR LOAD: the CPU averaged {0:N0}% during the scan. Main users: {1}. A busy CPU makes heat, so the fan speeds up." -f $cpu, $culpritText))
    } elseif ($cpu -ge 25) {
        Add-Finding 'Warning' 'Processor' ("Processor has a steady background load: {0:N0}% average" -f $cpu) ("Measured over {0} seconds. Biggest users: {1}." -f $m.Seconds, $culpritText) 'A PC that is not being used should normally sit below 10-15%. See the Processes tab for what is keeping it busy.' @('cpucap')
        [void]$reasons.Add(("BACKGROUND LOAD: the CPU averaged {0:N0}% even though nothing heavy should be running. Main users: {1}." -f $cpu, $culpritText))
    } else {
        Add-Finding 'Good' 'Processor' ("Processor load is low: {0:N0}% average" -f $cpu) ("Measured over {0} seconds. Top users: {1}." -f $m.Seconds, $(if ($culpritText) { $culpritText } else { 'nothing significant' })) ''
    }
    foreach ($p in $topCpu) {
        if ($p.Cpu -lt 15) { continue }
        $note = if ($p.Note) { $p.Note } else { 'Not a recognised Windows component. If you do not know what it is, look at its file location (Processes tab > Open file location).' }
        Add-Finding 'Warning' 'Processor' ("{0} is using {1:N0}% of the processor" -f $p.Name, $p.Cpu) ("Process ID {0}. {1}" -f $p.Id, $(if ($p.Path) { "Location: $($p.Path)" } else { '' })) $note
    }
    # Suspicious: busy program running from a temp folder
    foreach ($p in ($rows | Where-Object { $_.Cpu -ge 5 -and $_.Path -and $_.Path -match '\\(Temp|AppData\\Local\\Temp|Downloads)\\' })) {
        Add-Finding 'Critical' 'Security' ("Busy program running from a temporary/download folder: {0}" -f $p.Name) ("{0} is using {1:N0}% CPU and runs from {2}. Hidden crypto-mining malware often runs like this and keeps fans loud." -f $p.Name, $p.Cpu, $p.Path) 'If you do not recognise it, run a Microsoft Defender scan (tick it in the Tune-Up tab). For the deepest check use Windows Security > Virus & threat protection > Scan options > Microsoft Defender Offline scan.' @('defender')
        [void]$reasons.Add(("POSSIBLE UNWANTED PROGRAM: {0} is busy and runs from a temporary folder - scan for malware." -f $p.Name))
    }
    # Windows maintenance in progress
    $maint = @($topCpu | Where-Object { $_.Cpu -ge 5 -and $_.Name -match '^(MsMpEng|MpDefenderCoreService|SearchIndexer|SearchProtocolHost|TiWorker|TrustedInstaller|MoUsoCoreWorker|CompatTelRunner|wuauclt)$' })
    if ($maint.Count -gt 0) {
        [void]$reasons.Add(("WINDOWS MAINTENANCE: {0} is running (antivirus scan / indexing / updates). This is temporary - leave the PC plugged in and it will calm down." -f (($maint | ForEach-Object { $_.Name }) -join ', ')))
    }

    # ---- GPU ----
    if ($null -ne $m.GpuOverall) {
        $gpuText = ($topGpu | Select-Object -First 3 | ForEach-Object { '{0} ({1:N0}%)' -f $_.Name, $_.Gpu }) -join ', '
        if ($m.GpuOverall -ge 50) {
            Add-Finding 'Warning' 'Graphics' ("Graphics chip is busy: {0:N0}%" -f $m.GpuOverall) "Main users: $gpuText." 'Games, video, 3D apps, browser tabs with animations, and video-call effects load the GPU. Close what you are not using; cap frame rates in games (e.g. 60 FPS) to cut heat a lot.'
            [void]$reasons.Add(("GRAPHICS LOAD: the GPU is {0:N0}% busy ({1})." -f $m.GpuOverall, $gpuText))
        } else {
            Add-Finding 'Good' 'Graphics' ("Graphics load is low: {0:N0}%" -f $m.GpuOverall) $(if ($gpuText) { "Top users: $gpuText." } else { 'No significant GPU use.' }) ''
        }
    }

    # ---- Power plan ----
    $pw = $s.Power
    if ($pw.Kind -eq 'HighPerformance' -or $pw.Kind -eq 'Ultimate') {
        Add-Finding 'Warning' 'Power' ("Power plan is '{0}'" -f $pw.Name) 'High performance / Ultimate plans keep the processor at high speed even when idle, which makes more heat and a louder fan, for little real-world gain.' 'Switch to the Balanced plan (Tune-Up tab). Balanced still runs at full speed when you need it.' @('balanced')
        [void]$reasons.Add(("POWER SETTINGS: the '{0}' power plan keeps the processor running fast all the time." -f $pw.Name))
    } elseif ($pw.Guid) {
        Add-Finding 'Good' 'Power' ("Power plan: {0}" -f $pw.Name) 'This plan lets the processor slow down when idle.' ''
    }
    if ($pw.Min -and ($pw.Min.AC -ge 50 -or $pw.Min.DC -ge 50)) {
        Add-Finding 'Warning' 'Power' ("Minimum processor state is high ({0}% plugged in / {1}% on battery)" -f $pw.Min.AC, $pw.Min.DC) 'The processor is not allowed to slow down when idle, so it produces heat all the time.' 'Lower it to 5% (included in the "Cooler & quieter CPU" tune-up).' @('cpucap')
        [void]$reasons.Add('POWER SETTINGS: the processor is not allowed to slow down when idle (high minimum processor state).')
    }
    if ($pw.Max -and $pw.Max.AC -ge 100 -and $null -ne $m.PerfPercent -and $m.PerfPercent -gt 100 -and $cpu -lt 25) {
        Add-Finding 'Info' 'Power' ("Turbo boost is active at light load ({0:N0}% of base speed)" -f $m.PerfPercent) 'The processor is boosting above its base clock even though the load is light. Boosting creates bursts of heat that make the fan ramp up and down.' 'The "Cooler & quieter CPU" tune-up caps the processor at 99%, which disables turbo boost. Typical effect: much quieter and cooler, about 10-25% slower in heavy tasks only. Fully reversible.' @('cpucap')
        [void]$reasons.Add('TURBO BOOST: the processor keeps boosting above its normal speed even at light load, causing heat bursts.')
    }

    # ---- Memory ----
    $ramGB = $s.RamBytes / 1GB
    if ($s.RamUsedPct -ge 90) {
        Add-Finding 'Critical' 'Memory' ("Memory is almost full: {0}% used" -f $s.RamUsedPct) ("Total RAM {0:N1} GB. When RAM is full Windows constantly swaps to disk, which slows everything and adds CPU/disk load." -f $ramGB) 'Close programs and browser tabs, disable unneeded startup programs. If this is normal for you, adding RAM is the best upgrade.' @('startup')
        [void]$reasons.Add(("MEMORY FULL: {0}% of RAM is in use, so Windows is swapping to disk." -f $s.RamUsedPct))
    } elseif ($s.RamUsedPct -ge 80) {
        Add-Finding 'Warning' 'Memory' ("Memory is getting full: {0}% used" -f $s.RamUsedPct) ("Total RAM {0:N1} GB." -f $ramGB) 'Close programs you are not using; disable unneeded startup programs.' @('startup')
    } else {
        Add-Finding 'Good' 'Memory' ("Memory use is fine: {0}% of {1:N1} GB" -f $s.RamUsedPct, $ramGB) '' ''
    }
    if ($ramGB -lt 7.5) {
        Add-Finding 'Info' 'Memory' ("Only {0:N1} GB of RAM installed" -f $ramGB) 'Windows 10/11 with a modern browser is comfortable with 8 GB and ideal with 16 GB.' 'If your PC allows it, upgrading to 8-16 GB is one of the most effective improvements.'
    }

    # ---- Disks ----
    foreach ($v in $s.Volumes) {
        if (-not $v.Size) { continue }
        $freePct = [math]::Round(100 * $v.FreeSpace / $v.Size, 0)
        $isSys = ($v.DeviceID -eq $env:SystemDrive)
        $label = '{0} {1}' -f $v.DeviceID, $(if ($isSys) { '(Windows drive)' } else { '' })
        if ($freePct -lt 10) {
            Add-Finding 'Critical' 'Storage' ("Drive {0} is almost full: {1}% free ({2})" -f $label, $freePct, (Format-Bytes $v.FreeSpace)) 'Below 10% free space Windows slows down, updates fail and the page file cannot grow.' 'Clean temporary files and the Recycle Bin (Tune-Up tab), uninstall programs you do not use, move photos/videos to an external drive.' @('temp', 'recycle', 'dism')
        } elseif ($freePct -lt 20) {
            Add-Finding 'Warning' 'Storage' ("Drive {0} is getting full: {1}% free ({2})" -f $label, $freePct, (Format-Bytes $v.FreeSpace)) 'Keeping at least 15-20% free keeps Windows fast.' 'Clean temporary files and the Recycle Bin; remove unused programs.' @('temp', 'recycle')
        } else {
            Add-Finding 'Good' 'Storage' ("Drive {0}: {1}% free ({2})" -f $label, $freePct, (Format-Bytes $v.FreeSpace)) '' ''
        }
    }
    if ($s.SystemDisk) {
        $sd = $s.SystemDisk
        if ([string]$sd.MediaType -eq 'HDD') {
            Add-Finding 'Warning' 'Storage' 'Windows is installed on a mechanical hard drive (HDD)' ("Disk: {0}. Hard drives are 5-20x slower than SSDs, causing slow start-up and constant 100% disk use." -f $sd.FriendlyName) 'Upgrading to an SSD is the single biggest speed improvement for this PC. Meanwhile, the "Optimise drives" tune-up defragments it.' @('optimize')
        }
        if ($sd.HealthStatus -and [string]$sd.HealthStatus -ne 'Healthy') {
            Add-Finding 'Critical' 'Storage' ("Windows drive reports health status: {0}" -f $sd.HealthStatus) ("Disk: {0}." -f $sd.FriendlyName) 'Back up your important files NOW, then plan to replace the drive.'
        }
    }
    if ($s.DiskEvents.Count -gt 0) {
        Add-Finding 'Critical' 'Storage' ("{0} disk error(s) in the event log (last 30 days)" -f $s.DiskEvents.Count) 'Windows logged bad-block / controller / paging errors from the disk driver.' 'Back up your files. Run "Check and repair system files" and consider replacing the drive if errors continue.' @('sfc')
    }
    if ($null -ne $m.DiskIdle -and $m.DiskIdle -lt 15) {
        Add-Finding 'Warning' 'Storage' ("Disk is very busy ({0:N0}% idle)" -f $m.DiskIdle) 'The disk was almost constantly busy during the scan.' 'Usually caused by Windows Update, antivirus, indexing, syncing apps, or too little RAM. On HDDs an SSD upgrade fixes this.'
    }
    if ($s.TempOldBytes -ge 500MB) {
        Add-Finding 'Warning' 'Storage' ("{0} of old temporary files" -f (Format-Bytes $s.TempOldBytes)) ('Temporary folders hold {0} in total; {1} is older than 24 hours and safe to remove.' -f (Format-Bytes $s.TempBytes), (Format-Bytes $s.TempOldBytes)) 'Run "Clean temporary files" in the Tune-Up tab.' @('temp')
    } elseif ($s.TempOldBytes -ge 50MB) {
        Add-Finding 'Info' 'Storage' ("{0} of old temporary files" -f (Format-Bytes $s.TempOldBytes)) 'Safe to remove.' 'Run "Clean temporary files" in the Tune-Up tab.' @('temp')
    }

    # ---- Startup ----
    $enabledStartup = @($s.Startup | Where-Object { $_.Enabled })
    $safeToDisable  = @($enabledStartup | Where-Object { $_.Advice -like 'Usually safe*' })
    if ($enabledStartup.Count -ge 12 -or $safeToDisable.Count -ge 4) {
        Add-Finding 'Warning' 'Startup' ("{0} programs start automatically ({1} usually safe to disable)" -f $enabledStartup.Count, $safeToDisable.Count) ('Usually safe to disable: ' + (($safeToDisable | ForEach-Object { $_.Name }) -join ', ')) 'Open the Startup tab and disable what you do not need at sign-in. This only stops them auto-starting; you can still open them normally, and you can re-enable them any time.' @('startup')
    } else {
        Add-Finding 'Good' 'Startup' ("{0} startup programs enabled" -f $enabledStartup.Count) '' ''
    }
    if ($s.ProcessCount -ge 280) {
        Add-Finding 'Info' 'Processor' ("{0} processes running" -f $s.ProcessCount) 'A lot of background programs are running.' 'Disable startup programs and uninstall software you no longer use (Settings > Apps).' @('startup')
    }

    # ---- Uptime / restart ----
    if ($s.Uptime.TotalDays -ge 7) {
        Add-Finding 'Warning' 'Windows' ("PC has not been restarted for {0:N0} days" -f $s.Uptime.TotalDays) 'Note: with Fast Startup, "Shut down" does not fully reset Windows - only "Restart" does. Long uptimes let memory leaks and stuck background tasks build up.' 'Use Start > Power > Restart (not Shut down) at least once a week.'
        [void]$reasons.Add(("NO RECENT RESTART: Windows has been running for {0:N0} days - stuck background tasks may be keeping the CPU busy. A Restart often calms the fan." -f $s.Uptime.TotalDays))
    }
    if ($s.RebootPending) {
        Add-Finding 'Warning' 'Windows' 'A restart is waiting to finish installing updates' 'Update work continues in the background until the PC restarts.' 'Save your work and Restart.'
    }

    # ---- Stability ----
    if ($s.CrashEvents.Count -gt 0) {
        Add-Finding 'Warning' 'Stability' ("{0} unexpected shutdown(s) in the last 30 days" -f $s.CrashEvents.Count) 'Kernel-Power event 41: the PC turned off or restarted without shutting down cleanly. Overheating, power problems or driver crashes cause this.' 'If it happened under load, overheating is likely - clean the cooling system. Otherwise check power cables/charger and update drivers.' @('sfc')
    }
    if ($s.WheaEvents.Count -gt 0) {
        Add-Finding 'Warning' 'Stability' ("{0} hardware error report(s) (WHEA) in the last 30 days" -f $s.WheaEvents.Count) 'The processor, memory or PCIe devices reported corrected or uncorrected hardware errors. Heat and unstable overclocks are common causes.' 'Remove any overclock (BIOS defaults), keep the PC cool, and install BIOS/chipset updates from your PC maker when you are next online.'
    }

    # ---- Battery ----
    if ($s.IsLaptop -and $null -ne $s.BatteryWear -and $s.BatteryWear -ge 30) {
        Add-Finding 'Info' 'Battery' ("Battery has lost about {0}% of its original capacity" -f $s.BatteryWear) 'Worn batteries hold less charge and can make laptops limit performance.' 'Consider a battery replacement if battery life is a problem.'
    }

    # ---- Security ----
    if ($s.Defender) {
        $d = $s.Defender
        if (-not $d.AntivirusEnabled -or -not $d.RealTimeProtectionEnabled) {
            Add-Finding 'Warning' 'Security' 'Microsoft Defender real-time protection is off' 'This may be because another antivirus is installed (fine), or protection was switched off.' 'Make sure you have one active antivirus.'
        }
        if ($d.AntivirusSignatureLastUpdated -and ((Get-Date) - $d.AntivirusSignatureLastUpdated).TotalDays -gt 14) {
            Add-Finding 'Info' 'Security' ("Antivirus definitions are {0:N0} days old" -f ((Get-Date) - $d.AntivirusSignatureLastUpdated).TotalDays) 'This tool works offline and cannot update them.' 'Let Windows Update run next time the PC is online.'
        }
    }

    # ---- Fan verdict ----
    if ($reasons.Count -eq 0) {
        [void]$reasons.Add('NO SOFTWARE CAUSE FOUND right now: processor and graphics load are low and power settings are sensible.')
        [void]$reasons.Add('If the fan is still loud while the PC is idle, the cause is most likely physical: dust in the fan or heatsink, blocked vents, a worn/rattling fan bearing, or an aggressive fan profile in the BIOS or manufacturer app.')
        Add-Finding 'Info' 'Cooling' 'Loud fan at low load usually means a cooling hardware issue' 'No software reason was found during the scan.' ('Clean dust from vents and fan, use the PC on a hard surface, and check the fan profile. ' + (Get-VendorCoolingTip $s.Manufacturer))
    }
    if ($s.IsLaptop -and -not $s.OnBattery -and $s.BatteryCharge -ne $null -and $s.BatteryCharge -lt 95) {
        [void]$reasons.Add(("CHARGING: the battery is charging ({0}%). Charging adds heat, so laptop fans are often louder while plugged in and charging." -f $s.BatteryCharge))
    }
    $s.FanReasons = $reasons

    # ---- Health score ----
    $score = 100
    foreach ($f in $script:Findings) {
        if ($f.Severity -eq 'Critical') { $score -= 15 }
        elseif ($f.Severity -eq 'Warning') { $score -= 6 }
    }
    $s.Score = [math]::Max(0, $score)
}

# ---------------------------------------------------------------------------
#  Tune-up actions
# ---------------------------------------------------------------------------
$script:TuneActions = @(
    @{ Key = 'restorepoint'; Name = 'Create a System Restore point first';       Desc = 'A safety snapshot of Windows settings so you can roll back if anything goes wrong. (Windows allows one every 24 hours.)'; Undo = 'n/a' },
    @{ Key = 'balanced';     Name = 'Switch to the Balanced power plan';         Desc = 'Lets the processor slow down when idle - less heat, quieter fan, full speed still available when needed.'; Undo = 'Yes' },
    @{ Key = 'cpucap';       Name = 'Cooler & quieter CPU (cap at 99%)';          Desc = 'Sets maximum processor state to 99% (turns off turbo boost) and minimum to 5%. Usually the biggest fan-noise reduction; heavy tasks run ~10-25% slower.'; Undo = 'Yes' },
    @{ Key = 'temp';         Name = 'Clean temporary files';                      Desc = 'Deletes files older than 24 hours from your Temp folder and the Windows Temp folder. Files in use are skipped.'; Undo = 'No (safe)' },
    @{ Key = 'recycle';      Name = 'Empty the Recycle Bin';                      Desc = 'Permanently deletes everything in the Recycle Bin. Check it first if unsure!'; Undo = 'No' },
    @{ Key = 'optimize';     Name = 'Optimise drives';                            Desc = 'Runs TRIM on SSDs and defragmentation on hard drives. Opens a progress window; can take a while on HDDs.'; Undo = 'n/a' },
    @{ Key = 'dism';         Name = 'Clean up old Windows Update files';          Desc = 'DISM component-store cleanup - frees space taken by superseded updates. Works offline. Opens a progress window.'; Undo = 'No (safe)' },
    @{ Key = 'sfc';          Name = 'Check and repair system files';              Desc = 'Runs System File Checker (sfc /scannow) to repair corrupted Windows files using the local copy. Opens a progress window (10-20 min).'; Undo = 'n/a' },
    @{ Key = 'defender';     Name = 'Microsoft Defender quick scan';              Desc = 'Scans for malware (including crypto-miners that keep fans loud) using the definitions already on this PC. Opens a progress window.'; Undo = 'n/a' }
)

function Invoke-TuneUps([string[]]$Keys) {
    $undo = Read-Undo
    $console = New-Object System.Collections.Generic.List[string]

    foreach ($k in $Keys) {
        switch ($k) {
            'restorepoint' {
                Write-TuneLog 'Creating a System Restore point...'
                try {
                    $warn = $null
                    Checkpoint-Computer -Description 'PC TuneUp' -RestorePointType 'MODIFY_SETTINGS' -ErrorAction Stop -WarningVariable warn -WarningAction SilentlyContinue
                    if ($warn) { Write-TuneLog "  Note: $warn" } else { Write-TuneLog '  Restore point created.' }
                } catch {
                    Write-TuneLog "  Could not create a restore point: $($_.Exception.Message)"
                    Write-TuneLog '  (System Protection may be off. Turn it on in: Control Panel > System > System protection.) Continuing - all settings changes below are still recorded for Undo.'
                }
            }
            'balanced' {
                $pw = Get-PowerInfo
                if ($pw.Guid -eq $script:GuidBalanced) { Write-TuneLog 'Power plan is already Balanced.'; break }
                if (-not $undo.PowerScheme) { $undo.PowerScheme = $pw.Guid }
                & powercfg.exe /setactive $script:GuidBalanced 2>&1 | Out-Null
                if ((Get-PowerInfo).Guid -eq $script:GuidBalanced) {
                    Write-TuneLog ("Switched power plan from '{0}' to Balanced." -f $pw.Name)
                } else {
                    Write-TuneLog 'Balanced plan is missing - restoring it from Windows defaults...'
                    $dup = (& powercfg.exe /duplicatescheme $script:GuidBalanced 2>&1) -join ' '
                    if ($dup -match '([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})') {
                        & powercfg.exe /setactive $matches[1] 2>&1 | Out-Null
                        Write-TuneLog '  Balanced plan restored and activated.'
                    } else { Write-TuneLog "  Could not switch plan: $dup" }
                }
                Save-Undo $undo
            }
            'cpucap' {
                $pw = Get-PowerInfo
                if ($pw.Max -and $pw.Min -and -not $undo.CpuCap) {
                    $undo.CpuCap = @{ Scheme = $pw.Guid; AcMax = $pw.Max.AC; DcMax = $pw.Max.DC; AcMin = $pw.Min.AC; DcMin = $pw.Min.DC }
                    Save-Undo $undo
                }
                & powercfg.exe /setacvalueindex SCHEME_CURRENT SUB_PROCESSOR PROCTHROTTLEMAX 99 | Out-Null
                & powercfg.exe /setdcvalueindex SCHEME_CURRENT SUB_PROCESSOR PROCTHROTTLEMAX 99 | Out-Null
                & powercfg.exe /setacvalueindex SCHEME_CURRENT SUB_PROCESSOR PROCTHROTTLEMIN 5 | Out-Null
                & powercfg.exe /setdcvalueindex SCHEME_CURRENT SUB_PROCESSOR PROCTHROTTLEMIN 5 | Out-Null
                & powercfg.exe /setactive SCHEME_CURRENT | Out-Null
                $after = Get-PowerInfo
                if ($after.Max) { Write-TuneLog ("CPU capped: max {0}%/{1}% (plugged in/battery), min {2}%/{3}%. Turbo boost is now off - expect a quieter, cooler PC." -f $after.Max.AC, $after.Max.DC, $after.Min.AC, $after.Min.DC) }
                else { Write-TuneLog 'Could not read processor power settings after the change (this PC may hide them).' }
            }
            'temp' {
                Write-TuneLog 'Cleaning temporary files older than 24 hours...'
                $cutoff = (Get-Date).AddHours(-24)
                $freed = [double]0; $count = 0
                foreach ($root in (@($env:TEMP, (Join-Path $env:WINDIR 'Temp')) | Select-Object -Unique)) {
                    if (-not (Test-Path -LiteralPath $root)) { continue }
                    $dirs = New-Object System.Collections.ArrayList
                    $stack = New-Object System.Collections.Stack
                    $stack.Push((New-Object IO.DirectoryInfo $root))
                    while ($stack.Count -gt 0) {
                        $dir = $stack.Pop()
                        try { $entries = $dir.GetFileSystemInfos() } catch { continue }
                        foreach ($e in $entries) {
                            if ($e.Attributes -band [IO.FileAttributes]::ReparsePoint) { continue }   # never follow links
                            if ($e -is [IO.DirectoryInfo]) { $stack.Push($e); [void]$dirs.Add($e) }
                            elseif ($e.LastWriteTime -lt $cutoff) {
                                try { $len = $e.Length; $e.Delete(); $freed += $len; $count++ } catch { }
                            }
                        }
                        if (($count % 500) -eq 0) { [System.Windows.Forms.Application]::DoEvents() }
                    }
                    foreach ($d in ($dirs | Sort-Object { $_.FullName.Length } -Descending)) {
                        try { if ($d.LastWriteTime -lt $cutoff) { $d.Delete() } } catch { }   # only removes empty folders
                    }
                }
                Write-TuneLog ("  Removed {0:N0} files, freed {1}." -f $count, (Format-Bytes $freed))
            }
            'recycle' {
                try { Clear-RecycleBin -Force -ErrorAction Stop; Write-TuneLog 'Recycle Bin emptied.' }
                catch { Write-TuneLog "Recycle Bin: $($_.Exception.Message)" }
            }
            'optimize' {
                $console.Add(@'
Write-Host "`n=== Optimising drives (TRIM for SSD / defrag for HDD) ===" -ForegroundColor Cyan
Get-Volume | Where-Object { $_.DriveLetter -and $_.DriveType -eq 'Fixed' -and $_.FileSystem -eq 'NTFS' } | ForEach-Object {
    Write-Host "Drive $($_.DriveLetter):" -ForegroundColor Yellow
    try { Optimize-Volume -DriveLetter $_.DriveLetter -Verbose -ErrorAction Stop } catch { Write-Host $_.Exception.Message -ForegroundColor Red }
}
'@)
                Write-TuneLog 'Drive optimisation queued (runs in the progress window).'
            }
            'dism' {
                $console.Add(@'
Write-Host "`n=== Cleaning up old Windows Update files (DISM) ===" -ForegroundColor Cyan
& "$env:WINDIR\System32\Dism.exe" /Online /Cleanup-Image /StartComponentCleanup
'@)
                Write-TuneLog 'Windows Update clean-up queued (runs in the progress window).'
            }
            'sfc' {
                $console.Add(@'
Write-Host "`n=== Checking and repairing system files (sfc /scannow) ===" -ForegroundColor Cyan
& "$env:WINDIR\System32\sfc.exe" /scannow
'@)
                Write-TuneLog 'System file check queued (runs in the progress window).'
            }
            'defender' {
                $console.Add(@'
Write-Host "`n=== Microsoft Defender quick scan (using definitions already on this PC) ===" -ForegroundColor Cyan
$mp = Join-Path $env:ProgramFiles 'Windows Defender\MpCmdRun.exe'
if (Test-Path $mp) { & $mp -Scan -ScanType 1 } else { try { Start-MpScan -ScanType QuickScan -ErrorAction Stop; Write-Host 'Scan finished.' } catch { Write-Host $_.Exception.Message -ForegroundColor Red } }
'@)
                Write-TuneLog 'Defender quick scan queued (runs in the progress window).'
            }
        }
    }

    if ($console.Count -gt 0) {
        $scriptText = "`$Host.UI.RawUI.WindowTitle = 'PC TuneUp - maintenance tasks'`r`n" +
            "Write-Host 'PC TuneUp maintenance tasks. Please leave this window open until it says ALL DONE.' -ForegroundColor Green`r`n" +
            ($console -join "`r`n") +
            "`r`nWrite-Host ''`r`nWrite-Host '=== ALL DONE. You can close this window. A restart is recommended. ===' -ForegroundColor Green`r`n"
        $tmp = Join-Path $script:DataDir 'maintenance-tasks.ps1'
        Set-Content -LiteralPath $tmp -Value $scriptText -Encoding UTF8
        Start-Process -FilePath 'powershell.exe' -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-NoExit', '-File', "`"$tmp`"")
        Write-TuneLog 'Opened the maintenance progress window.'
    }
    Write-TuneLog 'Tune-up finished. Restart the PC to get the full benefit, then run a new scan to compare.'
}

function Invoke-UndoTuneUps {
    $undo = Read-Undo
    $did = $false
    if ($undo.CpuCap -and $undo.CpuCap.Scheme) {
        $c = $undo.CpuCap
        & powercfg.exe /setacvalueindex $c.Scheme SUB_PROCESSOR PROCTHROTTLEMAX $c.AcMax | Out-Null
        & powercfg.exe /setdcvalueindex $c.Scheme SUB_PROCESSOR PROCTHROTTLEMAX $c.DcMax | Out-Null
        & powercfg.exe /setacvalueindex $c.Scheme SUB_PROCESSOR PROCTHROTTLEMIN $c.AcMin | Out-Null
        & powercfg.exe /setdcvalueindex $c.Scheme SUB_PROCESSOR PROCTHROTTLEMIN $c.DcMin | Out-Null
        Write-TuneLog ("Restored processor limits (max {0}%/{1}%, min {2}%/{3}%)." -f $c.AcMax, $c.DcMax, $c.AcMin, $c.DcMin)
        $undo.CpuCap = $null; $did = $true
    }
    if ($undo.PowerScheme) {
        & powercfg.exe /setactive $undo.PowerScheme 2>&1 | Out-Null
        Write-TuneLog "Restored previous power plan ($($undo.PowerScheme))."
        $undo.PowerScheme = $null; $did = $true
    } else {
        & powercfg.exe /setactive SCHEME_CURRENT | Out-Null
    }
    foreach ($st in @($undo.Startup)) {
        try { Set-StartupState $st.ApprovedKey $st.ValueName $true; Write-TuneLog "Re-enabled startup program: $($st.Name)"; $did = $true }
        catch { Write-TuneLog "Could not re-enable $($st.Name): $($_.Exception.Message)" }
    }
    $undo.Startup = @()
    Save-Undo $undo
    if (-not $did) { Write-TuneLog 'Nothing to undo.' } else { Write-TuneLog 'Undo complete. (File clean-ups cannot be undone; a System Restore point can roll back other changes.)' }
}

# ---------------------------------------------------------------------------
#  HTML report
# ---------------------------------------------------------------------------
function Export-Report {
    if (-not $script:Scan) { [System.Windows.Forms.MessageBox]::Show('Run a scan first.', $script:AppName) | Out-Null; return }
    $s = $script:Scan
    $sb = New-Object System.Text.StringBuilder
    [void]$sb.Append('<!DOCTYPE html><html><head><meta charset="utf-8"><title>PC TuneUp report</title><style>')
    [void]$sb.Append('body{font-family:Segoe UI,Arial,sans-serif;margin:24px;color:#222;max-width:1100px}h1{margin-bottom:0}table{border-collapse:collapse;width:100%;margin:8px 0 24px}')
    [void]$sb.Append('td,th{border:1px solid #ddd;padding:6px 8px;text-align:left;vertical-align:top;font-size:14px}th{background:#f3f3f3}.Critical{background:#fde2e2}.Warning{background:#fff3cd}.Info{background:#e3f0ff}.Good{background:#e2f5e2}')
    [void]$sb.Append('.score{font-size:40px;font-weight:bold}.box{background:#f7f7f9;border-left:4px solid #3b6fd8;padding:10px 16px;margin:12px 0}</style></head><body>')
    [void]$sb.Append("<h1>PC TuneUp report</h1><p>$(HtmlEnc $s.Computer) &middot; $(Get-Date -Format 'dddd d MMMM yyyy, HH:mm') &middot; generated offline</p>")
    [void]$sb.Append("<p class='score'>Health score: $($s.Score) / 100</p>")
    [void]$sb.Append('<h2>Why is the fan loud?</h2><div class="box"><ul>')
    foreach ($r in $s.FanReasons) { [void]$sb.Append("<li>$(HtmlEnc $r)</li>") }
    [void]$sb.Append('</ul></div><h2>System</h2><table>')
    $sys = [ordered]@{
        'Computer' = "$($s.Manufacturer) $($s.Model)"; 'Windows' = $s.OS; 'Processor' = "$($s.CpuName) ($($s.Cores) cores / $($s.Threads) threads)"
        'Graphics' = $s.GpuNames; 'Memory' = ('{0:N1} GB ({1}% in use)' -f ($s.RamBytes / 1GB), $s.RamUsedPct); 'Power plan' = $s.Power.Name
        'Average CPU load' = ('{0:N0}%' -f $s.Measure.TotalCpu); 'GPU load' = $(if ($null -ne $s.Measure.GpuOverall) { '{0:N0}%' -f $s.Measure.GpuOverall } else { 'n/a' })
        'Temperature' = $(if ($null -ne $s.MaxTemp) { '{0:N0} °C (highest sensor)' -f $s.MaxTemp } else { 'not exposed by this PC' })
        'Uptime' = ('{0:N1} days' -f $s.Uptime.TotalDays); 'Type' = $(if ($s.IsLaptop) { 'Laptop' } else { 'Desktop' })
    }
    foreach ($k in $sys.Keys) { [void]$sb.Append("<tr><th style='width:200px'>$(HtmlEnc $k)</th><td>$(HtmlEnc ([string]$sys[$k]))</td></tr>") }
    [void]$sb.Append('</table><h2>Findings &amp; recommendations</h2><table><tr><th>Severity</th><th>Area</th><th>Finding</th><th>Details</th><th>Recommendation</th></tr>')
    foreach ($f in (Get-SortedFindings)) {
        [void]$sb.Append("<tr class='$($f.Severity)'><td>$($f.Severity)</td><td>$(HtmlEnc $f.Area)</td><td>$(HtmlEnc $f.Title)</td><td>$(HtmlEnc $f.Detail)</td><td>$(HtmlEnc $f.Recommendation)</td></tr>")
    }
    [void]$sb.Append('</table><h2>Top processes</h2><table><tr><th>Process</th><th>PID</th><th>CPU %</th><th>GPU %</th><th>Memory</th><th>Notes</th></tr>')
    foreach ($p in (@($s.Measure.Rows) | Sort-Object { if ($null -ne $_.Cpu) { $_.Cpu } else { 0 } } -Descending | Select-Object -First 25)) {
        [void]$sb.Append(("<tr><td>{0}</td><td>{1}</td><td>{2:N1}</td><td>{3:N0}</td><td>{4}</td><td>{5}</td></tr>" -f (HtmlEnc $p.Name), $p.Id, $(if ($null -ne $p.Cpu) { $p.Cpu } else { 0 }), $p.Gpu, (Format-Bytes $p.MemBytes), (HtmlEnc $p.Note)))
    }
    [void]$sb.Append('</table><h2>Startup programs</h2><table><tr><th>Name</th><th>Status</th><th>Advice</th><th>Command</th></tr>')
    foreach ($st in $s.Startup) {
        [void]$sb.Append("<tr><td>$(HtmlEnc $st.Name)</td><td>$(if ($st.Enabled) { 'Enabled' } else { 'Disabled' })</td><td>$(HtmlEnc $st.Advice)</td><td>$(HtmlEnc $st.Command)</td></tr>")
    }
    [void]$sb.Append('</table></body></html>')
    $path = Join-Path $script:DataDir ('PC-Report-{0:yyyyMMdd-HHmm}.html' -f (Get-Date))
    Set-Content -LiteralPath $path -Value $sb.ToString() -Encoding UTF8
    Invoke-Item -LiteralPath $path
}

function Get-SortedFindings {
    $order = @{ 'Critical' = 0; 'Warning' = 1; 'Info' = 2; 'Good' = 3 }
    return @($script:Findings | Sort-Object { $order[$_.Severity] }, Area)
}

# ===========================================================================
#  USER INTERFACE
# ===========================================================================
$font     = New-Object System.Drawing.Font('Segoe UI', 9)
$fontBold = New-Object System.Drawing.Font('Segoe UI', 10, [System.Drawing.FontStyle]::Bold)
$fontBig  = New-Object System.Drawing.Font('Segoe UI', 20, [System.Drawing.FontStyle]::Bold)
$colors = @{
    Critical = [System.Drawing.Color]::FromArgb(253, 226, 226)
    Warning  = [System.Drawing.Color]::FromArgb(255, 243, 205)
    Info     = [System.Drawing.Color]::FromArgb(227, 240, 255)
    Good     = [System.Drawing.Color]::FromArgb(226, 245, 226)
}

function New-Button([string]$Text, [int]$X, [int]$Y, [int]$W = 150, [int]$H = 32) {
    $b = New-Object System.Windows.Forms.Button
    $b.Text = $Text; $b.Location = New-Object System.Drawing.Point($X, $Y); $b.Size = New-Object System.Drawing.Size($W, $H); $b.Font = $font
    return $b
}

function New-ListView([string[]]$Columns, [int[]]$Widths) {
    $lv = New-Object System.Windows.Forms.ListView
    $lv.View = 'Details'; $lv.FullRowSelect = $true; $lv.GridLines = $true; $lv.HideSelection = $false; $lv.Dock = 'Fill'; $lv.Font = $font
    for ($i = 0; $i -lt $Columns.Count; $i++) { [void]$lv.Columns.Add($Columns[$i], $Widths[$i]) }
    return $lv
}

$form = New-Object System.Windows.Forms.Form
$form.Text = "$($script:AppName) $($script:Version) - offline fan & performance doctor" + $(if ($script:IsAdmin) { '' } else { '   [LIMITED MODE - not running as administrator]' })
$form.Size = New-Object System.Drawing.Size(1150, 760)
$form.MinimumSize = New-Object System.Drawing.Size(900, 600)
$form.StartPosition = 'CenterScreen'
$form.Font = $font

$tabs = New-Object System.Windows.Forms.TabControl
$tabs.Dock = 'Fill'
$tabOverview = New-Object System.Windows.Forms.TabPage; $tabOverview.Text = '  1. Diagnosis  '
$tabProcs    = New-Object System.Windows.Forms.TabPage; $tabProcs.Text    = '  2. Processes  '
$tabStartup  = New-Object System.Windows.Forms.TabPage; $tabStartup.Text  = '  3. Startup programs  '
$tabTune     = New-Object System.Windows.Forms.TabPage; $tabTune.Text     = '  4. Tune-Up  '
$tabs.TabPages.AddRange(@($tabOverview, $tabProcs, $tabStartup, $tabTune))

$status = New-Object System.Windows.Forms.StatusStrip
$statusLabel = New-Object System.Windows.Forms.ToolStripStatusLabel
$statusLabel.Text = ("Offline tool - no internet connection is used.   Mode: {0}.   Data folder: {1}" -f $(if ($script:IsAdmin) { 'Administrator' } else { 'Limited (some checks and fixes unavailable)' }), $script:DataDir)
[void]$status.Items.Add($statusLabel)

# ---------------- Tab 1: Diagnosis ----------------
$lvFind = New-ListView @('Severity', 'Area', 'Finding', 'Recommendation') @(80, 90, 380, 560)
$tabOverview.Controls.Add($lvFind)

$txtDetail = New-Object System.Windows.Forms.TextBox
$txtDetail.Multiline = $true; $txtDetail.ReadOnly = $true; $txtDetail.ScrollBars = 'Vertical'; $txtDetail.Dock = 'Bottom'; $txtDetail.Height = 110
$txtDetail.BackColor = [System.Drawing.Color]::White
$txtDetail.Text = 'Select a finding to see the details here.'
$tabOverview.Controls.Add($txtDetail)

$txtFan = New-Object System.Windows.Forms.TextBox
$txtFan.Multiline = $true; $txtFan.ReadOnly = $true; $txtFan.ScrollBars = 'Vertical'; $txtFan.Dock = 'Top'; $txtFan.Height = 150
$txtFan.Font = New-Object System.Drawing.Font('Segoe UI', 10)
$txtFan.BackColor = [System.Drawing.Color]::FromArgb(247, 247, 249)
$txtFan.Text = "Press 'Scan my PC' to find out why your fan is loud and how to make the PC faster." + [Environment]::NewLine + [Environment]::NewLine +
    "For the most accurate result: close the programs you normally use, wait a minute, then scan while the fan is loud." + [Environment]::NewLine +
    "The scan only READS information - nothing is changed until you choose tune-ups in tab 4."
$tabOverview.Controls.Add($txtFan)

$pnlTop = New-Object System.Windows.Forms.Panel
$pnlTop.Dock = 'Top'; $pnlTop.Height = 64
$btnScan = New-Button 'Scan my PC' 10 14 160 36
$btnScan.Font = $fontBold
$btnScan.BackColor = [System.Drawing.Color]::FromArgb(59, 111, 216); $btnScan.ForeColor = [System.Drawing.Color]::White; $btnScan.FlatStyle = 'Flat'
$progress = New-Object System.Windows.Forms.ProgressBar
$progress.Location = New-Object System.Drawing.Point(185, 16); $progress.Size = New-Object System.Drawing.Size(360, 14)
$lblProgress = New-Object System.Windows.Forms.Label
$lblProgress.Location = New-Object System.Drawing.Point(185, 34); $lblProgress.Size = New-Object System.Drawing.Size(500, 20); $lblProgress.Text = 'Ready.'
$btnReport = New-Button 'Save HTML report' 700 14 140 36
$btnFolder = New-Button 'Open data folder' 848 14 130 36
$lblScore = New-Object System.Windows.Forms.Label
$lblScore.Location = New-Object System.Drawing.Point(990, 10); $lblScore.Size = New-Object System.Drawing.Size(140, 46); $lblScore.Font = $fontBig; $lblScore.Text = ''
$lblScore.Anchor = 'Top, Right'
$pnlTop.Controls.AddRange(@($btnScan, $progress, $lblProgress, $btnReport, $btnFolder, $lblScore))
$tabOverview.Controls.Add($pnlTop)

# ---------------- Tab 2: Processes ----------------
$lvProc = New-ListView @('Process', 'PID', 'CPU %', 'GPU %', 'Memory', 'What it is / advice', 'Location') @(160, 60, 60, 60, 80, 470, 300)
$tabProcs.Controls.Add($lvProc)
$pnlProc = New-Object System.Windows.Forms.Panel; $pnlProc.Dock = 'Top'; $pnlProc.Height = 52
$btnProcRefresh = New-Button 'Re-measure (5 s)' 10 10 140
$btnProcEnd     = New-Button 'End selected process' 160 10 160
$btnProcLower   = New-Button 'Lower its priority' 330 10 140
$btnProcOpen    = New-Button 'Open file location' 480 10 140
$lblProcHint = New-Object System.Windows.Forms.Label
$lblProcHint.Location = New-Object System.Drawing.Point(632, 8); $lblProcHint.Size = New-Object System.Drawing.Size(500, 40)
$lblProcHint.Text = 'Sorted by CPU use. Ending a process closes that program (unsaved work is lost). Windows system processes are protected and cannot be ended here.'
$pnlProc.Controls.AddRange(@($btnProcRefresh, $btnProcEnd, $btnProcLower, $btnProcOpen, $lblProcHint))
$tabProcs.Controls.Add($pnlProc)

# ---------------- Tab 3: Startup ----------------
$lvStart = New-ListView @('Program', 'Status', 'Advice', 'Where', 'Command') @(200, 80, 220, 190, 450)
$lvStart.MultiSelect = $true
$tabStartup.Controls.Add($lvStart)
$pnlStart = New-Object System.Windows.Forms.Panel; $pnlStart.Dock = 'Top'; $pnlStart.Height = 52
$btnStartDisable = New-Button 'Disable selected' 10 10 140
$btnStartEnable  = New-Button 'Enable selected' 160 10 140
$btnStartRefresh = New-Button 'Refresh list' 310 10 120
$lblStartHint = New-Object System.Windows.Forms.Label
$lblStartHint.Location = New-Object System.Drawing.Point(442, 8); $lblStartHint.Size = New-Object System.Drawing.Size(690, 40)
$lblStartHint.Text = 'Disabling only stops a program starting automatically when you sign in (same switch as Task Manager). Nothing is uninstalled and you can re-enable it here at any time. Ctrl+click to select several.'
$pnlStart.Controls.AddRange(@($btnStartDisable, $btnStartEnable, $btnStartRefresh, $lblStartHint))
$tabStartup.Controls.Add($pnlStart)

# ---------------- Tab 4: Tune-Up ----------------
$lvTune = New-ListView @('Tune-up', 'What it does', 'Undo?') @(270, 720, 80)
$lvTune.CheckBoxes = $true
$lvTune.Dock = 'Fill'
foreach ($a in $script:TuneActions) {
    $it = New-Object System.Windows.Forms.ListViewItem($a.Name)
    [void]$it.SubItems.Add($a.Desc); [void]$it.SubItems.Add($a.Undo)
    $it.Tag = $a.Key
    if ($a.Key -eq 'restorepoint') { $it.Checked = $true }
    [void]$lvTune.Items.Add($it)
}
$script:TuneLogBox = New-Object System.Windows.Forms.TextBox
$script:TuneLogBox.Multiline = $true; $script:TuneLogBox.ReadOnly = $true; $script:TuneLogBox.ScrollBars = 'Vertical'
$script:TuneLogBox.Dock = 'Bottom'; $script:TuneLogBox.Height = 200; $script:TuneLogBox.BackColor = [System.Drawing.Color]::White
$script:TuneLogBox.Font = New-Object System.Drawing.Font('Consolas', 9)
$tabTune.Controls.Add($lvTune)
$tabTune.Controls.Add($script:TuneLogBox)

$pnlTune = New-Object System.Windows.Forms.Panel; $pnlTune.Dock = 'Top'; $pnlTune.Height = 92
$btnTuneApply = New-Button 'Apply ticked tune-ups' 10 10 190 36
$btnTuneApply.Font = $fontBold
$btnTuneApply.BackColor = [System.Drawing.Color]::FromArgb(40, 150, 80); $btnTuneApply.ForeColor = [System.Drawing.Color]::White; $btnTuneApply.FlatStyle = 'Flat'
$btnTuneRec  = New-Button 'Tick recommended' 210 10 150 36
$btnTuneUndo = New-Button 'Undo my tune-ups' 370 10 150 36
$lblTuneHint = New-Object System.Windows.Forms.Label
$lblTuneHint.Location = New-Object System.Drawing.Point(10, 52); $lblTuneHint.Size = New-Object System.Drawing.Size(1110, 38)
$lblTuneHint.Text = 'About fan speed: on almost all PCs the fan is controlled by the BIOS/firmware and Windows software cannot set it directly. This tool quietens the fan by reducing heat - less processor load, sensible power settings and less background work. After a scan, recommended tune-ups are ticked automatically.'
$pnlTune.Controls.AddRange(@($btnTuneApply, $btnTuneRec, $btnTuneUndo, $lblTuneHint))
$tabTune.Controls.Add($pnlTune)

$form.Controls.Add($tabs)
$form.Controls.Add($status)

# ---------------------------------------------------------------------------
#  UI population
# ---------------------------------------------------------------------------
function Show-Findings {
    $lvFind.BeginUpdate()
    $lvFind.Items.Clear()
    foreach ($f in (Get-SortedFindings)) {
        $it = New-Object System.Windows.Forms.ListViewItem($f.Severity)
        [void]$it.SubItems.Add($f.Area); [void]$it.SubItems.Add($f.Title); [void]$it.SubItems.Add($f.Recommendation)
        $it.BackColor = $colors[$f.Severity]
        $it.Tag = $f
        [void]$lvFind.Items.Add($it)
    }
    $lvFind.EndUpdate()
}

function Show-Processes($Measure) {
    $lvProc.BeginUpdate()
    $lvProc.Items.Clear()
    $sorted = @($Measure.Rows) | Sort-Object @{ Expression = { if ($null -ne $_.Cpu) { $_.Cpu } else { -1 } }; Descending = $true }, @{ Expression = { $_.MemBytes }; Descending = $true }
    foreach ($p in ($sorted | Select-Object -First 60)) {
        $it = New-Object System.Windows.Forms.ListViewItem($p.Name)
        [void]$it.SubItems.Add([string]$p.Id)
        [void]$it.SubItems.Add($(if ($null -ne $p.Cpu) { '{0:N1}' -f $p.Cpu } else { '?' }))
        [void]$it.SubItems.Add($(if ($p.Gpu -gt 0) { '{0:N0}' -f $p.Gpu } else { '' }))
        [void]$it.SubItems.Add((Format-Bytes $p.MemBytes))
        [void]$it.SubItems.Add([string]$p.Note)
        [void]$it.SubItems.Add([string]$p.Path)
        if ($null -ne $p.Cpu -and $p.Cpu -ge 15) { $it.BackColor = $colors.Critical }
        elseif ($null -ne $p.Cpu -and $p.Cpu -ge 5) { $it.BackColor = $colors.Warning }
        $it.Tag = $p
        [void]$lvProc.Items.Add($it)
    }
    $lvProc.EndUpdate()
}

function Show-Startup($Items) {
    $lvStart.BeginUpdate()
    $lvStart.Items.Clear()
    foreach ($st in ($Items | Sort-Object @{ Expression = { -not $_.Enabled } }, Name)) {
        $it = New-Object System.Windows.Forms.ListViewItem($st.Name)
        [void]$it.SubItems.Add($(if ($st.Enabled) { 'Enabled' } else { 'Disabled' }))
        [void]$it.SubItems.Add($st.Advice); [void]$it.SubItems.Add($st.Source); [void]$it.SubItems.Add($st.Command)
        if (-not $st.Enabled) { $it.ForeColor = [System.Drawing.Color]::Gray }
        elseif ($st.Advice -like 'Usually safe*') { $it.BackColor = $colors.Warning }
        elseif ($st.Advice -like 'Keep*') { $it.BackColor = $colors.Good }
        $it.Tag = $st
        [void]$lvStart.Items.Add($it)
    }
    $lvStart.EndUpdate()
}

function Select-RecommendedTuneUps {
    $rec = @{ 'restorepoint' = $true }
    foreach ($f in $script:Findings) { foreach ($k in $f.Fix) { if ($k) { $rec[$k] = $true } } }
    foreach ($it in $lvTune.Items) { $it.Checked = $rec.ContainsKey([string]$it.Tag) }
}

# ---------------------------------------------------------------------------
#  Event handlers
# ---------------------------------------------------------------------------
$btnScan.Add_Click({
    $btnScan.Enabled = $false; $btnTuneApply.Enabled = $false
    $form.Cursor = [System.Windows.Forms.Cursors]::WaitCursor
    $lblScore.Text = ''
    $txtFan.Text = 'Scanning... please keep using the PC as normal (or leave it idle) while the measurements are taken.'
    try {
        Invoke-SystemScan -Progress { param($pct, $msg) $progress.Value = [math]::Min(100, [math]::Max(0, $pct)); $lblProgress.Text = $msg; [System.Windows.Forms.Application]::DoEvents() }
        $s = $script:Scan
        Show-Findings
        Show-Processes $s.Measure
        Show-Startup $s.Startup
        Select-RecommendedTuneUps
        $lblScore.Text = "$($s.Score)/100"
        $lblScore.ForeColor = if ($s.Score -ge 80) { [System.Drawing.Color]::ForestGreen } elseif ($s.Score -ge 55) { [System.Drawing.Color]::DarkOrange } else { [System.Drawing.Color]::Firebrick }
        $nl = [Environment]::NewLine
        $hdr = "WHY IS THE FAN LOUD?   (CPU {0:N0}%  |  GPU {1}  |  Temp {2}  |  Power plan: {3}  |  {4})" -f $s.Measure.TotalCpu,
            $(if ($null -ne $s.Measure.GpuOverall) { '{0:N0}%' -f $s.Measure.GpuOverall } else { 'n/a' }),
            $(if ($null -ne $s.MaxTemp) { '{0:N0} °C' -f $s.MaxTemp } else { 'not exposed' }), $s.Power.Name,
            $(if ($s.IsLaptop) { "laptop, $(if ($s.OnBattery) { 'on battery' } else { 'plugged in' })" } else { 'desktop' })
        $body = ($s.FanReasons | ForEach-Object { ' - ' + $_ }) -join $nl
        $txtFan.Text = $hdr + $nl + $nl + $body + $nl + $nl + 'Next: review the findings below, then go to tab 4 (Tune-Up) - the recommended fixes are already ticked.'
        $lblProgress.Text = ('Scan complete: {0} findings. Health score {1}/100.' -f $script:Findings.Count, $s.Score)
    } catch {
        $lblProgress.Text = 'Scan error: ' + $_.Exception.Message
        $txtFan.Text = "Something went wrong during the scan:`r`n$($_.Exception.Message)`r`n$($_.InvocationInfo.PositionMessage)"
    } finally {
        $btnScan.Enabled = $true; $btnTuneApply.Enabled = $true
        $form.Cursor = [System.Windows.Forms.Cursors]::Default
    }
})

$lvFind.Add_SelectedIndexChanged({
    if ($lvFind.SelectedItems.Count -eq 0) { return }
    $f = $lvFind.SelectedItems[0].Tag
    $nl = [Environment]::NewLine
    $t = "[$($f.Severity)] $($f.Title)$nl"
    if ($f.Detail) { $t += "${nl}Details: $($f.Detail)$nl" }
    if ($f.Recommendation) { $t += "${nl}What to do: $($f.Recommendation)" }
    $txtDetail.Text = $t
})

$btnReport.Add_Click({ Export-Report })
$btnFolder.Add_Click({ Invoke-Item -LiteralPath $script:DataDir })

$btnProcRefresh.Add_Click({
    $btnProcRefresh.Enabled = $false; $form.Cursor = [System.Windows.Forms.Cursors]::WaitCursor
    try {
        $m = Measure-Processes -Seconds 5 -OnTick { param($i, $n) $btnProcRefresh.Text = "Measuring... $($i + 1)/$n" }
        Show-Processes $m
    } finally { $btnProcRefresh.Text = 'Re-measure (5 s)'; $btnProcRefresh.Enabled = $true; $form.Cursor = [System.Windows.Forms.Cursors]::Default }
})

$btnProcEnd.Add_Click({
    if ($lvProc.SelectedItems.Count -eq 0) { [System.Windows.Forms.MessageBox]::Show('Select a process first.', $script:AppName) | Out-Null; return }
    $p = $lvProc.SelectedItems[0].Tag
    if ($p.IsSelf -or ($script:ProtectedProcesses -contains $p.Name.ToLower())) {
        [System.Windows.Forms.MessageBox]::Show("$($p.Name) is a Windows system process and is protected. Ending it could crash Windows.", $script:AppName, 'OK', 'Warning') | Out-Null; return
    }
    $r = [System.Windows.Forms.MessageBox]::Show("End '$($p.Name)' (PID $($p.Id))?`n`nThe program will close immediately and any unsaved work in it will be lost.", $script:AppName, 'YesNo', 'Question')
    if ($r -ne 'Yes') { return }
    try { Stop-Process -Id $p.Id -Force -ErrorAction Stop; $lvProc.Items.Remove($lvProc.SelectedItems[0]); Write-TuneLog "Ended process $($p.Name) (PID $($p.Id))." }
    catch { [System.Windows.Forms.MessageBox]::Show("Could not end it: $($_.Exception.Message)", $script:AppName) | Out-Null }
})

$btnProcLower.Add_Click({
    if ($lvProc.SelectedItems.Count -eq 0) { [System.Windows.Forms.MessageBox]::Show('Select a process first.', $script:AppName) | Out-Null; return }
    $p = $lvProc.SelectedItems[0].Tag
    try {
        (Get-Process -Id $p.Id -ErrorAction Stop).PriorityClass = [Diagnostics.ProcessPriorityClass]::BelowNormal
        [System.Windows.Forms.MessageBox]::Show("$($p.Name) now runs at Below Normal priority, so your other programs get the processor first. This lasts until that program restarts.", $script:AppName) | Out-Null
        Write-TuneLog "Lowered priority of $($p.Name) (PID $($p.Id))."
    } catch { [System.Windows.Forms.MessageBox]::Show("Could not change priority: $($_.Exception.Message)", $script:AppName) | Out-Null }
})

$btnProcOpen.Add_Click({
    if ($lvProc.SelectedItems.Count -eq 0) { return }
    $p = $lvProc.SelectedItems[0].Tag
    if ($p.Path -and (Test-Path -LiteralPath $p.Path)) { Start-Process -FilePath 'explorer.exe' -ArgumentList "/select,`"$($p.Path)`"" }
    else { [System.Windows.Forms.MessageBox]::Show('The file location is not available for this process.', $script:AppName) | Out-Null }
})

function Set-SelectedStartup([bool]$Enable) {
    if ($lvStart.SelectedItems.Count -eq 0) { [System.Windows.Forms.MessageBox]::Show('Select one or more startup programs first.', $script:AppName) | Out-Null; return }
    $undo = Read-Undo
    $list = New-Object System.Collections.ArrayList
    foreach ($u in @($undo.Startup)) { [void]$list.Add($u) }
    $errors = @()
    foreach ($it in @($lvStart.SelectedItems)) {
        $st = $it.Tag
        if (-not $Enable -and $st.Advice -like 'Keep*') {
            $r = [System.Windows.Forms.MessageBox]::Show("'$($st.Name)' looks like security or hardware software ($($st.Advice)). Disable it anyway?", $script:AppName, 'YesNo', 'Warning')
            if ($r -ne 'Yes') { continue }
        }
        try {
            Set-StartupState $st.ApprovedKey $st.ValueName $Enable
            $st.Enabled = $Enable
            if ($Enable) {
                $keep = @($list | Where-Object { -not ($_.ApprovedKey -eq $st.ApprovedKey -and $_.ValueName -eq $st.ValueName) })
                $list.Clear(); foreach ($x in $keep) { [void]$list.Add($x) }
                Write-TuneLog "Enabled startup program: $($st.Name)"
            } else {
                [void]$list.Add(@{ ApprovedKey = $st.ApprovedKey; ValueName = $st.ValueName; Name = $st.Name })
                Write-TuneLog "Disabled startup program: $($st.Name)"
            }
        } catch {
            $msg = $_.Exception.Message
            if (-not $script:IsAdmin -and $st.ApprovedKey -like 'HKLM:*') { $msg = 'Administrator rights needed (start the tool with "Start PC TuneUp.cmd" and accept the prompt).' }
            $errors += "$($st.Name): $msg"
        }
    }
    $undo.Startup = @($list)
    Save-Undo $undo
    if ($script:Scan) { $script:Scan.Startup = @(Get-StartupItems); Show-Startup $script:Scan.Startup } else { Show-Startup @(Get-StartupItems) }
    if ($errors.Count -gt 0) { [System.Windows.Forms.MessageBox]::Show(($errors -join "`n"), $script:AppName, 'OK', 'Warning') | Out-Null }
}

$btnStartDisable.Add_Click({ Set-SelectedStartup $false })
$btnStartEnable.Add_Click({ Set-SelectedStartup $true })
$btnStartRefresh.Add_Click({ Show-Startup @(Get-StartupItems) })

$btnTuneRec.Add_Click({
    if (-not $script:Scan) { [System.Windows.Forms.MessageBox]::Show('Run a scan first (tab 1) so the tool knows what to recommend.', $script:AppName) | Out-Null; return }
    Select-RecommendedTuneUps
})

$btnTuneApply.Add_Click({
    $keys = @(); $names = @()
    foreach ($it in $lvTune.Items) { if ($it.Checked) { $keys += [string]$it.Tag; $names += ' - ' + $it.Text } }
    if ($keys.Count -eq 0) { [System.Windows.Forms.MessageBox]::Show('Tick at least one tune-up first.', $script:AppName) | Out-Null; return }
    if (-not $script:IsAdmin) {
        [System.Windows.Forms.MessageBox]::Show("Tune-ups need administrator rights.`n`nClose this window and start the tool with 'Start PC TuneUp.cmd', then click Yes on the Windows prompt.", $script:AppName, 'OK', 'Warning') | Out-Null; return
    }
    $r = [System.Windows.Forms.MessageBox]::Show("Apply these tune-ups?`n`n" + ($names -join "`n") + "`n`nSettings changes can be reversed with 'Undo my tune-ups'.", $script:AppName, 'YesNo', 'Question')
    if ($r -ne 'Yes') { return }
    $btnTuneApply.Enabled = $false; $form.Cursor = [System.Windows.Forms.Cursors]::WaitCursor
    try { Write-TuneLog '----- Applying tune-ups -----'; Invoke-TuneUps $keys }
    catch { Write-TuneLog "Error: $($_.Exception.Message)" }
    finally { $btnTuneApply.Enabled = $true; $form.Cursor = [System.Windows.Forms.Cursors]::Default }
})

$btnTuneUndo.Add_Click({
    if (-not $script:IsAdmin) { [System.Windows.Forms.MessageBox]::Show('Undo needs administrator rights. Start the tool with "Start PC TuneUp.cmd".', $script:AppName) | Out-Null; return }
    $r = [System.Windows.Forms.MessageBox]::Show("Restore the power plan, processor limits and startup programs that PC TuneUp changed?", $script:AppName, 'YesNo', 'Question')
    if ($r -ne 'Yes') { return }
    Write-TuneLog '----- Undoing tune-ups -----'
    Invoke-UndoTuneUps
    if ($script:Scan) { $script:Scan.Startup = @(Get-StartupItems); Show-Startup $script:Scan.Startup }
})

$form.Add_Shown({
    $form.Activate()
    Show-Startup @(Get-StartupItems)
    if (Test-Path -LiteralPath $script:LogPath) { Write-TuneLog "PC TuneUp started. Previous activity is in $($script:LogPath)" } else { Write-TuneLog 'PC TuneUp started.' }
})

[void]$form.ShowDialog()
$form.Dispose()
