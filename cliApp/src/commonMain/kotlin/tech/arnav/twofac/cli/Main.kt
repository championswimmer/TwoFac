package tech.arnav.twofac.cli

import kotlinx.coroutines.runBlocking
import tech.arnav.twofac.lib.TwoFacLib
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

    val twoFacLib = TwoFacLib.initialise(passKey = "234567")
    println("TwoFacLib initialized: $twoFacLib")

    runBlocking {
        twoFacLib.addAccount("otpauth://totp/Example:user@test.com?secret=JBSWY3DPEHPK3PXP")
        val accounts = twoFacLib.getAllAccounts()
        println("Accounts: $accounts")
        val otps = twoFacLib.getAllAccountOTPs()
        println("OTPs: $otps")
    }

}