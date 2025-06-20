package tech.arnav.twofac.cli

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    println("Hello, Kotlin CLI!")
    println("Running on: ${getPlatform().name}")
    println("Using library: ${tech.arnav.twofac.sharedlib.platform()}")
}