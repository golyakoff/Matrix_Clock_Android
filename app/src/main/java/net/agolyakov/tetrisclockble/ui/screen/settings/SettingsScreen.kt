package net.agolyakov.tetrisclockble.ui.screen.settings

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import net.agolyakov.tetrisclockble.BuildConfig
import net.agolyakov.tetrisclockble.R
import net.agolyakov.tetrisclockble.data.repository.AppUpdateRepository.UpdateCheckState
import net.agolyakov.tetrisclockble.ui.screen.device.CardButtonWide
import net.agolyakov.tetrisclockble.ui.screen.device.CardTitle

// The app's own settings, kept on the phone: about and updates, betas, language.

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val updateState by viewModel.updateState.collectAsState()
    val betaUpdates by viewModel.betaUpdates.collectAsState()

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .fillMaxSize()
            // As the home screen: the window is edge-to-edge, the header would cover the first card
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(8.dp)
    ) {
        AboutCard(updateState, viewModel::checkForUpdate)
        BetaCard(betaUpdates, viewModel::setBetaUpdates)
        LanguageCard()
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline
    )
}

@Composable
private fun AboutCard(state: UpdateCheckState, onCheck: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    SettingsCard {
        CardTitle(Icons.Default.Info, stringResource(R.string.mc_settings_about))
        Text(
            text = stringResource(R.string.mc_app_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = when (state) {
                UpdateCheckState.Checking -> stringResource(R.string.mc_settings_checking)
                UpdateCheckState.UpToDate -> stringResource(R.string.mc_settings_up_to_date)
                is UpdateCheckState.UpdateAvailable ->
                    stringResource(R.string.mc_app_update_available, state.release.tagName)
                is UpdateCheckState.Error -> stringResource(R.string.mc_settings_check_failed, state.message)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (state is UpdateCheckState.Error) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))

        val releaseUrl = (state as? UpdateCheckState.UpdateAvailable)?.release?.htmlUrl
        if (!releaseUrl.isNullOrBlank()) {
            CardButtonWide(
                Icons.Default.OpenInNew,
                stringResource(R.string.mc_settings_open_release),
                { uriHandler.openUri(releaseUrl) },
                true
            )
            Spacer(Modifier.height(12.dp))
        }
        if (state != UpdateCheckState.Checking) {
            CardButtonWide(
                Icons.Default.SystemUpdateAlt,
                stringResource(R.string.mc_settings_check),
                onCheck
            )
        }
    }
}

@Composable
private fun BetaCard(beta: Boolean, onChange: (Boolean) -> Unit) {
    SettingsCard {
        CardTitle(
            Icons.Default.Science,
            stringResource(R.string.mc_settings_beta),
            showSwitch = true,
            isOn = beta,
            onCheckedChange = onChange
        )
        Hint(stringResource(R.string.mc_settings_beta_hint))
    }
}

@Composable
private fun LanguageCard() {
    val context = LocalContext.current
    val current = LocalConfiguration.current.locales[0].language

    SettingsCard {
        CardTitle(Icons.Default.Language, stringResource(R.string.mc_settings_language))
        // Language names are endonyms and stay untranslated regardless of the active locale.
        listOf("ru" to "Русский", "en" to "English").forEach { (tag, name) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (tag != current) setAppLocale(context, tag) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = tag == current,
                    onClick = { if (tag != current) setAppLocale(context, tag) }
                )
                Text(text = name, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

private fun setAppLocale(context: Context, languageTag: String) {
    // AppCompatDelegate.setApplicationLocales() relies on an active AppCompatDelegate
    // instance to resolve the app context, which this app never creates since
    // MainActivity is a plain ComponentActivity. Call the framework LocaleManager
    // directly on API 33+ (where it's guaranteed to exist) and fall back to the
    // AppCompat shim + manual recreate() below that.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(LocaleManager::class.java).applicationLocales =
            LocaleList.forLanguageTags(languageTag)
    } else {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
        (context as? Activity)?.recreate()
    }
}
