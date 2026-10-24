@file:OptIn(ExperimentalMaterial3Api::class)

package net.agolyakov.tetrisclockble.ui.screen.device

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SignalWifi0Bar
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Wifi1Bar
import androidx.compose.material.icons.filled.Wifi2Bar
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.agolyakov.tetrisclockble.R
import net.agolyakov.tetrisclockble.protocol.NetworkCodec
import net.agolyakov.tetrisclockble.protocol.UpdateCheckState
import net.agolyakov.tetrisclockble.protocol.UpdateInfo
import net.agolyakov.tetrisclockble.protocol.WifiAuth
import net.agolyakov.tetrisclockble.protocol.WifiMode
import net.agolyakov.tetrisclockble.protocol.WifiNetwork
import net.agolyakov.tetrisclockble.protocol.WifiScan
import net.agolyakov.tetrisclockble.protocol.WifiScanState
import net.agolyakov.tetrisclockble.protocol.WifiSettings
import net.agolyakov.tetrisclockble.protocol.WifiSettingsUpdate
import net.agolyakov.tetrisclockble.protocol.WifiState
import net.agolyakov.tetrisclockble.protocol.WifiStatus
import net.agolyakov.tetrisclockble.protocol.utf8Length
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Cards of the Settings tab for firmware v2.0.0+: WiFi, and the firmware update with its two ways
// (over WiFi by the clock itself, or over Bluetooth from the phone)

private val WIFI_PERIODS = listOf(1, 2, 3, 6, 12, 24)

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
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
            content()
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline
    )
}

@Composable
private fun InfoLine(text: String, isError: Boolean = false) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    )
}

private fun formatEpoch(epochSeconds: Long): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .format(Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()))

// ---------- Device name ----------

@Composable
fun DeviceNameCard(
    saved: String,
    saveFailed: Boolean,
    actions: NetworkActions
) {
    var name by rememberSaveable(saved) { mutableStateOf(saved) }
    // Empty restores the default "Tetris Clock XXXX"
    val valid = NetworkCodec.isValidDeviceName(name.trim())
    val changed = name.trim() != saved

    SettingsCard {
        CardTitle(Icons.Default.Bluetooth, stringResource(R.string.mc_device_name))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.mc_device_name_label)) },
            supportingText = { Text(stringResource(R.string.mc_device_name_limit, name.trim().utf8Length(), NetworkCodec.DEVICE_NAME_MAX_BYTES)) },
            isError = !valid,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))
        Hint(stringResource(R.string.mc_device_name_hint))

        if (saveFailed) {
            Spacer(Modifier.height(8.dp))
            InfoLine(stringResource(R.string.mc_device_name_save_failed), isError = true)
        }

        if (changed) {
            Spacer(Modifier.height(16.dp))
            CardButtonWide(
                Icons.Default.Save,
                stringResource(R.string.mc_wifi_save),
                { if (valid) actions.saveDeviceName(name) },
                true
            )
        }
    }
}

// ---------- WiFi ----------

