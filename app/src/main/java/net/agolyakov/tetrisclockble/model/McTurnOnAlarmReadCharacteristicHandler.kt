package net.agolyakov.tetrisclockble.model

import android.bluetooth.BluetoothDevice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import no.nordicsemi.android.ble.data.Data

class McTurnOnAlarmReadCharacteristicHandler (
    private var turnOnAlarm: MutableStateFlow<McTurnOnOffAlarm>): ReadCharacteristicHandler {

    override fun onReadCharacteristicCallback(device: BluetoothDevice, data: Data) {
        val hexValues = data.value!!.map { String.format("%02X", it) }.toList()
        Log.d(
            "McTurnOnAlarmReadCharacteristicHandler",
            "onReadCharacteristicCallback(), data =" +
                    "[ ${hexValues.joinToString(separator = ", ")}]")

        turnOnAlarm.value = McTurnOnOffAlarm.fromByteArray(data.value!!)
    }
}