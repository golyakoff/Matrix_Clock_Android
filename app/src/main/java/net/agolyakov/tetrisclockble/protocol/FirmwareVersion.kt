package net.agolyakov.tetrisclockble.protocol

/**
 * Firmware versions as the clock reports them and as releases are tagged: "vMAJOR.MINOR.PATCH" or,
 * for a beta, "vMAJOR.MINOR.PATCH-betaN". The patch may be missing; other suffixes, like "-test",
 * are ignored. A beta comes before its release: v2.1.0-beta1 < v2.1.0-beta2 < v2.1.0.
 */
object FirmwareVersion {
    private val VERSION_RX = Regex("""^v?(\d+)\.(\d+)(?:\.(\d+))?(?:-beta(\d+))?""")

    /** Sorts after any beta number: a release. */
    private const val RELEASE = Int.MAX_VALUE

    /** [major, minor, patch, beta number or Int.MAX_VALUE for a release], or null if [version] is not a version. */
    fun parse(version: String): List<Int>? =
        VERSION_RX.find(version.trim())?.let { m ->
            val beta = m.groupValues[4].toIntOrNull()?.takeIf { it > 0 } ?: RELEASE
            listOf(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].ifEmpty { "0" }.toInt(), beta)
        }

    /** Whether [version] is a beta (vX.Y.Z-betaN). */
    fun isBeta(version: String): Boolean = parse(version)?.let { it[3] != RELEASE } ?: false

    /** Orders versions; invalid ones come first. */
    val comparator: Comparator<String> = Comparator { a, b -> compare(parse(a), parse(b)) }

    private fun compare(a: List<Int>?, b: List<Int>?): Int {
        if (a == null || b == null) return (if (a == null) 0 else 1) - (if (b == null) 0 else 1)
        for (i in a.indices) {
            if (a[i] != b[i]) return a[i].compareTo(b[i])
        }
        return 0
    }

    /**
     * Whether [latest] is an update for a clock running [current]: only a higher version, never an
     * older one (a v1 release after v2 must not downgrade the clock). A clock whose version is not
     * known is offered any valid release.
     */
    fun isNewer(latest: String, current: String): Boolean {
        val l = parse(latest) ?: return false
        val c = parse(current) ?: return true
        return compare(l, c) > 0
    }
}
