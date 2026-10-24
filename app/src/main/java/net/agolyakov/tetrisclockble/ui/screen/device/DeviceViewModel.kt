package net.agolyakov.tetrisclockble.ui.screen.device

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import net.agolyakov.tetrisclockble.R
import net.agolyakov.tetrisclockble.data.model.ble.ConnectionState
import net.agolyakov.tetrisclockble.data.repository.FirmwareRepository
import net.agolyakov.tetrisclockble.protocol.ClockCommand
import net.agolyakov.tetrisclockble.protocol.OtaError
import net.agolyakov.tetrisclockble.protocol.OtaState
import net.agolyakov.tetrisclockble.protocol.WifiMode
import net.agolyakov.tetrisclockble.protocol.WifiSettingsUpdate
import net.agolyakov.tetrisclockble.data.model.ble.SplashAnimations
import net.agolyakov.tetrisclockble.data.model.ble.SplashMode
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockAlarmType
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockDevice
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockAlarm
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockHourlyBrightness
import net.agolyakov.tetrisclockble.ui.component.TimePickerDialogState
import net.agolyakov.tetrisclockble.data.local.TetrisClockPreferences
import net.agolyakov.tetrisclockble.service.bluetooth.BluetoothService
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val preferences: TetrisClockPreferences,
    val bluetoothService: BluetoothService,
    private val firmwareRepository: FirmwareRepository,
    savedStateHandle: SavedStateHandle
): ViewModel(), NetworkActions {
    // Device and its FriendlyName
    private val _device = MutableStateFlow<TetrisClockDevice?>(null)
    val device: StateFlow<TetrisClockDevice?> = _device.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _editName = MutableStateFlow("")
    val editName: StateFlow<String> = _editName.asStateFlow()

    fun setDevice(device: TetrisClockDevice) {
//        val deviceWithFriendlyName = preferences.loadFriendlyNameToDevice(device)
//        _device.value = deviceWithFriendlyName
//        _editName.value = deviceWithFriendlyName.friendlyName ?: deviceWithFriendlyName.deviceName
    }

    fun startEditing() {
        _isEditing.value = true
    }

    fun updateEditName(name: String) {
        _editName.value = name
    }

    fun saveFriendlyName() {
        val device = _device.value ?: return
        preferences.saveFriendlyName(device.macAddress, _editName.value)
        _device.value = device.copy(friendlyName = _editName.value)
        _isEditing.value = false
    }

    fun cancelEditing() {
        val device = _device.value ?: return
        _editName.value = device.friendlyName ?: device.deviceName
        _isEditing.value = false
    }

    val tetrisClockFirmwareVersion = bluetoothService.tetrisClockFirmwareVersion
    var tetrisClockBleDeviceTime = bluetoothService.tetrisClockBleDeviceTime
    val tetrisClockTetrisOn = bluetoothService.tetrisClockIsOn
    val tetrisClockManualBrightness = bluetoothService.tetrisClockManualBrightness
    val tetrisClockIsAutoBrightness = bluetoothService.tetrisClockIsAutoBrightness
    val tetrisClockIsRrbbggColorOrder = bluetoothService.tetrisClockIsRrbbggColorOrder
    val tetrisClockAnimationSplash = bluetoothService.tetrisClockAnimationSplash
    val splashAnimations = SplashAnimations.all
    val tetrisClockHourlyBrightness = bluetoothService.tetrisClockHourlyBrightness
    val tetrisClockTurnOnAlarm = bluetoothService.tetrisClockTurnOnAlarm
    var tetrisClockTurnOffAlarm = bluetoothService.tetrisClockTurnOffAlarm
    val tetrisClockAgingOffset = bluetoothService.tetrisClockAgingOffset
    val tetrisClockRtcTemperature  =  bluetoothService.tetrisClockRtcTemperature
    val tetrisClockFlashSizeMb = bluetoothService.tetrisClockFlashSizeMb
    val tetrisClockAnimationCount = bluetoothService.tetrisClockAnimationCount

    private val _manualBrightnessState = MutableStateFlow<Byte>(0)
    private val _debouncedBrightness = MutableSharedFlow<Byte>(extraBufferCapacity = 1)


    // Alarm Dialog
    private val _timePickerState = MutableStateFlow(TimePickerDialogState())
    val timePickerState: StateFlow<TimePickerDialogState> = _timePickerState

    fun showTimePickerDialog(alarmType: TetrisClockAlarmType, alarm: TetrisClockAlarm) {
        _timePickerState.value = TimePickerDialogState(
            isVisible = true,
            alarmType = alarmType,
            hour = alarm.hours.toInt(),
            minute = alarm.minutes.toInt(),
            isActive = alarm.isActive
        )
    }

    fun hideTimePickerDialog() {
        _timePickerState.value = TimePickerDialogState(isVisible = false)
    }


    // Debounce Brightness
    private fun setupDebouncedBrightness() {
        viewModelScope.launch {
            _debouncedBrightness
                .debounce(300) // 300ms debounce
                .collect { value ->
                    bluetoothService.setManualBrightnessCharacteristic(value)
                    _manualBrightnessState.value = value
                }
        }
    }

    // Common

    val connectionState = bluetoothService.connectionState
    val currentDevice = bluetoothService.currentDevice

    init {
        val deviceFromNav = savedStateHandle.get<TetrisClockDevice>("device")
        deviceFromNav?.let {
            setDevice(it)
            bluetoothService.connect(it)
            setupDebouncedBrightness()
        }
    }

    // ---------- Network: WiFi, time zone, firmware update (firmware v2.0.0+) ----------

    private val _wifiSaveFailed = MutableStateFlow(false)
    private val _wifiSaving = MutableStateFlow(false)
    private val _deviceNameSaveFailed = MutableStateFlow(false)
    private val _firmwareCheck = MutableStateFlow<FirmwareCheck>(FirmwareCheck.Idle)
    private val _wifiUpdate = MutableStateFlow<WifiUpdate?>(null)

    val networkUiState: StateFlow<NetworkUiState> = combine(
        combine(
            bluetoothService.networkSupported,
            bluetoothService.timeZone,
            bluetoothService.wifiSettings,
            bluetoothService.wifiStatus,
            bluetoothService.wifiScan,
        ) { supported, timeZone, settings, status, scan ->
            NetworkUiState(supported, timeZone, settings, status, scan)
        },
        combine(_wifiSaveFailed, _wifiSaving, _deviceNameSaveFailed, ::Triple),
        bluetoothService.updateInfo,
        _firmwareCheck,
        combine(_wifiUpdate, bluetoothService.deviceName, bluetoothService.betaUpdates, ::Triple),
    ) { state, (saveFailed, saving, nameSaveFailed), updateInfo, firmwareCheck, (wifiUpdate, deviceName, betaUpdates) ->
        state.copy(
            wifiSaveFailed = saveFailed,
            wifiSaving = saving,
            updateInfo = updateInfo,
            firmwareCheck = firmwareCheck,
            wifiUpdate = wifiUpdate,
            deviceName = deviceName,
            deviceNameSaveFailed = nameSaveFailed,
            betaUpdates = betaUpdates
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NetworkUiState())

    // After the properties above: the coroutine may run checkPhoneFirmware() right away
    init {
        // The phone checks for a firmware update once the clock's version is known
        viewModelScope.launch {
            tetrisClockFirmwareVersion.first { it != UNKNOWN_VERSION }
            checkPhoneFirmware()
        }
    }

    override fun saveWifiSettings(update: WifiSettingsUpdate) {
        if (_wifiSaving.value) return
        viewModelScope.launch {
            _wifiSaveFailed.value = false
            _wifiSaving.value = true
            try {
                _wifiSaveFailed.value = !bluetoothService.saveWifiSettings(update)
            } finally {
                _wifiSaving.value = false
            }
        }
    }

    override fun saveDeviceName(name: String) {
        viewModelScope.launch {
            _deviceNameSaveFailed.value = false
            _deviceNameSaveFailed.value = !bluetoothService.saveDeviceName(name)
        }
    }

    override fun scanWifi() {
        viewModelScope.launch { bluetoothService.sendClockCommand(ClockCommand.SCAN_WIFI) }
    }

    override fun syncTimeFromInternet() {
        viewModelScope.launch { bluetoothService.sendClockCommand(ClockCommand.SYNC_TIME) }
    }

    /**
     * The Check button: the phone checks the GitHub releases, and a clock with WiFi checks its own
     * manifest too (the clock knows its build, and a debug build reads the test channel).
     */
    // The channel changes what both the clock and the phone offer: check again right away
    override fun setBetaUpdates(beta: Boolean) {
        viewModelScope.launch {
            if (bluetoothService.saveBetaUpdates(beta)) checkFirmware()
        }
    }

    override fun checkFirmware() {
        val settings = bluetoothService.wifiSettings.value
        if (bluetoothService.networkSupported.value && settings != null &&
            settings.mode != WifiMode.OFF && settings.ssid.isNotEmpty()
        ) {
            viewModelScope.launch { bluetoothService.sendClockCommand(ClockCommand.CHECK_UPDATE) }
        }
        checkPhoneFirmware()
    }

    /** The phone checks the latest GitHub release against the clock's version. */
    private fun checkPhoneFirmware() {
        if (_firmwareCheck.value == FirmwareCheck.Checking) return
        viewModelScope.launch {
            _firmwareCheck.value = FirmwareCheck.Checking
            _firmwareCheck.value = try {
                when (val state = firmwareRepository.checkForUpdates()) {
                    is FirmwareRepository.UpdateState.UpdateAvailable -> FirmwareCheck.Available(state.release.tagName)
                    is FirmwareRepository.UpdateState.NoUpdate -> FirmwareCheck.UpToDate
                    else -> FirmwareCheck.Failed
                }
            } catch (e: Exception) {
                Log.w("DeviceViewModel", "Firmware update check failed", e)
                FirmwareCheck.Failed
            }
        }
    }

    // The clock downloads and installs the update by itself (Command 0x03); the phone follows the
    // OTA status notifications, then waits for the clock to come back and compares the version
    override fun installFirmwareByWifi() {
        if (_wifiUpdate.value?.finished == false) return
        viewModelScope.launch {
            val oldVersion = tetrisClockFirmwareVersion.value
            _wifiUpdate.value = WifiUpdate.Checking
            if (!bluetoothService.sendClockCommand(ClockCommand.INSTALL_UPDATE)) {
                _wifiUpdate.value = WifiUpdate.Failed(R.string.mc_fw_error_not_sent)
                return@launch
            }

            val last = withTimeoutOrNull(WIFI_UPDATE_TIMEOUT_MS) {
                bluetoothService.otaStatus
                    .filterNotNull()
                    .first { status ->
                        when (status.state) {
                            OtaState.CHECKING -> _wifiUpdate.value = WifiUpdate.Checking
                            OtaState.RECEIVING -> _wifiUpdate.value = WifiUpdate.Downloading(status.received, status.total)
                            else -> Unit
                        }
                        status.state == OtaState.SUCCESS || status.state == OtaState.ERROR || status.state == OtaState.NO_UPDATE
                    }
            }
            when (last?.state) {
                OtaState.SUCCESS -> Unit
                OtaState.NO_UPDATE -> { _wifiUpdate.value = WifiUpdate.Failed(R.string.mc_fw_error_no_update); return@launch }
                OtaState.ERROR -> { _wifiUpdate.value = WifiUpdate.Failed(otaErrorMessage(last.error)); return@launch }
                // The link may be gone mid-download: the clock goes on, the result is unknown
                else -> { _wifiUpdate.value = WifiUpdate.Failed(R.string.mc_fw_error_not_verified); return@launch }
            }

            _wifiUpdate.value = WifiUpdate.Rebooting
            val newVersion = withTimeoutOrNull(WIFI_UPDATE_REBOOT_TIMEOUT_MS) {
                connectionState.first { it !is ConnectionState.Ready }
                connectionState.first { it is ConnectionState.Ready }
                bluetoothService.getCurrentVersion()
            }
            _wifiUpdate.value = when (newVersion) {
                null, UNKNOWN_VERSION -> WifiUpdate.Failed(R.string.mc_fw_error_not_verified)
                oldVersion -> WifiUpdate.RolledBack(oldVersion)
                else -> WifiUpdate.Done(newVersion)
            }
            if (newVersion != null && newVersion != oldVersion) checkPhoneFirmware()
        }
    }

    override fun dismissWifiUpdate() {
        _wifiUpdate.value = null
    }

    private fun otaErrorMessage(error: OtaError): Int = when (error) {
        OtaError.BUSY -> R.string.mc_fw_error_busy
        OtaError.FLASH_WRITE -> R.string.mc_fw_error_flash
        OtaError.SHA256_MISMATCH -> R.string.mc_fw_error_sha
        else -> R.string.mc_fw_error_download
    }

    /** The clock forgot this phone: drop the phone's half of the bond and pair anew. */
    fun forgetBondAndReconnect(device: TetrisClockDevice?) {
        device ?: return
        bluetoothService.forgetBond(device.macAddress)
        viewModelScope.launch {
            // Removing a bond is asynchronous in Android
            kotlinx.coroutines.delay(BOND_REMOVAL_DELAY_MS)
            bluetoothService.connect(device)
        }
    }

    companion object {
        private const val BOND_REMOVAL_DELAY_MS = 1000L
        private const val UNKNOWN_VERSION = "Unknown"
        private const val WIFI_UPDATE_TIMEOUT_MS = 10 * 60 * 1000L
        private const val WIFI_UPDATE_REBOOT_TIMEOUT_MS = 90 * 1000L
    }

    fun connectToDevice(device: TetrisClockDevice?)
    {
        device?.let {
            bluetoothService.connect(it)
            bluetoothService.setShouldMaintainConnection(true)
        }
    }

    fun setAlarmTime(alarmType: TetrisClockAlarmType, hour: Int, minute: Int, isActive: Boolean) {
        Log.d("DeviceViewModel", "setAlarmTime: active=$isActive $hour:$minute for ${alarmType}")
        bluetoothService.setAlarmTime(
            alarmType,
            hour,
            minute,
            isActive)
    }

    fun toggleOnOffCharacteristic() {
        bluetoothService.toggleOnOffCharacteristic()
    }

    fun setManualBrightnessCharacteristic(brightness: Byte) {
        bluetoothService.setManualBrightnessCharacteristic(brightness)
    }

    fun syncBleWithPhone() {
        bluetoothService.syncBleWithPhone()
    }

    fun setAgingOffsetCharacteristic(agingOffset: Int) {
        bluetoothService.setAgingOffsetCharacteristic(agingOffset)
    }

    fun toggleAutoBrightnessCharacteristic() {
        bluetoothService.toggleAutoBrightnessCharacteristic()
    }

    fun setColorOrderCharacteristic(useRrbbgg: Boolean) {
        bluetoothService.setColorOrderCharacteristic(useRrbbgg)
    }

    fun setSplashEnabled(enabled: Boolean) {
        val current = tetrisClockAnimationSplash.value
        val mode = when {
            !enabled -> SplashMode.OFF.value
            current.mode == SplashMode.OFF.value -> SplashMode.DEFAULT_ON.value
            else -> current.mode
        }
        bluetoothService.setAnimationSplashCharacteristic(mode, current.durationValue, current.animationIndex)
    }

    fun setSplashMode(mode: Int) {
        val current = tetrisClockAnimationSplash.value
        bluetoothService.setAnimationSplashCharacteristic(mode, current.durationValue, current.animationIndex)
    }

    fun setSplashDuration(duration: Int) {
        val current = tetrisClockAnimationSplash.value
        bluetoothService.setAnimationSplashCharacteristic(current.mode, duration, current.animationIndex)
    }

    fun setSplashAnimation(index: Int) {
        val current = tetrisClockAnimationSplash.value
        bluetoothService.setAnimationSplashCharacteristic(current.mode, current.durationValue, index)
    }

    fun previewSplash() {
        bluetoothService.previewAnimationSplash()
    }

    fun setHourlyBrightnessCharacteristic(hourlyBrightness: TetrisClockHourlyBrightness) {
        bluetoothService.setHourlyBrightnessCharacteristic(hourlyBrightness)
    }

    fun toggleAlarmActive(alarmType: TetrisClockAlarmType) {
        Log.d("DeviceViewModel", "toggleAlarmActive for ${alarmType}")
        bluetoothService.toggleAlarmActive(alarmType)
    }
}
