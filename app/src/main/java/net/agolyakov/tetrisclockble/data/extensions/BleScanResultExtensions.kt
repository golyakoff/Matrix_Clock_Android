package net.agolyakov.tetrisclockble.data.extensions

import android.bluetooth.le.ScanResult
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockDevice

fun ScanResult.toBleDevice(): TetrisClockDevice {
    return TetrisClockDevice(
        deviceName = this.scanRecord?.deviceName ?: "<без имени>",
        macAddress = this.device.address
    )
}
