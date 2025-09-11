package net.agolyakov.tetrisclockble.ui.screen.firmware

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirmwareUpdateScreen(
    viewModel: FirmwareUpdateViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Обновление прошивки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            is FirmwareUpdateState.Checking -> {
                CheckingForUpdates(padding)
            }
            is FirmwareUpdateState.UpdateAvailable -> {
                UpdateAvailableScreen(s, viewModel, padding)
            }
            is FirmwareUpdateState.Downloading -> {
                DownloadingScreen(s, padding)
            }
            is FirmwareUpdateState.ReadyToInstall -> {
                ReadyToInstallScreen(s, viewModel, padding)
            }
            is FirmwareUpdateState.Installing -> {
                InstallingScreen(s, padding)
            }
            is FirmwareUpdateState.Success -> {
                SuccessScreen(s, onBack, padding)
            }
            is FirmwareUpdateState.Error -> {
                ErrorScreen(s, viewModel, padding)
            }
            is FirmwareUpdateState.NoUpdate -> {
                NoUpdateScreen(onBack, padding)
            }
        }
    }
}