package net.agolyakov.tetrisclockble.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import net.agolyakov.tetrisclockble.data.BleDeviceRepository
import net.agolyakov.tetrisclockble.model.BleDevice
import net.agolyakov.tetrisclockble.navigation.Screen
import net.agolyakov.tetrisclockble.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    navController: NavHostController,
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    // for getting list if no devices are nearby
    // val devices = homeViewModel.getDeviceRepository().getDeviceList()
    val devices by homeViewModel.devices.observeAsState(emptyList())

    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .fillMaxHeight()
            .systemBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        DeviceList(devices , navController)
    }
}

@Composable
fun DeviceList(deviceList: List<BleDevice>, navController: NavHostController) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        itemsIndexed(deviceList) {
            _, item -> Device(item, navController)
        }
    }
}

@Composable
fun Device(device: BleDevice, navController: NavHostController) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor =MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .padding(5.dp, 5.dp, 5.dp, 0.dp)
            .clickable {
                navController.currentBackStackEntry?.savedStateHandle?.set(
                    key = "device",
                    value = device
                )
                navController.navigate(Screen.Details.route)
            }
    ){
        Row(verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier
                .padding(24.dp)){
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Настроить",
                    modifier = Modifier
                        .size(36.dp))
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Row {
                    Text(
                        text = device.overrideName ?: device.deviceName,
                        style = MaterialTheme.typography.headlineSmall,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Row {
                    Column {
                        Text(
                            text = device.deviceMacAddr,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                    Column (modifier = Modifier.padding(horizontal = 10.dp)) {
                        Text(
                            text = "Signal: " + device.txPowerLevel,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun DeviceListPreview() {
    val deviceList = BleDeviceRepository().getDeviceList()
    DeviceList(deviceList, rememberNavController())
}
