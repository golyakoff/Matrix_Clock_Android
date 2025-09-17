@file:OptIn(ExperimentalMaterial3Api::class)

package net.agolyakov.tetrisclockble.ui.screen.device

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockDevice
import net.agolyakov.tetrisclockble.R
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockAlarmType
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockTime
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockAlarm
import net.agolyakov.tetrisclockble.ui.component.TimePickerDialogState
import net.agolyakov.tetrisclockble.ui.theme.TetrisClockBLETheme
import java.time.LocalDateTime

@Composable
fun DeviceScreen(
    navController: NavHostController,
    device: TetrisClockDevice?
) {
    val viewModel: DeviceViewModel = hiltViewModel()
    val isOn: Boolean by viewModel.tetrisClockTetrisOn.collectAsState()
    val manualBrightness: Byte by viewModel.tetrisClockManualBrightness.collectAsState()
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
                viewModel.setAlarmTime(hour, minute, timePickerState.isActive)
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
        deviceFriendlyName = device?.friendlyName ?: device?.deviceName ?: "<без имени>",
        deviceMacAddress = device?.macAddress ?: "<без адреса?",
        firmwareVersion = "v1.0.0",
        isOn = isOn,
        onButtonOnOffClickAction = { viewModel.toggleOnOffCharacteristic() },
        manualBrightness = manualBrightness,
        onSliderBrightnessValueChanged = {
            newValue -> viewModel.setManualBrightnessCharacteristic(newValue.toInt().toByte())
        },
        bleTime = bleTime,
        phoneTime = phoneTime,
        onButtonSyncClickAction = { viewModel.syncBleWithPhone() },
        agingOffset = agingOffset,
        onSpinnerAgingOffsetValueChanged = {
            newValue -> viewModel.setAgingOffsetCharacteristic(newValue)
        },
        rtcTemperature = rtcTemperature,
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
        },
        onFirmwareUpdateButtonClickAction = {
            navController.navigate("firmware_update")
        }
    )
}

@Composable
fun DeviceSettings(
    deviceFriendlyName: String,
    deviceMacAddress: String,
    firmwareVersion: String,
    isOn: Boolean,
    onButtonOnOffClickAction: () -> Unit,
    manualBrightness: Byte,
    onSliderBrightnessValueChanged: (Float) -> Unit,
    bleTime: TetrisClockTime,
    phoneTime: TetrisClockTime,
    onButtonSyncClickAction: () -> Unit,
    agingOffset: Int,
    onSpinnerAgingOffsetValueChanged: (Int) -> Unit,
    rtcTemperature: Float,
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
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // ← Добавляем прокрутку здесь!
            .systemBarsPadding()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderCard(
            deviceFriendlyName,
            deviceMacAddress,
            firmwareVersion
        )

        Spacer(Modifier.height(20.dp))

        TimeSyncCard(
            bleTime,
            phoneTime,
            onButtonSyncClickAction,
            agingOffset,
            onSpinnerAgingOffsetValueChanged
        )

        Spacer(Modifier.height(30.dp))

        OnOffAlarms(
            turnOnAlarm,
            turnOnAlarmOnTimeClick,
            turnOnAlarmOnActiveToggle,
            turnOffAlarm,
            turnOffAlarmOnTimeClick,
            turnOffAlarmOnActiveToggle
        )

        Spacer(Modifier.height(20.dp))

        OnOffButton(
            isOn,
            onButtonOnOffClickAction,
        )

        Spacer(Modifier.height(30.dp))

        BrightnessSlider(
            manualBrightness,
            onSliderBrightnessValueChanged,
        )

        Spacer(Modifier.height(20.dp))

        FirmwareUpdateButton(
            onFirmwareUpdateButtonClickAction
        )

        Spacer(Modifier.height(10.dp))

        RtcTemperature(rtcTemperature)
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
fun AgingOffsetSpinner(
    currentValue: Int,
    onSpinnerAgingOffsetValueChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
       Box(
            modifier = Modifier
                .clickable { showDialog = true }
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                    text = "${stringResource(R.string.mc_aging_offset)}: ${if (currentValue > 0) "+" else ""
                    }${ "%.2f".format(currentValue * 0.1)} ${stringResource(R.string.mc_ppm)}",
           style = MaterialTheme.typography.bodyLarge,
           color = MaterialTheme.colorScheme.primary
           )
        }
    }

    // Диалог для ручного ввода
    if (showDialog) {
        var inputValue by remember(currentValue) { mutableStateOf(currentValue.toString()) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
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
                        label = { Text("Значение от -128 до 127") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newValue = inputValue.toIntOrNull()?.coerceIn(-128..127)
                        newValue?.let { onSpinnerAgingOffsetValueChanged(it) }
                        showDialog = false
                    }
                ) {
                    Text(stringResource(R.string.mc_dialog_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.mc_dialog_cancel))
                }
            }
        )
    }
}

