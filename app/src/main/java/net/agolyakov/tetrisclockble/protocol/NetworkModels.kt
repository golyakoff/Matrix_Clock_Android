package net.agolyakov.tetrisclockble.protocol

/**
 * Plain models of the network characteristics of firmware v2.0.0+ (time zone, WiFi, update from the
 * internet), see `main/include/ble.h` of Matrix_Clock_ESP32. No Android dependencies:
 * [NetworkCodec] converts them to and from bytes. Ported from FireLamp_Android's protocol models.
 */

/** An enum with a wire code. */
interface Coded {
    val code: Int
}

/** The entry with [code], or [fallback] for a code from a newer firmware. */
inline fun <reified E> codedOf(code: Int, fallback: E): E where E : Enum<E>, E : Coded =
    enumValues<E>().firstOrNull { it.code == code } ?: fallback

// ---------- WiFi settings / status ----------

enum class WifiMode(override val code: Int) : Coded {
    OFF(0),
    /** Connect every N hours for time sync and update check, then disconnect. */
    PERIODIC(1)
}

data class WifiSettings(
    val mode: WifiMode,
    val periodHours: Int,
    val ssid: String,
    /** Read: whether a password is stored (it is never returned). */
    val passwordStored: Boolean,
) {
    companion object {
        const val MAX_SSID_BYTES = 32
        const val MAX_PASSWORD_BYTES = 64
        val PERIOD_RANGE = 1..24
    }
}

data class WifiSettingsUpdate(
    val mode: WifiMode,
    val periodHours: Int,
    val ssid: String,
    /** Null keeps the stored password. */
    val password: String?,
)

enum class WifiState(override val code: Int) : Coded {
    /** Also between the sessions of the periodic mode. */
    OFF(0),
    CONNECTING(1),
    CONNECTED(2),
    /** Wrong password or network not found; kept until the next attempt. */
    ERROR(3),
    UNKNOWN(-1)
}

data class WifiStatus(
    val state: WifiState,
    /** Dotted IPv4, null if not connected. */
    val ip: String?,
    /** dBm, 0 if not connected. */
    val rssi: Int,
    /** The last time sync from the internet, UTC seconds, null if none since the clock started. */
    val syncedAtEpochSeconds: Long?,
)

// ---------- WiFi networks (scan) ----------

enum class WifiScanState(override val code: Int) : Coded {
    /** No scan since boot. */
    NONE(0),
    SCANNING(1),
    DONE(2),
    ERROR(3),
    UNKNOWN(-1)
}

enum class WifiAuth(override val code: Int) : Coded {
    OPEN(0),
    WEP(1),
    /** WPA / WPA2 / WPA3 personal: a password. */
    PERSONAL(2),
    /** WPA2/WPA3 enterprise: the clock cannot join it. */
    ENTERPRISE(3),
    UNKNOWN(-1)
}

data class WifiNetwork(
    val ssid: String,
    val rssi: Int,
    val auth: WifiAuth,
    val channel: Int,
) {
    val usable: Boolean get() = auth == WifiAuth.OPEN || auth == WifiAuth.WEP || auth == WifiAuth.PERSONAL
    val needsPassword: Boolean get() = auth != WifiAuth.OPEN
}

/**
 * Networks the clock hears (2.4 GHz only), strongest first, one entry per SSID: one page of them as
 * read, or all of them once BluetoothService has read every page.
 */
data class WifiScan(
    val state: WifiScanState,
    /** All networks found (up to 64). */
    val total: Int,
    /** Index of the first network of this page. */
    val first: Int,
    val networks: List<WifiNetwork>,
)

// ---------- Command ----------

enum class ClockCommand(override val code: Int) : Coded {
    /** Connect and sync time from the internet now. */
    SYNC_TIME(0x01),
    /** Check for a firmware update now; the result comes in [UpdateInfo]. */
    CHECK_UPDATE(0x02),
    /** Install the firmware update from the internet; the progress comes in [OtaStatus]. */
    INSTALL_UPDATE(0x03),
    /** Scan for WiFi networks; the result comes in [WifiScan]. */
    SCAN_WIFI(0x04)
}

// ---------- Update info / OTA status (update from the internet) ----------

enum class UpdateCheckState(override val code: Int) : Coded {
    NOT_CHECKED(0),
    CHECKING(1),
    UP_TO_DATE(2),
    AVAILABLE(3),
    FAILED(4),
    UNKNOWN(-1)
}

data class UpdateInfo(
    val state: UpdateCheckState,
    /** Empty if never checked. */
    val latestVersion: String,
    /** UTC seconds of the last successful check, 0 if unknown. */
    val lastCheckEpochSeconds: Long,
)

enum class OtaState(override val code: Int) : Coded {
    IDLE(0),
    /** Downloading and writing. */
    RECEIVING(1),
    /** The clock reboots into the new firmware. */
    SUCCESS(2),
    ERROR(3),
    CHECKING(4),
    NO_UPDATE(5),
    UNKNOWN(-1)
}

enum class OtaError(override val code: Int) : Coded {
    NONE(0),
    BUSY(1),
    FLASH_WRITE(3),
    SHA256_MISMATCH(5),
    /** No WiFi, or the manifest / file could not be downloaded. */
    DOWNLOAD_FAILED(8),
    UNKNOWN(-1)
}

data class OtaStatus(
    val state: OtaState,
    val error: OtaError,
    val received: Long,
    val total: Long,
)
