# Otomasyon scripti: derle, yükle, test et, log analiz et

# 1. Derle
Write-Host "Derleme baslatiliyor..."
./gradlew assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Host "Derleme hatasi!"
    exit 1
}

# 2. APK'yi bul
$apkPath = "app/build/outputs/apk/debug/app-debug.apk"
if (!(Test-Path $apkPath)) {
    Write-Host "APK bulunamadi!"
    exit 1
}

# 3. ADB ile yükle (cihaz bagli olmali)
Write-Host "APK yukleniyor..."
adb install -r $apkPath
if ($LASTEXITCODE -ne 0) {
    Write-Host "Yukleme hatasi!"
    exit 1
}

# 4. Logcat baslat (arka planda)
Write-Host "Log kaydi baslatiliyor..."
$logJob = Start-Job -ScriptBlock {
    adb logcat -c
    adb logcat > logcat_output.txt
}

# 5. Uygulamayi baslat
Write-Host "Uygulama baslatiliyor..."
adb shell am start -n com.universish.libre.keyboardtrigger/.MainActivity

# 6. Test icin bekle
Write-Host "Test icin 30 saniye bekleniyor... Butona tiklayin."
Start-Sleep -Seconds 30

# 7. Log durdur
Write-Host "Log kaydi durduruluyor..."
Stop-Job $logJob
Receive-Job $logJob

# 8. Hatalari analiz et
Write-Host "Hatalar araniyor..."
$errors = Select-String -Path logcat_output.txt -Pattern "ERROR|FATAL|Exception" -CaseSensitive:$false
if ($errors) {
    Write-Host "Hatalar bulundu:"
    $errors | ForEach-Object { Write-Host $_.Line }
    Write-Host "Hatalari duzeltmek icin kodlari kontrol edin."
} else {
    Write-Host "Hata bulunmadi."
}

# 9. Push hazirla
Write-Host "Degisiklikleri push etmek icin hazir."