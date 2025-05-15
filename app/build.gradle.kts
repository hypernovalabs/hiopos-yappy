plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.tefbanesco"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tefbanesco"
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
    // Dependencias de Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Reemplazamos las dependencias del catálogo que están causando problemas
    // por dependencias directas
    
    // Coroutines para manejo asíncrono
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    // Para trabajar con JSON en Android sin GSON
    implementation("org.json:json:20230618")

    // Para generar y mostrar códigos QR
    implementation("com.google.zxing:core:3.5.2")

    // Para animación de confeti en la pantalla de éxito
    implementation("nl.dionsegijn:konfetti-xml:2.0.2")
}