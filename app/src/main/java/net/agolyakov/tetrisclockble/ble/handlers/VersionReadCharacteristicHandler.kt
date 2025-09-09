package net.agolyakov.tetrisclockble.ble.handlers

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.MutableStateFlow
import no.nordicsemi.android.ble.data.Data

class VersionReadCharacteristicHandler (
    private var version: MutableStateFlow<String>

): ReadCharacteristicHandler {
    override fun onReadCharacteristicCallback(device: BluetoothDevice, data: Data) {
        version.value = data.value!!.toString(Charsets.US_ASCII)
    }
}