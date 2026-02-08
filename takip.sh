#!/bin/bash

# --- AYARLAR ---
HEDEF_KLASOR="/storage/emulated/0/Download/MyProjects"
APK_ISMI="KeyboardTrigger_Final.apk"
GECICI_KLASOR="temp_apk_indir"

echo "👀 GitHub Kontrol Ediliyor..."

# 1. Önce devam eden işlem var mı diye bak
gh run watch --exit-status

# NOT: 'watch' komutu işlem yoksa hata kodu döndürür, ama bu bir sorun değil.
# Biz her durumda indirmeyi deneyeceğiz.

echo "⬇️ En son başarılı APK indiriliyor..."

# 2. Temizlik
rm -rf "$GECICI_KLASOR"
mkdir -p "$GECICI_KLASOR"

# 3. İndir (Ne varsa getir)
gh run download --dir "$GECICI_KLASOR"

# 4. APK Kontrolü
APK_BULUNDU=$(find "$GECICI_KLASOR" -name "*.apk" -print -quit)

if [ -n "$APK_BULUNDU" ]; then
    echo "✅ APK BULUNDU!"
    
    # Hedefe Taşı
    mv "$APK_BULUNDU" "$HEDEF_KLASOR/$APK_ISMI"
    
    # Temizlik
    rm -rf "$GECICI_KLASOR"
    
    echo "---------------------------------------------------"
    echo "🎉 APK HAZIR: $HEDEF_KLASOR/$APK_ISMI"
    echo "👉 Dosya Yöneticisinden kur."
    echo "---------------------------------------------------"
    
    echo "⚠️ LOG KAYDI BAŞLATILIYOR (Durdurmak için CTRL+C)..."
    echo "Lütfen uygulamayı aç, butona bas ve hataları izle."
    echo "3 saniye içinde başlıyor..."
    sleep 3
    
    # Logları temizle ve başlat
    logcat -c && logcat -v time -s "FloatingService" "AndroidRuntime" "System.err"

else
    echo "❌ HATA: APK İndirilemedi veya Derleme Başarısız Oldu."
    echo "Sebep: Derleme hatası olabilir. Loglara bakılıyor:"
    gh run view --log-failed
fi
