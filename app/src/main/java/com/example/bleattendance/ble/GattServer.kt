// In ble/GattServer.kt

package com.example.bleattendance.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import com.example.bleattendance.model.StudentInfo
import com.example.bleattendance.utils.hasRequiredPermissions
import com.example.bleattendance.utils.isBluetoothAvailable
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.util.UUID
import kotlinx.coroutines.flow.SharedFlow

@SuppressLint("MissingPermission")
class GattServer(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val advertiser = bluetoothManager.adapter?.bluetoothLeAdvertiser
    private var gattServer: BluetoothGattServer? = null
    private val gson = Gson()
    private var isServerRunning = false
    private var currentSessionCode: String? = null
    
    // Session state management
    private val _sessionStateFlow = MutableSharedFlow<SessionState>()
    val sessionStateFlow = _sessionStateFlow.asSharedFlow()
    
    data class SessionState(
        val isActive: Boolean,
        val sessionCode: String?,
        val serverRunning: Boolean,
        val errorMessage: String? = null
    )

    private val _incomingStudentFlow = MutableSharedFlow<StudentInfo>()
    val incomingStudentFlow = _incomingStudentFlow.asSharedFlow()

    // ✅ ADDED: Status flow for real-time updates
    private val _serverStatusFlow = MutableSharedFlow<String>()
    val serverStatusFlow = _serverStatusFlow.asSharedFlow()

    init {
        // Ensure cleanup of any existing GATT server
        stopServer()
    }

    // ✅ ADVANCED MESSAGING SYSTEM INTEGRATION
    private val smartQueueManager = SmartQueueManager(maxConnections = 5)
    private val messageParser = MessageParser()
    private val messageSequencer = MessageSequencer()
    
    // ✅ ADDED: Connection queue management for handling 60-100 students
    private val maxConnections = 5 // Android BLE limit
    private val activeConnections = mutableSetOf<BluetoothDevice>()
    private val processingConnections = mutableSetOf<BluetoothDevice>() // Connections being processed
    private val connectionQueue = mutableListOf<BluetoothDevice>()
    private val processedStudents = mutableSetOf<String>() // studentEmail -> prevent duplicates
    
    // ✅ ADDED: Message tracking for bidirectional communication
    private val pendingMessages = mutableMapOf<String, BLEMessage>()
    private val messageAcks = mutableSetOf<String>()

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            println("GattServer: Advertising started successfully")
            // Update server running state using the advertised session code
            isServerRunning = true
            currentSessionCode = advertiseSessionCode
            coroutineScope.launch {
                _serverStatusFlow.emit("Server started - waiting for students...")
                _sessionStateFlow.emit(SessionState(
                    isActive = true,
                    sessionCode = advertiseSessionCode,
                    serverRunning = true
                ))
            }
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            val errorMessage = when (errorCode) {
                ADVERTISE_FAILED_ALREADY_STARTED -> "Advertising already started"
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "Advertisement data too large"
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers"
                ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal error"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "BLE advertising not supported"
                else -> "Unknown advertising error: $errorCode"
            }
            println("GattServer: Advertising failed - $errorMessage")
            coroutineScope.launch {
                _serverStatusFlow.emit("Server failed to start: $errorMessage")
            }
        }
    }

    // Holds the session code currently being advertised (used by advertiseCallback)
    private var advertiseSessionCode: String? = null

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            println("🔍 GATT SERVER DEBUG - CONNECTION STATE CHANGE:")
            println("📱 Device: ${device?.address}")
            println("📊 Status: $status")
            println("📊 New State: $newState")
            println("📊 State Name: ${when(newState) { 
                BluetoothProfile.STATE_CONNECTED -> "CONNECTED" 
                BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED" 
                else -> "UNKNOWN" 
            }}")
            
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    if (device != null) {
                        // ✅ ENHANCED DEBUG: Detailed connection analysis
                        val totalConnections = activeConnections.size + processingConnections.size
                        println("🔍 CONNECTION DEBUG - NEW CONNECTION ATTEMPT:")
                        println("📱 Device: ${device.address}")
                        println("📊 Active BLE Connections: ${activeConnections.size}")
                        println("📊 Processing Connections: ${processingConnections.size}")
                        println("📊 Total Connections: $totalConnections")
                        println("📊 Max Connections: $maxConnections")
                        println("📊 Can Accept Connection: ${totalConnections < maxConnections}")
                        println("📊 Active Connection Addresses: ${activeConnections.map { it.address }}")
                        println("📊 Processing Connection Addresses: ${processingConnections.map { it.address }}")
                        
                        // ✅ ADVANCED: Use SmartQueueManager for connection handling
                        if (smartQueueManager.canAcceptConnection()) {
                            activeConnections.add(device)
                            println("🔍 ADVANCED QUEUE - CONNECTION ACCEPTED:")
                            println("📱 Device: ${device.address}")
                            println("📊 Active Connections: ${activeConnections.size}/$maxConnections")
                            println("📊 Queue Status: ${smartQueueManager.getQueueStatus()}")
                            println("GattServer: Student connected: ${device.address} (${activeConnections.size}/$maxConnections)")
                            coroutineScope.launch {
                                _serverStatusFlow.emit("Student connected: ${device.address} (${activeConnections.size}/$maxConnections)")
                            }
                        } else {
                            // Add to queue using SmartQueueManager
                            val queueStatus = smartQueueManager.getQueueStatus()
                            val queuePosition = queueStatus.queueSize + 1
                            val estimatedWait = queueStatus.estimatedWait
                            
                            println("🔍 ADVANCED QUEUE - CONNECTION QUEUED:")
                            println("📱 Device: ${device.address}")
                            println("📊 Active Connections: ${activeConnections.size}/$maxConnections")
                            println("📊 Queue Position: $queuePosition")
                            println("⏰ Estimated Wait: ${estimatedWait}s")
                            println("📊 Queue Status: $queueStatus")
                            println("GattServer: Connection limit reached, queued: ${device.address} (Queue: $queuePosition)")
                            
                            coroutineScope.launch {
                                _serverStatusFlow.emit("Connection queued: ${device.address} (Position: $queuePosition)")
                            }
                            
                            // ✅ ADDED: Send advanced queue status to student
                            sendAdvancedQueueStatus(device, queuePosition, estimatedWait)
                            
                            // ✅ FIXED: Wait a bit for notification to be sent before disconnecting
                            coroutineScope.launch {
                                delay(1000) // Wait 1 second for notification to be sent
                                gattServer?.cancelConnection(device)
                            }
                        }
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (device != null) {
                        val wasInActive = activeConnections.remove(device)
                        val wasInProcessing = processingConnections.remove(device)
                        val totalConnections = activeConnections.size + processingConnections.size
                        
                        println("🔍 DISCONNECT DEBUG - DEVICE DISCONNECTED:")
                        println("📱 Device: ${device.address}")
                        println("📊 Was in active connections: $wasInActive")
                        println("📊 Was in processing connections: $wasInProcessing")
                        println("📊 Active Connections: ${activeConnections.size}")
                        println("📊 Processing Connections: ${processingConnections.size}")
                        println("📊 Total Connections: $totalConnections")
                        println("📊 Max Connections: $maxConnections")
                        println("📊 Queue Size: ${connectionQueue.size}")
                        println("GattServer: Student disconnected: ${device.address} (Total: $totalConnections/$maxConnections)")
                        
                        coroutineScope.launch {
                            _serverStatusFlow.emit("Student disconnected: ${device.address} (Total: $totalConnections, Queue: ${connectionQueue.size})")
                        }
                    }
                }
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)
            
            if (characteristic?.uuid == STUDENT_CHARACTERISTIC_UUID && value != null) {
                try {
                    val studentData = String(value)
                    println("🔍 BLE SERVER DEBUG - DATA RECEIVED:")
                    println("📥 Raw Data Size: ${value.size} bytes")
                    println("📥 Raw Data String: '$studentData'")
                    println("📥 Raw Data Hex: ${value.joinToString("") { "%02x".format(it) }}")
                    println("GattServer: Received raw data: ${value.size} bytes")
                    println("GattServer: Received data string: '$studentData'")
                    
                    // ✅ ADDED: Validate data string before parsing
                    if (studentData.isBlank()) {
                        println("GattServer: Received empty data string")
                        coroutineScope.launch {
                            _serverStatusFlow.emit("Error: Received empty data from student")
                        }
                        return
                    }
                    
                    // ✅ ADVANCED: Try to parse as advanced message first, fallback to simple format
                    val message = messageParser.decodeMessage(studentData)
                    val studentInfo = if (message != null) {
                        // Advanced message format
                        println("🔍 ADVANCED MESSAGE - PARSING:")
                        println("📥 Message Type: ${message.messageType}")
                        println("📥 Message ID: ${message.messageId}")
                        println("📥 Payload: ${message.payload}")
                        println("📥 Timestamp: ${message.timestamp}")
                        println("📥 Compression: ${message.compressionType}")
                        println("📥 Retry Count: ${message.retryCount}")
                        
                        // Parse student info from payload
                        val payloadParts = message.payload.split("|")
                        if (payloadParts.size >= 2) {
                            StudentInfo(payloadParts[0].trim(), payloadParts[1].trim())
                        } else {
                            null
                        }
                    } else {
                        // Fallback to simple formats. Support legacy "name|rollNumber"
                        // and the new plain format: "ATTENDANCE|name|rollNumber|timestamp"
                        if (!studentData.contains("|")) {
                            println("GattServer: Invalid data format (missing separator)")
                            coroutineScope.launch {
                                _serverStatusFlow.emit("Error: Invalid data format from student")
                            }
                            // Respond with failure to the client if needed
                            try {
                                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                            } catch (e: Exception) {
                                println("GattServer: Error sending failure response: ${e.message}")
                            }
                            return
                        }

                        val parts = studentData.split("|")
                        println("🔍 BLE SERVER DEBUG - DATA PARSING:")
                        println("📥 Split Parts Count: ${parts.size}")
                        println("📥 Parts: $parts")

                        // New format: ATTENDANCE|name|roll|timestamp
                        if (parts.size >= 3 && parts[0].trim().equals("ATTENDANCE", ignoreCase = true)) {
                            val name = parts[1].trim()
                            val rollNumber = parts[2].trim()

                            println("📥 Parsed (ATTENDANCE) Name: '$name' (${name.length} chars)")
                            println("📥 Parsed (ATTENDANCE) Roll Number: '$rollNumber' (${rollNumber.length} chars)")

                            if (name.isBlank() || rollNumber.isBlank()) {
                                println("GattServer: Empty name or roll number in ATTENDANCE message")
                                coroutineScope.launch {
                                    _serverStatusFlow.emit("Error: Empty name or roll number")
                                }
                                try {
                                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                                } catch (e: Exception) {
                                    println("GattServer: Error sending failure response: ${e.message}")
                                }
                                return
                            }

                            // Return the parsed StudentInfo
                            StudentInfo(name, rollNumber)
                        } else if (parts.size == 2) {
                            // Legacy format: name|rollNumber
                            val name = parts[0].trim()
                            val rollNumber = parts[1].trim()
                            println("📥 Parsed Name: '$name' (${name.length} chars)")
                            println("📥 Parsed Roll Number: '$rollNumber' (${rollNumber.length} chars)")

                            if (name.isBlank() || rollNumber.isBlank()) {
                                println("GattServer: Empty name or roll number")
                                coroutineScope.launch {
                                    _serverStatusFlow.emit("Error: Empty name or roll number")
                                }
                                try {
                                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                                } catch (e: Exception) {
                                    println("GattServer: Error sending failure response: ${e.message}")
                                }
                                return
                            }

                            StudentInfo(name, rollNumber)
                        } else {
                            println("GattServer: Invalid data format (wrong number of parts)")
                            coroutineScope.launch {
                                _serverStatusFlow.emit("Error: Invalid data format from student")
                            }
                            try {
                                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                            } catch (e: Exception) {
                                println("GattServer: Error sending failure response: ${e.message}")
                            }
                            return
                        }
                    }
                    
                    if (studentInfo == null) {
                        println("GattServer: Failed to parse student info")
                        coroutineScope.launch {
                            _serverStatusFlow.emit("Error: Failed to parse student info")
                        }
                        return
                    }
                    
                    // ✅ ADDED: Prevent duplicate attendance marking
                    val studentKey = "${studentInfo.name}|${studentInfo.rollNumber}"
                    if (processedStudents.contains(studentKey)) {
                        println("GattServer: Duplicate attendance attempt: ${studentInfo.name} (${studentInfo.rollNumber})")
                        coroutineScope.launch {
                            _serverStatusFlow.emit("Duplicate attendance: ${studentInfo.name} (${studentInfo.rollNumber})")
                        }
                        return
                    }
                    println("🔍 BLE SERVER DEBUG - STUDENT INFO CREATED:")
                    println("📥 Final Name: '${studentInfo.name}' (${studentInfo.name.length} chars)")
                    println("📥 Final Roll Number: '${studentInfo.rollNumber}' (${studentInfo.rollNumber.length} chars)")
                    println("📥 StudentInfo Object: $studentInfo")
                    processedStudents.add(studentKey)
                    // Send immediate ACK to the student to speed up client-side flow
                    if (device != null) {
                        sendAck(device, studentInfo)
                    }
                    println("GattServer: Successfully parsed student data: ${studentInfo.name} (${studentInfo.rollNumber})")
                    
                    // ✅ ADVANCED: Use SmartQueueManager for processing
                    if (device != null) {
                        // Add student to smart queue
                        val queuedStudent = smartQueueManager.addStudent(device, studentInfo)
                        println("🔍 ADVANCED PROCESSING - STUDENT ADDED TO QUEUE:")
                        println("📱 Device: ${device.address}")
                        println("👤 Student: ${studentInfo.name} (${studentInfo.rollNumber})")
                        println("📊 Queue Position: ${queuedStudent.queuePosition}")
                        println("⭐ Priority: ${queuedStudent.priority}")
                        println("📡 Connection Quality: ${(queuedStudent.connectionQuality * 100).toInt()}%")
                        println("⏰ Estimated Wait: ${queuedStudent.estimatedWait}s")
                        
                        // Move to processing state
                        activeConnections.remove(device)
                        processingConnections.add(device)
                    }
                    
                    // ✅ ADVANCED: Process with smart queue management
                    coroutineScope.launch {
                        _incomingStudentFlow.emit(studentInfo)
                        _serverStatusFlow.emit("Attendance marked: ${studentInfo.name} (${studentInfo.rollNumber}) - Total: ${processedStudents.size}")
                        
                        // Process next student from smart queue
                        val nextStudent = smartQueueManager.processNextStudent()
                        if (nextStudent != null) {
                            println("🔍 ADVANCED QUEUE - PROCESSING NEXT:")
                            println("📱 Next Device: ${nextStudent.device.address}")
                            println("👤 Next Student: ${nextStudent.studentInfo.name} (${nextStudent.studentInfo.rollNumber})")
                            println("📊 Queue Status: ${smartQueueManager.getQueueStatus()}")
                            
                            // Send advanced reconnection signal
                            sendAdvancedReconnectionSignal(nextStudent.device, nextStudent.queuePosition, nextStudent.estimatedWait)
                            _serverStatusFlow.emit("Processing next student: ${nextStudent.studentInfo.name}")
                        }
                        
                        // ✅ ADDED: 2-second delay between processing students for better stability
                        delay(2000)
                        
                        // ✅ ADVANCED: Complete processing with smart queue
                        if (device != null) {
                            smartQueueManager.completeStudentProcessing(device, studentInfo)
                            processingConnections.remove(device)
                            println("🔍 ADVANCED PROCESSING - COMPLETED:")
                            println("📱 Device: ${device.address}")
                            println("👤 Student: ${studentInfo.name} (${studentInfo.rollNumber})")
                            println("📊 Queue Status: ${smartQueueManager.getQueueStatus()}")
                        }
                    }
                } catch (e: Exception) {
                    println("GattServer: Error parsing student data: ${e.message}")
                    println("GattServer: Raw data as string: '${value?.let { String(it) }}'")
                    println("GattServer: Raw data as hex: ${value?.joinToString("") { "%02x".format(it) }}")
                    
                    coroutineScope.launch {
                        _serverStatusFlow.emit("Error processing student data: ${e.message}")
                    }
                }
            }
            
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            }
        }
    }

    fun startServer(sessionCode: String): Boolean {
        // Check if server is already running with the same session code
        if (isServerRunning && currentSessionCode == sessionCode) {
            println("GattServer: Server is already running with session code: $sessionCode")
            coroutineScope.launch {
                _sessionStateFlow.emit(SessionState(
                    isActive = true,
                    sessionCode = sessionCode,
                    serverRunning = true
                ))
            }
            return true
        }

        // If server is running with a different session code, stop it first
        if (isServerRunning) {
            stopServer()
        }

        // Check permissions and Bluetooth availability
        if (!hasRequiredPermissions(context)) {
            println("GattServer: Missing required permissions")
            coroutineScope.launch {
                _serverStatusFlow.emit("Error: Missing Bluetooth permissions")
                _sessionStateFlow.emit(SessionState(
                    isActive = false,
                    sessionCode = null,
                    serverRunning = false,
                    errorMessage = "Missing Bluetooth permissions"
                ))
            }
            return false
        }

        if (!isBluetoothAvailable(context)) {
            println("GattServer: Bluetooth not available")
            coroutineScope.launch {
                _serverStatusFlow.emit("Error: Bluetooth not available")
            }
            return false
        }

        // Check if BLE advertising is supported
        if (advertiser == null) {
            println("GattServer: BLE advertising not supported on this device")
            coroutineScope.launch {
                _serverStatusFlow.emit("Error: BLE advertising not supported on this device")
            }
            return false
        }

        try {
            println("🔵 GattServer: Opening GATT server...")
            gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
            if (gattServer == null) {
                println("❌ GattServer: Failed to open GATT server")
                coroutineScope.launch {
                    _serverStatusFlow.emit("Error: Failed to open GATT server")
                }
                return false
            }
            println("✅ GattServer: GATT server opened successfully")

            // Create service
            val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            
            // Student data characteristic (write-only)
            println("🔵 GattServer: Adding student characteristic...")
            val studentCharacteristic = BluetoothGattCharacteristic(
                STUDENT_CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )
            val added = service.addCharacteristic(studentCharacteristic)
            if (!added) {
                println("❌ GattServer: Failed to add student characteristic")
                coroutineScope.launch {
                    _serverStatusFlow.emit("Error: Failed to add characteristic")
                }
                return false
            }
            println("✅ GattServer: Student characteristic added successfully")
            
            // Response characteristic (notify-only) for sending queue status back to students
            val responseCharacteristic = BluetoothGattCharacteristic(
                RESPONSE_CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            service.addCharacteristic(responseCharacteristic)
            
            gattServer?.addService(service)
            
            println("GattServer: Starting advertising with session code: $sessionCode")
            startAdvertising(sessionCode)
            return true
        } catch (e: Exception) {
            println("GattServer: Failed to start server: ${e.message}")
            coroutineScope.launch {
                _serverStatusFlow.emit("Failed to start server: ${e.message}")
            }
            return false
        }
    }

    fun stopServer() {
        try {
            stopAdvertising()
            if (gattServer != null) {
                gattServer?.close()
                gattServer = null
                isServerRunning = false
                currentSessionCode = null
                println("GattServer: Server stopped and cleaned up")
                coroutineScope.launch {
                    _serverStatusFlow.emit("Server stopped")
                    _sessionStateFlow.emit(SessionState(
                        isActive = false,
                        sessionCode = null,
                        serverRunning = false
                    ))
                }
            }
        } catch (e: Exception) {
            println("GattServer: Error stopping server: ${e.message}")
            coroutineScope.launch {
                _sessionStateFlow.emit(SessionState(
                    isActive = false,
                    sessionCode = null,
                    serverRunning = false,
                    errorMessage = "Error stopping server: ${e.message}"
                ))
            }
        }
    }

    private fun startAdvertising(sessionCode: String) {
        println("🔵 GattServer: Starting BLE advertising...")
        if (advertiser == null) {
            println("❌ GattServer: BLE advertiser not available")
            coroutineScope.launch {
                _serverStatusFlow.emit("Error: BLE advertiser not available")
            }
            return
        }

        // Check session code length to prevent advertising data issues
        val maxSessionCodeLength = 20 // Conservative limit for BLE advertising
        val safeSessionCode = if (sessionCode.length > maxSessionCodeLength) {
            sessionCode.substring(0, maxSessionCodeLength)
        } else {
            sessionCode
        }
        
    println("📡 GattServer: Configuring advertisement settings...")
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .setTimeout(0) // No timeout
            .setTimeout(0) // Advertise indefinitely
            .build()
            
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            // ✅ FIXED: Removed redundant service UUID to prevent "data too large" error
            // The service data already identifies our service, so we don't need the UUID separately
            .addServiceData(ParcelUuid(SERVICE_UUID), safeSessionCode.toByteArray())
            .build()

        println("📡 GattServer: Starting advertising...")
        try {
            // Store the session code for the advertise callback to use
            advertiseSessionCode = safeSessionCode
            // Use the shared advertiseCallback so stopAdvertising() can stop it reliably
            advertiser?.startAdvertising(settings, data, advertiseCallback)
        } catch (e: Exception) {
            println("❌ GattServer: Exception while starting advertising: ${e.message}")
            coroutineScope.launch {
                _serverStatusFlow.emit("Error: Failed to start advertising - ${e.message}")
            }
        }
    }

    private fun stopAdvertising() {
        try {
            advertiser?.stopAdvertising(advertiseCallback)
            advertiseSessionCode = null
            println("GattServer: Advertising stopped via stopAdvertising()")
            coroutineScope.launch {
                _serverStatusFlow.emit("Advertising stopped")
            }
        } catch (e: Exception) {
            println("GattServer: Error stopping advertising: ${e.message}")
        }
    }
    
    // ✅ ADDED: Send reconnection signal to queued device when it's their turn
    private fun sendReconnectionSignal(device: BluetoothDevice) {
        coroutineScope.launch {
            try {
                println("🔍 QUEUE DEBUG - SENDING RECONNECTION SIGNAL:")
                println("📱 Target Device: ${device.address}")
                println("📊 Queue Position: 0 (now processing)")
                println("⏰ Estimated Wait: 0s")
                
                // Send immediate reconnection signal
                val reconnectionSignal = "RECONNECT|0|0"
                println("📤 Reconnection Signal: '$reconnectionSignal'")
                
                // Note: We can't directly send to a disconnected device
                // The student app should handle reconnection based on queue status
                println("GattServer: Reconnection signal prepared for ${device.address}")
                
                // Update server status
                _serverStatusFlow.emit("Ready for next student: ${device.address}")
                
            } catch (e: Exception) {
                println("GattServer: Error sending reconnection signal: ${e.message}")
            }
        }
    }
    
    // ✅ ADDED: Send advanced queue status to student before disconnecting
    private fun sendAdvancedQueueStatus(device: BluetoothDevice, queuePosition: Int, estimatedWait: Int) {
        coroutineScope.launch {
            try {
                // Wait a bit for student to subscribe to notifications
                delay(500)
                
                val responseCharacteristic = gattServer?.getService(SERVICE_UUID)
                    ?.getCharacteristic(RESPONSE_CHARACTERISTIC_UUID)
                
                if (responseCharacteristic != null) {
                    // Create advanced message for queue status
                    val queueStatus = smartQueueManager.getQueueStatus()
                    val payload = "QUEUE|$queuePosition|$estimatedWait|${queueStatus.processingRate}|${queueStatus.successRate}"
                    val message = BLEMessage(
                        messageId = messageSequencer.generateMessageId(),
                        messageType = MessageType.QUEUE_STATUS,
                        payload = payload,
                        timestamp = System.currentTimeMillis()
                    )
                    val serializedMessage = messageParser.encodeMessage(message)
                    
                    println("🔍 ADVANCED QUEUE - SENDING STATUS:")
                    println("📱 Target Device: ${device.address}")
                    println("📊 Queue Position: $queuePosition")
                    println("⏰ Estimated Wait: ${estimatedWait}s")
                    println("📊 Processing Rate: ${queueStatus.processingRate}")
                    println("📊 Success Rate: ${queueStatus.successRate}")
                    println("📤 Advanced Message: '$serializedMessage'")
                    
                    responseCharacteristic.value = serializedMessage.toByteArray()
                    
                    // Enable notifications for this device
                    val descriptor = responseCharacteristic.getDescriptor(
                        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                    )
                    descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gattServer?.notifyCharacteristicChanged(device, responseCharacteristic, false)
                    
                    println("GattServer: Sent advanced queue status to ${device.address}: Position $queuePosition, Wait ${estimatedWait}s")
                } else {
                    println("GattServer: Response characteristic not found")
                }
            } catch (e: Exception) {
                println("GattServer: Error sending advanced queue status: ${e.message}")
            }
        }
    }
    
    // ✅ ADDED: Send advanced reconnection signal to queued device
    private fun sendAdvancedReconnectionSignal(device: BluetoothDevice, queuePosition: Int, estimatedWait: Int) {
        coroutineScope.launch {
            try {
                println("🔍 ADVANCED QUEUE - SENDING RECONNECTION SIGNAL:")
                println("📱 Target Device: ${device.address}")
                println("📊 Queue Position: $queuePosition")
                println("⏰ Estimated Wait: ${estimatedWait}s")
                
                // Create advanced reconnection message
                val payload = "RECONNECT|$queuePosition|$estimatedWait"
                val message = BLEMessage(
                    messageId = messageSequencer.generateMessageId(),
                    messageType = MessageType.RETRY,
                    payload = payload,
                    timestamp = System.currentTimeMillis()
                )
                val serializedMessage = messageParser.encodeMessage(message)
                
                println("📤 Advanced Reconnection Signal: '$serializedMessage'")
                println("GattServer: Advanced reconnection signal prepared for ${device.address}")
                
                // Update server status
                _serverStatusFlow.emit("Ready for next student: ${device.address}")
                
            } catch (e: Exception) {
                println("GattServer: Error sending advanced reconnection signal: ${e.message}")
            }
        }
    }
    
    // ✅ ADDED: Send queue status to student before disconnecting (legacy method for compatibility)
    private fun sendQueueStatusToStudent(device: BluetoothDevice, queuePosition: Int, estimatedWait: Int) {
        sendAdvancedQueueStatus(device, queuePosition, estimatedWait)
    }

    // ✅ ADDED: Send acknowledgement to student after successful parse/mark
    private fun sendAck(device: BluetoothDevice, studentInfo: StudentInfo) {
        coroutineScope.launch {
            try {
                // small delay to allow client to subscribe
                delay(200)

                val responseCharacteristic = gattServer?.getService(SERVICE_UUID)
                    ?.getCharacteristic(RESPONSE_CHARACTERISTIC_UUID)

                if (responseCharacteristic != null) {
                    val payload = "ACK|${studentInfo.name}|${studentInfo.rollNumber}"
                    val message = BLEMessage(
                        messageId = messageSequencer.generateMessageId(),
                        messageType = MessageType.ACK,
                        payload = payload,
                        timestamp = System.currentTimeMillis()
                    )
                    val serializedMessage = messageParser.encodeMessage(message)

                    println("🔍 SENDING ACK TO STUDENT:")
                    println("📱 Target Device: ${device.address}")
                    println("📤 ACK Payload: '$payload'")
                    println("📤 Serialized ACK: '$serializedMessage'")

                    responseCharacteristic.value = serializedMessage.toByteArray()

                    val descriptor = responseCharacteristic.getDescriptor(
                        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                    )
                    descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gattServer?.notifyCharacteristicChanged(device, responseCharacteristic, false)

                    println("GattServer: ACK sent to ${device.address} for ${studentInfo.name}")
                } else {
                    println("GattServer: Response characteristic not found (ACK)")
                }
            } catch (e: Exception) {
                println("GattServer: Error sending ACK: ${e.message}")
            }
        }
    }
    
    // ✅ ADVANCED: Queue status methods for teacher UI
    fun getConnectionStatus(): String {
        val queueStatus = smartQueueManager.getQueueStatus()
        return "Active: ${queueStatus.activeConnections}, Processing: ${queueStatus.processingConnections}, Total: ${queueStatus.totalConnections}/${queueStatus.maxConnections}, Queue: ${queueStatus.queueSize}, Processed: ${processedStudents.size}, Success Rate: ${(queueStatus.successRate * 100).toInt()}%"
    }
    
    fun getQueueSize(): Int = smartQueueManager.getQueueStatus().queueSize
    
    fun getActiveConnections(): Int = smartQueueManager.getQueueStatus().activeConnections
    
    fun getProcessingConnections(): Int = smartQueueManager.getQueueStatus().processingConnections
    
    fun getTotalConnections(): Int = smartQueueManager.getQueueStatus().totalConnections
    
    fun getProcessedCount(): Int = processedStudents.size
    
    fun getQueueStatus(): QueueStatus = smartQueueManager.getQueueStatus()
    
    fun getQueueStatusFlow(): SharedFlow<QueueStatus> = smartQueueManager.queueStatusFlow
    
    fun clearProcessedStudents() {
        processedStudents.clear()
        smartQueueManager.clear()
        println("GattServer: Cleared processed students list and smart queue")
    }
}