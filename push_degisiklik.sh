#!/bin/bash
WORKDIR="/storage/emulated/0/Download/MyProjects/Keyboard..Trigger"

echo
echo -e "⮞ GIT push öncesi değişiklik yapılacak dizin: \033[1;32m$WORKDIR\033[0m"
cd "$WORKDIR" || { echo "Repo dizini bulunamadı!"; exit 1; }

git status
echo
read -p "Yukarıdaki değişiklikleri git'e push etmek istiyor musun? (y/n): " cevap
if [ "$cevap" == "y" ]; then
    git add .
    git commit -am "AI destekli hata düzeltme/güncelleme"
    git push
    echo "✅ Değişiklikler Github'a push edildi."
else
    echo "İşlem iptal edildi, push yapılmadı."
fi
