@file:OptIn(ExperimentalMaterial3Api::class)

package net.agolyakov.tetrisclockble.ui.screen.device

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PermDeviceInformation
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockDevice
import net.agolyakov.tetrisclockble.R
import net.agolyakov.tetrisclockble.navigation.Screen
import net.agolyakov.tetrisclockble.data.model.ble.SplashAnimation
import net.agolyakov.tetrisclockble.data.model.ble.SplashAnimations
import net.agolyakov.tetrisclockble.data.model.ble.SplashDuration
import net.agolyakov.tetrisclockble.data.model.ble.SplashMode
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockAlarmType
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockTime
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockAlarm
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockHourlyBrightness
import net.agolyakov.tetrisclockble.ui.component.TimePickerDialogState
import net.agolyakov.tetrisclockble.ui.theme.TetrisClockBLETheme
import net.agolyakov.tetrisclockble.ui.theme.displayFontFamily
import net.agolyakov.tetrisclockble.ui.theme.timeHeadlineMedium
import java.time.LocalDateTime

@Composable
fun DeviceScreen(
    navController: NavHostController,
    device: TetrisClockDevice?
) {
    val viewModel: DeviceViewModel = hiltViewModel()
    val firmwareVersion: String by viewModel.tetrisClockFirmwareVersion.collectAsState()
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

    LaunchedEffect(bleTime) {
        phoneTime = TetrisClockTime.now()
    }

    LaunchedEffect(device) {
        viewModel.connectToDevice(device)
        viewModel.bluetoothService.setShouldMaintainConnection(true)
    }

    DeviceSettings(
        deviceFriendlyName = device?.friendlyName ?: device?.deviceName ?: stringResource(R.string.mc_unnamed_device),
        deviceMacAddress = device?.macAddress ?: stringResource(R.string.mc_unknown_address),
        firmwareVersion = firmwareVersion,
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
        splashAnimations = viewModel.splashAnimations,
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
        }
    )
}

@Composable
fun DeviceSettings(
    deviceFriendlyName: String,
    deviceMacAddress: String,
    firmwareVersion: String,
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
    onFirmwareUpdateButtonClickAction: () -> Unit
) {

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .systemBarsPadding()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderCard(
            deviceFriendlyName,
            deviceMacAddress
        )

        MatrixPowerCard(
            modifier = Modifier,
            isOn,
            onSwitchOnOffCheckedChangeAction
        )

        BrightnessCard(
            modifier = Modifier,
            isOn,
            manualBrightness,
            onSliderBrightnessValueChanged,
            isAutoBrightness,
            onAutoBrightnessCheckedChangeAction,
            hourlyBrightness,
            onHourlyBrightnessConfirmed,
            currentHour = bleTime.localDateTime.hour,
        )

        TimeSyncCard(
            modifier = Modifier,
            bleTime,
            phoneTime,
            onButtonSyncClickAction,
            agingOffset,
            onAgingOffsetDialogValueChanged
        )

        OnOffScenariosCard(
            modifier = Modifier,
            turnOnAlarm,
            turnOnAlarmOnTimeClick,
            turnOnAlarmOnActiveToggle,
            turnOffAlarm,
            turnOffAlarmOnTimeClick,
            turnOffAlarmOnActiveToggle
        )

        SplashCard(
            modifier = Modifier,
            enabled = splashEnabled,
            mode = splashMode,
            duration = splashDuration,
            onEnabledChange = onSplashEnabledChange,
            onModeSelected = onSplashModeSelected,
            onDurationSelected = onSplashDurationSelected,
            animations = splashAnimations,
            selectedIndex = splashSelectedIndex,
            onAnimationSelected = onSplashAnimationSelected,
            onPreview = onSplashPreview
        )

        SystemInfoCard(
            modifier = Modifier,
            firmwareVersion,
            onFirmwareUpdateButtonClickAction,
            rtcTemperature,
            isRrbbggColorOrder,
            onColorOrderSelected
        )
    }
}

@Composable
fun HeaderCard(
    friendlyName: String,
    macAddress: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Matrix Clock",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = friendlyName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = macAddress,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun MatrixPowerCard(
    modifier: Modifier,
    isOn: Boolean,
    onSwitchOnOffCheckedChangeAction: (Boolean) -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CardTitle(
                Icons.Default.PowerSettingsNew,
                stringResource(R.string.mc_matrix_power),
                true,
                isOn,
                onSwitchOnOffCheckedChangeAction,
                showBottomSpacing = false
            )
        }
    }
}

