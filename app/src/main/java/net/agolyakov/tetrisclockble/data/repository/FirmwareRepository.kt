package net.agolyakov.tetrisclockble.data.repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.agolyakov.tetrisclockble.service.bluetooth.BluetoothService
import net.agolyakov.tetrisclockble.service.bluetooth.TetrisClockBleManager.Companion.OTA_CMD_START
import net.agolyakov.tetrisclockble.service.bluetooth.TetrisClockBleManager.Companion.OTA_CMD_END
import net.agolyakov.tetrisclockble.service.bluetooth.TetrisClockBleManager.Companion.OTA_CMD_ABORT
import net.agolyakov.tetrisclockble.service.bluetooth.TetrisClockBleManager.Companion.OTA_CMD_SWITCH_REBOOT
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirmwareRepository @Inject constructor(
    private val bluetoothService: BluetoothService
) {
    suspend fun getOtaStatus(): String {
        return bluetoothService.getOtaStatus()
    }

    suspend fun switchAndReboot() {
        val success = bluetoothService.setOtaControlCharacteristic(
            byteArrayOf(OTA_CMD_SWITCH_REBOOT)
        )
        if (!success) {
            throw IOException("Failed to send switch and reboot command")
        }
    }

    suspend fun abortOta() {
        val success = bluetoothService.setOtaControlCharacteristic(
            byteArrayOf(OTA_CMD_ABORT)
        )
        if (!success) {
            throw IOException("Failed to send abort command")
        }
    }

    suspend fun getCurrentVersion(): String {
        return bluetoothService.getCurrentVersion()
    }

    // Внутренние методы больше не нужны, т.к. используем прямое обращение к bluetoothService
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
                withContext(Dispatchers.Main) {
                    onProgress(progress.coerceIn(0f, 100f))
                }
                delay(20)
            }

            // 3. Завершаем OTA процесс
            val endSuccess = bluetoothService.setOtaControlCharacteristic(
                byteArrayOf(OTA_CMD_END)
            )
            if (!endSuccess) throw IOException("Failed to end OTA process")

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
}