package net.agolyakov.tetrisclockble.ble

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BleDevice(
    val deviceName: String,                     // Имя устройства, заданное производителем
    val deviceMacAddress: String,               // MAC-адрес
    val friendlyName: String? = null,           // Опционально заданное пользователем имя
) : Parcelable {
    fun getDisplayName(): String {
        return friendlyName ?: deviceName
    }
}
