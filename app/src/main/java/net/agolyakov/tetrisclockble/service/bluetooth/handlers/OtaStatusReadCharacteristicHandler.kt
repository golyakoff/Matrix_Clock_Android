package net.agolyakov.tetrisclockble.service.bluetooth.handlers

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.MutableStateFlow
import no.nordicsemi.android.ble.data.Data

class OtaStatusReadCharacteristicHandler(
    private val otaStatus: (ERROR) -> ERROR
) : ReadCharacteristicHandler {
    override fun onReadCharacteristicCallback(device: BluetoothDevice, data: Data) {
        val bytes = data.value ?: byteArrayOf()
        otaStatus.value = String(bytes, Charsets.US_ASCII)
    }
}
