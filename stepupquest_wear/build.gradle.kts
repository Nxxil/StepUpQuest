plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.stepupquest_wear"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.stepupquest_wear"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
// O la última versión estable
    implementation("com.google.android.gms:play-services-wearable:19.0.0")
// Para notificaciones y compatibilidad (MUY IMPORTANTE)
    implementation("androidx.core:core-app:1.13.1")
// O la última versión, para NotificationManagerCompat
    implementation("androidx.appcompat:appcompat:1.7.0")
// Para NotificationCompat.Builder si usas recursos de appcompat

    implementation("androidx.compose.ui:ui:...")
    implementation("androidx.wear.compose:compose-material:...")
    implementation("androidx.wear.compose:compose-foundation:...")
    implementation(libs.play.services.wearable)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.wear.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)

    implementation("androidx.cardview:cardview:1.0.0")

    // Bibliotecas Wear OS válidas
    implementation("androidx.wear:wear:1.3.0")
    implementation("androidx.wear.compose:compose-material:1.3.0")

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
