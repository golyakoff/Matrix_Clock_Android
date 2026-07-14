package net.agolyakov.tetrisclockble.service.bluetooth.handlers

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.MutableStateFlow
import no.nordicsemi.android.ble.data.Data

class PixelColorOrderReadCharacteristicHandler (
    private var isRrbbggColorOrder: MutableStateFlow<Boolean>
): ReadCharacteristicHandler {
    override fun onReadCharacteristicCallback(device: BluetoothDevice, data: Data) {
        isRrbbggColorOrder.value = data.value!![0] > 0
    }
}