@Composable
fun WifiCard(
    saved: WifiSettings,
    status: WifiStatus?,
    scan: WifiScan?,
    saveFailed: Boolean,
    saving: Boolean,
    actions: NetworkActions
) {
    // A draft of the whole characteristic, reset when the clock's value comes back
    var enabled by rememberSaveable(saved) { mutableStateOf(saved.mode != WifiMode.OFF) }
    var period by rememberSaveable(saved) { mutableStateOf(saved.periodHours.coerceIn(WifiSettings.PERIOD_RANGE)) }
    var ssid by rememberSaveable(saved) { mutableStateOf(saved.ssid) }
    // Never saved into the instance state: it is a secret
    var password by remember(saved) { mutableStateOf("") }
    // An open network picked from the scan: the stored password is to be cleared
    var pickedOpen by rememberSaveable(saved) { mutableStateOf(false) }
    var showPicker by rememberSaveable { mutableStateOf(false) }

    val ssidValid = ssid.utf8Length() <= WifiSettings.MAX_SSID_BYTES && (!enabled || ssid.isNotEmpty())
    val passwordValid = password.utf8Length() <= WifiSettings.MAX_PASSWORD_BYTES
    val mode = if (enabled) WifiMode.PERIODIC else WifiMode.OFF
    val changed = mode != saved.mode || period != saved.periodHours || ssid != saved.ssid ||
        password.isNotEmpty() || (pickedOpen && saved.passwordStored)

    SettingsCard {
        CardTitle(
            Icons.Default.Wifi,
            stringResource(R.string.mc_wifi),
            showSwitch = true,
            isOn = enabled,
            onCheckedChange = { if (!saving) enabled = it }
        )

        Hint(stringResource(if (enabled) R.string.mc_wifi_periodic_hint else R.string.mc_wifi_off_hint))

        if (saved.mode != WifiMode.OFF) {
            Spacer(Modifier.height(8.dp))
            status?.let { WifiStatusLines(it) }
        }

        if (enabled) {
            Spacer(Modifier.height(12.dp))
            WifiPeriodPicker(period) { period = it }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = ssid,
                onValueChange = { ssid = it; pickedOpen = false },
                enabled = !saving,
                label = { Text(stringResource(R.string.mc_wifi_ssid)) },
                supportingText = { Text(stringResource(R.string.mc_wifi_band_hint)) },
                isError = !ssidValid,
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = {
                        showPicker = true
                        if (scan?.state != WifiScanState.SCANNING) actions.scanWifi()
                    }) {
                        Icon(Icons.Default.WifiFind, contentDescription = stringResource(R.string.mc_wifi_picker_title))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (!pickedOpen) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    enabled = !saving,
                    label = { Text(stringResource(R.string.mc_wifi_password)) },
                    placeholder = {
                        Text(stringResource(if (saved.passwordStored) R.string.mc_wifi_password_kept else R.string.mc_wifi_password_none))
                    },
                    isError = !passwordValid,
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(8.dp))
            Hint(stringResource(R.string.mc_wifi_pairing_hint))
        }

        if (saveFailed && !saving) {
            Spacer(Modifier.height(8.dp))
            InfoLine(stringResource(R.string.mc_wifi_save_failed), isError = true)
        }

        if (saving) {
            // Bonding takes a few seconds before Android shows its pairing prompt
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.mc_wifi_saving),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else if (changed) {
            Spacer(Modifier.height(16.dp))
            CardButtonWide(
                Icons.Default.Save,
                stringResource(R.string.mc_wifi_save),
                {
                    if (ssidValid && passwordValid) {
                        // An empty field keeps the stored password; an open network picked from
                        // the scan clears it
                        val newPassword = when {
                            pickedOpen -> ""
                            password.isNotEmpty() -> password
                            else -> null
                        }
                        actions.saveWifiSettings(WifiSettingsUpdate(mode, period, ssid, newPassword))
                    }
                },
                true
            )
        } else if (saved.mode != WifiMode.OFF) {
            Spacer(Modifier.height(16.dp))
            CardButtonWide(
                Icons.Default.Sync,
                stringResource(R.string.mc_wifi_sync_now),
                actions::syncTimeFromInternet,
                false
            )
        }
    }

    if (showPicker) {
        WifiNetworkPicker(
            scan = scan,
            onRescan = actions::scanWifi,
            onPick = { network ->
                ssid = network.ssid
                pickedOpen = !network.needsPassword
                if (pickedOpen) password = ""
                showPicker = false
            },
            onManual = { showPicker = false },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun WifiStatusLines(status: WifiStatus) {
    when (status.state) {
        WifiState.CONNECTING -> InfoLine(stringResource(R.string.mc_wifi_state_connecting))
        WifiState.CONNECTED -> InfoLine(stringResource(R.string.mc_wifi_state_connected, status.ip ?: "", status.rssi))
        WifiState.ERROR -> InfoLine(stringResource(R.string.mc_wifi_state_error), isError = true)
        else -> Unit
    }
    InfoLine(
        status.syncedAtEpochSeconds?.let { stringResource(R.string.mc_wifi_synced_at, formatEpoch(it)) }
            ?: stringResource(R.string.mc_wifi_never_synced)
    )
}

@Composable
private fun WifiPeriodPicker(period: Int, onSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.mc_wifi_period),
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
                    text = stringResource(R.string.mc_wifi_period_hours, period),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                (WIFI_PERIODS + period).distinct().sorted().forEach { hours ->
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.mc_wifi_period_hours, hours)) },
                        onClick = {
                            expanded = false
                            onSelected(hours)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Networks the clock hears, strongest first: the clock scans itself, so only 2.4 GHz networks it
 * can actually join (with their signal where the clock stands) are listed. Ported from FireLamp.
 */
@Composable
fun WifiNetworkPicker(
    scan: WifiScan?,
    onRescan: () -> Unit,
    onPick: (WifiNetwork) -> Unit,
    onManual: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scanning = scan?.state == WifiScanState.SCANNING
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 24.dp, end = 12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.mc_wifi_picker_title), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.mc_wifi_band_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Box(contentAlignment = Alignment.Center) {
                    if (scanning) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(12.dp))
                    } else {
                        IconButton(onClick = onRescan) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.mc_wifi_picker_rescan))
                        }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

            val networks = scan?.networks.orEmpty()
            when {
                networks.isEmpty() && scanning -> PickerMessage(stringResource(R.string.mc_wifi_picker_scanning))
                networks.isEmpty() && scan?.state == WifiScanState.ERROR -> PickerMessage(stringResource(R.string.mc_wifi_picker_error))
                networks.isEmpty() && scan?.state == WifiScanState.DONE -> PickerMessage(stringResource(R.string.mc_wifi_picker_empty))
            }
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(networks, key = { it.ssid }) { network ->
                    NetworkRow(network) { onPick(network) }
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onManual)
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(R.string.mc_wifi_picker_manual), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkRow(network: WifiNetwork, onClick: () -> Unit) {
    val usable = network.usable
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = usable, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Icon(
            signalIcon(network.rssi),
            contentDescription = null,
            tint = if (usable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                network.ssid,
                style = MaterialTheme.typography.bodyLarge,
                color = if (usable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
            )
            val detail = when (network.auth) {
                WifiAuth.OPEN -> stringResource(R.string.mc_wifi_auth_open)
                WifiAuth.ENTERPRISE -> stringResource(R.string.mc_wifi_auth_enterprise)
                else -> null
            }
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        // Fixed slots: the lock and the signal stay in columns down the list
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
            if (network.needsPassword) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
            }
        }
        Text(
            stringResource(R.string.mc_unit_dbm, network.rssi),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.End,
            modifier = Modifier.width(56.dp)
        )
    }
}

