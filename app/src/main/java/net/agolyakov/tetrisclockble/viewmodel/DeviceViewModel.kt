package net.agolyakov.tetrisclockble.viewmodel

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import net.agolyakov.tetrisclockble.ble.TetrisClockAlarmType
import net.agolyakov.tetrisclockble.ble.BleConnectionState
import net.agolyakov.tetrisclockble.ble.BleDevice
import net.agolyakov.tetrisclockble.ble.handlers.AutoBrightnessReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.ManualBrightnessReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.OnOffReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.TetrisClockTime
import net.agolyakov.tetrisclockble.ble.handlers.TimeReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.TurnOffAlarmReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.TurnOnAlarmReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.TetrisClockAlarm
import net.agolyakov.tetrisclockble.ble.MyBleManager
import net.agolyakov.tetrisclockble.ble.TimePickerDialogState
import net.agolyakov.tetrisclockble.ble.handlers.AgingOffsetReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.RtcTemperatureReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.VersionReadCharacteristicHandler
import net.agolyakov.tetrisclockble.preferences.DevicePreferences
import net.agolyakov.tetrisclockble.service.BluetoothAdapterProvider
import no.nordicsemi.android.ble.observer.ConnectionObserver
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val bluetoothAdapterProvider: BluetoothAdapterProvider,
    private val devicePreferences: DevicePreferences,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    // Device and its FriendlyName
    private val _device = MutableStateFlow<BleDevice?>(null)
    val device: StateFlow<BleDevice?> = _device.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _editName = MutableStateFlow("")
    val editName: StateFlow<String> = _editName.asStateFlow()

    fun setDevice(device: BleDevice) {
        val deviceWithFriendlyName = devicePreferences.loadFriendlyNameToDevice(device)
        _device.value = deviceWithFriendlyName
        _editName.value = deviceWithFriendlyName.friendlyName ?: deviceWithFriendlyName.deviceName
    }

    fun startEditing() {
        _isEditing.value = true
    }

    fun updateEditName(name: String) {
        _editName.value = name
    }

    fun saveFriendlyName() {
        val device = _device.value ?: return
        devicePreferences.saveFriendlyName(device.deviceMacAddress, _editName.value)
        _device.value = device.copy(friendlyName = _editName.value)
        _isEditing.value = false
    }

    fun cancelEditing() {
        val device = _device.value ?: return
        _editName.value = device.friendlyName ?: device.deviceName
        _isEditing.value = false
    }

    // Time
    private var _bleDeviceTime: TetrisClockTime = TetrisClockTime()
    private var _tetrisClockBleDeviceTime = MutableStateFlow(_bleDeviceTime)
    var tetrisClockBleDeviceTime: StateFlow<TetrisClockTime> = _tetrisClockBleDeviceTime
    var timeReadCharacteristicHandler = TimeReadCharacteristicHandler(_tetrisClockBleDeviceTime)

    // ON/OFF
    private var _onOffState: Boolean = true
    private var _tetrisClockIsOn = MutableStateFlow(_onOffState)
    val tetrisClockTetrisOn: StateFlow<Boolean> = _tetrisClockIsOn
    var onOffReadCharacteristicHandler = OnOffReadCharacteristicHandler(
        _tetrisClockIsOn)


    // Manual Brightness
    private var _manualBrightnessState: Byte = 0
    private var _tetrisClockManualBrightness = MutableStateFlow(_manualBrightnessState)
    private val _debouncedBrightness = MutableSharedFlow<Byte>(extraBufferCapacity = 1)
    val tetrisClockManualBrightness : StateFlow<Byte> = _tetrisClockManualBrightness
    var manualBrightnessReadCharacteristicHandler = ManualBrightnessReadCharacteristicHandler(
        _tetrisClockManualBrightness)

    // Is Automatic Brightness Mode
    private var _isAutoBrightness: Boolean = false
    private var _tetrisClockIsAutoBrightness = MutableStateFlow(_isAutoBrightness)
    val tetrisClockIsAutoBrightness: StateFlow<Boolean> = _tetrisClockIsAutoBrightness
    var autoBrightnessReadCharacteristicHandler = AutoBrightnessReadCharacteristicHandler(
        _tetrisClockIsAutoBrightness)

    // Turn ON Alarm
    private var _turnOnAlarm: TetrisClockAlarm = TetrisClockAlarm()
    private var _tetrisClockTurnOnAlarm = MutableStateFlow(_turnOnAlarm)
    var tetrisClockTurnOnAlarm: StateFlow<TetrisClockAlarm> = _tetrisClockTurnOnAlarm
    var turnOnAlarmReadCharacteristicHandler = TurnOnAlarmReadCharacteristicHandler(_tetrisClockTurnOnAlarm)

    // Turn OFF Alarm
    private var _turnOffAlarm: TetrisClockAlarm = TetrisClockAlarm()
    private var _tetrisClockTurnOffAlarm = MutableStateFlow(_turnOffAlarm)
    var tetrisClockTurnOffAlarm: StateFlow<TetrisClockAlarm> = _tetrisClockTurnOffAlarm
    var turnOffAlarmReadCharacteristicHandler =
        TurnOffAlarmReadCharacteristicHandler(_tetrisClockTurnOffAlarm)

    // Alarm Dialog
    private val _timePickerState = MutableStateFlow(TimePickerDialogState())
    val timePickerState: StateFlow<TimePickerDialogState> = _timePickerState

    fun showTimePickerDialog(alarmType: TetrisClockAlarmType, alarm: TetrisClockAlarm) {
        _timePickerState.value = TimePickerDialogState(
            isVisible = true,
            alarmType = alarmType,
            hour = alarm.hours.toInt(),
            minute = alarm.minutes.toInt(),
            isActive = alarm.isActive
        )
    }

    fun hideTimePickerDialog() {
        _timePickerState.value = TimePickerDialogState(isVisible = false)
    }

    fun setAlarmTime(hour: Int, minute: Int, isActive: Boolean) {
        val currentState = _timePickerState.value
        when (currentState.alarmType) {
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
        hideTimePickerDialog()
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

    // RTC Aging Offset
    private var _agingOffsetState: Int = 0
    private var _tetrisClockAgingOffset  = MutableStateFlow(_agingOffsetState)
    val tetrisClockAgingOffset : StateFlow<Int> = _tetrisClockAgingOffset
    var agingOffsetReadCharacteristicHandler = AgingOffsetReadCharacteristicHandler(
        _tetrisClockAgingOffset)


    // RTC Temperature
    private var _rtcTemperatureState: Float = Float.NaN
    private var _tetrisClockRtcTemperature  = MutableStateFlow(_rtcTemperatureState)
    val tetrisClockRtcTemperature : StateFlow<Float> = _tetrisClockRtcTemperature
    var rtcTemperatureReadCharacteristicHandler = RtcTemperatureReadCharacteristicHandler(
        _tetrisClockRtcTemperature)

    init {
        val deviceFromNav = savedStateHandle.get<BleDevice>("device")
        deviceFromNav?.let { setDevice(it) }

        viewModelScope.launch {
            _debouncedBrightness
                .debounce(timeoutMillis = 300) // ждём 300 мс после последнего изменения
                .collect { value ->
                    if (myBleManager.isReady) {
                        myBleManager.setManualBrightnessCharacteristic(_manualBrightnessState)
                    }
                }
        }
    }

    // Firmware Version
    private var _firmwareVersion: String = "Unknown"
    private var _tetrisClockFirmwareVersion = MutableStateFlow(_firmwareVersion)
    var tetrisClockFirmwareVersion: StateFlow<String> = _tetrisClockFirmwareVersion
    var firmwareVersionReadCharacteristicHandler = VersionReadCharacteristicHandler(_tetrisClockFirmwareVersion)

    private val myBleManager: MyBleManager = MyBleManager(
        context = bluetoothAdapterProvider.getContext(),
        timeReadCharacteristicHandler,
        onOffReadCharacteristicHandler,
        manualBrightnessReadCharacteristicHandler,
        autoBrightnessReadCharacteristicHandler,
        turnOnAlarmReadCharacteristicHandler,
        turnOffAlarmReadCharacteristicHandler,
        agingOffsetReadCharacteristicHandler,
        rtcTemperatureReadCharacteristicHandler,
        firmwareVersionReadCharacteristicHandler)

    fun connectToDevice(device: BleDevice?)
    {
        device?.let {
            connect(it)
        }
    }

    private fun connect(myDevice: BleDevice) {
        val device = bluetoothAdapterProvider.getAdapter().getRemoteDevice(myDevice.deviceMacAddress)
        myBleManager.connect(device)
            .retry(2, 100)
            .useAutoConnect(false)
            .done {
                Log.i("ControlViewModel", "connection success!")
            }
            .fail { _, status ->
                Log.e("ControlViewModel", "connection failed, $status")
            }
            .enqueue()
        myBleManager.connectionObserver = connectionObserver
    }

    fun disconnect() {
        myBleManager.disconnect().enqueue()
    }

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
        _debouncedBrightness.tryEmit(brightness)
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

    fun syncBleWithPhone() {
        val mcNow = TetrisClockTime.now()
        myBleManager.setTimeCharacteristic(mcNow)
        _bleDeviceTime = mcNow
        _tetrisClockBleDeviceTime.value = mcNow
    }

    private val connectionObserver = object : ConnectionObserver {
        override fun onDeviceConnecting(device: BluetoothDevice) {}

        override fun onDeviceConnected(device: BluetoothDevice) {}

        override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {}

        override fun onDeviceReady(device: BluetoothDevice) {
            myBleManager.getTimeCharacteristic()
            myBleManager.getOnOffCharacteristic()
            myBleManager.getManualBrightnessCharacteristic()
            myBleManager.getAutoBrightnessCharacteristic()
            myBleManager.getTurnOnAlarmCharacteristic()
            myBleManager.getTurnOffAlarmCharacteristic()
            myBleManager.getAgingOffsetCharacteristic()
            myBleManager.getRtcTemperatureCharacteristic()
        }

        override fun onDeviceDisconnecting(device: BluetoothDevice) {}

        override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {}
    }
}
