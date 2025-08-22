package net.agolyakov.tetrisclockble.data

import net.agolyakov.tetrisclockble.model.BleDevice

class BleDeviceRepository() {
    private val _deviceList = listOf (
        BleDevice(
            deviceName = "LED Lamp",
            deviceMacAddr = "11:22:33:44:55:66"
        ),
        BleDevice(
            deviceName = "Matrix Clock YEY",
            deviceMacAddr = "11:22:33:44:55:77",
            overrideName = "Часы Андрея в гостиной"
        ),
        BleDevice(
            deviceName = "Matrix Clock BRW",
            deviceMacAddr = "11:22:33:44:55:21",
            overrideName = "Часы в десткой"
        ),
        BleDevice(
            deviceName = "Matrix Clock 1",
            deviceMacAddr = "11:22:33:44:55:41",
        ),
        BleDevice(
            deviceName = "Matrix Clock 2",
            deviceMacAddr = "11:22:33:44:55:da"
        ),
        BleDevice(
            deviceName = "Matrix Clock 3",
            deviceMacAddr = "11:22:33:44:55:bd",
        ),
        BleDevice(
            deviceName = "Matrix Clock 4",
            deviceMacAddr = "11:22:33:44:55:ff"
        ),
        BleDevice(
            deviceName = "Matrix Clock 5",
            deviceMacAddr = "11:22:33:44:55:da"
        ),
    )

    fun getDeviceList(): List<BleDevice> {
        return _deviceList
    }
}