@Composable
private fun PickerMessage(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
    )
}

/** Signal bars, roughly as Android shows them. */
private fun signalIcon(rssi: Int): ImageVector = when {
    rssi >= -60 -> Icons.Default.Wifi
    rssi >= -72 -> Icons.Default.Wifi2Bar
    rssi >= -85 -> Icons.Default.Wifi1Bar
    else -> Icons.Default.SignalWifi0Bar
}

// ---------- Firmware ----------

/**
 * The installed version and the update: the phone checks GitHub; with a newer release the user
 * picks how to install it - the clock downloads it over WiFi (firmware v2.0.0+ with WiFi set up) or
 * the phone sends it over Bluetooth (the firmware screen).
 */
@Composable
fun FirmwareCard(
    firmwareVersion: String,
    network: NetworkUiState,
    actions: NetworkActions,
    onInstallOverBluetooth: () -> Unit
) {
    val clockCanUpdate = network.supported && network.wifiConfigured
    val phoneFound = network.firmwareCheck is FirmwareCheck.Available
    // The clock checks its own manifest: it may find an update the phone doesn't offer (its build
    // is not in the latest release, a debug build)
    val clockFound = clockCanUpdate && network.updateInfo?.state == UpdateCheckState.AVAILABLE

    SettingsCard {
        CardTitle(
            Icons.Default.Memory,
            stringResource(R.string.mc_firmware)
        )

        InfoLine(stringResource(R.string.mc_clock_firmware_version, firmwareVersion))

        when (val check = network.firmwareCheck) {
            FirmwareCheck.Checking -> InfoLine(stringResource(R.string.mc_fw_checking))
            FirmwareCheck.UpToDate -> InfoLine(stringResource(R.string.mc_fw_up_to_date))
            is FirmwareCheck.Available -> InfoLine(stringResource(R.string.mc_fw_available, check.version))
            FirmwareCheck.Failed -> InfoLine(stringResource(R.string.mc_fw_check_failed), isError = true)
            FirmwareCheck.Idle -> Unit
        }
        if (network.supported) {
            network.updateInfo?.let { ClockCheckLine(it) }
        }

        network.betaUpdates?.takeIf { network.supported }?.let { beta ->
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.mc_fw_beta),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(checked = beta, onCheckedChange = actions::setBetaUpdates)
            }
            Hint(stringResource(R.string.mc_fw_beta_hint))
        }

        Spacer(Modifier.height(16.dp))

        if (phoneFound || clockFound) {
            if (clockCanUpdate) {
                CardButtonWide(
                    Icons.Default.Wifi,
                    stringResource(R.string.mc_fw_install_wifi),
                    actions::installFirmwareByWifi,
                    true
                )
                Spacer(Modifier.height(12.dp))
            }
            if (phoneFound) {
                CardButtonWide(
                    Icons.Default.Bluetooth,
                    stringResource(R.string.mc_fw_install_ble),
                    onInstallOverBluetooth,
                    !clockCanUpdate
                )
                Spacer(Modifier.height(8.dp))
                Hint(stringResource(if (clockCanUpdate) R.string.mc_fw_hint_both else R.string.mc_fw_hint_phone_only))
            }
        } else {
            CardButtonWide(
                Icons.Default.SystemUpdateAlt,
                stringResource(R.string.mc_fw_check),
                actions::checkFirmware,
                false
            )
        }
    }

    network.wifiUpdate?.let { WifiUpdateDialog(it, actions::dismissWifiUpdate) }
}

