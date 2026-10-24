package net.agolyakov.tetrisclockble.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.agolyakov.tetrisclockble.data.repository.AppUpdateRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appUpdateRepository: AppUpdateRepository
) : ViewModel() {
    val updateState: StateFlow<AppUpdateRepository.UpdateCheckState> = appUpdateRepository.state
    val betaUpdates: StateFlow<Boolean> = appUpdateRepository.betaUpdates

    fun checkForUpdate() {
        viewModelScope.launch { appUpdateRepository.checkForUpdate() }
    }

    /** Saves the setting and checks again: the answer depends on it. */
    fun setBetaUpdates(beta: Boolean) {
        appUpdateRepository.setBetaUpdates(beta)
        checkForUpdate()
    }
}
