package net.agolyakov.tetrisclockble.ble.handlers

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.MutableStateFlow
import net.agolyakov.tetrisclockble.ble.TetrisClockAlarm
import no.nordicsemi.android.ble.data.Data

class TurnOnAlarmReadCharacteristicHandler (
    private var turnOnAlarm: MutableStateFlow<TetrisClockAlarm>
): ReadCharacteristicHandler {

    override fun onReadCharacteristicCallback(device: BluetoothDevice, data: Data) {
        turnOnAlarm.value = TetrisClockAlarm.Companion.fromByteArray(data.value!!)
    }
}