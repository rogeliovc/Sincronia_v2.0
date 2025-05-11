plugins {
    alias(libs.plugins.android.application)
}


android {
    namespace = "com.example.sincronia"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.sincronia"
        minSdk = 27
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
}

dependencies {
    // Gson para Spotify App Remote SDK
    implementation("com.google.code.gson:gson:2.10.1")
    // Spotify Android Auth SDK
    implementation("com.spotify.android:auth:1.2.6")
    // Spotify App Remote SDK para reproducción
    implementation(files("libs/spotify-app-remote-release-0.8.0.aar"))

    // Glide para carga de imágenes
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Spotify App Remote para reproducción


    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}