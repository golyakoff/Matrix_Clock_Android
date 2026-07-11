package net.agolyakov.tetrisclockble.service.bluetooth.handlers

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.MutableStateFlow
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockHourlyBrightness
import no.nordicsemi.android.ble.data.Data

class HourlyBrightnessReadCharacteristicHandler(
    private var hourlyBrightness: MutableStateFlow<TetrisClockHourlyBrightness>
): ReadCharacteristicHandler {
    override fun onReadCharacteristicCallback(device: BluetoothDevice, data: Data) {
        hourlyBrightness.value = TetrisClockHourlyBrightness.fromByteArray(data.value!!)
    }
}
