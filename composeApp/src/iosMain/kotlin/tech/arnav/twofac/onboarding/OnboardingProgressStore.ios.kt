package tech.arnav.twofac.onboarding

import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

import tech.arnav.twofac.internal.getDocumentDirectory

actual fun createOnboardingProgressStore(): KStore<OnboardingProgressSnapshot> {
    val dir = getDocumentDirectory()
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
