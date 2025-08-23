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
import net.agolyakov.tetrisclockble.model.McManualBrightnessReadCharacteristicHandler
import net.agolyakov.tetrisclockble.model.McOnOffReadCharacteristicHandler
import net.agolyakov.tetrisclockble.model.MyBleManager
import net.agolyakov.tetrisclockble.service.BluetoothAdapterProvider
import no.nordicsemi.android.ble.observer.ConnectionObserver
import javax.inject.Inject


@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val bluetoothAdapterProvider: BluetoothAdapterProvider,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    // ON/OFF
    private var _onOffState: Boolean = true
    private var _matrixClockIsOn = MutableStateFlow(_onOffState)
    val MatrixClockIsOn: StateFlow<Boolean> = _matrixClockIsOn
    var onOffReadCharacteristicHandler = McOnOffReadCharacteristicHandler(
        _matrixClockIsOn)

    // Manual Brightness
    private var _manualBrightnessState: Byte = 0
    private var _matrixClockManualBrightness = MutableStateFlow(_manualBrightnessState)
    private val _debouncedBrightness = MutableSharedFlow<Byte>(extraBufferCapacity = 1)
    val MatrixClockManualBrightness : StateFlow<Byte> = _matrixClockManualBrightness
    var manualBrightnessReadCharacteristicHandler = McManualBrightnessReadCharacteristicHandler(
        _matrixClockManualBrightness)

    init {
        viewModelScope.launch {
            _debouncedBrightness
                .debounce(300) // ждём 300 мс после последнего изменения
                .collect { value ->
                    if (myBleManager.isReady) {
                        myBleManager.setManualBrightnessCharacteristic(_manualBrightnessState)
                    }
                }
        }
    }

    private val myBleManager: MyBleManager = MyBleManager(
        bluetoothAdapterProvider.getContext(),
        onOffReadCharacteristicHandler,
        manualBrightnessReadCharacteristicHandler)

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

    private val connectionObserver = object : ConnectionObserver {
        override fun onDeviceConnecting(device: BluetoothDevice) {}

        override fun onDeviceConnected(device: BluetoothDevice) {}

        override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {}

        override fun onDeviceReady(device: BluetoothDevice) {
            myBleManager.getOnOffCharacteristic()
            myBleManager.getManualBrightnessCharacteristic()
        }

        override fun onDeviceDisconnecting(device: BluetoothDevice) {}

        override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {}
    }
}
