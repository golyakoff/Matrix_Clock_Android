package net.agolyakov.tetrisclockble.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class BleDevice(
    var mfrName: String,
    var macAddress: String,
    var overrideName: String? = null,
): Parcelable
