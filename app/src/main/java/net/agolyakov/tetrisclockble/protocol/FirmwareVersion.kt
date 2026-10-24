package net.agolyakov.tetrisclockble.protocol

/**
 * Firmware versions as the clock reports them and as releases are tagged: "vMAJOR.MINOR.PATCH"
 * (the patch may be missing; anything after it, like "-test", is ignored).
 */
object FirmwareVersion {
    private val VERSION_RX = Regex("""^v?(\d+)\.(\d+)(?:\.(\d+))?""")

    /** [major, minor, patch], or null if [version] is not a version. */
    fun parse(version: String): List<Int>? =
        VERSION_RX.find(version.trim())?.let { m ->
            listOf(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].ifEmpty { "0" }.toInt())
        }

    /**
     * Whether [latest] is an update for a clock running [current]: only a higher version, never an
     * older one (a v1 release after v2 must not downgrade the clock). A clock whose version is not
     * known is offered any valid release.
     */
    fun isNewer(latest: String, current: String): Boolean {
        val l = parse(latest) ?: return false
        val c = parse(current) ?: return true
        for (i in l.indices) {
            if (l[i] != c[i]) return l[i] > c[i]
        }
        return false
    }
}
