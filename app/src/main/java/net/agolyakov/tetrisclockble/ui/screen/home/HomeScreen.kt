package net.agolyakov.tetrisclockble.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import net.agolyakov.tetrisclockble.BuildConfig
import net.agolyakov.tetrisclockble.R
import net.agolyakov.tetrisclockble.data.repository.DeviceRepository
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockDevice
import net.agolyakov.tetrisclockble.navigation.Screen
import net.agolyakov.tetrisclockble.ui.theme.TetrisClockBLETheme

@Composable
fun HomeScreen(
    navController: NavHostController,
    homeViewModel: HomeViewModel
) {
    val devices by homeViewModel.devices.observeAsState(emptyList())

    var showRenameDialog by remember { mutableStateOf(false) }
    var deviceToRename by remember { mutableStateOf<TetrisClockDevice?>(null) }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        DeviceList(
            devices,
            navController,
            modifier = Modifier.weight(1f),
            onRenameClick = { device ->
                deviceToRename = device
                showRenameDialog = true
            }
        )

        AppVersionPlaque(appVersion = BuildConfig.VERSION_NAME)
    }

    if (showRenameDialog && deviceToRename != null) {
        RenameDeviceDialog(
            device = deviceToRename!!,
            onDismiss = {
                showRenameDialog = false
                deviceToRename = null
            },
            onRename = { newName ->
                homeViewModel.renameDevice(deviceToRename!!.macAddress, newName)
            }
        )
    }
}

@Composable
fun DeviceList(
    deviceList: List<TetrisClockDevice>,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onRenameClick: (TetrisClockDevice) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        itemsIndexed(deviceList) { _, item ->
            Device(item, navController, onRenameClick)
        }
    }
}

@Composable
fun AppVersionPlaque(
    appVersion: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp, 8.dp, 16.dp, 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.width(12.dp))

            Text(
                text = stringResource(R.string.mc_app_version, appVersion),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun Device(
    device: TetrisClockDevice,
    navController: NavHostController,
    onRenameClick: (TetrisClockDevice) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .padding(16.dp, 16.dp, 16.dp, 0.dp)
            .clickable {
                navController.currentBackStackEntry?.savedStateHandle?.set(
                    key = "device",
                    value = device
                )
                navController.navigate(Screen.Device.route)
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 24.dp)
        ) {
            Icon(
                Icons.Outlined.AccessTime,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = "Настроить",
                modifier = Modifier.size(48.dp)
            )

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.getDisplayName(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = device.macAddress,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            IconButton(
                onClick = { onRenameClick(device) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.mc_rename),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun RenameDeviceDialog(
    device: TetrisClockDevice,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var text by remember { mutableStateOf(device.friendlyName ?: device.deviceName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mc_rename_device)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.mc_dialog_device_name)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onRename(text)
                    } else {
                        onRename("")
                    }
                    onDismiss()
                }
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

@Composable
@Preview(name = "Light Schema", heightDp = 800, showBackground = false)
fun DeviceListPreview_1() {
    TetrisClockBLETheme(darkTheme = false) {
        val deviceList = DeviceRepository().getDeviceList()
        DeviceList(deviceList, rememberNavController()) {}
    }
}

@Composable
@Preview(name = "Dark Schema", heightDp = 800, showBackground = true)
fun DeviceListPreview_2() {
    TetrisClockBLETheme(darkTheme = true) {
        val deviceList = DeviceRepository().getDeviceList()
        DeviceList(deviceList, rememberNavController()) {}
    }
}
