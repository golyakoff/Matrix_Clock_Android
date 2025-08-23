package net.agolyakov.tetrisclockble.model

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import java.util.UUID

class MyBleManager (
    context: Context,
    private val onOffReadCharacteristicHandler: McOnOffReadCharacteristicHandler,
    private val manualBrightValueCharacteristicHandler: McManualBrightnessReadCharacteristicHandler
)
    : BleManager(context)
{
    private var mcOnOffCharacteristic: BluetoothGattCharacteristic? = null
    private var mcManualBrightValueCharacteristic: BluetoothGattCharacteristic? = null

    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
        gatt.getService(SERVICE_CONTROL_UUID)?.let { service ->
            mcOnOffCharacteristic = service.getCharacteristic(MC_TURN_ON_CONTROL_CHAR_UUID)
            mcManualBrightValueCharacteristic = service.getCharacteristic(MC_MANUAL_BRIGHT_VAL_CHAR_UUID)
        }

        return mcOnOffCharacteristic != null
                && mcManualBrightValueCharacteristic != null
    }

    override fun onServicesInvalidated() {
        mcOnOffCharacteristic = null
        mcManualBrightValueCharacteristic = null
    }

    override fun initialize() {
        setNotificationCallback(mcOnOffCharacteristic)
            .with { device: BluetoothDevice?, data: Data? ->
                onOffReadCharacteristicHandler.onReadCharacteristicCallback(
                    device!!,
                    data!!)
            }
        enableNotifications(mcOnOffCharacteristic).enqueue()

        setNotificationCallback(mcManualBrightValueCharacteristic)
            .with { device: BluetoothDevice?, data: Data? ->
                manualBrightValueCharacteristicHandler.onReadCharacteristicCallback(
                    device!!,
                    data!!)
            }
        enableNotifications(mcManualBrightValueCharacteristic).enqueue()
    }

    fun getOnOffCharacteristic() {
        readCharacteristic(mcOnOffCharacteristic)
            .with { device: BluetoothDevice?, data: Data? ->
                onOffReadCharacteristicHandler.onReadCharacteristicCallback(
                    device!!,
                    data!!)
            }
            .enqueue()
    }

    fun getManualBrightnessCharacteristic() {
        readCharacteristic(mcManualBrightValueCharacteristic)
            .with { device: BluetoothDevice?, data: Data? ->
                manualBrightValueCharacteristicHandler.onReadCharacteristicCallback(
                    device!!,
                    data!!)
            }
            .enqueue()
    }

    fun setOnOffCharacteristic(on: Boolean) {
        writeCharacteristic(
            mcOnOffCharacteristic,
            byteArrayOf((if (on) CMD_CONTROL_LED_ON else CMD_CONTROL_LED_OFF).toByte()),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).enqueue()
    }

    fun setManualBrightnessCharacteristic(brightness: Byte) {
        writeCharacteristic(
            mcManualBrightValueCharacteristic,
            byteArrayOf(brightness),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).enqueue()
    }

    companion object {
        // Matrix Clock Service
        val SERVICE_CONTROL_UUID: UUID = UUID.fromString("5DE498A1-E7A6-4F4A-B323-913741895AD0")

        // Manual control point to turn off/on the displaying of the time
        // This manual operation has the lower priority than the alarm.
        // M Read, Write, Notify
        val MC_TURN_ON_CONTROL_CHAR_UUID: UUID = UUID.fromString("2E126C52-37B8-4A7D-9688-28E33104C0E1")
        const val CMD_CONTROL_LED_ON = 0x1
        const val CMD_CONTROL_LED_OFF = 0x0

        // Control point to switch between auto and manual brightness adjustment value
        // M Read, Write
        //val MC_AUTO_BRIGHT_ENABLE_CHAR_UUID: UUID = UUID.fromString("9B078810-99AB-4423-B3A8-6F2E86A09582")

        // Control point to setup manual brightness adjustment value
        // Possible values are 0..15 (0 is not fully Off, just minimum value)
        // M Read, Write
        val MC_MANUAL_BRIGHT_VAL_CHAR_UUID: UUID = UUID.fromString("117ED80D-AF6E-4E4D-B900-48F68725A7D3")
    }
}
