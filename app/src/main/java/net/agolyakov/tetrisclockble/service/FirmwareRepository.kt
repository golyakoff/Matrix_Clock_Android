package net.agolyakov.tetrisclockble.service

import android.content.Context
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.agolyakov.tetrisclockble.ble.MyBleManager
import net.agolyakov.tetrisclockble.ble.MyBleManager.Companion.OTA_CMD_ABORT
import net.agolyakov.tetrisclockble.ble.MyBleManager.Companion.OTA_CMD_END
import net.agolyakov.tetrisclockble.ble.MyBleManager.Companion.OTA_CMD_GET_STATUS
import net.agolyakov.tetrisclockble.ble.MyBleManager.Companion.OTA_CMD_START
import net.agolyakov.tetrisclockble.ble.MyBleManager.Companion.OTA_CMD_SWITCH_REBOOT
import net.agolyakov.tetrisclockble.ble.handlers.VersionReadCharacteristicHandler
import okhttp3.OkHttpClient
import java.io.IOException
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirmwareRepository @Inject constructor(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val bleManager: MyBleManager,
    private val otaVersionHandler: VersionReadCharacteristicHandler
) {
    suspend fun installFirmware(
        firmwareFile: File,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        var inputStream: FileInputStream? = null
        try {
            // 1. Начинаем OTA процесс
            setOtaControlCharacteristic(byteArrayOf(OTA_CMD_START))

            // 2. Отправляем данные прошивки
            inputStream = FileInputStream(firmwareFile)
            val buffer = ByteArray(244) // Максимальный размер для BLE пакета
            var totalBytesSent = 0L
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                // Отправляем пакет данных через OTA DATA характеристику
                sendOtaData(buffer.copyOf(bytesRead))
                totalBytesSent += bytesRead

                // Обновляем прогресс
                val progress = (totalBytesSent.toFloat() / firmwareFile.length().toFloat()) * 100f
                withContext(Dispatchers.Main) {
                    onProgress(progress.coerceIn(0f, 100f))
                }

                // Небольшая задержка для стабильности BLE
                delay(20)
            }

            // 3. Завершаем OTA процесс
            setOtaControlCharacteristic(byteArrayOf(OTA_CMD_END))

        } catch (e: Exception) {
            // Прерываем OTA в случае ошибки
            try {
                setOtaControlCharacteristic(byteArrayOf(OTA_CMD_ABORT))
            } catch (ignore: Exception) {}
            throw IOException("Firmware installation failed: ${e.message}", e)
        } finally {
            inputStream?.close()
        }
    }

    suspend fun getOtaStatus(): String {
        return suspendCancellableCoroutine { continuation ->
            // Отправляем команду получения статуса
            bleManager.setOtaControlCharacteristic(byteArrayOf(OTA_CMD_GET_STATUS)) { success ->
                if (success) {
                    // Статус будет получен через уведомления
                    // Нужно добавить обработчик уведомлений для OTA статуса
                    continuation.resume("Status request sent")
                } else {
                    continuation.resume("Failed to get status")
                }
            }
        }
    }

    suspend fun switchAndReboot() {
        setOtaControlCharacteristic(byteArrayOf(OTA_CMD_SWITCH_REBOOT))
    }

    suspend fun abortOta() {
        setOtaControlCharacteristic(byteArrayOf(OTA_CMD_ABORT))
    }

    suspend fun getCurrentVersion(): String {
        return suspendCancellableCoroutine { continuation ->
            bleManager.getVersionCharacteristic()

            continuation.invokeOnCancellation {
                otaVersionHandler.cancel()
            }
        }
    }

    private suspend fun setOtaControlCharacteristic(command: ByteArray) {
        suspendCancellableCoroutine { continuation ->
            bleManager.setOtaControlCharacteristic(command) { success ->
                if (success) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(IOException("Failed to send OTA control command"))
                }
            }
        }
    }

    private suspend fun sendOtaData(data: ByteArray) {
        suspendCancellableCoroutine { continuation ->
            bleManager.setOtaDataCharacteristic(data) { success ->
                if (success) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(IOException("Failed to send OTA data"))
                }
            }
        }
    }
}