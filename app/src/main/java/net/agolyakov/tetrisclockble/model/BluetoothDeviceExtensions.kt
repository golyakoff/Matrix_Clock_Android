package net.agolyakov.tetrisclockble.model

import android.bluetooth.BluetoothDevice

fun BluetoothDevice.toBleDevice(): BleDevice {
    return BleDevice(
        mfrName = this.address,//this.name,
        macAddress = this.address
    )
}
