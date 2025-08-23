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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import net.agolyakov.tetrisclockble.navigation.SetupNavGraph
import net.agolyakov.tetrisclockble.screen.MyRequestPermission
import net.agolyakov.tetrisclockble.ui.theme.TetrisClockBLETheme
import net.agolyakov.tetrisclockble.viewmodel.HomeViewModel

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
                Toast.makeText(context, "Bluetooth не включён", Toast.LENGTH_SHORT).show()
            }
        }

        // Запрос разрешений BLUETOOTH_SCAN и BLUETOOTH_CONNECT
        MyRequestPermission(
            permissions = listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) { granted ->
            if (granted) {
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            } else {
                Toast.makeText(context, "Не все разрешения даны", Toast.LENGTH_SHORT).show()
            }
        }

        // Навигация
        SetupNavGraph(navController = navController)
    }
}