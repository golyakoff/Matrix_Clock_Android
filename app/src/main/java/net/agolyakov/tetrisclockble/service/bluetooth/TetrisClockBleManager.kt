package net.agolyakov.tetrisclockble.service.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockAlarm
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockTime
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockHourlyBrightness
import net.agolyakov.tetrisclockble.service.bluetooth.handlers.AgingOffsetReadCharacteristicHandler
import net.agolyakov.tetrisclockble.service.bluetooth.handlers.AutoBrightnessReadCharacteristicHandler
import net.agolyakov.tetrisclockble.service.bluetooth.handlers.AnimationSplashReadCharacteristicHandler
import net.agolyakov.tetrisclockble.service.bluetooth.handlers.HourlyBrightnessReadCharacteristicHandler
import net.agolyakov.tetrisclockble.service.bluetooth.handlers.ManualBrightnessReadCharacteristicHandler
import net.agolyakov.tetrisclockble.service.bluetooth.handlers.OnOffReadCharacteristicHandler
import net.agolyakov.tetrisclockble.service.bluetooth.handlers.PixelColorOrderReadCharacteristicHandler
import net.agolyakov.tetrisclockble.service.bluetooth.handlers.RtcTemperatureReadCharacteristicHandler
import net.agolyakov.tetrisclockble.service.bluetooth.handlers.TimeReadCharacteristicHandler
import net.agolyakov.tetrisclockble.service.bluetooth.handlers.TurnOffAlarmReadCharacteristicHandler
import net.agolyakov.tetrisclockble.service.bluetooth.handlers.TurnOnAlarmReadCharacteristicHandler
import net.agolyakov.tetrisclockble.service.bluetooth.handlers.VersionReadCharacteristicHandler
import net.agolyakov.tetrisclockble.service.bluetooth.handlers.FlashSizeReadCharacteristicHandler
import net.agolyakov.tetrisclockble.service.bluetooth.handlers.AnimationCountReadCharacteristicHandler
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import java.util.UUID

