package net.agolyakov.tetrisclockble.service.bluetooth.handlers

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.MutableStateFlow
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockTime
import no.nordicsemi.android.ble.data.Data

class TimeReadCharacteristicHandler (
    private var time: MutableStateFlow<TetrisClockTime>
): ReadCharacteristicHandler {
    override fun onReadCharacteristicCallback(device: BluetoothDevice, data: Data) {
        time.value = TetrisClockTime.Companion.fromByteArray(data.value!!)
    }
}