@Composable
fun RtcTemperature(
    rtcTemperature: Float
) {
    Text(
        text = "${stringResource(R.string.mc_rtc_temperature)}: ${if (rtcTemperature > 0) "+" else ""
        }${"%.2f".format(rtcTemperature)} ${stringResource(R.string.mc_degrees_celsius)}",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun TimeSyncCard(
    bleTime: TetrisClockTime,
    phoneTime: TetrisClockTime,
    onSyncClick: () -> Unit,
    agingOffset: Int,
    onSpinnerAgingOffsetValueChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(start = 12.dp,top = 12.dp, end = 12.dp, bottom = 0.dp )
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Карточки времени в строку
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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

            Spacer(Modifier.height(8.dp))

            // Кнопка синхронизации
            Button(
                onClick = onSyncClick,
                modifier = Modifier
                    .padding(start = 8.dp, top = 0.dp, end = 8.dp, bottom = 12.dp)
                    .fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.mc_action_synchronize_time),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onSyncClick,
                modifier = Modifier
                    .padding(start = 8.dp, top = 0.dp, end = 8.dp, bottom = 12.dp)
                    .fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.mc_action_synchronize_time),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            AgingOffsetSpinner(
                agingOffset,
                onSpinnerAgingOffsetValueChanged
            )
        }
    }
}

@Composable
fun TimeBlock(
    header: String,
    time: TetrisClockTime,
    modifier: Modifier
) {
    Column (
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Text(
            text = header,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = time.formatTime(),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = time.formatDate(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
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
                imageVector = Icons.Default.Timelapse,
                contentDescription = stringResource(R.string.mc_action_set_time),
                tint = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
    }
}

@Composable
fun FirmwareUpdateButton(
    onButtonClick: () -> Unit
) {
    Button(
        onClick = onButtonClick,
        modifier = Modifier.padding(16.dp)
    ) {
        Text("Обновление прошивки устройства")
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
            deviceMacAddress = "1a:2b:3c:4d:5e:6f",
            firmwareVersion = "v1.0.1-rc6",
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
            agingOffset = 5,
            onSpinnerAgingOffsetValueChanged = {},
            rtcTemperature = 21.25F,
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
    name = "State 2: Device OFF",
    showBackground = true)
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
            bleTime = TetrisClockTime(
                LocalDateTime.now()
                    .minusHours(1)
                    .minusMinutes(1)
                    .minusSeconds(1)
            ),
            phoneTime = TetrisClockTime(LocalDateTime.now()),
            onButtonOnOffClickAction = {},
            agingOffset = -108,
            onSpinnerAgingOffsetValueChanged = {},
            rtcTemperature = -10.25F,
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

@Composable
fun HeaderCard(
    friendlyName: String,
    macAddress: String,
    firmwareVersion: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
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
            // Иконка устройства
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Matrix Clock",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(16.dp))

            // Название устройства
            Text(
                text = friendlyName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            // Детали устройства
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = macAddress,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = firmwareVersion,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
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
        modifier = modifier
        .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Заголовок карточки
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(8.dp))

            // Время крупно
            Text(
                text = time.formatTime(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Дата под временем
            Text(
                text = time.formatDate(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}