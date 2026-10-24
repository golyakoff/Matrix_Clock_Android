package net.agolyakov.tetrisclockble.data.model.ble

sealed class ConnectionState {
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    object Disconnecting : ConnectionState()
    object Disconnected : ConnectionState()
    object Ready : ConnectionState()
    /** The system pairing dialog is (or is about to be) shown (firmware v2.1.0+). */
    object Bonding : ConnectionState()

    /**
     * The clock did not accept this phone: it paired outside the pairing window (the clock drops such
     * keys and disconnects), or the clock forgot the bond (a 6th phone took its place). The user has
     * to hold the clock's touch pad for 5 seconds and connect again.
     * [staleBondRemoved]: the app already removed the phone's side of the bond.
     */
    data class PairingRejected(val staleBondRemoved: Boolean) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}