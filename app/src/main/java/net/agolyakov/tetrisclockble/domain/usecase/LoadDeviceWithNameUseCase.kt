package net.agolyakov.tetrisclockble.domain.usecase

import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockDevice
import net.agolyakov.tetrisclockble.domain.repository.PreferencesRepository
import javax.inject.Inject

class LoadDeviceWithNameUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(device: TetrisClockDevice): TetrisClockDevice {
        val friendlyName = preferencesRepository.getFriendlyName(device.macAddress)
        return device.copy(friendlyName = friendlyName)
    }
}