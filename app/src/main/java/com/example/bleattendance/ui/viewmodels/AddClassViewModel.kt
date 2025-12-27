// In ui/viewmodels/AddClassViewModel.kt

package com.example.bleattendance.ui.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bleattendance.AttendanceApp
import com.example.bleattendance.data.db.ClassEntity
import com.example.bleattendance.data.db.TeacherDao
import com.example.bleattendance.data.db.TeacherEntity
import com.example.bleattendance.data.repository.SupabaseRepository
import com.example.bleattendance.utils.GroupUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class AddClassViewModel(application: Application) : AndroidViewModel(application) {

    private val teacherDao: TeacherDao = (application as AttendanceApp).database.teacherDao()
    private val supabaseRepository: SupabaseRepository = (application as AttendanceApp).supabaseRepository

    // Department mapping: Full name -> Short form (same as student registration)
    private val departmentMapping = mapOf(
        "Computer Science" to "CS",
        "Information Technology" to "IT", 
        "Electronics & Communication" to "ECE",
        "Mechanical Engineering" to "ME",
        "Civil Engineering" to "CE",
        "Electrical Engineering" to "EE",
        "Chemical Engineering" to "CHE",
        "Aerospace Engineering" to "AE",
        "Biotechnology" to "BT",
        "Business Administration" to "BA",
        "Commerce" to "COM",
        "Arts" to "ARTS",
        "Science" to "SCI"
    )

    // UI State for the Add Class form
    var branch by mutableStateOf("")
    var division by mutableStateOf("") // Empty by default
    var batch by mutableStateOf("") // Empty by default
    var subjectName by mutableStateOf("")
    var semester by mutableStateOf(1) // Default to semester 1
    var classSchedule by mutableStateOf("") // Class schedule (e.g., "Mon, Wed, Fri 9:00-10:30")
    var classDate by mutableStateOf("") // Class date (e.g., "2025-01-15")
    var classStartTime by mutableStateOf("") // Class start time (e.g., "09:00")
    var classEndTime by mutableStateOf("") // Class end time (e.g., "10:30")
    var classDays by mutableStateOf("") // Class days (e.g., "Mon, Wed, Fri")
    var groupId by mutableStateOf("")
    
    // Get the short form of the selected department
    val departmentShortForm: String
        get() = departmentMapping[branch] ?: branch
    
    // Get list of department options for the UI
    val departmentOptions: List<String>
        get() = departmentMapping.keys.toList()
    
    // Division options
    val divisionOptions = listOf("A", "B", "C", "D", "E", "F")
    
    // Semester options (1-8 for engineering)
    val semesterOptions = listOf(1, 2, 3, 4, 5, 6, 7, 8)
    
    // Batch options (None = ALL batches for the division)
    val batchOptions = listOf("None", "1", "2", "3")
    
    // Class days options
    val classDaysOptions = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    
    // Teacher profile
    private var teacherProfile: TeacherEntity? = null
    
    // Function to build class schedule string (for backward compatibility)
    fun buildClassSchedule(): String {
        val scheduleParts = mutableListOf<String>()
        
        if (classDays.isNotBlank()) {
            scheduleParts.add(classDays)
        }
        
        if (classStartTime.isNotBlank() && classEndTime.isNotBlank()) {
            scheduleParts.add("$classStartTime-$classEndTime")
        }
        
        if (classDate.isNotBlank()) {
            scheduleParts.add("Date: $classDate")
        }
        
        return scheduleParts.joinToString(" ")
    }
    

    init {
        loadTeacherProfile()
    }
    
    // Public function to manually load teacher profile
    fun refreshTeacherProfile() {
        loadTeacherProfile()
    }
    
    private fun loadTeacherProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                teacherProfile = teacherDao.getTeacher().first()
                teacherProfile?.let { teacher ->
                    // Set default subject name from teacher's profile
                    if (teacher.subject.isNotBlank()) {
                        subjectName = teacher.subject
                        println("Loaded teacher profile: ${teacher.name} - Subject: ${teacher.subject}")
                    } else {
                        println("Teacher profile loaded but subject is empty: ${teacher.name}")
                    }
                } ?: run {
                    println("No teacher profile found")
                }
            } catch (e: Exception) {
                println("Error loading teacher profile: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun saveClass(teacherEmail: String, onResult: (Boolean, String?) -> Unit) {
        println("🚀 Starting class creation...")
        viewModelScope.launch {
            try {
                println("📝 Class data:")
                println("   - Subject: '$subjectName'")
                println("   - Branch: '$branch'")
                println("   - Division: '$division'")
                println("   - Batch: $batch")
                println("   - Class Schedule: '$classSchedule'")
                println("   - Teacher Email: '$teacherEmail'")
                
                val success = addClass(teacherEmail)
                println("💾 Save result: $success")
                onResult(success, if (!success) "Failed to save class" else null)
            } catch (e: Exception) {
                println("💥 Class creation error: ${e.message}")
                e.printStackTrace()
                onResult(false, "Error: ${e.message}")
            }
        }
    }

    suspend fun addClass(teacherEmail: String): Boolean {
        println("🔍 Validating class data...")
        
        if (branch.isBlank() || division.isBlank() || subjectName.isBlank()) {
            println("❌ Validation failed: Empty fields")
            println("   - branch.isBlank(): ${branch.isBlank()}")
            println("   - division.isBlank(): ${division.isBlank()}")
            println("   - subjectName.isBlank(): ${subjectName.isBlank()}")
            return false
        }

        // Calculate correct admission year based on semester
        // For engineering: 8 semesters over 4 years (2 semesters per year)
        // Sem 1-2: Year 1, Sem 3-4: Year 2, Sem 5-6: Year 3, Sem 7-8: Year 4
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1 // 1-12
        
        // Calculate which academic year we're in (July to June cycle)
        val academicYear = if (currentMonth >= 7) currentYear else currentYear - 1
        
        // Calculate admission year based on semester
        // If semester is 1-2, they started this academic year
        // If semester is 3-4, they started last academic year
        // If semester is 5-6, they started 2 academic years ago
        // If semester is 7-8, they started 3 academic years ago
        val yearsBack = (semester - 1) / 2
        val admissionYear = academicYear - yearsBack
        val graduationYear = admissionYear + 4
        
        println("🔍 Semester calculation:")
        println("   - Current Year: $currentYear")
        println("   - Current Month: $currentMonth")
        println("   - Academic Year: $academicYear")
        println("   - Selected Semester: $semester")
        println("   - Years Back: $yearsBack")
        println("   - Calculated Admission Year: $admissionYear")
        println("   - Calculated Graduation Year: $graduationYear")
        
        // Generate groupId - convert "None" to 0 for "ALL batches"
        val batchNum = if (batch == "None") 0 else batch.toIntOrNull() ?: 0
        val groupId = GroupUtils.generateGroupId(
            branch = departmentShortForm, // Use short form of department
            admissionYear = admissionYear,
            graduationYear = graduationYear,
            division = division,
            batch = batchNum,
            semester = semester
        )
        
        println("✅ Validation passed, creating class...")
        println("📝 Generated groupId: '$groupId'")
        println("📝 Selected semester: $semester")
        println("📝 Class details:")
        println("   - Subject: $subjectName")
        println("   - Teacher: $teacherEmail")
        println("   - Group ID: $groupId")

        return try {
            // First, ensure the teacher profile exists
            println("🔍 Checking if teacher profile exists...")
            val teacherExists = teacherDao.getTeacher().first() != null
            if (!teacherExists) {
                println("❌ Teacher profile not found in database")
                return false
            }
            println("✅ Teacher profile found")
            
            // Build the class schedule from individual components
            val finalSchedule = if (classSchedule.isNotBlank()) {
                classSchedule
            } else {
                buildClassSchedule()
            }
            
            println("💾 Creating class using repository...")
            println("📝 Final class schedule: '$finalSchedule'")
            val result = supabaseRepository.createClass(
                teacherEmail = teacherEmail,
                subjectName = subjectName,
                groupId = groupId,
                classSchedule = finalSchedule
            )
            
            if (result.isSuccess) {
                println("✅ Class added successfully: $subjectName")
                println("✅ Linked to group: $groupId")
                true
            } else {
                println("❌ Failed to add class: ${result.exceptionOrNull()?.message}")
                result.exceptionOrNull()?.printStackTrace()
                false
            }
        } catch (e: Exception) {
            println("❌ Failed to add class: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}