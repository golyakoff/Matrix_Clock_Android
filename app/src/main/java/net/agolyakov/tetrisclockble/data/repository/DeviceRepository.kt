package net.agolyakov.tetrisclockble.data.repository

import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockDevice

class DeviceRepository() {
    private val _deviceList = listOf (
        TetrisClockDevice(
            deviceName = "LED Lamp",
            macAddress = "11:22:33:44:55:66"
        ),
        TetrisClockDevice(
            deviceName = "Matrix Clock YEY",
            macAddress = "11:22:33:44:55:77",
            friendlyName = "Часы Андрея в гостиной"
        ),
        TetrisClockDevice(
            deviceName = "Matrix Clock BRW",
            macAddress = "11:22:33:44:55:21",
            friendlyName = "Часы в десткой"
        ),
        TetrisClockDevice(
            deviceName = "Matrix Clock 1",
            macAddress = "11:22:33:44:55:41",
        ),
        TetrisClockDevice(
            deviceName = "Matrix Clock 2",
            macAddress = "11:22:33:44:55:da"
        ),
        TetrisClockDevice(
            deviceName = "Matrix Clock 3",
            macAddress = "11:22:33:44:55:bd",
        ),
        TetrisClockDevice(
            deviceName = "Matrix Clock 4",
            macAddress = "11:22:33:44:55:ff"
        ),
        TetrisClockDevice(
            deviceName = "Matrix Clock 5",
            macAddress = "11:22:33:44:55:da"
        ),
        TetrisClockDevice(
            deviceName = "Matrix Clock 6",
            macAddress = "11:22:33:44:55:da"
        ),
    )

    fun getDeviceList(): List<TetrisClockDevice> {
        return _deviceList
    }
}