plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val noorDebugKeystoreFile = System.getenv("NOOR_DEBUG_KEYSTORE_FILE")
val noorDebugKeystorePassword = System.getenv("NOOR_DEBUG_KEYSTORE_PASSWORD")
val hasNoorDebugSigning =
    !noorDebugKeystoreFile.isNullOrBlank() && !noorDebugKeystorePassword.isNullOrBlank()

android {
    namespace = "mw.noor.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "mw.noor.app"
        minSdk = 23
        targetSdk = 35
        versionCode = 6
        versionName = "0.4.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    if (hasNoorDebugSigning) {
        signingConfigs {
            create("noorDevelopment") {
                storeFile = file(requireNotNull(noorDebugKeystoreFile))
                storePassword = requireNotNull(noorDebugKeystorePassword)
                keyAlias = "noor-development"
                keyPassword = requireNotNull(noorDebugKeystorePassword)
            }
        }
    }

    buildTypes {
        getByName("debug") {
            if (hasNoorDebugSigning) {
                signingConfig = signingConfigs.getByName("noorDevelopment")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
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
    val composeBom = platform("androidx.compose:compose-bom:2025.05.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.work:work-runtime-ktx:2.10.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
