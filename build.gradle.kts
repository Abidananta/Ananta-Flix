// Top-level build file
// NOTE: Do NOT add org.jetbrains.kotlin.plugin.compose here — that plugin
// only exists from Kotlin 2.0 onwards. With Kotlin 1.9.24 it is not
// published, and Gradle fails with "Plugin ... was not found".
// Compose works with just org.jetbrains.kotlin.android + the compiler
// extension version in app/build.gradle.kts (1.5.14 for Kotlin 1.9.24).
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