@Composable
private fun ClockCheckLine(info: UpdateInfo) {
    val text = when (info.state) {
        UpdateCheckState.AVAILABLE -> stringResource(R.string.mc_fw_clock_found, info.latestVersion)
        UpdateCheckState.UP_TO_DATE -> stringResource(R.string.mc_fw_clock_up_to_date)
        UpdateCheckState.CHECKING -> stringResource(R.string.mc_fw_clock_checking)
        UpdateCheckState.FAILED -> stringResource(R.string.mc_fw_clock_check_failed)
        else -> return
    }
    Hint(text)
}

/** Progress of the update the clock installs by itself; closing it does not stop the clock. */
@Composable
private fun WifiUpdateDialog(update: WifiUpdate, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (update.finished) onDismiss() },
        icon = { Icon(Icons.Default.SystemUpdateAlt, contentDescription = null) },
        title = { Text(stringResource(R.string.mc_fw_install_wifi)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (update) {
                    WifiUpdate.Checking -> {
                        Text(stringResource(R.string.mc_fw_progress_clock_checking))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    is WifiUpdate.Downloading -> {
                        Text(stringResource(R.string.mc_fw_progress_clock_downloading))
                        if (update.total > 0) {
                            LinearProgressIndicator(
                                progress = { (update.received.toFloat() / update.total).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                stringResource(R.string.mc_fw_progress_amount, (update.received / 1024).toString(), (update.total / 1024).toString()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                    WifiUpdate.Rebooting -> {
                        Text(stringResource(R.string.mc_fw_progress_rebooting))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    is WifiUpdate.Done -> Text(stringResource(R.string.mc_fw_done, update.version))
                    is WifiUpdate.RolledBack -> Text(stringResource(R.string.mc_fw_rolled_back, update.version))
                    is WifiUpdate.Failed -> Text(stringResource(update.message), color = MaterialTheme.colorScheme.error)
                }
                if (!update.finished) {
                    Text(
                        stringResource(R.string.mc_fw_progress_continues),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.mc_dialog_close))
            }
        }
    )
}
