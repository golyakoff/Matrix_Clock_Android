package net.agolyakov.tetrisclockble.service.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import net.agolyakov.tetrisclockble.data.model.ble.ConnectionState
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockAlarm
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockAlarmType
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockAnimationSplash
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockDevice
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockHourlyBrightness
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockTime
import net.agolyakov.tetrisclockble.protocol.ClockCommand
import net.agolyakov.tetrisclockble.protocol.NetworkCodec
import net.agolyakov.tetrisclockble.protocol.OtaStatus
import net.agolyakov.tetrisclockble.protocol.TimeZoneMapper
import net.agolyakov.tetrisclockble.protocol.UpdateInfo
import net.agolyakov.tetrisclockble.protocol.WifiNetwork
import net.agolyakov.tetrisclockble.protocol.WifiScan
import net.agolyakov.tetrisclockble.protocol.WifiScanState
import net.agolyakov.tetrisclockble.protocol.WifiSettings
import net.agolyakov.tetrisclockble.protocol.WifiSettingsUpdate
import net.agolyakov.tetrisclockble.protocol.WifiStatus
import net.agolyakov.tetrisclockble.service.bluetooth.TetrisClockBleManager.Companion.MC_COMMAND_CHAR_UUID
import net.agolyakov.tetrisclockble.service.bluetooth.TetrisClockBleManager.Companion.MC_DEVICE_NAME_CHAR_UUID
import net.agolyakov.tetrisclockble.service.bluetooth.TetrisClockBleManager.Companion.MC_OTA_STATUS_CHAR_UUID
import net.agolyakov.tetrisclockble.service.bluetooth.TetrisClockBleManager.Companion.MC_TIME_ZONE_CHAR_UUID
import net.agolyakov.tetrisclockble.service.bluetooth.TetrisClockBleManager.Companion.MC_UPDATE_CHANNEL_CHAR_UUID
import net.agolyakov.tetrisclockble.service.bluetooth.TetrisClockBleManager.Companion.MC_UPDATE_INFO_CHAR_UUID
import net.agolyakov.tetrisclockble.service.bluetooth.TetrisClockBleManager.Companion.MC_WIFI_NETWORKS_CHAR_UUID
import net.agolyakov.tetrisclockble.service.bluetooth.TetrisClockBleManager.Companion.MC_WIFI_SETTINGS_CHAR_UUID
import net.agolyakov.tetrisclockble.service.bluetooth.TetrisClockBleManager.Companion.MC_WIFI_STATUS_CHAR_UUID
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
import no.nordicsemi.android.ble.observer.BondingObserver
import no.nordicsemi.android.ble.observer.ConnectionObserver
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class BluetoothService @Inject constructor(
    private val bluetoothAdapterProvider: BluetoothAdapterProvider
) {
    private val _tag = "BluetoothService"

    private var isOtaUpdateMode = false

    // Set if the link drops while an OTA transfer is in flight. A mid-transfer disconnect means the
    // device rebooted/crashed and is no longer in OTA mode, so continuing to write would just push
    // bytes into a device that ignores them (the connection auto-reconnects). FirmwareRepository
    // polls this to fail the update instead of "succeeding" to 99% against a rebooted clock.
    @Volatile
    var otaConnectionDropped = false
        private set

    fun enterOtaUpdateMode() {
        isOtaUpdateMode = true
        otaConnectionDropped = false
        Log.d("BluetoothService", "Entered OTA update mode - connections will be preserved")
    }

    fun exitOtaUpdateMode() {
        isOtaUpdateMode = false
        Log.d("BluetoothService", "Exited OTA update mode")
    }

    // Android's pairing prompt pauses the activity: dropping the link then would break the pairing,
    // and take the clock's refusal for our own disconnect (the app reconnected over and over)
    fun shouldPreserveConnection(): Boolean {
        return isOtaUpdateMode || bleManager.bondPending ||
            _connectionState.value is ConnectionState.Bonding
    }

    // Firmware Version
    private var _firmwareVersion: String = "Unknown"
    private val _tetrisClockFirmwareVersion = MutableStateFlow(_firmwareVersion)
    val tetrisClockFirmwareVersion: StateFlow<String> = _tetrisClockFirmwareVersion
    private val _versionReadEvent = MutableStateFlow<Unit?>(null)
    private val firmwareVersionReadCharacteristicHandler = VersionReadCharacteristicHandler(
        version = _tetrisClockFirmwareVersion,
        readEvent = _versionReadEvent
    )

    // Time
    private var _bleDeviceTime: TetrisClockTime = TetrisClockTime()
    private var _tetrisClockBleDeviceTime = MutableStateFlow(_bleDeviceTime)
    val tetrisClockBleDeviceTime: StateFlow<TetrisClockTime> = _tetrisClockBleDeviceTime
    private val timeReadCharacteristicHandler = TimeReadCharacteristicHandler(
        _tetrisClockBleDeviceTime
    )

    // ON/OFF
    private var _onOffState: Boolean = true
    private val _tetrisClockIsOn = MutableStateFlow(_onOffState)
    val tetrisClockIsOn: StateFlow<Boolean> = _tetrisClockIsOn
    private val onOffReadCharacteristicHandler = OnOffReadCharacteristicHandler(
        _tetrisClockIsOn
    )

    // Manual Brightness
    private var _manualBrightnessState: Byte = 0
    private val _tetrisClockManualBrightness = MutableStateFlow(_manualBrightnessState)
    val tetrisClockManualBrightness : StateFlow<Byte> = _tetrisClockManualBrightness
    private val manualBrightnessReadCharacteristicHandler =
        ManualBrightnessReadCharacteristicHandler(
            _tetrisClockManualBrightness
        )

    // Is Automatic Brightness Mode
    private var _isAutoBrightness: Boolean = false
    private val _tetrisClockIsAutoBrightness = MutableStateFlow(_isAutoBrightness)
    val tetrisClockIsAutoBrightness: StateFlow<Boolean> = _tetrisClockIsAutoBrightness
    private val autoBrightnessReadCharacteristicHandler = AutoBrightnessReadCharacteristicHandler(
        _tetrisClockIsAutoBrightness
    )

    // LED Matrix Pixel Color Order (false = RRGGBB default, true = RRBBGG)
    private var _isRrbbggColorOrder: Boolean = false
    private val _tetrisClockIsRrbbggColorOrder = MutableStateFlow(_isRrbbggColorOrder)
    val tetrisClockIsRrbbggColorOrder: StateFlow<Boolean> = _tetrisClockIsRrbbggColorOrder
    private val colorOrderReadCharacteristicHandler = PixelColorOrderReadCharacteristicHandler(
        _tetrisClockIsRrbbggColorOrder
    )

    // Day Splash (animated screensaver played over the 00:00 minute at the day boundary)
    private var _animationSplashState: TetrisClockAnimationSplash = TetrisClockAnimationSplash()
    private val _tetrisClockAnimationSplash = MutableStateFlow(_animationSplashState)
    val tetrisClockAnimationSplash: StateFlow<TetrisClockAnimationSplash> = _tetrisClockAnimationSplash
    private val animationSplashReadCharacteristicHandler = AnimationSplashReadCharacteristicHandler(
        _tetrisClockAnimationSplash
    )

    // Hourly Brightness Schedule (used as the auto brightness source)
    private var _hourlyBrightnessState: TetrisClockHourlyBrightness = TetrisClockHourlyBrightness()
    private val _tetrisClockHourlyBrightness = MutableStateFlow(_hourlyBrightnessState)
    val tetrisClockHourlyBrightness: StateFlow<TetrisClockHourlyBrightness> = _tetrisClockHourlyBrightness
    private val hourlyBrightnessReadCharacteristicHandler = HourlyBrightnessReadCharacteristicHandler(
        _tetrisClockHourlyBrightness
    )

    // Turn ON Alarm
    private var _turnOnAlarm: TetrisClockAlarm = TetrisClockAlarm()
    private val _tetrisClockTurnOnAlarm = MutableStateFlow(_turnOnAlarm)
    val tetrisClockTurnOnAlarm: StateFlow<TetrisClockAlarm> = _tetrisClockTurnOnAlarm
    private val turnOnAlarmReadCharacteristicHandler = TurnOnAlarmReadCharacteristicHandler(
        _tetrisClockTurnOnAlarm
    )

    // Turn OFF Alarm
    private var _turnOffAlarm: TetrisClockAlarm = TetrisClockAlarm()
    private val _tetrisClockTurnOffAlarm = MutableStateFlow(_turnOffAlarm)
    val tetrisClockTurnOffAlarm: StateFlow<TetrisClockAlarm> = _tetrisClockTurnOffAlarm
    private val turnOffAlarmReadCharacteristicHandler = TurnOffAlarmReadCharacteristicHandler(
        _tetrisClockTurnOffAlarm
    )

    // RTC Aging Offset
    private var _agingOffsetState: Int = 0
    private val _tetrisClockAgingOffset  = MutableStateFlow(_agingOffsetState)
    val tetrisClockAgingOffset : StateFlow<Int> = _tetrisClockAgingOffset
    private val agingOffsetReadCharacteristicHandler = AgingOffsetReadCharacteristicHandler(
        _tetrisClockAgingOffset
    )

    // RTC Temperature
    private var _rtcTemperatureState: Float = Float.NaN
    private val _tetrisClockRtcTemperature  = MutableStateFlow(_rtcTemperatureState)
    val tetrisClockRtcTemperature : StateFlow<Float> = _tetrisClockRtcTemperature
    private val rtcTemperatureReadCharacteristicHandler = RtcTemperatureReadCharacteristicHandler(
        _tetrisClockRtcTemperature
    )

    // Physical flash chip size in whole megabytes (0 = unknown / not reported by older firmware).
    private val _tetrisClockFlashSizeMb = MutableStateFlow(0)
    val tetrisClockFlashSizeMb: StateFlow<Int> = _tetrisClockFlashSizeMb
    private val flashSizeReadCharacteristicHandler = FlashSizeReadCharacteristicHandler(
        _tetrisClockFlashSizeMb
    )

    // Number of Animation Splash animations this clock's firmware ships (0 = not reported by older
    // firmware, in which case the app shows its whole bundled catalog).
    private val _tetrisClockAnimationCount = MutableStateFlow(0)
    val tetrisClockAnimationCount: StateFlow<Int> = _tetrisClockAnimationCount
    private val animationCountReadCharacteristicHandler = AnimationCountReadCharacteristicHandler(
        _tetrisClockAnimationCount
    )

    // Region: network characteristics (firmware v2.0.0+: time zone, WiFi, update from the internet)
    // Their values are raw bytes in TetrisClockBleManager, decoded here with protocol.NetworkCodec.
    // All of them stay null (and networkSupported false) for a clock with older firmware.
    private val networkScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _networkSupported = MutableStateFlow(false)
    val networkSupported: StateFlow<Boolean> = _networkSupported.asStateFlow()

    private val _timeZone = MutableStateFlow<String?>(null)
    val timeZone: StateFlow<String?> = _timeZone.asStateFlow()

    // The clock's BLE name; null when its firmware cannot be renamed
    private val _deviceName = MutableStateFlow<String?>(null)
    val deviceName: StateFlow<String?> = _deviceName.asStateFlow()

    // Whether the clock is on the beta update channel; null when its firmware has no channels
    private val _betaUpdates = MutableStateFlow<Boolean?>(null)
    val betaUpdates: StateFlow<Boolean?> = _betaUpdates.asStateFlow()

    private val _wifiSettings = MutableStateFlow<WifiSettings?>(null)
    val wifiSettings: StateFlow<WifiSettings?> = _wifiSettings.asStateFlow()

    private val _wifiStatus = MutableStateFlow<WifiStatus?>(null)
    val wifiStatus: StateFlow<WifiStatus?> = _wifiStatus.asStateFlow()

    // All networks of the last scan (every page read), or just the scan state while it runs
    private val _wifiScan = MutableStateFlow<WifiScan?>(null)
    val wifiScan: StateFlow<WifiScan?> = _wifiScan.asStateFlow()

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    // Progress of an installation from the internet (null until the clock reports one)
    private val _otaStatus = MutableStateFlow<OtaStatus?>(null)
    val otaStatus: StateFlow<OtaStatus?> = _otaStatus.asStateFlow()

    // Region: BleManager and Bluetooth internals
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // The clock's settings of this connection are read: the screen shows them only then, not the
    // defaults moving to the read values
    private val _settingsLoaded = MutableStateFlow(false)
    val settingsLoaded: StateFlow<Boolean> = _settingsLoaded.asStateFlow()
    private var readyGeneration = 0

    private val _currentDevice = MutableStateFlow<TetrisClockDevice?>(null)
    val currentDevice: StateFlow<TetrisClockDevice?> = _currentDevice.asStateFlow()

    private var _lastConnectedDevice : TetrisClockDevice? = null

    /**
     * The clock the app is connecting or connected to. Going back to the list and opening another
     * clock right away starts the new connection while the old one is still being torn down; the old
     * clock's late callbacks then must not touch the state (its "disconnected" used to make
     * tryReconnect() connect the old clock again, under the new clock's screen).
     */
    @Volatile private var targetAddress: String? = null

    private fun notTarget(device: BluetoothDevice): Boolean {
        val target = targetAddress ?: return false
        if (device.address.equals(target, ignoreCase = true)) return false
        Log.d(_tag, "Ignoring a callback of ${device.address}, connecting to $target")
        return true
    }

    private val bleManager: TetrisClockBleManager = TetrisClockBleManager(
        bluetoothAdapterProvider.getContext(),
        timeReadCharacteristicHandler,
        onOffReadCharacteristicHandler,
        manualBrightnessReadCharacteristicHandler,
        autoBrightnessReadCharacteristicHandler,
        colorOrderReadCharacteristicHandler,
        animationSplashReadCharacteristicHandler,
        hourlyBrightnessReadCharacteristicHandler,
        turnOnAlarmReadCharacteristicHandler,
        turnOffAlarmReadCharacteristicHandler,
        agingOffsetReadCharacteristicHandler,
        rtcTemperatureReadCharacteristicHandler,
        firmwareVersionReadCharacteristicHandler,
        flashSizeReadCharacteristicHandler,
        animationCountReadCharacteristicHandler)

    private val connectionObserver = object : ConnectionObserver {
        override fun onDeviceConnecting(device: BluetoothDevice) {
            if (notTarget(device)) return
            _settingsLoaded.value = false
            _connectionState.value = ConnectionState.Connecting
        }

        override fun onDeviceConnected(device: BluetoothDevice) {
            if (notTarget(device)) return
            _connectionState.value = ConnectionState.Connected
        }

        override fun onDeviceReady(device: BluetoothDevice) {
            if (notTarget(device)) return
            // A phone pairing with the clock is not in yet: the clock may still refuse it, and
            // until it is bonded every read fails. Ready (and the clock's cards) waits for the bond.
            if (bleManager.bondPending) {
                readyAfterBond = true
                _connectionState.value = ConnectionState.Bonding
                return
            }
            becomeReady()
        }

        override fun onDeviceDisconnecting(device: BluetoothDevice) {
            if (notTarget(device)) return
            _connectionState.value = ConnectionState.Disconnecting
        }

        override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
            Log.e(_tag, "Connection failed with status code, $reason")
            if (notTarget(device)) return
            if (pairingRejected(device, reason)) return
            _connectionState.value = ConnectionState.Error("Failed to connect: $reason")
        }

        override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
            if (notTarget(device)) return
            if (pairingRejected(device, reason)) return
            _connectionState.value = ConnectionState.Disconnected
            // A drop during an OTA transfer means the clock rebooted mid-update; remember it so the
            // transfer fails instead of silently resuming against a device that left OTA mode.
            if (isOtaUpdateMode) {
                otaConnectionDropped = true
                Log.w(_tag, "Link dropped during OTA - the clock likely rebooted mid-update")
            }
            tryReconnect()
        }
    }

    /** onDeviceReady() came while the phone was still bonding: Ready follows the bond. */
    private var readyAfterBond = false

    private fun becomeReady() {
        readyAfterBond = false
        _settingsLoaded.value = false
        val generation = ++readyGeneration
        _connectionState.value = ConnectionState.Ready
        startReadingAllCharacteristics()
        networkScope.launch {
            initNetwork()
            // The reads above are queued before this mark
            bleManager.awaitQueue()
            if (generation == readyGeneration) {
                Log.i(_tag, "Settings of the clock read")
                _settingsLoaded.value = true
            }
        }
    }

    /** The phone had no bond when this connection attempt started, or bonding started during it. */
    private var bondedInThisAttempt = false

    private val bondingObserver = object : BondingObserver {
        override fun onBondingRequired(device: BluetoothDevice) {
            if (notTarget(device)) return
            bondedInThisAttempt = true
            _connectionState.value = ConnectionState.Bonding
        }

        override fun onBonded(device: BluetoothDevice) {
            Log.i(_tag, "Bonded")
        }

        override fun onBondingFailed(device: BluetoothDevice) {
            if (notTarget(device)) return
            Log.w(_tag, "Bonding failed")
            if (bleManager.clockRequiresBond) {
                _connectionState.value = ConnectionState.PairingRejected(staleBondRemoved = false)
            }
        }
    }

    /**
     * After a lost connection: whether it was the clock (firmware v2.1.0+) not letting this phone in,
     * in which case the state becomes PairingRejected (no automatic reconnect: the user has to open
     * the pairing window on the clock first). Ported from the FireLamp app.
     */
    private fun pairingRejected(device: BluetoothDevice, reason: Int): Boolean {
        val state = _connectionState.value
        if (state is ConnectionState.PairingRejected) return true
        val inProgress = state is ConnectionState.Connecting || state is ConnectionState.Connected ||
            state is ConnectionState.Bonding
        if (!inProgress || !bleManager.clockRequiresBond) return false
        when {
            bondedInThisAttempt -> {
                // A fresh bond and the clock hung up before we were in: it was outside the pairing
                // window and the clock dropped the keys. The phone's half of the bond is useless.
                val removed = removeBond(device)
                Log.w(_tag, "Pairing rejected by the clock (stale bond removed: $removed)")
                _connectionState.value = ConnectionState.PairingRejected(staleBondRemoved = removed)
            }
            reason != ConnectionObserver.REASON_LINK_LOSS && reason != ConnectionObserver.REASON_TIMEOUT &&
                device.bondState == BluetoothDevice.BOND_BONDED -> {
                // Bonded, but the clock does not let us in: it forgot this phone (a 6th phone took
                // its place). Removing the bond is left to the user, it may also be a fluke.
                Log.w(_tag, "The clock does not accept this phone's bond")
                _connectionState.value = ConnectionState.PairingRejected(staleBondRemoved = false)
            }
            else -> return false
        }
        _currentDevice.value = null
        return true
    }

    private fun removeBond(device: BluetoothDevice): Boolean = try {
        device.javaClass.getMethod("removeBond").invoke(device) as Boolean
    } catch (e: Exception) {
        Log.w(_tag, "removeBond failed", e)
        false
    }

    /** The clock forgot this phone: drop the phone's half of the bond (asynchronous in Android). */
    fun forgetBond(macAddress: String) {
        removeBond(bluetoothAdapterProvider.getAdapter().getRemoteDevice(macAddress))
    }

    init {
        bleManager.connectionObserver = connectionObserver
        bleManager.setBondingObserver(bondingObserver)
        bleManager.onBondDone = {
            if (readyAfterBond && _connectionState.value is ConnectionState.Bonding) becomeReady()
        }
        bleManager.onNetworkNotification = ::onNetworkNotification
    }

    // Resets all cached characteristic values back to their defaults.
    // Without this, a value read from a previously connected device (e.g. its
    // firmware version) would keep showing on screen for a device that doesn't
    // support that characteristic, since a failed/unsupported read never overwrites it.
    private fun resetDeviceState() {
        _firmwareVersion = "Unknown"
        _tetrisClockFirmwareVersion.value = _firmwareVersion

        _bleDeviceTime = TetrisClockTime()
        _tetrisClockBleDeviceTime.value = _bleDeviceTime

        _onOffState = true
        _tetrisClockIsOn.value = _onOffState

        _manualBrightnessState = 0
        _tetrisClockManualBrightness.value = _manualBrightnessState

        _isAutoBrightness = false
        _tetrisClockIsAutoBrightness.value = _isAutoBrightness

        _isRrbbggColorOrder = false
        _tetrisClockIsRrbbggColorOrder.value = _isRrbbggColorOrder

        _animationSplashState = TetrisClockAnimationSplash()
        _tetrisClockAnimationSplash.value = _animationSplashState

        _turnOnAlarm = TetrisClockAlarm()
        _tetrisClockTurnOnAlarm.value = _turnOnAlarm

        _turnOffAlarm = TetrisClockAlarm()
        _tetrisClockTurnOffAlarm.value = _turnOffAlarm

        _agingOffsetState = 0
        _tetrisClockAgingOffset.value = _agingOffsetState

        _rtcTemperatureState = Float.NaN
        _tetrisClockRtcTemperature.value = _rtcTemperatureState

        _tetrisClockFlashSizeMb.value = 0
        _tetrisClockAnimationCount.value = 0

        _networkSupported.value = false
        _timeZone.value = null
        _deviceName.value = null
        _betaUpdates.value = null
        _wifiSettings.value = null
        _wifiStatus.value = null
        _wifiScan.value = null
        _updateInfo.value = null
        _otaStatus.value = null
    }

    fun connect(tetrisClockDevice: TetrisClockDevice, useAutoConnect: Boolean = false) {
        // Ready belongs here just as much as Connected: it is the state of a *fully* established
        // link (onDeviceReady), which is exactly what we are in when the device screen is entered
        // again - e.g. coming back from the firmware update screen, whose LaunchedEffect asks to
        // connect once more. Letting that through breaks the working connection: the BLE library
        // completes a connect request to an already-connected device straight away, without
        // emitting any ConnectionObserver callback, so nothing would move the state on from the
        // Connecting we set below and nothing would re-read the characteristics we just cleared -
        // the screen stays empty and "connecting" forever over a link that is actually alive.
        if (_connectionState.value is ConnectionState.Connecting ||
            _connectionState.value is ConnectionState.Connected ||
            _connectionState.value is ConnectionState.Ready) {
            Log.w(_tag, "Already connecting/connected, ignoring new connection request")
            return
        }

        resetDeviceState()

        targetAddress = tetrisClockDevice.macAddress
        readyAfterBond = false
        // From now on a reconnect means this clock, also before the first connection succeeds
        _lastConnectedDevice = tetrisClockDevice
        _connectionState.value = ConnectionState.Connecting
        val device = bluetoothAdapterProvider.getAdapter().getRemoteDevice(tetrisClockDevice.macAddress)
        bondedInThisAttempt = device.bondState != BluetoothDevice.BOND_BONDED
        bleManager.connect(device)
            .retry(2, 100)
            .useAutoConnect(useAutoConnect)
            .done {
                _currentDevice.value = tetrisClockDevice
                _lastConnectedDevice = tetrisClockDevice
                Log.i(_tag, "Connection success!")
            }
            .fail { _, status ->
                Log.e(_tag, "Connection failed, $status")
                _connectionState.value = ConnectionState.Error("Connection failed with status code: $status")
            }
            .enqueue()
    }

    fun disconnect() {
        if (_connectionState.value is ConnectionState.Disconnecting ||
            _connectionState.value is ConnectionState.Disconnected) {
            Log.w(_tag, "Already disconnecting/disconnected, ignoring")
            return
        }

        _connectionState.value = ConnectionState.Disconnecting

        // Queued before the disconnect: the clock must not see a link Android keeps as subscribed
        bleManager.disableAllNotifications()
        bleManager.disconnect()
            .done {
                _currentDevice.value = null
            }
            .fail { _, status ->
                Log.e(_tag, "Disconnection failed with status code: $status")
                _currentDevice.value = null
            }
            .enqueue()
        _currentDevice.value = null
    }

    private var _shouldMaintainConnection = MutableStateFlow(false)

    fun setShouldMaintainConnection(shouldMaintain: Boolean) {
        _shouldMaintainConnection.value = shouldMaintain
        if (!shouldMaintain) {
            disconnect()
        }
    }

    fun tryReconnect() {
        _lastConnectedDevice?.let {
            // Error counts as "not connected" too: a connect attempt that failed used to leave the
            // state there for good, so nothing - not even bringing the app back to the foreground -
            // ever tried again.
            if (_shouldMaintainConnection.value &&
                (_connectionState.value is ConnectionState.Disconnected ||
                 _connectionState.value is ConnectionState.Error))
            {
                Log.i(_tag, "Reconnecting to last device: ${it.deviceName}")
                // Auto-connect: the device may be unreachable right now (e.g. rebooting), so let
                // Android wait and connect as soon as it starts advertising again, rather than
                // failing immediately like the initial user-initiated connect does.
                connect(it, useAutoConnect = true)
            }
        }
    }

    private fun startReadingAllCharacteristics() {
        bleManager.getTimeCharacteristic()
        bleManager.getOnOffCharacteristic()
        bleManager.getManualBrightnessCharacteristic()
        bleManager.getAutoBrightnessCharacteristic()
        bleManager.getColorOrderCharacteristic()
        bleManager.getAnimationSplashCharacteristic()
        bleManager.getHourlyBrightnessCharacteristic()
        bleManager.getTurnOnAlarmCharacteristic()
        bleManager.getTurnOffAlarmCharacteristic()
        bleManager.getAgingOffsetCharacteristic()
        bleManager.getRtcTemperatureCharacteristic()
        bleManager.getVersionCharacteristic()
        bleManager.getFlashSizeCharacteristic()
        bleManager.getAnimationCountCharacteristic()
    }

    // Region: BLE GATT characteristic setters

    fun setTimeCharacteristic(time: LocalDateTime) {
        _bleDeviceTime = TetrisClockTime(time)
        _tetrisClockBleDeviceTime.value = _bleDeviceTime

        if (bleManager.isReady)
        {
            bleManager.setTimeCharacteristic(_bleDeviceTime)
        }
    }

    fun toggleOnOffCharacteristic() {
        val on = !_tetrisClockIsOn.value

        _onOffState = on
        _tetrisClockIsOn.value = on

        if (bleManager.isReady) {
            bleManager.setOnOffCharacteristic(on)
        }
    }

    fun setManualBrightnessCharacteristic(brightness: Byte) {
        _manualBrightnessState = brightness
        _tetrisClockManualBrightness.value = brightness

        if (bleManager.isReady)
        {
            bleManager.setManualBrightnessCharacteristic(brightness)
        }
    }

    fun setAgingOffsetCharacteristic(agingOffset: Int) {
        _agingOffsetState = agingOffset
        _tetrisClockAgingOffset.value = agingOffset

        if (bleManager.isReady) {
            bleManager.setAgingOffsetCharacteristic(agingOffset)
        }
    }

    fun toggleAutoBrightnessCharacteristic() {
        val isAuto = !_tetrisClockIsAutoBrightness.value

        _isAutoBrightness = isAuto
        _tetrisClockIsAutoBrightness.value = isAuto

        if (bleManager.isReady) {
            bleManager.setAutoBrightnessCharacteristic(isAuto)
        }
    }

    fun setColorOrderCharacteristic(useRrbbgg: Boolean) {
        _isRrbbggColorOrder = useRrbbgg
        _tetrisClockIsRrbbggColorOrder.value = useRrbbgg

        if (bleManager.isReady) {
            bleManager.setColorOrderCharacteristic(useRrbbgg)
        }
    }

    fun setAnimationSplashCharacteristic(mode: Int, duration: Int, animationIndex: Int) {
        _animationSplashState = TetrisClockAnimationSplash(mode, duration, animationIndex)
        _tetrisClockAnimationSplash.value = _animationSplashState

        if (bleManager.isReady) {
            bleManager.setAnimationSplashCharacteristic(mode, duration, animationIndex, previewNow = false)
        }
    }

    // Fires a one-off preview of the currently selected animation right now, without changing the
    // stored config (the device plays it for the configured duration, regardless of time and mode).
    fun previewAnimationSplash() {
        val current = _tetrisClockAnimationSplash.value
        if (bleManager.isReady) {
            bleManager.setAnimationSplashCharacteristic(
                current.mode, current.durationValue, current.animationIndex, previewNow = true
            )
        }
    }

    fun setHourlyBrightnessCharacteristic(hourlyBrightness: TetrisClockHourlyBrightness) {
        _hourlyBrightnessState = hourlyBrightness
        _tetrisClockHourlyBrightness.value = hourlyBrightness

        if (bleManager.isReady) {
            bleManager.setHourlyBrightnessCharacteristic(hourlyBrightness)
        }
    }

    fun setTurnOnAlarmCharacteristic(isActive: Boolean, hours: Byte, minutes: Byte) {
        _turnOnAlarm = TetrisClockAlarm(isActive, hours, minutes)
        _tetrisClockTurnOnAlarm.value = _turnOnAlarm

        if (bleManager.isReady)
        {
            bleManager.setTurnOnAlarmCharacteristic(_turnOnAlarm)
        }
    }

    fun setTurnOffAlarmCharacteristic(isActive: Boolean, hours: Byte, minutes: Byte) {
        _turnOffAlarm = TetrisClockAlarm(isActive, hours, minutes)
        _tetrisClockTurnOffAlarm.value = _turnOffAlarm

        if (bleManager.isReady)
        {
            bleManager.setTurnOffAlarmCharacteristic(_turnOffAlarm)
        }
    }

    fun setAlarmTime(alarmType: TetrisClockAlarmType, hour: Int, minute: Int, isActive: Boolean) {
        when (alarmType) {
            TetrisClockAlarmType.TURN_ON -> {
                setTurnOnAlarmCharacteristic(
                    isActive,
                    hour.toByte(),
                    minute.toByte())
            }
            TetrisClockAlarmType.TURN_OFF -> {
                setTurnOffAlarmCharacteristic(isActive,
                    hour.toByte(),
                    minute.toByte())
            }
        }
    }

    fun syncBleWithPhone() {
        val mcNow = TetrisClockTime.Companion.now()
        bleManager.setTimeCharacteristic(mcNow)
        _bleDeviceTime = mcNow
        _tetrisClockBleDeviceTime.value = mcNow
    }

    fun toggleAlarmActive(alarmType: TetrisClockAlarmType) {
        when (alarmType) {
            TetrisClockAlarmType.TURN_ON -> {
                val current = _tetrisClockTurnOnAlarm.value
                val newAlarm = current.copy(isActive = !current.isActive)
                _tetrisClockTurnOnAlarm.value = newAlarm
                bleManager.setTurnOnAlarmCharacteristic(newAlarm)
            }
            TetrisClockAlarmType.TURN_OFF -> {
                val current = _tetrisClockTurnOffAlarm.value
                val newAlarm = current.copy(isActive = !current.isActive)
                _tetrisClockTurnOffAlarm.value = newAlarm
                bleManager.setTurnOffAlarmCharacteristic(newAlarm)
            }
        }
    }

    // Region: network (firmware v2.0.0+)

    // Reads the network characteristics once the link is ready, and gives the clock the phone's
    // time zone: the clock converts the UTC time it gets from the internet into local time with it.
    private suspend fun initNetwork() {
        val supported = bleManager.hasNetworkCharacteristics()
        _networkSupported.value = supported
        if (!supported) return
        if (bleManager.hasNetworkCharacteristic(MC_DEVICE_NAME_CHAR_UUID)) {
            try {
                _deviceName.value = NetworkCodec.decodeDeviceName(bleManager.readNetwork(MC_DEVICE_NAME_CHAR_UUID))
            } catch (e: Exception) {
                Log.w(_tag, "Reading the device name failed", e)
            }
        }
        if (bleManager.hasNetworkCharacteristic(MC_UPDATE_CHANNEL_CHAR_UUID)) {
            try {
                _betaUpdates.value = NetworkCodec.decodeBetaUpdates(bleManager.readNetwork(MC_UPDATE_CHANNEL_CHAR_UUID))
            } catch (e: Exception) {
                Log.w(_tag, "Reading the update channel failed", e)
            }
        }
        try {
            _wifiSettings.value = NetworkCodec.decodeWifiSettings(bleManager.readNetwork(MC_WIFI_SETTINGS_CHAR_UUID))
            _wifiStatus.value = NetworkCodec.decodeWifiStatus(bleManager.readNetwork(MC_WIFI_STATUS_CHAR_UUID))
            _updateInfo.value = NetworkCodec.decodeUpdateInfo(bleManager.readNetwork(MC_UPDATE_INFO_CHAR_UUID))
            _otaStatus.value = NetworkCodec.decodeOtaStatus(bleManager.readNetwork(MC_OTA_STATUS_CHAR_UUID))

            val clockZone = NetworkCodec.decodeTimeZone(bleManager.readNetwork(MC_TIME_ZONE_CHAR_UUID))
            val phoneZone = TimeZoneMapper.toPosix(ZoneId.systemDefault())
            if (clockZone != phoneZone) {
                Log.i(_tag, "Time zone of the clock '$clockZone' -> '$phoneZone'")
                bleManager.writeNetwork(MC_TIME_ZONE_CHAR_UUID, NetworkCodec.encodeTimeZone(phoneZone))
            }
            _timeZone.value = phoneZone
        } catch (e: Exception) {
            Log.w(_tag, "Reading the network characteristics failed", e)
        }
    }

    private fun onNetworkNotification(uuid: UUID, value: ByteArray) {
        try {
            when (uuid) {
                MC_WIFI_STATUS_CHAR_UUID -> _wifiStatus.value = NetworkCodec.decodeWifiStatus(value)
                MC_UPDATE_INFO_CHAR_UUID -> _updateInfo.value = NetworkCodec.decodeUpdateInfo(value)
                MC_OTA_STATUS_CHAR_UUID -> _otaStatus.value = NetworkCodec.decodeOtaStatus(value)
                MC_WIFI_NETWORKS_CHAR_UUID -> {
                    // A scan start / end: the header only, the list is read page by page
                    val header = NetworkCodec.decodeWifiScan(value)
                    if (header.state == WifiScanState.DONE) {
                        networkScope.launch { readWifiNetworks() }
                    } else {
                        _wifiScan.value = header.copy(networks = emptyList())
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(_tag, "Bad notification of $uuid", e)
        }
    }

    // Reads every page of the scan result (the clock resets its page cursor after each scan)
    private suspend fun readWifiNetworks() {
        try {
            val networks = mutableListOf<WifiNetwork>()
            var page = NetworkCodec.decodeWifiScan(bleManager.readNetwork(MC_WIFI_NETWORKS_CHAR_UUID))
            networks += page.networks
            while (page.state == WifiScanState.DONE && page.networks.isNotEmpty() && page.first + page.networks.size < page.total) {
                bleManager.writeNetwork(MC_WIFI_NETWORKS_CHAR_UUID, NetworkCodec.encodeWifiScanCursor(page.first + page.networks.size))
                page = NetworkCodec.decodeWifiScan(bleManager.readNetwork(MC_WIFI_NETWORKS_CHAR_UUID))
                networks += page.networks
            }
            _wifiScan.value = WifiScan(page.state, page.total, 0, networks)
        } catch (e: Exception) {
            Log.w(_tag, "Reading the WiFi networks failed", e)
            _wifiScan.value = WifiScan(WifiScanState.ERROR, 0, 0, emptyList())
        }
    }

    /**
     * Renames the clock (an empty name restores its default) and reads the name back. The clock
     * advertises the new name after this connection ends.
     *
     * @return false if the clock could not be written to (link lost, invalid name).
     */
    suspend fun saveDeviceName(name: String): Boolean = try {
        bleManager.writeNetwork(MC_DEVICE_NAME_CHAR_UUID, NetworkCodec.encodeDeviceName(name.trim()))
        _deviceName.value = NetworkCodec.decodeDeviceName(bleManager.readNetwork(MC_DEVICE_NAME_CHAR_UUID))
        true
    } catch (e: Exception) {
        Log.w(_tag, "Saving the device name failed", e)
        false
    }

    /**
     * Saves the WiFi settings (the phone bonds with the clock first, see
     * TetrisClockBleManager.writeWifiSettings) and reads them back.
     *
     * @return false if the clock could not be written to (not paired, link lost, invalid values).
     */
    /**
     * Puts the clock on the beta update channel or back on releases only, and reads it back. Both
     * the clock's own update check and the phone's one (for the BLE update) follow it.
     *
     * @return false if the clock could not be written to.
     */
    suspend fun saveBetaUpdates(beta: Boolean): Boolean = try {
        bleManager.writeNetwork(MC_UPDATE_CHANNEL_CHAR_UUID, NetworkCodec.encodeBetaUpdates(beta))
        _betaUpdates.value = NetworkCodec.decodeBetaUpdates(bleManager.readNetwork(MC_UPDATE_CHANNEL_CHAR_UUID))
        true
    } catch (e: Exception) {
        Log.w(_tag, "Saving the update channel failed", e)
        false
    }

    suspend fun saveWifiSettings(update: WifiSettingsUpdate): Boolean = try {
        bleManager.writeWifiSettings(NetworkCodec.encodeWifiSettings(update))
        _wifiSettings.value = NetworkCodec.decodeWifiSettings(bleManager.readNetwork(MC_WIFI_SETTINGS_CHAR_UUID))
        true
    } catch (e: Exception) {
        Log.w(_tag, "Saving the WiFi settings failed", e)
        false
    }

    /** Sends a command to the clock; the results come as notifications (status, scan, update info, OTA). */
    suspend fun sendClockCommand(command: ClockCommand): Boolean = try {
        if (command == ClockCommand.SCAN_WIFI) {
            _wifiScan.value = WifiScan(WifiScanState.SCANNING, 0, 0, emptyList())
        }
        if (command == ClockCommand.INSTALL_UPDATE) {
            _otaStatus.value = null
        }
        bleManager.writeNetwork(MC_COMMAND_CHAR_UUID, NetworkCodec.encodeCommand(command))
        true
    } catch (e: Exception) {
        Log.w(_tag, "Command $command failed", e)
        if (command == ClockCommand.SCAN_WIFI) {
            _wifiScan.value = WifiScan(WifiScanState.ERROR, 0, 0, emptyList())
        }
        false
    }

    // Region: OTA Update

    suspend fun getCurrentVersion(): String {
        return withTimeout(5000L) {
            _versionReadEvent.value = null
            bleManager.getVersionCharacteristic()

            _versionReadEvent
                .filterNotNull()
                .first()

            _tetrisClockFirmwareVersion.value
        }
    }

    suspend fun setOtaControlCharacteristic(command: ByteArray): Boolean {
        return suspendCancellableCoroutine { continuation ->
            bleManager.setOtaControlCharacteristic(command) { success ->
                if (success) {
                    continuation.resume(true)
                } else {
                    continuation.resume(false)
                }
            }
        }
    }

    suspend fun setOtaDataCharacteristic(data: ByteArray): Boolean {
        return suspendCancellableCoroutine { continuation ->
            bleManager.setOtaDataCharacteristic(data) { success ->
                if (success) {
                    continuation.resume(true)
                } else {
                    continuation.resume(false)
                }
            }
        }
    }

    suspend fun getNegotiatedMtu(): Int = bleManager.awaitNegotiatedMtu()

    // Bracket an OTA transfer with these: HIGH shortens the connection interval (faster, fewer
    // write timeouts), BALANCED restores the power-friendly default afterwards.
    fun requestHighConnectionPriority() = bleManager.requestHighConnectionPriority()
    fun requestBalancedConnectionPriority() = bleManager.requestBalancedConnectionPriority()
}