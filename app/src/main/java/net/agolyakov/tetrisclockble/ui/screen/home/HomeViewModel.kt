package net.agolyakov.tetrisclockble.ui.screen.home

import android.Manifest
import android.bluetooth.le.*
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.agolyakov.tetrisclockble.data.repository.DeviceRepository
import net.agolyakov.tetrisclockble.data.model.ble.TetrisClockDevice
import net.agolyakov.tetrisclockble.data.extensions.toBleDevice
import net.agolyakov.tetrisclockble.service.bluetooth.BluetoothAdapterProvider
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val bluetoothAdapterProvider: BluetoothAdapterProvider,
): ViewModel() {
    private val foundDevices = HashMap<String, TetrisClockDevice>()
    private val _devices: MutableLiveData<List<TetrisClockDevice>> = MutableLiveData()
    val devices: LiveData<List<TetrisClockDevice>> get() = _devices

    private val adapter = bluetoothAdapterProvider.getAdapter()
    private var scanner: BluetoothLeScanner? = null
    private var callback: BleScanCallback? = null

    private val settings: ScanSettings =
        ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

    private val filters: List<ScanFilter> = listOf(
        ScanFilter.Builder()
            .setServiceUuid(FILTER_UUID)
            .build())

    @RequiresPermission(value = Manifest.permission.BLUETOOTH_SCAN)
    fun startScan() {
        if (callback == null) {
            callback = BleScanCallback()
            scanner = adapter.bluetoothLeScanner
            scanner?.startScan(filters, settings, callback)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        if (callback != null) {
            scanner?.stopScan(callback)
            scanner = null
            callback = null
        }
    }

    @RequiresPermission(value = Manifest.permission.BLUETOOTH_SCAN)
    override fun onCleared() {
        super.onCleared()
        stopScan()
    }

    inner class BleScanCallback : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {

            if (!foundDevices.containsKey(result.device.address)) {
                foundDevices[result.device.address] = result.toBleDevice()
            }

            _devices.postValue(foundDevices.values.toList())
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { result ->
                if (!foundDevices.containsKey(result.device.address)) foundDevices[result.device.address] =
                    result.toBleDevice()
            }

            _devices.postValue(foundDevices.values.toList())
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BluetoothScanner", "onScanFailed: scan error $errorCode")
        }
    }

    companion object {
        val FILTER_UUID = ParcelUuid.fromString("5DE498A1-E7A6-4F4A-B323-913741895AD0")!!
    }

    fun getDeviceRepository(): DeviceRepository {
        return deviceRepository
    }
}
