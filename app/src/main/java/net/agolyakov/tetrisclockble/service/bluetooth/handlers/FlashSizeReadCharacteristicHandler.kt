package net.agolyakov.tetrisclockble.service.bluetooth.handlers

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.MutableStateFlow
import no.nordicsemi.android.ble.data.Data

/**
 * Reads the clock's physical flash chip size (MC_FLASH_SIZE_CHAR_UUID): a single byte holding the
 * size in whole megabytes (e.g. 4 or 16). Older firmware without this characteristic never triggers
 * this handler, so the flow keeps its 0 ("unknown") default and the app falls back to the 4MB build.
 */
class FlashSizeReadCharacteristicHandler(
    private var flashSizeMb: MutableStateFlow<Int>
): ReadCharacteristicHandler {
    override fun onReadCharacteristicCallback(device: BluetoothDevice, data: Data) {
        val bytes = data.value ?: return
        if (bytes.isEmpty()) return
        flashSizeMb.value = bytes[0].toInt() and 0xFF
    }
}
