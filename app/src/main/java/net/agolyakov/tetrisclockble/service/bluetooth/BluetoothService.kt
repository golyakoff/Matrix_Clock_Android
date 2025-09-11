package net.agolyakov.tetrisclockble.service.bluetooth

import android.bluetooth.BluetoothDevice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import net.agolyakov.tetrisclockble.data.model.ble.ConnectionState
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockAlarm
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockAlarmType
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockDevice
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockTime
import net.agolyakov.tetrisclockble.service.bluetooth.handlers.AgingOffsetReadCharacteristicHandler
import net.agolyakov.tetrisclockble.service.bluetooth.handlers.AutoBrightnessReadCharacteristicHandler
import net.agolyakov.tetrisclockble.service.bluetooth.handlers.ManualBrightnessReadCharacteristicHandler
import net.agolyakov.tetrisclockble.service.bluetooth.handlers.OnOffReadCharacteristicHandler
import net.agolyakov.tetrisclockble.service.bluetooth.handlers.OtaStatusReadCharacteristicHandler
import net.agolyakov.tetrisclockble.service.bluetooth.handlers.RtcTemperatureReadCharacteristicHandler
import net.agolyakov.tetrisclockble.service.bluetooth.handlers.TimeReadCharacteristicHandler
import net.agolyakov.tetrisclockble.service.bluetooth.handlers.TurnOffAlarmReadCharacteristicHandler
import net.agolyakov.tetrisclockble.service.bluetooth.handlers.TurnOnAlarmReadCharacteristicHandler
import net.agolyakov.tetrisclockble.service.bluetooth.handlers.VersionReadCharacteristicHandler
import no.nordicsemi.android.ble.observer.ConnectionObserver
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class BluetoothService @Inject constructor(
    private val bluetoothAdapterProvider: BluetoothAdapterProvider,
) {
    // Region: StateFlows and Handlers

    // Firmware Version
    private var _firmwareVersion: String = "Unknown"
    private val _tetrisClockFirmwareVersion = MutableStateFlow(_firmwareVersion)
    val tetrisClockFirmwareVersion: StateFlow<String> = _tetrisClockFirmwareVersion
    private val firmwareVersionReadCharacteristicHandler = VersionReadCharacteristicHandler(
        _tetrisClockFirmwareVersion
    )

    // Time
    private var _bleDeviceTime: TetrisClockTime = TetrisClockTime()
    private var _tetrisClockBleDeviceTime = MutableStateFlow(_bleDeviceTime)
    val tetrisClockBleDeviceTime: StateFlow<TetrisClockTime> = _tetrisClockBleDeviceTime
    private val timeReadCharacteristicHandler = TimeReadCharacteristicHandler(
        _tetrisClockBleDeviceTime
    )

    // ON/OFF
    private var _onOffState: Boolean = true
    private val _tetrisClockIsOn = MutableStateFlow(_onOffState)
    val tetrisClockIsOn: StateFlow<Boolean> = _tetrisClockIsOn
    private val onOffReadCharacteristicHandler = OnOffReadCharacteristicHandler(
        _tetrisClockIsOn
    )

    // Manual Brightness
    private var _manualBrightnessState: Byte = 0
    private val _tetrisClockManualBrightness = MutableStateFlow(_manualBrightnessState)
    val tetrisClockManualBrightness : StateFlow<Byte> = _tetrisClockManualBrightness
    private val manualBrightnessReadCharacteristicHandler =
        ManualBrightnessReadCharacteristicHandler(
            _tetrisClockManualBrightness
        )

    // Is Automatic Brightness Mode
    private var _isAutoBrightness: Boolean = false
    private val _tetrisClockIsAutoBrightness = MutableStateFlow(_isAutoBrightness)
    val tetrisClockIsAutoBrightness: StateFlow<Boolean> = _tetrisClockIsAutoBrightness
    private val autoBrightnessReadCharacteristicHandler = AutoBrightnessReadCharacteristicHandler(
        _tetrisClockIsAutoBrightness
    )

    // Turn ON Alarm
    private var _turnOnAlarm: TetrisClockAlarm = TetrisClockAlarm()
    private val _tetrisClockTurnOnAlarm = MutableStateFlow(_turnOnAlarm)
    val tetrisClockTurnOnAlarm: StateFlow<TetrisClockAlarm> = _tetrisClockTurnOnAlarm
    private val turnOnAlarmReadCharacteristicHandler = TurnOnAlarmReadCharacteristicHandler(
        _tetrisClockTurnOnAlarm
    )

    // Turn OFF Alarm
    private var _turnOffAlarm: TetrisClockAlarm = TetrisClockAlarm()
    private val _tetrisClockTurnOffAlarm = MutableStateFlow(_turnOffAlarm)
    val tetrisClockTurnOffAlarm: StateFlow<TetrisClockAlarm> = _tetrisClockTurnOffAlarm
    private val turnOffAlarmReadCharacteristicHandler = TurnOffAlarmReadCharacteristicHandler(
        _tetrisClockTurnOffAlarm
    )

    // RTC Aging Offset
    private var _agingOffsetState: Int = 0
    private val _tetrisClockAgingOffset  = MutableStateFlow(_agingOffsetState)
    val tetrisClockAgingOffset : StateFlow<Int> = _tetrisClockAgingOffset
    private val agingOffsetReadCharacteristicHandler = AgingOffsetReadCharacteristicHandler(
        _tetrisClockAgingOffset
    )

    // RTC Temperature
    private var _rtcTemperatureState: Float = Float.NaN
    private val _tetrisClockRtcTemperature  = MutableStateFlow(_rtcTemperatureState)
    val tetrisClockRtcTemperature : StateFlow<Float> = _tetrisClockRtcTemperature
    private val rtcTemperatureReadCharacteristicHandler = RtcTemperatureReadCharacteristicHandler(
        _tetrisClockRtcTemperature
    )

    // Region: BleManager and Bluetooth internals
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _otaStatus = MutableStateFlow<String?>(null)
    val otaStatus: StateFlow<String?> = _otaStatus
    private val otaStatusHandler = OtaStatusReadCharacteristicHandler { status ->
        _otaStatus.value = status
    }


    private val bleManager: TetrisClockBleManager = TetrisClockBleManager(
        bluetoothAdapterProvider.getContext(),
        timeReadCharacteristicHandler,
        onOffReadCharacteristicHandler,
        manualBrightnessReadCharacteristicHandler,
        autoBrightnessReadCharacteristicHandler,
        turnOnAlarmReadCharacteristicHandler,
        turnOffAlarmReadCharacteristicHandler,
        agingOffsetReadCharacteristicHandler,
        rtcTemperatureReadCharacteristicHandler,
        firmwareVersionReadCharacteristicHandler,
        otaStatusReadCharacteristicHandler)

    private val connectionObserver = object : ConnectionObserver {
        override fun onDeviceConnecting(device: BluetoothDevice) {
            _connectionState.value = ConnectionState.Connecting
        }

        override fun onDeviceConnected(device: BluetoothDevice) {
            _connectionState.value = ConnectionState.Connected
        }

        override fun onDeviceReady(device: BluetoothDevice) {
            _connectionState.value = ConnectionState.Ready
            startReadingAllCharacteristics()
        }

        override fun onDeviceDisconnecting(device: BluetoothDevice) {
            _connectionState.value = ConnectionState.Disconnecting
        }

        override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
            _connectionState.value = ConnectionState.Error("Failed to connect: $reason")
        }

        override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    init {
        bleManager.connectionObserver = connectionObserver
    }

    fun connect(tetrisClockDevice: TetrisClockDevice) {
        val device = bluetoothAdapterProvider.getAdapter().getRemoteDevice(tetrisClockDevice.macAddress)
        bleManager.connect(device)
            .retry(2, 100)
            .useAutoConnect(false)
            .done {
                Log.i("BluetoothService", "Connection success!")
            }
            .fail { _, status ->
                Log.e("BluetoothService", "Connection failed, $status")
                _connectionState.value = ConnectionState.Error("Connection failed: $status")
            }
            .enqueue()
    }

    fun disconnect() {
        bleManager.disconnect().enqueue()
    }

    private fun startReadingAllCharacteristics() {
        bleManager.getTimeCharacteristic()
        bleManager.getOnOffCharacteristic()
        bleManager.getManualBrightnessCharacteristic()
        bleManager.getAutoBrightnessCharacteristic()
        bleManager.getTurnOnAlarmCharacteristic()
        bleManager.getTurnOffAlarmCharacteristic()
        bleManager.getAgingOffsetCharacteristic()
        bleManager.getRtcTemperatureCharacteristic()
        bleManager.getVersionCharacteristic()
    }

    // Region: BLE GATT characteristic setters

    fun setTimeCharacteristic(time: LocalDateTime) {
        _bleDeviceTime = TetrisClockTime(time)
        _tetrisClockBleDeviceTime.value = _bleDeviceTime

        if (bleManager.isReady)
        {
            bleManager.setTimeCharacteristic(_bleDeviceTime)
        }
    }

    fun toggleOnOffCharacteristic() {
        val on = !_tetrisClockIsOn.value

        _onOffState = on
        _tetrisClockIsOn.value = on

        if (bleManager.isReady) {
            bleManager.setOnOffCharacteristic(on)
        }
    }

    fun setManualBrightnessCharacteristic(brightness: Byte) {
        _manualBrightnessState = brightness
        _tetrisClockManualBrightness.value = brightness

        if (bleManager.isReady)
        {
            bleManager.setManualBrightnessCharacteristic(brightness)
        }
    }

    fun setAgingOffsetCharacteristic(agingOffset: Int) {
        _agingOffsetState = agingOffset
        _tetrisClockAgingOffset.value = agingOffset

        if (bleManager.isReady) {
            bleManager.setAgingOffsetCharacteristic(agingOffset)
        }
    }

    fun toggleAutoBrightnessCharacteristic() {
        val isAuto = !_tetrisClockIsAutoBrightness.value

        _isAutoBrightness = isAuto
        _tetrisClockIsAutoBrightness.value = isAuto

        if (bleManager.isReady) {
            bleManager.setAutoBrightnessCharacteristic(isAuto)
        }
    }

    fun setTurnOnAlarmCharacteristic(isActive: Boolean, hours: Byte, minutes: Byte) {
        _turnOnAlarm = TetrisClockAlarm(isActive, hours, minutes)
        _tetrisClockTurnOnAlarm.value = _turnOnAlarm

        if (bleManager.isReady)
        {
            bleManager.setTurnOnAlarmCharacteristic(_turnOnAlarm)
        }
    }

    fun setTurnOffAlarmCharacteristic(isActive: Boolean, hours: Byte, minutes: Byte) {
        _turnOffAlarm = TetrisClockAlarm(isActive, hours, minutes)
        _tetrisClockTurnOffAlarm.value = _turnOffAlarm

        if (bleManager.isReady)
        {
            bleManager.setTurnOffAlarmCharacteristic(_turnOffAlarm)
        }
    }

    fun setAlarmTime(alarmType: TetrisClockAlarmType, hour: Int, minute: Int, isActive: Boolean) {
        when (alarmType) {
            TetrisClockAlarmType.TURN_ON -> {
                setTurnOnAlarmCharacteristic(
                    isActive,
                    hour.toByte(),
                    minute.toByte())
            }
            TetrisClockAlarmType.TURN_OFF -> {
                setTurnOffAlarmCharacteristic(isActive,
                    hour.toByte(),
                    minute.toByte())
            }
        }
    }

    fun syncBleWithPhone() {
        val mcNow = TetrisClockTime.Companion.now()
        bleManager.setTimeCharacteristic(mcNow)
        _bleDeviceTime = mcNow
        _tetrisClockBleDeviceTime.value = mcNow
    }

    fun toggleAlarmActive(alarmType: TetrisClockAlarmType) {
        when (alarmType) {
            TetrisClockAlarmType.TURN_ON -> {
                val current = _tetrisClockTurnOnAlarm.value
                val newAlarm = current.copy(isActive = !current.isActive)
                _tetrisClockTurnOnAlarm.value = newAlarm
                bleManager.setTurnOffAlarmCharacteristic(newAlarm)
            }
            TetrisClockAlarmType.TURN_OFF -> {
                val current = _tetrisClockTurnOffAlarm.value
                val newAlarm = current.copy(isActive = !current.isActive)
                _tetrisClockTurnOffAlarm.value = newAlarm
                bleManager.setTurnOffAlarmCharacteristic(newAlarm)
            }
        }
    }

    // Region: OTA Update

    private val _versionResult = MutableStateFlow<Result<String>?>(null)
    private val _otaControlResult = MutableStateFlow<Result<Boolean>?>(null)
    private val _otaDataResult = MutableStateFlow<Result<Boolean>?>(null)

    private suspend fun <T> waitForResult(
        resultFlow: MutableStateFlow<Result<T>?>,
        timeout: Long = 5000L,
        operation: () -> Unit
    ): T {
        return withTimeout(timeout) {
            resultFlow.value = null // Сбрасываем предыдущий результат
            operation() // Запускаем операцию

            // Ждём результат в Flow
            resultFlow
                .filterNotNull()
                .first()
                .getOrThrow()
        }
    }

    suspend fun getCurrentVersion(): String {
        return waitForResult(_versionResult) {
            bleManager.getVersionCharacteristic()
        }
    }

    suspend fun setOtaControlCharacteristic(command: ByteArray): Boolean {
        return suspendCancellableCoroutine { continuation ->
            bleManager.setOtaControlCharacteristic(command) { success ->
                if (success) {
                    continuation.resume(true)
                } else {
                    continuation.resume(false)
                }
            }
        }
    }

    suspend fun setOtaDataCharacteristic(data: ByteArray): Boolean {
        return suspendCancellableCoroutine { continuation ->
            bleManager.setOtaDataCharacteristic(data) { success ->
                if (success) {
                    continuation.resume(true)
                } else {
                    continuation.resume(false)
                }
            }
        }
    }



    // Метод для получения OTA статуса
    suspend fun getOtaStatus(): String {
        return suspendCancellableCoroutine { continuation ->
            // Сбрасываем предыдущий статус
            _otaStatus.value = OtaStatus.UNDEFINED

            // Отправляем команду запроса статуса
            bleManager.setOtaControlCharacteristic(
                byteArrayOf(TetrisClockBleManager.OTA_CMD_GET_STATUS)
            ) { success ->
                if (success) {
                    // Ждём получения статуса через уведомление
                    try {
                        // Ждём обновления статуса в течение таймаута
                        val status = withTimeout(5000) {
                            otaStatus
                                .filterNotNull()
                                .first()
                        }
                        continuation.resume(status)
                    } catch (e: TimeoutCancellationException) {
                        continuation.resume("Timeout waiting for status")
                    } catch (e: Exception) {
                        continuation.resume("Error: ${e.message}")
                    }
                } else {
                    continuation.resume("Failed to send status request")
                }
            }
        }
    }
}