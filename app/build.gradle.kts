import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.countup.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.countup.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 58
        versionName = "3.0.6"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val releaseKeystore = file("../keystore/countup-release.jks")
    val localProps = Properties().apply {
        val propFile = rootProject.file("local.properties")
        if (propFile.exists()) {
            FileInputStream(propFile).use { load(it) }
        }
    }
    val keystorePass = System.getenv("COUNTUP_KEYSTORE_PASS")
        ?: localProps.getProperty("countup.keystore.pass")
        ?: file("../keystore/keystore-pass.txt").takeIf { it.exists() }?.readText()?.trim()

    signingConfigs {
        if (releaseKeystore.exists() && !keystorePass.isNullOrBlank()) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = keystorePass
                keyAlias = "countup"
                keyPassword = keystorePass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            vcsInfo {
                include = false
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            vcsInfo {
                include = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        disable += setOf(
            "GradleDependency",
            "AndroidGradlePluginVersion",
            "NewerVersionAvailable",
            "UnusedAttribute",
            "IconLauncherShape",
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.orgjson)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
