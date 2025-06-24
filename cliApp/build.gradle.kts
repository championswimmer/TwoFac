plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
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
                implementation(libs.mosaic.runtime)
            }
        }
        commonTest {}
    }
}
