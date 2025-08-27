package net.agolyakov.tetrisclockble.ble.handlers

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.MutableStateFlow
import no.nordicsemi.android.ble.data.Data

class ManualBrightnessReadCharacteristicHandler (
    private var manualBrightness: MutableStateFlow<Byte>
): ReadCharacteristicHandler {
    override fun onReadCharacteristicCallback(device: BluetoothDevice, data: Data) {
       manualBrightness.value = data.value!![0]
    }
}