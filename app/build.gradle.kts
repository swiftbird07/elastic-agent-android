import java.util.Properties

val props = Properties()
val envFile = rootProject.file(".env")
if (envFile.exists()) {
    envFile.inputStream().use { props.load(it) }
}

plugins {
    alias(libs.plugins.androidApplication)
    id("com.google.devtools.ksp") version "1.8.10-1.0.9" apply true
}

android {
    namespace = "de.swiftbird.elasticandroid"
    compileSdk = 34

    defaultConfig {
        applicationId = "de.swiftbird.elasticandroid"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        android.buildFeatures.buildConfig = true

        val enrollmentString = props.getProperty("ENROLLMENT_STRING") ?: ""
        buildConfigField("String", "ENROLLMENT_STRING", "\"$enrollmentString\"")


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Retrofit for network requests
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Gson converter for parsing JSON responses
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // LiveData for reactive UI updates
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.3.1")

    // ViewModel for managing UI-related data in a lifecycle-conscious way
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.3.1")

    // Room Database for local data storage
    implementation("androidx.room:room-runtime:2.2.6")
    // Annotation processor for Room to generate the necessary code
    ksp("androidx.room:room-compiler:2.5.0")
}



