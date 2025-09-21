package net.agolyakov.tetrisclockble.service.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.dropWhile
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
    private val bluetoothAdapterProvider: BluetoothAdapterProvider
) {
    private val _tag = "BluetoothService"

    private var isOtaUpdateMode = false

    fun enterOtaUpdateMode() {
        isOtaUpdateMode = true
        Log.d("BluetoothService", "Entered OTA update mode - connections will be preserved")
    }

    fun exitOtaUpdateMode() {
        isOtaUpdateMode = false
        Log.d("BluetoothService", "Exited OTA update mode")
    }

    fun shouldPreserveConnection(): Boolean {
        return isOtaUpdateMode
    }

    // Firmware Version
    private var _firmwareVersion: String = "Unknown"
    private val _tetrisClockFirmwareVersion = MutableStateFlow(_firmwareVersion)
    val tetrisClockFirmwareVersion: StateFlow<String> = _tetrisClockFirmwareVersion
    private val _versionReadEvent = MutableStateFlow<Unit?>(null)
    private val firmwareVersionReadCharacteristicHandler = VersionReadCharacteristicHandler(
        version = _tetrisClockFirmwareVersion,
        readEvent = _versionReadEvent
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

    private val _currentDevice = MutableStateFlow<TetrisClockDevice?>(null)
    val currentDevice: StateFlow<TetrisClockDevice?> = _currentDevice.asStateFlow()

    private var _lastConnectedDevice : TetrisClockDevice? = null

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
        firmwareVersionReadCharacteristicHandler)

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
            Log.e(_tag, "Connection failed with status code, $reason")
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
        if (_connectionState.value is ConnectionState.Connecting ||
            _connectionState.value is ConnectionState.Connected) {
            Log.w(_tag, "Already connecting/connected, ignoring new connection request")
            return
        }

        _connectionState.value = ConnectionState.Connecting
        val device = bluetoothAdapterProvider.getAdapter().getRemoteDevice(tetrisClockDevice.macAddress)
        bleManager.connect(device)
            .retry(2, 100)
            .useAutoConnect(false)
            .done {
                _currentDevice.value = tetrisClockDevice
                _lastConnectedDevice = tetrisClockDevice
                Log.i(_tag, "Connection success!")
            }
            .fail { _, status ->
                Log.e(_tag, "Connection failed, $status")
                _connectionState.value = ConnectionState.Error("Connection failed with status code: $status")
            }
            .enqueue()
    }

    fun disconnect() {
        if (_connectionState.value is ConnectionState.Disconnecting ||
            _connectionState.value is ConnectionState.Disconnected) {
            Log.w(_tag, "Already disconnecting/disconnected, ignoring")
            return
        }

        _connectionState.value = ConnectionState.Disconnecting

        bleManager.disconnect()
            .done {
                _currentDevice.value = null
            }
            .fail { _, status ->
                Log.e(_tag, "Disconnection failed with status code: $status")
                _currentDevice.value = null
            }
            .enqueue()
        _currentDevice.value = null
    }

    private var _shouldMaintainConnection = MutableStateFlow(false)

    fun setShouldMaintainConnection(shouldMaintain: Boolean) {
        _shouldMaintainConnection.value = shouldMaintain
        if (!shouldMaintain) {
            disconnect()
        }
    }

    fun tryReconnect() {
        _lastConnectedDevice?.let {
            if (_shouldMaintainConnection.value &&
                _connectionState.value is ConnectionState.Disconnected)
            {
                Log.i(_tag, "Reconnecting to last device: ${it.deviceName}")
                connect(it)
            }
        }
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
                bleManager.setTurnOnAlarmCharacteristic(newAlarm)
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

    suspend fun getCurrentVersion(): String {
        return withTimeout(5000L) {
            _versionReadEvent.value = null
            bleManager.getVersionCharacteristic()

            _versionReadEvent
                .filterNotNull()
                .first()

            _tetrisClockFirmwareVersion.value
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
}