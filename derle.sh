#!/bin/bash
echo "🔨 Derleme Başlıyor..."

# 1. Gradle ile derle (Offline modu ve Daemon kullanarak hızlandırıyoruz)
# İlk derleme uzun sürer, sonrakiler saniyeler sürer.
./gradlew assembleDebug --no-daemon

if [ $? -eq 0 ]; then
    echo "✅ Derleme Başarılı! APK Kopyalanıyor..."
    # APK'yı ana dizine çek ki kolay bul
    cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/KeyboardTrigger.apk
    echo "🚀 APK Hazır: /sdcard/Download/KeyboardTrigger.apk"
    
    # Otomatik kurmak istersen (Termux-API gerekir, şimdilik manuel yapalım)
    termux-open /sdcard/Download/KeyboardTrigger.apk
else
    echo "❌ HATA: Derleme başarısız oldu. Acode'da kodu kontrol et."
fi
