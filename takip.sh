#!/bin/bash
gh run watch --exit-status
if [ $? -eq 0 ]; then
    echo "✅ BAŞARILI! İndiriliyor..."
    rm -rf KeyboardTrigger-APK
    gh run download -n KeyboardTrigger-APK
    find . -name "*.apk" -exec mv {} ../KeyboardTrigger_Final.apk \;
    rm -rf KeyboardTrigger-APK
    echo "🎉 APK: ../KeyboardTrigger_Final.apk"
else
    echo "❌ HATA LOGLARI:"
    gh run view --log-failed
fi
