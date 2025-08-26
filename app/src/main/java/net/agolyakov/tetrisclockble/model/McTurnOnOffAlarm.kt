package net.agolyakov.tetrisclockble.model

data class McTurnOnOffAlarm(
    val isActive: Boolean = false,
    val hours: Byte = 0,
    val minutes: Byte = 0
) {
    companion object {
        const val TOTAL_MINUTES_MASK = 0x07FF // 0b0000011111111111
        const val IS_ACTIVE_MASK = 0x0800     // 0b0000100000000000

        fun fromByteArray(binaryData: ByteArray): McTurnOnOffAlarm {
            require(binaryData.size >= 2) {
                "Expected binary data size should be at least 2 bytes."
            }

            val result = ((binaryData[1].toInt() shl 8) or (binaryData[0].toInt())) and 0xffff
            val isActive = (result and IS_ACTIVE_MASK) != 0
            val totalMinutes = (result and TOTAL_MINUTES_MASK)
            val hours = (totalMinutes / 60).toByte()
            val minutes = (totalMinutes % 60).toByte()

            return McTurnOnOffAlarm(isActive, hours, minutes)
        }
    }

    fun toByteArray(): ByteArray {
        val totalMinutes = minutes + hours * 60
        val result = if (isActive) totalMinutes or IS_ACTIVE_MASK else totalMinutes

        return byteArrayOf(
            (result and 0xff).toByte(),          // Младшие 8 бит
            ((result shr 8) and 0xff).toByte()   // Старший бит активности и пустые старшие биты
        )
    }
}
