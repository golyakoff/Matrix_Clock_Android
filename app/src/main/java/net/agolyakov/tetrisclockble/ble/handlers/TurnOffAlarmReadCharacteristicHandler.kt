package net.agolyakov.tetrisclockble.ble.handlers

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.MutableStateFlow
import net.agolyakov.tetrisclockble.ble.TetrisClockAlarm
import no.nordicsemi.android.ble.data.Data

class TurnOffAlarmReadCharacteristicHandler (
    private var turnOffAlarm: MutableStateFlow<TetrisClockAlarm>
): ReadCharacteristicHandler {

    override fun onReadCharacteristicCallback(device: BluetoothDevice, data: Data) {
        turnOffAlarm.value = TetrisClockAlarm.Companion.fromByteArray(data.value!!)
    }
}