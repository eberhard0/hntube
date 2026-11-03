import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing, from the git-ignored signing.properties (storeFile, storePassword, keyAlias,
// keyPassword). Without it, release builds fall back to the debug key.
val signingProps = Properties().apply {
    rootProject.file("signing.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

// The app version; versionCode must go up by one with every release.
val appVersionName = "1.0.0"
val appVersionCode = 1

base {
    archivesName = "HNTube-$appVersionName"
}

android {
    namespace = "com.eberhard.hntube"
    // API 37 is published as the minor-versioned platform "android-37.0" (there is no bare
    // "android-37"); CI installs that. compileSdk = 37 resolves to it.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.eberhard.hntube"
        // Android TV boxes (the Rokid Station runs Android TV 10/11) and phones alike.
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (signingProps.getProperty("storeFile") != null) {
            create("release") {
                storeFile = file(signingProps.getProperty("storeFile"))
                storePassword = signingProps.getProperty("storePassword")
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")

    // Thumbnails straight from i.ytimg.com (Coil 3: built for current Compose; Coil 2 targets Compose 1.6).
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
}
