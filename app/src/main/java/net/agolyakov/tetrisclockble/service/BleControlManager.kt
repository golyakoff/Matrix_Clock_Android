package net.agolyakov.tetrisclockble.service

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Context
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import java.util.*
import javax.inject.Inject

class BleControlManager @Inject constructor(
    context: Context
) : BleManager(context)
{
    private var ledCharacteristic: BluetoothGattCharacteristic? = null

    fun enableLed(enable: Boolean) {
        writeCharacteristic(
            ledCharacteristic,
            byteArrayOf((if (enable) CMD_CONTROL_LED_ON else CMD_CONTROL_LED_OFF).toByte()),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
            .enqueue()
    }

    fun requestLedEnabledStatusUpdate() {
        readCharacteristic(ledCharacteristic)
            .with { device: BluetoothDevice?, data: Data? ->
                TODO("ledReadCharacteristicHandler.onReadCharacteristicCallback(device!!, data!!)")
            }
            .enqueue()
    }

    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
        gatt.getService(SERVICE_CONTROL_UUID)?.let { service ->
            ledCharacteristic = service.getCharacteristic(LED_CHAR_UUID)
        }

        return ledCharacteristic != null
    }

    override fun onServicesInvalidated() {
        ledCharacteristic = null
    }

    override fun initialize() {
        setNotificationCallback(ledCharacteristic)
            .with { device: BluetoothDevice?, data: Data? ->
                TODO("ledReadCharacteristicHandler.onReadCharacteristicCallback(device!!, data!!)")
            }

        enableNotifications(ledCharacteristic)
            .enqueue()
    }

    companion object {
        val SERVICE_CONTROL_UUID: UUID = UUID.fromString("000000ff-0000-1000-8000-00805f9b34fb")
        val LED_CHAR_UUID: UUID = UUID.fromString("0000ff03-0000-1000-8000-00805f9b34fb")
        const val CMD_CONTROL_LED_ON = 0x1
        const val CMD_CONTROL_LED_OFF = 0x0
    }
}
