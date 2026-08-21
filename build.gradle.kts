// Top-level build file.
// AGP 9 has built-in Kotlin support, so the org.jetbrains.kotlin.android plugin is not applied.
// The plan pins Kotlin 2.3.21 (higher than AGP's bundled KGP 2.2.10); pull it via buildscript classpath.
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
