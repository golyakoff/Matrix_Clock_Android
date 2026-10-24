package net.agolyakov.tetrisclockble.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.agolyakov.tetrisclockble.BuildConfig
import net.agolyakov.tetrisclockble.data.model.github.GithubRelease
import net.agolyakov.tetrisclockble.di.AppGithubRepository
import net.agolyakov.tetrisclockble.protocol.FirmwareVersion
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateRepository @Inject constructor(
    @AppGithubRepository private val githubRepository: GithubRepository,
    @ApplicationContext context: Context
) {
    sealed class UpdateCheckState {
        object Checking : UpdateCheckState()
        object UpToDate : UpdateCheckState()
        data class UpdateAvailable(val release: GithubRelease) : UpdateCheckState()
        data class Error(val message: String) : UpdateCheckState()
    }

    // App settings, kept on the phone (unlike the clock's own settings).
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _betaUpdates = MutableStateFlow(prefs.getBoolean(KEY_BETA_UPDATES, false))
    /** Whether app betas (vX.Y.Z-betaN) are offered too. */
    val betaUpdates: StateFlow<Boolean> = _betaUpdates.asStateFlow()

    private val _state = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Checking)
    /** The result of the last check, shared by the home screen, its gear badge and settings. */
    val state: StateFlow<UpdateCheckState> = _state.asStateFlow()

    fun setBetaUpdates(beta: Boolean) {
        prefs.edit { putBoolean(KEY_BETA_UPDATES, beta) }
        _betaUpdates.value = beta
    }

    suspend fun checkForUpdate(): UpdateCheckState {
        _state.value = UpdateCheckState.Checking
        val result = try {
            // The latest release, or with betas on the highest version including betas: an update
            // only if it is newer, so a beta of the next version is not offered the previous
            // release. The app's versions follow the firmware's scheme.
            val release = githubRepository.getLatestRelease(includePreReleases = _betaUpdates.value)
            if (FirmwareVersion.isNewer(release.tagName, BuildConfig.VERSION_NAME)) {
                UpdateCheckState.UpdateAvailable(release)
            } else {
                UpdateCheckState.UpToDate
            }
        } catch (e: Exception) {
            UpdateCheckState.Error(e.message ?: "Update check failed")
        }
        _state.value = result
        return result
    }

    private companion object {
        const val KEY_BETA_UPDATES = "beta_updates"
    }
}
