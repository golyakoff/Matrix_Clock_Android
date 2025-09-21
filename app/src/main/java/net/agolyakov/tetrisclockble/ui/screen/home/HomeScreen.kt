package net.agolyakov.tetrisclockble.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import net.agolyakov.tetrisclockble.data.repository.DeviceRepository
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockDevice
import net.agolyakov.tetrisclockble.navigation.Screen
import net.agolyakov.tetrisclockble.ui.theme.TetrisClockBLETheme

@Composable
fun HomeScreen(
    navController: NavHostController,
    homeViewModel: HomeViewModel
) {
    // for getting list if no devices are nearby
    // val devices = homeViewModel.getDeviceRepository().getDeviceList()
    val devices by homeViewModel.devices.observeAsState(emptyList())

    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .fillMaxSize()
            .systemBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        DeviceList(devices , navController)
    }
}

@Composable
fun DeviceList(deviceList: List<TetrisClockDevice>, navController: NavHostController) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        itemsIndexed(deviceList) {
            _, item -> Device(item, navController)
        }
    }
}

@Composable
fun Device(device: TetrisClockDevice, navController: NavHostController) {
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
                modifier = Modifier
                    .size(48.dp)
            )

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        modifier = Modifier,
                        text = device.friendlyName ?: device.deviceName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = device.macAddress,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.outline
                )

            }
        }
    }
}

@Composable
@Preview(
    name = "Light Schema",
    heightDp = 800,
    showBackground = false)
fun DeviceListPreview_1() {
    TetrisClockBLETheme(darkTheme = false) {
        val deviceList = DeviceRepository().getDeviceList()
        DeviceList(deviceList, rememberNavController())
    }
}

@Composable
@Preview(
    name = "Dark Schema",
    heightDp = 800,
    showBackground = true)
fun DeviceListPreview_2() {
    TetrisClockBLETheme(darkTheme = true) {
        val deviceList = DeviceRepository().getDeviceList()
        DeviceList(deviceList, rememberNavController())
    }
}