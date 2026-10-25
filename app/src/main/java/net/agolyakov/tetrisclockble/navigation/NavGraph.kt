package net.agolyakov.tetrisclockble.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.coroutines.delay
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockDevice
import net.agolyakov.tetrisclockble.ui.screen.device.DeviceScreen
import net.agolyakov.tetrisclockble.ui.screen.device.DeviceViewModel
import net.agolyakov.tetrisclockble.ui.screen.firmware.FirmwareScreen
import net.agolyakov.tetrisclockble.ui.screen.home.HomeScreen
import net.agolyakov.tetrisclockble.ui.screen.home.HomeViewModel
import net.agolyakov.tetrisclockble.ui.screen.settings.SettingsScreen

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
                delay(50)
                homeViewModel.bluetoothService.setShouldMaintainConnection(false)
            }
            // Scan only while the list is visible: not under the clock's screen, not in background
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> homeViewModel.startScan()
                        Lifecycle.Event.ON_PAUSE -> homeViewModel.stopScan()
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    homeViewModel.stopScan()
                }
            }
            HomeScreen(navController, homeViewModel)
        }

        composable(route = Screen.Device.route) {
            val device: TetrisClockDevice? =
                navController.previousBackStackEntry?.savedStateHandle?.get<TetrisClockDevice>("device")

            val deviceViewModel: DeviceViewModel = hiltViewModel()

            DeviceScreen(navController, device)
        }

        composable(route = Screen.Firmware.route) {
            FirmwareScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
