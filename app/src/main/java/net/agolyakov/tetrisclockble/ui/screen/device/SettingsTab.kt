@file:OptIn(ExperimentalMaterial3Api::class)

package net.agolyakov.tetrisclockble.ui.screen.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PermDeviceInformation
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.agolyakov.tetrisclockble.R
import net.agolyakov.tetrisclockble.protocol.TimeZoneMapper
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockTime
import net.agolyakov.tetrisclockble.ui.theme.displayFontFamily
import net.agolyakov.tetrisclockble.ui.theme.timeHeadlineMedium

/** Rarely touched settings: time, WiFi, firmware, the panel and the clock's hardware. */
@Composable
fun SettingsTab(
    bleTime: TetrisClockTime,
    phoneTime: TetrisClockTime,
    onButtonSyncClickAction: () -> Unit,
    agingOffset: Int,
    onAgingOffsetDialogValueChanged: (Int) -> Unit,
    firmwareVersion: String,
    onFirmwareUpdateButtonClickAction: () -> Unit,
    flashSizeMb: Int,
    rtcTemperature: Float,
    isRrbbggColorOrder: Boolean,
    onColorOrderSelected: (Boolean) -> Unit,
    network: NetworkUiState,
    networkActions: NetworkActions
) {
    TimeSyncCard(
        modifier = Modifier,
        bleTime,
        phoneTime,
        onButtonSyncClickAction,
        agingOffset,
        onAgingOffsetDialogValueChanged,
        timeZone = network.timeZone
    )

    // Firmware v2.0.0+ only
    network.deviceName?.takeIf { network.supported }?.let { saved ->
        DeviceNameCard(
            saved = saved,
            saveFailed = network.deviceNameSaveFailed,
            actions = networkActions
        )
    }

    network.wifiSettings?.takeIf { network.supported }?.let { saved ->
        WifiCard(
            saved = saved,
            status = network.wifiStatus,
            scan = network.wifiScan,
            saveFailed = network.wifiSaveFailed,
            actions = networkActions
        )
    }

    FirmwareCard(
        firmwareVersion = firmwareVersion,
        network = network,
        actions = networkActions,
        onInstallOverBluetooth = onFirmwareUpdateButtonClickAction
    )

    SystemInfoCard(
        modifier = Modifier,
        flashSizeMb,
        rtcTemperature,
        isRrbbggColorOrder,
        onColorOrderSelected
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
    timeZone: String? = null,
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

            // The clock's time zone (firmware v2.0.0+): the app keeps it the phone's one
            timeZone?.let { zone ->
                Spacer(Modifier.height(12.dp))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.mc_time_zone, timeZoneLabel(zone)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
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
fun SystemInfoCard(
    modifier: Modifier,
    flashSizeMb: Int,
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

            // Physical flash size is only known on firmware v1.7.0+ (0 = not reported); hide the row
            // on older firmware rather than showing a meaningless "0 MB".
            if (flashSizeMb > 0) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.mc_clock_flash_size, flashSizeMb),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
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

/** A POSIX TZ string as people read it: "UTC+3", or "UTC+1, UTC+2 in summer". */
@Composable
private fun timeZoneLabel(posix: String): String {
    val offsets = TimeZoneMapper.parseOffsets(posix) ?: return posix
    fun utc(minutes: Int): String {
        val sign = if (minutes < 0) "-" else "+"
        val abs = kotlin.math.abs(minutes)
        return if (abs % 60 == 0) "UTC$sign${abs / 60}" else "UTC$sign${abs / 60}:%02d".format(abs % 60)
    }
    val standard = utc(offsets.standardMinutes)
    return offsets.daylightMinutes?.let { stringResource(R.string.mc_time_zone_dst, standard, utc(it)) } ?: standard
}
