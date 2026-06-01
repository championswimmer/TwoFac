# cryptography-kotlin uses ServiceLoader to discover providers at runtime.
# Keep only the provider entry points recommended by upstream docs.
# Broad package-wide keep rules also retain optional BouncyCastle bridge classes,
# which is unnecessary and can surface missing-class failures in shrinker builds.
-keep class dev.whyoleg.cryptography.CryptographyProviderContainer
-keep class dev.whyoleg.cryptography.providers.jdk.JdkCryptographyProviderContainer
# If we ever switch to `cryptography-provider-jdk-bc`, also keep:
# -keep class dev.whyoleg.cryptography.providers.jdk.DefaultJdkSecurityProvider
# -keep class dev.whyoleg.cryptography.providers.jdk.bc.BcDefaultJdkSecurityProvider
