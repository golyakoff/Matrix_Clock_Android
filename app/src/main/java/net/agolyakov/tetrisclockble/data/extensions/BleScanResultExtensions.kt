package net.agolyakov.tetrisclockble.data.extensions

import android.bluetooth.le.ScanResult
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockDevice

fun ScanResult.toBleDevice(unnamedDeviceFallback: String): TetrisClockDevice {
    return TetrisClockDevice(
        deviceName = this.scanRecord?.deviceName ?: unnamedDeviceFallback,
        macAddress = this.device.address
    )
}
