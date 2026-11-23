plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    val persistentDebugSigning = System.getenv("EASYWAY_KEYSTORE_PATH")?.let { path ->
        signingConfigs.create("persistentDebug") {
            storeFile = file(path)
            storePassword = System.getenv("EASYWAY_STORE_PASSWORD")
            keyAlias = System.getenv("EASYWAY_KEY_ALIAS")
            keyPassword = System.getenv("EASYWAY_KEY_PASSWORD")
        }
    }
    namespace = "ca.easyway.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "ca.easyway.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 10
        versionName = "1.0.0"
    }
    buildTypes {
        getByName("debug") {
            if (persistentDebugSigning != null) signingConfig = persistentDebugSigning
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.10.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