@Composable
fun BrightnessCard(
    modifier: Modifier,
    isOn: Boolean,
    manualBrightness: Byte,
    onSliderBrightnessValueChanged: (Float) -> Unit,
    isAutoBrightness: Boolean,
    onAutoBrightnessCheckedChangeAction: (Boolean) -> Unit,
    hourlyBrightness: TetrisClockHourlyBrightness,
    onHourlyBrightnessConfirmed: (TetrisClockHourlyBrightness) -> Unit,
    currentHour: Int
) {
    var showHourlyBrightnessDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        )
        {
            CardTitle(
                Icons.Default.BrightnessAuto,
                "${stringResource(R.string.mc_auto_brightness)} ${hourlyBrightness.valueForHour(currentHour) + 1}",
                true,
                isAutoBrightness,
                onAutoBrightnessCheckedChangeAction
            )

            CardButtonWide(
                Icons.Default.BarChart,
                stringResource(R.string.mc_hourly_brightness),
                { showHourlyBrightnessDialog = true },
                false
            )

            Spacer(Modifier.height(16.dp))

            CardTitle(
                Icons.Default.Brightness6,
                "${stringResource(R.string.mc_manual_brightness)} ${manualBrightness + 1}"
            )

            // The manual value is ignored by the clock while the hourly schedule drives the brightness.
            BrightnessSlider(
                isOn && !isAutoBrightness,
                manualBrightness,
                onSliderBrightnessValueChanged,
            )

            HourlyBrightnessDialog(
                currentValue = hourlyBrightness,
                showDialog = showHourlyBrightnessDialog,
                autoBrightnessEnabled = isAutoBrightness,
                onDismiss = { showHourlyBrightnessDialog = false },
                onValueConfirmed = { newValue ->
                    showHourlyBrightnessDialog = false
                    onHourlyBrightnessConfirmed(newValue)
                }
            )
        }
    }
}

