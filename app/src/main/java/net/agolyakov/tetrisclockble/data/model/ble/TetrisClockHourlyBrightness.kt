package net.agolyakov.tetrisclockble.data.model.ble

data class TetrisClockHourlyBrightness(
    val values: List<Int> = List(HOURS) { DEFAULT_VALUE }
) {
    companion object {
        const val HOURS = 24
        const val MIN_VALUE = 0
        const val MAX_VALUE = 15
        const val DEFAULT_VALUE = 8

        fun fromByteArray(binaryData: ByteArray): TetrisClockHourlyBrightness {
            val values = (0 until HOURS).map { hour ->
                if (hour < binaryData.size)
                    (binaryData[hour].toInt() and 0xFF).coerceIn(MIN_VALUE, MAX_VALUE)
                else
                    MIN_VALUE
            }

            return TetrisClockHourlyBrightness(values)
        }
    }

    fun toByteArray(): ByteArray =
        ByteArray(HOURS) { hour ->
            values.getOrElse(hour) { MIN_VALUE }.coerceIn(MIN_VALUE, MAX_VALUE).toByte()
        }
}
