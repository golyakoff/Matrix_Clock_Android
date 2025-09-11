package net.agolyakov.tetrisclockble.service

import android.bluetooth.BluetoothDevice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.agolyakov.tetrisclockble.ble.BleDevice
import net.agolyakov.tetrisclockble.ble.MyBleManager
import net.agolyakov.tetrisclockble.ble.TetrisClockAlarm
import net.agolyakov.tetrisclockble.ble.TetrisClockAlarmType
import net.agolyakov.tetrisclockble.ble.TetrisClockTime
import net.agolyakov.tetrisclockble.ble.handlers.AgingOffsetReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.AutoBrightnessReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.ManualBrightnessReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.OnOffReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.RtcTemperatureReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.TimeReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.TurnOffAlarmReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.TurnOnAlarmReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.VersionReadCharacteristicHandler
import no.nordicsemi.android.ble.observer.ConnectionObserver
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothService @Inject constructor(
    private val myBleManager: MyBleManager,
    private val bluetoothAdapterProvider: BluetoothAdapterProvider,
) {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

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
        myBleManager.connectionObserver = connectionObserver
    }

    fun connect(myDevice: BleDevice) {
        val device = bluetoothAdapterProvider.getAdapter().getRemoteDevice(myDevice.deviceMacAddress)
        myBleManager.connect(device)
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
        myBleManager.disconnect()
    }

    fun startReadingAllCharacteristics() {
        myBleManager.getTimeCharacteristic()
        myBleManager.getOnOffCharacteristic()
        myBleManager.getManualBrightnessCharacteristic()
        myBleManager.getAutoBrightnessCharacteristic()
        myBleManager.getTurnOnAlarmCharacteristic()
        myBleManager.getTurnOffAlarmCharacteristic()
        myBleManager.getAgingOffsetCharacteristic()
        myBleManager.getRtcTemperatureCharacteristic()
        myBleManager.getVersionCharacteristic()
    }

    // Enum для состояния подключения
    sealed class ConnectionState {
        object Disconnecting : ConnectionState()
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        object Ready : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    // Firmware Version
    private var _firmwareVersion: String = "Unknown"
    private val _tetrisClockFirmwareVersion = MutableStateFlow(_firmwareVersion)
    val tetrisClockFirmwareVersion: StateFlow<String> = _tetrisClockFirmwareVersion
    private val firmwareVersionReadCharacteristicHandler = VersionReadCharacteristicHandler(
        _tetrisClockFirmwareVersion)

    // Time
    private var _bleDeviceTime: TetrisClockTime = TetrisClockTime()
    private var _tetrisClockBleDeviceTime = MutableStateFlow(_bleDeviceTime)
    val tetrisClockBleDeviceTime: StateFlow<TetrisClockTime> = _tetrisClockBleDeviceTime
    private val timeReadCharacteristicHandler = TimeReadCharacteristicHandler(
        _tetrisClockBleDeviceTime)

    // ON/OFF
    private var _onOffState: Boolean = true
    private val _tetrisClockIsOn = MutableStateFlow(_onOffState)
    val tetrisClockIsOn: StateFlow<Boolean> = _tetrisClockIsOn
    private val onOffReadCharacteristicHandler = OnOffReadCharacteristicHandler(
        _tetrisClockIsOn)

    // Manual Brightness
    private var _manualBrightnessState: Byte = 0
    private val _tetrisClockManualBrightness = MutableStateFlow(_manualBrightnessState)
    val tetrisClockManualBrightness : StateFlow<Byte> = _tetrisClockManualBrightness
    private val manualBrightnessReadCharacteristicHandler = ManualBrightnessReadCharacteristicHandler(
        _tetrisClockManualBrightness)

    // Is Automatic Brightness Mode
    private var _isAutoBrightness: Boolean = false
    private val _tetrisClockIsAutoBrightness = MutableStateFlow(_isAutoBrightness)
    val tetrisClockIsAutoBrightness: StateFlow<Boolean> = _tetrisClockIsAutoBrightness
    private val autoBrightnessReadCharacteristicHandler = AutoBrightnessReadCharacteristicHandler(
        _tetrisClockIsAutoBrightness)

    // Turn ON Alarm
    private var _turnOnAlarm: TetrisClockAlarm = TetrisClockAlarm()
    private val _tetrisClockTurnOnAlarm = MutableStateFlow(_turnOnAlarm)
    val tetrisClockTurnOnAlarm: StateFlow<TetrisClockAlarm> = _tetrisClockTurnOnAlarm
    private val turnOnAlarmReadCharacteristicHandler = TurnOnAlarmReadCharacteristicHandler(
        _tetrisClockTurnOnAlarm)

    // Turn OFF Alarm
    private var _turnOffAlarm: TetrisClockAlarm = TetrisClockAlarm()
    private val _tetrisClockTurnOffAlarm = MutableStateFlow(_turnOffAlarm)
    val tetrisClockTurnOffAlarm: StateFlow<TetrisClockAlarm> = _tetrisClockTurnOffAlarm
    private val turnOffAlarmReadCharacteristicHandler = TurnOffAlarmReadCharacteristicHandler(
        _tetrisClockTurnOffAlarm)

    // RTC Aging Offset
    private var _agingOffsetState: Int = 0
    private val _tetrisClockAgingOffset  = MutableStateFlow(_agingOffsetState)
    val tetrisClockAgingOffset : StateFlow<Int> = _tetrisClockAgingOffset
    private val agingOffsetReadCharacteristicHandler = AgingOffsetReadCharacteristicHandler(
        _tetrisClockAgingOffset)

    // RTC Temperature
    private var _rtcTemperatureState: Float = Float.NaN
    private val _tetrisClockRtcTemperature  = MutableStateFlow(_rtcTemperatureState)
    val tetrisClockRtcTemperature : StateFlow<Float> = _tetrisClockRtcTemperature
    private val rtcTemperatureReadCharacteristicHandler = RtcTemperatureReadCharacteristicHandler(
        _tetrisClockRtcTemperature)

    fun setTimeCharacteristic(time: LocalDateTime) {
        _bleDeviceTime = TetrisClockTime(time)
        _tetrisClockBleDeviceTime.value = _bleDeviceTime

        if (myBleManager.isReady)
        {
            myBleManager.setTimeCharacteristic(_bleDeviceTime)
        }
    }

    fun toggleOnOffCharacteristic() {
        val on = !_tetrisClockIsOn.value

        _onOffState = on
        _tetrisClockIsOn.value = on

        if (myBleManager.isReady) {
            myBleManager.setOnOffCharacteristic(on)
        }
    }

    fun setManualBrightnessCharacteristic(brightness: Byte) {
        _manualBrightnessState = brightness
        _tetrisClockManualBrightness.value = brightness

        if (myBleManager.isReady)
        {
            myBleManager.setManualBrightnessCharacteristic(brightness)
        }
    }

    fun setAgingOffsetCharacteristic(agingOffset: Int) {
        _agingOffsetState = agingOffset
        _tetrisClockAgingOffset.value = agingOffset

        if (myBleManager.isReady) {
            myBleManager.setAgingOffsetCharacteristic(agingOffset)
        }
    }

    fun toggleAutoBrightnessCharacteristic() {
        val isAuto = !_tetrisClockIsAutoBrightness.value

        _isAutoBrightness = isAuto
        _tetrisClockIsAutoBrightness.value = isAuto

        if (myBleManager.isReady) {
            myBleManager.setAutoBrightnessCharacteristic(isAuto)
        }
    }

    fun setTurnOnAlarmCharacteristic(isActive: Boolean, hours: Byte, minutes: Byte) {
        _turnOnAlarm = TetrisClockAlarm(isActive, hours, minutes)
        _tetrisClockTurnOnAlarm.value = _turnOnAlarm

        if (myBleManager.isReady)
        {
            myBleManager.setTurnOnAlarmCharacteristic(_turnOnAlarm)
        }
    }

    fun setTurnOffAlarmCharacteristic(isActive: Boolean, hours: Byte, minutes: Byte) {
        _turnOffAlarm = TetrisClockAlarm(isActive, hours, minutes)
        _tetrisClockTurnOffAlarm.value = _turnOffAlarm

        if (myBleManager.isReady)
        {
            myBleManager.setTurnOffAlarmCharacteristic(_turnOffAlarm)
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
        val mcNow = TetrisClockTime.now()
        myBleManager.setTimeCharacteristic(mcNow)
        _bleDeviceTime = mcNow
        _tetrisClockBleDeviceTime.value = mcNow
    }

    fun toggleAlarmActive(alarmType: TetrisClockAlarmType) {
        when (alarmType) {
            TetrisClockAlarmType.TURN_ON -> {
                val current = _tetrisClockTurnOnAlarm.value
                val newAlarm = current.copy(isActive = !current.isActive)
                _tetrisClockTurnOnAlarm.value = newAlarm
                myBleManager.setTurnOffAlarmCharacteristic(newAlarm)
            }
            TetrisClockAlarmType.TURN_OFF -> {
                val current = _tetrisClockTurnOffAlarm.value
                val newAlarm = current.copy(isActive = !current.isActive)
                _tetrisClockTurnOffAlarm.value = newAlarm
                myBleManager.setTurnOffAlarmCharacteristic(newAlarm)
            }
        }
    }
}