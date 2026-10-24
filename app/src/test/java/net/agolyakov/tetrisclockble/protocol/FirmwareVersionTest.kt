package net.agolyakov.tetrisclockble.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirmwareVersionTest {

    private val release = Int.MAX_VALUE

    @Test
    fun parse() {
        assertEquals(listOf(2, 0, 1, release), FirmwareVersion.parse("v2.0.1"))
        assertEquals(listOf(1, 10, 0, release), FirmwareVersion.parse("1.10"))
        assertEquals(listOf(2, 0, 1, release), FirmwareVersion.parse("v2.0.1-test"))
        assertEquals(listOf(2, 1, 0, 3), FirmwareVersion.parse("v2.1.0-beta3"))
        assertNull(FirmwareVersion.parse("Unknown"))
        assertNull(FirmwareVersion.parse("beta"))
        assertNull(FirmwareVersion.parse(""))
    }

    @Test
    fun beta() {
        assertTrue(FirmwareVersion.isBeta("v2.1.0-beta1"))
        assertFalse(FirmwareVersion.isBeta("v2.1.0"))
        assertFalse(FirmwareVersion.isBeta("ota-test"))
    }

    @Test
    fun newer() {
        assertTrue(FirmwareVersion.isNewer("v2.0.1", "v2.0.0"))
        assertTrue(FirmwareVersion.isNewer("v2.1.0", "v2.0.9"))
        assertTrue(FirmwareVersion.isNewer("v1.10.0", "v1.9.2"))
        assertTrue(FirmwareVersion.isNewer("v2.0.0", "Unknown"))
        assertTrue(FirmwareVersion.isNewer("v2.1.0-beta1", "v2.0.0"))
        assertTrue(FirmwareVersion.isNewer("v2.1.0-beta2", "v2.1.0-beta1"))
        assertTrue(FirmwareVersion.isNewer("v2.1.0-beta10", "v2.1.0-beta9"))
        assertTrue(FirmwareVersion.isNewer("v2.1.0", "v2.1.0-beta2"))
    }

    @Test
    fun notNewer() {
        assertFalse(FirmwareVersion.isNewer("v1.8.1", "v2.0.0"))
        assertFalse(FirmwareVersion.isNewer("v2.0.0", "v2.0.0"))
        assertFalse(FirmwareVersion.isNewer("v2.0", "v2.0.0"))
        assertFalse(FirmwareVersion.isNewer("nightly", "v2.0.0"))
        assertFalse(FirmwareVersion.isNewer("v2.1.0-beta1", "v2.1.0"))
        assertFalse(FirmwareVersion.isNewer("v2.1.0-beta1", "v2.1.0-beta1"))
    }

    @Test
    fun order() {
        val sorted = listOf("v2.1.0", "ota-test", "v2.1.0-beta2", "v1.8.1", "v2.1.0-beta1")
            .sortedWith(FirmwareVersion.comparator)
        assertEquals(listOf("ota-test", "v1.8.1", "v2.1.0-beta1", "v2.1.0-beta2", "v2.1.0"), sorted)
    }
}
