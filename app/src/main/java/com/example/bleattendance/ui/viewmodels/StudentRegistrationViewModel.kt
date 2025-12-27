// In ui/viewmodels/StudentRegistrationViewModel.kt

package com.example.bleattendance.ui.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bleattendance.AttendanceApp
import com.example.bleattendance.data.db.StudentDao
import com.example.bleattendance.data.db.StudentEntity
import com.example.bleattendance.data.db.StudentGroupEntity
import com.example.bleattendance.data.db.AcademicConfigEntity
import com.example.bleattendance.data.repository.SupabaseRepository
import com.example.bleattendance.utils.GroupUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class StudentRegistrationViewModel(application: Application) : AndroidViewModel(application) {

    private val studentDao: StudentDao = (application as AttendanceApp).database.studentDao()
    private val supabaseRepository: SupabaseRepository = (application as AttendanceApp).supabaseRepository

    // Department mapping: Full name -> Short form
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

    // UI State for the student form
    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var rollNumber by mutableStateOf("")
    var department by mutableStateOf("") // Empty by default
    var division by mutableStateOf("") // Empty by default
    var batch by mutableStateOf("") // Empty by default
    var password by mutableStateOf("")
    
    // New fields for enhanced registration
    var admissionYear by mutableStateOf("") // Empty by default
    var graduationYear by mutableStateOf("")
    
    // Computed properties
    val currentSemester: Int
        get() = if (admissionYear.isNotBlank()) {
            GroupUtils.calculateCurrentSemester(admissionYear.toIntOrNull() ?: 0)
        } else 1
    
    val calculatedGraduationYear: Int
        get() = if (admissionYear.isNotBlank()) {
            GroupUtils.calculateGraduationYear(admissionYear.toIntOrNull() ?: 0)
        } else 0
    
    // Get the short form of the selected department
    val departmentShortForm: String
        get() = departmentMapping[department] ?: department
    
    // Get list of department options for the UI
    val departmentOptions: List<String>
        get() = departmentMapping.keys.toList()
    
    val groupId: String
        get() = if (department.isNotBlank() && admissionYear.isNotBlank() && 
                   division.isNotBlank() && batch.isNotBlank()) {
            val admission = admissionYear.toIntOrNull() ?: 0
            val grad = graduationYear.toIntOrNull() ?: calculatedGraduationYear
            val batchNum = batch.toIntOrNull() ?: 1
            // Use the short form of the department for group ID generation
            GroupUtils.generateGroupId(departmentShortForm, admission, grad, division, batchNum, currentSemester)
        } else ""
    
    // Function to reset form to default values
    fun resetToDefaults() {
        name = ""
        email = ""
        rollNumber = ""
        department = ""
        division = ""
        batch = ""
        admissionYear = ""
        graduationYear = ""
    }

    fun registerStudent(onResult: (Boolean, String?) -> Unit) {
        println("🚀 Starting student registration...")
        viewModelScope.launch {
            try {
                println("📝 Student data:")
                println("   - Name: '$name'")
                println("   - Email: '$email'")
                println("   - Roll Number: '$rollNumber'")
                println("   - Department: '$department'")
                println("   - Division: '$division'")
                println("   - Batch: '$batch'")
                println("   - Admission Year: '$admissionYear'")
                println("   - Group ID: '$groupId'")
                println("   - Current Semester: $currentSemester")
                
                val success = saveStudentProfile()
                println("💾 Save result: $success")
                onResult(success, if (!success) "Failed to register student" else null)
            } catch (e: Exception) {
                println("💥 Registration error: ${e.message}")
                e.printStackTrace()
                onResult(false, "Error: ${e.message}")
            }
        }
    }

    suspend fun saveStudentProfile(): Boolean {
        println("🔍 Validating student data...")
        
        if (name.isBlank() || email.isBlank() || rollNumber.isBlank() || 
            department.isBlank() || division.isBlank() || batch.isBlank() ||
            admissionYear.isBlank() || groupId.isBlank()) {
            println("❌ Validation failed: Empty fields")
            println("   - name.isBlank(): ${name.isBlank()}")
            println("   - email.isBlank(): ${email.isBlank()}")
            println("   - rollNumber.isBlank(): ${rollNumber.isBlank()}")
            println("   - department.isBlank(): ${department.isBlank()}")
            println("   - division.isBlank(): ${division.isBlank()}")
            println("   - batch.isBlank(): ${batch.isBlank()}")
            println("   - admissionYear.isBlank(): ${admissionYear.isBlank()}")
            println("   - groupId.isBlank(): ${groupId.isBlank()}")
            return false
        }

        // Validate that roll number is not empty and contains valid characters
        if (rollNumber.isBlank() || !rollNumber.matches(Regex("^[A-Za-z0-9]+$"))) {
            println("❌ Validation failed: Roll number must be alphanumeric (letters and numbers only)")
            println("   - rollNumber: '$rollNumber'")
            return false
        }

        println("✅ Validation passed, creating student entity...")
        
        val admission = admissionYear.toIntOrNull() ?: 0
        val grad = graduationYear.toIntOrNull() ?: calculatedGraduationYear
        val batchNum = batch.toIntOrNull() ?: 1

        // Hash the password
        val hashedPassword = com.example.bleattendance.utils.PasswordUtils.hashPassword(password)

        val student = StudentEntity(
            name = name,
            email = email,
            rollNumber = rollNumber,
            branch = departmentShortForm, // Using short form of department as branch
            admissionYear = admission,
            graduationYear = grad,
            division = division,
            batch = batchNum,
            currentSemester = currentSemester,
            groupId = groupId,
            password = hashedPassword
        )

        println("📝 Created student entity:")
        println("   - Name: ${student.name}")
        println("   - Email: ${student.email}")
        println("   - Roll Number: ${student.rollNumber}")
        println("   - Department (Full): $department")
        println("   - Branch (Short): ${student.branch}")
        println("   - Group ID: ${student.groupId}")
        println("   - Current Semester: ${student.currentSemester}")

        return try {
            println("💾 Creating student using SupabaseRepository...")
            val result = supabaseRepository.createStudent(
                email = student.email,
                name = student.name,
                rollNumber = student.rollNumber,
                branch = student.branch,
                admissionYear = student.admissionYear,
                graduationYear = student.graduationYear,
                division = student.division,
                batch = student.batch,
                currentSemester = student.currentSemester,
                groupId = groupId, // Use the non-null groupId from ViewModel
                password = password
            )
            
            if (result.isSuccess) {
                println("✅ Student profile saved successfully: ${student.name}")
                println("✅ Assigned to group: ${student.groupId}")
                println("✅ Current semester: ${student.currentSemester}")
                true
            } else {
                println("❌ Failed to save student profile: ${result.exceptionOrNull()?.message}")
                result.exceptionOrNull()?.printStackTrace()
                false
            }
        } catch (e: Exception) {
            println("❌ Failed to save student profile: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}