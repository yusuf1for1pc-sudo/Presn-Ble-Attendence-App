package com.example.bleattendance.ble

import android.bluetooth.BluetoothDevice
import com.example.bleattendance.model.StudentInfo
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.mutableListOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * ✅ SMART QUEUE MANAGER
 * Advanced queue management with priority, quality monitoring, and real-time updates
 */
data class QueuedStudent(
    val device: BluetoothDevice,
    val studentInfo: StudentInfo,
    val queuePosition: Int,
    val timestamp: Long,
    val connectionQuality: Float,
    val priority: Int = 0,
    val retryCount: Int = 0,
    val estimatedWait: Int = 0
)

data class QueueStatus(
    val queueSize: Int,
    val activeConnections: Int,
    val processingConnections: Int,
    val totalConnections: Int,
    val maxConnections: Int,
    val estimatedWait: Int,
    val processingRate: Float,
    val successRate: Float
)

class SmartQueueManager(
    private val maxConnections: Int = 5
) {
    private val priorityQueue = PriorityQueue<QueuedStudent>(compareBy { it.priority })
    private val activeConnections = mutableSetOf<BluetoothDevice>()
    private val processingConnections = mutableSetOf<BluetoothDevice>()
    private val processedStudents = mutableSetOf<String>()
    private val qualityMonitor = ConnectionQualityMonitor()
    private val messageBatcher = MessageBatcher()
    
    private val _queueStatusFlow = MutableSharedFlow<QueueStatus>()
    val queueStatusFlow = _queueStatusFlow.asSharedFlow()
    
    private val _queueUpdateFlow = MutableSharedFlow<QueuedStudent>()
    val queueUpdateFlow = _queueUpdateFlow.asSharedFlow()
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * ✅ ADD STUDENT TO QUEUE
     * Calculates priority based on connection quality and other factors
     */
    fun addStudent(device: BluetoothDevice, studentInfo: StudentInfo): QueuedStudent {
        val connectionQuality = qualityMonitor.getConnectionQuality(device.address)
        val priority = calculatePriority(connectionQuality, studentInfo)
        val queuePosition = priorityQueue.size + 1
        val estimatedWait = calculateEstimatedWait(queuePosition)
        
        val queuedStudent = QueuedStudent(
            device = device,
            studentInfo = studentInfo,
            queuePosition = queuePosition,
            timestamp = System.currentTimeMillis(),
            connectionQuality = connectionQuality,
            priority = priority,
            estimatedWait = estimatedWait
        )
        
        priorityQueue.add(queuedStudent)
        
        println("🔍 SMART QUEUE - STUDENT ADDED:")
        println("📱 Device: ${device.address}")
        println("👤 Student: ${studentInfo.name} (${studentInfo.rollNumber})")
        println("📊 Queue Position: $queuePosition")
        println("⭐ Priority: $priority")
        println("📡 Connection Quality: ${(connectionQuality * 100).toInt()}%")
        println("⏰ Estimated Wait: ${estimatedWait}s")
        
        // Notify queue update
        coroutineScope.launch {
            _queueUpdateFlow.emit(queuedStudent)
            updateQueueStatus()
        }
        
        return queuedStudent
    }
    
    /**
     * ✅ PROCESS NEXT STUDENT
     * Processes the highest priority student in queue
     */
    fun processNextStudent(): QueuedStudent? {
        val nextStudent = priorityQueue.poll()
        if (nextStudent != null) {
            // Move to processing state
            activeConnections.remove(nextStudent.device)
            processingConnections.add(nextStudent.device)
            
            println("🔍 SMART QUEUE - PROCESSING STUDENT:")
            println("📱 Device: ${nextStudent.device.address}")
            println("👤 Student: ${nextStudent.studentInfo.name} (${nextStudent.studentInfo.rollNumber})")
            println("📊 Remaining Queue: ${priorityQueue.size}")
            println("📊 Processing Connections: ${processingConnections.size}")
            
            // Update queue positions for remaining students
            updateQueuePositions()
            
            // Notify queue update
            coroutineScope.launch {
                _queueUpdateFlow.emit(nextStudent)
                updateQueueStatus()
            }
        }
        
        return nextStudent
    }
    
    /**
     * ✅ COMPLETE STUDENT PROCESSING
     * Marks student as processed and removes from processing state
     */
    fun completeStudentProcessing(device: BluetoothDevice, studentInfo: StudentInfo) {
        processingConnections.remove(device)
        processedStudents.add("${studentInfo.name}|${studentInfo.rollNumber}")
        
        println("🔍 SMART QUEUE - PROCESSING COMPLETED:")
        println("📱 Device: ${device.address}")
        println("👤 Student: ${studentInfo.name} (${studentInfo.rollNumber})")
        println("📊 Processed Students: ${processedStudents.size}")
        println("📊 Processing Connections: ${processingConnections.size}")
        
        coroutineScope.launch {
            updateQueueStatus()
        }
    }
    
    /**
     * ✅ CAN ACCEPT CONNECTION
     * Checks if new connection can be accepted
     */
    fun canAcceptConnection(): Boolean {
        val totalConnections = activeConnections.size + processingConnections.size
        return totalConnections < maxConnections
    }
    
    /**
     * ✅ GET QUEUE STATUS
     * Returns current queue status
     */
    fun getQueueStatus(): QueueStatus {
        val totalConnections = activeConnections.size + processingConnections.size
        val estimatedWait = if (priorityQueue.isNotEmpty()) {
            calculateEstimatedWait(priorityQueue.size)
        } else {
            0
        }
        
        val processingRate = calculateProcessingRate()
        val successRate = calculateSuccessRate()
        
        return QueueStatus(
            queueSize = priorityQueue.size,
            activeConnections = activeConnections.size,
            processingConnections = processingConnections.size,
            totalConnections = totalConnections,
            maxConnections = maxConnections,
            estimatedWait = estimatedWait,
            processingRate = processingRate,
            successRate = successRate
        )
    }
    
    /**
     * ✅ UPDATE CONNECTION QUALITY
     * Updates connection quality for a device
     */
    fun updateConnectionQuality(device: BluetoothDevice, rssi: Int, latency: Long) {
        qualityMonitor.updateMetrics(device.address, rssi, latency)
        
        // Update priority for queued students
        priorityQueue.forEach { student ->
            if (student.device.address == device.address) {
                val newQuality = qualityMonitor.getConnectionQuality(device.address)
                val newPriority = calculatePriority(newQuality, student.studentInfo)
                
                // Remove and re-add with new priority
                priorityQueue.remove(student)
                val updatedStudent = student.copy(
                    connectionQuality = newQuality,
                    priority = newPriority
                )
                priorityQueue.add(updatedStudent)
            }
        }
        
        updateQueuePositions()
    }
    
    /**
     * ✅ REMOVE STUDENT FROM QUEUE
     * Removes student from queue (e.g., on disconnect)
     */
    fun removeStudent(device: BluetoothDevice) {
        val student = priorityQueue.find { it.device.address == device.address }
        if (student != null) {
            priorityQueue.remove(student)
            activeConnections.remove(device)
            processingConnections.remove(device)
            
            println("🔍 SMART QUEUE - STUDENT REMOVED:")
            println("📱 Device: ${device.address}")
            println("👤 Student: ${student.studentInfo.name} (${student.studentInfo.rollNumber})")
            println("📊 Remaining Queue: ${priorityQueue.size}")
            
            updateQueuePositions()
            
            coroutineScope.launch {
                updateQueueStatus()
            }
        }
    }
    
    /**
     * ✅ GET BATCH ACKNOWLEDGMENTS
     * Returns pending acknowledgments for batching
     */
    fun getBatchAcks(): List<String> {
        return messageBatcher.getPendingAcks()
    }
    
    /**
     * ✅ ADD ACKNOWLEDGMENT
     * Adds acknowledgment to batch
     */
    fun addAcknowledgment(messageId: String) {
        messageBatcher.addAck(messageId)
    }
    
    /**
     * ✅ FLUSH BATCH ACKNOWLEDGMENTS
     * Flushes pending acknowledgments
     */
    fun flushBatchAcks(): List<String> {
        return messageBatcher.flushBatch()
    }
    
    /**
     * ✅ PRIVATE HELPER METHODS
     */
    private fun calculatePriority(connectionQuality: Float, studentInfo: StudentInfo): Int {
        var priority = 0
        
        // Connection quality (0-3 points)
        priority += when {
            connectionQuality > 0.8f -> 3
            connectionQuality > 0.6f -> 2
            connectionQuality > 0.4f -> 1
            else -> 0
        }
        
        // Student type (0-2 points)
        priority += when {
            studentInfo.rollNumber.startsWith("IT") -> 2  // IT students
            studentInfo.rollNumber.startsWith("CS") -> 1  // CS students
            else -> 0
        }
        
        // Time in queue (0-1 points)
        priority += 1  // Always give some priority
        
        return priority
    }
    
    private fun calculateEstimatedWait(queuePosition: Int): Int {
        val processingTime = 2 // 2 seconds per student
        val queueTime = queuePosition * processingTime
        val connectionTime = 1 // 1 second for connection
        return queueTime + connectionTime
    }
    
    private fun updateQueuePositions() {
        val students = priorityQueue.toList()
        priorityQueue.clear()
        
        students.forEachIndexed { index, student ->
            val updatedStudent = student.copy(
                queuePosition = index + 1,
                estimatedWait = calculateEstimatedWait(index + 1)
            )
            priorityQueue.add(updatedStudent)
        }
    }
    
    private fun calculateProcessingRate(): Float {
        // Simplified processing rate calculation
        // In real implementation, track processing times
        return 0.5f // 0.5 students per second
    }
    
    private fun calculateSuccessRate(): Float {
        val totalAttempts = processedStudents.size + priorityQueue.size
        return if (totalAttempts > 0) {
            processedStudents.size.toFloat() / totalAttempts
        } else {
            1.0f
        }
    }
    
    private suspend fun updateQueueStatus() {
        val status = getQueueStatus()
        _queueStatusFlow.emit(status)
    }
    
    /**
     * ✅ CLEANUP
     */
    fun clear() {
        priorityQueue.clear()
        activeConnections.clear()
        processingConnections.clear()
        processedStudents.clear()
        qualityMonitor.clear()
        messageBatcher.clear()
        coroutineScope.cancel()
    }
}


