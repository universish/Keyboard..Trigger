#!/bin/bash
echo "🕵️‍♂️ SON HATA RAPORU OLUŞTURULUYOR..."
echo "-----------------------------------------"

# Logcat'ten son hataları çekiyoruz (-d: dump, -t: son satırlar)
# Şunları arıyoruz:
# 1. Uygulamanın adı (com.universish...)
# 2. FATAL EXCEPTION (Uygulamanın çökme anı)
# 3. AndroidRuntime (Sistemin hata mesajı)
# 4. System.err (Java hata çıktıları)

logcat -d -v time | grep -E "FATAL|AndroidRuntime|System.err|com.universish.libre.keyboardtrigger" | tail -n 50

echo "-----------------------------------------"
echo "📋 YUKARIDAKİ KISMI KOPYALA VE BANA YAPIŞTIR."
