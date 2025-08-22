package net.agolyakov.tetrisclockble.viewmodel

import android.Manifest
import android.bluetooth.le.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.agolyakov.tetrisclockble.data.BleDeviceRepository
import net.agolyakov.tetrisclockble.model.BleDevice
import net.agolyakov.tetrisclockble.model.toBleDevice
import net.agolyakov.tetrisclockble.service.BleControlManager
import net.agolyakov.tetrisclockble.service.BluetoothAdapterProvider
import javax.inject.Inject


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val deviceRepository: BleDeviceRepository,
    private val bluetoothAdapterProvider: BluetoothAdapterProvider,
    private val bleControlManager: BleControlManager
): ViewModel() {
    private val foundDevices = HashMap<String, BleDevice>()
    private val _devices: MutableLiveData<List<BleDevice>> = MutableLiveData()
    val devices: LiveData<List<BleDevice>> get() = _devices

    private val adapter = bluetoothAdapterProvider.getAdapter()
    private var scanner: BluetoothLeScanner? = null
    private var callback: BleScanCallback? = null

    private val settings: ScanSettings
    private val filters: List<ScanFilter>

    init {
        settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

        filters = listOf(
            ScanFilter.Builder().setServiceUuid(FILTER_UUID).build()
        )

        startScan()
    }

    @RequiresPermission(value = Manifest.permission.BLUETOOTH_SCAN)
    fun startScan() {
        if (callback == null) {
            callback = BleScanCallback()
            scanner = adapter.bluetoothLeScanner
            scanner?.startScan(filters, settings, callback)
        }
    }

    @RequiresPermission(value = Manifest.permission.BLUETOOTH_SCAN)
    override fun onCleared() {
        super.onCleared()
        if (callback != null) {
            scanner?.stopScan(callback)
            scanner = null
            callback = null
        }
    }

    inner class BleScanCallback : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (!foundDevices.containsKey(result.device.address)) {
                foundDevices[result.device.address] = result.device.toBleDevice()
            }

            _devices.postValue(foundDevices.values.toList())
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { result ->
                if (!foundDevices.containsKey(result.device.address)) foundDevices[result.device.address] =
                    result.device.toBleDevice()
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

    fun getDeviceRepository(): BleDeviceRepository {
        return deviceRepository
    }
}