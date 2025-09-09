package net.agolyakov.tetrisclockble.service

import java.io.File

sealed class FirmwareUpdateState {
    object Idle : FirmwareUpdateState()
    object Checking : FirmwareUpdateState()
    data class UpdateAvailable(val release: GithubRelease) : FirmwareUpdateState()
    data class Downloading(val progress: Float) : FirmwareUpdateState()
    data class ReadyToInstall(val file: File, val release: GithubRelease) : FirmwareUpdateState()
    data class Installing(val progress: Float) : FirmwareUpdateState()
    object Success : FirmwareUpdateState()
    data class Error(val message: String) : FirmwareUpdateState()
    object NoUpdate : FirmwareUpdateState()
}
