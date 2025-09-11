package net.agolyakov.tetrisclockble.service.bluetooth.handlers

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.MutableStateFlow
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockAlarm
import no.nordicsemi.android.ble.data.Data

class TurnOnAlarmReadCharacteristicHandler (
    private var turnOnAlarm: MutableStateFlow<TetrisClockAlarm>
): ReadCharacteristicHandler {

    override fun onReadCharacteristicCallback(device: BluetoothDevice, data: Data) {
        turnOnAlarm.value = TetrisClockAlarm.Companion.fromByteArray(data.value!!)
    }
}