import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")

if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
android {
    namespace = "com.example.canhbao"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.canhbao"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "BASE_IP",
            "\"${localProperties.getProperty("BASE_IP") ?: "192.168.1.13"}\""
        )
        buildConfigField(
            "String",
            "MAPBOX_PUBLIC_TOKEN",
            "\"${localProperties.getProperty("MAPBOX_PUBLIC_TOKEN")}\""
        )

        manifestPlaceholders["MAPBOX_PUBLIC_TOKEN"] =
            localProperties.getProperty("MAPBOX_PUBLIC_TOKEN") ?: ""
    }

    // Giữ nguyên các phần khác...
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")

    // 1. Mapbox Maps SDK
    implementation("com.mapbox.maps:android:11.8.0")

    implementation("com.google.android.gms:play-services-location:21.3.0")

    // 2. FIREBASE & GOOGLE AUTH
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // 3. COMPOSE & NAVIGATION
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    // 4. PERMISSIONS & TOOLS
    implementation("com.google.accompanist:accompanist-permissions:0.35.0-alpha")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")


    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    implementation("com.google.guava:guava:31.1-android")

    implementation("androidx.compose.ui:ui-unit")

    implementation("com.google.android.libraries.places:places:3.5.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.foundation)

    implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation(libs.androidx.ui.text)
    implementation(libs.androidx.foundation)

    implementation("io.github.webrtc-sdk:android:137.7151.05")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.foundation)
}