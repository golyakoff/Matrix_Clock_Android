package net.agolyakov.tetrisclockble.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import net.agolyakov.tetrisclockble.model.BleDevice
import net.agolyakov.tetrisclockble.screen.DeviceScreen
import net.agolyakov.tetrisclockble.screen.HomeScreen

@Composable
fun SetupNavGraph(
    navController: NavHostController
) {
    NavHost(navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen(navController)
        }

        composable(route = Screen.Device.route) {
            val device: BleDevice? =
                navController.previousBackStackEntry?.savedStateHandle?.get<BleDevice>("device")

            DeviceScreen(navController, device)
        }
    }
}
