package net.agolyakov.tetrisclockble.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Byte layouts as the firmware builds and parses them (Matrix_Clock_ESP32 `main/ble.cpp`, the
 * network characteristics section) - keep both in sync.
 */
class NetworkCodecTest {

    private fun hex(s: String): ByteArray =
        s.split(' ').filter { it.isNotEmpty() }.map { it.toInt(16).toByte() }.toByteArray()

    private fun str8(s: String): String {
        val bytes = s.toByteArray(Charsets.UTF_8)
        return (listOf(bytes.size) + bytes.map { it.toInt() and 0xFF }).joinToString(" ") { "%02x".format(it) }
    }

    @Test
    fun deviceName() {
        assertArrayEquals(hex("4b 69 74 63 68 65 6e"), NetworkCodec.encodeDeviceName("Kitchen"))
        assertArrayEquals(ByteArray(0), NetworkCodec.encodeDeviceName(""))
        assertEquals("Часы кухня", NetworkCodec.decodeDeviceName("Часы кухня".toByteArray()))
        assertTrue(NetworkCodec.isValidDeviceName("N".repeat(20)))
        assertFalse(NetworkCodec.isValidDeviceName("N".repeat(21)))
        // 11 Cyrillic letters are 22 bytes
        assertFalse(NetworkCodec.isValidDeviceName("Ч".repeat(11)))
        assertFalse(NetworkCodec.isValidDeviceName("Tab\there"))
        assertFalse(NetworkCodec.isValidDeviceName("Del\u007f"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun deviceNameTooLong() {
        NetworkCodec.encodeDeviceName("N".repeat(21))
    }

    @Test
    fun timeZone() {
        assertArrayEquals("MSK-3".toByteArray(), NetworkCodec.encodeTimeZone("MSK-3"))
        assertEquals("CET-1CEST,M3.5.0,M10.5.0/3", NetworkCodec.decodeTimeZone("CET-1CEST,M3.5.0,M10.5.0/3".toByteArray()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun timeZoneTooLong() {
        NetworkCodec.encodeTimeZone("X".repeat(48))
    }

    @Test
    fun wifiSettingsWrite() {
        assertArrayEquals(
            hex("01 03 " + str8("home") + " " + str8("secret12")),
            NetworkCodec.encodeWifiSettings(WifiSettingsUpdate(WifiMode.PERIODIC, 3, "home", "secret12"))
        )
        // null password: keep the stored one
        assertArrayEquals(
            hex("01 01 " + str8("home") + " ff"),
            NetworkCodec.encodeWifiSettings(WifiSettingsUpdate(WifiMode.PERIODIC, 1, "home", null))
        )
        // open network: an empty password
        assertArrayEquals(
            hex("00 01 00 00"),
            NetworkCodec.encodeWifiSettings(WifiSettingsUpdate(WifiMode.OFF, 1, "", ""))
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun wifiSettingsNeedSsid() {
        NetworkCodec.encodeWifiSettings(WifiSettingsUpdate(WifiMode.PERIODIC, 1, "", null))
    }

    @Test(expected = IllegalArgumentException::class)
    fun wifiSettingsPeriodRange() {
        NetworkCodec.encodeWifiSettings(WifiSettingsUpdate(WifiMode.PERIODIC, 25, "home", null))
    }

    @Test
    fun wifiSettingsRead() {
        assertEquals(
            WifiSettings(WifiMode.PERIODIC, 12, "home", passwordStored = true),
            NetworkCodec.decodeWifiSettings(hex("01 0c " + str8("home") + " ff"))
        )
        assertFalse(NetworkCodec.decodeWifiSettings(hex("00 01 00 00")).passwordStored)
        // a mode from a newer firmware reads as off
        assertEquals(WifiMode.OFF, NetworkCodec.decodeWifiSettings(hex("07 01 00 00")).mode)
    }

    @Test
    fun wifiStatus() {
        // connected, 192.168.1.42, -67 dBm, synced at 0x6A3C2F00
        assertEquals(
            WifiStatus(WifiState.CONNECTED, "192.168.1.42", -67, 0x6A3C2F00L),
            NetworkCodec.decodeWifiStatus(hex("02 c0 a8 01 2a bd 00 2f 3c 6a"))
        )
        val off = NetworkCodec.decodeWifiStatus(hex("00 00 00 00 00 00 00 00 00 00"))
        assertEquals(WifiStatus(WifiState.OFF, null, 0, null), off)
        assertNull(off.syncedAtEpochSeconds)
    }

    @Test
    fun wifiScan() {
        // Page: state 2 (done), 5 networks in total, this page starts at 2 and holds 3
        val scan = NetworkCodec.decodeWifiScan(
            hex("02 05 02 03 " + str8("HomeNet") + " c4 02 06 " + str8("Cafe") + " b5 00 0b " + str8("Office") + " a6 03 01")
        )
        assertEquals(WifiScanState.DONE, scan.state)
        assertEquals(5, scan.total)
        assertEquals(2, scan.first)
        assertEquals(
            listOf(
                WifiNetwork("HomeNet", -60, WifiAuth.PERSONAL, 6),
                WifiNetwork("Cafe", -75, WifiAuth.OPEN, 11),
                WifiNetwork("Office", -90, WifiAuth.ENTERPRISE, 1),
            ),
            scan.networks
        )
        assertFalse(scan.networks[1].needsPassword)
        assertFalse(scan.networks[2].usable)
        assertTrue(scan.networks[0].usable)
        // the header-only notification of a scan start
        assertEquals(WifiScan(WifiScanState.SCANNING, 0, 0, emptyList()), NetworkCodec.decodeWifiScan(hex("01 00 00 00")))
        assertArrayEquals(hex("05"), NetworkCodec.encodeWifiScanCursor(5))
    }

    @Test
    fun command() {
        assertArrayEquals(hex("01"), NetworkCodec.encodeCommand(ClockCommand.SYNC_TIME))
        assertArrayEquals(hex("03"), NetworkCodec.encodeCommand(ClockCommand.INSTALL_UPDATE))
        assertArrayEquals(hex("04"), NetworkCodec.encodeCommand(ClockCommand.SCAN_WIFI))
    }

    @Test
    fun updateInfo() {
        assertEquals(
            UpdateInfo(UpdateCheckState.AVAILABLE, "v2.1.0", 0x6A3C2F00L),
            NetworkCodec.decodeUpdateInfo(hex("03 " + str8("v2.1.0") + " 00 2f 3c 6a"))
        )
        assertEquals(
            UpdateInfo(UpdateCheckState.NOT_CHECKED, "", 0),
            NetworkCodec.decodeUpdateInfo(hex("00 00 00 00 00 00"))
        )
    }

    @Test
    fun otaStatus() {
        // error 5 (SHA-256 mismatch) after 0x1000 of 0x155530 bytes
        assertEquals(
            OtaStatus(OtaState.ERROR, OtaError.SHA256_MISMATCH, 0x1000, 0x155530),
            NetworkCodec.decodeOtaStatus(hex("03 05 00 10 00 00 30 55 15 00"))
        )
        assertEquals(OtaState.UNKNOWN, NetworkCodec.decodeOtaStatus(hex("09 00 00 00 00 00 00 00 00 00")).state)
    }

    @Test(expected = ProtocolException::class)
    fun shortValue() {
        NetworkCodec.decodeWifiStatus(hex("02 c0 a8"))
    }
}
