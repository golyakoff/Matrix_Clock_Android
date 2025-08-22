package net.agolyakov.tetrisclockble.model

import android.bluetooth.le.ScanResult

fun ScanResult.toBleDevice(): BleDevice {
    return BleDevice(
        deviceName = this.scanRecord?.deviceName ?: "<без имени>",
        deviceMacAddr = this.device.address,
        txPowerLevel = this.scanRecord?.txPowerLevel ?: Integer.MIN_VALUE
    )
}
