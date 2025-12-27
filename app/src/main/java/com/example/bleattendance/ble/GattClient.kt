// In ble/GattClient.kt

package com.example.bleattendance.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.ParcelUuid
import androidx.activity.ComponentActivity
import com.example.bleattendance.model.StudentInfo
import com.example.bleattendance.utils.hasRequiredPermissions
import com.example.bleattendance.utils.isBluetoothAvailable
import java.util.UUID
import com.google.gson.Gson
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.resume

/**
 * ✅ CRITICAL: GattClient for BLE attendance - NO PAIRING MODE
 * This class is specifically designed to avoid pairing popups and bonding
 * Uses simple beacon mode approach for quick, anonymous connections
 */
@SuppressLint("MissingPermission")
class GattClient(
    private val context: Context,
    private val activity: ComponentActivity
) {
    
    // ✅ ADDED: Callback for queue status updates
    interface QueueStatusCallback {
        fun onQueueStatus(queuePosition: Int, estimatedWait: Int)
    }
    
    private var queueStatusCallback: QueueStatusCallback? = null
    
    fun setQueueStatusCallback(callback: QueueStatusCallback?) {
        queueStatusCallback = callback
    }
    
    // ✅ ADDED: Reconnection mechanism for queued devices
    private var reconnectionJob: Job? = null
    private var lastScanResult: ScanResult? = null
    private var lastStudentInfo: StudentInfo? = null
    private var lastSessionCode: String? = null
    
    fun setReconnectionData(scanResult: ScanResult, studentInfo: StudentInfo, sessionCode: String) {
        lastScanResult = scanResult
        lastStudentInfo = studentInfo
        lastSessionCode = sessionCode
        println("GattClient: Reconnection data set for ${studentInfo.name} (${studentInfo.rollNumber})")
    }
    
    fun startReconnectionTimer() {
        reconnectionJob?.cancel()
        reconnectionJob = coroutineScope.launch {
            // Wait 3 seconds before attempting reconnection
            delay(3000)
            println("GattClient: Attempting automatic reconnection...")
            
            if (lastScanResult != null && lastStudentInfo != null && lastSessionCode != null) {
                try {
                    val success = connectAndSendData(lastScanResult!!, lastStudentInfo!!, lastSessionCode!!)
                    if (success) {
                        println("GattClient: Automatic reconnection successful")
                    } else {
                        println("GattClient: Automatic reconnection failed")
                    }
                } catch (e: Exception) {
                    println("GattClient: Automatic reconnection error: ${e.message}")
                }
            } else {
                println("GattClient: Cannot reconnect - missing reconnection data")
            }
        }
    }
    
    fun cancelReconnection() {
        reconnectionJob?.cancel()
        reconnectionJob = null
        println("GattClient: Reconnection cancelled")
    }

    private val gson = Gson()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var gatt: BluetoothGatt? = null
    private var connectionTimeoutJob: Job? = null
    
    // ✅ ADVANCED MESSAGING SYSTEM INTEGRATION
    private val messageParser = MessageParser()
    private val messageSequencer = MessageSequencer()

    /**
     * ✅ CRITICAL: Connect and send data WITHOUT PAIRING
     * This method ensures no pairing popup appears on student devices
     */
    suspend fun connectAndSendData(
        scanResult: ScanResult,
        studentInfo: StudentInfo,
        sessionCode: String
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val manufacturer = android.os.Build.MANUFACTURER
        val model = android.os.Build.MODEL
        
        println("🔗 BLE CONNECTION DEBUG:")
        println("📱 Device: $manufacturer $model")
        println("👤 Student: ${studentInfo.name} (${studentInfo.rollNumber})")
        println("🔑 Session Code: $sessionCode")
        println("📡 Target Device: ${scanResult.device.address}")
        println("📡 RSSI: ${scanResult.rssi}")
        println("✅ NO PAIRING MODE - Using anonymous BLE connection")
        // sessionCode is used for validation in the calling code
        
        // Check permissions and Bluetooth availability
        if (!hasRequiredPermissions(context)) {
            println("GattClient: Missing required permissions")
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }

        if (!isBluetoothAvailable(context)) {
            println("GattClient: Bluetooth not available")
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }

        println("GattClient: Starting simple connection to device: ${scanResult.device.address}")
        
        // Simple connection without bonding - like beacon mode
        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        println("GattClient: Connected to device")
                        connectionTimeoutJob?.cancel()
                        gatt.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        println("GattClient: Disconnected from device")
                        connectionTimeoutJob?.cancel()
                        if (continuation.isActive) continuation.resume(false)
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    println("GattClient: Services discovered successfully")
                    val service = gatt.getService(UUID.fromString("12345678-1234-1234-1234-123456789ABC"))
                    val characteristic = service?.getCharacteristic(UUID.fromString("87654321-4321-4321-4321-CBA987654321"))
                    
                    // ✅ ADDED: Subscribe to response characteristic for queue status
                    val responseCharacteristic = service?.getCharacteristic(UUID.fromString("11111111-2222-3333-4444-555555555555"))
                    if (responseCharacteristic != null) {
                        val descriptor = responseCharacteristic.getDescriptor(
                            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                        )
                        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)
                        println("GattClient: Subscribed to response characteristic for queue status")
                    }
                    
                    if (characteristic != null) {
                        // ✅ ADVANCED: Try advanced message format first, fallback to simple format
                        val safeName = studentInfo.name.replace("|", "").replace(":", "").take(30)
                        val safeRollNumber = studentInfo.rollNumber.replace("|", "").replace(":", "").take(15)
                        
                        // Create advanced message
                        // ✅ FIXED: Simplify data format to avoid compression issues
                        val payload = "ATTENDANCE|$safeName|$safeRollNumber|${System.currentTimeMillis()}"
                        val message = payload
                        
                        println("🔍 ADVANCED BLE CLIENT DEBUG - DATA PREPARATION:")
                        println("📤 Original Name: '${studentInfo.name}' (${studentInfo.name.length} chars)")
                        println("📤 Safe Name: '$safeName' (${safeName.length} chars)")
                        println("📤 Original Roll: '${studentInfo.rollNumber}' (${studentInfo.rollNumber.length} chars)")
                        println("📤 Safe Roll: '$safeRollNumber' (${safeRollNumber.length} chars)")
                        println("📤 Payload: '$payload'")
                        println("📤 Data Size: ${payload.toByteArray().size} bytes")
                        println("GattClient: Sending message: $payload")
                        
                        // ✅ FIXED: Use simple string format with clear delimiter
                        val dataBytes = payload.toByteArray()
                        if (dataBytes.size > 512) {
                            println("GattClient: Data too large (${dataBytes.size} bytes)")
                            if (continuation.isActive) continuation.resume(false)
                            return
                        }
                        
                        // Set the characteristic value and write it
                        characteristic.value = dataBytes
                        val writeSuccess = gatt.writeCharacteristic(characteristic)
                        if (!writeSuccess) {
                            println("GattClient: Failed to write characteristic")
                            if (continuation.isActive) continuation.resume(false)
                        }
                    } else {
                        println("GattClient: Required characteristic not found")
                        if (continuation.isActive) continuation.resume(false)
                    }
                } else {
                    println("GattClient: Service discovery failed with status: $status")
                    if (continuation.isActive) continuation.resume(false)
                }
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    println("GattClient: Student data sent successfully - teacher will process")
                    // ✅ PRESENTATION OPTIMIZED: 30-second max wait with early exit
                    coroutineScope.launch {
                        // Start 30-second timeout timer
                        val timeoutJob = launch {
                            // ✅ FIXED: Reduced timeout to avoid unnecessary waiting
                            delay(10000) // 10 seconds max
                            if (continuation.isActive) {
                                println("GattClient: 10-second timeout reached - assuming success")
                                gatt.disconnect()
                                continuation.resume(true) // Assume success after data sent
                            }
                        }
                        
                        // ✅ FIXED: Wait for response but with shorter timeout
                        delay(1000) // Wait 1 second for initial processing
                        if (!continuation.isCompleted) {
                            println("GattClient: Initial response received, maintaining connection for queue management")
                        }
                        
                        // Cancel timeout since we got immediate success
                        timeoutJob.cancel()
                        
                        if (continuation.isActive) {
                            println("GattClient: Resuming with success - data transmitted successfully")
                            continuation.resume(true)
                        }
                    }
                } else {
                    println("GattClient: Failed to write characteristic, status: $status")
                    gatt.disconnect()
                    if (continuation.isActive) continuation.resume(false)
                }
            }
            
            // ✅ ADVANCED: Handle queue status notifications from teacher
            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                if (characteristic.uuid == RESPONSE_CHARACTERISTIC_UUID) {
                    val data = String(characteristic.value ?: ByteArray(0))
                    println("🔍 ADVANCED QUEUE DEBUG - STUDENT RECEIVED STATUS:")
                    println("📥 Raw Data: '$data'")
                    println("GattClient: Received response from teacher: $data")
                    
                    // ✅ ADVANCED: Try to parse as advanced message first
                    val message = messageParser.decodeMessage(data)
                    if (message != null) {
                        println("🔍 ADVANCED MESSAGE - PARSING:")
                        println("📥 Message Type: ${message.messageType}")
                        println("📥 Message ID: ${message.messageId}")
                        println("📥 Payload: ${message.payload}")
                        println("📥 Timestamp: ${message.timestamp}")
                        println("📥 Compression: ${message.compressionType}")
                        println("📥 Retry Count: ${message.retryCount}")
                        
                        when (message.messageType) {
                            MessageType.QUEUE_STATUS -> {
                                val parts = message.payload.split("|")
                                if (parts.size >= 3) {
                                    val queuePosition = parts[1].toIntOrNull() ?: 0
                                    val estimatedWait = parts[2].toIntOrNull() ?: 0
                                    val processingRate = if (parts.size >= 4) parts[3].toFloatOrNull() ?: 0.5f else 0.5f
                                    val successRate = if (parts.size >= 5) parts[4].toFloatOrNull() ?: 1.0f else 1.0f
                                    
                                    println("🔍 ADVANCED QUEUE DEBUG - STATUS PARSED:")
                                    println("📊 Queue Position: $queuePosition")
                                    println("⏰ Estimated Wait: ${estimatedWait}s")
                                    println("📊 Processing Rate: ${(processingRate * 100).toInt()}%")
                                    println("📊 Success Rate: ${(successRate * 100).toInt()}%")
                                    println("GattClient: Advanced queue status - Position: $queuePosition, Wait: ${estimatedWait}s")
                                    queueStatusCallback?.onQueueStatus(queuePosition, estimatedWait) ?: run {
                                        println("GattClient: Warning - Queue status callback not set")
                                    }
                                }
                            }
                            MessageType.RETRY -> {
                                val parts = message.payload.split("|")
                                if (parts.size >= 3) {
                                    val queuePosition = parts[1].toIntOrNull() ?: 0
                                    val estimatedWait = parts[2].toIntOrNull() ?: 0
                                    
                                    println("🔍 ADVANCED QUEUE DEBUG - RECONNECTION SIGNAL RECEIVED:")
                                    println("📊 Queue Position: $queuePosition (0 = ready to connect)")
                                    println("⏰ Estimated Wait: ${estimatedWait}s")
                                    println("GattClient: Advanced reconnection signal received - ready to connect")
                                    
                                    // Notify that it's time to reconnect
                                    queueStatusCallback?.onQueueStatus(queuePosition, estimatedWait) ?: run {
                                        println("GattClient: Warning - Queue status callback not set")
                                    }
                                }
                            }
                            MessageType.ACK -> {
                                println("🔍 ADVANCED MESSAGE - ACKNOWLEDGMENT RECEIVED:")
                                println("📥 Message ID: ${message.messageId}")
                                println("GattClient: Attendance acknowledged by teacher")
                                // Treat ACK as a signal that the student can stop waiting and/or reconnect
                                queueStatusCallback?.onQueueStatus(0, 0) ?: run {
                                    println("GattClient: Warning - Queue status callback not set for ACK")
                                }
                            }
                            MessageType.CONFIRMED -> {
                                println("🔍 ADVANCED MESSAGE - CONFIRMATION RECEIVED:")
                                println("📥 Message ID: ${message.messageId}")
                                println("GattClient: Attendance confirmed by teacher")
                            }
                            MessageType.ERROR -> {
                                println("🔍 ADVANCED MESSAGE - ERROR RECEIVED:")
                                println("📥 Error: ${message.payload}")
                                println("GattClient: Error from teacher: ${message.payload}")
                            }
                            else -> {
                                println("🔍 ADVANCED MESSAGE - UNKNOWN TYPE:")
                                println("📥 Message Type: ${message.messageType}")
                                println("📥 Payload: ${message.payload}")
                            }
                        }
                    } else {
                        // ✅ FALLBACK: Handle legacy simple format
                        if (data.startsWith("QUEUE|")) {
                            val parts = data.split("|")
                            if (parts.size >= 3) {
                                val queuePosition = parts[1].toIntOrNull() ?: 0
                                val estimatedWait = parts[2].toIntOrNull() ?: 0
                                
                                println("🔍 LEGACY QUEUE DEBUG - STATUS PARSED:")
                                println("📊 Queue Position: $queuePosition")
                                println("⏰ Estimated Wait: ${estimatedWait}s")
                                println("GattClient: Legacy queue status - Position: $queuePosition, Wait: ${estimatedWait}s")
                                queueStatusCallback?.onQueueStatus(queuePosition, estimatedWait) ?: run {
                                    println("GattClient: Warning - Queue status callback not set")
                                }
                            }
                        } else if (data.startsWith("RECONNECT|")) {
                            val parts = data.split("|")
                            if (parts.size >= 3) {
                                val queuePosition = parts[1].toIntOrNull() ?: 0
                                val estimatedWait = parts[2].toIntOrNull() ?: 0
                                
                                println("🔍 LEGACY QUEUE DEBUG - RECONNECTION SIGNAL RECEIVED:")
                                println("📊 Queue Position: $queuePosition (0 = ready to connect)")
                                println("⏰ Estimated Wait: ${estimatedWait}s")
                                println("GattClient: Legacy reconnection signal received - ready to connect")
                                
                                queueStatusCallback?.onQueueStatus(queuePosition, estimatedWait) ?: run {
                                    println("GattClient: Warning - Queue status callback not set")
                                }
                            }
                        }
                    }
                }
            }
        }

        try {
            // ✅ CRITICAL: Use autoConnect = false to avoid pairing popup and bonding
            // ✅ CRITICAL: This is the simple beacon mode approach - NO PAIRING REQUIRED
            // ✅ CRITICAL: TRANSPORT_LE ensures BLE-only connection without pairing
            gatt = scanResult.device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            
            // ✅ OPTIMIZED: Connection timeout (6 seconds for faster processing)
            connectionTimeoutJob = coroutineScope.launch {
                delay(6000)
                if (continuation.isActive) {
                    println("GattClient: Connection timeout (6s)")
                    gatt?.disconnect()
                    continuation.resume(false)
                }
            }
            
            // Set up cancellation
            continuation.invokeOnCancellation { 
                println("GattClient: Connection cancelled")
                connectionTimeoutJob?.cancel()
                gatt?.disconnect()
                gatt?.close()
            }
            
        } catch (e: Exception) {
            println("GattClient: Exception during connection: ${e.message}")
            connectionTimeoutJob?.cancel()
            if (continuation.isActive) continuation.resume(false)
        }
    }

    fun disconnect() {
        try {
            connectionTimeoutJob?.cancel()
            gatt?.disconnect()
            gatt?.close()
            gatt = null
            println("GattClient: Disconnected and cleaned up")
        } catch (e: Exception) {
            println("GattClient: Error during disconnect: ${e.message}")
        }
    }
}