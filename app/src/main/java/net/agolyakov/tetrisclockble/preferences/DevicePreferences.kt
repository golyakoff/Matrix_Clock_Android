package net.agolyakov.tetrisclockble.preferences

import android.content.Context
import net.agolyakov.tetrisclockble.ble.BleDevice
import androidx.core.content.edit

class DevicePreferences(
    private val context: Context
) {
    private val prefs = context.getSharedPreferences(
        "device_friendly_names",
        Context.MODE_PRIVATE
    )

    fun saveFriendlyName(macAddress: String, friendlyName: String?) {
        if (friendlyName.isNullOrBlank()) {
            prefs.edit { remove(macAddress) }
        } else {
            prefs.edit { putString(macAddress, friendlyName) }
        }
    }

    fun getFriendlyName(macAddress: String): String? {
        return prefs.getString(macAddress, null)
    }

    fun loadFriendlyNameToDevice(device: BleDevice): BleDevice {
        val savedName = getFriendlyName(device.deviceMacAddress)
        return device.copy(friendlyName = savedName)
    }

    fun deleteFriendlyName(macAddress: String) {
        prefs.edit { remove(macAddress) }
    }

    fun getAllFriendlyNames(): Map<String, String> {
        return prefs.all.mapNotNull { (key, value) ->
            if (value is String) key to value else null
        }.toMap()
    }
}