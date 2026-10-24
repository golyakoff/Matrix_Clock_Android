package net.agolyakov.tetrisclockble.data.repository

import net.agolyakov.tetrisclockble.BuildConfig
import net.agolyakov.tetrisclockble.data.model.github.GithubRelease
import net.agolyakov.tetrisclockble.di.AppGithubRepository
import net.agolyakov.tetrisclockble.protocol.FirmwareVersion
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateRepository @Inject constructor(
    @AppGithubRepository private val githubRepository: GithubRepository
) {
    sealed class UpdateCheckState {
        object UpToDate : UpdateCheckState()
        data class UpdateAvailable(val release: GithubRelease) : UpdateCheckState()
        data class Error(val message: String) : UpdateCheckState()
    }

    suspend fun checkForUpdate(): UpdateCheckState {
        return try {
            // The latest release (never a beta): an update only if it is newer, so a beta of the
            // next version is not offered the previous release. The app's versions follow the
            // firmware's scheme.
            val release = githubRepository.getLatestRelease()
            if (FirmwareVersion.isNewer(release.tagName, BuildConfig.VERSION_NAME)) {
                UpdateCheckState.UpdateAvailable(release)
            } else {
                UpdateCheckState.UpToDate
            }
        } catch (e: Exception) {
            UpdateCheckState.Error(e.message ?: "Update check failed")
        }
    }
}
