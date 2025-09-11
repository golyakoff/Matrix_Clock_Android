package net.agolyakov.tetrisclockble.ui.screen.firmware

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.agolyakov.tetrisclockble.service.bluetooth.handlers.VersionReadCharacteristicHandler
import net.agolyakov.tetrisclockble.service.bluetooth.BluetoothAdapterProvider
import net.agolyakov.tetrisclockble.data.repository.FirmwareRepository
import net.agolyakov.tetrisclockble.data.model.github.GithubRelease
import net.agolyakov.tetrisclockble.data.repository.GithubRepository
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FirmwareUpdateViewModel @Inject constructor(
    private val githubRepository: GithubRepository,
    private val firmwareRepository: FirmwareRepository,
    private val bluetoothAdapterProvider: BluetoothAdapterProvider
) : ViewModel() {
    // Firmware Version
    private var _firmwareVersion: String = "Unknown"
    private var _tetrisClockFirmwareVersion = MutableStateFlow(_firmwareVersion)
    var tetrisClockFirmwareVersion: StateFlow<String> = _tetrisClockFirmwareVersion
    var firmwareVersionReadCharacteristicHandler = VersionReadCharacteristicHandler(_tetrisClockFirmwareVersion)


    private val _uiState = MutableStateFlow<FirmwareUpdateState>(FirmwareUpdateState.Idle)
    val uiState: StateFlow<FirmwareUpdateState> = _uiState.asStateFlow()

    fun checkForUpdates(currentVersion: String, includePreReleases: Boolean) {
        viewModelScope.launch {
            _uiState.value = FirmwareUpdateState.Checking
            try {
                val latestRelease = githubRepository.getLatestRelease(includePreReleases)
                if (isNewVersionAvailable(currentVersion, latestRelease.tagName)) {
                    _uiState.value = FirmwareUpdateState.UpdateAvailable(latestRelease)
                } else {
                    _uiState.value = FirmwareUpdateState.NoUpdate
                }
            } catch (e: Exception) {
                _uiState.value = FirmwareUpdateState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun downloadFirmware(release: GithubRelease) {
        viewModelScope.launch {
            _uiState.value = FirmwareUpdateState.Downloading(0f)
            try {
                val file = firmwareRepository.downloadFirmware(
                    release.assets.first { it.name.endsWith(".bin") }.browserDownloadUrl,
                    onProgress = { progress ->
                        _uiState.value = FirmwareUpdateState.Downloading(progress)
                    }
                )
                _uiState.value = FirmwareUpdateState.ReadyToInstall(file, release)
            } catch (e: Exception) {
                _uiState.value = FirmwareUpdateState.Error(e.message ?: "Download failed")
            }
        }
    }

    fun installFirmware(device: BluetoothDevice, firmwareFile: File) {
        viewModelScope.launch {
            _uiState.value = FirmwareUpdateState.Installing(0f)
            try {
                firmwareRepository.installFirmware(
                    device,
                    firmwareFile,
                    onProgress = { progress ->
                        _uiState.value = FirmwareUpdateState.Installing(progress)
                    }
                )
                _uiState.value = FirmwareUpdateState.Success
            } catch (e: Exception) {
                _uiState.value = FirmwareUpdateState.Error(e.message ?: "Installation failed")
            }
        }
    }

    private fun isNewVersionAvailable(current: String, latest: String): Boolean {
        // Implement version comparison logic
        return Version(latest) > Version(current)
    }
}