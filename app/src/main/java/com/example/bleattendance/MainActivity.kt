package com.example.bleattendance

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.bleattendance.navigation.AppNavHost
import com.example.bleattendance.ui.theme.BLEAttendanceFullscreenTheme
import com.example.bleattendance.utils.ActivityPermissionManager
import com.example.bleattendance.utils.REQUEST_ENABLE_BT

class MainActivity : FragmentActivity() {
    
    private lateinit var permissionManager: ActivityPermissionManager
    private var permissionsGranted = false
    private var uiInitialized = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            permissionManager = ActivityPermissionManager(this)
            
            // Request permissions immediately
            requestPermissionsAndSetup()
        } catch (e: Exception) {
            // If there's any error, just show the UI
            permissionsGranted = false
            setupUI()
        }
    }
    
    private fun requestPermissionsAndSetup() {
        permissionManager.requestPermissions { granted ->
            permissionsGranted = granted
            if (granted) {
                // Check if Bluetooth is enabled
                if (!isBluetoothEnabled()) {
                    requestBluetoothEnable()
                } else {
                    setupUI()
                }
            } else {
                // Show UI even if permissions are denied
                setupUI()
            }
        }
    }
    
    private fun setupUI() {
        if (!uiInitialized) {
            uiInitialized = true
            setContent {
                BLEAttendanceFullscreenTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavHost(permissionsGranted = permissionsGranted)
                    }
                }
            }
        }
    }
    
    private fun isBluetoothEnabled(): Boolean {
        return try {
            // Check if we have the required permissions first
            if (!permissionsGranted) {
                return false
            }
            
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
            bluetoothManager.adapter?.isEnabled == true
        } catch (e: SecurityException) {
            // Permission denied or not granted
            false
        } catch (e: Exception) {
            false
        }
    }
    
    private fun requestBluetoothEnable() {
        try {
            // Check if we have the required permissions first
            if (!permissionsGranted) {
                setupUI()
                return
            }
            
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            
            if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else {
                setupUI()
            }
        } catch (e: SecurityException) {
            // Permission denied or not granted
            setupUI()
        } catch (e: Exception) {
            // If there's any error, just show the UI
            setupUI()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                // Always show UI regardless of result
                setupUI()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Re-check permissions when returning from settings
        if (!permissionsGranted && uiInitialized) {
            try {
                permissionManager.requestPermissions { granted ->
                    permissionsGranted = granted
                    if (granted && !isBluetoothEnabled()) {
                        requestBluetoothEnable()
                    }
                }
            } catch (e: Exception) {
                // If there's any error, just continue
            }
        }
    }
}
