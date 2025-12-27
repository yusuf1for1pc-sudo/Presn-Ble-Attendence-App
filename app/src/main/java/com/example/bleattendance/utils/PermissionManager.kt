package com.example.bleattendance.utils

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionManager(
    onPermissionsGranted: @Composable () -> Unit
) {
    var shouldShowApp by remember { mutableStateOf(false) }
    var permissionStatus by remember { mutableStateOf("Checking permissions...") }
    
    // Define the list of permissions needed based on the Android version.
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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

    val permissionState = rememberMultiplePermissionsState(permissions)

    // When the composable is first launched, request the permissions.
    LaunchedEffect(Unit) {
        try {
            if (!permissionState.allPermissionsGranted) {
                permissionStatus = "Requesting Bluetooth permissions..."
                permissionState.launchMultiplePermissionRequest()
            } else {
                permissionStatus = "All permissions granted"
                shouldShowApp = true
            }
        } catch (e: Exception) {
            permissionStatus = "Permission request failed: ${e.message}"
            println("Permission request failed: ${e.message}")
        }
    }

    // Check if permissions are granted and show app
    if (permissionState.allPermissionsGranted) {
        shouldShowApp = true
        onPermissionsGranted()
    } else {
        // Show permission request UI
        androidx.compose.material3.Text(
            text = "Bluetooth permissions are required for attendance marking. Please grant permissions to continue.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
        )
    }
}

// Enhanced PermissionManager for Activity-based permission handling
class ActivityPermissionManager(private val activity: FragmentActivity) {
    
    private var permissionCallback: ((Boolean) -> Unit)? = null
    
    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        
        if (allGranted) {
            permissionCallback?.invoke(true)
        } else {
            // Some permissions denied
            permissionCallback?.invoke(false)
        }
    }
    
    fun requestPermissions(callback: (Boolean) -> Unit) {
        permissionCallback = callback
        
        val permissions = getRequiredPermissions()
        val permissionsToRequest = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isEmpty()) {
            // All permissions already granted
            callback(true)
            return
        }
        
        // Request permissions directly
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }
    
    companion object {
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
    }
}

// Utility functions for checking BLE status
fun hasRequiredPermissions(context: Context): Boolean {
    val permissions = ActivityPermissionManager.getRequiredPermissions()
    
    return permissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}

fun isBluetoothAvailable(context: Context): Boolean {
    return try {
        // Check if we have the required permissions first
        if (!hasRequiredPermissions(context)) {
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

fun isBluetoothEnabled(context: Context): Boolean {
    return try {
        // Check if we have the required permissions first
        if (!hasRequiredPermissions(context)) {
            return false
        }
        
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter?.isEnabled == true
    } catch (e: SecurityException) {
        // Permission denied or not granted
        false
    } catch (e: Exception) {
        // Other exceptions
        false
    }
}

fun requestBluetoothEnable(activity: Activity): Boolean {
    return try {
        // Check if we have the required permissions first
        if (!hasRequiredPermissions(activity)) {
            return false
        }
        
        val bluetoothManager = activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        
        if (bluetoothAdapter == null) {
            return false
        }
        
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            return false
        }
        
        return true
    } catch (e: SecurityException) {
        // Permission denied or not granted
        false
    } catch (e: Exception) {
        // Other exceptions
        false
    }
}

const val REQUEST_ENABLE_BT = 1
