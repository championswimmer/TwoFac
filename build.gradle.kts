plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false
}

// Centralized app versioning — all subprojects inherit from here
extra["appVersionCode"] = 260201050 // eg: 2026 02(Feb) 01 00 3 (1.0.3)
extra["appVersionName"] = "1.5.0"
