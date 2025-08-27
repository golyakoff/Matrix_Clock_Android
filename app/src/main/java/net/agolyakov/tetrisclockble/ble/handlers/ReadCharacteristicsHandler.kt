package net.agolyakov.tetrisclockble.ble.handlers

import android.bluetooth.BluetoothDevice
import no.nordicsemi.android.ble.data.Data

interface ReadCharacteristicHandler {
    fun onReadCharacteristicCallback(device: BluetoothDevice, data: Data) { }
}
