package net.agolyakov.tetrisclockble.service.bluetooth.handlers

import android.bluetooth.BluetoothDevice
import no.nordicsemi.android.ble.data.Data

interface ReadCharacteristicHandler {
    fun onReadCharacteristicCallback(device: BluetoothDevice, data: Data) { }
}
