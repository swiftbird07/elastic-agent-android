import java.util.Properties

val props = Properties()
val envFile = rootProject.file(".env")
if (envFile.exists()) {
    envFile.inputStream().use { props.load(it) }
}


plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
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
        buildConfigField("String", "AGENT_VERSION", "\"8.10.2\"")


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
    //implementation(libs.room.common)
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

    val roomVersion = "2.4.2" // Use the latest version available

    implementation("androidx.room:room-runtime:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    implementation("com.google.android.material:material")

    // Unit testing dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:3.+")

    // For Android-specific mocking
    testImplementation("org.robolectric:robolectric:4.+")

}



