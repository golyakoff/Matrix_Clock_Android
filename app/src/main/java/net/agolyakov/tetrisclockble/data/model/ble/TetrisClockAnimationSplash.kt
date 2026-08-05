package net.agolyakov.tetrisclockble.data.model.ble

/**
 * Animation Splash configuration: an animated screensaver the clock plays over the :00 minute at the
 * start of the hour instead of the usual Tetris layout (firmware feature, see
 * MC_ANIMATION_SPLASH_CHAR_UUID).
 *
 * @param mode how often the animation plays (see [SplashMode]); 0 = off.
 * @param durationValue how long each run lasts (see [SplashDuration]).
 * @param animationIndex the selected animation index (0..7).
 */
data class TetrisClockAnimationSplash(
    val mode: Int = SplashMode.OFF.value,
    val durationValue: Int = SplashDuration.S10.value,
    val animationIndex: Int = 0
) {
    val enabled: Boolean get() = mode != SplashMode.OFF.value
}
