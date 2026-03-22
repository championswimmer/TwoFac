# cryptography-kotlin uses ServiceLoader to discover providers at runtime.
# ProGuard strips or renames the JdkCryptographyProviderContainer class, breaking
# CryptographyProvider.Default on the packaged release app.
-keep class dev.whyoleg.cryptography.** { *; }
