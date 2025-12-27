package com.example.bleattendance.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.ParcelUuid
import androidx.activity.ComponentActivity
import com.example.bleattendance.model.StudentInfo
import com.example.bleattendance.utils.hasRequiredPermissions
import com.example.bleattendance.utils.isBluetoothAvailable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.resume
import java.util.UUID
import com.google.gson.Gson

/**
 * Universal BLE Connector that handles bonding and connection for all Android devices (6-15)
 * This class specifically addresses the Pixel pairing popup issue by implementing proper bonding flow
 */
@SuppressLint("MissingPermission")
class UniversalBleConnector(
    private val context: Context,
    private val activity: ComponentActivity
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()
    
    private var gatt: BluetoothGatt? = null
    private var currentDevice: BluetoothDevice? = null
    private var bondingReceiver: BroadcastReceiver? = null
    
    /**
     * Universal connection method that handles bonding and connection for all Android versions
     */
    suspend fun connectAndSendData(
        scanResult: ScanResult,
        studentInfo: StudentInfo,
        sessionCode: String
    ): Boolean = suspendCancellableCoroutine { continuation ->
        
        // Check permissions and Bluetooth availability
        if (!hasRequiredPermissions(context)) {
            println("UniversalBleConnector: Missing required permissions")
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }

        if (!isBluetoothAvailable(context)) {
            println("UniversalBleConnector: Bluetooth not available")
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }

        // Verify session code from advertised data
        val advertisedData = scanResult.scanRecord?.getServiceData(ParcelUuid(UUID.fromString("12345678-1234-1234-1234-123456789ABC")))
        val advertisedCode = advertisedData?.let { String(it) }
        if (advertisedCode != sessionCode) {
            println("UniversalBleConnector: Session code mismatch. Expected: $sessionCode, Found: $advertisedCode")
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }

        currentDevice = scanResult.device
        println("UniversalBleConnector: Starting universal connection to device: ${scanResult.device.address}")
        
        // Start the universal connection flow
        startUniversalConnectionFlow(scanResult.device, studentInfo, continuation)
    }
    
    /**
     * Universal connection flow that handles different Android versions
     */
    private fun startUniversalConnectionFlow(
        device: BluetoothDevice,
        studentInfo: StudentInfo,
        continuation: kotlinx.coroutines.CancellableContinuation<Boolean>
    ) {
        println("UniversalBleConnector: Device manufacturer: ${device.name ?: "Unknown"}")
        println("UniversalBleConnector: Device address: ${device.address}")
        println("UniversalBleConnector: Current bond state: ${device.bondState}")
        println("UniversalBleConnector: Android version: ${Build.VERSION.SDK_INT}")
        
        // Simplified approach: Try direct connection first, only bond if needed
        when (device.bondState) {
            BluetoothDevice.BOND_BONDED -> {
                println("UniversalBleConnector: Device already bonded, connecting directly")
                connectToDevice(device, studentInfo, continuation)
            }
            BluetoothDevice.BOND_NONE -> {
                println("UniversalBleConnector: Device not bonded, trying direct connection first")
                tryDirectConnection(device, studentInfo, continuation)
            }
            BluetoothDevice.BOND_BONDING -> {
                println("UniversalBleConnector: Device is already bonding, waiting...")
                waitForBondingComplete(device, studentInfo, continuation)
            }
        }
    }
    
    /**
     * Try direct connection first, only bond if connection fails
     */
    private fun tryDirectConnection(
        device: BluetoothDevice,
        studentInfo: StudentInfo,
        continuation: kotlinx.coroutines.CancellableContinuation<Boolean>
    ) {
        println("UniversalBleConnector: Attempting direct GATT connection without bonding")
        
        var connectionTimeoutJob: kotlinx.coroutines.Job? = null
        
        val tempGattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        println("UniversalBleConnector: Direct connection successful!")
                        connectionTimeoutJob?.cancel()
                        gatt.disconnect()
                        gatt.close()
                        // If direct connection works, proceed with the actual connection
                        connectToDevice(device, studentInfo, continuation)
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        println("UniversalBleConnector: Direct connection failed, trying bonding")
                        connectionTimeoutJob?.cancel()
                        gatt.close()
                        // If direct connection fails, try bonding
                        startBondingProcess(device, studentInfo, continuation)
                    }
                }
            }
        }
        
        try {
            val tempGatt = device.connectGatt(context, false, tempGattCallback, BluetoothDevice.TRANSPORT_LE)
            
            // Set a short timeout for direct connection attempt
            connectionTimeoutJob = coroutineScope.launch {
                delay(5000) // 5 second timeout for direct connection
                if (continuation.isActive) {
                    println("UniversalBleConnector: Direct connection timeout, trying bonding")
                    tempGatt.disconnect()
                    tempGatt.close()
                    startBondingProcess(device, studentInfo, continuation)
                }
            }
            
        } catch (e: Exception) {
            println("UniversalBleConnector: Direct connection failed with exception: ${e.message}")
            connectionTimeoutJob?.cancel()
            // Fall back to bonding
            startBondingProcess(device, studentInfo, continuation)
        }
    }
    
    /**
     * Start the bonding process and register for bond state changes
     */
    private fun startBondingProcess(
        device: BluetoothDevice,
        studentInfo: StudentInfo,
        continuation: kotlinx.coroutines.CancellableContinuation<Boolean>
    ) {
        println("UniversalBleConnector: Starting bonding process for device: ${device.address}")
        
        // Register broadcast receiver for bond state changes
        bondingReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                    val bondDevice = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                    
                    if (bondDevice?.address == device.address) {
                        when (bondState) {
                            BluetoothDevice.BOND_BONDED -> {
                                println("UniversalBleConnector: Bonding successful!")
                                unregisterBondingReceiver()
                                connectToDevice(device, studentInfo, continuation)
                            }
                            BluetoothDevice.BOND_NONE -> {
                                println("UniversalBleConnector: Bonding failed or cancelled")
                                unregisterBondingReceiver()
                                if (continuation.isActive) continuation.resume(false)
                            }
                        }
                    }
                }
            }
        }
        
        // Register the receiver
        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        context.registerReceiver(bondingReceiver, filter)
        
        // For Pixel devices and Android 15, try multiple bonding strategies
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE || 
            device.name?.contains("Pixel", ignoreCase = true) == true) {
            println("UniversalBleConnector: Using enhanced bonding for Pixel/Android 15")
            
            // Strategy 1: Try to create bond with PIN pairing
            try {
                val bondingStarted = device.createBond()
                if (bondingStarted) {
                    println("UniversalBleConnector: Bonding started successfully")
                } else {
                    println("UniversalBleConnector: Standard bonding failed, trying alternative")
                    // Strategy 2: Try to connect GATT first to trigger pairing
                    tryAlternativeBonding(device, studentInfo, continuation)
                }
            } catch (e: Exception) {
                println("UniversalBleConnector: Bonding exception: ${e.message}")
                tryAlternativeBonding(device, studentInfo, continuation)
            }
        } else {
            // Standard bonding for other devices
            val bondingStarted = device.createBond()
            if (!bondingStarted) {
                println("UniversalBleConnector: Failed to start bonding")
                unregisterBondingReceiver()
                if (continuation.isActive) continuation.resume(false)
            }
        }
    }
    
    /**
     * Alternative bonding strategy for Pixel devices
     */
    private fun tryAlternativeBonding(
        device: BluetoothDevice,
        studentInfo: StudentInfo,
        continuation: kotlinx.coroutines.CancellableContinuation<Boolean>
    ) {
        println("UniversalBleConnector: Trying alternative bonding strategy")
        
        // Try to connect GATT first, which often triggers the pairing popup on Pixel devices
        val tempGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        println("UniversalBleConnector: GATT connected, now trying to bond")
                        gatt.disconnect()
                        gatt.close()
                        // Now try bonding again
                        if (device.createBond()) {
                            println("UniversalBleConnector: Alternative bonding started")
                        } else {
                            println("UniversalBleConnector: Alternative bonding failed")
                            if (continuation.isActive) continuation.resume(false)
                        }
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        println("UniversalBleConnector: GATT disconnected during bonding attempt")
                        gatt.close()
                        // Final attempt with standard bonding
                        if (device.createBond()) {
                            println("UniversalBleConnector: Final bonding attempt started")
                        } else {
                            println("UniversalBleConnector: All bonding attempts failed")
                            if (continuation.isActive) continuation.resume(false)
                        }
                    }
                }
            }
        }, BluetoothDevice.TRANSPORT_LE)
        
        // Set a timeout for the alternative bonding approach
        coroutineScope.launch {
            delay(10000) // 10 second timeout
            if (continuation.isActive && device.bondState != BluetoothDevice.BOND_BONDED) {
                println("UniversalBleConnector: Alternative bonding timeout")
                tempGatt.disconnect()
                tempGatt.close()
                // Final fallback
                if (continuation.isActive) continuation.resume(false)
            }
        }
    }
    
    /**
     * Wait for bonding to complete if already in progress
     */
    private fun waitForBondingComplete(
        device: BluetoothDevice,
        studentInfo: StudentInfo,
        continuation: kotlinx.coroutines.CancellableContinuation<Boolean>
    ) {
        // Register broadcast receiver for bond state changes
        bondingReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                    val bondDevice = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                    
                    if (bondDevice?.address == device.address) {
                        when (bondState) {
                            BluetoothDevice.BOND_BONDED -> {
                                println("UniversalBleConnector: Bonding completed")
                                unregisterBondingReceiver()
                                connectToDevice(device, studentInfo, continuation)
                            }
                            BluetoothDevice.BOND_NONE -> {
                                println("UniversalBleConnector: Bonding failed")
                                unregisterBondingReceiver()
                                if (continuation.isActive) continuation.resume(false)
                            }
                        }
                    }
                }
            }
        }
        
        // Register the receiver
        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        context.registerReceiver(bondingReceiver, filter)
        
        // Set a timeout for bonding
        coroutineScope.launch {
            delay(30000) // 30 second timeout for bonding
            if (continuation.isActive) {
                println("UniversalBleConnector: Bonding timeout")
                unregisterBondingReceiver()
                continuation.resume(false)
            }
        }
    }
    
    /**
     * Connect to the device using GATT after bonding is complete
     */
    private fun connectToDevice(
        device: BluetoothDevice,
        studentInfo: StudentInfo,
        continuation: kotlinx.coroutines.CancellableContinuation<Boolean>
    ) {
        println("UniversalBleConnector: Connecting to device via GATT")
        
        var connectionTimeoutJob: kotlinx.coroutines.Job? = null
        
        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        println("UniversalBleConnector: Connected to device")
                        connectionTimeoutJob?.cancel()
                        gatt.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        println("UniversalBleConnector: Disconnected from device")
                        connectionTimeoutJob?.cancel()
                        if (continuation.isActive) continuation.resume(false)
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    println("UniversalBleConnector: Services discovered successfully")
                    val service = gatt.getService(UUID.fromString("12345678-1234-1234-1234-123456789ABC"))
                    val characteristic = service?.getCharacteristic(UUID.fromString("87654321-4321-4321-4321-CBA987654321"))
                    
                    if (characteristic != null) {
                        // Use simple string format instead of JSON to avoid corruption
                        val safeName = studentInfo.name.replace("|", "").replace(":", "").take(30)
                        val safeRollNumber = studentInfo.rollNumber.replace("|", "").replace(":", "").take(15)
                        
                        // Simple format: "name|rollNumber"
                        val studentData = "$safeName|$safeRollNumber"
                        println("UniversalBleConnector: Sending data: $studentData")
                        
                        // Ensure data is not too large for BLE transmission
                        val dataBytes = studentData.toByteArray()
                        if (dataBytes.size > 512) {
                            println("UniversalBleConnector: Data too large (${dataBytes.size} bytes)")
                            if (continuation.isActive) continuation.resume(false)
                            return
                        }
                        
                        characteristic.value = dataBytes
                        val writeSuccess = gatt.writeCharacteristic(characteristic)
                        if (!writeSuccess) {
                            println("UniversalBleConnector: Failed to write characteristic")
                            if (continuation.isActive) continuation.resume(false)
                        }
                    } else {
                        println("UniversalBleConnector: Required characteristic not found")
                        if (continuation.isActive) continuation.resume(false)
                    }
                } else {
                    println("UniversalBleConnector: Service discovery failed with status: $status")
                    if (continuation.isActive) continuation.resume(false)
                }
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    println("UniversalBleConnector: Student data sent successfully")
                    // Wait a moment for server to process before disconnecting
                    coroutineScope.launch {
                        delay(500)
                        gatt.disconnect()
                        if (continuation.isActive) continuation.resume(true)
                    }
                } else {
                    println("UniversalBleConnector: Failed to write characteristic, status: $status")
                    gatt.disconnect()
                    if (continuation.isActive) continuation.resume(false)
                }
            }
        }

        try {
            // Use TRANSPORT_LE for better compatibility
            gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            
            // Connection timeout (15 seconds)
            connectionTimeoutJob = coroutineScope.launch {
                delay(15000)
                if (continuation.isActive) {
                    println("UniversalBleConnector: Connection timeout")
                    gatt?.disconnect()
                    continuation.resume(false)
                }
            }
            
            // Set up cancellation
            continuation.invokeOnCancellation { 
                println("UniversalBleConnector: Connection cancelled")
                connectionTimeoutJob?.cancel()
                gatt?.disconnect()
                gatt?.close()
            }
            
        } catch (e: Exception) {
            println("UniversalBleConnector: Exception during connection: ${e.message}")
            connectionTimeoutJob?.cancel()
            if (continuation.isActive) continuation.resume(false)
        }
    }
    

    
    /**
     * Unregister bonding receiver
     */
    private fun unregisterBondingReceiver() {
        bondingReceiver?.let { receiver ->
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                println("UniversalBleConnector: Error unregistering bonding receiver: ${e.message}")
            }
            bondingReceiver = null
        }
    }
    
    /**
     * Disconnect and cleanup
     */
    fun disconnect() {
        try {
            unregisterBondingReceiver()
            gatt?.disconnect()
            gatt?.close()
            gatt = null
            currentDevice = null
            pendingConnection = null
            println("UniversalBleConnector: Disconnected and cleaned up")
        } catch (e: Exception) {
            println("UniversalBleConnector: Error during disconnect: ${e.message}")
        }
    }
    
    companion object {
        private var pendingConnection: ConnectionRequest? = null
        
        private data class ConnectionRequest(
            val studentInfo: StudentInfo,
            val continuation: kotlinx.coroutines.CancellableContinuation<Boolean>
        )
    }
}
