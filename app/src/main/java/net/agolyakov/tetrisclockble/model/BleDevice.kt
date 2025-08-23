package net.agolyakov.tetrisclockble.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BleDevice(
    val deviceName: String,                     // Имя устройства, заданное производителем
    val deviceMacAddr: String,                  // MAC-адрес
    val overrideName: String? = null,           // Опционально заданное пользователем имя
) : Parcelable
