#!/bin/bash
echo "🚑 KURTARMA OPERASYONU BAŞLADI..."

# 1. Sorun çıkaran dosyaları geçici olarak sil (Gradle kafası karışmasın)
rm -f build.gradle settings.gradle
rm -rf .gradle

# 2. Şimdi ortam temizken Wrapper'ı oluştur
# Hata verecek dosya olmadığı için bu komut çalışacak.
echo "🎁 Gradle Wrapper (v8.2) indiriliyor..."
gradle wrapper --gradle-version 8.2 --distribution-type all

# 3. Dosyaları TEKRAR OLUŞTUR (Wrapper oluştuktan sonra)
echo "📝 Ayar dosyaları yeniden yazılıyor..."

# -- settings.gradle --
cat <<SETTINGS > settings.gradle
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "KeyboardTrigger"
include ':app'
SETTINGS

# -- build.gradle (Root) --
cat <<ROOT > build.gradle
buildscript {
    ext.kotlin_version = '1.9.0'
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.1.0'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:\$kotlin_version"
    }
}
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
task clean(type: Delete) {
    delete rootProject.buildDir
}
ROOT

# 4. Çevre Değişkenlerini Garantile (SDK Yolu)
export ANDROID_HOME=$HOME/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# 5. DERLEME (Artık sistem gradle'ı değil, ./gradlew kullanıyoruz)
if [ -f "gradlew" ]; then
    echo "🔨 Fabrika çalışıyor (Bu işlem 2-5 dk sürebilir)..."
    chmod +x gradlew
    
    # --offline parametresini sildim, ilk seferde indirmesi lazım.
    ./gradlew assembleDebug --no-daemon
    
    if [ $? -eq 0 ]; then
        echo "✅✅✅ MUTLU SON! APK HAZIR ✅✅✅"
        cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/KeyboardTrigger_Local.apk
        echo "📂 Dosya şurada: /sdcard/Download/KeyboardTrigger_Local.apk"
    else
        echo "❌ Yine hata verdi. Yukarıdaki loglara bak."
    fi
else
    echo "❌ HATA: Wrapper hala oluşmadı. İnternet bağlantını kontrol et."
fi
