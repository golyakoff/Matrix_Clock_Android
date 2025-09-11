package net.agolyakov.tetrisclockble.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import net.agolyakov.tetrisclockble.ble.handlers.AgingOffsetReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.AutoBrightnessReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.ManualBrightnessReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.OnOffReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.RtcTemperatureReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.TimeReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.TurnOffAlarmReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.TurnOnAlarmReadCharacteristicHandler
import net.agolyakov.tetrisclockble.ble.handlers.VersionReadCharacteristicHandler
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import java.util.UUID
import javax.inject.Inject

class MyBleManager @Inject constructor(
    @ApplicationContext context: Context
) : BleManager(context) {
    private var timeReadCharacteristicHandler: TimeReadCharacteristicHandler? = null
    private var onOffReadCharacteristicHandler: OnOffReadCharacteristicHandler? = null
    private var manualBrightnessReadCharacteristicHandler: ManualBrightnessReadCharacteristicHandler? = null
    private var autoBrightnessReadCharacteristicHandler: AutoBrightnessReadCharacteristicHandler? = null
    private var turnOnAlarmReadCharacteristicHandler: TurnOnAlarmReadCharacteristicHandler? = null
    private var turnOffAlarmReadCharacteristicHandler: TurnOffAlarmReadCharacteristicHandler? = null
    private var agingOffsetReadCharacteristicHandler: AgingOffsetReadCharacteristicHandler? = null
    private var rtcTemperatureReadCharacteristicHandler: RtcTemperatureReadCharacteristicHandler? = null
    private var versionReadCharacteristicHandler: VersionReadCharacteristicHandler? = null

    private var mcTimeCharacteristic: BluetoothGattCharacteristic? = null
    private var mcOnOffCharacteristic: BluetoothGattCharacteristic? = null
    private var mcManualBrightValueCharacteristic: BluetoothGattCharacteristic? = null
    private var mcAutoBrightnessCharacteristic: BluetoothGattCharacteristic? = null
    private var mcTurnOnAlarmCharacteristic: BluetoothGattCharacteristic? = null
    private var mcTurnOffAlarmCharacteristic: BluetoothGattCharacteristic? = null
    private var mcAgingOffsetCharacteristic: BluetoothGattCharacteristic? = null
    private var mcRtcTemperatureCharacteristic: BluetoothGattCharacteristic? = null
    private var mcVersionCharacteristic: BluetoothGattCharacteristic? = null
    private var mcOtaControlCharacteristic: BluetoothGattCharacteristic? = null
    private var mcOtaDataCharacteristic: BluetoothGattCharacteristic? = null

    private var isInitialized = false
    private val pendingHandlers = mutableListOf<() -> Unit>()

    fun setHandlers(
        timeHandler: TimeReadCharacteristicHandler,
        onOffHandler: OnOffReadCharacteristicHandler,
        manualBrightnessHandler: ManualBrightnessReadCharacteristicHandler,
        autoBrightnessHandler: AutoBrightnessReadCharacteristicHandler,
        turnOnAlarmHandler: TurnOnAlarmReadCharacteristicHandler,
        turnOffAlarmHandler: TurnOffAlarmReadCharacteristicHandler,
        agingOffsetHandler: AgingOffsetReadCharacteristicHandler,
        rtcTemperatureHandler: RtcTemperatureReadCharacteristicHandler,
        versionHandler: VersionReadCharacteristicHandler
    ) {
        this.timeReadCharacteristicHandler = timeHandler
        this.onOffReadCharacteristicHandler = onOffHandler
        this.manualBrightnessReadCharacteristicHandler = manualBrightnessHandler
        this.autoBrightnessReadCharacteristicHandler = autoBrightnessHandler
        this.turnOnAlarmReadCharacteristicHandler = turnOnAlarmHandler
        this.turnOffAlarmReadCharacteristicHandler = turnOffAlarmHandler
        this.agingOffsetReadCharacteristicHandler = agingOffsetHandler
        this.rtcTemperatureReadCharacteristicHandler = rtcTemperatureHandler
        this.versionReadCharacteristicHandler = versionHandler
    }

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
            mcVersionCharacteristic = service.getCharacteristic(MC_VERSION_CHAR_UUID)
            mcOtaControlCharacteristic = service.getCharacteristic(MC_OTA_CONTROL_CHAR_UUID)
            mcOtaDataCharacteristic = service.getCharacteristic(MC_OTA_DATA_CHAR_UUID)
        }

        return mcTimeCharacteristic != null
                && mcOnOffCharacteristic != null
                && mcManualBrightValueCharacteristic != null
                && mcAutoBrightnessCharacteristic != null
                && mcTurnOnAlarmCharacteristic != null
                && mcTurnOffAlarmCharacteristic != null

                // commented for dirty backward compatibility:
                //&& mcAgingOffsetCharacteristic != null
                //&& mcRtcTemperatureCharacteristic != null
                //&& mcVersionCharacteristic != null
                //&& mcOtaControlCharacteristic != null
                //&& mcOtaDataCharacteristic != null
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
        mcVersionCharacteristic = null
        mcOtaControlCharacteristic = null
        mcOtaDataCharacteristic = null
    }

    override fun initialize() {
        isInitialized = true
        // Если handlers уже установлены, настраиваем уведомления
        if (timeReadCharacteristicHandler != null &&
            versionReadCharacteristicHandler != null &&
            onOffReadCharacteristicHandler != null)
        {
            setupNotifications()
        }
    }

    private fun setupNotifications() {
        // Time characteristic notifications
        timeReadCharacteristicHandler?.let {
            setNotificationCallback(mcTimeCharacteristic)
                .with { device: BluetoothDevice?, data: Data? ->
                    it.onReadCharacteristicCallback(
                        device!!,
                        data!!
                    )
                }
            enableNotifications(mcTimeCharacteristic).enqueue()
        }

        // On/Off characteristic notifications
        onOffReadCharacteristicHandler?.let {
            setNotificationCallback(mcOnOffCharacteristic)
                .with { device: BluetoothDevice?, data: Data? ->
                    it.onReadCharacteristicCallback(
                        device!!,
                        data!!
                    )
                }
            enableNotifications(mcOnOffCharacteristic).enqueue()
        }

        // Version characteristic notifications
        versionReadCharacteristicHandler?.let {
            setNotificationCallback(mcVersionCharacteristic)
                .with { device: BluetoothDevice?, data: Data? ->
                    it.onReadCharacteristicCallback(
                        device!!,
                        data!!
                    )
                }
            enableNotifications(mcVersionCharacteristic).enqueue()
        }
    }

    fun getTimeCharacteristic() {
        timeReadCharacteristicHandler?.let {
            readCharacteristic(mcTimeCharacteristic)
                .with { device: BluetoothDevice?, data: Data? ->
                    it.onReadCharacteristicCallback(
                        device!!,
                        data!!
                    )
                }
                .enqueue()
        }
    }

    fun getOnOffCharacteristic() {
        onOffReadCharacteristicHandler?.let {
            readCharacteristic(mcOnOffCharacteristic)
                .with { device: BluetoothDevice?, data: Data? ->
                    it.onReadCharacteristicCallback(
                        device!!,
                        data!!
                    )
                }
                .enqueue()
        }
    }

    fun getManualBrightnessCharacteristic() {
        manualBrightnessReadCharacteristicHandler?.let {
            readCharacteristic(mcManualBrightValueCharacteristic)
                .with { device: BluetoothDevice?, data: Data? ->
                    it.onReadCharacteristicCallback(
                        device!!,
                        data!!
                    )
                }
                .enqueue()
        }
    }

    fun getAutoBrightnessCharacteristic() {
        autoBrightnessReadCharacteristicHandler?.let {
            readCharacteristic(mcAutoBrightnessCharacteristic)
                .with { device: BluetoothDevice?, data: Data? ->
                    it.onReadCharacteristicCallback(
                        device!!,
                        data!!
                    )
                }
                .enqueue()
        }
    }

    fun getTurnOnAlarmCharacteristic() {
        turnOnAlarmReadCharacteristicHandler?.let {
            readCharacteristic(mcTurnOnAlarmCharacteristic)
                .with { device: BluetoothDevice?, data: Data? ->
                    it.onReadCharacteristicCallback(
                        device!!,
                        data!!
                    )
                }
                .enqueue()
        }
    }

    fun getTurnOffAlarmCharacteristic() {
        turnOffAlarmReadCharacteristicHandler?.let {
            readCharacteristic(mcTurnOffAlarmCharacteristic)
                .with { device: BluetoothDevice?, data: Data? ->
                    it.onReadCharacteristicCallback(
                        device!!,
                        data!!
                    )
                }
                .enqueue()
        }
    }

    fun getAgingOffsetCharacteristic() {
        agingOffsetReadCharacteristicHandler?.let {
            readCharacteristic(mcAgingOffsetCharacteristic)
                .with { device: BluetoothDevice?, data: Data? ->
                    it.onReadCharacteristicCallback(
                        device!!,
                        data!!
                    )
                }
                .enqueue()
        }
    }

    fun getRtcTemperatureCharacteristic() {
        rtcTemperatureReadCharacteristicHandler?.let {
            readCharacteristic(mcRtcTemperatureCharacteristic)
                .with { device: BluetoothDevice?, data: Data? ->
                    it.onReadCharacteristicCallback(
                        device!!,
                        data!!
                    )
                }
                .enqueue()
        }
    }

    fun getVersionCharacteristic() {
        versionReadCharacteristicHandler?.let {
            readCharacteristic(mcVersionCharacteristic)
                .with { device: BluetoothDevice?, data: Data? ->
                    it.onReadCharacteristicCallback(
                        device!!,
                        data!!
                    )
                }
                .enqueue()
        }
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

    fun setOtaControlCharacteristic(command: ByteArray, callback: (Boolean) -> Unit) {
        writeCharacteristic(
            mcOtaControlCharacteristic,
            command,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).with { device, data ->
            callback(true)
        }.fail { device, status ->
            callback(false)
        }.enqueue()
    }

    fun setOtaDataCharacteristic(data: ByteArray, callback: (Boolean) -> Unit) {
        writeCharacteristic(
            mcOtaDataCharacteristic,
            data,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).with { device, data ->
            callback(true)
        }.fail { device, status ->
            callback(false)
        }.enqueue()
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

        // BLE Device firmware version
        // M Read, Notify
        val MC_VERSION_CHAR_UUID: UUID = UUID.fromString("BEB5483E-36E1-4688-B7F5-EA07361B26A0")

        // BLE Device OTA update process control point
        // M Write
        val MC_OTA_CONTROL_CHAR_UUID: UUID = UUID.fromString("BEB5483E-36E1-4688-B7F5-EA07361B26A1")

        // BLE Device OTA update data upload point (to the secondary partition)
        // M Write
        val MC_OTA_DATA_CHAR_UUID: UUID = UUID.fromString("BEB5483E-36E1-4688-B7F5-EA07361B26A2")

        // OTA Control Commands
        const val OTA_CMD_START: Byte = 0x01
        const val OTA_CMD_END: Byte = 0x02
        const val OTA_CMD_SWITCH_REBOOT: Byte = 0x03
        const val OTA_CMD_ABORT: Byte = 0x04
        const val OTA_CMD_GET_STATUS: Byte = 0x05

        // OTA Status Responses
        const val OTA_STATUS_OK: Byte = 0x00
        const val OTA_STATUS_ERROR: Byte = 0x01
        const val OTA_STATUS_BUSY: Byte = 0x02
    }
}