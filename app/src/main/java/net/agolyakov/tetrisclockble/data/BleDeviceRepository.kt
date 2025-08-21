package net.agolyakov.tetrisclockble.data

import net.agolyakov.tetrisclockble.model.BleDevice

class BleDeviceRepository() {
    private val _deviceList = listOf (
        BleDevice(
            mfrName = "LED Lamp",
            macAddress = "11:22:33:44:55:66"
        ),
        BleDevice(
            mfrName = "Matrix Clock YEY",
            macAddress = "11:22:33:44:55:77",
            overrideName = "Часы Андрея в гостиной"
        ),
        BleDevice(
            mfrName = "Matrix Clock BRW",
            macAddress = "11:22:33:44:55:21",
            overrideName = "Часы в десткой"
        ),
        BleDevice(
            mfrName = "Matrix Clock 1",
            macAddress = "11:22:33:44:55:41",
        ),
        BleDevice(
            mfrName = "Matrix Clock 2",
            macAddress = "11:22:33:44:55:da"
        ),
        BleDevice(
            mfrName = "Matrix Clock 3",
            macAddress = "11:22:33:44:55:bd",
        ),
        BleDevice(
            mfrName = "Matrix Clock 4",
            macAddress = "11:22:33:44:55:ff"
        ),
        BleDevice(
            mfrName = "Matrix Clock 5",
            macAddress = "11:22:33:44:55:da"
        ),
    )

    fun getDeviceList(): List<BleDevice> {
        return _deviceList
    }
}
