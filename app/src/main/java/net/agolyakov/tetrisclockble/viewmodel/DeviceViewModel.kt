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
import net.agolyakov.tetrisclockble.model.BleConnectionState
import net.agolyakov.tetrisclockble.model.BleDevice
import net.agolyakov.tetrisclockble.model.McAutoBrightnessReadCharacteristicHandler
import net.agolyakov.tetrisclockble.model.McManualBrightnessReadCharacteristicHandler
import net.agolyakov.tetrisclockble.model.McOnOffReadCharacteristicHandler
import net.agolyakov.tetrisclockble.model.McTime
import net.agolyakov.tetrisclockble.model.McTimeReadCharacteristicHandler
import net.agolyakov.tetrisclockble.model.McTurnOffAlarmReadCharacteristicHandler
import net.agolyakov.tetrisclockble.model.McTurnOnAlarmReadCharacteristicHandler
import net.agolyakov.tetrisclockble.model.McTurnOnOffAlarm
import net.agolyakov.tetrisclockble.model.MyBleManager
import net.agolyakov.tetrisclockble.service.BluetoothAdapterProvider
import no.nordicsemi.android.ble.observer.ConnectionObserver
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val bluetoothAdapterProvider: BluetoothAdapterProvider,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    // Time
    private var _bleDeviceTime: McTime = McTime()
    private var _matrixClockBleDeviceTime = MutableStateFlow(_bleDeviceTime)
    var matrixClockBleDeviceTime: StateFlow<McTime> = _matrixClockBleDeviceTime
    var timeReadCharacteristicHandler = McTimeReadCharacteristicHandler(_matrixClockBleDeviceTime)

    // ON/OFF
    private var _onOffState: Boolean = true
    private var _matrixClockIsOn = MutableStateFlow(_onOffState)
    val matrixClockIsOn: StateFlow<Boolean> = _matrixClockIsOn
    var onOffReadCharacteristicHandler = McOnOffReadCharacteristicHandler(
        _matrixClockIsOn)


    // Manual Brightness
    private var _manualBrightnessState: Byte = 0
    private var _matrixClockManualBrightness = MutableStateFlow(_manualBrightnessState)
    private val _debouncedBrightness = MutableSharedFlow<Byte>(extraBufferCapacity = 1)
    val matrixClockManualBrightness : StateFlow<Byte> = _matrixClockManualBrightness
    var manualBrightnessReadCharacteristicHandler = McManualBrightnessReadCharacteristicHandler(
        _matrixClockManualBrightness)

    // Is Automatic Brightness Mode
    private var _isAutoBrightness: Boolean = false
    private var _matrixClockIsAutoBrightness = MutableStateFlow(_isAutoBrightness)
    val matrixClockIsAutoBrightness: StateFlow<Boolean> = _matrixClockIsAutoBrightness
    var autoBrightnessReadCharacteristicHandler = McAutoBrightnessReadCharacteristicHandler(
        _matrixClockIsAutoBrightness)

    // Turn ON Alarm
    private var _turnOnAlarm: McTurnOnOffAlarm = McTurnOnOffAlarm()
    private var _matrixClockTurnOnAlarm = MutableStateFlow(_turnOnAlarm)
    var matrixClockTurnOnAlarm: StateFlow<McTurnOnOffAlarm> = _matrixClockTurnOnAlarm
    var turnOnAlarmReadCharacteristicHandler = McTurnOnAlarmReadCharacteristicHandler(_matrixClockTurnOnAlarm)

    // Turn OFF Alarm
    private var _turnOffAlarm: McTurnOnOffAlarm = McTurnOnOffAlarm()
    private var _matrixClockTurnOffAlarm = MutableStateFlow(_turnOffAlarm)
    var matrixClockTurnOffAlarm: StateFlow<McTurnOnOffAlarm> = _matrixClockTurnOffAlarm
    var turnOffAlarmReadCharacteristicHandler =
        McTurnOffAlarmReadCharacteristicHandler(_matrixClockTurnOffAlarm)

    init {
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
        val device = bluetoothAdapterProvider.getAdapter().getRemoteDevice(myDevice.deviceMacAddr)
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
        _bleDeviceTime = McTime(time)
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
        _turnOnAlarm = McTurnOnOffAlarm(isActive, hours, minutes)
        _matrixClockTurnOnAlarm.value = _turnOnAlarm

        if (myBleManager.isReady)
        {
            myBleManager.setTurnOnAlarmCharacteristic(_turnOnAlarm)
        }
    }

    fun setTurnOffAlarmCharacteristic(isActive: Boolean, hours: Byte, minutes: Byte) {
        _turnOffAlarm = McTurnOnOffAlarm(isActive, hours, minutes)
        _matrixClockTurnOffAlarm.value = _turnOffAlarm

        if (myBleManager.isReady)
        {
            myBleManager.setTurnOffAlarmCharacteristic(_turnOffAlarm)
        }
    }

    private val connectionObserver = object : ConnectionObserver {
        override fun onDeviceConnecting(device: BluetoothDevice) {}

        override fun onDeviceConnected(device: BluetoothDevice) {}

        override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {}

        override fun onDeviceReady(device: BluetoothDevice) {
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
