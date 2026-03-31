# ProGuard / R8 rules for androidApp (release build)
# Base: proguard-android-optimize.txt (included via build.gradle.kts)
# Library-specific rules come from :composeApp/consumer-rules.pro automatically.

# ── Keep application entry points ───────────────────────────────────────────
-keep class tech.arnav.twofac.TwoFacApplication { *; }
-keep class tech.arnav.twofac.MainActivity { *; }

# ── Kotlin reflection / metadata ────────────────────────────────────────────
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable

# ── Kotlin coroutines ────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}
