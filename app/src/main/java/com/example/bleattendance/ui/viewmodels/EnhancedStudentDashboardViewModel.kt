package com.example.bleattendance.ui.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.combine
import com.example.bleattendance.AttendanceApp
import com.example.bleattendance.data.db.ClassEntity
import com.example.bleattendance.data.db.StudentEntity
import com.example.bleattendance.data.db.TeacherEntity
import com.example.bleattendance.data.db.StudentGroupEntity
import com.example.bleattendance.data.repository.SupabaseRepository
import com.example.bleattendance.utils.PasswordUtils
import com.example.bleattendance.ble.GattClient
import com.example.bleattendance.ble.BleScanner
import com.example.bleattendance.data.repository.UserPreferencesRepository
import androidx.activity.ComponentActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

data class ClassWithTeacher(
    val classEntity: ClassEntity,
    val teacher: TeacherEntity?
)

data class ClassCardData(
    val subjectName: String,
    val teacherName: String,
    val teacherEmail: String,
    val startTime: String,
    val endTime: String,
    val days: String,
    val attendanceStatus: AttendanceStatus,
    val groupId: String,
    val classId: Int
)

enum class AttendanceStatus {
    ATTENDED, MISSED, UPCOMING
}

data class AttendanceStats(
    val totalLectures: Int = 0,
    val attendedLectures: Int = 0,
    val missedLectures: Int = 0,
    val attendancePercentage: Float = 0f
)

data class RecentSession(
    val sessionId: String,
    val subjectName: String,
    val teacherName: String,
    val date: String,
    val time: String,
    val status: AttendanceStatus,
    val sessionCode: String
)

class EnhancedStudentDashboardViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = (application as AttendanceApp).database
    private val studentDao = database.studentDao()
    private val teacherDao = database.teacherDao()
    private val studentGroupDao = database.studentGroupDao()
    private val classSessionDao = database.classSessionDao()
    private val assignmentDao = database.assignmentDao()
    private val submissionDao = database.submissionDao()
    private val supabaseRepository = (application as AttendanceApp).supabaseRepository
    private val userPreferencesRepository = (application as AttendanceApp).userPreferencesRepository
    
    // BLE scanning properties
    private var gattClient: GattClient? = null
    private var bleScanner: BleScanner? = null
    private var scanJob: Job? = null
    private var isBleAvailable = false
    
    var isLoading by mutableStateOf(false)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    // BLE scanning states
    var isScanning by mutableStateOf(false)
        private set
    
    var showAttendanceDialog by mutableStateOf(false)
        private set
    
    var showBiometricAuthDialog by mutableStateOf(false)
        private set
    
    var detectedSessionCode by mutableStateOf("")
        private set
    
    var currentClassId by mutableStateOf(0)
        private set
    
    var currentSubjectName by mutableStateOf("")
        private set
    
    var currentTeacherName by mutableStateOf("")
        private set
    
    // Manual code entry states
    var showManualCodeDialog by mutableStateOf(false)
        private set
    
    var isManualCodeConnecting by mutableStateOf(false)
        private set
    
    var manualCodeError by mutableStateOf<String?>(null)
        private set
    
    // Attendance statistics
    var attendanceStats by mutableStateOf(AttendanceStats())
        private set
    
    var recentSessions by mutableStateOf<List<RecentSession>>(emptyList())
        private set
    
    // Classes state for manual refresh
    var classesState by mutableStateOf<List<ClassCardData>>(emptyList())
        private set
    
    // Tab selection state (0 = Today's Classes, 1 = All Classes)
    private val _selectedTab = mutableStateOf(0)
    val selectedTab: State<Int> = _selectedTab
    
    init {
        // Load data from local database first (fast)
        viewModelScope.launch {
            try {
                println("🚀 EnhancedStudentDashboardViewModel initialized - loading local data...")
                
                // Load student profile data from local database
                loadStudentProfile()
                
                // Load attendance statistics and recent sessions from local database
                loadAttendanceStatistics()
                loadRecentSessions()
                
                // Schedule periodic sync (every 15 minutes) instead of immediate sync
                startPeriodicSync()
                
            } catch (e: Exception) {
                println("⚠️ Initial data load failed: ${e.message}")
            }
        }
    }
    
    private fun startPeriodicSync() {
        viewModelScope.launch {
            while (true) {
                delay(15 * 60 * 1000) // 15 minutes
                try {
                    println("🔄 Periodic sync (15min interval)...")
                    syncDataFromSupabase()
                } catch (e: Exception) {
                    println("⚠️ Periodic sync failed: ${e.message}")
                }
            }
        }
    }
    
    // Get current student profile
    val studentProfile: Flow<StudentEntity?> = studentDao.getStudent()
    
    // Student profile state variables for settings
    var studentName by mutableStateOf("")
        private set
    
    var studentEmail by mutableStateOf("")
        private set
    
    var studentRollNumber by mutableStateOf("")
        private set
    
    var studentBranch by mutableStateOf("")
        private set
    
    var studentGroupId by mutableStateOf("")
        private set
    
    // Get classes for current student (using student-specific methods)
    val classes: Flow<List<ClassCardData>> = studentProfile.flatMapLatest { student ->
        if (student == null) {
            flowOf(emptyList())
        } else {
            try {
                combine(
                    supabaseRepository.getClassesForStudent(student.email),
                    teacherDao.getAllTeachers()
                ) { studentClasses, allTeachers ->
                    println("🔍 EnhancedStudentDashboardViewModel - Class filtering:")
                    println("   - Student: ${student.name} (${student.email})")
                    println("   - Student GroupId: '${student.groupId}'")
                    println("   - Student Classes: ${studentClasses.size}")
                    println("   - Total Teachers: ${allTeachers.size}")
                    
                    // Convert to ClassCardData
                    studentClasses.map { classEntity ->
                        println("   - Creating ClassCardData for: ${classEntity.subjectName}")
                        
                        // Try exact match first
                        var teacher = allTeachers.find { teacher -> teacher.email == classEntity.teacherEmail }
                        
                        // If exact match fails, try partial match
                        if (teacher == null) {
                            teacher = allTeachers.find { teacher -> 
                                teacher.email.startsWith(classEntity.teacherEmail) || 
                                classEntity.teacherEmail.startsWith(teacher.email.split("@")[0])
                            }
                        }
                        
                        // If still no match, try to find by name
                        if (teacher == null) {
                            teacher = allTeachers.find { teacher -> 
                                teacher.name.lowercase().contains(classEntity.teacherEmail.lowercase()) ||
                                classEntity.teacherEmail.lowercase().contains(teacher.name.lowercase())
                            }
                        }
                        
                        val finalTeacherName = if (teacher != null) {
                            teacher.name
                        } else {
                            extractNameFromEmail(classEntity.teacherEmail)
                        }
                        
                        ClassCardData(
                            subjectName = classEntity.subjectName,
                            teacherName = finalTeacherName,
                            teacherEmail = classEntity.teacherEmail,
                            startTime = extractStartTime(classEntity.classSchedule),
                            endTime = extractEndTime(classEntity.classSchedule),
                            days = extractDays(classEntity.classSchedule),
                            attendanceStatus = AttendanceStatus.UPCOMING,
                            groupId = classEntity.groupId,
                            classId = classEntity.classId
                        )
                    }
                }
            } catch (e: Exception) {
                println("❌ Error in classes Flow: ${e.message}")
                flowOf(emptyList())
            }
        }
    }
    
    fun refreshClasses() {
        viewModelScope.launch {
            isLoading = true
            try {
                // Sync data from Supabase first
                println("🔄 Manual refresh - syncing data from Supabase...")
                syncDataFromSupabase()
                
                // Force a small delay to ensure database operations complete
                delay(100)
                
                // Manually load classes to ensure UI updates
                loadClassesManually()
                
                // Also refresh attendance data
                loadAttendanceStatistics()
                loadRecentSessions()
                
                // The Flow will automatically update the UI
                errorMessage = null
                println("✅ Classes and attendance data refreshed!")
            } catch (e: Exception) {
                errorMessage = e.message
                println("❌ Error refreshing classes: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
    
    fun forceSyncNow() {
        viewModelScope.launch {
            isLoading = true
            try {
                println("🔄 Force sync requested by user...")
                syncDataFromSupabase()
                
                // Refresh all data after sync
                loadStudentProfile()
                loadAttendanceStatistics()
                loadRecentSessions()
                loadClassesManually()
                
                errorMessage = null
                println("✅ Force sync completed!")
            } catch (e: Exception) {
                errorMessage = e.message
                println("❌ Error in force sync: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
    
    private suspend fun loadClassesManually() {
        try {
            // Wait a moment to ensure all database operations complete
            delay(100)
            
            val student = studentDao.getStudent().first()
            if (student != null) {
                // Debug: Check all students in database
                val allStudents = studentDao.getAllStudents().first()
                println("🔍 DEBUG: All students in database: ${allStudents.size}")
                allStudents.forEachIndexed { index, s ->
                    println("   Student $index: ${s.name} (${s.email}) - GroupId: '${s.groupId}'")
                }
                
                val studentClasses = supabaseRepository.getClassesForStudent(student.email).first()
                val allTeachers = teacherDao.getAllTeachers().first()
                
                println("🔍 Manual class loading:")
                println("   - Student: ${student.name} (${student.email})")
                println("   - Student GroupId: '${student.groupId}'")
                println("   - Student Classes: ${studentClasses.size}")
                println("   - Total Teachers: ${allTeachers.size}")
                
                val classCardData = studentClasses.map { classEntity ->
                    val teacher = allTeachers.find { it.email == classEntity.teacherEmail }
                    val teacherName = teacher?.name ?: "Unknown Teacher"
                    
                    println("   - Creating ClassCardData for: ${classEntity.subjectName}")
                    println("   - No class schedule provided, using default start time: 09:00")
                    println("   - No class schedule provided, using default end time: 10:30")
                    println("   - No class schedule provided, using default days: Daily")
                    
                    ClassCardData(
                        subjectName = classEntity.subjectName,
                        teacherName = teacherName,
                        teacherEmail = classEntity.teacherEmail,
                        startTime = extractStartTime(classEntity.classSchedule),
                        endTime = extractEndTime(classEntity.classSchedule),
                        days = extractDays(classEntity.classSchedule),
                        attendanceStatus = AttendanceStatus.UPCOMING,
                        groupId = classEntity.groupId,
                        classId = classEntity.classId
                    )
                }
                
                classesState = classCardData
                println("✅ Manually loaded ${classCardData.size} classes")
            }
        } catch (e: Exception) {
            println("❌ Error manually loading classes: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private suspend fun syncDataFromSupabase() {
        try {
            println("🔄 Starting Supabase sync...")
            
            // Get current student to sync their data
            val student = studentDao.getStudent().first()
            if (student != null) {
                println("🔄 Syncing student data for: ${student.name}")
                println("📝 Student groupId: ${student.groupId}")
                
                // Sync only student-specific data from Supabase
                // This will pull only classes, assignments, and sessions for this student
                println("🔄 Syncing student-specific data from Supabase...")
                val refreshResult = supabaseRepository.refreshCurrentStudentData()
                
                if (refreshResult.isSuccess) {
                    println("✅ Student data refresh completed successfully")
                } else {
                    println("⚠️ Student data refresh had issues: ${refreshResult.exceptionOrNull()?.message}")
                }
                
                // Check student-specific data after sync
                val studentClassesAfterSync = supabaseRepository.getClassesForStudent(student.email).first()
                println("📊 After sync - Student Classes: ${studentClassesAfterSync.size}")
                
            } else {
                println("❌ No student profile found for sync")
            }
            
        } catch (e: Exception) {
            println("❌ Sync error: ${e.message}")
            e.printStackTrace()
        }
    }
    
    // Helper functions to extract timing from class schedule
    private fun extractStartTime(classSchedule: String?): String {
        if (classSchedule.isNullOrBlank()) {
            println("   - No class schedule provided, using default start time: 09:00")
            return "09:00"
        }
        
        println("   - Extracting start time from: '$classSchedule'")
        
        // Look for time pattern like "09:00-10:30" or "Mon, Wed, Fri 09:00-10:30"
        val timePattern = Regex("(\\d{1,2}:\\d{2})-(\\d{1,2}:\\d{2})")
        val match = timePattern.find(classSchedule)
        val startTime = match?.groupValues?.get(1) ?: "09:00"
        
        println("   - Extracted start time: $startTime")
        return startTime
    }
    
    private fun extractEndTime(classSchedule: String?): String {
        if (classSchedule.isNullOrBlank()) {
            println("   - No class schedule provided, using default end time: 10:30")
            return "10:30"
        }
        
        println("   - Extracting end time from: '$classSchedule'")
        
        // Look for time pattern like "09:00-10:30" or "Mon, Wed, Fri 09:00-10:30"
        val timePattern = Regex("(\\d{1,2}:\\d{2})-(\\d{1,2}:\\d{2})")
        val match = timePattern.find(classSchedule)
        val endTime = match?.groupValues?.get(2) ?: "10:30"
        
        println("   - Extracted end time: $endTime")
        return endTime
    }
    
    private fun extractDays(classSchedule: String?): String {
        if (classSchedule.isNullOrBlank()) {
            println("   - No class schedule provided, using default days: Daily")
            return "Daily"
        }
        
        println("   - Extracting days from: '$classSchedule'")
        
        // First, try to extract days from the beginning of the schedule string
        // The schedule format is typically: "Monday 09:00-10:30" or "Mon, Wed, Fri 09:00-10:30"
        val trimmedSchedule = classSchedule.trim()
        
        // Look for day patterns at the start of the string
        val dayPattern = Regex("^([A-Za-z,\\s]+?)(?=\\s+\\d{2}:\\d{2}|$)")
        val dayMatch = dayPattern.find(trimmedSchedule)
        
        if (dayMatch != null) {
            val daysString = dayMatch.value.trim()
            println("   - Found days string: '$daysString'")
            
            // Clean up the days string and return it
            val cleanedDays = daysString.replace(Regex("\\s+"), " ").trim()
            if (cleanedDays.isNotBlank()) {
                println("   - Extracted days: $cleanedDays")
                return cleanedDays
            }
        }
        
        // Fallback: look for any day patterns in the entire string
        val anyDayPattern = Regex("(Mon|Tue|Wed|Thu|Fri|Sat|Sun|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)(?:\\s*,\\s*(Mon|Tue|Wed|Thu|Fri|Sat|Sun|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday))*")
        val anyMatch = anyDayPattern.find(classSchedule)
        val days = anyMatch?.value ?: "Daily"
        
        println("   - Extracted days (fallback): $days")
        return days
    }
    
    private fun extractNameFromEmail(email: String): String {
        // If it's already a name (no @ symbol), return as is
        if (!email.contains("@")) {
            return email.replaceFirstChar { it.uppercase() } // Capitalize first letter
        }
        
        // If it's an email, extract the part before @ and capitalize
        val name = email.split("@")[0]
        return name.replaceFirstChar { it.uppercase() }
    }

    // Helper function to determine if a class is visible to a student
    private fun isClassVisibleToStudent(classGroupId: String, studentGroupId: String): Boolean {
        println("   - isClassVisibleToStudent: '$classGroupId' vs '$studentGroupId'")
        
        // Direct match
        if (classGroupId == studentGroupId) {
            println("   - Direct match found!")
            return true
        }
        
        // Parse both groupIds to check for "ALL" batch case
        // Format: Branch_AdmissionYear-GraduationYear_Division_Batch_SemX
        val classParts = classGroupId.split("_")
        val studentParts = studentGroupId.split("_")
        
        if (classParts.size >= 5 && studentParts.size >= 5) {
            val classBranch = classParts[0]
            val classYearRange = classParts[1]
            val classDivision = classParts[2]
            val classBatch = classParts[3]
            val classSemester = classParts[4]
            
            val studentBranch = studentParts[0]
            val studentYearRange = studentParts[1]
            val studentDivision = studentParts[2]
            val studentBatch = studentParts[3]
            val studentSemester = studentParts[4]
            
            // Check if class is for "ALL" batches (batch = "ALL")
            if (classBatch == "ALL") {
                println("   - Checking ALL batch case...")
                // Match if branch, year range, division, and semester are the same
                val branchMatch = classBranch == studentBranch
                val yearMatch = classYearRange == studentYearRange
                val divisionMatch = classDivision == studentDivision
                val semesterMatch = classSemester == studentSemester
                
                val isVisible = branchMatch && yearMatch && divisionMatch && semesterMatch
                println("   - ALL batch check: Branch($branchMatch) Year($yearMatch) Division($divisionMatch) Semester($semesterMatch) = $isVisible")
                return isVisible
            } else {
                println("   - Class batch '$classBatch' is not ALL, no match")
            }
        }
        
        return false
    }

    // Debug function to check data
    fun debugData() {
        viewModelScope.launch {
            try {
                println("🔍 COMPREHENSIVE DEBUG DATA:")
                
                // Check all students in database
                val allStudents = studentDao.getAllStudents().first()
                println("📊 Total students in database: ${allStudents.size}")
                allStudents.forEachIndexed { index, student ->
                    println("   Student $index: ${student.name} (${student.email}) - GroupId: ${student.groupId}")
                }
                
                // Get current student (what the app is using)
                val student = studentDao.getStudent().first()
                println("📝 Current student (getStudent()): ${student?.name} (${student?.email}) - GroupId: ${student?.groupId}")
                
                // Check student classes
                val studentClasses = if (student != null) {
                    supabaseRepository.getClassesForStudent(student.email).first()
                } else {
                    emptyList()
                }
                val allTeachers = teacherDao.getAllTeachers().first()
                
                println("📚 CLASS DATA:")
                println("   - Student Classes: ${studentClasses.size}")
                println("   - Total Teachers: ${allTeachers.size}")
                
                studentClasses.forEach { classEntity ->
                    println("   - Student Class: ${classEntity.subjectName} | GroupId: '${classEntity.groupId}' | Teacher: ${classEntity.teacherEmail}")
                }
                
                // Check state variables
                println("🎯 STATE VARIABLES:")
                println("   - studentName: '$studentName'")
                println("   - studentEmail: '$studentEmail'")
                println("   - studentGroupId: '$studentGroupId'")
                
            } catch (e: Exception) {
                println("❌ Debug error: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    // Check BLE permissions before starting scan
    @SuppressLint("MissingPermission")
    private fun checkBlePermissions(): Boolean {
        return try {
            val context = getApplication<Application>()
            val bluetoothManager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            bluetoothAdapter?.isEnabled == true
        } catch (e: SecurityException) {
            println("❌ BLE permission denied: ${e.message}")
            false
        } catch (e: Exception) {
            println("❌ BLE error: ${e.message}")
            false
        }
    }
    
    // BLE Scanning Methods
    @SuppressLint("MissingPermission")
    fun checkBluetoothAndStartScan() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                if (isScanning) {
                    stopScanning(foundDevice = false)
                    return@launch
                }
                
                // Check BLE permissions first
                if (!checkBlePermissions()) {
                    println("❌ BLE permissions not available")
                    return@launch
                }
                
                isScanning = true
                println("🔍 Starting BLE scan for attendance sessions...")
                
                // Initialize BLE scanner
                bleScanner = BleScanner(
                    context = getApplication(),
                    onDeviceFound = { scanResult ->
                        val device = scanResult.device
                        println("📱 Found BLE device: ${device.name} - ${device.address}")
                        
                        // Check if this is an attendance session device
                        if (device.name?.contains("Attendance") == true || 
                            device.name?.contains("Session") == true) {
                            println("✅ Found attendance session device!")
                            stopScanning(foundDevice = true)
                            
                            // Show attendance dialog
                            showAttendanceDialog = true
                            detectedSessionCode = device.name ?: "Unknown"
                        }
                    },
                    onScanFailed = { error ->
                        println("❌ BLE scan failed: $error")
                        stopScanning(foundDevice = false)
                    }
                )
                
                val scanStarted = bleScanner?.startScanning() ?: false
                if (!scanStarted) {
                    println("❌ Failed to start BLE scanning")
                    stopScanning(foundDevice = false)
                }
                
            } catch (e: Exception) {
                println("❌ Error starting BLE scan: ${e.message}")
                stopScanning(foundDevice = false)
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    fun stopScanning(foundDevice: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                bleScanner?.stopScanning()
                scanJob?.cancel()
                isScanning = false
                
                if (foundDevice) {
                    println("✅ BLE scan completed - device found")
                } else {
                    println("⏹️ BLE scan stopped")
                }
            } catch (e: Exception) {
                println("❌ Error stopping BLE scan: ${e.message}")
            }
        }
    }
    
    fun initializeGattClient(activity: ComponentActivity) {
        try {
            gattClient = GattClient(getApplication(), activity)
            println("🔧 GattClient initialized with activity context")
        } catch (e: Exception) {
            println("❌ Failed to initialize GattClient: ${e.message}")
        }
    }
    
    fun onAttendanceDialogDismiss() {
        showAttendanceDialog = false
        detectedSessionCode = ""
        currentClassId = 0
        currentSubjectName = ""
        currentTeacherName = ""
    }
    
    fun onAttendanceDialogConfirmed() {
        showAttendanceDialog = false
        // Show biometric authentication dialog
        showBiometricAuthDialog = true
    }
    
    fun onManualConnectionRequested() {
        showAttendanceDialog = false
        // Connect to teacher session using existing session code via BLE
        connectToTeacherSessionWithCode()
    }
    
    fun onBiometricAuthDialogDismiss() {
        showBiometricAuthDialog = false
    }
    
    fun onBiometricAuthSuccess() {
        showBiometricAuthDialog = false
        // Mark attendance for the current class
        if (currentClassId > 0) {
            markAttendanceForClass(currentClassId, detectedSessionCode)
            println("🎉 Attendance marked for class $currentClassId after biometric authentication")
        }
        // Reset state
        currentClassId = 0
        detectedSessionCode = ""
        currentSubjectName = ""
        currentTeacherName = ""
    }
    
    // Manual code entry methods
    fun showManualCodeEntry() {
        showManualCodeDialog = true
        manualCodeError = null
    }
    
    fun onManualCodeDismissed() {
        println("🛑 Manual code dialog dismissed - stopping BLE scanning")
        println("🛑 Before: showManualCodeDialog = $showManualCodeDialog")
        showManualCodeDialog = false
        println("🛑 After: showManualCodeDialog = $showManualCodeDialog")
        isManualCodeConnecting = false
        manualCodeError = null
        
        // Stop BLE scanning if it's running
        if (isScanning) {
            stopScanning(foundDevice = false)
            println("🛑 BLE scanning stopped due to manual code dialog cancellation")
        } else {
            println("🛑 No BLE scanning was active")
        }
    }
    
    fun onManualCodeConfirmed(sessionCode: String) {
        if (sessionCode.isBlank()) {
            manualCodeError = "Please enter a valid session code"
            return
        }
        
        isManualCodeConnecting = true
        manualCodeError = null
        
        // Use the entered session code to connect
        connectWithManualCode(sessionCode)
    }
    
    // Mock test method to simulate BLE device detection
    fun testMockAttendancePopup() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                println("🧪 Testing mock attendance popup...")
                
                // Simulate finding a class (use first available student class or create mock data)
                val student = studentDao.getStudent().first()
                val studentClasses = if (student != null) {
                    supabaseRepository.getClassesForStudent(student.email).first()
                } else {
                    emptyList()
                }
                if (studentClasses.isNotEmpty()) {
                    val mockClass = studentClasses.first()
                    currentClassId = mockClass.classId
                    currentSubjectName = mockClass.subjectName
                    
                    // Get teacher data
                    val teacher = teacherDao.getTeacher().first()
                    currentTeacherName = teacher?.name ?: "Mock Teacher"
                    
                    detectedSessionCode = "MOCK_SESSION_${System.currentTimeMillis()}"
                    
                    // Show the attendance dialog
                    showAttendanceDialog = true
                    
                    println("✅ Mock popup triggered for class: ${mockClass.subjectName} with teacher: ${currentTeacherName}")
                } else {
                    // No classes available, use mock data
                    currentClassId = 1
                    currentSubjectName = "Data Structures"
                    currentTeacherName = "Dr. Smith"
                    detectedSessionCode = "MOCK_SESSION_${System.currentTimeMillis()}"
                    showAttendanceDialog = true
                    
                    println("✅ Mock popup triggered with mock data: Data Structures - Dr. Smith")
                }
                
            } catch (e: Exception) {
                println("❌ Error in mock test: ${e.message}")
                // Fallback to mock data
                currentClassId = 1
                currentSubjectName = "Data Structures"
                currentTeacherName = "Dr. Smith"
                detectedSessionCode = "MOCK_SESSION_${System.currentTimeMillis()}"
                showAttendanceDialog = true
            }
        }
    }
    
    // Load class and teacher information for the popup
    private fun loadClassInformation(classId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get class data
                val classEntity = teacherDao.getClassById(classId).first()
                if (classEntity != null) {
                    currentSubjectName = classEntity.subjectName
                    
                    // Get teacher data
                    val teacher = teacherDao.getTeacher().first()
                    if (teacher != null) {
                        currentTeacherName = teacher.name
                    } else {
                        currentTeacherName = "Unknown Teacher"
                    }
                } else {
                    currentSubjectName = "Unknown Subject"
                    currentTeacherName = "Unknown Teacher"
                }
            } catch (e: Exception) {
                println("❌ Error loading class information: ${e.message}")
                currentSubjectName = "Unknown Subject"
                currentTeacherName = "Unknown Teacher"
            }
        }
    }
    
    // Connect to teacher session using existing session code via BLE with proximity validation
    @SuppressLint("MissingPermission")
    private fun connectToTeacherSessionWithCode() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                if (detectedSessionCode.isEmpty()) {
                    println("❌ No session code available for manual connection")
                    return@launch
                }
                
                println("🔗 Connecting to teacher session with code: $detectedSessionCode")
                println("📍 Proximity validation: Student must be physically present in class")
                println("⏰ Connection timeout: 30 seconds to prevent abuse")
                
                // Check BLE permissions first
                if (!checkBlePermissions()) {
                    println("❌ BLE permissions not available for manual connection")
                    return@launch
                }
                
                // Start BLE scanning to find the teacher's device
                isScanning = true
                
                // Add timeout to prevent abuse (30 seconds)
                val timeoutJob = viewModelScope.launch(Dispatchers.IO) {
                    kotlinx.coroutines.delay(30000) // 30 seconds
                    if (isScanning) {
                        println("⏰ Connection timeout reached - stopping scan to prevent abuse")
                        stopScanning(foundDevice = false)
                    }
                }
                
                // Initialize BLE scanner to find the specific session with proximity validation
                bleScanner = BleScanner(
                    context = getApplication(),
                    onDeviceFound = { scanResult ->
                        val device = scanResult.device
                        val rssi = scanResult.rssi // Signal strength indicator
                        println("📱 Found BLE device during manual connection: ${device.name} - ${device.address} (RSSI: $rssi)")
                        
                        // Check if this device matches our session code
                        val deviceSessionCode = extractSessionCodeFromDevice(device)
                        if (deviceSessionCode == detectedSessionCode) {
                            // ✅ PROXIMITY VALIDATION: Check if student is physically present
                            if (validateProximity(rssi)) {
                                println("✅ Found matching teacher device for session: $detectedSessionCode")
                                println("✅ Proximity validation passed - student is physically present")
                                stopScanning(foundDevice = true)
                                
                            // Connect to the teacher's device
                            connectToAttendanceDevice(device, currentClassId)
                            } else {
                                println("❌ Proximity validation failed - student is too far from teacher device")
                                println("❌ RSSI: $rssi dBm (too weak for physical presence)")
                                // Continue scanning for a closer device
                            }
                        } else {
                            println("⏳ Device doesn't match session code, continuing scan...")
                        }
                    },
                    onScanFailed = { error ->
                        println("❌ BLE scan failed during manual connection: $error")
                        stopScanning(foundDevice = false)
                    }
                )
                
                val scanStarted = bleScanner?.startScanning() ?: false
                if (!scanStarted) {
                    println("❌ Failed to start BLE scanning for manual connection")
                    stopScanning(foundDevice = false)
                }
                
            } catch (e: Exception) {
                println("❌ Error in manual connection: ${e.message}")
                stopScanning(foundDevice = false)
            }
        }
    }
    
    // Validate that student is physically present based on BLE signal strength
    private fun validateProximity(rssi: Int): Boolean {
        // RSSI (Received Signal Strength Indicator) values:
        // -30 to -50 dBm: Very close (1-2 meters)
        // -50 to -70 dBm: Close (2-5 meters) 
        // -70 to -90 dBm: Moderate distance (5-15 meters)
        // -90+ dBm: Far away (15+ meters)
        
        // Only allow connection if signal is strong enough (student is physically present)
        val proximityThreshold = -70 // dBm - allows connection within ~5-10 meters
        val isValidProximity = rssi >= proximityThreshold
        
        println("📍 Proximity Check: RSSI = $rssi dBm, Threshold = $proximityThreshold dBm, Valid = $isValidProximity")
        
        if (!isValidProximity) {
            println("🚫 SECURITY: Student is too far from teacher device to mark attendance")
            println("🚫 This prevents remote attendance marking from outside the classroom")
        }
        
        return isValidProximity
    }
    
    // Connect using manually entered session code
    @SuppressLint("MissingPermission")
    private fun connectWithManualCode(sessionCode: String) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                println("🔗 Connecting with manual session code: $sessionCode")
                
                // Check BLE permissions first
                if (!checkBlePermissions()) {
                    println("❌ BLE permissions not available for manual code connection")
                    manualCodeError = "Bluetooth permissions not available. Please check app permissions."
                    isManualCodeConnecting = false
                    return@launch
                }
                
                // Set the session code
                detectedSessionCode = sessionCode
                
                // Try to find which class this session belongs to
                val detectedClassId = findClassBySessionCode(sessionCode)
                
                if (detectedClassId > 0) {
                    println("✅ Found class for manual session code: $detectedClassId")
                    
                    // Load class information
                    loadClassInformation(detectedClassId)
                    currentClassId = detectedClassId
                    
                    // Start BLE scanning to find the teacher's device
                    isScanning = true
                    
                    // Initialize BLE scanner to find the specific session with proximity validation
                    bleScanner = BleScanner(
                        context = getApplication(),
                        onDeviceFound = { scanResult ->
                            val device = scanResult.device
                            val rssi = scanResult.rssi
                            println("📱 Found BLE device with manual code: ${device.name} - ${device.address} (RSSI: $rssi)")
                            
                            // Check if this device matches our session code
                            val deviceSessionCode = extractSessionCodeFromDevice(device)
                            if (deviceSessionCode == sessionCode) {
                                // ✅ PROXIMITY VALIDATION: Check if student is physically present
                                if (validateProximity(rssi)) {
                                    println("✅ Found matching teacher device for manual session: $sessionCode")
                                    println("✅ Proximity validation passed - student is physically present")
                                    stopScanning(foundDevice = true)
                                    
                                    // Close manual code dialog and show biometric auth
                                    isManualCodeConnecting = false
                                    showManualCodeDialog = false
                                    showBiometricAuthDialog = true
                                } else {
                                    println("❌ Proximity validation failed - student is too far from teacher device")
                                    println("❌ RSSI: $rssi dBm (too weak for physical presence)")
                                    manualCodeError = "You must be physically present in the classroom. Move closer to the teacher's device."
                                    isManualCodeConnecting = false
                                }
                            } else {
                                println("⏳ Device doesn't match session code, continuing scan...")
                            }
                        },
                        onScanFailed = { error ->
                            println("❌ BLE scan failed with manual code: $error")
                            stopScanning(foundDevice = false)
                            manualCodeError = "Failed to find teacher device. Please check the session code and try again."
                            isManualCodeConnecting = false
                        }
                    )
                    
                    val scanStarted = bleScanner?.startScanning() ?: false
                    if (!scanStarted) {
                        println("❌ Failed to start BLE scanning with manual code")
                        stopScanning(foundDevice = false)
                        manualCodeError = "Failed to start scanning. Please check Bluetooth and try again."
                        isManualCodeConnecting = false
                    }
                    
                } else {
                    println("❌ Could not find class for manual session code: $sessionCode")
                    manualCodeError = "Invalid session code. Please check with your teacher."
                    isManualCodeConnecting = false
                }
                
            } catch (e: Exception) {
                println("❌ Error connecting with manual code: ${e.message}")
                manualCodeError = "Connection failed. Please try again."
                isManualCodeConnecting = false
            }
        }
    }
    
    // Start auto-detect scan that finds the class automatically
    @SuppressLint("MissingPermission")
    fun startAutoDetectScan() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                if (isScanning) {
                    stopScanning(foundDevice = false)
                    return@launch
                }
                
                // Check BLE permissions first
                if (!checkBlePermissions()) {
                    println("❌ BLE permissions not available for auto-detect scan")
                    return@launch
                }
                
                isScanning = true
                println("🔍 Starting REAL BLE scan (not mock)...")
                println("📡 Scanning for BLE devices with attendance service...")
                
                // Initialize BLE scanner
                bleScanner = BleScanner(
                    context = getApplication(),
                    onDeviceFound = { scanResult ->
                        val device = scanResult.device
                        val rssi = scanResult.rssi
                        println("📱 Found BLE device: ${device.name} - ${device.address} (RSSI: $rssi)")
                        
                        // ✅ PROXIMITY VALIDATION: Check if student is physically present
                        if (validateProximity(rssi)) {
                            // The BleScanner already filters for devices with our service data
                            // So any device found here is an attendance session device
                            println("✅ Found attendance session device!")
                            println("✅ Proximity validation passed - student is physically present")
                            stopScanning(foundDevice = true)
                            
                            // Auto-detect which class this is for
                            autoDetectAndMarkAttendance(device)
                        } else {
                            println("❌ Proximity validation failed - student is too far from teacher device")
                            println("❌ RSSI: $rssi dBm (too weak for physical presence)")
                            // Continue scanning for a closer device
                        }
                    },
                    onScanFailed = { error ->
                        println("❌ BLE scan failed: $error")
                        stopScanning(foundDevice = false)
                    }
                )
                
                val scanStarted = bleScanner?.startScanning() ?: false
                if (!scanStarted) {
                    println("❌ Failed to start BLE scanning")
                    stopScanning(foundDevice = false)
                }
                
            } catch (e: Exception) {
                println("❌ Error starting auto-detect BLE scan: ${e.message}")
                stopScanning(foundDevice = false)
            }
        }
    }
    
    // Start scan for a specific class (kept for compatibility)
    @SuppressLint("MissingPermission")
    fun startScanForClass(classId: Int) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                if (isScanning) {
                    stopScanning(foundDevice = false)
                    return@launch
                }
                
                isScanning = true
                println("🔍 Starting BLE scan for class $classId...")
                
                // Initialize BLE scanner
                bleScanner = BleScanner(
                    context = getApplication(),
                    onDeviceFound = { scanResult ->
                        val device = scanResult.device
                        println("📱 Found BLE device: ${device.name} - ${device.address}")
                        
                        // Check if this is an attendance session device
                        if (device.name?.contains("Attendance") == true || 
                            device.name?.contains("Session") == true) {
                            println("✅ Found attendance session device for class $classId!")
                            stopScanning(foundDevice = true)
                            
                            // Connect to the device and get session code
                            connectToAttendanceDevice(device, classId)
                        }
                    },
                    onScanFailed = { error ->
                        println("❌ BLE scan failed: $error")
                        stopScanning(foundDevice = false)
                    }
                )
                
                val scanStarted = bleScanner?.startScanning() ?: false
                if (!scanStarted) {
                    println("❌ Failed to start BLE scanning")
                    stopScanning(foundDevice = false)
                }
                
            } catch (e: Exception) {
                println("❌ Error starting BLE scan for class $classId: ${e.message}")
                stopScanning(foundDevice = false)
            }
        }
    }
    
    // Auto-detect which class the student is scanning for and mark attendance
    @SuppressLint("MissingPermission")
    private fun autoDetectAndMarkAttendance(device: android.bluetooth.BluetoothDevice) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                println("🔍 Auto-detecting class for device: ${device.name}")
                
                // Extract session code from BLE device
                val sessionCode = extractSessionCodeFromDevice(device)
                
                if (sessionCode.isNotEmpty()) {
                    println("📝 Session code received: $sessionCode")
                    
                    // Try to find which class this session belongs to
                    val detectedClassId = findClassBySessionCode(sessionCode)
                    
                    if (detectedClassId > 0) {
                        println("✅ Auto-detected class ID: $detectedClassId for session: $sessionCode")
                        
                        // Load class and teacher information
                        loadClassInformation(detectedClassId)
                        
                        // Show attendance dialog with detected class info
                        showAttendanceDialog = true
                        detectedSessionCode = sessionCode
                        currentClassId = detectedClassId
                        
                        println("✅ Ready to mark attendance for auto-detected class $detectedClassId")
                    } else {
                        println("❌ Could not auto-detect class for session: $sessionCode")
                        // Still show dialog but without specific class info
                        showAttendanceDialog = true
                        detectedSessionCode = sessionCode
                        currentSubjectName = "Unknown Subject"
                        currentTeacherName = "Unknown Teacher"
                    }
                } else {
                    println("❌ Could not extract session code from device")
                }
                
            } catch (e: Exception) {
                println("❌ Error in auto-detect attendance: ${e.message}")
            }
        }
    }
    
    // Find class ID by session code
    private suspend fun findClassBySessionCode(sessionCode: String): Int {
        return try {
            // Get student's classes and try to match session code
            val student = studentDao.getStudent().first()
            val studentClasses = if (student != null) {
                supabaseRepository.getClassesForStudent(student.email).first()
            } else {
                emptyList()
            }
            
            // For now, we'll use a simple approach:
            // If session code contains class info, extract it
            // Otherwise, we'll need to implement a more sophisticated matching
            
            // Example: if session code is "DS_001" and we have a class with subject "Data Structures"
            // we could match them, but for now let's use a simple approach
            
            // TODO: Implement proper session code to class ID mapping
            // This could be done by:
            // 1. Storing session codes in the database when teachers start sessions
            // 2. Matching session codes to class IDs
            // 3. Using teacher email or other identifiers
            
            // For now, return the first available class ID as a fallback
            if (studentClasses.isNotEmpty()) {
                studentClasses.first().classId
            } else {
                0
            }
        } catch (e: Exception) {
            println("❌ Error finding class by session code: ${e.message}")
            0
        }
    }
    
    // Connect to attendance device and get session code
    @SuppressLint("MissingPermission")
    private fun connectToAttendanceDevice(device: android.bluetooth.BluetoothDevice, classId: Int) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                println("🔗 Connecting to attendance device: ${device.name}")
                
                // Initialize GattClient if not already done
                if (gattClient == null) {
                    // We need an activity context for GattClient
                    // For now, we'll use a simplified approach
                    println("⚠️ GattClient not initialized - using simplified connection")
                }
                
                // Extract session code from BLE device
                val sessionCode = extractSessionCodeFromDevice(device)
                
                if (sessionCode.isNotEmpty()) {
                    println("📝 Session code received: $sessionCode")
                    
                    // Show attendance dialog with real session code
                    showAttendanceDialog = true
                    detectedSessionCode = sessionCode
                    
                    // Mark attendance for this specific class
                    markAttendanceForClass(classId, sessionCode)
                } else {
                    println("❌ Could not extract session code from device")
                }
                
            } catch (e: Exception) {
                println("❌ Error connecting to attendance device: ${e.message}")
            }
        }
    }
    
    // Extract session code from BLE device
    @SuppressLint("MissingPermission")
    private fun extractSessionCodeFromDevice(device: android.bluetooth.BluetoothDevice): String {
        // Try to get session code from device name first
        val deviceName = device.name ?: ""
        if (deviceName.isNotEmpty()) {
            val patterns = listOf(
                "Session_(\\w+)".toRegex(),
                "Attendance_(\\w+)".toRegex(),
                "Class_(\\w+)".toRegex()
            )
            
            for (pattern in patterns) {
                val match = pattern.find(deviceName)
                if (match != null) {
                    return match.groupValues[1]
                }
            }
        }
        
        // If no pattern matches in device name, generate a session code from device address
        // This ensures we always have a unique session code
        val address = device.address.replace(":", "").takeLast(6)
        return "SESS_$address"
    }
    
    // Mark attendance for a specific class
    private fun markAttendanceForClass(classId: Int, sessionCode: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val student = studentDao.getStudent().first()
                if (student != null) {
                    println("🔄 Marking attendance for class $classId with session code: $sessionCode")
                    
                    // Find the class session that matches the session code
                    val classSessionDao = database.classSessionDao()
                    val classSessions = classSessionDao.getSessionsForClass(classId).first()
                    val matchingSession = classSessions.find { it.sessionCode == sessionCode }
                    
                    if (matchingSession != null) {
                        println("✅ Found matching class session: ${matchingSession.sessionId} for code: $sessionCode")
                        
                        // Use SupabaseRepository to create attendance session (this will sync to Supabase)
                        val result = supabaseRepository.createAttendanceSession(
                            sessionId = matchingSession.sessionId,
                            studentEmail = student.email,
                            attendanceStatus = "PRESENT",
                            markedVia = "BLE"
                        )
                        
                        if (result.isSuccess) {
                            println("✅ Attendance marked and synced to Supabase for session ${matchingSession.sessionId}")
                            
                            // Immediately refresh recent sessions to update UI
                            loadRecentSessions()
                        } else {
                            println("⚠️ Attendance marked locally but failed to sync to Supabase: ${result.exceptionOrNull()?.message}")
                        }
                    } else {
                        println("❌ No matching class session found for code: $sessionCode in class: $classId")
                        // Fallback: create attendance with classId as sessionId (for backward compatibility)
                        val result = supabaseRepository.createAttendanceSession(
                            sessionId = classId,
                            studentEmail = student.email,
                            attendanceStatus = "PRESENT",
                            markedVia = "BLE"
                        )
                        
                        if (result.isSuccess) {
                            println("✅ Attendance marked with fallback method for class $classId")
                            
                            // Immediately refresh recent sessions to update UI
                            loadRecentSessions()
                        } else {
                            println("❌ Failed to mark attendance with fallback method: ${result.exceptionOrNull()?.message}")
                        }
                    }
                    
                    // Refresh attendance statistics
                    loadAttendanceStatistics()
                } else {
                    println("❌ No student found to mark attendance")
                }
            } catch (e: Exception) {
                println("❌ Error marking attendance for class $classId: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    // Student Profile Methods
    fun loadStudentProfile() {
        viewModelScope.launch {
            try {
                val student = studentDao.getStudent().first()
                if (student != null) {
                    studentName = student.name
                    studentEmail = student.email
                    studentRollNumber = student.rollNumber
                    studentBranch = student.branch
                    studentGroupId = student.groupId ?: "No Group"
                    
                    println("👤 Student Profile Loaded: $studentName ($studentEmail)")
                } else {
                    println("❌ No student profile found")
                }
            } catch (e: Exception) {
                println("❌ Error loading student profile: ${e.message}")
            }
        }
    }
    
    // Debug function to clear all data (for testing)
    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                println("🧹 DEBUG: Clearing all app data...")
                
                // Clear all database tables
                studentDao.deleteStudentProfile()
                teacherDao.deleteAllTeachers()
                studentGroupDao.deleteAllStudentGroups()
                classSessionDao.deleteAllSessions()
                assignmentDao.deleteAllAssignments()
                submissionDao.deleteAllSubmissions()
                
                // Clear user preferences
                userPreferencesRepository.clearAllPreferences()
                
                // Reset all state variables
                studentName = ""
                studentEmail = ""
                studentRollNumber = ""
                studentBranch = ""
                studentGroupId = ""
                attendanceStats = AttendanceStats()
                recentSessions = emptyList()
                
                println("✅ DEBUG: All app data cleared successfully")
            } catch (e: Exception) {
                println("❌ DEBUG: Error clearing data: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    // Debug function to check what students are in the database
    fun debugCheckStudents() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                println("🔍 DEBUG: Checking all students in database...")
                val allStudents = studentDao.getAllStudents().first()
                println("📊 Total students in database: ${allStudents.size}")
                allStudents.forEachIndexed { index, student ->
                    println("   Student $index: ${student.name} (${student.email}) - GroupId: ${student.groupId}")
                }
                
                val currentStudent = studentDao.getStudent().first()
                println("📝 Current student (getStudent()): ${currentStudent?.name} (${currentStudent?.email}) - GroupId: ${currentStudent?.groupId}")
            } catch (e: Exception) {
                println("❌ DEBUG: Error checking students: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    // Force clear all data and restart app state
    fun forceClearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                println("🧹 FORCE CLEAR: Clearing all app data aggressively...")
                
                // Clear all database tables multiple times to ensure they're empty
                repeat(3) {
                    studentDao.deleteStudentProfile()
                    teacherDao.deleteAllTeachers()
                    studentGroupDao.deleteAllStudentGroups()
                    classSessionDao.deleteAllSessions()
                    assignmentDao.deleteAllAssignments()
                    submissionDao.deleteAllSubmissions()
                }
                
                // Clear user preferences
                userPreferencesRepository.clearAllPreferences()
                
                // Reset all state variables
                studentName = ""
                studentEmail = ""
                studentRollNumber = ""
                studentBranch = ""
                studentGroupId = ""
                attendanceStats = AttendanceStats()
                recentSessions = emptyList()
                
                // Force reload student profile
                loadStudentProfile()
                
                println("✅ FORCE CLEAR: All app data cleared and reloaded")
            } catch (e: Exception) {
                println("❌ FORCE CLEAR: Error clearing data: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    // Logout function to clear all student data
    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                println("🔄 Logging out student - clearing all data...")
                
                // Clear all database tables to ensure clean logout
                studentDao.deleteStudentProfile()
                teacherDao.deleteAllTeachers()
                studentGroupDao.deleteAllStudentGroups()
                classSessionDao.deleteAllSessions()
                assignmentDao.deleteAllAssignments()
                submissionDao.deleteAllSubmissions()
                
                // Clear user preferences
                userPreferencesRepository.clearAllPreferences()
                
                // Reset all state variables
                studentName = ""
                studentEmail = ""
                studentRollNumber = ""
                studentBranch = ""
                studentGroupId = ""
                attendanceStats = AttendanceStats()
                recentSessions = emptyList()
                
                println("✅ Student logged out successfully - all data cleared")
            } catch (e: Exception) {
                println("❌ Logout error: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    // Attendance Statistics Methods
    fun loadAttendanceStatistics() {
        viewModelScope.launch {
            try {
                val student = studentDao.getStudent().first()
                if (student != null) {
                    // Get attendance data from database
                    val attendanceDao = database.attendanceDao()
                    val allAttendance = attendanceDao.getAllAttendance().first()
                    
                    // Filter attendance for current student
                    val studentAttendance = allAttendance.filter { attendance ->
                        attendance.studentEmail == student.email 
                    }
                    
                    // Calculate statistics
                    val totalLectures = studentAttendance.size
                    val attendedLectures = studentAttendance.count { attendance ->
                        attendance.attendanceStatus == "PRESENT" || attendance.attendanceStatus == "ATTENDED" 
                    }
                    val missedLectures = totalLectures - attendedLectures
                    val attendancePercentage = if (totalLectures > 0) {
                        (attendedLectures.toFloat() / totalLectures.toFloat()) * 100f
                    } else 0f
                    
                    attendanceStats = AttendanceStats(
                        totalLectures = totalLectures,
                        attendedLectures = attendedLectures,
                        missedLectures = missedLectures,
                        attendancePercentage = attendancePercentage
                    )
                    
                    println("📊 Attendance Stats: $attendanceStats")
                }
            } catch (e: Exception) {
                println("❌ Error loading attendance statistics: ${e.message}")
            }
        }
    }
    
    fun loadRecentSessions() {
        viewModelScope.launch {
            try {
                val student = studentDao.getStudent().first()
                if (student != null) {
                    // Get recent attendance sessions
                    val attendanceDao = database.attendanceDao()
                    val allAttendance = attendanceDao.getAllAttendance().first()
                    
                    // Filter and sort recent sessions for current student
                    val studentAttendance = allAttendance.filter { attendance ->
                        attendance.studentEmail == student.email 
                    }.sortedByDescending { attendance -> attendance.markedAt }
                    
                    // Get class information for each session
                    val studentClasses = supabaseRepository.getClassesForStudent(student.email).first()
                    val allTeachers = teacherDao.getAllTeachers().first()
                    
                    val recentSessionsList = studentAttendance.take(5).map { attendance ->
                        // Find the class for this attendance
                        val classEntity = studentClasses.find { it.classId == attendance.sessionId }
                        val teacher = allTeachers.find { it.email == classEntity?.teacherEmail }
                        
                        val status = when (attendance.attendanceStatus) {
                            "PRESENT", "ATTENDED" -> AttendanceStatus.ATTENDED
                            "ABSENT", "MISSED" -> AttendanceStatus.MISSED
                            else -> AttendanceStatus.UPCOMING
                        }
                        
                        RecentSession(
                            sessionId = attendance.sessionId.toString(),
                            subjectName = classEntity?.subjectName ?: "Unknown Subject",
                            teacherName = teacher?.name ?: "Unknown Teacher",
                            date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(attendance.markedAt)),
                            time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(attendance.markedAt)),
                            status = status,
                            sessionCode = attendance.sessionId.toString()
                        )
                    }
                    
                    recentSessions = recentSessionsList
                    println("📅 Recent Sessions: ${recentSessions.size} sessions loaded")
                }
            } catch (e: Exception) {
                println("❌ Error loading recent sessions: ${e.message}")
            }
        }
    }
    
    // Function to update selected tab
    fun updateSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }
    
    // Function to create test classes for assignment testing
    suspend fun createTestClassesForTeacher(teacherEmail: String): Result<Unit> {
        return try {
            println("🔄 Creating test classes for teacher: $teacherEmail")
            
            // First, ensure the teacher exists in the database
            val existingTeacher = teacherDao.getTeacherByEmail(teacherEmail)
            if (existingTeacher == null) {
                println("📝 Teacher not found, creating teacher first...")
                val teacherEntity = TeacherEntity(
                    email = teacherEmail,
                    name = "Test Teacher",
                    subject = "Computer Science",
                    password = PasswordUtils.hashPassword("default123")
                )
                teacherDao.insertTeacher(teacherEntity)
                println("✅ Created teacher: $teacherEmail")
            } else {
                println("✅ Teacher already exists: $teacherEmail")
            }
            
            // Get all student groups to create classes for them
            val studentGroups = studentGroupDao.getAllGroups().first()
            println("📝 Found ${studentGroups.size} student groups")
            
            var classesCreated = 0
            
            for (group in studentGroups) {
                // Check if the group exists in the database
                val existingGroup = studentGroupDao.getGroupById(group.groupId).first()
                if (existingGroup == null) {
                    println("⚠️ Group ${group.groupId} not found in database, skipping class creation")
                    continue
                }
                
                // Create a class for each group with a unique subject name
                val classId = (teacherEmail + "TestClass" + group.groupId + System.currentTimeMillis()).hashCode()
                val subjectName = "Test Subject ${System.currentTimeMillis() % 10000} for ${group.groupId}"
                val classEntity = ClassEntity(
                    classId = classId,
                    subjectName = subjectName,
                    teacherEmail = teacherEmail,
                    groupId = group.groupId,
                    classSchedule = "Monday 09:00-10:30",
                    isActive = true
                )
                
                try {
                    // Check if class already exists
                    val existingClass = teacherDao.getClassById(classEntity.classId).first()
                    if (existingClass != null) {
                        println("⚠️ Class with ID ${classEntity.classId} already exists, skipping creation")
                        continue
                    }
                    
                    teacherDao.insertClass(classEntity)
                    classesCreated++
                    println("✅ Created test class: ${classEntity.subjectName} for group ${group.groupId}")
                } catch (e: Exception) {
                    println("⚠️ Failed to create class for group ${group.groupId}: ${e.message}")
                    println("   - Teacher email exists: ${teacherDao.getTeacherByEmail(teacherEmail) != null}")
                    println("   - Group exists: ${existingGroup != null}")
                    println("   - Class ID: ${classEntity.classId}")
                    println("   - Teacher Email: ${classEntity.teacherEmail}")
                    println("   - Group ID: ${classEntity.groupId}")
                    println("   - Subject: ${classEntity.subjectName}")
                }
            }
            
            println("✅ Created $classesCreated test classes for teacher $teacherEmail")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Failed to create test classes: ${e.message}")
            Result.failure(e)
        }
    }
}
