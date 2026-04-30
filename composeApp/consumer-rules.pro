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

# ── Google ML Kit (barcode-scanning) ─────────────────────────────────────────
# AGP 9 enables R8 strict full mode by default. ML Kit's barcode-scanning AAR
# does not ship consumer keep rules for its internal reflective classes, which
# causes BarcodeScanning.getClient(...) to throw NullPointerException at
# com.google.mlkit.vision.barcode.internal.zzg.zzb on release builds.
# See: https://github.com/googlesamples/mlkit/issues/1007
#      https://github.com/googlesamples/mlkit/issues/1018
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode_bundled.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_common.** { *; }

# Generated proto fields are accessed via reflection by ML Kit at runtime.
-keepclassmembers class * extends com.google.android.gms.internal.mlkit_vision_barcode_bundled.zzeh {
    <fields>;
}

# Native methods (libbarhopper_v3.so) — preserve names to avoid
# UnsatisfiedLinkError after obfuscation.
-keepclasseswithmembernames class * {
    native <methods>;
}