@Composable
fun HourlyBrightnessDialog(
    currentValue: TetrisClockHourlyBrightness,
    showDialog: Boolean,
    autoBrightnessEnabled: Boolean,
    onDismiss: () -> Unit,
    onValueConfirmed: (TetrisClockHourlyBrightness) -> Unit
) {
    if (showDialog) {
        var localValues by remember(currentValue) { mutableStateOf(currentValue.values) }

        AlertDialog(
            title = { Text(stringResource(R.string.mc_hourly_brightness_dialog_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (!autoBrightnessEnabled) {
                        Text(
                            text = stringResource(R.string.mc_hourly_brightness_disabled_tip),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    localValues.forEachIndexed { hour, value ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "%02d:00".format(hour),
                                modifier = Modifier.width(52.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Slider(
                                value = value.toFloat(),
                                onValueChange = { newValue ->
                                    localValues = localValues.toMutableList().also {
                                        it[hour] = newValue.toInt()
                                    }
                                },
                                valueRange = 0f..15f,
                                steps = 14,
                                enabled = autoBrightnessEnabled,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = "${value + 1}",
                                modifier = Modifier.width(24.dp),
                                textAlign = TextAlign.End,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            onDismissRequest = onDismiss,
            confirmButton = {
                Button(
                    onClick = { onValueConfirmed(TetrisClockHourlyBrightness(localValues)) }
                ) {
                    Text(stringResource(R.string.mc_dialog_save))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.mc_dialog_cancel))
                }
            }
        )
    }
}

@Composable
fun BrightnessSlider(
    enabled: Boolean,
    manualBrightness: Byte,
    onSliderBrightnessValueChanged: (Float) -> Unit,
) {
    Slider(
        value = manualBrightness.toFloat(),
        onValueChange = onSliderBrightnessValueChanged,
        valueRange = 0f..15f,
        steps = 14,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        enabled = enabled
    )
}

@Composable
fun TimeSyncCard(
    modifier: Modifier = Modifier,
    bleTime: TetrisClockTime,
    phoneTime: TetrisClockTime,
    onButtonSyncClickAction: () -> Unit,
    agingOffset: Int,
    onAgingOffsetDialogValueChanged: (Int) -> Unit,
) {
    var showAgingOffsetDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CardTitle(
                Icons.Default.Adjust,
                stringResource(R.string.mc_accuracy))

            // Карточки времени в строку
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TimeCard(
                    title = stringResource(R.string.mc_time_of_tetris_clock),
                    time = bleTime,
                    modifier = Modifier.weight(1f)
                )

                TimeCard(
                    title = stringResource(R.string.mc_time_of_mobile_phone),
                    time = phoneTime,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            CardButtonWide(
                Icons.Default.SwapCalls,
                stringResource(R.string.mc_aging_offset),
                { showAgingOffsetDialog = true},
                false
            )

            AgingOffsetDialog(
                agingOffset,
                showAgingOffsetDialog,
                onDismiss = { showAgingOffsetDialog = false },
                onValueConfirmed = { newValue ->
                    showAgingOffsetDialog = false
                    onAgingOffsetDialogValueChanged(newValue)
                }
            )

            Spacer(Modifier.height(12.dp))

            CardButtonWide(
                Icons.Default.Sync,
                stringResource(R.string.mc_action_synchronize_time),
                onButtonSyncClickAction,
                true
            )
        }
    }
}

@Composable
fun TimeCard(
    title: String,
    time: TetrisClockTime,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor =MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = time.formatTimeHours(),
                    style = MaterialTheme.typography.timeHeadlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = ":",
                    style = MaterialTheme.typography.timeHeadlineMedium.copy(fontFamily = displayFontFamily),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = time.formatTimeMinutes(),
                    style = MaterialTheme.typography.timeHeadlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = ":",
                    style = MaterialTheme.typography.timeHeadlineMedium.copy(fontFamily = displayFontFamily),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = time.formatTimeSeconds(),
                    style = MaterialTheme.typography.timeHeadlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = time.formatDayAndMonth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = time.formatYear(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun AgingOffsetDialog(
    currentValue: Int,
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onValueConfirmed: (Int) -> Unit
) {
    if (showDialog) {
        var inputValue by remember(currentValue) { mutableStateOf(currentValue.toString()) }

        AlertDialog(
            title = { Text(stringResource( R.string.mc_dialog_set_aging_offset)) },
            text = {
                Column()
                {
                    Text(text = stringResource(R.string.mc_dialog_agin_offset_tip1))

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = {
                            inputValue = it.filter { char -> char.isDigit() || char == '-' }
                        },
                        label = { Text(stringResource(R.string.mc_aging_offset_range_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            onDismissRequest = onDismiss,
            confirmButton = {
                Button(
                    onClick = {
                        val newValue = inputValue.toIntOrNull()?.coerceIn(-128..127)
                        newValue?.let { onValueConfirmed(it) }
                    }
                ) {
                    Text(stringResource(R.string.mc_dialog_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.mc_dialog_cancel))
                }
            }
        )
    }
}

@Composable
fun OnOffScenariosCard(
    modifier: Modifier,
    turnOnAlarm: TetrisClockAlarm,
    turnOnAlarmOnTimeClick: () -> Unit,
    turnOnAlarmOnActiveToggle: () -> Unit,
    turnOffAlarm: TetrisClockAlarm,
    turnOffAlarmOnTimeClick: () -> Unit,
    turnOffAlarmOnActiveToggle: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        )
        {
            CardTitle(
                Icons.Default.Timelapse,
                stringResource(R.string.mc_scenarios))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AlarmItem(
                    modifier = Modifier.weight(1f),
                    labelTop = stringResource(R.string.mc_auto),
                    labelBottom = stringResource(R.string.mc_on),
                    alarm = turnOnAlarm,
                    onTimeClick = turnOnAlarmOnTimeClick,
                    onActiveToggle = turnOnAlarmOnActiveToggle
                )
                AlarmItem(
                    modifier = Modifier.weight(1f),
                    labelTop = stringResource(R.string.mc_auto),
                    labelBottom = stringResource(R.string.mc_off),
                    alarm = turnOffAlarm,
                    onTimeClick = turnOffAlarmOnTimeClick,
                    onActiveToggle = turnOffAlarmOnActiveToggle
                )
            }
        }
    }
}

@Composable
fun AlarmItem(
    modifier: Modifier = Modifier,
    labelTop: String,
    labelBottom: String,
    alarm: TetrisClockAlarm,
    onTimeClick: () -> Unit,
    onActiveToggle: () -> Unit
) {
    val color = if (alarm.isActive) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column {
                    Text(
                        text = labelTop,
                        color = color,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)

                    Text(
                        text = labelBottom,
                        color = color,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                }

                Switch(
                    checked = alarm.isActive,
                    onCheckedChange = {
                            _ -> onActiveToggle()
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            AlarmTimeField(
                alarm.hours,
                alarm.minutes,
                isActive = alarm.isActive,
                onFieldClick = onTimeClick
            )
        }

    }
}

@Composable
fun AlarmTimeField(
    alarmHours: Byte,
    alarmMinutes: Byte,
    isActive: Boolean,
    onFieldClick: () -> Unit
) {
    val color = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = isActive,
                onClick = onFieldClick
            )
            .border(
                width = 1.dp,
                color = color,
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = "%02d".format(alarmHours),
                style = MaterialTheme.typography.timeHeadlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = ":",
                style = MaterialTheme.typography.timeHeadlineMedium.copy(fontFamily = displayFontFamily),
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = "%02d".format(alarmMinutes),
                style = MaterialTheme.typography.timeHeadlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onTimeSelected: (Int, Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(timePickerState.hour, timePickerState.minute)
                    onDismiss()
                }
            ) {
                Text(text = stringResource(R.string.mc_dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.mc_dialog_cancel))
            }
        },
        title = { Text(stringResource(R.string.mc_action_set_time)) },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}

@Composable
fun SystemInfoCard(
    modifier: Modifier,
    firmwareVersion: String,
    onFirmwareUpdateButtonClickAction: () -> Unit,
    rtcTemperature: Float,
    isRrbbggColorOrder: Boolean,
    onColorOrderSelected: (Boolean) -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        )
        {
            CardTitle(
                Icons.Default.PermDeviceInformation,
                stringResource(R.string.mc_system)
            )

            PixelColorOrderSelector(
                isRrbbgg = isRrbbggColorOrder,
                onSelected = onColorOrderSelected
            )

            Spacer(Modifier.height(16.dp))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "${stringResource(R.string.mc_rtc_temperature)}: ${
                    if (rtcTemperature > 0) "+" else ""
                }${"%.2f".format(rtcTemperature)} ${stringResource(R.string.mc_degrees_celsius)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.width(8.dp))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.mc_clock_firmware_version, firmwareVersion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.width(16.dp))

            CardButtonWide(
                Icons.Default.SystemUpdateAlt,
                stringResource(R.string.mc_firmware_update_title),
                onFirmwareUpdateButtonClickAction,
                false
            )
        }
    }
}

@Composable
fun PixelColorOrderSelector(
    isRrbbgg: Boolean,
    onSelected: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
    ) {
        Text(
            text = stringResource(R.string.mc_pixel_color_order),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = !isRrbbgg,
                    onClick = { onSelected(false) },
                    role = Role.RadioButton
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = !isRrbbgg,
                onClick = null
            )
            Text(
                text = stringResource(R.string.mc_pixel_color_order_rrggbb),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = isRrbbgg,
                    onClick = { onSelected(true) },
                    role = Role.RadioButton
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isRrbbgg,
                onClick = null
            )
            Text(
                text = stringResource(R.string.mc_pixel_color_order_rrbbgg),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun SplashCard(
    modifier: Modifier,
    enabled: Boolean,
    mode: Int,
    duration: Int,
    onEnabledChange: (Boolean) -> Unit,
    onModeSelected: (Int) -> Unit,
    onDurationSelected: (Int) -> Unit,
    animations: List<SplashAnimation>,
    selectedIndex: Int,
    onAnimationSelected: (Int) -> Unit,
    onPreview: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CardTitle(
                imageVector = Icons.Default.Pets,
                text = stringResource(R.string.mc_splash_title),
                showSwitch = true,
                isOn = enabled,
                onCheckedChange = onEnabledChange
            )

            // Text follows the current state: a general blurb when off, the cadence-specific
            // description when on.
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = if (enabled) splashModeHint(mode) else stringResource(R.string.mc_splash_hint_off),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )

            if (enabled) {
                Spacer(Modifier.height(16.dp))

                SplashFrequencyPicker(
                    selectedMode = mode,
                    onModeSelected = onModeSelected
                )

                Spacer(Modifier.height(16.dp))

                SplashDurationPicker(
                    selectedDuration = duration,
                    onDurationSelected = onDurationSelected
                )

                Spacer(Modifier.height(16.dp))

                SplashAnimationPicker(
                    animations = animations,
                    selectedIndex = selectedIndex,
                    onAnimationSelected = onAnimationSelected
                )

                Spacer(Modifier.height(16.dp))

                CardButtonWide(
                    imageVector = Icons.Default.PlayArrow,
                    text = stringResource(R.string.mc_splash_preview),
                    onClickAction = onPreview,
                    isPrimary = false
                )
            }
        }
    }
}

// Human-readable cadence label for a splash mode value (used in the frequency dropdown).
@Composable
fun splashModeLabel(mode: Int): String = when (mode) {
    SplashMode.EVERY_3H.value -> stringResource(R.string.mc_splash_freq_3h)
    SplashMode.HOURLY.value -> stringResource(R.string.mc_splash_freq_hourly)
    else -> stringResource(R.string.mc_splash_freq_daily)
}

// Cadence-specific description shown under the title when the feature is on.
@Composable
fun splashModeHint(mode: Int): String = when (mode) {
    SplashMode.EVERY_3H.value -> stringResource(R.string.mc_splash_hint_3h)
    SplashMode.HOURLY.value -> stringResource(R.string.mc_splash_hint_hourly)
    else -> stringResource(R.string.mc_splash_hint_daily)
}

// Human-readable label for a duration value, e.g. "20 s" (used in the duration dropdown).
@Composable
fun splashDurationLabel(duration: Int): String =
    stringResource(R.string.mc_splash_duration_seconds, SplashDuration.fromValue(duration).seconds)

@Composable
fun SplashDurationPicker(
    selectedDuration: Int,
    onDurationSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.mc_splash_duration_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(Modifier.height(4.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = splashDurationLabel(selectedDuration),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                SplashDuration.all.forEach { splashDuration ->
                    DropdownMenuItem(
                        text = { Text(splashDurationLabel(splashDuration.value)) },
                        onClick = {
                            expanded = false
                            onDurationSelected(splashDuration.value)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SplashFrequencyPicker(
    selectedMode: Int,
    onModeSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.mc_splash_frequency_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(Modifier.height(4.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = splashModeLabel(selectedMode),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                SplashMode.selectable.forEach { splashMode ->
                    DropdownMenuItem(
                        text = { Text(splashModeLabel(splashMode.value)) },
                        onClick = {
                            expanded = false
                            onModeSelected(splashMode.value)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SplashAnimationPicker(
    animations: List<SplashAnimation>,
    selectedIndex: Int,
    onAnimationSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedAnimation = animations.firstOrNull { it.index == selectedIndex }
        ?: animations.firstOrNull()
    val selectedName = selectedAnimation?.let { stringResource(it.nameRes) } ?: ""

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.mc_splash_animation_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(Modifier.height(4.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = selectedName,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                animations.forEach { animation ->
                    DropdownMenuItem(
                        text = { Text(stringResource(animation.nameRes)) },
                        onClick = {
                            expanded = false
                            onAnimationSelected(animation.index)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CardTitle(
    imageVector: ImageVector,
    text: String,
    showSwitch: Boolean = false,
    isOn: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
    showBottomSpacing: Boolean = true
) {
    Row (
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    )
    {
        Row (
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(28.dp)
            )

            Spacer(Modifier.width(8.dp))

            Text(
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                text = text
            )
        }

        if(showSwitch)
        {
            Switch(
                checked = isOn,
                onCheckedChange = onCheckedChange
            )
        }
    }

    if (showBottomSpacing) {
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun CardButtonWide(
    imageVector: ImageVector,
    text: String,
    onClickAction: () -> Unit,
    isPrimary: Boolean = false
) {
    val containerColor =
        if (isPrimary)  MaterialTheme.colorScheme.primary
        else  MaterialTheme.colorScheme.surfaceVariant

    val contentColor =
        if (isPrimary)  MaterialTheme.colorScheme.onPrimary
        else  MaterialTheme.colorScheme.onSurfaceVariant

    Button(
        onClick = onClickAction,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = text,
            modifier = Modifier.size(24.dp)
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium
        )
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
            onFirmwareUpdateButtonClickAction = {}
        )
    }
}
