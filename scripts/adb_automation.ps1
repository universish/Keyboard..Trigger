# ADB Automation script for Keyboard Trigger
# Usage: Run in PowerShell from project root. Requires adb on PATH and connected device with adb.

$ErrorActionPreference = 'Stop'

# Paths
$APK = "app\build\outputs\apk\debug\app-debug.apk"
$Pkg = "com.universish.libre.keyboardtrigger"
$AccService = "$Pkg/$Pkg.KeyboardTriggerAccessibilityService"
$LogDir = "$PSScriptRoot\logs"
New-Item -Path $LogDir -ItemType Directory -Force | Out-Null
$ts = Get-Date -Format "yyyyMMdd_HHmmss"
$LogFile = Join-Path $LogDir "log_$ts.txt"

# Detect adb executable: prefer $env:ADB, then C:\platform-tools\adb.exe, else assume 'adb' on PATH
if ($env:ADB -and (Test-Path $env:ADB)) {
    $adbExe = $env:ADB
} elseif (Test-Path "C:\\platform-tools\\adb.exe") {
    $adbExe = "C:\\platform-tools\\adb.exe"
} else {
    $adbExe = "adb"
}
Write-Host "Using adb: $adbExe"

function Run-GradleBuild {
    Write-Host "Building project (gradlew)..."
    & .\gradlew.bat clean assembleDebug
}

function Install-APK {
    if (-Not (Test-Path $APK)) { throw "APK bulunamadı: $APK" }
    Write-Host "Installing APK..."
    & $adbExe install -r $APK
}

function Grant-Overlay {
    Write-Host "Granting overlay (appops)..."
    & $adbExe shell appops set $Pkg SYSTEM_ALERT_WINDOW allow
}

function Enable-Accessibility {
    Write-Host "Enabling Accessibility service ($AccService)..."
    $existing = (& $adbExe shell settings get secure enabled_accessibility_services) -join "\n" | Out-String
    $existing = $existing.Trim()
    if ($existing -eq "null" -or $existing -eq "") {
        & $adbExe shell settings put secure enabled_accessibility_services $AccService
    } elseif ($existing -notlike "*$AccService*") {
        $new = $existing + ":" + $AccService
        & $adbExe shell settings put secure enabled_accessibility_services "$new"
    } else {
        Write-Host "Accessibility servisi zaten etkin görünuyor"
    }
    & $adbExe shell settings put secure accessibility_enabled 1
}

function Start-FloatingService {
    Write-Host "Starting FloatingService (foreground)..."
    & $adbExe shell am start-foreground-service -n $Pkg/.$("FloatingService")
}

function Start-LogcatAndWait {
    Write-Host "Starting logcat -> $LogFile"
    $proc = Start-Process -FilePath $adbExe -ArgumentList "logcat -v time FloatingService:* AccessibilityService:* KeyboardShowActivity:* *:S" -RedirectStandardOutput $LogFile -NoNewWindow -PassThru
    Write-Host "Cihazda test et. Test tamamlanınca bu terminale dön ve ENTER'a bas." -ForegroundColor Yellow
    [void][System.Console]::ReadLine()
    Write-Host "Stopping logcat..."
    $proc | Stop-Process -Force
    Write-Host "Log kaydedildi: $LogFile"
    return $LogFile
}

function Analyze-Log {
    param([string]$path)
    Write-Host "Hataları arıyorum (FATAL EXCEPTION ve Exception)..." -ForegroundColor Cyan
    Select-String -Path $path -Pattern "FATAL EXCEPTION|Exception" -CaseSensitive | Select-Object -First 200
}

function Uninstall-App {
    Write-Host "Uninstalling app..."
    & $adbExe uninstall $Pkg
}

try {
    Run-GradleBuild
    Install-APK
    Grant-Overlay
    Enable-Accessibility
    Start-FloatingService
    $log = Start-LogcatAndWait
    Analyze-Log -path $log
    Write-Host "İşlem tamamlandı. İstersen logu bu makinede inceleyebilir veya bana yapıştırabilirsin." -ForegroundColor Green
} catch {
    Write-Host "Hata: $_" -ForegroundColor Red
    exit 1
}
