import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {

    val frameworkName = "TwoFacKit"
    val libraryName = "twofac"

    // XCFramework for iOS targets
    listOf(
        iosX64(),
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
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
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
        nativeTarget.binaries {
            staticLib {
                baseName = libraryName
            }
            sharedLib {
                baseName = libraryName
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "${libraryName}.js"
        binaries.library()
        browser()
        nodejs()
    }


    applyDefaultHierarchyTemplate()

    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
                implementation(libs.crypto.kt.core)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.crypto.kt.jdk)
            }
        }
        nativeMain {
            dependencies {
                implementation(libs.crypto.kt.openssl)
            }
        }
        wasmJsMain {
            dependencies {
                implementation(libs.crypto.kt.web)
            }
        }
    }

}
