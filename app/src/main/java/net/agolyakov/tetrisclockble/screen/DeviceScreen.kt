@file:OptIn(ExperimentalMaterial3Api::class)

package net.agolyakov.tetrisclockble.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import net.agolyakov.tetrisclockble.ble.BleDevice
import net.agolyakov.tetrisclockble.viewmodel.DeviceViewModel
import net.agolyakov.tetrisclockble.R
import net.agolyakov.tetrisclockble.ble.TetrisClockAlarmType
import net.agolyakov.tetrisclockble.ble.TetrisClockTime
import net.agolyakov.tetrisclockble.ble.TetrisClockAlarm
import net.agolyakov.tetrisclockble.ble.TimePickerDialogState
import net.agolyakov.tetrisclockble.ui.theme.TetrisClockBLETheme
import java.time.LocalDateTime

@Composable
fun DeviceScreen(
    navController: NavHostController,
    device: BleDevice?,
) {
    val viewModel: DeviceViewModel = hiltViewModel()
    val isOn: Boolean by viewModel.matrixClockIsOn.collectAsState()
    val manualBrightness: Byte by viewModel.matrixClockManualBrightness.collectAsState()
    val bleTime: TetrisClockTime by viewModel.matrixClockBleDeviceTime.collectAsState()
    var phoneTime: TetrisClockTime by remember { mutableStateOf(TetrisClockTime.now()) }
    val timePickerState: TimePickerDialogState by viewModel.timePickerState.collectAsState()
    val turnOnAlarm: TetrisClockAlarm by viewModel.matrixClockTurnOnAlarm.collectAsState()
    val turnOffAlarm: TetrisClockAlarm by viewModel.matrixClockTurnOffAlarm.collectAsState()

    if (timePickerState.isVisible) {
        TimePickerDialog(
            initialHour = timePickerState.hour,
            initialMinute = timePickerState.minute,
            onDismiss = { viewModel.hideTimePickerDialog() },
            onTimeSelected = { hour, minute ->
                viewModel.setAlarmTime(hour, minute, timePickerState.isActive)
            }
        )
    }

    LaunchedEffect(bleTime) {
        phoneTime = TetrisClockTime.now()
    }

    LaunchedEffect(device) {
        viewModel.connectToDevice(device)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.disconnect()
        }
    }

    DeviceSettings(
        deviceFriendlyName = device?.friendlyName ?: device?.deviceName ?: "<без имени>",
        deviceName = device?.deviceName ?: "<без имени>",
        deviceMacAddress = device?.deviceMacAddress ?: "<без адреса?",
        isOn = isOn,
        onButtonOnOffClickAction = { viewModel.toggleOnOffCharacteristic() },
        manualBrightness = manualBrightness,
        onSliderBrightnessValueChanged = {
            newValue -> viewModel.setManualBrightnessCharacteristic(newValue.toInt().toByte())
        },
        bleTime = bleTime,
        phoneTime = phoneTime,
        onButtonSyncClickAction = { viewModel.syncBleWithPhone() },
        turnOnAlarm = turnOnAlarm,
        turnOnAlarmOnTimeClick = {
            viewModel.showTimePickerDialog(TetrisClockAlarmType.TURN_ON, turnOnAlarm) },
        turnOnAlarmOnActiveToggle = {
            viewModel.toggleAlarmActive(TetrisClockAlarmType.TURN_ON) },
        turnOffAlarm,
        turnOffAlarmOnTimeClick = {
            viewModel.showTimePickerDialog(TetrisClockAlarmType.TURN_OFF, turnOffAlarm)
        },
        turnOffAlarmOnActiveToggle = {
            viewModel.toggleAlarmActive(TetrisClockAlarmType.TURN_OFF)
        }
    )
}

@Composable
fun DeviceSettings (
    deviceFriendlyName: String,
    deviceName: String,
    deviceMacAddress: String,
    isOn: Boolean,
    onButtonOnOffClickAction: () -> Unit,
    manualBrightness: Byte,
    onSliderBrightnessValueChanged: (Float) -> Unit,
    bleTime: TetrisClockTime,
    phoneTime: TetrisClockTime,
    onButtonSyncClickAction: () -> Unit,
    turnOnAlarm: TetrisClockAlarm,
    turnOnAlarmOnTimeClick: () -> Unit,
    turnOnAlarmOnActiveToggle: () -> Unit,
    turnOffAlarm: TetrisClockAlarm,
    turnOffAlarmOnTimeClick: () -> Unit,
    turnOffAlarmOnActiveToggle: () -> Unit)
{
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxHeight()
            .systemBarsPadding(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Header(
                deviceFriendlyName,
                deviceName,
                deviceMacAddress,
            )

            Spacer(Modifier.height(20.dp))

            ClocksWithSyncButton(
                bleTime,
                phoneTime,
                onButtonSyncClickAction,
            )

            Spacer(Modifier.height(40.dp))

            OnOffAlarms(
                turnOnAlarm,
                turnOnAlarmOnTimeClick,
                turnOnAlarmOnActiveToggle,
                turnOffAlarm,
                turnOffAlarmOnTimeClick,
                turnOffAlarmOnActiveToggle
            )

            Spacer(Modifier.height(20.dp))

            OnOffButton (
                isOn,
                onButtonOnOffClickAction,
            )

            Spacer(Modifier.height(40.dp))

            BrightnessSlider(
                manualBrightness,
                onSliderBrightnessValueChanged,
            )
        }
    }
}

