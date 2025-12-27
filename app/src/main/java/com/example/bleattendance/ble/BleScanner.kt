// In ble/BleScanner.kt

package com.example.bleattendance.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import com.example.bleattendance.utils.hasRequiredPermissions
import com.example.bleattendance.utils.isBluetoothAvailable
import java.util.UUID

@SuppressLint("MissingPermission")
class BleScanner(
    context: Context,
    private val onDeviceFound: (ScanResult) -> Unit,
    private val onScanFailed: (String) -> Unit // ✅ IMPROVED: Now provides error message
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val scanner = bluetoothManager.adapter?.bluetoothLeScanner

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            
            val device = result.device
            val rssi = result.rssi
            val scanRecord = result.scanRecord
            
            println("🔍 BLE SCANNER DEBUG - DEVICE FOUND:")
            println("📱 Device Name: '${device.name}'")
            println("📱 Device Address: '${device.address}'")
            println("📡 RSSI: $rssi dBm")
            println("📡 Scan Record: ${scanRecord != null}")
            println("📡 Callback Type: $callbackType")
            
            // ✅ ADDED: Filter for devices with our service data
            val serviceData = result.scanRecord?.getServiceData(ParcelUuid(UUID.fromString("12345678-1234-1234-1234-123456789ABC")))
            if (serviceData != null) {
                val dataString = String(serviceData, Charsets.UTF_8)
                println("✅ BLE Scanner: Device found with our service - ${result.device.address}")
                println("✅ Service Data: '$dataString' (${serviceData.size} bytes)")
                onDeviceFound(result)
            } else {
                // Device found but doesn't have our service data - ignore it
                println("❌ BLE Scanner: Device found but no service data - ${result.device.address}")
                println("❌ Available Services: ${scanRecord?.serviceUuids?.size ?: 0}")
                println("❌ Available Service Data: ${scanRecord?.serviceData?.size ?: 0}")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            val errorMessage = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Application registration failed"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "BLE scanning not supported"
                SCAN_FAILED_INTERNAL_ERROR -> "Internal error during scan"
                SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> "Out of hardware resources"
                else -> "Unknown scan error: $errorCode"
            }
            println("BLE Scanner: Scan failed - $errorMessage")
            onScanFailed(errorMessage)
        }
    }

    fun startScanning(): Boolean {
        println("🔍 BLE SCANNER DEBUG - STARTING SCAN:")
        println("📱 Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        println("📱 Android Version: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
        
        // Check if BLE is supported
        if (bluetoothManager.adapter == null) {
            println("❌ BLE Scanner: Bluetooth not supported on this device")
            onScanFailed("Bluetooth not supported on this device")
            return false
        }
        println("✅ BLE Scanner: Bluetooth adapter available")

        // Check if Bluetooth is enabled
        if (!bluetoothManager.adapter.isEnabled) {
            println("❌ BLE Scanner: Bluetooth is not enabled")
            onScanFailed("Bluetooth is not enabled")
            return false
        }
        println("✅ BLE Scanner: Bluetooth is enabled")

        // Check if BLE is supported
        if (!bluetoothManager.adapter.isMultipleAdvertisementSupported) {
            println("❌ BLE Scanner: BLE advertising not supported on this device")
            onScanFailed("BLE advertising not supported on this device")
            return false
        }
        println("✅ BLE Scanner: BLE advertising supported")

        // Check if scanner is available
        if (scanner == null) {
            println("❌ BLE Scanner: BLE scanner not available")
            onScanFailed("BLE scanner not available")
            return false
        }
        println("✅ BLE Scanner: BLE scanner available")

        try {
            // ✅ UPDATED: Scan for service data instead of service UUID
            // Since we removed the UUID from advertising to fix data size, we scan for any device
            // and then filter by service data in the callback
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0) // Report results immediately
                .build()

            println("🔍 BLE SCANNER DEBUG - SCAN SETTINGS:")
            println("📡 Scan Mode: LOW_LATENCY")
            println("📡 Report Delay: 0ms")
            println("📡 Filters: None (scanning all devices)")

            scanner.startScan(null, settings, scanCallback)
            println("✅ BLE Scanner: Started scanning for BLE devices")
            return true
        } catch (e: Exception) {
            val errorMsg = "Failed to start scan: ${e.message}"
            println("BLE Scanner: $errorMsg")
            onScanFailed(errorMsg)
            return false
        }
    }

    fun stopScanning() {
        try {
            scanner?.stopScan(scanCallback)
            println("BLE Scanner: Stopped scanning")
        } catch (e: Exception) {
            println("BLE Scanner: Error stopping scan: ${e.message}")
        }
    }
}