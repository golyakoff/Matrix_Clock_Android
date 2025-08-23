package net.agolyakov.tetrisclockble.model

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import java.util.UUID

class MyBleManager (context: Context, private val onOffReadCharacteristicHandler: McOnOffReadCharacteristicHandler)
    : BleManager(context)
{
    private var mcOnOffCharacteristic: BluetoothGattCharacteristic? = null

    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
        gatt.getService(SERVICE_CONTROL_UUID)?.let { service ->
            mcOnOffCharacteristic = service.getCharacteristic(MC_TURN_ON_CONTROL_CHAR_UUID)
        }

        return mcOnOffCharacteristic != null
    }

    override fun onServicesInvalidated() {
        mcOnOffCharacteristic = null
    }

    override fun initialize() {
        setNotificationCallback(mcOnOffCharacteristic)
            .with { device: BluetoothDevice?, data: Data? ->
                onOffReadCharacteristicHandler.onReadCharacteristicCallback(device!!, data!!)
            }

        enableNotifications(mcOnOffCharacteristic)
            .enqueue()
    }

    fun getOnOffCharacteristic() {
        readCharacteristic(mcOnOffCharacteristic)
            .with { device: BluetoothDevice?, data: Data? ->
                onOffReadCharacteristicHandler.onReadCharacteristicCallback(device!!, data!!)
            }
            .enqueue()
    }

    fun setOnOffCharacteristic(on: Boolean) {
        writeCharacteristic(
            mcOnOffCharacteristic,
            byteArrayOf((if (on) CMD_CONTROL_LED_ON else CMD_CONTROL_LED_OFF).toByte()),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
            .enqueue()
    }

    companion object {
        val SERVICE_CONTROL_UUID: UUID = UUID.fromString("5DE498A1-E7A6-4F4A-B323-913741895AD0")
        val MC_TURN_ON_CONTROL_CHAR_UUID: UUID = UUID.fromString("2E126C52-37B8-4A7D-9688-28E33104C0E1")
        const val CMD_CONTROL_LED_ON = 0x1
        const val CMD_CONTROL_LED_OFF = 0x0
    }
}
