package net.agolyakov.tetrisclockble.ui.screen.device

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.agolyakov.tetrisclockble.R
import net.agolyakov.tetrisclockble.ui.screen.firmware.UpdateAction
import net.agolyakov.tetrisclockble.ui.screen.firmware.UpdateProgressScreen
import net.agolyakov.tetrisclockble.ui.screen.firmware.UpdateResultScreen
import net.agolyakov.tetrisclockble.ui.screen.firmware.UpdateScreenFrame

/**
 * The update the clock installs by itself over WiFi, followed from the phone in the forms of the
 * update over Bluetooth. Closing it does not stop the clock.
 */
@Composable
fun WifiUpdateScreen(update: WifiUpdate, onClose: () -> Unit, onRetry: () -> Unit) {
    BackHandler(onBack = onClose)
    val close = UpdateAction(stringResource(R.string.mc_dialog_close), onClose)
    val note = stringResource(R.string.mc_fw_progress_continues)
    UpdateScreenFrame {
        when (update) {
            WifiUpdate.Checking -> UpdateProgressScreen(
                title = stringResource(R.string.mc_fw_progress_clock_checking),
                note = note,
                close = close
            )

            is WifiUpdate.Downloading -> UpdateProgressScreen(
                title = stringResource(R.string.mc_fw_progress_clock_downloading),
                progress = if (update.total > 0) (update.received * 100f / update.total).coerceIn(0f, 100f) else null,
                detail = if (update.total > 0) {
                    stringResource(R.string.mc_fw_progress_amount, (update.received / 1024).toString(), (update.total / 1024).toString())
                } else null,
                note = note,
                close = close
            )

            WifiUpdate.Rebooting -> UpdateProgressScreen(
                title = stringResource(R.string.mc_fw_progress_rebooting),
                note = note,
                close = close
            )

            is WifiUpdate.Done -> UpdateResultScreen(
                success = true,
                title = stringResource(R.string.mc_fw_wifi_title_done),
                message = stringResource(R.string.mc_fw_done, update.version),
                primary = UpdateAction(stringResource(R.string.mc_firmware_done_action), onClose)
            )

            is WifiUpdate.RolledBack -> UpdateResultScreen(
                success = false,
                title = stringResource(R.string.mc_fw_wifi_title_failed),
                message = stringResource(R.string.mc_fw_rolled_back, update.version),
                primary = close
            )

            is WifiUpdate.Failed -> UpdateResultScreen(
                success = false,
                title = stringResource(R.string.mc_fw_wifi_title_failed),
                message = stringResource(update.message),
                primary = UpdateAction(stringResource(R.string.mc_firmware_retry_action), onRetry),
                secondary = close
            )
        }
    }
}
