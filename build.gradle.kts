import com.android.build.api.dsl.ApplicationExtension

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
extra["appVersionCode"] = 260401062 // eg: 2026 02(Feb) 01 00 3 (1.0.3)
extra["appVersionName"] = "1.6.2"

subprojects {
    if (path !in setOf(":androidApp", ":watchApp")) {
        return@subprojects
    }

    plugins.withId("com.android.application") {
        val androidSigningStoreFile = providers.environmentVariable("ANDROID_SIGNING_STORE_FILE")
            .orNull
            ?.takeIf { it.isNotBlank() }
            ?.let(rootProject::file)
            ?: rootProject.layout.projectDirectory.file("android-keystore.jks").asFile.takeIf { it.exists() }

        val androidSigningKeyAlias = providers.environmentVariable("ANDROID_SIGNING_KEY_ALIAS")
            .orNull
            ?.takeIf { it.isNotBlank() }
        val androidSigningKeyPassword = providers.environmentVariable("ANDROID_SIGNING_KEY_PASSWORD")
            .orNull
            ?.takeIf { it.isNotBlank() }
        val androidSigningStorePassword = providers.environmentVariable("ANDROID_SIGNING_STORE_PASSWORD")
            .orNull
            ?.takeIf { it.isNotBlank() }

        val hasAndroidSigning = androidSigningStoreFile != null &&
            !androidSigningKeyAlias.isNullOrBlank() &&
            !androidSigningKeyPassword.isNullOrBlank() &&
            !androidSigningStorePassword.isNullOrBlank()

        val normalizedProjectPath = project.path.lowercase()
        val requiresAndroidReleaseSigning = gradle.startParameter.taskNames.any { taskName ->
            val normalizedTaskName = taskName.lowercase()
            normalizedTaskName == "assemblerelease" ||
                normalizedTaskName == "bundlerelease" ||
                normalizedTaskName == "$normalizedProjectPath:assemblerelease" ||
                normalizedTaskName == "$normalizedProjectPath:bundlerelease" ||
                normalizedTaskName == "$normalizedProjectPath:bundle" ||
                (normalizedTaskName.startsWith("$normalizedProjectPath:") && normalizedTaskName.contains("release"))
        }

        if (requiresAndroidReleaseSigning && !hasAndroidSigning) {
            throw GradleException(
                "Android release signing is not configured. Set ANDROID_SIGNING_KEY_ALIAS, " +
                    "ANDROID_SIGNING_KEY_PASSWORD, ANDROID_SIGNING_STORE_PASSWORD, and optionally " +
                    "ANDROID_SIGNING_STORE_FILE (defaults to android-keystore.jks at the repository root when present)."
            )
        }

        extensions.configure<ApplicationExtension> {
            val releaseSigningConfig = if (hasAndroidSigning) {
                signingConfigs.maybeCreate("release").apply {
                    storeFile = androidSigningStoreFile
                    storePassword = androidSigningStorePassword
                    keyAlias = androidSigningKeyAlias
                    keyPassword = androidSigningKeyPassword
                }
            } else {
                null
            }

            buildTypes.named("release") {
                if (releaseSigningConfig != null) {
                    signingConfig = releaseSigningConfig
                }
            }
        }
    }
}
