package net.agolyakov.tetrisclockble.viewmodel

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.agolyakov.tetrisclockble.model.BleConnectionState
import net.agolyakov.tetrisclockble.model.BleDevice
import net.agolyakov.tetrisclockble.model.McOnOffReadCharacteristicHandler
import net.agolyakov.tetrisclockble.model.McOnOffState
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

    //private val _services = MutableStateFlow<List<BluetoothGattService>>(emptyList())
    //val services = _services.asStateFlow()

    private var _onOffState = McOnOffState(true)

    private var _matrixClockIsOn = MutableStateFlow(_onOffState.isOn)
    val MatricClockisOn: StateFlow<Boolean> = _matrixClockIsOn

    var onOffReadCharacteristicHandler = McOnOffReadCharacteristicHandler(
        _onOffState,
        _matrixClockIsOn)

    private val myBleManager: MyBleManager = MyBleManager(
        bluetoothAdapterProvider.getContext(),
        onOffReadCharacteristicHandler)

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

    fun getOnOffCharacteristic() {
        if (myBleManager.isReady) {
            myBleManager.getOnOffCharacteristic()
        }
    }

    fun setOnOffCharacteristic() {
        val on = !_matrixClockIsOn.value

        _onOffState = McOnOffState(on)
        _matrixClockIsOn.value = on

        if (myBleManager.isReady) {
            myBleManager.setOnOffCharacteristic(on)
        }
    }

    private val connectionObserver = object : ConnectionObserver {
        override fun onDeviceConnecting(device: BluetoothDevice) {}

        override fun onDeviceConnected(device: BluetoothDevice) {}

        override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {}

        override fun onDeviceReady(device: BluetoothDevice) {
            myBleManager.getOnOffCharacteristic()
        }

        override fun onDeviceDisconnecting(device: BluetoothDevice) {}

        override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {}
    }
}
