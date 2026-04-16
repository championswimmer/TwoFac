plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val androidSigningStoreFile = providers.environmentVariable("ANDROID_SIGNING_STORE_FILE")
    .orNull
    ?.takeIf { it.isNotBlank() }
    ?.let(::file)
    ?: project.layout.projectDirectory.file("android-keystore.jks").asFile.takeIf { it.exists() }

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

val requiresAndroidReleaseSigning = gradle.startParameter.taskNames.any { taskName ->
    val normalized = taskName.lowercase()
    normalized == "assemblerelease" ||
        normalized == "bundlerelease" ||
        normalized == ":androidapp:assemblerelease" ||
        normalized == ":androidapp:bundlerelease" ||
        normalized == ":androidapp:bundle" ||
        (normalized.startsWith(":androidapp:") && normalized.contains("release"))
}

if (requiresAndroidReleaseSigning && !hasAndroidSigning) {
    throw GradleException(
        "Android release signing is not configured. Set ANDROID_SIGNING_KEY_ALIAS, ANDROID_SIGNING_KEY_PASSWORD, ANDROID_SIGNING_STORE_PASSWORD, and optionally ANDROID_SIGNING_STORE_FILE (defaults to androidApp/android-keystore.jks when present)."
    )
}

android {
    namespace = "tech.arnav.twofac.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "tech.arnav.twofac.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = rootProject.extra["appVersionCode"] as Int
        versionName = rootProject.extra["appVersionName"] as String
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        if (hasAndroidSigning) {
            create("release") {
                storeFile = androidSigningStoreFile
                storePassword = androidSigningStorePassword
                keyAlias = androidSigningKeyAlias
                keyPassword = androidSigningKeyPassword
            }
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasAndroidSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.kotlin.multiplatform.appdirs)
    implementation(libs.filekit.core)
    implementation(libs.filekit.dialogs)
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

