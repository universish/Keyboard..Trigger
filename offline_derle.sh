#!/bin/bash
echo "🏭 Fabrika Çalışıyor: Yerel Derleme Başladı..."

# Android SDK yeri (Termux için standart değil, o yüzden Java ile halledeceğiz)
# Termux'ta tam Android SDK kurmak zordur.
# O YÜZDEN: Biz sadece Java ve Gradle kullanarak 'Assemble' yapacağız.

# 1. Önce temizlik
./gradlew clean

# 2. Derleme (Debug sürümü)
# --no-daemon: Telefon RAM'ini şişirmesin diye servisi sürekli açık tutma
echo "🔨 İnşa ediliyor (Bu işlem telefon hızına göre 1-3 dk sürebilir)..."
./gradlew assembleDebug --no-daemon

# 3. Sonuç Kontrolü
if [ $? -eq 0 ]; then
    echo "✅ BAŞARILI! APK Oluşturuldu."
    
    # APK'yı kolay bulunan yere kopyala
    cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/KeyboardTrigger_Local.apk
    
    echo "📂 APK Şurada: İndirilenler > KeyboardTrigger_Local.apk"
    echo "Hemen kurup test edebilirsin!"
else
    echo "❌ HATA: Derleme başarısız oldu."
    echo "Yukarıdaki hata mesajlarını oku."
fi
