package net.agolyakov.tetrisclockble.model

import android.bluetooth.BluetoothDevice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import no.nordicsemi.android.ble.data.Data

class McManualBrightnessReadCharacteristicHandler (
    private var manualBrightness: MutableStateFlow<Byte>): ReadCharacteristicHandler {
    override fun onReadCharacteristicCallback(device: BluetoothDevice, data: Data) {
        val brightnessByte: Byte = data.value!![0]
        Log.d(
            "McManualBrightnessCharacteristicHandler",
            "onReadCharacteristicCallback(), data = ${brightnessByte}"
        )
        manualBrightness.value = brightnessByte
    }
}
