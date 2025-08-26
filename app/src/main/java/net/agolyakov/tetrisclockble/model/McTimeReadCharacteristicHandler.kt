package net.agolyakov.tetrisclockble.model

import android.bluetooth.BluetoothDevice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import no.nordicsemi.android.ble.data.Data

class McTimeReadCharacteristicHandler (
    private var time: MutableStateFlow<McTime>): ReadCharacteristicHandler {
    override fun onReadCharacteristicCallback(device: BluetoothDevice, data: Data) {
        val hexValues = data.value!!.map { String.format("%02X", it) }.toList()
        Log.d(
            "McOnOffReadCharacteristicHandler",
            "onReadCharacteristicCallback(), data =" +
                    "[ ${hexValues.joinToString(separator = ", ")}]")

        time.value = McTime.fromByteArray(data.value!!)
    }
}

