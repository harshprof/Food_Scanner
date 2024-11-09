// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
//    alias(libs.plugins.android.library) apply false
    // Add other plugins here as needed
}

buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.3.15")
    }
}


