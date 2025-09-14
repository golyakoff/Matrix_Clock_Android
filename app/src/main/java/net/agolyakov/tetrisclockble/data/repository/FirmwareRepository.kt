package net.agolyakov.tetrisclockble.data.repository

import android.content.Context
import com.yourcompany.yourapp.utils.HashUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.agolyakov.tetrisclockble.data.model.github.GithubAsset
import net.agolyakov.tetrisclockble.data.model.github.GithubRelease
import net.agolyakov.tetrisclockble.service.bluetooth.BluetoothService
import net.agolyakov.tetrisclockble.service.bluetooth.TetrisClockBleManager.Companion.OTA_CMD_START
import net.agolyakov.tetrisclockble.service.bluetooth.TetrisClockBleManager.Companion.OTA_CMD_END
import net.agolyakov.tetrisclockble.service.bluetooth.TetrisClockBleManager.Companion.OTA_CMD_ABORT
import net.agolyakov.tetrisclockble.service.bluetooth.TetrisClockBleManager.Companion.OTA_CMD_SWITCH_REBOOT
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class FirmwareRepository @Inject constructor(
    private val bluetoothService: BluetoothService,
    private val githubRepository: GithubRepository,
    private val okHttpClient: OkHttpClient,
    @ApplicationContext private val context: Context
) {
    sealed class UpdateResult {
        data class Success(val oldVersion: String, val newVersion: String) : UpdateResult()
        data class NoUpdateAvailable(val currentVersion: String) : UpdateResult()
        data class Error(val message: String, val cause: Exception? = null) : UpdateResult()
        object Cancelled : UpdateResult()
    }

    sealed class UpdateState {
        object Idle : UpdateState()
        object Checking : UpdateState()
        object Processing : UpdateState()
        data class UpdateAvailable(val release: GithubRelease, val currentVersion: String) : UpdateState()
        data class NoUpdate(val currentVersion: String) : UpdateState()
        data class Downloading(val progress: Float) : UpdateState()
        data class ReadyToInstall(val file: File, val release: GithubRelease) : UpdateState()
        data class Installing(val progress: Float) : UpdateState()
        object Success : UpdateState()
        data class Error(val message: String) : UpdateState()
    }

    suspend fun getCurrentVersion(): String {
        return bluetoothService.getCurrentVersion()
    }

    suspend fun checkForUpdates(includePreReleases: Boolean = false): UpdateState {
        val currentVersion = getCurrentVersion()
        val latestRelease = githubRepository.getLatestRelease(includePreReleases)
        return if (isNewVersionAvailable(currentVersion, latestRelease.tagName)) {
            UpdateState.UpdateAvailable(latestRelease, currentVersion)
        } else {
            UpdateState.NoUpdate(currentVersion)
        }
    }

    suspend fun downloadFirmware(
        release: GithubRelease,
        onProgress: (Float) -> Unit = {}
    ): UpdateState {
        return withContext(Dispatchers.IO) {
            try {
                val asset = release.assets.first { it.name.endsWith("_debug_4mb_fw.bin") }
                val firmwareFile = downloadFirmwareAsset(asset, onProgress)
                validateFirmware(firmwareFile, asset)
                UpdateState.ReadyToInstall(firmwareFile, release)
            } catch (e: Exception) {
                UpdateState.Error(e.message ?: "Download failed")
            }
        }
    }

    suspend fun installFirmware(
        firmwareFile: File,
        onProgress: (Float) -> Unit
    ): UpdateState {
        return withContext(Dispatchers.IO) {
            try {
                installFirmwareInternal(firmwareFile, onProgress)
                UpdateState.Success
            } catch (e: Exception) {
                UpdateState.Error(e.message ?: "Installation failed")
            }
        }
    }

    suspend fun performCompleteUpdate(
        includePreReleases: Boolean = false,
        onProgress: (Float, String) -> Unit
    ): UpdateResult = withContext(Dispatchers.IO) {
        try {
            // 1. Проверяем текущую версию
            onProgress(0f, "Checking current version...")
            val currentVersion = getCurrentVersion()

            // 2. Проверяем обновления
            onProgress(10f, "Checking for updates...")
            val updateState = checkForUpdates(includePreReleases)
            if (updateState is UpdateState.NoUpdate) {
                return@withContext UpdateResult.NoUpdateAvailable(currentVersion)
            }

            val release = (updateState as UpdateState.UpdateAvailable).release

            // 3. Скачиваем прошивку
            onProgress(20f, "Downloading firmware...")
            val downloadResult = downloadFirmware(release) { downloadProgress ->
                val overallProgress = 20f + downloadProgress * 0.4f
                onProgress(overallProgress, "Downloading...")
            }

            if (downloadResult is UpdateState.Error) {
                return@withContext UpdateResult.Error(downloadResult.message)
            }

            val firmwareFile = (downloadResult as UpdateState.ReadyToInstall).file

            // 4. Устанавливаем прошивку
            onProgress(60f, "Installing firmware...")
            val installResult = installFirmware(firmwareFile) { installProgress ->
                val overallProgress = 60f + installProgress * 0.3f
                onProgress(overallProgress, "Installing...")
            }

            if (installResult is UpdateState.Error) {
                return@withContext UpdateResult.Error(installResult.message)
            }

            // 5. Проверяем обновление
            onProgress(90f, "Verifying update...")
            delay(5000) // Ждём перезагрузки

            var retries = 0
            while (retries < 10) {
                try {
                    val newVersion = getCurrentVersion()
                    if (newVersion != currentVersion) {
                        onProgress(100f, "Update completed!")
                        return@withContext UpdateResult.Success(currentVersion, newVersion)
                    }
                    delay(1000)
                    retries++
                } catch (e: Exception) {
                    delay(1000)
                    retries++
                }
            }

            UpdateResult.Error("Failed to verify update")

        } catch (e: CancellationException) {
            abortOta()
            UpdateResult.Cancelled
        } catch (e: Exception) {
            abortOta()
            UpdateResult.Error("Update failed: ${e.message}", e)
        }
    }

    suspend fun abortOta() {
        val success = bluetoothService.setOtaControlCharacteristic(
            byteArrayOf(OTA_CMD_ABORT)
        )
        if (!success) {
            throw IOException("Failed to abort OTA process")
        }
    }

    private suspend fun downloadFirmwareAsset(
        asset: GithubAsset,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val responseBody = githubRepository.downloadAsset(asset.id.toString())
        val contentLength = responseBody.contentLength()
        val inputStream = responseBody.byteStream()

        val outputFile = File(context.cacheDir, "firmware_${System.currentTimeMillis()}.bin")
        FileOutputStream(outputFile).use { outputStream ->
            val buffer = ByteArray(8192)
            var totalRead: Long = 0
            var bytesRead: Int
            var lastProgress = -1f

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead

                if (contentLength > 0) {
                    val progress = (totalRead.toFloat() / contentLength.toFloat()) * 100f
                    if (progress - lastProgress >= 1f || progress >= 100f) {
                        lastProgress = progress
                        withContext(Dispatchers.Main) {
                            onProgress(progress.coerceIn(0f, 100f))
                        }
                    }
                }
            }
        }

        withContext(Dispatchers.Main) {
            onProgress(100f)
        }

        outputFile
    }

    private suspend fun validateFirmware(file: File, asset: GithubAsset) {
        if (file.length() != asset.size) {
            throw IOException("File size mismatch: expected ${asset.size}, got ${file.length()}")
        }

        asset.sha256Hash?.let { expectedHash ->
            if (!HashUtils.verifyFileHash(file, expectedHash)) {
                throw IOException("SHA256 hash mismatch")
            }
        }
    }

    private suspend fun installFirmwareInternal(
        firmwareFile: File,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        var inputStream: FileInputStream? = null
        try {
            val startSuccess = bluetoothService.setOtaControlCharacteristic(
                byteArrayOf(OTA_CMD_START)
            )
            if (!startSuccess) throw IOException("Failed to start OTA process")

            inputStream = FileInputStream(firmwareFile)
            val buffer = ByteArray(512)
            var totalBytesSent = 0L
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                val dataSuccess = bluetoothService.setOtaDataCharacteristic(
                    buffer.copyOf(bytesRead)
                )
                if (!dataSuccess) throw IOException("Failed to send OTA data packet")

                totalBytesSent += bytesRead
                val progress = (totalBytesSent.toFloat() / firmwareFile.length()) * 100f
                onProgress(progress.coerceIn(0f, 100f))
                delay(30)
            }

            val endSuccess = bluetoothService.setOtaControlCharacteristic(
                byteArrayOf(OTA_CMD_END)
            )
            if (!endSuccess) throw IOException("Failed to end OTA process")

            val rebootSuccess = bluetoothService.setOtaControlCharacteristic(
                byteArrayOf(OTA_CMD_SWITCH_REBOOT)
            )
            if (!rebootSuccess) {
                throw IOException("Failed to switch and reboot")
            }

        } catch (e: Exception) {
            try {
                bluetoothService.setOtaControlCharacteristic(
                    byteArrayOf(OTA_CMD_ABORT)
                )
            } catch (ignore: Exception) {
            }
            throw IOException("Firmware installation failed: ${e.message}", e)
        } finally {
            inputStream?.close()
        }
    }

    private fun isNewVersionAvailable(current: String, latest: String): Boolean {
        return latest != current
    }
}