plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.utnay.stepupquest"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.utnay.stepupquest"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    // Wear OS dependencies
    implementation("com.google.android.gms:play-services-wearable:18.0.0")
    implementation("androidx.wear:wear-remote-interactions:1.0.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

// Google Cast SDK para Smart TV
    implementation("com.google.android.gms:play-services-cast:21.2.0")
    implementation("com.google.android.gms:play-services-cast-framework:21.2.0")
// Gson para serialización
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}