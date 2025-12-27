// In ui/viewmodels/TeacherRegistrationViewModel.kt

package com.example.bleattendance.ui.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bleattendance.AttendanceApp
import com.example.bleattendance.data.db.TeacherDao
import com.example.bleattendance.data.db.TeacherEntity
import com.example.bleattendance.data.repository.SupabaseRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class TeacherRegistrationViewModel(application: Application) : AndroidViewModel(application) {

    private val teacherDao: TeacherDao = (application as AttendanceApp).database.teacherDao()
    private val supabaseRepository: SupabaseRepository = (application as AttendanceApp).supabaseRepository

    // UI State is now much simpler
    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var subject by mutableStateOf("")
    var password by mutableStateOf("")

    fun registerTeacher(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val success = saveTeacherProfileWithSupabase()
                onResult(success, if (!success) "Failed to register teacher" else null)
            } catch (e: Exception) {
                onResult(false, "Error: ${e.message}")
            }
        }
    }

    suspend fun saveTeacherProfile(): Boolean {
        if (name.isBlank() || email.isBlank() || subject.isBlank() || password.isBlank()) {
            println("Validation failed: Empty fields")
            return false
        }

        // Hash the password
        val hashedPassword = com.example.bleattendance.utils.PasswordUtils.hashPassword(password)

        val teacher = TeacherEntity(
            name = name,
            email = email,
            subject = subject,
            password = hashedPassword
        )

        return try {
            teacherDao.insertTeacher(teacher)
            println("Teacher profile saved successfully: ${teacher.name}")
            true
        } catch (e: Exception) {
            println("Failed to save teacher profile: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // New method that uses Supabase integration
    suspend fun saveTeacherProfileWithSupabase(): Boolean {
        if (name.isBlank() || email.isBlank() || subject.isBlank() || password.isBlank()) {
            println("Validation failed: Empty fields")
            return false
        }

        return try {
            val result = supabaseRepository.createTeacher(name, email, subject, password)
            if (result.isSuccess) {
                println("Teacher profile saved successfully with Supabase sync: $name")
                true
            } else {
                println("Failed to save teacher profile with Supabase: ${result.exceptionOrNull()?.message}")
                // Fallback to local only
                saveTeacherProfile()
            }
        } catch (e: Exception) {
            println("Failed to save teacher profile with Supabase: ${e.message}")
            e.printStackTrace()
            // Fallback to local only
            saveTeacherProfile()
        }
    }
}