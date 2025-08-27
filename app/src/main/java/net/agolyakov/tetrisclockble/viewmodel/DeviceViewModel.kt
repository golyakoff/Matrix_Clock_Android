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
    private var _matrixClockBleDeviceTime = MutableStateFlow(_bleDeviceTime)
    var matrixClockBleDeviceTime: StateFlow<TetrisClockTime> = _matrixClockBleDeviceTime
    var timeReadCharacteristicHandler = TimeReadCharacteristicHandler(_matrixClockBleDeviceTime)

    // ON/OFF
    private var _onOffState: Boolean = true
    private var _matrixClockIsOn = MutableStateFlow(_onOffState)
    val matrixClockIsOn: StateFlow<Boolean> = _matrixClockIsOn
    var onOffReadCharacteristicHandler = OnOffReadCharacteristicHandler(
        _matrixClockIsOn)


    // Manual Brightness
    private var _manualBrightnessState: Byte = 0
    private var _matrixClockManualBrightness = MutableStateFlow(_manualBrightnessState)
    private val _debouncedBrightness = MutableSharedFlow<Byte>(extraBufferCapacity = 1)
    val matrixClockManualBrightness : StateFlow<Byte> = _matrixClockManualBrightness
    var manualBrightnessReadCharacteristicHandler = ManualBrightnessReadCharacteristicHandler(
        _matrixClockManualBrightness)

    // Is Automatic Brightness Mode
    private var _isAutoBrightness: Boolean = false
    private var _matrixClockIsAutoBrightness = MutableStateFlow(_isAutoBrightness)
    val matrixClockIsAutoBrightness: StateFlow<Boolean> = _matrixClockIsAutoBrightness
    var autoBrightnessReadCharacteristicHandler = AutoBrightnessReadCharacteristicHandler(
        _matrixClockIsAutoBrightness)

    // Turn ON Alarm
    private var _turnOnAlarm: TetrisClockAlarm = TetrisClockAlarm()
    private var _matrixClockTurnOnAlarm = MutableStateFlow(_turnOnAlarm)
    var matrixClockTurnOnAlarm: StateFlow<TetrisClockAlarm> = _matrixClockTurnOnAlarm
    var turnOnAlarmReadCharacteristicHandler = TurnOnAlarmReadCharacteristicHandler(_matrixClockTurnOnAlarm)

    // Turn OFF Alarm
    private var _turnOffAlarm: TetrisClockAlarm = TetrisClockAlarm()
    private var _matrixClockTurnOffAlarm = MutableStateFlow(_turnOffAlarm)
    var matrixClockTurnOffAlarm: StateFlow<TetrisClockAlarm> = _matrixClockTurnOffAlarm
    var turnOffAlarmReadCharacteristicHandler =
        TurnOffAlarmReadCharacteristicHandler(_matrixClockTurnOffAlarm)

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
                val current = _matrixClockTurnOnAlarm.value
                val newAlarm = current.copy(isActive = !current.isActive)
                _matrixClockTurnOnAlarm.value = newAlarm
                // bleRepository.writeTurnOnAlarm(newAlarm.toByteArray())
            }
            TetrisClockAlarmType.TURN_OFF -> {
                val current = _matrixClockTurnOffAlarm.value
                val newAlarm = current.copy(isActive = !current.isActive)
                _matrixClockTurnOffAlarm.value = newAlarm
                // bleRepository.writeTurnOffAlarm(newAlarm.toByteArray())
            }
        }
    }

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

    private val myBleManager: MyBleManager = MyBleManager(
        context = bluetoothAdapterProvider.getContext(),
        timeReadCharacteristicHandler,
        onOffReadCharacteristicHandler,
        manualBrightnessReadCharacteristicHandler,
        autoBrightnessReadCharacteristicHandler,
        turnOnAlarmReadCharacteristicHandler,
        turnOffAlarmReadCharacteristicHandler)

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
        _matrixClockBleDeviceTime.value = _bleDeviceTime

        if (myBleManager.isReady)
        {
            myBleManager.setTimeCharacteristic(_bleDeviceTime)
        }
    }

    fun toggleOnOffCharacteristic() {
        val on = !_matrixClockIsOn.value

        _onOffState = on
        _matrixClockIsOn.value = on

        if (myBleManager.isReady) {
            myBleManager.setOnOffCharacteristic(on)
        }
    }

    fun setManualBrightnessCharacteristic(brightness: Byte) {
        _manualBrightnessState = brightness
        _matrixClockManualBrightness.value = brightness
        _debouncedBrightness.tryEmit(brightness)
    }

    fun toggleAutoBrightnessCharacteristic() {
        val isAuto = !_matrixClockIsAutoBrightness.value

        _isAutoBrightness = isAuto
        _matrixClockIsAutoBrightness.value = isAuto

        if (myBleManager.isReady) {
            myBleManager.setAutoBrightnessCharacteristic(isAuto)
        }
    }

    fun setTurnOnAlarmCharacteristic(isActive: Boolean, hours: Byte, minutes: Byte) {
        _turnOnAlarm = TetrisClockAlarm(isActive, hours, minutes)
        _matrixClockTurnOnAlarm.value = _turnOnAlarm

        if (myBleManager.isReady)
        {
            myBleManager.setTurnOnAlarmCharacteristic(_turnOnAlarm)
        }
    }

    fun setTurnOffAlarmCharacteristic(isActive: Boolean, hours: Byte, minutes: Byte) {
        _turnOffAlarm = TetrisClockAlarm(isActive, hours, minutes)
        _matrixClockTurnOffAlarm.value = _turnOffAlarm

        if (myBleManager.isReady)
        {
            myBleManager.setTurnOffAlarmCharacteristic(_turnOffAlarm)
        }
    }

    fun syncBleWithPhone() {
        val mcNow = TetrisClockTime.now()
        myBleManager.setTimeCharacteristic(mcNow)
        _bleDeviceTime = mcNow
        _matrixClockBleDeviceTime.value = mcNow
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
        }

        override fun onDeviceDisconnecting(device: BluetoothDevice) {}

        override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {}
    }
}