class TetrisClockBleManager(
    @ApplicationContext context: Context,
    val timeReadCharacteristicHandler: TimeReadCharacteristicHandler,
    val onOffReadCharacteristicHandler: OnOffReadCharacteristicHandler,
    val manualBrightnessReadCharacteristicHandler: ManualBrightnessReadCharacteristicHandler,
    val autoBrightnessReadCharacteristicHandler: AutoBrightnessReadCharacteristicHandler,
    val colorOrderReadCharacteristicHandler: PixelColorOrderReadCharacteristicHandler,
    val animationSplashReadCharacteristicHandler: AnimationSplashReadCharacteristicHandler,
    val hourlyBrightnessReadCharacteristicHandler: HourlyBrightnessReadCharacteristicHandler,
    var turnOnAlarmReadCharacteristicHandler: TurnOnAlarmReadCharacteristicHandler,
    val turnOffAlarmReadCharacteristicHandler: TurnOffAlarmReadCharacteristicHandler,
    val agingOffsetReadCharacteristicHandler: AgingOffsetReadCharacteristicHandler,
    val rtcTemperatureReadCharacteristicHandler: RtcTemperatureReadCharacteristicHandler,
    val versionReadCharacteristicHandler: VersionReadCharacteristicHandler,
    val flashSizeReadCharacteristicHandler: FlashSizeReadCharacteristicHandler,
    val animationCountReadCharacteristicHandler: AnimationCountReadCharacteristicHandler
) : BleManager(context) {
    private val _tag = "TetrisClockBleManager"
    // The MTU exchange can run more than once per connection (initialize() re-runs after the GATT
    // cache refresh), and the first attempt often fails on the still-settling connection. Track the
    // best value seen plus a separate readiness signal, so a failed early attempt can't poison a
    // later successful one - a one-shot CompletableDeferred<Int> completed on the first (failed)
    // attempt pinned the MTU at 23 forever even after the later exchange succeeded at 515, which is
    // what made every OTA crawl with 20-byte chunks.
    @Volatile private var _negotiatedMtu = 23
    private val _mtuReady = CompletableDeferred<Unit>()
    private var _hasRefreshedGattCache = false
    private var mcTimeCharacteristic: BluetoothGattCharacteristic? = null
    private var mcOnOffCharacteristic: BluetoothGattCharacteristic? = null
    private var mcManualBrightValueCharacteristic: BluetoothGattCharacteristic? = null
    private var mcAutoBrightnessCharacteristic: BluetoothGattCharacteristic? = null
    private var mcColorOrderCharacteristic: BluetoothGattCharacteristic? = null
    private var mcAnimationSplashCharacteristic: BluetoothGattCharacteristic? = null
    private var mcHourlyBrightnessCharacteristic: BluetoothGattCharacteristic? = null
    private var mcTurnOnAlarmCharacteristic: BluetoothGattCharacteristic? = null
    private var mcTurnOffAlarmCharacteristic: BluetoothGattCharacteristic? = null
    private var mcAgingOffsetCharacteristic: BluetoothGattCharacteristic? = null
    private var mcRtcTemperatureCharacteristic: BluetoothGattCharacteristic? = null
    private var mcVersionCharacteristic: BluetoothGattCharacteristic? = null
    private var mcFlashSizeCharacteristic: BluetoothGattCharacteristic? = null
    private var mcAnimationCountCharacteristic: BluetoothGattCharacteristic? = null
    private var mcOtaControlCharacteristic: BluetoothGattCharacteristic? = null
    private var mcOtaDataCharacteristic: BluetoothGattCharacteristic? = null

    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
        gatt.getService(SERVICE_CONTROL_UUID)?.let { service ->
            mcTimeCharacteristic = service.getCharacteristic(MC_TIME_CHAR_UUID)
            mcOnOffCharacteristic = service.getCharacteristic(MC_TURN_ON_CONTROL_CHAR_UUID)
            mcManualBrightValueCharacteristic = service.getCharacteristic(MC_MANUAL_BRIGHT_VAL_CHAR_UUID)
            mcAutoBrightnessCharacteristic = service.getCharacteristic(MC_AUTO_BRIGHT_ENABLE_CHAR_UUID)
            mcColorOrderCharacteristic = service.getCharacteristic(MC_COLOR_ORDER_CHAR_UUID)
            mcAnimationSplashCharacteristic = service.getCharacteristic(MC_ANIMATION_SPLASH_CHAR_UUID)
            mcHourlyBrightnessCharacteristic = service.getCharacteristic(MC_HOURLY_BRIGHTNESS_CHAR_UUID)
            mcTurnOnAlarmCharacteristic = service.getCharacteristic(MC_TURN_ON_ALARM_CHAR_UUID)
            mcTurnOffAlarmCharacteristic = service.getCharacteristic(MC_TURN_OFF_ALARM_CHAR_UUID)
            mcAgingOffsetCharacteristic = service.getCharacteristic(MC_AGING_OFFSET_CHAR_UUID)
            mcRtcTemperatureCharacteristic = service.getCharacteristic(MC_TEMPERATURE_CHAR_UUID)
            mcVersionCharacteristic = service.getCharacteristic(MC_VERSION_CHAR_UUID)
            mcFlashSizeCharacteristic = service.getCharacteristic(MC_FLASH_SIZE_CHAR_UUID)
            mcAnimationCountCharacteristic = service.getCharacteristic(MC_ANIMATION_COUNT_CHAR_UUID)
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
                //&& mcHourlyBrightnessCharacteristic != null
                //&& mcColorOrderCharacteristic != null
    }

    override fun onServicesInvalidated() {
        mcTimeCharacteristic = null
        mcOnOffCharacteristic = null
        mcManualBrightValueCharacteristic = null
        mcAutoBrightnessCharacteristic = null
        mcColorOrderCharacteristic = null
        mcAnimationSplashCharacteristic = null
        mcHourlyBrightnessCharacteristic = null
        mcTurnOnAlarmCharacteristic = null
        mcTurnOffAlarmCharacteristic = null
        mcAgingOffsetCharacteristic = null
        mcRtcTemperatureCharacteristic = null
        mcVersionCharacteristic = null
        mcFlashSizeCharacteristic = null
        mcAnimationCountCharacteristic = null
        mcOtaControlCharacteristic = null
        mcOtaDataCharacteristic = null
    }

    override fun initialize() {
        super.initialize()
        // The ESP32 firmware doesn't send a Service Changed indication when its GATT table
        // gains new characteristics between firmware updates, so Android would otherwise keep
        // serving a stale cached service list (silently failing to find/write new
        // characteristics) until the phone's Bluetooth cache is cleared some other way.
        // Refreshing triggers the library to rediscover services and call initialize() again
        // on its own, so this must run only once per manager instance or it loops forever.
        if (!_hasRefreshedGattCache) {
            _hasRefreshedGattCache = true
            refreshDeviceCache().enqueue()
        }
        setupNotifications()
        setupMtu()
    }

    /**
     * Negotiated ATT MTU (suspends until the MTU exchange completes after connect).
     * OTA chunks must not exceed MTU-3 or Android falls back to slow long writes.
     */
    suspend fun awaitNegotiatedMtu(): Int {
        // Wait briefly for the first successful exchange, then return the best MTU seen (falling
        // back to the 23-byte default only if none ever succeeded). By the time an OTA starts the
        // exchange has long since completed, so this returns immediately in practice.
        withTimeoutOrNull(3000) { _mtuReady.await() }
        return _negotiatedMtu
    }

    /**
     * Asks Android for the fastest connection interval (CONNECTION_PRIORITY_HIGH). Used for the
     * duration of an OTA transfer: with write-with-response, throughput is one chunk per connection
     * interval, so a shorter interval directly speeds the transfer up and cuts write timeouts.
     */
    fun requestHighConnectionPriority() {
        requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH).enqueue()
    }

    /** Restores the default (balanced) connection interval after an OTA transfer. */
    fun requestBalancedConnectionPriority() {
        requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED).enqueue()
    }

    private fun setupMtu() {
        requestMtu(BLE_MTU)
            .with { device, mtu ->
            Log.d(_tag, "MTU set to: $mtu")
            _negotiatedMtu = mtu
            if (!_mtuReady.isCompleted) _mtuReady.complete(Unit)
        }.fail { device, status ->
            // Don't pin a fallback here: initialize() runs setupMtu() again after the GATT cache
            // refresh, and that later attempt usually succeeds - keep the best value seen so far.
            Log.w(_tag, "MTU request failed (status=$status), keeping current MTU ${_negotiatedMtu}")
        }.enqueue()
    }

    private fun setupNotifications() {
        // Time characteristic notifications
        timeReadCharacteristicHandler.let {
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
        onOffReadCharacteristicHandler.let {
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
        versionReadCharacteristicHandler.let {
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
        timeReadCharacteristicHandler.let {
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
        onOffReadCharacteristicHandler.let {
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
        manualBrightnessReadCharacteristicHandler.let {
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
        autoBrightnessReadCharacteristicHandler.let {
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

    fun getColorOrderCharacteristic() {
        colorOrderReadCharacteristicHandler.let {
            readCharacteristic(mcColorOrderCharacteristic)
                .with { device: BluetoothDevice?, data: Data? ->
                    it.onReadCharacteristicCallback(
                        device!!,
                        data!!
                    )
                }
                .enqueue()
        }
    }

    fun getAnimationSplashCharacteristic() {
        animationSplashReadCharacteristicHandler.let {
            readCharacteristic(mcAnimationSplashCharacteristic)
                .with { device: BluetoothDevice?, data: Data? ->
                    it.onReadCharacteristicCallback(
                        device!!,
                        data!!
                    )
                }
                .enqueue()
        }
    }

    fun getHourlyBrightnessCharacteristic() {
        hourlyBrightnessReadCharacteristicHandler.let {
            readCharacteristic(mcHourlyBrightnessCharacteristic)
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
        turnOnAlarmReadCharacteristicHandler.let {
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
        turnOffAlarmReadCharacteristicHandler.let {
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
        agingOffsetReadCharacteristicHandler.let {
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
        rtcTemperatureReadCharacteristicHandler.let {
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
        versionReadCharacteristicHandler.let {
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

    fun getFlashSizeCharacteristic() {
        // mcFlashSizeCharacteristic is null on older firmware that doesn't expose it; readCharacteristic
        // on a null target is a no-op, so the flash-size flow just stays at its "unknown" default.
        flashSizeReadCharacteristicHandler.let {
            readCharacteristic(mcFlashSizeCharacteristic)
                .with { device: BluetoothDevice?, data: Data? ->
                    it.onReadCharacteristicCallback(
                        device!!,
                        data!!
                    )
                }
                .enqueue()
        }
    }

    fun getAnimationCountCharacteristic() {
        // Null on older firmware that doesn't expose it; the read is then a no-op and the flow keeps
        // its 0 default, so the app falls back to showing its whole bundled animation catalog.
        animationCountReadCharacteristicHandler.let {
            readCharacteristic(mcAnimationCountCharacteristic)
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

    fun setColorOrderCharacteristic(useRrbbgg: Boolean) {
        writeCharacteristic(
            mcColorOrderCharacteristic,
            byteArrayOf((if (useRrbbgg) CMD_CONTROL_LED_ON else CMD_CONTROL_LED_OFF).toByte()),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).enqueue()
    }

    fun setAnimationSplashCharacteristic(mode: Int, duration: Int, animationIndex: Int, previewNow: Boolean) {
        var value = mode and ANIM_SPLASH_MODE_MASK
        value = value or ((duration shl ANIM_SPLASH_DURATION_SHIFT) and ANIM_SPLASH_DURATION_MASK)
        value = value or ((animationIndex shl ANIM_SPLASH_INDEX_SHIFT) and ANIM_SPLASH_INDEX_MASK)
        if (previewNow) value = value or ANIM_SPLASH_PREVIEW_MASK

        // Second byte carries the animation index's high bit (indices 8..15), added in firmware
        // v1.8.0. Older firmware ignores a write that isn't exactly one byte, so only send it when
        // it is actually needed - then a clock that predates it still gets animations 0..7.
        val indexHi = (animationIndex shr ANIM_SPLASH_INDEX_HI_SHIFT) and ANIM_SPLASH_INDEX_HI_MASK
        val bytes = if (indexHi != 0)
            byteArrayOf(value.toByte(), indexHi.toByte())
        else
            byteArrayOf(value.toByte())

        writeCharacteristic(
            mcAnimationSplashCharacteristic,
            bytes,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).enqueue()
    }

    fun setHourlyBrightnessCharacteristic(hourlyBrightness: TetrisClockHourlyBrightness) {
        writeCharacteristic(
            mcHourlyBrightnessCharacteristic,
            hourlyBrightness.toByteArray(),
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
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT /* WRITE_TYPE_NO_RESPONSE */
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

        // Control point to switch the LED matrix pixel color order.
        // 0 = RRGGBB (default), 1 = RRBBGG (green/blue swapped, red unchanged)
        // Mode: Read, Write
        val MC_COLOR_ORDER_CHAR_UUID: UUID = UUID.fromString("36B8588D-6F99-4BDC-8B6F-13089F234978")

        // Animation Splash control point: an animated screensaver played at the start of the hour.
        // Byte 0: bits0..1 = cadence mode (0 = off, 1 = daily, 2 = every 3h, 3 = hourly),
        // bits2..3 = playback duration (0 = 10s, 1 = 20s, 2 = 40s, 3 = 60s), bits4..6 = the low
        // 3 bits of the animation index, bit7 = "play now" preview command (fires an immediate
        // one-off preview, independent of time and mode).
        // Byte 1 (firmware v1.8.0+): bit0 = the index's high bit, so the index spans 0..15; the
        // rest is reserved. Optional in both directions - a one-byte value means index 0..7.
        // Mode: Read, Write
        val MC_ANIMATION_SPLASH_CHAR_UUID: UUID = UUID.fromString("A3F1C2D4-5B6E-47A8-9C0D-1E2F3A4B5C6D")
        const val ANIM_SPLASH_MODE_MASK = 0x03
        const val ANIM_SPLASH_DURATION_MASK = 0x0C
        const val ANIM_SPLASH_DURATION_SHIFT = 2
        const val ANIM_SPLASH_INDEX_MASK = 0x70
        const val ANIM_SPLASH_INDEX_SHIFT = 4
        const val ANIM_SPLASH_PREVIEW_MASK = 0x80
        const val ANIM_SPLASH_INDEX_HI_MASK = 0x01   // byte 1: the high bit of the index
        const val ANIM_SPLASH_INDEX_HI_SHIFT = 3     // ...which is bit 3 of the index itself

        // Hourly brightness schedule: 24 brightness nibbles (0..15), one per hour of day
        // (index 0 = 00h..00:59, ... index 23 = 23h..23:59).
        // Used as the brightness source when auto brightness is enabled (MC_AUTO_BRIGHT_ENABLE_CHAR_UUID).
        // Mode: Read, Write
        val MC_HOURLY_BRIGHTNESS_CHAR_UUID: UUID = UUID.fromString("C2C5D9AA-4C0B-4A69-9E9B-9E1D8B7A2F31")

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

        // BLE Device physical flash chip size in whole megabytes (1 byte, e.g. 4 or 16)
        // M Read
        val MC_FLASH_SIZE_CHAR_UUID: UUID = UUID.fromString("C9F0E1D2-3B4A-5C6D-7E8F-A0B1C2D3E4F5")

        // Number of Animation Splash animations this firmware build ships (1 byte)
        // M Read
        val MC_ANIMATION_COUNT_CHAR_UUID: UUID = UUID.fromString("C9F0E1D2-3B4A-5C6D-7E8F-A0B1C2D3E4F6")

        // BLE Device OTA update process control point
        // M Write
        val MC_OTA_CONTROL_CHAR_UUID: UUID = UUID.fromString("BEB5483E-36E1-4688-B7F5-EA07361B26A1")

        // BLE Device OTA update data upload point (to the secondary partition)
        // M Write
        val MC_OTA_DATA_CHAR_UUID: UUID = UUID.fromString("BEB5483E-36E1-4688-B7F5-EA07361B26A2")

        // OTA Control Commands
        const val OTA_CMD_START: Byte = 0x01
        const val OTA_CMD_END: Byte = 0x02
        const val OTA_CMD_ABORT: Byte = 0x03

        // MTU
        const val BLE_MTU: Int = 517
    }
}