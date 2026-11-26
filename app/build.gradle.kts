plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // No org.jetbrains.kotlin.plugin.compose — it does not exist for
    // Kotlin 1.9.24 (only from Kotlin 2.0). See root build.gradle.kts.
}

android {
    namespace = "com.iptv.glass"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.iptv.glass"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
    }
    composeOptions {
        // 1.5.14 is the Compose compiler extension for Kotlin 1.9.24.
        // "1.9.24" here is wrong — that is the Kotlin version, not the
        // compiler extension version.
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.0")

    // Media3 ExoPlayer — HLS (.m3u8) and MP4
    // NOTE: media3-exoplayer-hls is REQUIRED, not optional. The core
    // exoplayer artifact alone has no HLS support: when a .m3u8 stream
    // is played, DefaultMediaSourceFactory tries to load
    // androidx.media3.exoplayer.hls.HlsMediaSource$Factory by reflection
    // and the app fails with ClassNotFoundException without this line.
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-common:1.4.1")

    // Coil — channel logos / posters
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Networking — fetch M3U / EPG from GitHub
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // DataStore — favourites + recent watched, persisted locally
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("junit:junit:4.13.2")
}
