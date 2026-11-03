plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.gurjot.torchtester"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gurjot.torchtester"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
