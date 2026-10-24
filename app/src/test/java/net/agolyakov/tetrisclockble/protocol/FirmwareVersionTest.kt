package net.agolyakov.tetrisclockble.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirmwareVersionTest {

    @Test
    fun parse() {
        assertEquals(listOf(2, 0, 1), FirmwareVersion.parse("v2.0.1"))
        assertEquals(listOf(1, 10, 0), FirmwareVersion.parse("1.10"))
        assertEquals(listOf(2, 0, 1), FirmwareVersion.parse("v2.0.1-test"))
        assertNull(FirmwareVersion.parse("Unknown"))
        assertNull(FirmwareVersion.parse(""))
    }

    @Test
    fun newer() {
        assertTrue(FirmwareVersion.isNewer("v2.0.1", "v2.0.0"))
        assertTrue(FirmwareVersion.isNewer("v2.1.0", "v2.0.9"))
        assertTrue(FirmwareVersion.isNewer("v1.10.0", "v1.9.2"))
        assertTrue(FirmwareVersion.isNewer("v2.0.0", "Unknown"))
    }

    @Test
    fun notNewer() {
        assertFalse(FirmwareVersion.isNewer("v1.8.1", "v2.0.0"))
        assertFalse(FirmwareVersion.isNewer("v2.0.0", "v2.0.0"))
        assertFalse(FirmwareVersion.isNewer("v2.0", "v2.0.0"))
        assertFalse(FirmwareVersion.isNewer("nightly", "v2.0.0"))
    }
}
