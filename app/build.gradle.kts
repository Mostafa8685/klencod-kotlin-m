plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Automatically purge any leftover/legacy files retained by Klencod IDE from older project versions
val activeKotlinFiles = setOf(
    "MainActivity.kt",
    "GameView.kt",
    "engine/ActionButton.kt",
    "engine/Assets.kt",
    "engine/Camera2D.kt",
    "engine/LanMultiplayer.kt",
    "engine/SaveStore.kt",
    "engine/Sound.kt",
    "engine/SpriteSheet.kt",
    "engine/Ui.kt",
    "engine/VirtualJoystick.kt",
    "model/Crafting.kt",
    "model/Entities.kt",
    "model/Era.kt",
    "model/ShopAndItems.kt",
    "model/WorldObjects.kt",
    "story/StoryDialogue.kt",
    "world/OpenWorld.kt",
    "world/TileMap.kt"
)

val appRootDir = file("src/main/java/com/clashangprpr/app")
if (appRootDir.exists()) {
    appRootDir.walkTopDown().forEach { f ->
        if (f.isFile && (f.name.endsWith(".kt") || f.name.endsWith(".java"))) {
            val rel = f.relativeTo(appRootDir).invariantSeparatorsPath
            if (rel !in activeKotlinFiles) {
                println("Purging legacy leftover file: $rel")
                f.delete()
            }
        }
    }
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
