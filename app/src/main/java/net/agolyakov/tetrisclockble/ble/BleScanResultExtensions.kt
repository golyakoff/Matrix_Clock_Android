package net.agolyakov.tetrisclockble.ble

import android.bluetooth.le.ScanResult

fun ScanResult.toBleDevice(): BleDevice {
    return BleDevice(
        deviceName = this.scanRecord?.deviceName ?: "<без имени>",
        deviceMacAddress = this.device.address
    )
}
