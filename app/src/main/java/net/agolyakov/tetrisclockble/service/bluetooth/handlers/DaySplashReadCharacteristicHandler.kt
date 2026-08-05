package net.agolyakov.tetrisclockble.service.bluetooth.handlers

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.MutableStateFlow
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockDaySplash
import no.nordicsemi.android.ble.data.Data

/**
 * Parses the single Day Splash byte: bit0 = enabled, bits4..6 = animation index (0..7).
 * (bit3, the "play now" preview command, is write-only and never reported back on a read.)
 */
class DaySplashReadCharacteristicHandler(
    private var daySplash: MutableStateFlow<TetrisClockDaySplash>
): ReadCharacteristicHandler {
    override fun onReadCharacteristicCallback(device: BluetoothDevice, data: Data) {
        val raw = data.value?.getOrNull(0)?.toInt()?.and(0xFF) ?: return
        val enabled = (raw and 0x01) != 0
        val index = (raw shr 4) and 0x07
        daySplash.value = TetrisClockDaySplash(enabled, index)
    }
}
