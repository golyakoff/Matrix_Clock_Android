package net.agolyakov.tetrisclockble.service.bluetooth.handlers

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.MutableStateFlow
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockAnimationSplash
import no.nordicsemi.android.ble.data.Data

/**
 * Parses the single Animation Splash byte: bits0..1 = cadence mode (0..3), bits2..3 = duration
 * (0..3), bits4..6 = animation index (0..7). (bit7, the "play now" preview command, is write-only
 * and never reported on a read.)
 */
class AnimationSplashReadCharacteristicHandler(
    private var animationSplash: MutableStateFlow<TetrisClockAnimationSplash>
): ReadCharacteristicHandler {
    override fun onReadCharacteristicCallback(device: BluetoothDevice, data: Data) {
        val raw = data.value?.getOrNull(0)?.toInt()?.and(0xFF) ?: return
        val mode = raw and 0x03
        val duration = (raw shr 2) and 0x03
        val index = (raw shr 4) and 0x07
        animationSplash.value = TetrisClockAnimationSplash(mode, duration, index)
    }
}
