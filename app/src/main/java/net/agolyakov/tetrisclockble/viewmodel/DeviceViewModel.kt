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
import net.agolyakov.tetrisclockble.service.BluetoothService
import no.nordicsemi.android.ble.observer.ConnectionObserver
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val devicePreferences: DevicePreferences,
    private val bluetoothService: BluetoothService,
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
        val deviceFromNav = savedStateHandle.get<BleDevice>("device")
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

    fun connectToDevice(device: BleDevice?)
    {
        device?.let {
            bluetoothService.connect(it)
        }
    }
}
