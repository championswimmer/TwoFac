# Consumer ProGuard rules for :composeApp (Android library)
# These rules are automatically applied to any app module that depends on :composeApp.

# ── kotlinx.serialization ────────────────────────────────────────────────────
# Keep the generated serializer companions so @Serializable classes survive R8.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class **$$serializer {
    static **$$serializer INSTANCE;
}
-keep,includedescriptorclasses class **$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    static **$Companion Companion;
    static ** serializer(...);
    *** Companion;
}

# ── Koin ─────────────────────────────────────────────────────────────────────
# Koin uses Kotlin reflection to instantiate classes declared in modules.
-keepnames class tech.arnav.twofac.** { *; }
-keep class org.koin.** { *; }

# ── Compose Multiplatform resources ──────────────────────────────────────────
-keep class tech.arnav.twofac.composeapp.generated.** { *; }

# ── Cryptography-kotlin (ServiceLoader providers) ────────────────────────────
-keep class dev.whyoleg.cryptography.** { *; }

# ── FileKit ───────────────────────────────────────────────────────────────────
-keep class io.github.vinceglb.filekit.** { *; }

# ── AppDirs ───────────────────────────────────────────────────────────────────
-keep class dev.dirs.** { *; }

# ── Kotlin metadata ──────────────────────────────────────────────────────────
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes Signature
-keepattributes Exceptions
