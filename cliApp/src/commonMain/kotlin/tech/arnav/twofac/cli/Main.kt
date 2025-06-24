package tech.arnav.twofac.cli

import com.jakewharton.mosaic.runMosaicBlocking
import tech.arnav.twofac.cli.screens.HomeScreen

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    println("Hello, Kotlin CLI!")
    println("Running on: ${getPlatform().name}")
    println("Using library: ${tech.arnav.twofac.lib.libPlatform()}")

    runMosaicBlocking {
        HomeScreen()
    }
}