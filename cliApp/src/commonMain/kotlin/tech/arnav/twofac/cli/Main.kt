package tech.arnav.twofac.cli

import kotlinx.coroutines.runBlocking
import tech.arnav.twofac.cli.storage.AppDirUtils
import tech.arnav.twofac.cli.storage.FileStorage
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


    runBlocking {
        val storagePath = AppDirUtils.getStorageFilePath(forceCreate = true)
        println("Storage file path: $storagePath")

        val storage = FileStorage(storagePath)

        val twoFacLib = TwoFacLib.initialise(passKey = "234567", storage = storage)
        println("TwoFacLib initialized: $twoFacLib")

        twoFacLib.addAccount("otpauth://totp/Example:user@test.com?secret=JBSWY3DPEHPK3PXP")
        val accounts = twoFacLib.getAllAccounts()
        println("Accounts: $accounts")
        val otps = twoFacLib.getAllAccountOTPs()
        otps.forEach {
            println("Account: ${it.first}, OTP: ${it.second}")
        }
    }


}