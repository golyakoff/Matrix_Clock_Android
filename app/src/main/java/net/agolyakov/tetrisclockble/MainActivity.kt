package net.agolyakov.tetrisclockble

import android.Manifest
import android.app.Activity.RESULT_CANCELED
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import net.agolyakov.tetrisclockble.data.repository.AppUpdateRepository
import net.agolyakov.tetrisclockble.navigation.Screen
import net.agolyakov.tetrisclockble.navigation.SetupNavGraph
import net.agolyakov.tetrisclockble.service.bluetooth.BluetoothService
import net.agolyakov.tetrisclockble.ui.viewmodel.MyRequestPermission
import net.agolyakov.tetrisclockble.ui.theme.TetrisClockBLETheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var bluetoothService: BluetoothService

    @Inject
    lateinit var appUpdateRepository: AppUpdateRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        // The status bar icons follow the light/dark theme over the app's own top bar
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val updateState by appUpdateRepository.state.collectAsState()
            MainContent(updateAvailable = updateState is AppUpdateRepository.UpdateCheckState.UpdateAvailable)
        }
    }

    override fun onPause() {
        super.onPause()
        if (!bluetoothService.shouldPreserveConnection()) {
            bluetoothService.disconnect()
        }
    }

    override fun onResume() {
        super.onResume()
        bluetoothService.tryReconnect()
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothService.disconnect()
    }
}

@Composable
fun MainContent(updateAvailable: Boolean = false) {
    TetrisClockBLETheme {
        val navController = rememberNavController()
        val context = LocalContext.current
        // The name of the clock open on the device screen, for the top bar
        var deviceTitle by remember { mutableStateOf<String?>(null) }

        val enableBluetoothLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_CANCELED) {
                Toast.makeText(context, R.string.perm_bluetooth_is_off, Toast.LENGTH_SHORT).show()
            }
        }

        val permissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT)
            else
                listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION)

        MyRequestPermission(permissions) { granted ->
            if (granted) {
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            } else {
                Toast.makeText(context, R.string.perm_not_enough_permissions, Toast.LENGTH_SHORT).show()
            }
        }

        val route = navController.currentBackStackEntryAsState().value?.destination?.route
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            AppTopBar(
                title = when (route) {
                    Screen.Settings.route -> stringResource(R.string.mc_settings)
                    Screen.Firmware.route -> stringResource(R.string.mc_firmware_update_title)
                    Screen.Device.route -> deviceTitle ?: stringResource(R.string.app_name)
                    else -> stringResource(R.string.app_name)
                },
                onBack = if (route == null || route == Screen.Home.route) null
                         else ({ navController.popBackStack() }),
                onSettings = if (route == Screen.Home.route) ({ navController.navigate(Screen.Settings.route) })
                             else null,
                updateAvailable = updateAvailable
            )
            // The top bar takes the status bar: the screens below pad for the navigation bar only
            Box(Modifier.weight(1f).consumeWindowInsets(WindowInsets.statusBars)) {
                SetupNavGraph(navController = navController, onDeviceTitle = { deviceTitle = it })
            }
        }
    }
}

/**
 * The header of every screen, in the theme's colors: the screen's title (the clock's name on the
 * clock's screen), back on every screen but the list, the gear to the app's settings on the list
 * (with a dot while an app update is available).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    title: String,
    onBack: (() -> Unit)?,
    onSettings: (() -> Unit)?,
    updateAvailable: Boolean
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            }
        },
        actions = {
            if (onSettings != null) {
                IconButton(onClick = onSettings) {
                    BadgedBox(badge = { if (updateAvailable) Badge() }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.mc_settings))
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
            navigationIconContentColor = MaterialTheme.colorScheme.primary,
            actionIconContentColor = MaterialTheme.colorScheme.primary
        )
    )
}

fun getBlePermissions(): List<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}