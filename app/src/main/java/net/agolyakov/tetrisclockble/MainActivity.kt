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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import net.agolyakov.tetrisclockble.navigation.SetupNavGraph
import net.agolyakov.tetrisclockble.service.bluetooth.BluetoothService
import net.agolyakov.tetrisclockble.ui.viewmodel.MyRequestPermission
import net.agolyakov.tetrisclockble.ui.theme.TetrisClockBLETheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var bluetoothService: BluetoothService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainContent()
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
fun MainContent() {
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