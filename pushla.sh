#!/bin/bash
echo "📤 Kodlar GitHub'a gönderiliyor..."

# Bu klasördeki değişiklikleri al
git add .

# Mesajla kaydet
git commit -m "Update from Acode: $(date)"

# Gönder
git push

echo "✅ Kodlar Buluta Gitti! Derleme Durumu:"
# İzlemeye başla
gh run watch
