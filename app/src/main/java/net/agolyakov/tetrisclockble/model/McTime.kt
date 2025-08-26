package net.agolyakov.tetrisclockble.model

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

data class McTime(
    val localDateTime: LocalDateTime = LocalDateTime.MIN
) {
    companion object {
        // Offset between January 1, 1900 and January 1, 1970 in seconds: 70 years plus leap years
        private const val EPOCH_DIFF_SECONDS = 2208988800L

        fun fromByteArray(raw: ByteArray): McTime {
            require(raw.size == 4) { "Time characteristic must be 4 bytes" }

            val secondsSince1900 = ((raw[3].toLong() and 0xFF) shl 24) or
                    ((raw[2].toLong() and 0xFF) shl 16) or
                    ((raw[1].toLong() and 0xFF) shl 8) or
                    (raw[0].toLong() and 0xFF)

            val epochSeconds = secondsSince1900 - EPOCH_DIFF_SECONDS

            return McTime(
                LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(epochSeconds),
                    ZoneId.systemDefault()
                )
            )
        }
    }

    fun toByteArray(): ByteArray {
        val epochSeconds = localDateTime.atZone(ZoneId.systemDefault()).toEpochSecond()
        val secondsSince1900 = epochSeconds + EPOCH_DIFF_SECONDS

        return byteArrayOf(
            (secondsSince1900 and 0xFF).toByte(),
            ((secondsSince1900 shr 8) and 0xFF).toByte(),
            ((secondsSince1900 shr 16) and 0xFF).toByte(),
            ((secondsSince1900 shr 24) and 0xFF).toByte()
        )
    }
}
