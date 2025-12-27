package com.example.bleattendance.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bleattendance.AttendanceApp
import com.example.bleattendance.data.repository.SupabaseRepository
import com.example.bleattendance.data.repository.UserPreferencesRepository
import com.example.bleattendance.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    
    private val supabaseRepository = (application as AttendanceApp).supabaseRepository
    private val userPreferencesRepository = (application as AttendanceApp).userPreferencesRepository
    
    // UI State
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()
    
    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()
    
    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess.asStateFlow()
    
    private val _userRole = MutableStateFlow<UserRole?>(null)
    val userRole: StateFlow<UserRole?> = _userRole.asStateFlow()
    
    fun updateEmail(email: String) {
        _email.value = email
        _errorMessage.value = ""
    }
    
    fun updatePassword(password: String) {
        _password.value = password
        _errorMessage.value = ""
    }
    
    fun login() {
        val emailValue = _email.value.trim()
        val passwordValue = _password.value.trim()
        
        if (emailValue.isEmpty() || passwordValue.isEmpty()) {
            _errorMessage.value = "Please enter both email and password"
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) {
            _errorMessage.value = "Please enter a valid email address"
            return
        }
        
        _isLoading.value = true
        _errorMessage.value = ""
        
        viewModelScope.launch {
            try {
                println("🔐 Attempting login for: $emailValue")
                
                // Check if user exists in local database first
                val studentResult = supabaseRepository.getStudentByEmail(emailValue)
                val teacherResult = supabaseRepository.getTeacherByEmail(emailValue)
                
                var student = if (studentResult.isSuccess) studentResult.getOrNull() else null
                var teacher = if (teacherResult.isSuccess) teacherResult.getOrNull() else null
                
                // Always try to sync from Supabase to get the latest password
                println("🔄 Syncing latest data from Supabase...")
                try {
                    // Try to sync teacher from Supabase first
                    val syncTeacherResult = supabaseRepository.syncTeacherFromSupabase(emailValue)
                    if (syncTeacherResult.isSuccess) {
                        teacher = supabaseRepository.getTeacherByEmail(emailValue).getOrNull()
                        println("✅ Teacher synced from Supabase: ${teacher?.name}")
                    } else {
                        // If teacher sync failed, try student sync
                        val syncStudentResult = supabaseRepository.syncStudentFromSupabase(emailValue)
                        if (syncStudentResult.isSuccess) {
                            student = supabaseRepository.getStudentByEmail(emailValue).getOrNull()
                            println("✅ Student synced from Supabase: ${student?.name}")
                        }
                    }
                } catch (e: Exception) {
                    println("⚠️ Sync from Supabase failed: ${e.message}")
                }
                
                when {
                    student != null -> {
                        println("✅ Student found: ${student.name}")
                        println("🔐 Stored password hash: ${student.password}")
                        println("🔐 Entered password: $passwordValue")
                        println("🔐 Password match check: ${passwordValue == student.password}")
                        
                        // Verify password using proper hashing
                        if (com.example.bleattendance.utils.PasswordUtils.verifyPassword(passwordValue, student.password)) {
                            _userRole.value = UserRole.STUDENT
                            userPreferencesRepository.saveUserRole(UserRole.STUDENT)
                            userPreferencesRepository.saveUserEmail(emailValue)
                            
                            // Note: Auto-sync will be triggered by the dashboard ViewModel after navigation
                            
                            _loginSuccess.value = true
                            println("✅ Student login successful")
                        } else {
                            _errorMessage.value = "Invalid password"
                            println("❌ Student password verification failed")
                        }
                    }
                    teacher != null -> {
                        println("✅ Teacher found: ${teacher.name}")
                        println("🔐 Stored password hash: ${teacher.password}")
                        println("🔐 Entered password: $passwordValue")
                        println("🔐 Password match check: ${passwordValue == teacher.password}")
                        
                        if (com.example.bleattendance.utils.PasswordUtils.verifyPassword(passwordValue, teacher.password)) {
                            _userRole.value = UserRole.TEACHER
                            userPreferencesRepository.saveUserRole(UserRole.TEACHER)
                            userPreferencesRepository.saveUserEmail(emailValue)
                            
                            // Note: Auto-sync will be triggered by the dashboard ViewModel after navigation
                            
                            _loginSuccess.value = true
                            println("✅ Teacher login successful")
                        } else {
                            _errorMessage.value = "Invalid password"
                            println("❌ Password verification failed")
                        }
                    }
                    else -> {
                        _errorMessage.value = "No account found with this email. Please register first."
                        println("❌ No account found for: $emailValue")
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Login failed: ${e.message}"
                println("❌ Login error: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    
    fun clearError() {
        _errorMessage.value = ""
    }
    
    fun resetLoginState() {
        _loginSuccess.value = false
        _userRole.value = null
        _errorMessage.value = ""
    }
}
