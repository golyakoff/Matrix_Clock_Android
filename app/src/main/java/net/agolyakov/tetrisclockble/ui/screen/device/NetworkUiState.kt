package net.agolyakov.tetrisclockble.ui.screen.device

import androidx.annotation.StringRes
import net.agolyakov.tetrisclockble.protocol.UpdateInfo
import net.agolyakov.tetrisclockble.protocol.WifiScan
import net.agolyakov.tetrisclockble.protocol.WifiSettings
import net.agolyakov.tetrisclockble.protocol.WifiSettingsUpdate
import net.agolyakov.tetrisclockble.protocol.WifiStatus

/** The firmware update check done by the phone (GitHub releases), shown on the Settings tab. */
sealed interface FirmwareCheck {
    data object Idle : FirmwareCheck
    data object Checking : FirmwareCheck
    data object UpToDate : FirmwareCheck
    data class Available(val version: String) : FirmwareCheck
    data object Failed : FirmwareCheck
}

/** An installation the clock does by itself over WiFi, followed from the phone. */
sealed interface WifiUpdate {
    /** The clock looks for the update (reads the release manifest). */
    data object Checking : WifiUpdate
    data class Downloading(val received: Long, val total: Long) : WifiUpdate
    /** Written: the clock restarts, the phone waits to read the new version. */
    data object Rebooting : WifiUpdate
    data class Done(val version: String) : WifiUpdate
    /** The new firmware did not start and the bootloader went back to [version]. */
    data class RolledBack(val version: String) : WifiUpdate
    data class Failed(@StringRes val message: Int) : WifiUpdate

    val finished: Boolean get() = this is Done || this is RolledBack || this is Failed
}

/** What the network cards of the Settings tab show (all null / false for older firmware). */
data class NetworkUiState(
    val supported: Boolean = false,
    val timeZone: String? = null,
    val wifiSettings: WifiSettings? = null,
    val wifiStatus: WifiStatus? = null,
    val wifiScan: WifiScan? = null,
    val wifiSaveFailed: Boolean = false,
    /** A WiFi settings write is on its way (the phone may be pairing with the clock first). */
    val wifiSaving: Boolean = false,
    val updateInfo: UpdateInfo? = null,
    val firmwareCheck: FirmwareCheck = FirmwareCheck.Idle,
    val wifiUpdate: WifiUpdate? = null,
    /** The name the clock advertises; null for firmware without the device name characteristic. */
    val deviceName: String? = null,
    val deviceNameSaveFailed: Boolean = false,
    /** Whether the clock is on the beta update channel; null for firmware without channels. */
    val betaUpdates: Boolean? = null,
) {
    /** The clock can download an update itself: WiFi is on and has a network. */
    val wifiConfigured: Boolean
        get() = wifiSettings?.let { it.mode != net.agolyakov.tetrisclockble.protocol.WifiMode.OFF && it.ssid.isNotEmpty() } == true
}

interface NetworkActions {
    fun saveWifiSettings(update: WifiSettingsUpdate)
    fun saveDeviceName(name: String)
    fun setBetaUpdates(beta: Boolean)
    fun scanWifi()
    fun syncTimeFromInternet()
    fun checkFirmware()
    fun installFirmwareByWifi()
    fun dismissWifiUpdate()
}
