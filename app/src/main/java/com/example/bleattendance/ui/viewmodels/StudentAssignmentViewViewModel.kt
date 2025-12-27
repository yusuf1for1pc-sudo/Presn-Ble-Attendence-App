package com.example.bleattendance.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bleattendance.AttendanceApp
import com.example.bleattendance.data.db.AssignmentEntity
import com.example.bleattendance.data.db.SubmissionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeout

class StudentAssignmentViewViewModel(application: Application) : AndroidViewModel(application) {
    
    private val supabaseRepository = (application as AttendanceApp).supabaseRepository
    
    private val _assignment = MutableStateFlow<AssignmentEntity?>(null)
    val assignment: StateFlow<AssignmentEntity?> = _assignment.asStateFlow()
    
    private val _submission = MutableStateFlow<SubmissionEntity?>(null)
    val submission: StateFlow<SubmissionEntity?> = _submission.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    fun loadAssignment(assignmentId: String, studentEmail: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                println("🔄 Loading assignment: $assignmentId for student: $studentEmail")
                
                // Load assignment details
                val assignmentFlow = supabaseRepository.getAssignmentById(assignmentId)
                val assignment = withTimeout(5000) {
                    assignmentFlow.first()
                }
                _assignment.value = assignment
                
                // Load student's submission for this assignment
                val submission = supabaseRepository.getSubmissionByStudent(assignmentId, studentEmail)
                _submission.value = submission
                
                println("✅ Loaded assignment and submission")
            } catch (e: Exception) {
                _errorMessage.value = "Error loading assignment: ${e.message}"
                println("❌ Error loading assignment: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun submitAssignment(
        assignmentId: String,
        studentId: String,
        submissionText: String? = null,
        attachmentUrls: List<String>? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _successMessage.value = null
                
                println("🔄 Submitting professional assignment: $assignmentId for student: $studentId")
                
                val result = supabaseRepository.submitAssignment(
                    assignmentId = assignmentId,
                    studentId = studentId,
                    submissionText = submissionText,
                    attachmentUrls = attachmentUrls
                )
                
                if (result.isSuccess) {
                    _successMessage.value = "Assignment submitted successfully!"
                    // Reload submission data
                    val submission = supabaseRepository.getSubmissionByStudent(assignmentId, studentId)
                    _submission.value = submission
                    println("✅ Professional assignment submitted successfully")
                } else {
                    _errorMessage.value = "Failed to submit assignment: ${result.exceptionOrNull()?.message}"
                    println("❌ Failed to submit assignment: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error submitting assignment: ${e.message}"
                println("❌ Error submitting assignment: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun clearSuccess() {
        _successMessage.value = null
    }
}
