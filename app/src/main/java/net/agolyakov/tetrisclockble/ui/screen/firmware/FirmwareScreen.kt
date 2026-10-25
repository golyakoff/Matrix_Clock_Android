package net.agolyakov.tetrisclockble.ui.screen.firmware

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import net.agolyakov.tetrisclockble.R
import net.agolyakov.tetrisclockble.data.repository.FirmwareRepository.UpdateState
import net.agolyakov.tetrisclockble.ui.theme.TetrisClockBLETheme

/** The update from the phone over Bluetooth, in the forms of UpdateScreens.kt. */
@Composable
fun FirmwareScreen(
    viewModel: FirmwareViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val back = UpdateAction(stringResource(R.string.mc_back), onBack)
    UpdateScreenFrame {
        when (val s = state) {
            UpdateState.Idle -> UpdatePromptScreen(
                title = stringResource(R.string.mc_firmware_update_title),
                primary = UpdateAction(stringResource(R.string.mc_firmware_check_for_updates), viewModel::checkForUpdates)
            )

            UpdateState.Checking -> UpdateProgressScreen(stringResource(R.string.mc_firmware_checking))

            UpdateState.Processing -> UpdateProgressScreen(
                title = stringResource(R.string.mc_firmware_processing),
                progress = progress,
                detail = statusMessage
            )

            is UpdateState.UpdateAvailable -> UpdatePromptScreen(
                title = stringResource(R.string.mc_firmware_update_available),
                lines = listOf(
                    stringResource(R.string.mc_firmware_current_version, s.currentVersion),
                    stringResource(R.string.mc_firmware_new_version, s.release.tagName)
                ),
                primary = UpdateAction(stringResource(R.string.mc_firmware_update_action), viewModel::performCompleteUpdate),
                secondary = back
            )

            is UpdateState.NoUpdate -> UpdateResultScreen(
                success = true,
                title = stringResource(R.string.mc_firmware_up_to_date),
                message = stringResource(R.string.mc_firmware_version, s.currentVersion),
                primary = back
            )

            is UpdateState.Downloading -> UpdateProgressScreen(
                title = stringResource(R.string.mc_ota_downloading),
                progress = progress,
                detail = statusMessage
            )

            is UpdateState.ReadyToInstall -> UpdatePromptScreen(
                title = stringResource(R.string.mc_firmware_ready),
                lines = listOf(
                    stringResource(R.string.mc_firmware_version, s.release.tagName),
                    stringResource(R.string.mc_firmware_size, s.file.length() / 1024)
                ),
                primary = UpdateAction(stringResource(R.string.mc_firmware_install_action)) { viewModel.installFirmware(s.file) }
            )

            is UpdateState.Installing -> UpdateProgressScreen(
                title = stringResource(R.string.mc_ota_installing),
                progress = progress,
                detail = statusMessage
            )

            UpdateState.Success -> UpdateResultScreen(
                success = true,
                title = stringResource(R.string.mc_ota_complete),
                primary = UpdateAction(stringResource(R.string.mc_firmware_done_action), onBack)
            )

            is UpdateState.Error -> UpdateResultScreen(
                success = false,
                title = stringResource(R.string.mc_error),
                message = s.message,
                primary = UpdateAction(stringResource(R.string.mc_firmware_retry_action), viewModel::checkForUpdates),
                secondary = back
            )
        }
    }
}

@Composable
private fun PreviewFrame(darkTheme: Boolean, content: @Composable () -> Unit) {
    TetrisClockBLETheme(darkTheme = darkTheme) { UpdateScreenFrame(content) }
}

@Composable
private fun PromptPreview() = UpdatePromptScreen(
    title = "Доступно обновление!",
    lines = listOf("Текущая версия: v2.1.0", "Новая версия: v2.1.1"),
    primary = UpdateAction("Обновить") {},
    secondary = UpdateAction("Назад") {}
)

@Composable
private fun ProgressPreview() = UpdateProgressScreen(
    title = "Выполняется обновление...",
    progress = 42.5f,
    detail = "Установка обновления..."
)

@Composable
private fun ErrorPreview() = UpdateResultScreen(
    success = false,
    title = "Ошибка",
    message = "Сообщение об ошибке!",
    primary = UpdateAction("Попробовать снова") {},
    secondary = UpdateAction("Назад") {}
)

@Composable
private fun SuccessPreview() = UpdateResultScreen(
    success = true,
    title = "Обновление завершено!",
    primary = UpdateAction("Готово") {}
)

@Preview(name = "Prompt: Light", heightDp = 400)
@Composable
fun PromptScreen_Light_Preview() = PreviewFrame(false) { PromptPreview() }

@Preview(name = "Prompt: Dark", heightDp = 400)
@Composable
fun PromptScreen_Dark_Preview() = PreviewFrame(true) { PromptPreview() }

@Preview(name = "Progress: Light", heightDp = 400)
@Composable
fun ProgressScreen_Light_Preview() = PreviewFrame(false) { ProgressPreview() }

@Preview(name = "Progress: Dark", heightDp = 400)
@Composable
fun ProgressScreen_Dark_Preview() = PreviewFrame(true) { ProgressPreview() }

@Preview(name = "Error: Light", heightDp = 400)
@Composable
fun ErrorScreen_Light_Preview() = PreviewFrame(false) { ErrorPreview() }

@Preview(name = "Error: Dark", heightDp = 400)
@Composable
fun ErrorScreen_Dark_Preview() = PreviewFrame(true) { ErrorPreview() }

@Preview(name = "Success: Light", heightDp = 400)
@Composable
fun SuccessScreen_Light_Preview() = PreviewFrame(false) { SuccessPreview() }

@Preview(name = "Success: Dark", heightDp = 400)
@Composable
fun SuccessScreen_Dark_Preview() = PreviewFrame(true) { SuccessPreview() }
