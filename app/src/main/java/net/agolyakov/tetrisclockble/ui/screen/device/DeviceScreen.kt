@file:OptIn(ExperimentalMaterial3Api::class)

package net.agolyakov.tetrisclockble.ui.screen.device

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import net.agolyakov.tetrisclockble.protocol.UpdateCheckState
import net.agolyakov.tetrisclockble.protocol.WifiSettingsUpdate
import net.agolyakov.tetrisclockble.protocol.WifiMode
import net.agolyakov.tetrisclockble.protocol.WifiSettings
import net.agolyakov.tetrisclockble.protocol.WifiState
import net.agolyakov.tetrisclockble.protocol.WifiStatus
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import net.agolyakov.tetrisclockble.data.model.ble.ConnectionState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import java.time.LocalDateTime
import net.agolyakov.tetrisclockble.R
import net.agolyakov.tetrisclockble.data.model.ble.SplashAnimation
import net.agolyakov.tetrisclockble.data.model.ble.SplashAnimations
import net.agolyakov.tetrisclockble.data.model.ble.SplashDuration
import net.agolyakov.tetrisclockble.data.model.ble.SplashMode
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockAlarm
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockAlarmType
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockDevice
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockHourlyBrightness
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockTime
import net.agolyakov.tetrisclockble.navigation.Screen
import net.agolyakov.tetrisclockble.ui.component.TimePickerDialogState
import net.agolyakov.tetrisclockble.ui.theme.TetrisClockBLETheme

