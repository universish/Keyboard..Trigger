#!/bin/bash

REPO="universish/Keyboard..Trigger"
WORKDIR="/storage/emulated/0/Download/MyProjects"
PACKAGE="com.universish.libre.keyboardtrigger"
WORKFLOW="Android Build"

step() { echo -e "\n\033[1;34m⮞ $1\033[0m"; }

# 1. Son build job veya yeni run ID öğren
step "1) Son build job ID öğreniliyor..."
RUNID=$(gh run list --repo "$REPO" --workflow "$WORKFLOW" --limit 1 --json databaseId --jq '.[0].databaseId')
if [ -z "$RUNID" ]; then
    echo "Run ID bulunamadı veya workflow ad�� yanlış."
    exit 1
fi
echo "Son run ID: $RUNID"

# 2. Logcat başlatılır
LOGFILE="$WORKDIR/apk_test_log_$(date +%Y%m%d_%H%M%S).txt"
step "2) Logcat kaydı başlatılıyor ($LOGFILE)..."
logcat -c
logcat -v time > "$LOGFILE" &
LOGPID=$!
echo "Logcat PID: $LOGPID"

# 3. Artifact indir (temiz bir alt klasör!)
APKDIR="$WORKDIR/run_$RUNID"
rm -rf "$APKDIR"
mkdir -p "$APKDIR"
step "3) APK artifact indiriliyor..."
gh run download $RUNID --repo "$REPO" --dir "$APKDIR"
APKPATH=$(find "$APKDIR" -type f -name "*.apk" | head -n1)
if [ -n "$APKPATH" ]; then
    echo "APK bulundu: $APKPATH"
else
    echo "APK bulunamadı!"
    kill $LOGPID
    exit 1
fi

# 4. Test: APK'yı yükle & seni bekle
step "4) Şimdi APK'yı yükleyip testi gerçekleştir. Bitince [Enter] tuşuna bas."
echo " (İPUCU: APK yükleme için:  adb install -r \"$APKPATH\" ya da sistemle aç.)"
read -n 1 -s -r -p "-- Test bittiğinde devam etmek için bir tuşa basın..."; echo

# 5. Logcat kaydını durdur & hataları topla
step "5) Logcat kaydı durduruluyor; hatalar çekiliyor..."
kill $LOGPID
sleep 1
ERRORLOG="$WORKDIR/error_log_$RUNID.txt"
grep -Ei 'exception|error|fatal|AndroidRuntime|keyboardtrigger' "$LOGFILE" > "$ERRORLOG"

if [ -s "$ERRORLOG" ]; then
    echo -e "\033[1;31m\n\U1F534 Hatalar bulundu, üstteki dosyada listelendi:\033[0m"
    cat "$ERRORLOG"
else
    echo -e "\033[1;32m\n\U2705 Hata bulunamadı! (error_log_$RUNID.txt boş)\033[0m"
fi

# 6. Hataları sana gösterecek ve bekleyecek
step "6) Hataları kopyalayıp asistanına yollayabilirsin. Devam için [Enter]."
read -n 1 -s -r -p "-- Kopyalayıp AI'ye yapıştır, devam etmek için bas."; echo

# 7. Hata düzeltme süreci: cat ile dosyaları güncelle
step "7) Asistan yeni kod önerirse, aşağıda örnekte olduğu gibi cat ile dosyayı değiştir:"
echo 'cat <<EOF > app/src/main/java/com/universish/libre/keyboardtrigger/MainActivity.kt'
echo '-- (BURAYA KOD YAPIŞTIR) --'
echo 'EOF'
echo -e "\nBitince devam etmek için [Enter]."
read -n 1 -s -r -p "-- Kodlar güncellendiyse devam etmek için bas."; echo

# 8. Git push işlemi
step "8) GIT ile push işlemi başlatılıyor..."
cd "$WORKDIR/Keyboard..Trigger" || { echo "Repo klasörü bulunamadı!"; exit 1; }
git add . && git commit -am "Otomatik hata düzeltme ve güncelleme" && git push

# 9. Workflow durumu ve logları
step "9) Derleme başlatıldı. Run ID alınacak, durum bekleniyor..."
sleep 10
NEW_RUNID=$(gh run list --repo "$REPO" --workflow "$WORKFLOW" --limit 1 --json databaseId --jq '.[0].databaseId')
echo "Yeni run ID: $NEW_RUNID"

step "10) Derleme sonucu bekleniyor; durum kontrol ediliyor."
gh run watch "$NEW_RUNID" --repo "$REPO"
STATUS=$(gh run view "$NEW_RUNID" --repo "$REPO" --json conclusion --jq '.conclusion')

if [[ "$STATUS" == "success" ]]; then
    step "11) Derleme BAŞARILI. Yeni APK indirilecek."
    DOWNLOADDIR="$WORKDIR/run_$NEW_RUNID"
    rm -rf "$DOWNLOADDIR"
    mkdir -p "$DOWNLOADDIR"
    gh run download "$NEW_RUNID" --repo "$REPO" --dir "$DOWNLOADDIR"
    APKPATHNEW=$(find "$DOWNLOADDIR" -type f -name "*.apk" | head -n1)
    echo "Yeni APK bulundu: $APKPATHNEW"
    echo "Yüklemek için: adb install -r \"$APKPATHNEW\""
else
    step "11) Derleme başarısız oldu. Hatalar gösteriliyor..."
    BUILDLOG="$WORKDIR/build_log_$NEW_RUNID.txt"
    gh run view "$NEW_RUNID" --repo "$REPO" --log > "$BUILDLOG"
    echo "Derleme hatalarını görmek için:"
    grep -i "error\|exception\|fail" "$BUILDLOG"
fi

step "⮞ Otomasyon döngüsü tamamlandı. Sonraki düzeltmeler için yukarıdaki adımlar tekrar edilebilir!"
