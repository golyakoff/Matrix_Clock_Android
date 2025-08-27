package net.agolyakov.tetrisclockble.data

import net.agolyakov.tetrisclockble.ble.BleDevice

class BleDeviceRepository() {
    private val _deviceList = listOf (
        BleDevice(
            deviceName = "LED Lamp",
            deviceMacAddress = "11:22:33:44:55:66"
        ),
        BleDevice(
            deviceName = "Matrix Clock YEY",
            deviceMacAddress = "11:22:33:44:55:77",
            friendlyName = "Часы Андрея в гостиной"
        ),
        BleDevice(
            deviceName = "Matrix Clock BRW",
            deviceMacAddress = "11:22:33:44:55:21",
            friendlyName = "Часы в десткой"
        ),
        BleDevice(
            deviceName = "Matrix Clock 1",
            deviceMacAddress = "11:22:33:44:55:41",
        ),
        BleDevice(
            deviceName = "Matrix Clock 2",
            deviceMacAddress = "11:22:33:44:55:da"
        ),
        BleDevice(
            deviceName = "Matrix Clock 3",
            deviceMacAddress = "11:22:33:44:55:bd",
        ),
        BleDevice(
            deviceName = "Matrix Clock 4",
            deviceMacAddress = "11:22:33:44:55:ff"
        ),
        BleDevice(
            deviceName = "Matrix Clock 5",
            deviceMacAddress = "11:22:33:44:55:da"
        ),
        BleDevice(
            deviceName = "Matrix Clock 6",
            deviceMacAddress = "11:22:33:44:55:da"
        ),
    )

    fun getDeviceList(): List<BleDevice> {
        return _deviceList
    }
}
