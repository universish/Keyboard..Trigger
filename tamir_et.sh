#!/bin/bash
echo "🔧 Gradle Çakışması Gideriliyor..."

# 1. Çevre Değişkenlerini Tanımla (SDK'yı bulması için şart)
export ANDROID_HOME=$HOME/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# 2. Settings.gradle'ı Düzelt (Kavgayı bitiren ayar: PREFER_PROJECT)
# Bu ayar, build.gradle dosyasının da repo eklemesine izin verir.
cat <<GRADLE_SETTINGS > settings.gradle
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    // KRİTİK DÜZELTME: FAIL yerine PREFER_PROJECT yapıyoruz.
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "KeyboardTrigger"
include ':app'
GRADLE_SETTINGS

# 3. Build.gradle (Root) Dosyasını Yenile
cat <<GRADLE_ROOT > build.gradle
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
GRADLE_ROOT

# 4. Gradle Wrapper'ı Tekrar Oluştur (Artık hata vermemeli)
echo "🎁 Gradle Wrapper yeniden oluşturuluyor..."
gradle wrapper --gradle-version 8.2

# 5. Derlemeyi Başlat
if [ -f "gradlew" ]; then
    echo "🔨 DERLEME BAŞLIYOR (Telefonda)..."
    chmod +x gradlew
    ./gradlew assembleDebug --no-daemon
    
    if [ $? -eq 0 ]; then
        echo "✅✅✅ DERLEME BAŞARILI! ✅✅✅"
        cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/KeyboardTrigger_Local.apk
        echo "📂 APK Şurada: /sdcard/Download/KeyboardTrigger_Local.apk"
        echo "Hemen kurup test et!"
    else
        echo "❌ Derleme sırasında hata oluştu."
    fi
else
    echo "❌ Gradle Wrapper oluşturulamadı. SDK veya Java hatası olabilir."
fi