@Composable
fun DeviceScreen(
    navController: NavHostController,
    device: TetrisClockDevice?
) {
    val viewModel: DeviceViewModel = hiltViewModel()
    val firmwareVersion: String by viewModel.tetrisClockFirmwareVersion.collectAsState()
    val flashSizeMb: Int by viewModel.tetrisClockFlashSizeMb.collectAsState()
    val animationCount: Int by viewModel.tetrisClockAnimationCount.collectAsState()
    // Show only the animations the connected clock actually ships (reported over BLE); 0 means
    // firmware older than v1.7.0, which doesn't report it and only ever had the first four.
    // take() caps safely if the clock ever reports more than the app currently bundles.
    val visibleSplashAnimations = viewModel.splashAnimations.take(
        if (animationCount > 0) animationCount else SplashAnimations.LEGACY_COUNT
    )
    val isOn: Boolean by viewModel.tetrisClockTetrisOn.collectAsState()
    val manualBrightness: Byte by viewModel.tetrisClockManualBrightness.collectAsState()
    val isAutoBrightness: Boolean by viewModel.tetrisClockIsAutoBrightness.collectAsState()
    val isRrbbggColorOrder: Boolean by viewModel.tetrisClockIsRrbbggColorOrder.collectAsState()
    val animationSplash by viewModel.tetrisClockAnimationSplash.collectAsState()
    val hourlyBrightness: TetrisClockHourlyBrightness by viewModel.tetrisClockHourlyBrightness.collectAsState()
    val bleTime: TetrisClockTime by viewModel.tetrisClockBleDeviceTime.collectAsState()
    var phoneTime: TetrisClockTime by remember { mutableStateOf(TetrisClockTime.now()) }
    val timePickerState: TimePickerDialogState by viewModel.timePickerState.collectAsState()
    val turnOnAlarm: TetrisClockAlarm by viewModel.tetrisClockTurnOnAlarm.collectAsState()
    val turnOffAlarm: TetrisClockAlarm by viewModel.tetrisClockTurnOffAlarm.collectAsState()
    val agingOffset: Int by viewModel.tetrisClockAgingOffset.collectAsState()
    val rtcTemperature: Float by viewModel.tetrisClockRtcTemperature.collectAsState()
    val networkUiState: NetworkUiState by viewModel.networkUiState.collectAsState()

    if (timePickerState.isVisible) {
        TimePickerDialog(
            initialHour = timePickerState.hour,
            initialMinute = timePickerState.minute,
            onDismiss = { viewModel.hideTimePickerDialog() },
            onTimeSelected = { hour, minute ->
                viewModel.setAlarmTime(
                    timePickerState.alarmType,
                    hour,
                    minute,
                    timePickerState.isActive
                )
                viewModel.hideTimePickerDialog()
            }
        )
    }

    val connectionState by viewModel.connectionState.collectAsState()
    // Until the clock is first connected (or when it refused this phone) the screen shows only the
    // connection, not the clock's cards with their empty defaults. Later drops (a WiFi update reboots
    // the clock) keep the cards: their dialogs follow the reconnect.
    var everReady by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.Ready) everReady = true
    }

    LaunchedEffect(bleTime) {
        phoneTime = TetrisClockTime.now()
    }

    LaunchedEffect(device) {
        viewModel.connectToDevice(device)
        viewModel.bluetoothService.setShouldMaintainConnection(true)
    }

    val friendlyName = device?.friendlyName ?: device?.deviceName ?: stringResource(R.string.mc_unnamed_device)
    val macAddress = device?.macAddress ?: stringResource(R.string.mc_unknown_address)
    if (connectionState is ConnectionState.PairingRejected ||
        (connectionState != ConnectionState.Ready && !everReady)) {
        ConnectionScreen(
            friendlyName = friendlyName,
            macAddress = macAddress,
            state = connectionState,
            onRetry = { viewModel.connectToDevice(device) },
            onForgetAndRetry = { viewModel.forgetBondAndReconnect(device) }
        )
        return
    }

    DeviceSettings(
        deviceFriendlyName = device?.friendlyName ?: device?.deviceName ?: stringResource(R.string.mc_unnamed_device),
        deviceMacAddress = device?.macAddress ?: stringResource(R.string.mc_unknown_address),
        firmwareVersion = firmwareVersion,
        flashSizeMb = flashSizeMb,
        isOn = isOn,
        onSwitchOnOffCheckedChangeAction = { isChecked ->
            viewModel.toggleOnOffCharacteristic()
        },
        manualBrightness = manualBrightness,
        onSliderBrightnessValueChanged = { newValue ->
            viewModel.setManualBrightnessCharacteristic(newValue.toInt().toByte())
        },
        isAutoBrightness = isAutoBrightness,
        onAutoBrightnessCheckedChangeAction = {
            viewModel.toggleAutoBrightnessCharacteristic()
        },
        hourlyBrightness = hourlyBrightness,
        onHourlyBrightnessConfirmed = { newValue ->
            viewModel.setHourlyBrightnessCharacteristic(newValue)
        },
        bleTime = bleTime,
        phoneTime = phoneTime,
        onButtonSyncClickAction = { viewModel.syncBleWithPhone() },
        agingOffset = agingOffset,
        onAgingOffsetDialogValueChanged = { newValue ->
            viewModel.setAgingOffsetCharacteristic(newValue)
        },
        rtcTemperature = rtcTemperature,
        isRrbbggColorOrder = isRrbbggColorOrder,
        onColorOrderSelected = { useRrbbgg ->
            viewModel.setColorOrderCharacteristic(useRrbbgg)
        },
        splashEnabled = animationSplash.enabled,
        splashMode = animationSplash.mode,
        splashDuration = animationSplash.durationValue,
        onSplashEnabledChange = { enabled ->
            viewModel.setSplashEnabled(enabled)
        },
        onSplashModeSelected = { mode ->
            viewModel.setSplashMode(mode)
        },
        onSplashDurationSelected = { duration ->
            viewModel.setSplashDuration(duration)
        },
        splashAnimations = visibleSplashAnimations,
        splashSelectedIndex = animationSplash.animationIndex,
        onSplashAnimationSelected = { index ->
            viewModel.setSplashAnimation(index)
        },
        onSplashPreview = { viewModel.previewSplash() },
        turnOnAlarm = turnOnAlarm,
        turnOnAlarmOnTimeClick = {
            viewModel.showTimePickerDialog(TetrisClockAlarmType.TURN_ON, turnOnAlarm)
        },
        turnOnAlarmOnActiveToggle = {
            viewModel.toggleAlarmActive(TetrisClockAlarmType.TURN_ON)
        },
        turnOffAlarm = turnOffAlarm,
        turnOffAlarmOnTimeClick = {
            viewModel.showTimePickerDialog(TetrisClockAlarmType.TURN_OFF, turnOffAlarm)
        },
        turnOffAlarmOnActiveToggle = {
            viewModel.toggleAlarmActive(TetrisClockAlarmType.TURN_OFF)
        },
        onFirmwareUpdateButtonClickAction = {
            navController.navigate(Screen.Firmware.route)
        },
        network = networkUiState,
        networkActions = viewModel
    )
}

