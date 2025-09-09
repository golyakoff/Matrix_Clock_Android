package net.agolyakov.tetrisclockble.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import net.agolyakov.tetrisclockble.ble.handlers.AgingOffsetReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.AutoBrightnessReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.ManualBrightnessReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.OnOffReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.RtcTemperatureReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.TimeReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.TurnOffAlarmReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.TurnOnAlarmReadCharacteristicHandler
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import java.util.UUID

class MyBleManager (
    context: Context,
    val timeReadCharacteristicHandler: TimeReadCharacteristicHandler,
    val onOffReadCharacteristicHandler: OnOffReadCharacteristicHandler,
    val manualBrightnessReadCharacteristicHandler: ManualBrightnessReadCharacteristicHandler,
    val autoBrightnessReadCharacteristicHandler: AutoBrightnessReadCharacteristicHandler,
    val turnOnAlarmReadCharacteristicHandler: TurnOnAlarmReadCharacteristicHandler,
    val turnOffAlarmReadCharacteristicHandler: TurnOffAlarmReadCharacteristicHandler,
    val agingOffsetReadCharacteristicHandler: AgingOffsetReadCharacteristicHandler,
    val rtcTemperatureReadCharacteristicHandler: RtcTemperatureReadCharacteristicHandler
    )
    : BleManager(context)
{
    private var mcTimeCharacteristic: BluetoothGattCharacteristic? = null
    private var mcOnOffCharacteristic: BluetoothGattCharacteristic? = null
    private var mcManualBrightValueCharacteristic: BluetoothGattCharacteristic? = null
    private var mcAutoBrightnessCharacteristic: BluetoothGattCharacteristic? = null
    private var mcTurnOnAlarmCharacteristic: BluetoothGattCharacteristic? = null
    private var mcTurnOffAlarmCharacteristic: BluetoothGattCharacteristic? = null

    private var mcAgingOffsetCharacteristic: BluetoothGattCharacteristic? = null

    private var mcRtcTemperatureCharacteristic: BluetoothGattCharacteristic? = null

    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
        gatt.getService(SERVICE_CONTROL_UUID)?.let { service ->
            mcTimeCharacteristic = service.getCharacteristic(MC_TIME_CHAR_UUID)
            mcOnOffCharacteristic = service.getCharacteristic(MC_TURN_ON_CONTROL_CHAR_UUID)
            mcManualBrightValueCharacteristic = service.getCharacteristic(MC_MANUAL_BRIGHT_VAL_CHAR_UUID)
            mcAutoBrightnessCharacteristic = service.getCharacteristic(MC_AUTO_BRIGHT_ENABLE_CHAR_UUID)
            mcTurnOnAlarmCharacteristic = service.getCharacteristic(MC_TURN_ON_ALARM_CHAR_UUID)
            mcTurnOffAlarmCharacteristic = service.getCharacteristic(MC_TURN_OFF_ALARM_CHAR_UUID)
            mcAgingOffsetCharacteristic = service.getCharacteristic(MC_AGING_OFFSET_CHAR_UUID)
            mcRtcTemperatureCharacteristic = service.getCharacteristic(MC_TEMPERATURE_CHAR_UUID)
        }

        return mcTimeCharacteristic != null
                && mcOnOffCharacteristic != null
                && mcManualBrightValueCharacteristic != null
                && mcAutoBrightnessCharacteristic != null
                && mcTurnOnAlarmCharacteristic != null
                && mcTurnOffAlarmCharacteristic != null
                //&& mcAgingOffsetCharacteristic != null
                //&& mcRtcTemperatureCharacteristic != null

    }

    override fun onServicesInvalidated() {
        mcTimeCharacteristic = null
        mcOnOffCharacteristic = null
        mcManualBrightValueCharacteristic = null
        mcAutoBrightnessCharacteristic = null
        mcTurnOnAlarmCharacteristic = null
        mcTurnOffAlarmCharacteristic = null
        mcAgingOffsetCharacteristic = null
        mcRtcTemperatureCharacteristic = null
    }

    override fun initialize() {
        setNotificationCallback(mcTimeCharacteristic)
            .with { device: BluetoothDevice?, data: Data? ->
                timeReadCharacteristicHandler.onReadCharacteristicCallback(
                    device!!,
                    data!!)
            }
        enableNotifications(mcTimeCharacteristic).enqueue()

        setNotificationCallback(mcOnOffCharacteristic)
            .with { device: BluetoothDevice?, data: Data? ->
                onOffReadCharacteristicHandler.onReadCharacteristicCallback(
                    device!!,
                    data!!)
            }
        enableNotifications(mcOnOffCharacteristic).enqueue()
    }

    fun getTimeCharacteristic() {
        readCharacteristic(mcTimeCharacteristic)
            .with { device: BluetoothDevice?, data: Data? ->
                timeReadCharacteristicHandler.onReadCharacteristicCallback(
                    device!!,
                    data!!)
            }
            .enqueue()
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
                manualBrightnessReadCharacteristicHandler.onReadCharacteristicCallback(
                    device!!,
                    data!!)
            }
            .enqueue()
    }

    fun getAutoBrightnessCharacteristic() {
        readCharacteristic(mcAutoBrightnessCharacteristic)
            .with { device: BluetoothDevice?, data: Data? ->
                autoBrightnessReadCharacteristicHandler.onReadCharacteristicCallback(
                    device!!,
                    data!!)
            }
            .enqueue()
    }

    fun getTurnOnAlarmCharacteristic() {
        readCharacteristic(mcTurnOnAlarmCharacteristic)
            .with { device: BluetoothDevice?, data: Data? ->
                turnOnAlarmReadCharacteristicHandler.onReadCharacteristicCallback(
                    device!!,
                    data!!)
            }
            .enqueue()
    }

    fun getTurnOffAlarmCharacteristic() {
        readCharacteristic(mcTurnOffAlarmCharacteristic)
            .with { device: BluetoothDevice?, data: Data? ->
                turnOffAlarmReadCharacteristicHandler.onReadCharacteristicCallback(
                    device!!,
                    data!!)
            }
            .enqueue()
    }

    fun getAgingOffsetCharacteristic() {
        readCharacteristic(mcAgingOffsetCharacteristic)
            .with { device: BluetoothDevice?, data: Data? ->
                agingOffsetReadCharacteristicHandler.onReadCharacteristicCallback(
                    device!!,
                    data!!)
            }
            .enqueue()
    }

    fun getRtcTemperatureCharacteristic() {
        readCharacteristic(mcRtcTemperatureCharacteristic)
            .with { device: BluetoothDevice?, data: Data? ->
                rtcTemperatureReadCharacteristicHandler.onReadCharacteristicCallback(
                    device!!,
                    data!!)
            }
            .enqueue()
    }

    fun setTimeCharacteristic(time: TetrisClockTime) {
        writeCharacteristic(
            mcTimeCharacteristic,
            time.toByteArray(),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).enqueue()
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

    fun setAutoBrightnessCharacteristic(on: Boolean) {
        writeCharacteristic(
            mcAutoBrightnessCharacteristic,
            byteArrayOf((if (on) CMD_CONTROL_LED_ON else CMD_CONTROL_LED_OFF).toByte()),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).enqueue()
    }

    fun setTurnOnAlarmCharacteristic(alarm: TetrisClockAlarm) {
        writeCharacteristic(
            mcTurnOnAlarmCharacteristic,
            alarm.toByteArray(),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).enqueue()
    }

    fun setTurnOffAlarmCharacteristic(alarm: TetrisClockAlarm) {
        writeCharacteristic(
            mcTurnOffAlarmCharacteristic,
            alarm.toByteArray(),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).enqueue()
    }

    fun setAgingOffsetCharacteristic(agingOffset: Int) {
        val lower8Bits = agingOffset and 0xFF
        val data = if (lower8Bits and 0x80 != 0) {
            (lower8Bits - 256).toByte()
        } else {
            lower8Bits.toByte()
        }

        writeCharacteristic(
            mcAgingOffsetCharacteristic,
            byteArrayOf(data),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).enqueue()
    }


    companion object {
        // Matrix Clock Service
        val SERVICE_CONTROL_UUID: UUID = UUID.fromString("5DE498A1-E7A6-4F4A-B323-913741895AD0")

        // Manual control point to turn off/on the displaying of the time
        // This manual operation has the lower priority than the alarm.
        // Mode: Read, Write, Notify
        val MC_TURN_ON_CONTROL_CHAR_UUID: UUID = UUID.fromString("2E126C52-37B8-4A7D-9688-28E33104C0E1")
        const val CMD_CONTROL_LED_ON = 0x1
        const val CMD_CONTROL_LED_OFF = 0x0

        // Control point to switch between auto and manual brightness adjustment value
        // Mode: Read, Write
        val MC_AUTO_BRIGHT_ENABLE_CHAR_UUID: UUID = UUID.fromString("9B078810-99AB-4423-B3A8-6F2E86A09582")

        // Control point to setup manual brightness adjustment value
        // Possible values are 0..15 (0 is not fully Off, just minimum value)
        // Mode: Read, Write
        val MC_MANUAL_BRIGHT_VAL_CHAR_UUID: UUID = UUID.fromString("117ED80D-AF6E-4E4D-B900-48F68725A7D3")

        // Alarm timer to turn ON the clock at a specific time (for example, in the morning).
        // This alarm event takes precedence over manual control.
        // Mode: Read, Write
        val MC_TURN_ON_ALARM_CHAR_UUID: UUID = UUID.fromString("6BDBD293-B623-411C-BB2A-F429EAF93CF1")

        // Alarm timer to turn OFF the clock at a specific time (for example, at night).
        // This alarm event takes precedence over manual control.
        // Mode: Read, Write
        val MC_TURN_OFF_ALARM_CHAR_UUID: UUID = UUID.fromString("84915734-BF86-46E7-B394-22E25B3F9007")

        // MatrixClock time in UINT32 format: number of seconds since 1900 year.
        // Time is in local time zone (not UTC, no time zone specified).
        // Mode: Read, Write, Notify
        val MC_TIME_CHAR_UUID: UUID = UUID.fromString("D5BD8D18-BD9A-4EF4-B206-8C78FFBE2774")

        // MatrixClock time in formatted string "YYYY.MM.DD HH:mm:ss", example: "2023.12.31 09:05:42"
        // Time is in local time zone (not UTC, no time zone specified).
        // Mode: Read, Notify
        // val MC_TIME_STR_CHAR_UUID: UUID = UUID.fromString("AA063B0F-DB36-47D0-8F19-A70FA97D86DF")

        // Control point to setup aging offset value (8-bit signed integer)
        // Mode: Read, Write
        val MC_AGING_OFFSET_CHAR_UUID: UUID = UUID.fromString("F89E201D-434F-4675-B60E-2CF682200C50")

        // RTC chip temperature formatted string "-XX.YY C" with the 0.25 degree precision
        // Mode: Read
        val MC_TEMPERATURE_CHAR_UUID: UUID = UUID.fromString("13BE1932-508D-4BEB-AFBC-2C21D1397920")

    }
}