package net.agolyakov.tetrisclockble.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import net.agolyakov.tetrisclockble.model.BleDevice

@Composable
fun DetailsScreen(
    navController: NavHostController,
    device: BleDevice?
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text =
                if (device == null) "null"
                else "${device.overrideName ?: device.mfrName}\n(${device.macAddress})",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
@Preview(showBackground = true)
fun DetailsScreenPreview() {
    DetailsScreen(
        rememberNavController(),
        BleDevice(
            mfrName = "Mfr name",
            overrideName = "Моё устройство",
            macAddress = "00:00:00:00:00:ff"
        ))
}
