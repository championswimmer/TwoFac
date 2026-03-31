package tech.arnav.twofac.onboarding

import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import tech.arnav.twofac.internal.androidAppDirs

actual fun createOnboardingProgressStore(): KStore<OnboardingProgressSnapshot> {
    val dir = androidAppDirs.getUserDataDir()
    SystemFileSystem.createDirectories(Path(dir))
    val filePath = Path(dir, ONBOARDING_PROGRESS_STORAGE_FILE)
    ensureOnboardingFileExists(filePath)
    return storeOf(
        file = filePath,
        default = OnboardingProgressSnapshot(),
    )
}
