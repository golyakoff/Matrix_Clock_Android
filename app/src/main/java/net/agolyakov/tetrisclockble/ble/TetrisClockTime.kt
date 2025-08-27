package net.agolyakov.tetrisclockble.ble

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class TetrisClockTime(
    val localDateTime: LocalDateTime = LocalDateTime.MIN
) {
    companion object {
        fun fromByteArray(raw: ByteArray): TetrisClockTime {
            require(raw.size == 4) { "Time characteristic must be 4 bytes" }

            val secondsSince1900 =
                    ((raw[3].toLong() and 0xFF) shl 24) or
                    ((raw[2].toLong() and 0xFF) shl 16) or
                    ((raw[1].toLong() and 0xFF) shl 8) or
                    (raw[0].toLong() and 0xFF)

            return TetrisClockTime(
                LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(secondsSince1900),
                    ZoneOffset.UTC
                )
            )
        }

        fun now(): TetrisClockTime =
            TetrisClockTime(LocalDateTime.now())
    }

    fun toByteArray(): ByteArray {
        val secondsSince1900 = localDateTime.atZone(ZoneOffset.UTC).toEpochSecond()

        return byteArrayOf(
            (secondsSince1900 and 0xFF).toByte(),
            ((secondsSince1900 shr 8) and 0xFF).toByte(),
            ((secondsSince1900 shr 16) and 0xFF).toByte(),
            ((secondsSince1900 shr 24) and 0xFF).toByte()
        )
    }

    fun formatTime(): String =
        localDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))

    fun formatDate(): String =
        localDateTime.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))

}
