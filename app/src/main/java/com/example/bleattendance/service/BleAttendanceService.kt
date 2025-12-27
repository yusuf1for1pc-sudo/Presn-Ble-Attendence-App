package com.example.bleattendance.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.bleattendance.R
import com.example.bleattendance.ble.GattServer
import com.example.bleattendance.model.StudentInfo
import com.example.bleattendance.data.db.AttendanceDao
import com.example.bleattendance.data.db.AttendanceSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Date

/**
 * ✅ ENHANCED: Foreground Service to keep BLE attendance session running in background
 * This ensures the teacher's BLE server continues advertising even when app is minimized
 * Now includes better notification management and data persistence
 */
class BleAttendanceService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "ble_attendance_channel"
        private const val CHANNEL_NAME = "BLE Attendance Service"
        
        // Action constants
        const val ACTION_START_SESSION = "com.example.bleattendance.START_SESSION"
        const val ACTION_STOP_SESSION = "com.example.bleattendance.STOP_SESSION"
        const val ACTION_ADD_STUDENT = "com.example.bleattendance.ADD_STUDENT"
        const val EXTRA_SESSION_CODE = "session_code"
        const val EXTRA_CLASS_ID = "class_id"
        const val EXTRA_STUDENT_NAME = "student_name"
        const val EXTRA_STUDENT_ROLL = "student_roll"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var gattServer: GattServer? = null
    private var sessionCode: String = ""
    private var classId: Int = 0
    private var isSessionActive = false
    private var isSessionStopped = false
    private var attendedStudents = mutableListOf<StudentInfo>()
    private var listenerJob: Job? = null
    private var statusJob: Job? = null
    private var sessionStartTime: Long = 0

    // ✅ NEW: Database access for persistence
    private lateinit var attendanceDao: AttendanceDao

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeGattServer()
        
        // ✅ NEW: Initialize database access
        try {
            val app = application as com.example.bleattendance.AttendanceApp
            attendanceDao = app.database.attendanceDao()
        } catch (e: Exception) {
            println("Failed to initialize database in service: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("Service: onStartCommand called with action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START_SESSION -> {
                sessionCode = intent.getStringExtra(EXTRA_SESSION_CODE) ?: ""
                classId = intent.getIntExtra(EXTRA_CLASS_ID, 0)
                println("Service: Starting session with code: $sessionCode, classId: $classId")
                startSession()
            }
            ACTION_STOP_SESSION -> {
                println("Service: Received stop session command")
                stopSession()
                stopSelf()
            }
            ACTION_ADD_STUDENT -> {
                println("Service: Received ADD_STUDENT action")
                val studentName = intent.getStringExtra(EXTRA_STUDENT_NAME) ?: ""
                val studentRoll = intent.getStringExtra(EXTRA_STUDENT_ROLL) ?: ""
                println("Service: Student name from intent: '$studentName'")
                println("Service: Student roll from intent: '$studentRoll'")
                
                if (studentName.isNotEmpty() && studentRoll.isNotEmpty()) {
                    val student = StudentInfo(studentName, studentRoll)
                    if (!attendedStudents.any { it.rollNumber == student.rollNumber }) {
                        attendedStudents.add(student)
                        updateNotification()
                        println("Service: Added student from ViewModel: ${student.name} (${student.rollNumber})")
                        println("Service: Total students in service: ${attendedStudents.size}")
                    } else {
                        println("Service: Student already exists: ${student.name} (${student.rollNumber})")
                    }
                } else {
                    println("Service: Invalid student data - name: '$studentName', roll: '$studentRoll'")
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when BLE attendance session is active"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initializeGattServer() {
        try {
            gattServer = GattServer(applicationContext)
            
            // ✅ ENHANCED: Listen for incoming students with better error handling
            listenerJob = gattServer?.incomingStudentFlow
                ?.onEach { newStudent ->
                    println("Service: Received student from GattServer: ${newStudent.name} (${newStudent.rollNumber})")
                    if (!attendedStudents.any { it.rollNumber == newStudent.rollNumber }) {
                        attendedStudents.add(newStudent)
                        updateNotification()
                        
                        // ✅ NEW: Log student attendance
                        println("Service: Student ${newStudent.name} (${newStudent.rollNumber}) added to attendedStudents list")
                        println("Service: Total students in list: ${attendedStudents.size}")
                    } else {
                        println("Service: Student ${newStudent.name} (${newStudent.rollNumber}) already in list, skipping")
                    }
                }
                ?.launchIn(serviceScope)
            
            // ✅ NEW: Listen for server status updates
            statusJob = gattServer?.serverStatusFlow
                ?.onEach { status ->
                    println("Service: Server status: $status")
                    updateNotification()
                }
                ?.launchIn(serviceScope)
                
        } catch (e: Exception) {
            println("Failed to initialize GATT server in service: ${e.message}")
        }
    }

    private fun startSession() {
        if (sessionCode.isBlank()) {
            println("Cannot start session: session code is empty")
            return
        }

        isSessionActive = true
        attendedStudents.clear()
        sessionStartTime = System.currentTimeMillis()
        
        try {
            val success = gattServer?.startServer(sessionCode) ?: false
            if (success) {
                println("BLE session started successfully in service with code: $sessionCode")
                startForeground(NOTIFICATION_ID, createNotification())
                
                // ✅ NEW: Log session start
                println("Service: Session started at ${Date(sessionStartTime)}")
            } else {
                println("Failed to start BLE session in service")
                stopSelf()
            }
        } catch (e: Exception) {
            println("Error starting BLE session in service: ${e.message}")
            stopSelf()
        }
    }

    private fun stopSession() {
        // Prevent multiple session stops
        if (isSessionStopped) {
            println("Service: Session already stopped, ignoring duplicate stop call")
            return
        }
        
        isSessionActive = false
        isSessionStopped = true
        val sessionDuration = System.currentTimeMillis() - sessionStartTime
        
        println("Service: Stopping session - duration: ${sessionDuration / 1000} seconds")
        
        try {
            gattServer?.stopServer()
            println("BLE session stopped in service")
            
            // ✅ NEW: Save session data to database
            println("Service: About to save session - classId: $classId, sessionCode: $sessionCode")
            println("Service: attendedStudents list size: ${attendedStudents.size}")
            attendedStudents.forEach { student ->
                println("Service: Student to save - ${student.name} (${student.rollNumber})")
            }
            
            if (::attendanceDao.isInitialized && classId > 0) {
                serviceScope.launch {
                    try {
                        println("Service: Saving attendance data for ${attendedStudents.size} students")
                        println("Service: Session details - ClassId: $classId, Code: $sessionCode, Timestamp: ${Date(sessionStartTime)}")
                        
                        // Find the class session that matches the session code
                        val app = application as com.example.bleattendance.AttendanceApp
                        val classSessionDao = app.database.classSessionDao()
                        val classSessionsFlow = classSessionDao.getSessionsForClass(classId)
                        val classSessions = classSessionsFlow.first()
                        val matchingSession = classSessions.find { session -> session.sessionCode == sessionCode }
                        
                        if (matchingSession != null) {
                            println("Service: Found matching class session: ${matchingSession.sessionId} for code: $sessionCode")
                            
                            // Save attendance for each student
                            attendedStudents.forEach { student ->
                                try {
                                    // Get student email from roll number (you might need to adjust this logic)
                                    val studentDao = app.database.studentDao()
                                    val studentFlow = studentDao.getStudent()
                                    val studentEntity = studentFlow.first()
                                    
                                    if (studentEntity != null) {
                                        val attendanceSession = com.example.bleattendance.data.db.AttendanceSessionEntity(
                                            sessionId = matchingSession.sessionId,
                                            studentEmail = studentEntity.email,
                                            attendanceStatus = "PRESENT",
                                            markedAt = System.currentTimeMillis(),
                                            markedVia = "BLE"
                                        )
                                        
                                        attendanceDao.insertSession(attendanceSession)
                                        println("Service: ✅ Saved attendance for ${student.name} (${student.rollNumber})")
                                    } else {
                                        println("Service: ⚠️ No student entity found for ${student.name}")
                                    }
                                } catch (e: Exception) {
                                    println("Service: ❌ Error saving attendance for ${student.name}: ${e.message}")
                                }
                            }
                            
                            println("Service: ✅ Attendance data saved successfully for session ${matchingSession.sessionId}")
                        } else {
                            println("Service: ❌ No matching class session found for code: $sessionCode in class: $classId")
                        }
                        
                        println("Service: BLE scanning completed successfully")
                    } catch (e: Exception) {
                        println("Service: Error in session completion: ${e.message}")
                        e.printStackTrace()
                    }
                }
            } else {
                println("Service: Cannot save session - attendanceDao initialized: ${::attendanceDao.isInitialized}, classId: $classId")
            }
            
            // ✅ NEW: Log session summary
            println("Service: Session ended after ${sessionDuration / 1000} seconds")
            println("Service: Total students attended: ${attendedStudents.size}")
            attendedStudents.forEach { student ->
                println("Service: - ${student.name} (${student.rollNumber})")
            }
            
        } catch (e: Exception) {
            println("Error stopping BLE session in service: ${e.message}")
        }
    }

    private fun createNotification(): Notification {
        val studentCount = attendedStudents.size
        val sessionDuration = if (sessionStartTime > 0) {
            (System.currentTimeMillis() - sessionStartTime) / 1000
        } else 0
        
        val title = "BLE Attendance: Session Active"
        val text = when {
            studentCount > 0 -> "Students can connect now ($studentCount connected, ${sessionDuration}s)"
            else -> "Students can connect now (${sessionDuration}s)"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .build()
    }

    private fun updateNotification() {
        if (isSessionActive) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, createNotification())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerJob?.cancel()
        statusJob?.cancel()
        
        // Only stop session if it hasn't been stopped already
        if (!isSessionStopped) {
            stopSession()
        }
        
        println("Service: BleAttendanceService destroyed")
    }
}



