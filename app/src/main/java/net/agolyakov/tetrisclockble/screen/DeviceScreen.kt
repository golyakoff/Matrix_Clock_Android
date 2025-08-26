package net.agolyakov.tetrisclockble.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import net.agolyakov.tetrisclockble.model.BleDevice
import net.agolyakov.tetrisclockble.viewmodel.DeviceViewModel
import net.agolyakov.tetrisclockble.R

@Composable
fun DeviceScreen(
    navController: NavHostController,
    device: BleDevice?,
) {
    val viewModel: DeviceViewModel = hiltViewModel()
    val isOn by viewModel.matrixClockIsOn.collectAsState()
    val manualBrightness by viewModel.matrixClockManualBrightness.collectAsState()

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
                onClick = { viewModel.toggleOnOffCharacteristic() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text =
                        if (isOn) stringResource(R.string.mc_turn_off_clock)
                        else stringResource(R.string.mc_turn_on_clock),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                // увеличим на 1 чтобы не смущать пользователя нулевой яркостью )
                text = "${stringResource(R.string.mc_manual_brightness)} : ${manualBrightness + 1}",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Slider(
                value = manualBrightness.toFloat(),
                onValueChange = { newValue ->
                    viewModel.setManualBrightnessCharacteristic(newValue.toInt().toByte())
                },
                valueRange = 0f..15f,
                steps = 14, // количество шагов = max - min - 1
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }
}
