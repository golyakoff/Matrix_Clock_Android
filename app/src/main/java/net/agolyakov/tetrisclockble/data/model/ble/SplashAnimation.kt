package net.agolyakov.tetrisclockble.data.model.ble

import androidx.annotation.StringRes
import net.agolyakov.tetrisclockble.R

/**
 * Cadence of the Animation Splash: how often the animation replaces the clock over the :00 minute.
 * The [value] is what the firmware stores/expects in the low 2 bits of the characteristic.
 */
enum class SplashMode(val value: Int) {
    OFF(0),
    DAILY(1),      // once per day, at 00:00
    EVERY_3H(2),   // every 3 hours (00:00, 03:00, ... 21:00)
    HOURLY(3);     // every hour, at HH:00

    companion object {
        /** The user-selectable "on" cadences shown in the frequency dropdown, in display order. */
        val selectable: List<SplashMode> = listOf(DAILY, EVERY_3H, HOURLY)

        /** The cadence used when the feature is switched on from off. */
        val DEFAULT_ON: SplashMode = DAILY

        fun fromValue(value: Int): SplashMode = entries.firstOrNull { it.value == value } ?: OFF
    }
}

/**
 * How long an Animation Splash run lasts before the clock returns. The [value] is what the firmware
 * stores/expects in bits 2..3 of the characteristic.
 */
enum class SplashDuration(val value: Int, val seconds: Int) {
    S10(0, 10),
    S20(1, 20),
    S40(2, 40),
    S60(3, 60);

    companion object {
        /** All durations, in display order for the dropdown. */
        val all: List<SplashDuration> = listOf(S10, S20, S40, S60)

        fun fromValue(value: Int): SplashDuration = all.firstOrNull { it.value == value } ?: S10
    }
}

/**
 * An animation the device can play, identified by the index the firmware stores.
 *
 * @param index the animation index sent to the device (0..7).
 * @param nameRes string resource for the human-readable label shown in the picker (localized).
 */
data class SplashAnimation(
    val index: Int,
    @StringRes val nameRes: Int
)

/**
 * Catalog of animations available on the device. The order and indices MUST stay in sync with the
 * firmware's `include/animations.h` (ANIMATIONS[] table): index N here must be the same animation as
 * ANIMATIONS[N] there. Add a new entry whenever a new animation ships in the firmware.
 */
object SplashAnimations {
    val all: List<SplashAnimation> = listOf(
        SplashAnimation(0, R.string.mc_anim_nyan_cat),
        SplashAnimation(1, R.string.mc_anim_russian_flag)
    )
}
