@file:OptIn(ExperimentalKotlinGradlePluginApi::class)
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import java.io.File

abstract class ValidateLocalizationResourcesTask : DefaultTask() {
    @get:InputDirectory
    abstract val resourcesRoot: DirectoryProperty

    @TaskAction
    fun validate() {
        val root = resourcesRoot.asFile.get()
        val sourceDir = root.resolve("values")
        require(sourceDir.isDirectory) { "Missing source locale directory: ${sourceDir.absolutePath}" }

        val localeDirs = listOf(
            "values-es",
            "values-fr",
            "values-it",
            "values-de",
            "values-ru",
            "values-zh-rCN",
            "values-ja",
        ).map { root.resolve(it) }

        val sourceFiles = sourceDir
            .listFiles()
            ?.filter { it.isFile && it.name.startsWith("strings_") && it.name.endsWith(".xml") }
            ?.sortedBy { it.name }
            ?: emptyList()
        require(sourceFiles.isNotEmpty()) { "No strings_*.xml files found in ${sourceDir.absolutePath}" }

        localeDirs.forEach { localeDir ->
            require(localeDir.isDirectory) { "Missing locale directory: ${localeDir.absolutePath}" }

            sourceFiles.forEach { sourceFile ->
                val localeFile = localeDir.resolve(sourceFile.name)
                require(localeFile.isFile) {
                    "Missing localized file ${sourceFile.name} in ${localeDir.name}"
                }

                val sourceEntries = parseStringEntries(sourceFile)
                val localeEntries = parseStringEntries(localeFile)

                val sourceKeys = sourceEntries.keys.toList()
                val localeKeys = localeEntries.keys.toList()
                require(sourceKeys == localeKeys) {
                    "Key mismatch in ${localeFile.absolutePath}. Expected keys from ${sourceFile.name} to match exactly."
                }

                sourceEntries.forEach { (key, sourceValue) ->
                    val localeValue = localeEntries.getValue(key)
                    val sourcePlaceholders = parsePlaceholders(sourceValue)
                    val localePlaceholders = parsePlaceholders(localeValue)
                    require(sourcePlaceholders == localePlaceholders) {
                        "Placeholder mismatch for key '$key' in ${localeFile.absolutePath}. " +
                            "Expected $sourcePlaceholders but found $localePlaceholders"
                    }
                }
            }
        }
    }

    private fun parseStringEntries(file: File): LinkedHashMap<String, String> {
        val stringRegex = Regex("""<string\\s+name=\"([^\"]+)\">(.*?)</string>""", setOf(RegexOption.DOT_MATCHES_ALL))
        val text = file.readText()
        val entries = LinkedHashMap<String, String>()
        stringRegex.findAll(text).forEach { match ->
            entries[match.groupValues[1]] = match.groupValues[2]
        }
        return entries
    }

    private fun parsePlaceholders(value: String): List<String> {
        val placeholderRegex = Regex("""%\\d+\\$[sd]""")
        return placeholderRegex.findAll(value).map { it.value }.toList().sorted()
    }
}

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
        optimization {
            consumerKeepRules.publish = true
            consumerKeepRules.files.add(project.file("consumer-rules.pro"))
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
val composeResourceRootDir = layout.projectDirectory.dir("src/commonMain/composeResources")

val validateLocalizationResources by tasks.registering(ValidateLocalizationResourcesTask::class) {
    resourcesRoot.set(composeResourceRootDir)
}

tasks.named("check") {
    dependsOn(validateLocalizationResources)
}

val prepareMacDmgResources by tasks.registering(Sync::class) {
    from(layout.projectDirectory.file("src/desktopMain/resources/icons/macos.icns"))
    into(macDmgResourceRootDir.map { it.dir("macOS") })
    rename("macos.icns", "${desktopPackageName}-volume.icns")
}

val prepareMacNativeLibraries by tasks.registering(Sync::class) {
    from(layout.projectDirectory.file("src/desktopMain/native/macos/TwoFacKeychain/libtwofac_keychain.dylib"))
    into(macDmgResourceRootDir.map { it.dir("macOS") })
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
                bundleID = "tech.arnav.twofac"
                entitlementsFile.set(project.file("macos.entitlements"))
            }
        }
    }
}

tasks.matching { it.name in setOf("packageDmg", "packageReleaseDmg", "notarizeDmg", "notarizeReleaseDmg") }
    .configureEach {
        dependsOn(prepareMacDmgResources, prepareMacNativeLibraries)
    }

tasks.matching { it.name == "prepareAppResources" }
    .configureEach {
        dependsOn(prepareMacDmgResources, prepareMacNativeLibraries)
    }

// Re-sign the .app bundle with the local Apple Development certificate.
// Required for distribution builds (DMG/notarization) but NOT for ./gradlew run,
// since the biometric unlock implementation uses LAContext.evaluatePolicy() rather
// than SecAccessControl-gated keychain items and therefore does not need the
// keychain-access-groups entitlement at runtime.
// This is separate from the distribution signing path (Developer ID Application),
// which is used for DMG/notarization releases.
val macSigningIdentity = "Apple Development: Arnav Gupta (NR7UC33DNY)"
val macAppBundlePath = layout.buildDirectory
    .dir("compose/binaries/main/app/TwoFac.app")
    .get().asFile.absolutePath

val signMacAppBundle by tasks.registering(Exec::class) {
    onlyIf { org.gradle.internal.os.OperatingSystem.current().isMacOsX }
    dependsOn("createDistributable")
    description = "Re-signs the .app bundle with the local Apple Development cert for Keychain access."

    val entitlementsPath = project.file("macos.entitlements").absolutePath
    commandLine(
        "codesign", "--force", "--deep", "--sign", macSigningIdentity,
        "--entitlements", entitlementsPath,
        "--options", "runtime",
        "--timestamp",
        macAppBundlePath,
    )
}

// packageDmg packages whatever is in the distributable directory, so signing
// must happen before it runs (and after createDistributable).
tasks.matching { it.name in setOf("packageDmg", "packageReleaseDmg") }
    .configureEach {
        dependsOn(signMacAppBundle)
    }

tasks.matching { it.name == "run" }
    .configureEach {
        dependsOn(prepareMacNativeLibraries)
    }
