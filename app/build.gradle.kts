plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "studio.mgn.mgn"
    compileSdk = 35

    defaultConfig {
        applicationId = "studio.mgn.mgn"
        minSdk = 35
        targetSdk = 35
        // Overridden from the release tag in CI (-PversionCode/-PversionName).
        versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("versionName") as String?) ?: "0.3.0"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    // Production signing comes ONLY from environment/GitHub Secrets
    // (see RELEASE.md). Without them the build falls back to debug keys
    // and prints a warning — no keystore ever lives in the repo.
    val releaseKeystore = System.getenv("MGN_KEYSTORE_PATH")
    val releaseStorePassword = System.getenv("MGN_STORE_PASSWORD")
    val releaseKeyAlias = System.getenv("MGN_KEY_ALIAS")
    val releaseKeyPassword = System.getenv("MGN_KEY_PASSWORD")
    val hasReleaseKeys = !releaseKeystore.isNullOrBlank() &&
        !releaseStorePassword.isNullOrBlank() &&
        !releaseKeyAlias.isNullOrBlank() &&
        !releaseKeyPassword.isNullOrBlank()

    signingConfigs {
        if (hasReleaseKeys) {
            create("release") {
                storeFile = file(releaseKeystore!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            signingConfig = if (hasReleaseKeys) {
                signingConfigs.getByName("release")
            } else {
                logger.warn("MGN: no release keys in env; signing with debug keys.")
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:engine"))
    implementation(project(":core:data"))
    implementation(project(":core:audio"))
    implementation(project(":design"))
    implementation(project(":content"))
    implementation(project(":feature:setup"))
    implementation(project(":feature:command"))
    implementation(project(":feature:economy"))
    implementation(project(":feature:diplomacy"))
    implementation(project(":feature:development"))

    implementation(libs.room.runtime)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.coroutines.android)
    implementation(libs.lottie.compose)
    implementation(libs.profileinstaller)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.navigation.testing)
    debugImplementation(libs.compose.ui.test.manifest)
}

tasks.withType<Test> {
    useJUnit()
}
