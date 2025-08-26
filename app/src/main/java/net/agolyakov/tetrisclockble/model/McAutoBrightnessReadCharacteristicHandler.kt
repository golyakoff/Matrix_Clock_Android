package net.agolyakov.tetrisclockble.model

import android.bluetooth.BluetoothDevice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import no.nordicsemi.android.ble.data.Data

class McAutoBrightnessReadCharacteristicHandler (
    private var isAutoBrightness: MutableStateFlow<Boolean>): ReadCharacteristicHandler {
    override fun onReadCharacteristicCallback(device: BluetoothDevice, data: Data) {
        val isOnByte: Byte = data.value!![0]
        Log.d(
            "McAutoBrightnessReadCharacteristicHandler",
            "onReadCharacteristicCallback(), data = ${isOnByte}"
        )
        isAutoBrightness.value = isOnByte > 0
    }
}
