@file:OptIn(ExperimentalKotlinGradlePluginApi::class)
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
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
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composePwa)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    androidLibrary {
        namespace = "tech.arnav.twofac.composeapp"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
        androidResources {
            enable = true
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "TwoFacUIKit"
            freeCompilerArgs += listOf(
                "-Xbinary=bundleId=tech.arnav.twofac.app"
            )
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
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val desktopMain by getting
        val wasmJsMain by getting

        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.components.resources)
            implementation(libs.kotlinx.io.core)
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.material.icons.extended)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.kstore)
            api(project(":sharedLib"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.biometric)
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.play.services.wearable)
            implementation(libs.play.services.auth)
            implementation(libs.kstore.file)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.android)
            implementation(libs.kotlin.multiplatform.appdirs)
            implementation(libs.kscan)
        }
        iosMain.dependencies {
            implementation(libs.kstore.file)
            implementation(libs.kscan)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.kstore.file)
            implementation(libs.kotlin.multiplatform.appdirs)
            implementation(libs.zxing.javase)
            implementation(libs.jna)
            implementation(libs.jna.platform)
            implementation(project(":sharedLib"))
        }
        wasmJsMain.resources.srcDir(layout.buildDirectory.dir("generated/wasmJs/resources"))
        wasmJsMain.dependencies {
            implementation(libs.kstore.storage)
            implementation(npm("jsqr", libs.versions.jsqr.get()))
            implementation(npm("webextension-polyfill", "0.12.0"))
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

val appVersionName = rootProject.extra["appVersionName"] as String
val desktopPackageName = "TwoFac"
val wasmInteropTypescriptDir = file("src/wasmJsMain/typescript")
val wasmInteropGeneratedResourcesDir = layout.buildDirectory.dir("generated/wasmJs/resources")
val extensionResourcesDir = layout.projectDirectory.dir("extension")
val extensionManifestOutputDir = layout.buildDirectory.dir("generated/extensionManifests")
val extensionBuildDir = layout.buildDirectory.dir("extension")
val macDmgResourceRootDir = layout.buildDirectory.dir("generated/nativeDistributions/resources")

val prepareMacDmgResources by tasks.registering(Sync::class) {
    // jpackage uses <packageName>-volume.icns from the macOS resource dir for the mounted DMG icon.
    from(layout.projectDirectory.file("src/desktopMain/resources/icons/macos.icns"))
    into(macDmgResourceRootDir.map { it.dir("macos") })
    rename("macos.icns", "${desktopPackageName}-volume.icns")
}

val installWasmInteropDependencies by tasks.registering(Exec::class) {
    workingDir = wasmInteropTypescriptDir
    commandLine("npm", "ci")
    inputs.file(wasmInteropTypescriptDir.resolve("package.json"))
    inputs.file(wasmInteropTypescriptDir.resolve("package-lock.json"))
    outputs.dir(wasmInteropTypescriptDir.resolve("node_modules"))
}

val compileWasmInterop by tasks.registering(Exec::class) {
    dependsOn(installWasmInteropDependencies)
    workingDir = wasmInteropTypescriptDir
    commandLine("npx", "tsc", "-p", "tsconfig.json")
    inputs.dir(wasmInteropTypescriptDir.resolve("src"))
    inputs.file(wasmInteropTypescriptDir.resolve("tsconfig.json"))
    outputs.dir(wasmInteropGeneratedResourcesDir)
}

tasks.named("wasmJsProcessResources") {
    dependsOn(compileWasmInterop)
}

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
        // Enable macOS template images for the system tray icon so that macOS can dim
        // the icon on secondary displays and adapt it to dark/light mode automatically.
        jvmArgs("-Dapple.awt.enableTemplateImages=true")

        buildTypes.release.proguard {
            configurationFiles.from("proguard-desktop.pro")
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = desktopPackageName
            packageVersion = rootProject.extra["appVersionName"] as String
            appResourcesRootDir.set(macDmgResourceRootDir)

            linux {
                iconFile.set(project.file("src/desktopMain/resources/icons/linux.png"))
            }
            windows {
                dirChooser = false
                iconFile.set(project.file("src/desktopMain/resources/icons/windows.ico"))
            }
            macOS {
                iconFile.set(project.file("src/desktopMain/resources/icons/macos.icns"))
            }
        }
    }
}

tasks.matching { it.name in setOf("packageDmg", "packageReleaseDmg", "notarizeDmg", "notarizeReleaseDmg") }
    .configureEach {
        dependsOn(prepareMacDmgResources)
    }
