plugins {
    
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    
    alias(libs.plugins.google.devtools.ksp)
}

android {
    buildFeatures {
        buildConfig = true
    }

    namespace = "com.example.meshchat"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aistudio.meshchat.kxmpzq"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // TFLite GPU
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-gpu-api:2.14.0")

    
    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)
    
    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.retrofit.converter.serialization)
    
    // Zxing
    implementation("com.google.zxing:core:3.5.2")
    
    // ML Kit
    
    // Play Services
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")

    
    // Zstd
    implementation("com.github.luben:zstd-jni:1.5.5-11@aar")
    
    // Conscrypt
    implementation("org.conscrypt:conscrypt-android:2.5.2")
    
    // Fastutil
    implementation("it.unimi.dsi:fastutil:8.5.12")
    
    // Janino
    implementation("org.codehaus.janino:janino:3.1.10")
    
    // LuaJ
    implementation("org.luaj:luaj-jse:3.0.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
