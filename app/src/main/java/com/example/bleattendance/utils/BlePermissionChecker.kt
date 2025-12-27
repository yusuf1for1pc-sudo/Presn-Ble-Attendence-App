package com.example.bleattendance.utils

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Utility class to check BLE permissions and provide detailed status information
 */
class BlePermissionChecker(private val context: Context) {
    
    /**
     * Check if all required BLE permissions are granted
     */
    fun hasRequiredPermissions(): Boolean {
        val permissions = getRequiredPermissions()
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Get the list of required permissions based on Android version
     */
    fun getRequiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }
    
    /**
     * Check if Bluetooth is available and enabled
     */
    fun isBluetoothAvailable(): Boolean {
        return try {
            // Check if we have the required permissions first
            if (!hasRequiredPermissions()) {
                return false
            }
            
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            bluetoothAdapter != null && bluetoothAdapter.isEnabled
        } catch (e: SecurityException) {
            // Permission denied or not granted
            false
        } catch (e: Exception) {
            // Other exceptions
            false
        }
    }
    
    /**
     * Check if BLE is supported on this device
     */
    fun isBleSupported(): Boolean {
        return try {
            // Check if we have the required permissions first
            if (!hasRequiredPermissions()) {
                return false
            }
            
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            bluetoothAdapter?.isMultipleAdvertisementSupported == true
        } catch (e: SecurityException) {
            // Permission denied or not granted
            false
        } catch (e: Exception) {
            // Other exceptions
            false
        }
    }
    
    /**
     * Get detailed BLE status information
     */
    fun getBleStatus(): BleStatus {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            
            when {
                bluetoothAdapter == null -> {
                    BleStatus(
                        isAvailable = false,
                        isEnabled = false,
                        isSupported = false,
                        statusMessage = "Bluetooth not supported on this device",
                        missingPermissions = getMissingPermissions(),
                        canUseBle = false
                    )
                }
                !hasRequiredPermissions() -> {
                    BleStatus(
                        isAvailable = true,
                        isEnabled = try { bluetoothAdapter.isEnabled } catch (e: SecurityException) { false },
                        isSupported = try { bluetoothAdapter.isMultipleAdvertisementSupported } catch (e: SecurityException) { false },
                        statusMessage = "Bluetooth permissions required",
                        missingPermissions = getMissingPermissions(),
                        canUseBle = false
                    )
                }
                !bluetoothAdapter.isEnabled -> {
                    BleStatus(
                        isAvailable = true,
                        isEnabled = false,
                        isSupported = bluetoothAdapter.isMultipleAdvertisementSupported,
                        statusMessage = "Please enable Bluetooth in device settings",
                        missingPermissions = getMissingPermissions(),
                        canUseBle = false
                    )
                }
                !bluetoothAdapter.isMultipleAdvertisementSupported -> {
                    BleStatus(
                        isAvailable = true,
                        isEnabled = true,
                        isSupported = false,
                        statusMessage = "BLE not supported on this device",
                        missingPermissions = getMissingPermissions(),
                        canUseBle = false
                    )
                }
                else -> {
                    BleStatus(
                        isAvailable = true,
                        isEnabled = true,
                        isSupported = true,
                        statusMessage = "BLE is ready to use",
                        missingPermissions = emptyList(),
                        canUseBle = true
                    )
                }
            }
        } catch (e: SecurityException) {
            // Permission denied or not granted
            BleStatus(
                isAvailable = false,
                isEnabled = false,
                isSupported = false,
                statusMessage = "Bluetooth permissions required",
                missingPermissions = getMissingPermissions(),
                canUseBle = false
            )
        } catch (e: Exception) {
            // Other exceptions
            BleStatus(
                isAvailable = false,
                isEnabled = false,
                isSupported = false,
                statusMessage = "Error checking Bluetooth status: ${e.message}",
                missingPermissions = getMissingPermissions(),
                canUseBle = false
            )
        }
    }
    
    /**
     * Refresh BLE status (useful after permission changes)
     */
    fun refreshBleStatus(): BleStatus {
        return getBleStatus()
    }
    
    /**
     * Get list of missing permissions
     */
    private fun getMissingPermissions(): List<String> {
        val permissions = getRequiredPermissions()
        return permissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Check if this is a problematic device (like Pixel devices)
     */
    fun isProblematicDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()
        return manufacturer.contains("google") && model.contains("pixel")
    }
    
    /**
     * Get device compatibility information
     */
    fun getDeviceCompatibilityInfo(): String {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()
        
        return when {
            manufacturer.contains("google") && model.contains("pixel") -> {
                "Google Pixel detected. Some Pixel devices have BLE compatibility issues."
            }
            manufacturer.contains("samsung") -> {
                "Samsung device detected. BLE should work normally."
            }
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
                "Xiaomi/Redmi device detected. BLE should work normally."
            }
            else -> {
                "Device: $manufacturer $model. BLE compatibility may vary."
            }
        }
    }
    
    /**
     * Data class representing BLE status
     */
    data class BleStatus(
        val isAvailable: Boolean,
        val isEnabled: Boolean,
        val isSupported: Boolean,
        val statusMessage: String,
        val missingPermissions: List<String>,
        val canUseBle: Boolean
    )
}
