package net.agolyakov.tetrisclockble.ui.screen.home

import android.Manifest
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.os.SystemClock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.agolyakov.tetrisclockble.R
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockDevice
import net.agolyakov.tetrisclockble.data.extensions.toBleDevice
import net.agolyakov.tetrisclockble.data.repository.AppUpdateRepository
import net.agolyakov.tetrisclockble.service.bluetooth.BluetoothAdapterProvider
import net.agolyakov.tetrisclockble.service.bluetooth.BluetoothService
import net.agolyakov.tetrisclockble.domain.repository.PreferencesRepository
import net.agolyakov.tetrisclockble.domain.usecase.LoadDeviceWithNameUseCase
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    val bluetoothService: BluetoothService,
    bluetoothAdapterProvider: BluetoothAdapterProvider,
    private val preferencesRepository: PreferencesRepository,
    private val loadDeviceWithNameUseCase: LoadDeviceWithNameUseCase,
    private val appUpdateRepository: AppUpdateRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val appUpdateState: StateFlow<AppUpdateRepository.UpdateCheckState> = appUpdateRepository.state

    // The clocks heard recently, with the time each was last heard (elapsedRealtime). A clock not
    // heard for DEVICE_TIMEOUT_MS (switched off, out of range) leaves the list.
    private val foundDevices = HashMap<String, TetrisClockDevice>()
    private val lastSeen = HashMap<String, Long>()
    private var pruneJob: Job? = null
    private val _devices: MutableLiveData<List<TetrisClockDevice>> = MutableLiveData()
    val devices: LiveData<List<TetrisClockDevice>> get() = _devices

    private val adapter = bluetoothAdapterProvider.getAdapter()
    private var scanner: BluetoothLeScanner? = null
    private var callback: BleScanCallback? = null

    private val settings: ScanSettings =
        ScanSettings.Builder()
            // Low power hears a clock about every 5 s only: too coarse to drop silent ones in time
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()

    private val filters: List<ScanFilter> = listOf(
        ScanFilter.Builder()
            .setServiceUuid(FILTER_UUID)
            .build()
    )

    init {
        viewModelScope.launch {
            appUpdateRepository.checkForUpdate()
        }
    }

    /**
     * Scans while the list is on screen. Android turns a scan running for more than 30 minutes into
     * an opportunistic one that hears nothing by itself, so a scan left running in the background
     * (it used to run on under the clock's screen too) stopped finding clocks switched on later.
     * The list screen therefore stops the scan when it leaves and starts a fresh one when it is back;
     * a long stay on the list restarts it every SCAN_RESTART_MS.
     */
    @RequiresPermission(value = Manifest.permission.BLUETOOTH_SCAN)
    fun startScan() {
        if (callback == null) {
            callback = BleScanCallback()
            scanner = adapter.bluetoothLeScanner
            scanner?.startScan(filters, settings, callback)
        }
        if (pruneJob == null) {
            pruneJob = viewModelScope.launch {
                var scanStarted = SystemClock.elapsedRealtime()
                while (isActive) {
                    delay(PRUNE_PERIOD_MS)
                    pruneSilentDevices()
                    if (SystemClock.elapsedRealtime() - scanStarted > SCAN_RESTART_MS) {
                        restartScan()
                        scanStarted = SystemClock.elapsedRealtime()
                    }
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        pruneJob?.cancel()
        pruneJob = null
        if (callback != null) {
            scanner?.stopScan(callback)
            scanner = null
            callback = null
        }
    }

    @Suppress("MissingPermission") // only called while scanning, i.e. with the permission granted
    private fun restartScan() {
        callback?.let { scanner?.stopScan(it) }
        scanner = adapter.bluetoothLeScanner
        callback?.let { scanner?.startScan(filters, settings, it) }
    }

    private fun pruneSilentDevices() {
        val now = SystemClock.elapsedRealtime()
        val silent = lastSeen.filterValues { now - it > DEVICE_TIMEOUT_MS }.keys
        if (silent.isEmpty()) return
        silent.forEach {
            lastSeen.remove(it)
            foundDevices.remove(it)
        }
        publish()
    }

    // Sorted, so that the list does not reshuffle as clocks come and go (HashMap order moved the
    // card under the user's finger when another clock showed up)
    private fun publish() {
        _devices.value = foundDevices.values.sortedWith(
            compareBy<TetrisClockDevice>({ it.getDisplayName().lowercase() }, { it.macAddress })
        )
    }

    private fun onDeviceHeard(result: ScanResult) {
        val address = result.device.address
        lastSeen[address] = SystemClock.elapsedRealtime()
        val device = loadDeviceWithNameUseCase(
            result.toBleDevice(context.getString(R.string.mc_unnamed_device))
        )
        // Republish only on a change: a new clock, or one that was renamed
        if (foundDevices[address] != device) {
            foundDevices[address] = device
            publish()
        }
    }

    @RequiresPermission(value = Manifest.permission.BLUETOOTH_SCAN)
    override fun onCleared() {
        super.onCleared()
        stopScan()
    }

    inner class BleScanCallback : ScanCallback() {
        // Scan callbacks arrive on the main thread, as the pruning does: the maps need no locking
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            onDeviceHeard(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { onDeviceHeard(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BluetoothScanner", "onScanFailed: scan error $errorCode")
        }
    }

    fun renameDevice(macAddress: String, newName: String?) {
        if (newName.isNullOrBlank()) {
            preferencesRepository.deleteFriendlyName(macAddress)
        } else {
            preferencesRepository.saveFriendlyName(macAddress, newName)
        }
        foundDevices[macAddress]?.let { device ->
            val updatedDevice = device.copy(friendlyName = if (newName.isNullOrBlank()) null else newName)
            foundDevices[macAddress] = updatedDevice
            publish()
        }
    }

    companion object {
        private const val DEVICE_TIMEOUT_MS = 15_000L
        private const val PRUNE_PERIOD_MS = 2_000L
        private const val SCAN_RESTART_MS = 20 * 60 * 1000L
        val FILTER_UUID = ParcelUuid.fromString("5DE498A1-E7A6-4F4A-B323-913741895AD0")!!
    }
}
