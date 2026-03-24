package tech.arnav.twofac.onboarding

import ca.gosyer.appdirs.AppDirs
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

private val appDirs = AppDirs {
    appName = "TwoFac"
    appAuthor = "tech.arnav"
    macOS.useSpaceBetweenAuthorAndApp = false
}

actual fun createOnboardingProgressStore(): KStore<OnboardingProgressSnapshot> {
    val dir = appDirs.getUserDataDir()
    SystemFileSystem.createDirectories(Path(dir))
    val filePath = Path(dir, ONBOARDING_PROGRESS_STORAGE_FILE)
    ensureOnboardingFileExists(filePath)
    return storeOf(
        file = filePath,
        default = OnboardingProgressSnapshot(),
    )
}

private fun ensureOnboardingFileExists(filePath: Path) {
    if (SystemFileSystem.exists(filePath)) return
    SystemFileSystem.sink(filePath).buffered().use { sink ->
        sink.write("""{"hasSeenInitialOnboardingGuide":false,"stepStates":{}}""".encodeToByteArray())
        sink.flush()
    }
}
