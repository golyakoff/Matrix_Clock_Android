package net.agolyakov.tetrisclockble.data.model.ble

/**
 * A Day Splash animation the device can play, identified by the index the firmware stores.
 *
 * @param index the animation index sent to the device (0..7).
 * @param name human-readable label shown in the picker.
 */
data class DaySplashAnimation(
    val index: Int,
    val name: String
)

/**
 * Catalog of animations available on the device. The order and indices MUST stay in sync with the
 * firmware's `include/animations.h` (ANIMATIONS[] table): index N here must be the same animation as
 * ANIMATIONS[N] there. Add a new entry whenever a new animation ships in the firmware.
 */
object DaySplashAnimations {
    val all: List<DaySplashAnimation> = listOf(
        DaySplashAnimation(0, "Nyan Cat")
    )

    fun nameForIndex(index: Int): String =
        all.firstOrNull { it.index == index }?.name ?: all.firstOrNull()?.name.orEmpty()
}
