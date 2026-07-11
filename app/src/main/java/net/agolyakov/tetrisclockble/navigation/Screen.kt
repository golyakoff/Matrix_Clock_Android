package net.agolyakov.tetrisclockble.navigation

sealed class Screen(val route: String) {
    object Home    : Screen(route = "home_screen")
    object Device : Screen(route = "device_screen")
    object Firmware: Screen(route = "firmware_screen")
}
