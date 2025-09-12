package net.agolyakov.tetrisclockble.ui.screen.firmware

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.agolyakov.tetrisclockble.data.repository.FirmwareRepository
import net.agolyakov.tetrisclockble.data.model.github.GithubRelease
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FirmwareViewModel @Inject constructor(
    private val firmwareRepository: FirmwareRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FirmwareUpdateState>(FirmwareUpdateState.Idle)
    val uiState: StateFlow<FirmwareUpdateState> = _uiState.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    fun checkForUpdates(includePreReleases: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = FirmwareUpdateState.Checking
            try {
                val currentVersion = firmwareRepository.getCurrentVersion()
                val update = firmwareRepository.checkForUpdates(includePreReleases)

                _uiState.value = if (update != null) {
                    FirmwareUpdateState.UpdateAvailable(update, currentVersion)
                } else {
                    FirmwareUpdateState.NoUpdate(currentVersion)
                }
            } catch (e: Exception) {
                _uiState.value = FirmwareUpdateState.Error(e.message ?: "Check failed")
            }
        }
    }

    fun downloadFirmware(release: GithubRelease) {
        viewModelScope.launch {
            _uiState.value = FirmwareUpdateState.Downloading(0f)
            try {
                val file = firmwareRepository.downloadFirmware(
                    release.assets.first { it.name.endsWith(".bin") }.browserDownloadUrl
                ) { progress ->
                    _uiState.value = FirmwareUpdateState.Downloading(progress)
                    _progress.value = progress
                    _statusMessage.value = "Downloading: ${progress.toInt()}%"
                }
                _uiState.value = FirmwareUpdateState.ReadyToInstall(file, release)
            } catch (e: Exception) {
                _uiState.value = FirmwareUpdateState.Error(e.message ?: "Download failed")
            }
        }
    }

    fun installFirmware(firmwareFile: File) {
        viewModelScope.launch {
            _uiState.value = FirmwareUpdateState.Installing(0f)
            try {
                firmwareRepository.installFirmware(firmwareFile) { progress ->
                    _uiState.value = FirmwareUpdateState.Installing(progress)
                    _progress.value = progress
                    _statusMessage.value = "Installing: ${progress.toInt()}%"
                }
                _uiState.value = FirmwareUpdateState.Success
                _statusMessage.value = "Installation completed successfully!"
            } catch (e: Exception) {
                _uiState.value = FirmwareUpdateState.Error(e.message ?: "Installation failed")
            }
        }
    }

    fun performCompleteUpdate(includePreReleases: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = FirmwareUpdateState.Processing
            _progress.value = 0f
            _statusMessage.value = "Starting update..."

            val result = firmwareRepository.performCompleteUpdate(includePreReleases) { progress, message ->
                _progress.value = progress
                _statusMessage.value = message
            }

            when (result) {
                is FirmwareRepository.UpdateResult.Success -> {
                    _uiState.value = FirmwareUpdateState.Success
                    _progress.value = 100f
                    _statusMessage.value = "Update successful! ${result.oldVersion} → ${result.newVersion}"
                }
                is FirmwareRepository.UpdateResult.NoUpdateAvailable -> {
                    _uiState.value = FirmwareUpdateState.NoUpdate(result.currentVersion)
                    _statusMessage.value = "Already on latest version: ${result.currentVersion}"
                }
                is FirmwareRepository.UpdateResult.Error -> {
                    _uiState.value = FirmwareUpdateState.Error(result.message)
                    _statusMessage.value = "Error: ${result.message}"
                }
                FirmwareRepository.UpdateResult.Cancelled -> {
                    _uiState.value = FirmwareUpdateState.Idle
                    _statusMessage.value = "Update cancelled"
                }
            }
        }
    }

    fun abortUpdate() {
        viewModelScope.launch {
            try {
                firmwareRepository.abortOta()
                _uiState.value = FirmwareUpdateState.Idle
                _statusMessage.value = "Update aborted"
            } catch (e: Exception) {
                _statusMessage.value = "Failed to abort: ${e.message}"
            }
        }
    }
}