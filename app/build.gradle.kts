plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("plugin.compose")
}

android {
    namespace = "com.ad.articulosdigitales"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ad.articulosdigitales"
        minSdk = 21
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.annotation:annotation:1.7.1")
    implementation("androidx.appcompat:appcompat:1.6.1") // 1.6.1 es la última versión estable, usa la que prefieras
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // 2.1.4 es la versión estable más reciente, puedes usar la que prefieras
    implementation("com.google.android.material:material:1.10.0") // O la versión más reciente
    implementation("com.github.bumptech.glide:glide:4.16.0") // Usa la versión más reciente
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0") // También se require el compiler

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}