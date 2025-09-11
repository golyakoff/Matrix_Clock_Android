package net.agolyakov.tetrisclockble.ui.screen.device

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
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockAlarmType
import net.agolyakov.tetrisclockble.ble.BleConnectionState
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockDevice
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockAlarm
import net.agolyakov.tetrisclockble.ui.component.TimePickerDialogState
import net.agolyakov.tetrisclockble.data.local.TetrisClockPreferences
import net.agolyakov.tetrisclockble.service.bluetooth.BluetoothService
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val preferences: TetrisClockPreferences,
    private val bluetoothService: BluetoothService,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    // Device and its FriendlyName
    private val _device = MutableStateFlow<TetrisClockDevice?>(null)
    val device: StateFlow<TetrisClockDevice?> = _device.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _editName = MutableStateFlow("")
    val editName: StateFlow<String> = _editName.asStateFlow()

    fun setDevice(device: TetrisClockDevice) {
        val deviceWithFriendlyName = preferences.loadFriendlyNameToDevice(device)
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
        preferences.saveFriendlyName(device.macAddress, _editName.value)
        _device.value = device.copy(friendlyName = _editName.value)
        _isEditing.value = false
    }

    fun cancelEditing() {
        val device = _device.value ?: return
        _editName.value = device.friendlyName ?: device.deviceName
        _isEditing.value = false
    }

    var tetrisClockBleDeviceTime = bluetoothService.tetrisClockBleDeviceTime
    val tetrisClockTetrisOn = bluetoothService.tetrisClockIsOn
    val tetrisClockManualBrightness = bluetoothService.tetrisClockManualBrightness
    val tetrisClockIsAutoBrightness = bluetoothService.tetrisClockIsAutoBrightness
    val tetrisClockTurnOnAlarm = bluetoothService.tetrisClockTurnOnAlarm
    var tetrisClockTurnOffAlarm = bluetoothService.tetrisClockTurnOffAlarm
    val tetrisClockAgingOffset = bluetoothService.tetrisClockAgingOffset
    val tetrisClockRtcTemperature  =  bluetoothService.tetrisClockRtcTemperature


    private val _manualBrightnessState = MutableStateFlow<Byte>(0)
    private val _debouncedBrightness = MutableSharedFlow<Byte>(extraBufferCapacity = 1)


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

    init {
        val deviceFromNav = savedStateHandle.get<TetrisClockDevice>("device")
        deviceFromNav?.let {
            setDevice(it)
            bluetoothService.connect(it)
            setupDebouncedBrightness()
        }
    }

    private fun setupDebouncedBrightness() {
        viewModelScope.launch {
            _debouncedBrightness
                .debounce(300) // 300ms debounce
                .collect { value ->
                    bluetoothService.setManualBrightnessCharacteristic(value)
                    _manualBrightnessState.value = value
                }
        }
    }

    fun connectToDevice(device: TetrisClockDevice?)
    {
        device?.let {
            bluetoothService.connect(it)
        }
    }
}
