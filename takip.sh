#!/bin/bash

# --- AYARLAR ---
HEDEF_KLASOR="/storage/emulated/0/Download/MyProjects"
APK_ISMI="KeyboardTrigger_Final.apk"
GECICI_KLASOR="temp_apk_indir"

echo "👀 GitHub üzerinde derleme izleniyor..."
echo "-------------------------------------"

# 1. Derlemeyi İzle (Hata varsa dur)
gh run watch --exit-status

if [ $? -eq 0 ]; then
    echo "✅ DERLEME BAŞARILI! Dosya indiriliyor..."
    
    # 2. Temizlik Yap (Eski kalıntıları sil)
    rm -rf "$GECICI_KLASOR"
    mkdir -p "$GECICI_KLASOR"
    
    # 3. Dosyaları Geçici Klasöre İndir (İsim sormadan ne varsa indirir)
    # --dir parametresi ile dosyayı nereye koyacağını biz emrediyoruz.
    gh run download --dir "$GECICI_KLASOR"
    
    echo "📦 Dosyalar taraniyor..."

    # 4. APK Dosyasını BUL ve TAŞI (En Kritik Adım)
    # find komutu o klasörün altındaki tüm delikleri arar, apk'yı bulur.
    APK_BULUNDU=$(find "$GECICI_KLASOR" -name "*.apk" -print -quit)
    
    if [ -n "$APK_BULUNDU" ]; then
        echo "🎯 APK Bulundu: $APK_BULUNDU"
        mv "$APK_BULUNDU" "$HEDEF_KLASOR/$APK_ISMI"
        
        echo "---------------------------------------------------"
        echo "🎉 İŞLEM TAMAM! APK ŞURADA:"
        echo "📂 $HEDEF_KLASOR/$APK_ISMI"
        echo "---------------------------------------------------"
        
        # Geçici klasörü sil
        rm -rf "$GECICI_KLASOR"
        
        echo "⚠️ Şimdi Dosya Yöneticisinden APK'yı kur."
        echo "Logları izlemek için ENTER'a bas (Çıkış: CTRL+C)"
        read
        
        # 5. Logları Başlat
        echo "🕵️‍♂️ LOG KAYDI BAŞLIYOR..."
        logcat -c && logcat -v time -s "FloatingService" "AndroidRuntime" "System.err"
    else
        echo "❌ HATA: İndirilenlerin içinde .apk dosyası bulunamadı!"
        ls -R "$GECICI_KLASOR"
    fi

else
    echo "❌ DERLEME HATASI OLUŞTU!"
    echo "Loglara bakılıyor..."
    gh run view --log-failed
fi
