package net.agolyakov.tetrisclockble.data.repository

import android.content.Context
import android.util.Log
import com.yourcompany.yourapp.utils.HashUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.agolyakov.tetrisclockble.R
import net.agolyakov.tetrisclockble.data.model.ble.ConnectionState
import net.agolyakov.tetrisclockble.data.model.github.GithubAsset
import net.agolyakov.tetrisclockble.data.model.github.GithubRelease
import net.agolyakov.tetrisclockble.service.bluetooth.BluetoothService
import net.agolyakov.tetrisclockble.service.bluetooth.TetrisClockBleManager.Companion.OTA_CMD_START
import net.agolyakov.tetrisclockble.service.bluetooth.TetrisClockBleManager.Companion.OTA_CMD_END
import net.agolyakov.tetrisclockble.service.bluetooth.TetrisClockBleManager.Companion.OTA_CMD_ABORT
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
                val asset = release.assets.first { it.name.endsWith("_release_4mb_fw.bin") }
                val firmwareFile = downloadFirmwareAsset(asset, onProgress)
                validateFirmware(firmwareFile, asset)
                UpdateState.ReadyToInstall(firmwareFile, release)
            } catch (e: Exception) {
                UpdateState.Error(e.message ?: context.getString(R.string.mc_ota_download_failed))
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
                UpdateState.Error(e.message ?: context.getString(R.string.mc_ota_install_failed))
            }
        }
    }

    suspend fun performCompleteUpdate(
        includePreReleases: Boolean = false,
        onProgress: (Float, String) -> Unit
    ): UpdateResult = withContext(Dispatchers.IO) {
        try {
            // 1. Проверяем текущую версию
            onProgress(0f, context.getString(R.string.mc_ota_getting_current_version))
            val currentVersion = getCurrentVersion()

            // 2. Проверяем обновления
            onProgress(1f, context.getString(R.string.mc_ota_checking_updates))
            val updateState = checkForUpdates(includePreReleases)
            if (updateState is UpdateState.NoUpdate) {
                return@withContext UpdateResult.NoUpdateAvailable(currentVersion)
            }

            val release = (updateState as UpdateState.UpdateAvailable).release

            // 3. Скачиваем прошивку
            onProgress(2f, context.getString(R.string.mc_ota_downloading))
            val downloadResult = downloadFirmware(release) { downloadProgress ->
                val overallProgress = 2f + downloadProgress * 0.03f
                onProgress(overallProgress, context.getString(R.string.mc_ota_downloading))
            }

            if (downloadResult is UpdateState.Error) {
                return@withContext UpdateResult.Error(downloadResult.message)
            }

            val firmwareFile = (downloadResult as UpdateState.ReadyToInstall).file

            // 4. Устанавливаем прошивку
            onProgress(5f, context.getString(R.string.mc_ota_installing))
            val installResult = installFirmware(firmwareFile) { installProgress ->
                val overallProgress = 5f + installProgress * 0.94f
                onProgress(overallProgress, context.getString(R.string.mc_ota_installing))
            }

            if (installResult is UpdateState.Error) {
                return@withContext UpdateResult.Error(installResult.message)
            }

            // 5. Проверяем обновление
            onProgress(99f, context.getString(R.string.mc_ota_rebooting_device))
            bluetoothService.disconnect()

            delay(5000)

            var retries = 0
            while (retries < 20) {
                try {
                    if (bluetoothService.connectionState.value != ConnectionState.Connected) {
                        bluetoothService.tryReconnect()
                        delay(2000)
                    }

                    val uploadedFirmwareVersion = updateState.release.tagName
                    val realDeviceFirmwareVersion = getCurrentVersion()
                    if (realDeviceFirmwareVersion == uploadedFirmwareVersion) {
                        onProgress(100f, context.getString(R.string.mc_ota_complete))
                        return@withContext UpdateResult.Success(currentVersion, realDeviceFirmwareVersion)
                    }
                    delay(1000)
                    retries++
                } catch (e: Exception) {
                    delay(1000)
                    retries++
                }
            }

            UpdateResult.Error(context.getString(R.string.mc_ota_verify_failed))

        } catch (e: CancellationException) {
            abortOta()
            UpdateResult.Cancelled
        } catch (e: Exception) {
            abortOta()
            UpdateResult.Error(context.getString(R.string.mc_ota_error, e.message), e)
        }
    }

    suspend fun abortOta() {
        bluetoothService.exitOtaUpdateMode()
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
            bluetoothService.enterOtaUpdateMode();
            // Ask for the fastest connection interval for the whole transfer: with write-with-
            // response, throughput is one chunk per interval, so this is the single biggest speed
            // lever and also cuts the write timeouts that abort the transfer on flaky links.
            bluetoothService.requestHighConnectionPriority()

            val startCommand = createStartCommand(firmwareFile.length())
            val startSuccess = bluetoothService.setOtaControlCharacteristic(startCommand)
            if (!startSuccess) throw IOException("Failed to start OTA process")

            // Chunk must fit into a single ATT Write Request (MTU-3, 512 with the
            // firmware's MTU of 515), otherwise Android falls back to slow
            // Prepare/Execute long writes. No pacing delay is needed: the firmware
            // queues each chunk for its flash-writer task and a full queue delays
            // the write response, which naturally throttles this loop.
            val mtu = bluetoothService.getNegotiatedMtu()
            val chunkSize = (mtu - 3).coerceIn(20, 512)
            // A negotiated MTU near 23 (chunkSize ~20) means the peer capped it: the transfer then
            // needs ~25x more round-trips and crawls. Logged so a slow update can be diagnosed.
            Log.i(TAG, "OTA transfer: negotiated MTU=$mtu, chunkSize=$chunkSize, size=${firmwareFile.length()} bytes")

            inputStream = FileInputStream(firmwareFile)
            val buffer = ByteArray(chunkSize)
            var totalBytesSent = 0L
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                val chunk = buffer.copyOf(bytesRead)

                // Retry a failed chunk a few times before giving up, so a single dropped write on a
                // weak link no longer aborts the whole update (the "failed at N%" symptom). A write
                // reports failure when its ATT response didn't arrive, so the peer almost always
                // didn't get the chunk - resending is safe; in the rare case the response alone was
                // lost, the final size check catches the resulting mismatch.
                var attempt = 0
                var dataSuccess = false
                while (attempt < OTA_CHUNK_MAX_ATTEMPTS) {
                    dataSuccess = bluetoothService.setOtaDataCharacteristic(chunk)
                    if (dataSuccess) break
                    attempt++
                    Log.w(TAG, "OTA chunk write failed at ${totalBytesSent} bytes, retry $attempt/$OTA_CHUNK_MAX_ATTEMPTS")
                    delay(OTA_CHUNK_RETRY_DELAY_MS * attempt)
                }
                if (!dataSuccess) {
                    throw IOException("Failed to send OTA data packet after $OTA_CHUNK_MAX_ATTEMPTS attempts")
                }

                totalBytesSent += bytesRead
                val progress = (totalBytesSent.toFloat() / firmwareFile.length()) * 100f
                onProgress(progress.coerceIn(0f, 100f))
            }

            bluetoothService.setOtaControlCharacteristic(byteArrayOf(OTA_CMD_END))

        } catch (e: Exception) {
            try {
                bluetoothService.setOtaControlCharacteristic(byteArrayOf(OTA_CMD_ABORT))
            } catch (ignore: Exception) {
            }
            throw IOException("Firmware installation failed: ${e.message}", e)
        } finally {
            inputStream?.close()
            bluetoothService.requestBalancedConnectionPriority()
            bluetoothService.exitOtaUpdateMode();
        }
    }

    private fun createStartCommand(fileSize: Long): ByteArray {
        val buffer = ByteBuffer.allocate(5)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        buffer.put(OTA_CMD_START)
        buffer.putInt(fileSize.toInt())

        return buffer.array()
    }

    private fun isNewVersionAvailable(current: String, latest: String): Boolean {
        return latest != current
    }

    companion object {
        private const val TAG = "FirmwareRepository"

        // Per-chunk send retry: how many times to resend a chunk whose BLE write failed before
        // aborting the whole update, and the base backoff between attempts (grows per attempt).
        private const val OTA_CHUNK_MAX_ATTEMPTS = 5
        private const val OTA_CHUNK_RETRY_DELAY_MS = 20L
    }
}