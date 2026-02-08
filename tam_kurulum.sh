#!/bin/bash
echo "🚀 Termux Android Geliştirme Ortamı Kuruluyor..."

# 1. Gerekli Araçlar
pkg install wget unzip openjdk-17 -y

# 2. Android SDK Kurulumu (Eğer yoksa)
SDK_DIR="$HOME/android-sdk"
if [ ! -d "$SDK_DIR" ]; then
    echo "📦 Android SDK indiriliyor (Bu biraz sürebilir)..."
    mkdir -p $SDK_DIR/cmdline-tools
    
    # Google'dan resmi araçları çek
    wget https://dl.google.com/android/repository/commandlinetools-linux-10406996_latest.zip -O sdk_tools.zip
    
    unzip -q sdk_tools.zip -d $SDK_DIR/cmdline-tools
    mv $SDK_DIR/cmdline-tools/cmdline-tools $SDK_DIR/cmdline-tools/latest
    rm sdk_tools.zip
    
    echo "✅ SDK Dosyaları açıldı."
fi

# 3. Çevre Değişkenleri (PATH)
export ANDROID_HOME=$SDK_DIR
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# 4. Lisansları Kabul Et ve Platformları İndir
echo "📜 Lisanslar kabul ediliyor ve Build-Tools indiriliyor..."
yes | sdkmanager --licenses > /dev/null 2>&1
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# 5. Build.gradle Dosyalarını Onar (Hata Veren Kısım)
echo "🔧 Gradle dosyaları onarılıyor..."

# -- PROJE SEVİYESİ --
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

# -- APP SEVİYESİ --
cat <<GRADLE_APP > app/build.gradle
plugins {
    id 'com.android.application'
    id 'kotlin-android'
}

android {
    namespace 'com.universish.libre.keyboardtrigger'
    compileSdk 34

    defaultConfig {
        applicationId "com.universish.libre.keyboardtrigger"
        minSdk 24
        targetSdk 34
        versionCode 2
        versionName "2.0"
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = '17'
    }
    
    buildFeatures {
        viewBinding false
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.10.0'
}
GRADLE_APP

# 6. Gradle Wrapper Oluştur (Artık hata vermemeli)
echo "🎁 Gradle Wrapper oluşturuluyor..."
gradle wrapper --gradle-version 8.2

# 7. DERLEME
echo "🔨 DERLEME BAŞLIYOR..."
chmod +x gradlew
./gradlew assembleDebug --no-daemon

if [ $? -eq 0 ]; then
    echo "✅✅✅ BAŞARILI! ✅✅✅"
    cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/KeyboardTrigger_Local.apk
    echo "APK Şurada: /sdcard/Download/KeyboardTrigger_Local.apk"
else
    echo "❌ Derleme başarısız oldu."
fi
