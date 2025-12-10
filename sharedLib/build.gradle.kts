@file:OptIn(ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxKover)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.dokka)
}

group = "tech.arnav.twofac"
version = "0.1.0"

kotlin {

    val frameworkName = "TwoFacKit"
    val libraryName = "twofac"

    // XCFramework for iOS targets
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        macosX64(),
        macosArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = frameworkName
            isStatic = true // Set to false if you want a dynamic framework
        }
    }

    // JVM library for Android and Desktop
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    // Native libraries for Linux, macOS, and Windows
    listOf(
        linuxX64(),
        linuxArm64(),
        macosArm64(),
        macosX64(),
        mingwX64(),
    ).forEach { nativeTarget ->
        nativeTarget.compilations.getByName("main") {
            cinterops {
                val zlib by creating {}
                zlib.apply {
                    packageName = "zlib"
                    definitionFile.set(project.file("src/nativeMain/cinterop/zlib.def"))
                }
            }
        }
        nativeTarget.binaries {
            staticLib {
                baseName = libraryName
                if (buildType == NativeBuildType.DEBUG) { // skip linking for debug builds
                    linkTaskProvider.configure { enabled = false }
                }
            }
            sharedLib {
                baseName = libraryName
                if (buildType == NativeBuildType.DEBUG) { // skip linking for debug builds
                    linkTaskProvider.configure { enabled = false }
                }
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class) wasmJs {
        outputModuleName = "${libraryName}.js"
        binaries.library()
        browser()
        nodejs()
    }

    applyDefaultHierarchyTemplate()

    abiValidation {
        // TODO: fix the gradle task name in github actions https://youtrack.jetbrains.com/projects/KT/issues/KT-80674
        enabled.set(true)
        filters {
            included {
                annotatedWith.add("tech.arnav.twofac.lib.PublicApi")
            }
            excluded {
                annotatedWith.add("tech.arnav.twofac.lib.InternalApi")
            }
        }
    }

    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.crypto.kt.core)
                api(libs.kotlinx.serialization.core)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.crypto.kt.jdk)
            }
        }
        nativeMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.crypto.kt.openssl)
            }
        }
        wasmJsMain {
            dependencies {
                implementation(libs.crypto.kt.web)
                implementation(libs.kotlinx.browser)
            }
        }
    }

}
