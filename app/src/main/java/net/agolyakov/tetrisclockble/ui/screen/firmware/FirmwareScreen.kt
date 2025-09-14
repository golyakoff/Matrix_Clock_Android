package net.agolyakov.tetrisclockble.ui.screen.firmware

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.agolyakov.tetrisclockble.data.repository.FirmwareRepository
import java.io.File

@Composable
fun FirmwareScreen(
    viewModel: FirmwareViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with back button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Обновление прошивки",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val currentState = state) {
            is FirmwareRepository.UpdateState.Idle -> {
                IdleScreen(viewModel)
            }
            is FirmwareRepository.UpdateState.Checking -> {
                CheckingScreen()
            }
            is FirmwareRepository.UpdateState.Processing -> {
                ProcessingScreen(progress, statusMessage)
            }
            is FirmwareRepository.UpdateState.UpdateAvailable -> {
                UpdateAvailableScreen(currentState, viewModel)
            }
            is FirmwareRepository.UpdateState.NoUpdate -> {
                NoUpdateScreen(currentState, onBack)
            }
            is FirmwareRepository.UpdateState.Downloading -> {
                DownloadingScreen(progress, statusMessage)
            }
            is FirmwareRepository.UpdateState.ReadyToInstall -> {
                ReadyToInstallScreen(currentState, viewModel)
            }
            is FirmwareRepository.UpdateState.Installing -> {
                InstallingScreen(progress, statusMessage)
            }
            is FirmwareRepository.UpdateState.Success -> {
                SuccessScreen(onBack)
            }
            is FirmwareRepository.UpdateState.Error -> {
                ErrorScreen(currentState, viewModel, onBack)
            }
        }
    }
}

@Composable
private fun IdleScreen(viewModel: FirmwareViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Проверка обновлений прошивки",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.checkForUpdates() },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Проверить обновления")
        }
    }
}

@Composable
private fun CheckingScreen() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text("Проверяем доступность обновлений...")
    }
}

@Composable
private fun UpdateAvailableScreen(
    state: FirmwareRepository.UpdateState.UpdateAvailable,
    viewModel: FirmwareViewModel
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Доступно обновление!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Текущая версия: ${state.currentVersion}")
        Text("Новая версия: ${state.release.tagName}")

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.performCompleteUpdate() },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Обновить автоматически")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.downloadFirmware(state.release) },
            modifier = Modifier.fillMaxWidth(0.8f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Text("Только скачать")
        }
    }
}

@Composable
private fun NoUpdateScreen(
    state: FirmwareRepository.UpdateState.NoUpdate,
    onBack: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Up to date",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Прошивка актуальна",
            style = MaterialTheme.typography.headlineMedium
        )

        Text("Версия: ${state.currentVersion}")

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onBack) {
            Text("Назад")
        }
    }
}

@Composable
private fun DownloadingScreen(
    progress: Float,
    statusMessage: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(progress = { progress / 100f })
        Spacer(modifier = Modifier.height(16.dp))
        Text("Скачивание: ${progress.toInt()}%")
        Text(statusMessage, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ReadyToInstallScreen(
    state: FirmwareRepository.UpdateState.ReadyToInstall,
    viewModel: FirmwareViewModel
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Прошивка готова к установке",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Версия: ${state.release.tagName}")
        Text("Размер: ${state.file.length() / 1024} KB")

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.installFirmware(state.file) },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Установить прошивку")
        }
    }
}

@Composable
private fun InstallingScreen(
    progress: Float,
    statusMessage: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(progress = { progress / 100f })
        Spacer(modifier = Modifier.height(16.dp))
        Text("Установка: ${progress.toInt()}%")
        Text(statusMessage, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ProcessingScreen(
    progress: Float,
    statusMessage: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(progress = { progress / 100f })
        Spacer(modifier = Modifier.height(16.dp))
        Text("Выполняется обновление...")
        Text("${progress.toInt()}%")
        Text(statusMessage, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SuccessScreen(onBack: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Success",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Обновление завершено!",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onBack) {
            Text("Готово")
        }
    }
}

@Composable
private fun ErrorScreen(
    state: FirmwareRepository.UpdateState.Error,
    viewModel: FirmwareViewModel,
    onBack: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Ошибка",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )

        Text(state.message, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.checkForUpdates() },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Попробовать снова")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(0.8f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Text("Назад")
        }
    }
}