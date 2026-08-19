package net.agolyakov.tetrisclockble.service.bluetooth.handlers

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.MutableStateFlow
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockAnimationSplash
import no.nordicsemi.android.ble.data.Data

/**
 * Parses the Animation Splash value. Byte 0: bits0..1 = cadence mode (0..3), bits2..3 = duration
 * (0..3), bits4..6 = the low 3 bits of the animation index. (bit7, the "play now" preview command,
 * is write-only and never reported on a read.) Byte 1 bit0 = the index's high bit, added in
 * firmware v1.8.0; firmware older than that sends one byte only, which means index 0..7.
 */
class AnimationSplashReadCharacteristicHandler(
    private var animationSplash: MutableStateFlow<TetrisClockAnimationSplash>
): ReadCharacteristicHandler {
    override fun onReadCharacteristicCallback(device: BluetoothDevice, data: Data) {
        val raw = data.value?.getOrNull(0)?.toInt()?.and(0xFF) ?: return
        val rawHi = data.value?.getOrNull(1)?.toInt()?.and(0xFF) ?: 0
        val mode = raw and 0x03
        val duration = (raw shr 2) and 0x03
        val index = ((raw shr 4) and 0x07) or ((rawHi and 0x01) shl 3)
        animationSplash.value = TetrisClockAnimationSplash(mode, duration, index)
    }
}
