package net.agolyakov.tetrisclockble.model

import android.bluetooth.BluetoothDevice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import no.nordicsemi.android.ble.data.Data

class McOnOffReadCharacteristicHandler (
    private var isOn: MutableStateFlow<Boolean>): ReadCharacteristicHandler {
    override fun onReadCharacteristicCallback(device: BluetoothDevice, data: Data) {
        val isOnByte: Byte = data.value!![0]
        Log.d(
            "McOnOffReadCharacteristicHandler",
            "onReadCharacteristicCallback(), data = ${isOnByte}"
        )
        isOn.value = isOnByte > 0
    }
}
