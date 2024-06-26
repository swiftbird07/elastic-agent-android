import java.util.Properties

// Load the .env file if it exists
val props = Properties()
val envFile = rootProject.file(".env")
if (envFile.exists()) {
    envFile.inputStream().use { props.load(it) }
}


plugins {
    id("com.android.application")
    kotlin("android")
    //kotlin("kapt")
    id("com.google.android.gms.oss-licenses-plugin")
}

android {
    namespace = "de.swiftbird.elasticandroid"
    compileSdk = 34

    defaultConfig {
        applicationId = "de.swiftbird.elasticandroid"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.3.0"
        android.buildFeatures.buildConfig = true

        val enrollmentString = props.getProperty("ENROLLMENT_STRING") ?: ""
        buildConfigField("String", "ENROLLMENT_STRING", "\"$enrollmentString\"")
        buildConfigField("String", "AGENT_VERSION", "\"8.10.2\"")

        javaCompileOptions {
            annotationProcessorOptions {
                argument("room.schemaLocation", "$projectDir/schemas".toString())
            }
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    testOptions {
        unitTests.apply {
            isIncludeAndroidResources = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isDebuggable = false // make debugging is disabled for release builds
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_15
        targetCompatibility = JavaVersion.VERSION_15
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
    implementation(libs.retrofit)
    // Gson converter for parsing JSON responses
    implementation(libs.converter.gson)

    // LiveData for reactive UI updates
    implementation(libs.lifecycle.livedata.ktx)

    // ViewModel for managing UI-related data in a lifecycle-conscious way
    implementation(libs.lifecycle.viewmodel.ktx)

    implementation(libs.room.runtime)

    //noinspection UseTomlInstead
    implementation("com.google.android.material:material")

    implementation(libs.work.runtime)
    // Unit testing dependencies
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)

    // For Android-specific mocking
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.work.testing)

    implementation(libs.play.services.oss.licenses)

    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    implementation(libs.security.crypto)

    testImplementation(libs.mockito.core)
    testImplementation(libs.robolectric)
    //noinspection UseTomlInstead
    testImplementation("androidx.test:core:1.5.0")
    //noinspection UseTomlInstead
    testImplementation("org.mockito:mockito-inline:4.0.0")

}



