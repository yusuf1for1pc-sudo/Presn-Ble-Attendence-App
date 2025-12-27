package com.example.bleattendance.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bleattendance.AttendanceApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class TeacherAssignmentCreationViewModel(application: Application) : AndroidViewModel(application) {
    
    private val supabaseRepository = (application as AttendanceApp).supabaseRepository
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    fun createAssignment(
        classId: Int,
        title: String,
        description: String,
        instructions: String? = null,
        assignmentType: String = "homework",
        maxPoints: Int = 100,
        dueDate: Long,
        allowLateSubmission: Boolean = false,
        latePenaltyPercentage: Int = 0,
        attachmentUrls: List<String>? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _successMessage.value = null
                
                println("🔄 Creating professional assignment: $title")
                
                val result = supabaseRepository.createAssignment(
                    classId = classId,
                    title = title,
                    description = description,
                    instructions = instructions,
                    assignmentType = assignmentType,
                    maxPoints = maxPoints,
                    dueDate = dueDate,
                    allowLateSubmission = allowLateSubmission,
                    latePenaltyPercentage = latePenaltyPercentage,
                    attachmentUrls = attachmentUrls
                )
                
                if (result.isSuccess) {
                    _successMessage.value = "Professional assignment posted successfully!"
                    println("✅ Professional assignment created successfully")
                } else {
                    _errorMessage.value = "Failed to create assignment: ${result.exceptionOrNull()?.message}"
                    println("❌ Failed to create assignment: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error creating assignment: ${e.message}"
                println("❌ Error creating assignment: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun setError(message: String) {
        _errorMessage.value = message
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun clearSuccess() {
        _successMessage.value = null
    }
}
