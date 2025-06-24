@file:OptIn(ExperimentalNativeApi::class)

package twofac

import kotlinx.coroutines.runBlocking
import tech.arnav.twofac.lib.crypto.CryptoTools
import tech.arnav.twofac.lib.otp.HOTP
import tech.arnav.twofac.lib.otp.TOTP
import tech.arnav.twofac.lib.uri.OtpAuthURI
import kotlin.experimental.ExperimentalNativeApi

enum class HashAlgo { SHA1, SHA256, SHA512 }

@CName("gen_hotp")
fun genHOTP(
    secret: String,
    counter: Long,
    digits: Int = 6,
    algorithm: HashAlgo = HashAlgo.SHA1,
): String {
    val hotp = HOTP(
        secret = secret,
        digits = digits,
        algorithm = when (algorithm) {
            HashAlgo.SHA1 -> CryptoTools.Algo.SHA1
            HashAlgo.SHA256 -> CryptoTools.Algo.SHA256
            HashAlgo.SHA512 -> CryptoTools.Algo.SHA512
        }
    )
    val otp = runBlocking { hotp.generateOTP(counter) }
    return otp
}

@CName("gen_hotp_from_uri")
fun genHOTPFromUri(otpauthUri: String): String {
    val hotp = OtpAuthURI.parse(otpauthUri) as HOTP
    val counter = otpauthUri
        .substringAfter("counter=").substringBefore("&")
        .toLongOrNull() ?: 0L
    return runBlocking {
        hotp.generateOTP(counter)
    }
}

@CName("gen_totp")
fun genTOTP(
    secret: String,
    timeInterval: Long = 30L,
    digits: Int = 6,
    algorithm: HashAlgo = HashAlgo.SHA1,
): String {
    val totp = TOTP(
        secret = secret,
        timeInterval = timeInterval,
        digits = digits,
        algorithm = when (algorithm) {
            HashAlgo.SHA1 -> CryptoTools.Algo.SHA1
            HashAlgo.SHA256 -> CryptoTools.Algo.SHA256
            HashAlgo.SHA512 -> CryptoTools.Algo.SHA512
        }
    )
    val currentTimeSeconds = kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeSeconds
    return runBlocking { totp.generateOTP(currentTimeSeconds) }
}

@CName("gen_totp_from_uri")
fun genTOTPFromUri(otpauthUri: String): String {
    val totp = OtpAuthURI.parse(otpauthUri) as TOTP
    val currentTimeSeconds = kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeSeconds
    return runBlocking { totp.generateOTP(currentTimeSeconds) }
}