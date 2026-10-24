package net.agolyakov.tetrisclockble

import android.Manifest
import android.app.Activity.RESULT_CANCELED
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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

    // The header (the framework action bar) shows the gear to the settings on the home screen only.
    private var navController: NavHostController? = null
    private var onHome = false

    private val destinationListener = NavController.OnDestinationChangedListener { _, destination, _ ->
        onHome = destination.route == Screen.Home.route
        setTitle(if (destination.route == Screen.Settings.route) R.string.mc_settings else R.string.app_name)
        invalidateOptionsMenu()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainContent { navController ->
                DisposableEffect(navController) {
                    this@MainActivity.navController = navController
                    navController.addOnDestinationChangedListener(destinationListener)
                    onDispose {
                        navController.removeOnDestinationChangedListener(destinationListener)
                        this@MainActivity.navController = null
                    }
                }
            }
        }

        // The gear gets a dot while an app update is available.
        lifecycleScope.launch {
            appUpdateRepository.state.collect { invalidateOptionsMenu() }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (!onHome) return false
        menu.add(Menu.NONE, MENU_SETTINGS, Menu.NONE, R.string.mc_settings).apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            icon = settingsIcon(
                appUpdateRepository.state.value is AppUpdateRepository.UpdateCheckState.UpdateAvailable
            )
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == MENU_SETTINGS) {
            navController?.navigate(Screen.Settings.route)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /** The gear, tinted for the header (its own theme), with a red dot at the top right if [badge]. */
    private fun settingsIcon(badge: Boolean): Drawable? {
        val context = actionBar?.themedContext ?: this
        val gear = context.getDrawable(R.drawable.ic_settings) ?: return null
        if (!badge) return gear
        val dot = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(BADGE_COLOR)
        }
        val size = gear.intrinsicWidth
        val dotSize = size * 3 / 8
        return LayerDrawable(arrayOf(gear, dot)).apply {
            setLayerInset(1, size - dotSize, 0, 0, size - dotSize)
        }
    }

    private companion object {
        const val MENU_SETTINGS = 1
        const val BADGE_COLOR = 0xFFE53935.toInt()
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
fun MainContent(onNavController: @Composable (NavHostController) -> Unit = {}) {
    TetrisClockBLETheme {
        val navController = rememberNavController()
        val context = LocalContext.current

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

        onNavController(navController)
        SetupNavGraph(navController = navController)
    }
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