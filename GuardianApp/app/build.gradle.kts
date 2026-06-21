plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.guardianapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.guardianapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // =====================================
    // CORE
    // =====================================
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // =====================================
    // MATERIAL
    // =====================================
    implementation("com.google.android.material:material:1.12.0")

    // =====================================
    // CARDVIEW
    // =====================================
    implementation("androidx.cardview:cardview:1.0.0")

    // =====================================
    // KOTLIN
    // =====================================
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")

    // =====================================
    // COMPOSE BOM
    // =====================================
    implementation(platform(libs.androidx.compose.bom))

    // =====================================
    // COMPOSE
    // =====================================
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // =====================================
    // LOTTIE ANIMATIONS (Added for Dashboard Effects)
    // =====================================
    implementation("com.airbnb.android:lottie-compose:6.4.0")

    // =====================================
    // TOOLING
    // =====================================
    debugImplementation(libs.androidx.compose.ui.tooling)
}