/** The two tabs of the clock screen: everyday controls and the rarely touched settings. */
enum class DeviceTab(val icon: ImageVector, val labelRes: Int) {
    CLOCK(Icons.Default.Schedule, R.string.mc_tab_clock),
    SETTINGS(Icons.Default.Settings, R.string.mc_tab_settings)
}

/**
 * Between the clock list and the clock's cards: the connection under way, or why there is none (the
 * clock refused this phone, the connection failed). Modeled on the FireLamp app.
 */
@Composable
private fun ConnectionScreen(
    friendlyName: String,
    macAddress: String,
    state: ConnectionState,
    onRetry: () -> Unit,
    onForgetAndRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderCard(friendlyName, macAddress)
        when (state) {
            is ConnectionState.PairingRejected -> MessageCard(
                title = stringResource(R.string.mc_pairing_rejected_title),
                text = stringResource(R.string.mc_pairing_rejected_hint)
            ) {
                Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.mc_pairing_retry))
                }
                // When the app removed the stale bond itself there is nothing left to forget
                if (!state.staleBondRemoved) {
                    Text(
                        text = stringResource(R.string.mc_pairing_rejected_forget_hint),
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(onClick = onForgetAndRetry, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.mc_pairing_forget_and_retry))
                    }
                }
            }
            is ConnectionState.Error -> MessageCard(
                title = stringResource(R.string.mc_state_failed),
                text = stringResource(R.string.mc_state_failed_hint),
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.mc_pairing_retry))
                }
            }
            else -> Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(
                        when (state) {
                            ConnectionState.Bonding -> R.string.mc_state_bonding
                            ConnectionState.Connected -> R.string.mc_state_loading
                            else -> R.string.mc_state_connecting
                        }
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MessageCard(
    title: String,
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.errorContainer,
    actions: @Composable () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodyMedium)
            actions()
        }
    }
}

