// In ui/viewmodels/ClassDetailsViewModel.kt

package com.example.bleattendance.ui.viewmodels

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.bleattendance.AttendanceApp
import com.example.bleattendance.data.db.AttendanceSessionEntity
import com.example.bleattendance.data.db.ClassSessionEntity
import com.example.bleattendance.data.db.StudentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.FileWriter
import java.io.IOException
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

data class ClassAttendanceStats(
    val totalLectures: Int = 0,
    val attendedLectures: Int = 0,
    val missedLectures: Int = 0,
    val attendancePercentage: Float = 0f
)

data class ClassRecentSession(
    val sessionId: String,
    val date: String,
    val time: String,
    val status: String,
    val sessionCode: String,
    val attendedStudents: List<StudentAttendanceInfo> = emptyList()
)

data class StudentAttendanceInfo(
    val name: String,
    val rollNumber: String,
    val email: String,
    val attendanceStatus: String
)

class ClassDetailsViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val attendanceDao = (application as AttendanceApp).database.attendanceDao()
    private val classSessionDao = (application as AttendanceApp).database.classSessionDao()
    private val studentDao = (application as AttendanceApp).database.studentDao()
    private val teacherDao = (application as AttendanceApp).database.teacherDao()
    private val supabaseRepository = (application as AttendanceApp).supabaseRepository
    private val classId: StateFlow<Int> = savedStateHandle.getStateFlow("classId", 0)

    private val _sessionsForClass = MutableStateFlow<List<ClassSessionEntity>>(emptyList())
    val sessionsForClass: StateFlow<List<ClassSessionEntity>> = _sessionsForClass.asStateFlow()
    
    // Class and teacher data
    var classData by mutableStateOf<com.example.bleattendance.data.db.ClassEntity?>(null)
        private set
    
    var teacherData by mutableStateOf<com.example.bleattendance.data.db.TeacherEntity?>(null)
        private set
    
    // Attendance statistics for this specific class
    var classAttendanceStats by mutableStateOf(ClassAttendanceStats())
        private set
    
    var classRecentSessions by mutableStateOf<List<ClassRecentSession>>(emptyList())
        private set
    
    var exportStatus by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch(Dispatchers.IO) {
            classId.collect { id ->
                println("📚 ClassDetailsViewModel: Received classId: $id")
                if (id > 0) {
                    try {
                        println("📚 ClassDetailsViewModel: Loading attendance data for classId: $id")
                        
                        // Load class sessions
                        val sessions = classSessionDao.getSessionsForClass(id).first()
                        _sessionsForClass.value = sessions
                        println("📚 ClassDetailsViewModel: Loaded ${sessions.size} sessions for class $id")
                        
                        // Debug: Print all sessions to see what we have
                        sessions.forEach { session ->
                            println("📚 Session: ID=${session.sessionId}, ClassID=${session.classId}, Code=${session.sessionCode}, Date=${session.scheduledDate}")
                        }
                        
                        // Load attendance data for this class
                        loadClassAttendanceData(id)
                    } catch (e: Exception) {
                        println("📚 ClassDetailsViewModel: Database error: ${e.message}")
                        e.printStackTrace()
                        _sessionsForClass.value = emptyList()
                    }
                } else {
                    println("📚 ClassDetailsViewModel: Invalid classId: $id")
                    _sessionsForClass.value = emptyList()
                }
            }
        }
    }

    fun exportSessionToCsv(context: Context, session: ClassSessionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val csvContent = generateCsvContent(session)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
                val dateStr = dateFormat.format(Date())
                val fileName = "Attendance_${session.sessionCode}_$dateStr.csv"

                val cachePath = File(context.cacheDir, "csv")
                cachePath.mkdirs()
                val file = File(cachePath, fileName)
                file.writeText(csvContent)

                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Attendance Report - ${session.sessionCode}")
                    putExtra(Intent.EXTRA_TEXT, "Attendance report for session ${session.sessionCode}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(Intent.createChooser(shareIntent, "Share Attendance Report"))
            } catch (e: Exception) {
                println("Export error: ${e.message}")
            }
        }
    }

    private suspend fun generateCsvContent(session: ClassSessionEntity): String {
        val builder = StringBuilder()
        
        // Get attendance records for this session
        val attendanceRecords = attendanceDao.getSessionsForClass(session.classId).first()
        val sessionAttendance = attendanceRecords.filter { it.sessionId == session.sessionId }
        
        val sessionDate = Date()
        println("Export: Session ${session.sessionCode} has ${sessionAttendance.size} attendance records")
        
        // Header
        builder.append("Roll Number,Name,Status,Marked At,Marked Via\n")
        
        // Get student details for each attendance record
        sessionAttendance.forEach { attendance ->
            val student = studentDao.getStudent().first()
            student?.let {
                if (it.email == attendance.studentEmail) {
                    builder.append("${it.rollNumber},${it.name},${attendance.attendanceStatus},${Date(attendance.markedAt)},${attendance.markedVia ?: "Unknown"}\n")
                }
            }
        }
        
        return builder.toString()
    }

    fun exportAllSessionsToCsv(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val csvContent = generateAllSessionsCsvContent()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
                val dateStr = dateFormat.format(Date())
                val fileName = "All_Attendance_Reports_$dateStr.csv"

                val cachePath = File(context.cacheDir, "csv")
                cachePath.mkdirs()
                val file = File(cachePath, fileName)
                file.writeText(csvContent)

                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, "All Attendance Reports")
                    putExtra(Intent.EXTRA_TEXT, "Complete attendance reports for all sessions")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(Intent.createChooser(shareIntent, "Share All Attendance Reports"))
            } catch (e: Exception) {
                println("Export all error: ${e.message}")
            }
        }
    }

    private suspend fun generateAllSessionsCsvContent(): String {
        val builder = StringBuilder()
        val sessions = _sessionsForClass.value
        
        // Header
        builder.append("Session Code,Date,Time,Total Attendance\n")
        
        sessions.forEach { session ->
            val attendanceRecords = attendanceDao.getSessionsForClass(session.classId).first()
            val sessionAttendance = attendanceRecords.filter { it.sessionId == session.sessionId }
            val presentCount = sessionAttendance.count { it.attendanceStatus == "present" }
            
            builder.append("${session.sessionCode},${session.scheduledDate},${session.startTime},$presentCount\n")
        }
        
        return builder.toString()
    }

    fun formatTimestamp(timestamp: Long, durationMinutes: Int = 60): Pair<String, String> {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        val startTime = Date(timestamp)
        val endTime = Date(timestamp + durationMinutes * 60 * 1000)
        
        return Pair(
            dateFormat.format(startTime),
            "${timeFormat.format(startTime)} - ${timeFormat.format(endTime)}"
        )
    }

    fun exportSessionToTxt(context: Context, session: ClassSessionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val txtContent = generateTxtContent(session)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
                val dateStr = dateFormat.format(Date())
                val fileName = "Attendance_${session.sessionCode}_$dateStr.txt"

                val cachePath = File(context.cacheDir, "txt")
                cachePath.mkdirs()
                val file = File(cachePath, fileName)
                file.writeText(txtContent)

                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Attendance Report - ${session.sessionCode}")
                    putExtra(Intent.EXTRA_TEXT, "Attendance report for session ${session.sessionCode}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(Intent.createChooser(shareIntent, "Share Attendance Report"))
            } catch (e: Exception) {
                println("Export txt error: ${e.message}")
            }
        }
    }

    private suspend fun generateTxtContent(session: ClassSessionEntity): String {
        val builder = StringBuilder()
        
        // Get attendance records for this session
        val attendanceRecords = attendanceDao.getSessionsForClass(session.classId).first()
        val sessionAttendance = attendanceRecords.filter { it.sessionId == session.sessionId }
        
        val sessionDate = Date()
        
        // Header
        builder.append("ATTENDANCE REPORT\n")
        builder.append("================\n\n")
        builder.append("Session Code: ${session.sessionCode}\n")
        builder.append("Date: ${session.scheduledDate}\n")
        builder.append("Time: ${session.startTime} - ${session.endTime}\n")
        builder.append("Total Students: ${sessionAttendance.size}\n\n")
        
        // Student list
        builder.append("STUDENT LIST:\n")
        builder.append("-------------\n")
        
        // Get student details for each attendance record
        sessionAttendance.forEach { attendance ->
            val student = studentDao.getStudent().first()
            student?.let {
                if (it.email == attendance.studentEmail) {
                    builder.append("${it.rollNumber} - ${it.name} (${attendance.attendanceStatus})\n")
                }
            }
        }
        
        return builder.toString()
    }

    fun deleteSession(session: ClassSessionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                classSessionDao.deleteSession(session)
                println("Session deleted successfully: ${session.sessionCode}")
            } catch (e: Exception) {
                println("Delete error: ${e.message}")
            }
        }
    }
    
    fun refreshAttendanceData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                println("🔄 Refreshing attendance data from Supabase...")
                
                // Sync all data from Supabase
                supabaseRepository.syncAllDataFromSupabase()
                
                // Reload attendance data for current class
                val currentClassId = classId.value
                if (currentClassId > 0) {
                    loadClassAttendanceData(currentClassId)
                }
                
                println("✅ Attendance data refreshed successfully!")
            } catch (e: Exception) {
                println("❌ Error refreshing attendance data: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    private fun loadClassAttendanceData(classId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get current student
                val student = studentDao.getStudent().first()
                if (student != null) {
                    println("📚 Loading attendance data for student: ${student.email}, classId: $classId")
                    
                    // First, get all class sessions for this class
                    val classSessions = classSessionDao.getSessionsForClass(classId).first()
                    println("📚 Found ${classSessions.size} class sessions for classId: $classId")
                    
                    if (classSessions.isNotEmpty()) {
                        // Get session IDs for this class
                        val sessionIds = classSessions.map { it.sessionId }
                        println("📚 Session IDs for class $classId: $sessionIds")
                        
                        // Get all attendance records for these sessions
                        val allAttendance = attendanceDao.getAllAttendance().first()
                        println("📚 Total attendance records in database: ${allAttendance.size}")
                        
                        // Filter attendance for current student and these specific sessions
                        val classAttendance = allAttendance.filter { attendance ->
                            attendance.studentEmail == student.email && 
                            sessionIds.contains(attendance.sessionId)
                        }
                        println("📚 Found ${classAttendance.size} attendance records for student ${student.email} in class $classId")
                        
                        // Calculate statistics
                        val totalLectures = classSessions.size // Total sessions for this class
                        val attendedLectures = classAttendance.count { attendance ->
                            attendance.attendanceStatus == "PRESENT" || attendance.attendanceStatus == "ATTENDED"
                        }
                        val missedLectures = totalLectures - attendedLectures
                        val attendancePercentage = if (totalLectures > 0) {
                            (attendedLectures.toFloat() / totalLectures.toFloat()) * 100f
                        } else 0f
                        
                        classAttendanceStats = ClassAttendanceStats(
                            totalLectures = totalLectures,
                            attendedLectures = attendedLectures,
                            missedLectures = missedLectures,
                            attendancePercentage = attendancePercentage
                        )
                        
                        // Get recent sessions for this class (from class sessions, not attendance)
                        // Sort by most recent first to ensure we get the latest sessions
                        val recentSessionsList = classSessions
                            .sortedByDescending { it.sessionId } // Sort by session ID (most recent first)
                            .take(5) // Take the 5 most recent sessions
                            .map { session ->
                                // Find attendance record for this session
                                val attendanceRecord = classAttendance.find { it.sessionId == session.sessionId }
                                
                                // Get students who attended this session
                                val attendedStudents = try {
                                    attendanceDao.getStudentsWhoAttendedSession(session.sessionId).map { studentInfo ->
                                        com.example.bleattendance.ui.viewmodels.StudentAttendanceInfo(
                                            name = studentInfo.name,
                                            rollNumber = studentInfo.rollNumber,
                                            email = studentInfo.email,
                                            attendanceStatus = studentInfo.attendanceStatus
                                        )
                                    }
                                } catch (e: Exception) {
                                    println("❌ Error fetching students for session ${session.sessionId}: ${e.message}")
                                    emptyList()
                                }
                                
                                ClassRecentSession(
                                    sessionId = session.sessionId.toString(), // Ensure this is a string
                                    date = session.scheduledDate,
                                    time = session.startTime,
                                    status = if (attendanceRecord != null) {
                                        when (attendanceRecord.attendanceStatus) {
                                            "PRESENT", "ATTENDED" -> "Present"
                                            "ABSENT", "MISSED" -> "Absent"
                                            else -> "Unknown"
                                        }
                                    } else {
                                        "Absent" // No attendance record means absent
                                    },
                                    sessionCode = session.sessionCode,
                                    attendedStudents = attendedStudents
                                )
                            }
                        
                        classRecentSessions = recentSessionsList
                        
                        println("📊 Class ${classId} Attendance Stats: $classAttendanceStats")
                        println("📅 Class ${classId} Recent Sessions: ${classRecentSessions.size} sessions")
                    } else {
                        println("📚 No class sessions found for classId: $classId")
                        classAttendanceStats = ClassAttendanceStats()
                        classRecentSessions = emptyList()
                    }
                } else {
                    println("❌ No student found")
                }
            } catch (e: Exception) {
                println("❌ Error loading class attendance data: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    fun exportSessionsToCSV() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                exportStatus = "Exporting..."
                
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "class_sessions_${timestamp}.csv"
                
                // Get the external files directory
                val externalDir = getApplication<AttendanceApp>().getExternalFilesDir(null)
                val exportFile = File(externalDir, fileName)
                
                FileWriter(exportFile).use { writer ->
                    // Write CSV header
                    writer.write("Session Date,Session Time,Session Code,Status,Student Name,Roll Number,Email,Attendance Status\n")
                    
                    // Write session data
                    classRecentSessions.forEach { session ->
                        if (session.attendedStudents.isNotEmpty()) {
                            session.attendedStudents.forEach { student ->
                                writer.write("${session.date},${session.time},${session.sessionCode},${session.status},${student.name},${student.rollNumber},${student.email},${student.attendanceStatus}\n")
                            }
                        } else {
                            // Write session without students
                            writer.write("${session.date},${session.time},${session.sessionCode},${session.status},,,,\n")
                        }
                    }
                }
                
                exportStatus = "Exported to: ${exportFile.absolutePath}"
                println("📤 Sessions exported to: ${exportFile.absolutePath}")
                
            } catch (e: Exception) {
                exportStatus = "Export failed: ${e.message}"
                println("❌ Export failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}