plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val buildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "com.aplay.tv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aplay.tv"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
        // Filled in by GitHub Actions, so the app knows where the playlists folder lives.
        buildConfigField("String", "REPO", "\"${System.getenv("GITHUB_REPOSITORY").orEmpty()}\"")
        buildConfigField("String", "BRANCH", "\"${System.getenv("GITHUB_REF_NAME").orEmpty()}\"")
    }

    // Personal signing key kept in the project so each new APK installs over the old one.
    // Replace it with a private key (GitHub Secrets) before you publish the app widely.
    signingConfigs {
        create("aplay") {
            storeFile = file("aplay.keystore")
            storePassword = "android"
            keyAlias = "aplay"
            keyPassword = "android"
        }
    }

    // Everything in the top-level "playlists" folder is packed into the app.
    sourceSets {
        getByName("main") {
            assets.setSrcDirs(listOf("src/main/builtin", "../playlists"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("aplay")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    val media3 = "1.5.1"
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-exoplayer-hls:$media3")
    implementation("androidx.media3:media3-exoplayer-dash:$media3")
    implementation("androidx.media3:media3-exoplayer-smoothstreaming:$media3")
    implementation("androidx.media3:media3-exoplayer-rtsp:$media3")
    implementation("androidx.media3:media3-ui:$media3")

    implementation("io.coil-kt:coil-compose:2.7.0")
}
