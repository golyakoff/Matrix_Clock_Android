package net.agolyakov.tetrisclockble.ui.screen.firmware

import net.agolyakov.tetrisclockble.data.model.github.GithubRelease
import java.io.File

sealed class FirmwareUpdateState {
    object Idle : FirmwareUpdateState()
    object Checking : FirmwareUpdateState()
    data class UpdateAvailable(val release: GithubRelease, val currentVersion: String) : FirmwareUpdateState()
    data class NoUpdate(val currentVersion: String) : FirmwareUpdateState()
    data class Downloading(val progress: Float) : FirmwareUpdateState()
    data class ReadyToInstall(val file: File, val release: GithubRelease) : FirmwareUpdateState()
    data class Installing(val progress: Float) : FirmwareUpdateState()
    object Processing : FirmwareUpdateState()
    object Success : FirmwareUpdateState()
    data class Error(val message: String) : FirmwareUpdateState()
}