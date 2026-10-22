package net.agolyakov.tetrisclockble.protocol

/**
 * Bytes of the network characteristics (firmware v2.0.0+) <-> [NetworkModels]. The wire format is
 * documented next to the UUIDs in the firmware's `main/include/ble.h`: little-endian integers, a
 * `str8` is a length byte and that many UTF-8 bytes. Encoders range-check like the firmware does.
 */
object NetworkCodec {
    /** POSIX TZ string length limit (NET_TZ_MAX_LEN). */
    const val TIME_ZONE_MAX_BYTES = 47

    /** The password length byte: keep the stored password (write) / a password is stored (read). */
    private const val PASSWORD_KEEP = 0xFF

    // ---------- Time zone ----------

    fun encodeTimeZone(posix: String): ByteArray {
        val bytes = posix.toByteArray(Charsets.UTF_8)
        require(bytes.isNotEmpty() && bytes.size <= TIME_ZONE_MAX_BYTES) { "Bad time zone \"$posix\"" }
        return bytes
    }

    fun decodeTimeZone(bytes: ByteArray): String = ByteReader(bytes).restAsString()

    // ---------- WiFi settings ----------

    fun decodeWifiSettings(bytes: ByteArray): WifiSettings = with(ByteReader(bytes)) {
        val mode = codedOf(u8(), WifiMode.OFF)
        val period = u8()
        val ssid = str8()
        val passwordLength = u8()
        WifiSettings(mode, period, ssid, passwordStored = passwordLength != 0)
    }

    fun encodeWifiSettings(update: WifiSettingsUpdate): ByteArray {
        require(update.periodHours in WifiSettings.PERIOD_RANGE) { "Period out of range: ${update.periodHours}" }
        require(update.mode == WifiMode.OFF || update.ssid.isNotEmpty()) { "SSID is required" }
        return bytesOf {
            u8(update.mode.code)
            u8(update.periodHours)
            str8(update.ssid, WifiSettings.MAX_SSID_BYTES)
            if (update.password == null) u8(PASSWORD_KEEP)
            else str8(update.password, WifiSettings.MAX_PASSWORD_BYTES)
        }
    }

    // ---------- WiFi status ----------

    fun decodeWifiStatus(bytes: ByteArray): WifiStatus = with(ByteReader(bytes)) {
        val state = codedOf(u8(), WifiState.UNKNOWN)
        val ip = (0 until 4).map { u8() }
        val rssi = i8()
        val syncedAt = u32()
        WifiStatus(
            state,
            ip = ip.takeIf { it.any { b -> b != 0 } }?.joinToString("."),
            rssi = rssi,
            syncedAtEpochSeconds = syncedAt.takeIf { it != 0L }
        )
    }

    // ---------- WiFi networks ----------

    /** One page of the scan result (or the 4-byte header of a scan start / end notification). */
    fun decodeWifiScan(bytes: ByteArray): WifiScan = with(ByteReader(bytes)) {
        val state = codedOf(u8(), WifiScanState.UNKNOWN)
        val total = u8()
        val first = u8()
        val count = u8()
        val networks = List(count) {
            WifiNetwork(ssid = str8(), rssi = i8(), auth = codedOf(u8(), WifiAuth.UNKNOWN), channel = u8())
        }
        WifiScan(state, total, first, networks)
    }

    /** Selects the first network of the next page to read. */
    fun encodeWifiScanCursor(first: Int): ByteArray = bytesOf { u8(first) }

    // ---------- Command ----------

    fun encodeCommand(command: ClockCommand): ByteArray = bytesOf { u8(command.code) }

    // ---------- Update info / OTA status ----------

    fun decodeUpdateInfo(bytes: ByteArray): UpdateInfo = with(ByteReader(bytes)) {
        UpdateInfo(state = codedOf(u8(), UpdateCheckState.UNKNOWN), latestVersion = str8(), lastCheckEpochSeconds = u32())
    }

    fun decodeOtaStatus(bytes: ByteArray): OtaStatus = with(ByteReader(bytes)) {
        OtaStatus(
            state = codedOf(u8(), OtaState.UNKNOWN),
            error = codedOf(u8(), OtaError.UNKNOWN),
            received = u32(),
            total = u32()
        )
    }
}