@Composable
fun DeviceSettings(
    deviceFriendlyName: String,
    deviceMacAddress: String,
    firmwareVersion: String,
    flashSizeMb: Int,
    isOn: Boolean,
    onSwitchOnOffCheckedChangeAction: (Boolean) -> Unit,
    manualBrightness: Byte,
    onSliderBrightnessValueChanged: (Float) -> Unit,
    isAutoBrightness: Boolean,
    onAutoBrightnessCheckedChangeAction: (Boolean) -> Unit,
    hourlyBrightness: TetrisClockHourlyBrightness,
    onHourlyBrightnessConfirmed: (TetrisClockHourlyBrightness) -> Unit,
    bleTime: TetrisClockTime,
    phoneTime: TetrisClockTime,
    onButtonSyncClickAction: () -> Unit,
    agingOffset: Int,
    onAgingOffsetDialogValueChanged: (Int) -> Unit,
    rtcTemperature: Float,
    isRrbbggColorOrder: Boolean,
    onColorOrderSelected: (Boolean) -> Unit,
    splashEnabled: Boolean,
    splashMode: Int,
    splashDuration: Int,
    onSplashEnabledChange: (Boolean) -> Unit,
    onSplashModeSelected: (Int) -> Unit,
    onSplashDurationSelected: (Int) -> Unit,
    splashAnimations: List<SplashAnimation>,
    splashSelectedIndex: Int,
    onSplashAnimationSelected: (Int) -> Unit,
    onSplashPreview: () -> Unit,
    turnOnAlarm: TetrisClockAlarm,
    turnOnAlarmOnTimeClick: () -> Unit,
    turnOnAlarmOnActiveToggle: () -> Unit,
    turnOffAlarm: TetrisClockAlarm,
    turnOffAlarmOnTimeClick: () -> Unit,
    turnOffAlarmOnActiveToggle: () -> Unit,
    onFirmwareUpdateButtonClickAction: () -> Unit,
    network: NetworkUiState = NetworkUiState(),
    networkActions: NetworkActions = NoNetworkActions,
    initialTab: DeviceTab = DeviceTab.CLOCK
) {
    // A newer firmware found by the phone or by the clock itself
    val updateAvailable = network.firmwareCheck is FirmwareCheck.Available ||
        network.updateInfo?.state == UpdateCheckState.AVAILABLE

    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    // Each tab keeps its scroll position while the other one is shown
    val tabStateHolder = rememberSaveableStateHolder()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        bottomBar = {
            NavigationBar {
                DeviceTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        icon = {
                            if (tab == DeviceTab.SETTINGS && updateAvailable) {
                                BadgedBox(badge = { Badge() }) { Icon(tab.icon, contentDescription = null) }
                            } else {
                                Icon(tab.icon, contentDescription = null)
                            }
                        },
                        label = { Text(stringResource(tab.labelRes)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        tabStateHolder.SaveableStateProvider(selectedTab) {
            DeviceTabColumn(innerPadding) {
                HeaderCard(
                    deviceFriendlyName,
                    deviceMacAddress
                )

                when (selectedTab) {
                    DeviceTab.CLOCK -> ClockTab(
                        isOn = isOn,
                        onSwitchOnOffCheckedChangeAction = onSwitchOnOffCheckedChangeAction,
                        manualBrightness = manualBrightness,
                        onSliderBrightnessValueChanged = onSliderBrightnessValueChanged,
                        isAutoBrightness = isAutoBrightness,
                        onAutoBrightnessCheckedChangeAction = onAutoBrightnessCheckedChangeAction,
                        hourlyBrightness = hourlyBrightness,
                        onHourlyBrightnessConfirmed = onHourlyBrightnessConfirmed,
                        currentHour = bleTime.localDateTime.hour,
                        turnOnAlarm = turnOnAlarm,
                        turnOnAlarmOnTimeClick = turnOnAlarmOnTimeClick,
                        turnOnAlarmOnActiveToggle = turnOnAlarmOnActiveToggle,
                        turnOffAlarm = turnOffAlarm,
                        turnOffAlarmOnTimeClick = turnOffAlarmOnTimeClick,
                        turnOffAlarmOnActiveToggle = turnOffAlarmOnActiveToggle,
                        splashEnabled = splashEnabled,
                        splashMode = splashMode,
                        splashDuration = splashDuration,
                        onSplashEnabledChange = onSplashEnabledChange,
                        onSplashModeSelected = onSplashModeSelected,
                        onSplashDurationSelected = onSplashDurationSelected,
                        splashAnimations = splashAnimations,
                        splashSelectedIndex = splashSelectedIndex,
                        onSplashAnimationSelected = onSplashAnimationSelected,
                        onSplashPreview = onSplashPreview
                    )

                    DeviceTab.SETTINGS -> SettingsTab(
                        bleTime = bleTime,
                        phoneTime = phoneTime,
                        onButtonSyncClickAction = onButtonSyncClickAction,
                        agingOffset = agingOffset,
                        onAgingOffsetDialogValueChanged = onAgingOffsetDialogValueChanged,
                        firmwareVersion = firmwareVersion,
                        onFirmwareUpdateButtonClickAction = onFirmwareUpdateButtonClickAction,
                        flashSizeMb = flashSizeMb,
                        rtcTemperature = rtcTemperature,
                        isRrbbggColorOrder = isRrbbggColorOrder,
                        onColorOrderSelected = onColorOrderSelected,
                        network = network,
                        networkActions = networkActions
                    )
                }
            }
        }
    }
}

/** For the previews: no clock to talk to. */
private object NoNetworkActions : NetworkActions {
    override fun saveWifiSettings(update: WifiSettingsUpdate) {}
    override fun saveDeviceName(name: String) {}
    override fun setBetaUpdates(beta: Boolean) {}
    override fun scanWifi() {}
    override fun syncTimeFromInternet() {}
    override fun checkFirmware() {}
    override fun installFirmwareByWifi() {}
    override fun dismissWifiUpdate() {}
}

/** The scrolling column of cards every tab is laid out in. */
@Composable
private fun DeviceTabColumn(
    innerPadding: PaddingValues,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        content()
    }
}

@Composable
@Preview(
    name = "State 1: Light Theme",
    showBackground = true,
    heightDp = 1200
)
fun DeviceSettings_State1_Preview(){
    TetrisClockBLETheme (
        darkTheme = false
    ) {
        DeviceSettings(
            deviceFriendlyName = "Часы в детской",
            deviceMacAddress = "1a:2b:3c:4d:5e:6f",
            firmwareVersion = "v1.0.1-rc6",
            flashSizeMb = 4,
            isOn = true,
            onButtonSyncClickAction = {},
            manualBrightness = 8,
            onSliderBrightnessValueChanged = {},
            isAutoBrightness = true,
            onAutoBrightnessCheckedChangeAction = {},
            hourlyBrightness = TetrisClockHourlyBrightness(),
            onHourlyBrightnessConfirmed = {},
            bleTime = TetrisClockTime(
                LocalDateTime.now()
                    .minusHours(1)
                    .minusMinutes(1)
                    .minusSeconds(1)
            ),
            phoneTime = TetrisClockTime(LocalDateTime.now()),
            onSwitchOnOffCheckedChangeAction = {},
            agingOffset = 5,
            onAgingOffsetDialogValueChanged = {},
            rtcTemperature = 21.25F,
            isRrbbggColorOrder = false,
            onColorOrderSelected = {},
            splashEnabled = true,
            splashMode = SplashMode.EVERY_3H.value,
            splashDuration = SplashDuration.S20.value,
            onSplashEnabledChange = {},
            onSplashModeSelected = {},
            onSplashDurationSelected = {},
            splashAnimations = SplashAnimations.all,
            splashSelectedIndex = 0,
            onSplashAnimationSelected = {},
            onSplashPreview = {},
            turnOnAlarm = TetrisClockAlarm(
                isActive = true,
                hours = 6,
                minutes = 15),
            turnOnAlarmOnTimeClick = {},
            turnOnAlarmOnActiveToggle = {},
            turnOffAlarm = TetrisClockAlarm(
                isActive = false,
                hours = 23,
                minutes = 45),
            turnOffAlarmOnTimeClick = {},
            turnOffAlarmOnActiveToggle = {},
            onFirmwareUpdateButtonClickAction = {}
        )
    }
}

@Composable
@Preview(
    name = "State 2: Dark Theme",
    showBackground = true,
    heightDp = 1200)
fun DeviceSettings_State2_Preview(){
    TetrisClockBLETheme (
        darkTheme = true
    ) {
        DeviceSettings(
            deviceFriendlyName = "Tetris Clock",
            deviceMacAddress = "11:22:33:44:55:66",
            firmwareVersion = "v.1.0.0",
            flashSizeMb = 16,
            isOn = false,
            onButtonSyncClickAction = {},
            manualBrightness = 3,
            onSliderBrightnessValueChanged = {},
            isAutoBrightness = false,
            onAutoBrightnessCheckedChangeAction = {},
            hourlyBrightness = TetrisClockHourlyBrightness(),
            onHourlyBrightnessConfirmed = {},
            bleTime = TetrisClockTime(
                LocalDateTime.now()
                    .minusHours(1)
                    .minusMinutes(1)
                    .minusSeconds(1)
            ),
            phoneTime = TetrisClockTime(LocalDateTime.now()),
            onSwitchOnOffCheckedChangeAction = {},
            agingOffset = -108,
            onAgingOffsetDialogValueChanged = {},
            rtcTemperature = -10.25F,
            isRrbbggColorOrder = true,
            onColorOrderSelected = {},
            splashEnabled = false,
            splashMode = SplashMode.OFF.value,
            splashDuration = SplashDuration.S10.value,
            onSplashEnabledChange = {},
            onSplashModeSelected = {},
            onSplashDurationSelected = {},
            splashAnimations = SplashAnimations.all,
            splashSelectedIndex = 0,
            onSplashAnimationSelected = {},
            onSplashPreview = {},
            turnOnAlarm = TetrisClockAlarm(
                isActive = false,
                hours = 6,
                minutes = 15),
            turnOnAlarmOnTimeClick = {},
            turnOnAlarmOnActiveToggle = {},
            turnOffAlarm = TetrisClockAlarm(
                isActive = true,
                hours = 23,
                minutes = 45),
            turnOffAlarmOnTimeClick = {},
            turnOffAlarmOnActiveToggle = {},
            onFirmwareUpdateButtonClickAction = {},
            network = NetworkUiState(
                supported = true,
                timeZone = "MSK-3",
                wifiSettings = WifiSettings(WifiMode.PERIODIC, 1, "HomeNet", passwordStored = true),
                wifiStatus = WifiStatus(WifiState.OFF, null, 0, 1792665600L),
                firmwareCheck = FirmwareCheck.Available("v2.1.0")
            ),
            initialTab = DeviceTab.SETTINGS
        )
    }
}
