package tech.arnav.twofac.cli

import tech.arnav.twofac.lib.libPlatform

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    println("Hello, Kotlin CLI!")
    println("Running on: ${getPlatform().name}")
    println("Using library: ${libPlatform()}")
    with(getPlatform().appDirs) {
        println("Data Directory: ${getUserDataDir()}")
        println("Cache Directory: ${getUserCacheDir()}")
    }
}