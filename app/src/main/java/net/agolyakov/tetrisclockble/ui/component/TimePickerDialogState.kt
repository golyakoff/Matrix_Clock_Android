package net.agolyakov.tetrisclockble.ui.component

import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockAlarmType

data class TimePickerDialogState(
    val isVisible: Boolean = false,
    val alarmType: TetrisClockAlarmType = TetrisClockAlarmType.TURN_ON,
    val hour: Int = 0,
    val minute: Int = 0,
    val isActive: Boolean = false
)