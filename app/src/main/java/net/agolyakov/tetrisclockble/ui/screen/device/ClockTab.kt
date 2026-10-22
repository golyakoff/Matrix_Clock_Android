@file:OptIn(ExperimentalMaterial3Api::class)

package net.agolyakov.tetrisclockble.ui.screen.device

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import net.agolyakov.tetrisclockble.R
import net.agolyakov.tetrisclockble.data.model.ble.SplashAnimation
import net.agolyakov.tetrisclockble.data.model.ble.SplashDuration
import net.agolyakov.tetrisclockble.data.model.ble.SplashMode
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockAlarm
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockHourlyBrightness
import net.agolyakov.tetrisclockble.ui.theme.displayFontFamily
import net.agolyakov.tetrisclockble.ui.theme.timeHeadlineMedium

/** Everyday controls: power, brightness, on/off scenarios, Animation Splash. */
@Composable
fun ClockTab(
    isOn: Boolean,
    onSwitchOnOffCheckedChangeAction: (Boolean) -> Unit,
    manualBrightness: Byte,
    onSliderBrightnessValueChanged: (Float) -> Unit,
    isAutoBrightness: Boolean,
    onAutoBrightnessCheckedChangeAction: (Boolean) -> Unit,
    hourlyBrightness: TetrisClockHourlyBrightness,
    onHourlyBrightnessConfirmed: (TetrisClockHourlyBrightness) -> Unit,
    currentHour: Int,
    turnOnAlarm: TetrisClockAlarm,
    turnOnAlarmOnTimeClick: () -> Unit,
    turnOnAlarmOnActiveToggle: () -> Unit,
    turnOffAlarm: TetrisClockAlarm,
    turnOffAlarmOnTimeClick: () -> Unit,
    turnOffAlarmOnActiveToggle: () -> Unit,
    splashEnabled: Boolean,
    splashMode: Int,
    splashDuration: Int,
    onSplashEnabledChange: (Boolean) -> Unit,
    onSplashModeSelected: (Int) -> Unit,
    onSplashDurationSelected: (Int) -> Unit,
    splashAnimations: List<SplashAnimation>,
    splashSelectedIndex: Int,
    onSplashAnimationSelected: (Int) -> Unit,
    onSplashPreview: () -> Unit
) {
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
        currentHour = currentHour,
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
                imageVector = Icons.Default.Movie,
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

                SplashAnimationGallery(
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

// A gallery of animated GIF tiles, one per animation, all playing at once so you can see what you
// are choosing before you pick it. Tapping a tile selects that animation; the selected one is
// framed. Replaces the old plain-text dropdown.

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SplashAnimationGallery(
    animations: List<SplashAnimation>,
    selectedIndex: Int,
    onAnimationSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    // Two loaders: the one WITH a GIF decoder animates (used only for the selected tile), the plain
    // one has no GIF decoder so Coil decodes just the first frame (used for the non-selected tiles).
    // ImageDecoderDecoder needs API 28, which the app's minSdk guarantees; GifDecoder is a fallback.
    val gifImageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
    val staticImageLoader = remember { ImageLoader.Builder(context).build() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.mc_splash_animation_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            animations.forEach { animation ->
                val selected = animation.index == selectedIndex
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(140.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onAnimationSelected(animation.index) }
                        .padding(6.dp)
                ) {
                    // key(selected) recreates the AsyncImage when selection flips, forcing a fresh
                    // load with the right loader. Without it, changing only imageLoader doesn't
                    // reload (Coil keys on the model), so the old tile kept animating and the newly
                    // selected one stayed on its static frame.
                    key(selected) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(animation.gifRes)
                                .build(),
                            // Selected tile animates; the rest show only their first frame.
                            imageLoader = if (selected) gifImageLoader else staticImageLoader,
                            contentDescription = stringResource(animation.nameRes),
                            // Nearest-neighbour scaling keeps the 64x32 pixel art crisp when enlarged.
                            filterQuality = FilterQuality.None,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f) // the panel is 64x32
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black)
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = stringResource(animation.nameRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
