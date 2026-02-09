$log = Join-Path $PSScriptRoot "logs\log_$(Get-Date -Format 'yyyyMMdd_HHmmss').txt"
$p = Start-Process -FilePath 'C:\platform-tools\adb.exe' -ArgumentList "logcat -v time FloatingService:* AccessibilityService:* KeyboardShowActivity:* *:S" -RedirectStandardOutput $log -NoNewWindow -PassThru
$p.Id | Out-File -FilePath (Join-Path $PSScriptRoot 'logs\log_pid.txt') -Encoding ascii
Write-Host "Log started: $log"
Start-Sleep -Milliseconds 300
& 'C:\platform-tools\adb.exe' shell am start -n com.universish.libre.keyboardtrigger/.MainActivity
Write-Host "MainActivity started. Test on device, then press ENTER here to stop logging."
[void][System.Console]::ReadLine()
$procId = Get-Content (Join-Path $PSScriptRoot 'logs\log_pid.txt') -Raw
Stop-Process -Id $procId -ErrorAction SilentlyContinue
Write-Host "Stopped. Log saved to: $log"