@Composable
fun Header(
    friendlyName: String?,
    deviceName: String,
    deviceMacAddress: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = friendlyName ?: deviceName,
                style = MaterialTheme.typography.headlineLarge,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f, fill = false)
            )
        }

        Spacer(Modifier.height(5.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.width(15.dp))

            Text(
                text = deviceName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
            )

            Spacer(Modifier.width(40.dp))

            Text(
                text = deviceMacAddress,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun OnOffButton (
    isOn: Boolean,
    onButtonOnOffClickAction: () -> Unit
) {
    Button(
        onClick = onButtonOnOffClickAction
    ) {
        Text(
            text =
                if (isOn) stringResource(R.string.mc_manual_turn_off_clock)
                else stringResource(R.string.mc_manual_turn_on_clock),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BrightnessSlider(
    manualBrightness: Byte,
    onSliderBrightnessValueChanged: (Float) -> Unit,
) {
    Text(
        text = "${stringResource(R.string.mc_manual_brightness)} : ${manualBrightness + 1}",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center
    )

    Spacer(Modifier.height(10.dp))

    Slider(

        value = manualBrightness.toFloat(),
        onValueChange = onSliderBrightnessValueChanged,
        valueRange = 0f..15f,
        steps = 14,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)

    )
}

@Composable
fun ClocksWithSyncButton(
    bleTime: TetrisClockTime,
    phoneTime: TetrisClockTime,
    onSyncClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row (
            modifier = Modifier.fillMaxWidth()
        ){
            Column (
                modifier = Modifier
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Text(
                    text = stringResource(R.string.mc_time_of_tetris_clock),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = bleTime.formatTime(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = bleTime.formatDate(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }
            }

            Column (
                modifier = Modifier
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ){

                Text(
                    text = stringResource(R.string.mc_time_of_mobile_phone),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = phoneTime.formatTime(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = phoneTime.formatDate(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(onClick = onSyncClick) {
            Text(text = stringResource(R.string.mc_action_synchronize_time),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center)
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
fun OnOffAlarms(
    turnOnAlarm: TetrisClockAlarm,
    turnOnAlarmOnTimeClick: () -> Unit,
    turnOnAlarmOnActiveToggle: () -> Unit,
    turnOffAlarm: TetrisClockAlarm,
    turnOffAlarmOnTimeClick: () -> Unit,
    turnOffAlarmOnActiveToggle: () -> Unit)
{
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AlarmItem(
                    label = stringResource(R.string.mc_check_auto_on),
                    alarm = turnOnAlarm,
                    onTimeClick = turnOnAlarmOnTimeClick,
                    onActiveToggle = turnOnAlarmOnActiveToggle
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AlarmItem(
                    label = stringResource(R.string.mc_check_auto_off),
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
    label: String,
    alarm: TetrisClockAlarm,
    onTimeClick: () -> Unit,
    onActiveToggle: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = if (alarm.isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Switch(
                checked = alarm.isActive,
                onCheckedChange = { onActiveToggle() }
            )
        }

        AlarmTimeField(
            time = String.format("%02d:%02d", alarm.hours, alarm.minutes),
            isActive = alarm.isActive,
            onFieldClick = onTimeClick
        )
    }
}

@Composable
fun AlarmTimeField(
    time: String,
    isActive: Boolean,
    onFieldClick: () -> Unit
) {
    val borderColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }

    val textColor = if (isActive) {
        MaterialTheme.colorScheme.onSurface
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
                color = borderColor,
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = time,
                color = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                style = MaterialTheme.typography.headlineLarge
            )

            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = stringResource(R.string.mc_action_set_time),
                tint = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
    }
}

@Composable
@Preview(
    name = "State 1: Device ON",
    showBackground = true
)
fun DeviceSettings_State1_Preview(){
    TetrisClockBLETheme (
        darkTheme = false
    ) {
        DeviceSettings(
            deviceFriendlyName = "Часы в детской",
            deviceName = "TetrisClock YOG",
            deviceMacAddress = "1a:2b:3c:4d:5e:6f",
            isOn = true,
            onButtonSyncClickAction = {},
            manualBrightness = 8,
            onSliderBrightnessValueChanged = {},
            bleTime = TetrisClockTime(
                LocalDateTime.now()
                    .minusHours(1)
                    .minusMinutes(1)
                    .minusSeconds(1)
            ),
            phoneTime = TetrisClockTime(LocalDateTime.now()),
            onButtonOnOffClickAction = {},
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
            turnOffAlarmOnActiveToggle = {}
        )
    }
}

@Composable
@Preview(
    name = "State 2: Device OFF",
    showBackground = true)
fun DeviceSettings_State2_Preview(){
    TetrisClockBLETheme (
        darkTheme = true
    ) {
        DeviceSettings(
            deviceFriendlyName = "Часы в гостиной",
            deviceName = "TetrisClock OYO",
            deviceMacAddress = "11:22:33:44:55:66",
            isOn = false,
            onButtonSyncClickAction = {},
            manualBrightness = 3,
            onSliderBrightnessValueChanged = {},
            bleTime = TetrisClockTime(
                LocalDateTime.now()
                    .minusHours(1)
                    .minusMinutes(1)
                    .minusSeconds(1)
            ),
            phoneTime = TetrisClockTime(LocalDateTime.now()),
            onButtonOnOffClickAction = {},
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
            turnOffAlarmOnActiveToggle = {}
        )
    }
}
