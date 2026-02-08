#!/bin/bash
echo "🐎 Truva Atı Operasyonu Başlıyor..."

# 1. TEMİZLİK: Her şeyi sil (Hata verecek hiçbir şey kalmasın)
rm -f build.gradle settings.gradle
rm -rf gradle .gradle
rm -f gradlew gradlew.bat

# 2. KANDIRMACA: Boş ve zararsız dosyalar oluştur
# Sistemdeki Gradle bunları görünce hata vermez.
touch build.gradle
echo 'rootProject.name = "GeciciProje"' > settings.gradle

# 3. WRAPPER OLUŞTURMA
# Şimdi sistemdeki Gradle'a diyoruz ki: "Bana 8.2 sürümünü hazırlayan bir başlatıcı ver."
echo "🎁 Gradle Wrapper (8.2) oluşturuluyor..."
gradle wrapper --gradle-version 8.2

# 4. GERÇEK DOSYALARI YAZMA
# Wrapper oluştuğuna göre, sahte dosyaları silip gerçek Android kodlarını yazabiliriz.
echo "📝 Gerçek proje dosyaları yazılıyor..."

# -- settings.gradle (Gerçek) --
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

# -- build.gradle (Gerçek - Root) --
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

# 5. UYGULAMA AYARLARI (App Level)
# Bunu da garanti olsun diye tekrar yazıyoruz
cat <<APP > app/build.gradle
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
APP

# 6. VE BÜYÜK FİNAL: DERLEME
# Artık sistemdeki 'gradle'ı değil, az önce ürettiğimiz './gradlew'yu kullanıyoruz.
if [ -f "gradlew" ]; then
    echo "🔨 FABRİKA ÇALIŞIYOR..."
    echo "Lütfen bekleyin, internetten gerekli parçalar indirilecek (3-5 dk sürebilir)..."
    
    chmod +x gradlew
    
    # --no-daemon: Hafızayı korur
    # --stacktrace: Hata olursa detay gösterir
    ./gradlew assembleDebug --no-daemon
    
    if [ $? -eq 0 ]; then
        echo "✅✅✅ TEBRİKLER! APK BAŞARIYLA OLUŞTU ✅✅✅"
        cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/KeyboardTrigger_Local.apk
        echo "📂 APK Nerede: /sdcard/Download/KeyboardTrigger_Local.apk"
        echo "Hemen kur ve o yeşil butona bas!"
    else
        echo "❌ Bir hata oluştu. İnternet bağlantını kontrol et."
    fi
else
    echo "❌ HATA: Wrapper dosyası (gradlew) oluşturulamadı."
fi
