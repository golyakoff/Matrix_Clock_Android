package net.agolyakov.tetrisclockble.data.model.ble

import java.io.Serializable

// Serializable rather than Parcelable: Parcelize needs a compiler plugin, and the app builds with
// AGP's built-in Kotlin (see app/build.gradle.kts). Passed between screens via SavedStateHandle.
data class TetrisClockDevice(
    val deviceName: String,             // Имя устройства, заданное производителем
    val macAddress: String,             // MAC-адрес
    val friendlyName: String? = null,   // Опционально заданное пользователем имя
) : Serializable {
    fun getDisplayName(): String {
        return friendlyName ?: deviceName
    }
}
