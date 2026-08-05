package net.agolyakov.tetrisclockble.ui.screen.device

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import net.agolyakov.tetrisclockble.data.model.ble.SplashAnimations
import net.agolyakov.tetrisclockble.data.model.ble.SplashMode
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockAlarmType
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockDevice
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockAlarm
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockHourlyBrightness
import net.agolyakov.tetrisclockble.ui.component.TimePickerDialogState
import net.agolyakov.tetrisclockble.data.local.TetrisClockPreferences
import net.agolyakov.tetrisclockble.service.bluetooth.BluetoothService
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val preferences: TetrisClockPreferences,
    val bluetoothService: BluetoothService,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    // Device and its FriendlyName
    private val _device = MutableStateFlow<TetrisClockDevice?>(null)
    val device: StateFlow<TetrisClockDevice?> = _device.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _editName = MutableStateFlow("")
    val editName: StateFlow<String> = _editName.asStateFlow()

    fun setDevice(device: TetrisClockDevice) {
//        val deviceWithFriendlyName = preferences.loadFriendlyNameToDevice(device)
//        _device.value = deviceWithFriendlyName
//        _editName.value = deviceWithFriendlyName.friendlyName ?: deviceWithFriendlyName.deviceName
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

    val tetrisClockFirmwareVersion = bluetoothService.tetrisClockFirmwareVersion
    var tetrisClockBleDeviceTime = bluetoothService.tetrisClockBleDeviceTime
    val tetrisClockTetrisOn = bluetoothService.tetrisClockIsOn
    val tetrisClockManualBrightness = bluetoothService.tetrisClockManualBrightness
    val tetrisClockIsAutoBrightness = bluetoothService.tetrisClockIsAutoBrightness
    val tetrisClockIsRrbbggColorOrder = bluetoothService.tetrisClockIsRrbbggColorOrder
    val tetrisClockAnimationSplash = bluetoothService.tetrisClockAnimationSplash
    val splashAnimations = SplashAnimations.all
    val tetrisClockHourlyBrightness = bluetoothService.tetrisClockHourlyBrightness
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


    // Debounce Brightness
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

    // Common

    val connectionState = bluetoothService.connectionState
    val currentDevice = bluetoothService.currentDevice

    init {
        val deviceFromNav = savedStateHandle.get<TetrisClockDevice>("device")
        deviceFromNav?.let {
            setDevice(it)
            bluetoothService.connect(it)
            setupDebouncedBrightness()
        }
    }

    fun connectToDevice(device: TetrisClockDevice?)
    {
        device?.let {
            bluetoothService.connect(it)
            bluetoothService.setShouldMaintainConnection(true)
        }
    }

    fun setAlarmTime(alarmType: TetrisClockAlarmType, hour: Int, minute: Int, isActive: Boolean) {
        Log.d("DeviceViewModel", "setAlarmTime: active=$isActive $hour:$minute for ${alarmType}")
        bluetoothService.setAlarmTime(
            alarmType,
            hour,
            minute,
            isActive)
    }

    fun toggleOnOffCharacteristic() {
        bluetoothService.toggleOnOffCharacteristic()
    }

    fun setManualBrightnessCharacteristic(brightness: Byte) {
        bluetoothService.setManualBrightnessCharacteristic(brightness)
    }

    fun syncBleWithPhone() {
        bluetoothService.syncBleWithPhone()
    }

    fun setAgingOffsetCharacteristic(agingOffset: Int) {
        bluetoothService.setAgingOffsetCharacteristic(agingOffset)
    }

    fun toggleAutoBrightnessCharacteristic() {
        bluetoothService.toggleAutoBrightnessCharacteristic()
    }

    fun setColorOrderCharacteristic(useRrbbgg: Boolean) {
        bluetoothService.setColorOrderCharacteristic(useRrbbgg)
    }

    fun setSplashEnabled(enabled: Boolean) {
        val current = tetrisClockAnimationSplash.value
        val mode = when {
            !enabled -> SplashMode.OFF.value
            current.mode == SplashMode.OFF.value -> SplashMode.DEFAULT_ON.value
            else -> current.mode
        }
        bluetoothService.setAnimationSplashCharacteristic(mode, current.durationValue, current.animationIndex)
    }

    fun setSplashMode(mode: Int) {
        val current = tetrisClockAnimationSplash.value
        bluetoothService.setAnimationSplashCharacteristic(mode, current.durationValue, current.animationIndex)
    }

    fun setSplashDuration(duration: Int) {
        val current = tetrisClockAnimationSplash.value
        bluetoothService.setAnimationSplashCharacteristic(current.mode, duration, current.animationIndex)
    }

    fun setSplashAnimation(index: Int) {
        val current = tetrisClockAnimationSplash.value
        bluetoothService.setAnimationSplashCharacteristic(current.mode, current.durationValue, index)
    }

    fun previewSplash() {
        bluetoothService.previewAnimationSplash()
    }

    fun setHourlyBrightnessCharacteristic(hourlyBrightness: TetrisClockHourlyBrightness) {
        bluetoothService.setHourlyBrightnessCharacteristic(hourlyBrightness)
    }

    fun toggleAlarmActive(alarmType: TetrisClockAlarmType) {
        Log.d("DeviceViewModel", "toggleAlarmActive for ${alarmType}")
        bluetoothService.toggleAlarmActive(alarmType)
    }
}
