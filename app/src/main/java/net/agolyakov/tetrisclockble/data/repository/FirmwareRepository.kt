package net.agolyakov.tetrisclockble.data.repository
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.agolyakov.tetrisclockble.data.model.github.GithubRelease
import net.agolyakov.tetrisclockble.service.bluetooth.BluetoothService
import net.agolyakov.tetrisclockble.service.bluetooth.TetrisClockBleManager.Companion.OTA_CMD_START
import net.agolyakov.tetrisclockble.service.bluetooth.TetrisClockBleManager.Companion.OTA_CMD_END
import net.agolyakov.tetrisclockble.service.bluetooth.TetrisClockBleManager.Companion.OTA_CMD_ABORT
import net.agolyakov.tetrisclockble.service.bluetooth.TetrisClockBleManager.Companion.OTA_CMD_SWITCH_REBOOT
import okhttp3.OkHttpClient
import okhttp3.Request
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

    suspend fun getCurrentVersion(): String {
        return bluetoothService.getCurrentVersion()
    }

    suspend fun checkForUpdates(includePreReleases: Boolean = false): GithubRelease? {
        val currentVersion = getCurrentVersion()
        val latestRelease = githubRepository.getLatestRelease(includePreReleases)
        return if (isNewVersionAvailable(currentVersion, latestRelease.tagName)) {
            latestRelease
        } else {
            null
        }
    }

    suspend fun downloadFirmware(
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(downloadUrl)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Failed to download firmware: ${response.code}")
        }

        val body = response.body ?: throw IOException("Empty response body")
        val contentLength = body.contentLength()
        val inputStream = body.byteStream()

        val outputFile = File(context.cacheDir, "firmware_${System.currentTimeMillis()}.bin")
        FileOutputStream(outputFile).use { outputStream ->
            val buffer = ByteArray(8192)
            var totalRead: Long = 0
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead

                if (contentLength > 0) {
                    val progress = (totalRead.toFloat() / contentLength.toFloat()) * 100f
                    onProgress(progress.coerceIn(0f, 100f))
                }
            }
        }

        outputFile
    }

    suspend fun installFirmware(
        firmwareFile: File,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        var inputStream: FileInputStream? = null
        try {
            // 1. Начинаем OTA процесс
            val startSuccess = bluetoothService.setOtaControlCharacteristic(
                byteArrayOf(OTA_CMD_START)
            )
            if (!startSuccess) throw IOException("Failed to start OTA process")

            // 2. Отправляем данные прошивки
            inputStream = FileInputStream(firmwareFile)
            val buffer = ByteArray(244)
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
                delay(20)
            }

            // 3. Завершаем OTA процесс
            val endSuccess = bluetoothService.setOtaControlCharacteristic(
                byteArrayOf(OTA_CMD_END)
            )
            if (!endSuccess) throw IOException("Failed to end OTA process")

            // 4. Переключаем на новую прошивку и перезагружаем
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
            val latestRelease = checkForUpdates(includePreReleases)
                ?: return@withContext UpdateResult.NoUpdateAvailable(currentVersion)

            // 3. Скачиваем прошивку
            onProgress(20f, "Downloading firmware...")
            val firmwareFile = downloadFirmware(
                latestRelease.assets.first { it.name.endsWith(".bin") }.browserDownloadUrl
            ) { downloadProgress ->
                val overallProgress = 20f + downloadProgress * 0.4f
                onProgress(overallProgress, "Downloading...")
            }

            // 4. Устанавливаем прошивку
            onProgress(60f, "Installing firmware...")
            installFirmware(firmwareFile) { installProgress ->
                val overallProgress = 60f + installProgress * 0.3f
                onProgress(overallProgress, "Installing...")
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

    private fun isNewVersionAvailable(current: String, latest: String): Boolean {
        // Реализуйте сравнение версий
        return latest != current
    }
}