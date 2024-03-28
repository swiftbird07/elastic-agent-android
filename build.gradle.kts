plugins {
    alias(libs.plugins.androidApplication) apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10")
        // Other classpath dependencies
    }
}