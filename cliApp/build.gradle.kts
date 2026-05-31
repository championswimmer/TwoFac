@file:OptIn(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi::class)

import java.net.URI

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "tech.arnav.twofac"
version = rootProject.extra["appVersionName"] as String

kotlin {
    listOf(
        macosArm64(),
        linuxX64(),
        mingwX64(),
    ).forEach { target ->
        target.apply {
            binaries.executable {
                baseName = "2fac"
                entryPoint = "tech.arnav.twofac.cli.main"
                if (target.name == "linuxX64") {
                    disableNativeCache(
                        version = org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion.`2_3_21`,
                        reason = "Clikt has duplicate symbol",
                        issueUrl = URI("https://github.com/ajalt/clikt/issues/598"),
                    )
                }
            }
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":sharedLib"))
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.clikt)
                implementation(libs.mordant.coroutines)
                implementation(libs.kotlin.multiplatform.appdirs)
                implementation(libs.kstore)
                implementation(libs.kstore.file)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.koin.test)

            }
        }
    }
}

tasks.withType<Exec> {
    if (name.startsWith("runDebugExecutable")) {
        val runArgs = project.findProperty("runArgs") as? String
        if (!runArgs.isNullOrEmpty()) {
            args(runArgs.split(" "))
        }
    }
}
