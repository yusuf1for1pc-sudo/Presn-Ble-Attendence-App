// In ui/viewmodels/TeacherDashboardViewModel.kt

package com.example.bleattendance.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bleattendance.AttendanceApp
import com.example.bleattendance.data.db.ClassEntity
import com.example.bleattendance.data.db.TeacherEntity
import com.example.bleattendance.data.db.AttendanceSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

class TeacherDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val teacherDao = (application as AttendanceApp).database.teacherDao()
    private val attendanceDao = (application as AttendanceApp).database.attendanceDao()
    private val userPreferencesRepository = (application as AttendanceApp).userPreferencesRepository
    private val supabaseRepository = (application as AttendanceApp).supabaseRepository

    val teacherProfile: StateFlow<TeacherEntity?> = teacherDao.getTeacher()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Individual teacher properties for easy access
    val teacherName: StateFlow<String> = teacherProfile.map { it?.name ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    
    val teacherEmail: StateFlow<String> = teacherProfile.map { it?.email ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    
    val teacherSubject: StateFlow<String> = teacherProfile.map { it?.subject ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    


    private val _classList = MutableStateFlow<List<ClassEntity>>(emptyList())
    val classList: StateFlow<List<ClassEntity>> = _classList.asStateFlow()

    // Statistics for dashboard
    private val _totalSessions = MutableStateFlow(0)
    val totalSessions: StateFlow<Int> = _totalSessions.asStateFlow()

    private val _totalStudents = MutableStateFlow(0)
    val totalStudents: StateFlow<Int> = _totalStudents.asStateFlow()

    private val _lastSessionInfo = MutableStateFlow<String>("No sessions yet")
    val lastSessionInfo: StateFlow<String> = _lastSessionInfo.asStateFlow()

    init {
        println("TeacherDashboardViewModel initialized")
        
        // Trigger auto-sync when ViewModel initializes (after login)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                delay(500) // Small delay to ensure everything is ready
                println("🔄 Auto-syncing teacher data after login...")
                val refreshResult = supabaseRepository.refreshCurrentTeacherData()
                if (refreshResult.isSuccess) {
                    println("✅ Teacher auto-sync completed after login")
                } else {
                    println("⚠️ Teacher auto-sync had issues: ${refreshResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                println("❌ Teacher auto-sync failed after login: ${e.message}")
            }
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            teacherProfile.collect { teacher: TeacherEntity? ->
                println("Teacher profile updated: ${teacher?.name} (${teacher?.email})")
                loadClassesForTeacher(teacher)
                
                // Calculate statistics for the teacher
                viewModelScope.launch {
                    loadTeacherStatistics(teacher)
                }
            }
        }
        
        // Listen to class changes directly to catch auto-sync updates
        viewModelScope.launch(Dispatchers.IO) {
            teacherProfile
                .flatMapLatest { teacher: TeacherEntity? ->
                    if (teacher != null) {
                        teacherDao.getClassesForTeacher(teacher.email)
                    } else {
                        flowOf(emptyList<ClassEntity>())
                    }
                }
                .collect { classes: List<ClassEntity> ->
                    val teacher = teacherProfile.value
                    println("🔄 Classes updated for teacher ${teacher?.email}: ${classes.size} classes")
                    classes.forEach { classEntity: ClassEntity ->
                        println("  - Class: ${classEntity.subjectName} (Group: ${classEntity.groupId})")
                    }
                    _classList.value = classes
                }
        }
    }
    
    private suspend fun loadClassesForTeacher(teacher: TeacherEntity?) {
        val classes = teacher?.let { teacherEntity ->
            try {
                val teacherClasses = teacherDao.getClassesForTeacher(teacherEntity.email).first()
                println("Found ${teacherClasses.size} classes for teacher ${teacherEntity.email}")
                teacherClasses.forEach { classEntity ->
                    println("  - Class: ${classEntity.subjectName} (Group: ${classEntity.groupId})")
                }
                teacherClasses
            } catch (e: Exception) {
                println("Database error loading classes: ${e.message}")
                emptyList<ClassEntity>()
            }
        } ?: emptyList()
        println("Updating class list with ${classes.size} classes")
        _classList.value = classes
    }

    private suspend fun loadTeacherStatistics(teacher: TeacherEntity?) {
        teacher?.let { teacherEntity ->
            try {
                // Get total sessions
                val totalSessions = attendanceDao.getTotalSessionsForTeacher(teacherEntity.email)
                _totalSessions.value = totalSessions
                println("Total sessions for teacher: $totalSessions")

                // Get total students
                val totalStudents = attendanceDao.getTotalStudentsForTeacher(teacherEntity.email) ?: 0
                _totalStudents.value = totalStudents
                println("Total students for teacher: $totalStudents")

                // Get last session info
                val lastSession = attendanceDao.getLastSessionForTeacher(teacherEntity.email)
                val lastSessionText = if (lastSession != null) {
                    val date = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                        .format(java.util.Date(lastSession.markedAt))
                    "Last: $date"
                } else {
                    "No sessions yet"
                }
                _lastSessionInfo.value = lastSessionText
                println("Last session info: $lastSessionText")
            } catch (e: Exception) {
                println("Error calculating statistics: ${e.message}")
                _totalSessions.value = 0
                _totalStudents.value = 0
                _lastSessionInfo.value = "No sessions yet"
            }
        } ?: run {
            _totalSessions.value = 0
            _totalStudents.value = 0
            _lastSessionInfo.value = "No sessions yet"
        }
    }

    // Manual refresh function to force reload classes
    fun refreshClasses() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val teacher = teacherProfile.value
                if (teacher != null) {
                    println("🔄 Manually refreshing classes for teacher: ${teacher.email}")
                    
                    // First, refresh current teacher's data from Supabase
                    println("🔄 Refreshing current teacher's data from Supabase...")
                    val refreshResult = supabaseRepository.refreshCurrentTeacherData()
                    if (refreshResult.isSuccess) {
                        println("✅ Teacher data refresh completed successfully")
                    } else {
                        println("⚠️ Teacher data refresh had issues: ${refreshResult.exceptionOrNull()?.message}")
                    }
                    
                    // Then reload from local database
                    val teacherClasses = teacherDao.getClassesForTeacher(teacher.email).first()
                    println("📊 Manual refresh found ${teacherClasses.size} classes after sync")
                    teacherClasses.forEach { classEntity ->
                        println("  - Class: ${classEntity.subjectName} (Group: ${classEntity.groupId})")
                    }
                    _classList.value = teacherClasses
                    
                    // Also refresh statistics
                    loadTeacherStatistics(teacher)
                }
            } catch (e: Exception) {
                println("❌ Manual refresh error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                teacherDao.deleteTeacherProfile()
                userPreferencesRepository.clearUserRole()
                println("Teacher logged out successfully")
            } catch (e: Exception) {
                println("Logout error: ${e.message}")
            }
        }
    }

    fun deleteClass(classId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                teacherDao.deleteClassById(classId)
                println("Class deleted successfully: $classId")
                // Refresh the class list after deletion
                refreshClasses()
            } catch (e: Exception) {
                println("Delete class error: ${e.message}")
            }
        }
    }
    
    fun updateTeacherProfile(name: String, email: String, subject: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentTeacher = teacherProfile.value
                if (currentTeacher != null) {
                    teacherDao.updateTeacherFields(
                        email = email,
                        name = name,
                        subject = subject
                    )
                    println("Teacher profile updated successfully: $name, $email, $subject")
                } else {
                    println("No current teacher found to update")
                }
            } catch (e: Exception) {
                println("Update teacher profile error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // Get sessions for a specific class
    fun getSessionsForClass(classId: Int): Flow<List<AttendanceSessionEntity>> {
        return attendanceDao.getSessionsForClass(classId)
    }
}