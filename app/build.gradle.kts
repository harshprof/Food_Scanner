plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services") // Correct placement for the plugin
}

android {
    namespace = "com.example.mad_final_project"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mad_final_project"
        minSdk = 26  // Consider setting to a lower value (e.g., 21) for broader compatibility
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // Firebase BOM for version management
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))

    // Firebase libraries
    implementation("com.google.firebase:firebase-auth:21.0.8")
    implementation("com.google.firebase:firebase-firestore:24.3.1")
    implementation("com.google.firebase:firebase-core:21.0.0")
    implementation("com.google.firebase:firebase-database:20.0.5")  // Firebase Realtime Database

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Android libraries
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // ML Kit for barcode scanning
    implementation("com.google.mlkit:barcode-scanning:17.0.2")

    // CameraX
    val cameraxVersion = "1.1.0-beta01"
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    implementation ("com.android.volley:volley:1.2.1")

    implementation ("com.github.PhilJay:MPAndroidChart:3.1.0")
}


// Apply the Google Services plugin
apply(plugin = "com.google.gms.google-services")
