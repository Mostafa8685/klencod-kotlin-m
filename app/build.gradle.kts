plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.clashangprpr.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.clashangprpr.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    sourceSets {
        getByName("main") {
            java.exclude(
                "**/ui/GameDialogs.kt",
                "**/ui/GameHUD.kt",
                "**/world/EraLayout.kt",
                "**/world/WorldEnvironmentSystem.kt",
                "**/engine/SaveManager.kt",
                "**/model/Particle.kt",
                "**/model/ProjectilesAndNodes.kt",
                "**/model/TimePortal.kt",
                "**/model/Trap.kt",
                "**/model/WorldProps.kt"
            )
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
}
