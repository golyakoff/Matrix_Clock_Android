package net.agolyakov.tetrisclockble.data.model.ble

import androidx.annotation.RawRes
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
 * @param index the animation index sent to the device (0..15).
 * @param nameRes string resource for the human-readable label shown in the picker (localized).
 * @param gifRes raw resource of the animation's GIF, shown as a live preview tile in the picker.
 */
data class SplashAnimation(
    val index: Int,
    @StringRes val nameRes: Int,
    @RawRes val gifRes: Int
)

/**
 * Catalog of animations available on the device. The order and indices MUST stay in sync with the
 * firmware's `include/animations.h` (ANIMATIONS[] table): index N here must be the same animation as
 * ANIMATIONS[N] there. Add a new entry whenever a new animation ships in the firmware.
 */
object SplashAnimations {
    val all: List<SplashAnimation> = listOf(
        SplashAnimation(0, R.string.mc_anim_russian_flag, R.raw.splash_russian_flag),
        SplashAnimation(1, R.string.mc_anim_nyan_cat, R.raw.splash_nyan_cat),
        SplashAnimation(2, R.string.mc_anim_pacman, R.raw.splash_pacman),
        SplashAnimation(3, R.string.mc_anim_rick_and_morty, R.raw.splash_rick_and_morty),
        SplashAnimation(4, R.string.mc_anim_minions, R.raw.splash_minions),
        SplashAnimation(5, R.string.mc_anim_bmo, R.raw.splash_bmo),
        SplashAnimation(6, R.string.mc_anim_finn, R.raw.splash_finn),
        SplashAnimation(7, R.string.mc_anim_mochi_cat, R.raw.splash_mochi_cat),
        SplashAnimation(8, R.string.mc_anim_color_bars, R.raw.splash_color_bars),
        SplashAnimation(9, R.string.mc_anim_audio_tape, R.raw.splash_audio_tape)
    )

    /**
     * How many animations a clock has when its firmware is too old to report the count
     * (MC_ANIMATION_COUNT_CHAR_UUID arrived in firmware v1.7.0): the four that shipped back then.
     */
    const val LEGACY_COUNT: Int = 4
}
