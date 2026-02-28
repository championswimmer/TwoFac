import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

abstract class GenerateBrowserExtensionManifestsTask : DefaultTask() {
    @get:InputFile
    abstract val baseManifestFile: RegularFileProperty

    @get:Input
    abstract val versionName: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        @Suppress("UNCHECKED_CAST")
        val chromeManifest = JsonSlurper()
            .parse(baseManifestFile.asFile.get()) as MutableMap<String, Any?>
        chromeManifest["version"] = versionName.get()

        @Suppress("UNCHECKED_CAST")
        val firefoxManifest = JsonSlurper()
            .parseText(JsonOutput.toJson(chromeManifest)) as MutableMap<String, Any?>
        firefoxManifest.remove("side_panel")
        @Suppress("UNCHECKED_CAST")
        val firefoxBackground = (firefoxManifest["background"] as? MutableMap<String, Any?>)
            ?: mutableMapOf()
        val serviceWorkerFile = firefoxBackground["service_worker"] as? String ?: "background.js"
        firefoxBackground["scripts"] = listOf(serviceWorkerFile)
        firefoxManifest["background"] = firefoxBackground
        firefoxManifest["permissions"] = (firefoxManifest["permissions"] as? List<*>)
            ?.filterNot { it == "sidePanel" }
            ?: emptyList<Any>()
        firefoxManifest["browser_specific_settings"] = mapOf(
            "gecko" to mapOf(
                "id" to "twofac@arnav.tech",
                "strict_min_version" to "120.0",
            )
        )

        val outputDir = outputDirectory.asFile.get()
        outputDir.mkdirs()
        outputDir.resolve("manifest.chrome.json").writeText(
            JsonOutput.prettyPrint(JsonOutput.toJson(chromeManifest)) + "\n"
        )
        outputDir.resolve("manifest.firefox.json").writeText(
            JsonOutput.prettyPrint(JsonOutput.toJson(firefoxManifest)) + "\n"
        )
    }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.composePwa)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "TwoFacKit"
            isStatic = true
            export(projects.sharedLib)
        }
    }

    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "composeApp.js"
        browser {
            val rootDirPath = rootDir.path
            val projectDirPath = projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static(rootDirPath)
                    static(projectDirPath)
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.material.icons.extended)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.kstore)
            implementation(libs.compose.toast)
            api(project(":sharedLib"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation("androidx.biometric:biometric:1.1.0")
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.play.services.wearable)
            implementation(libs.kstore.file)
            implementation(libs.kotlin.multiplatform.appdirs)
        }
        iosMain.dependencies {
            implementation(libs.kstore.file)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.kstore.file)
            implementation(libs.kotlin.multiplatform.appdirs)
            implementation(project(":sharedLib"))
        }
        wasmJsMain.dependencies {
            implementation(libs.kstore.storage)
        }
    }
}

android {
    namespace = "tech.arnav.twofac"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "tech.arnav.twofac"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = rootProject.extra["appVersionCode"] as Int
        versionName = rootProject.extra["appVersionName"] as String
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
    androidTestImplementation(libs.androidx.testExt.junit)
    androidTestImplementation(libs.androidx.core)
}

val appVersionName = rootProject.extra["appVersionName"] as String
val extensionResourcesDir = layout.projectDirectory.dir("extension")
val extensionManifestOutputDir = layout.buildDirectory.dir("generated/extensionManifests")
val extensionBuildDir = layout.buildDirectory.dir("extension")

val generateBrowserExtensionManifests by tasks.registering(GenerateBrowserExtensionManifestsTask::class) {
    baseManifestFile.set(extensionResourcesDir.file("manifest.base.json"))
    versionName.set(appVersionName)
    outputDirectory.set(extensionManifestOutputDir)
}

fun stageDirectory(browser: String) = extensionBuildDir.map { it.dir("$browser-unpacked") }

fun registerStageExtensionTask(
    browser: String,
    manifestFileName: String,
) = tasks.register<Sync>("stage${browser.replaceFirstChar { it.uppercase() }}Extension") {
    dependsOn("wasmJsBrowserDistribution", generateBrowserExtensionManifests)
    from(layout.buildDirectory.dir("dist/wasmJs/productionExecutable")) {
        exclude("manifest.json")
    }
    from(extensionResourcesDir) {
        include("popup.html", "sidepanel.html", "background.js")
    }
    from(extensionManifestOutputDir) {
        include(manifestFileName)
        rename { "manifest.json" }
    }
    into(stageDirectory(browser))
}

fun registerPackageExtensionTask(
    browser: String,
    stageTask: org.gradle.api.tasks.TaskProvider<Sync>,
) = tasks.register<Zip>("package${browser.replaceFirstChar { it.uppercase() }}Extension") {
    dependsOn(stageTask)
    from(stageDirectory(browser))
    destinationDirectory.set(extensionBuildDir.map { it.dir("dist") })
    archiveFileName.set("twofac-$browser-extension-$appVersionName.zip")
}

val stageChromeExtension = registerStageExtensionTask("chrome", "manifest.chrome.json")
val stageFirefoxExtension = registerStageExtensionTask("firefox", "manifest.firefox.json")

val packageChromeExtension = registerPackageExtensionTask("chrome", stageChromeExtension)
val packageFirefoxExtension = registerPackageExtensionTask("firefox", stageFirefoxExtension)

tasks.register("packageBrowserExtensions") {
    dependsOn(packageChromeExtension, packageFirefoxExtension)
}

compose.desktop {
    application {
        mainClass = "tech.arnav.twofac.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "tech.arnav.twofac"
            packageVersion = rootProject.extra["appVersionName"] as String
        }
    }
}
