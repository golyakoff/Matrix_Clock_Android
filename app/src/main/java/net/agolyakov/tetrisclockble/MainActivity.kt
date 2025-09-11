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
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import net.agolyakov.tetrisclockble.navigation.SetupNavGraph
import net.agolyakov.tetrisclockble.ui.viewmodel.MyRequestPermission
import net.agolyakov.tetrisclockble.ui.theme.TetrisClockBLETheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainContent()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun MainContent() {
    TetrisClockBLETheme {
        val navController = rememberNavController()
        val context = LocalContext.current

        // Launcher для включения Bluetooth
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

        // Запрос разрешений
        MyRequestPermission(permissions) { granted ->
            if (granted) {
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            } else {
                Toast.makeText(context, R.string.perm_not_enough_permissions, Toast.LENGTH_SHORT).show()
            }
        }

        // Навигация
        SetupNavGraph(navController = navController)
    }

    fun getBlePermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+)
            listOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            // Android 6 – Android 11
            listOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }
}