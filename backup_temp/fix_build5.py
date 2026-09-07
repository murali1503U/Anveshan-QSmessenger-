import re

with open("app/build.gradle.kts", "r") as f:
    content = f.read()

deps = """
    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)
    
    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.serialization)
    
    // Zxing
    implementation("com.google.zxing:core:3.5.2")
    
    // ML Kit
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    
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
"""

content = content.replace("testImplementation(libs.junit)", deps + "\n    testImplementation(libs.junit)")

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
