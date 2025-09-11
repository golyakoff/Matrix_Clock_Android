package net.agolyakov.tetrisclockble.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockDevice
import net.agolyakov.tetrisclockble.ui.screen.device.DeviceScreen
import net.agolyakov.tetrisclockble.ui.screen.firmware.FirmwareUpdateScreen
import net.agolyakov.tetrisclockble.ui.screen.home.HomeScreen
import net.agolyakov.tetrisclockble.ui.screen.home.HomeViewModel

@Composable
fun SetupNavGraph(
    navController: NavHostController
) {
    NavHost(navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(route = Screen.Home.route) {
            val homeViewModel: HomeViewModel = hiltViewModel()
            LaunchedEffect(Unit) {
                homeViewModel.startScan()
            }
            HomeScreen(navController, homeViewModel)
        }

        composable(route = Screen.Device.route) {
            val device: TetrisClockDevice? =
                navController.previousBackStackEntry?.savedStateHandle?.get<TetrisClockDevice>("device")

            DeviceScreen(navController, device)
        }

        composable(route = "firmware_update") {
            FirmwareUpdateScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
