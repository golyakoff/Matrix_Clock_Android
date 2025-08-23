package net.agolyakov.tetrisclockble.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import net.agolyakov.tetrisclockble.model.BleDevice
import net.agolyakov.tetrisclockble.viewmodel.DeviceViewModel

@Composable
fun DeviceScreen(
    navController: NavHostController,
    device: BleDevice?,
) {
    val viewModel: DeviceViewModel = hiltViewModel()
    val isOn by viewModel.MatricClockisOn.collectAsState()

    LaunchedEffect(device) {
        // как только экран появился – просим VM подключиться
        viewModel.connectToDevice(device)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.disconnect()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Button(
                onClick = { viewModel.setOnOffCharacteristic() },
            ) {
                Text(
                    text =
                        if (isOn) "Погасить часы"
                        else "Включить часы",
                    style = MaterialTheme . typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
