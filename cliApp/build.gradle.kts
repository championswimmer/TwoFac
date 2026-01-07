plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "tech.arnav.twofac"
version = "1.0-SNAPSHOT"

kotlin {
    listOf(
        macosArm64(),
        macosX64(),
        linuxX64(),
        mingwX64(),
    ).forEach {
        it.apply {
            binaries.executable {
                baseName = "2fac"
                entryPoint = "tech.arnav.twofac.cli.main"
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
