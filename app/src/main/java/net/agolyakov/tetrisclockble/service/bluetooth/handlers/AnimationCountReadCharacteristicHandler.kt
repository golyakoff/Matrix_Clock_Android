package net.agolyakov.tetrisclockble.service.bluetooth.handlers

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.MutableStateFlow
import no.nordicsemi.android.ble.data.Data

/**
 * Reads how many Animation Splash animations the connected clock ships (MC_ANIMATION_COUNT_CHAR_UUID):
 * a single byte. The app shows exactly this many animations from its bundled catalog. Older firmware
 * without this characteristic never triggers this handler, so the flow keeps its 0 default and the
 * app falls back to showing the whole bundled catalog.
 */
class AnimationCountReadCharacteristicHandler(
    private var animationCount: MutableStateFlow<Int>
): ReadCharacteristicHandler {
    override fun onReadCharacteristicCallback(device: BluetoothDevice, data: Data) {
        val bytes = data.value ?: return
        if (bytes.isEmpty()) return
        animationCount.value = bytes[0].toInt() and 0xFF
    }
}
