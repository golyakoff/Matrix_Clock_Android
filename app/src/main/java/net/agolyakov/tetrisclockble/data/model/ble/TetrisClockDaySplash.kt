package net.agolyakov.tetrisclockble.data.model.ble

/**
 * Day Splash configuration: an animated screensaver the clock plays over the 00:00 minute at the
 * day boundary instead of the usual Tetris layout (firmware feature, see MC_DAY_SPLASH_CHAR_UUID).
 *
 * @param enabled whether the day-boundary animation is enabled.
 * @param animationIndex the selected animation index (0..7); currently only 0 (Nyan Cat) exists.
 */
data class TetrisClockDaySplash(
    val enabled: Boolean = false,
    val animationIndex: Int = 0
)
