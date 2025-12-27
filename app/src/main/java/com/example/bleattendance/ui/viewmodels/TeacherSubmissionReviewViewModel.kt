package com.example.bleattendance.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bleattendance.AttendanceApp
import com.example.bleattendance.data.db.AssignmentEntity
import com.example.bleattendance.data.db.StudentEntity
import com.example.bleattendance.data.db.SubmissionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeout

class TeacherSubmissionReviewViewModel(application: Application) : AndroidViewModel(application) {
    
    private val supabaseRepository = (application as AttendanceApp).supabaseRepository
    
    private val _assignment = MutableStateFlow<AssignmentEntity?>(null)
    val assignment: StateFlow<AssignmentEntity?> = _assignment.asStateFlow()
    
    private val _students = MutableStateFlow<List<StudentEntity>>(emptyList())
    val students: StateFlow<List<StudentEntity>> = _students.asStateFlow()
    
    private val _submissions = MutableStateFlow<List<SubmissionEntity>>(emptyList())
    val submissions: StateFlow<List<SubmissionEntity>> = _submissions.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Grading dialog state
    private val _showGradingDialog = MutableStateFlow(false)
    val showGradingDialog: StateFlow<Boolean> = _showGradingDialog.asStateFlow()
    
    private val _gradingStudentEmail = MutableStateFlow<String?>(null)
    val gradingStudentEmail: StateFlow<String?> = _gradingStudentEmail.asStateFlow()
    
    private val _gradingSubmission = MutableStateFlow<SubmissionEntity?>(null)
    val gradingSubmission: StateFlow<SubmissionEntity?> = _gradingSubmission.asStateFlow()
    
    fun loadAssignmentData(assignmentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                println("🔄 Loading assignment review data for ID: $assignmentId")
                
                // Load assignment details
                val assignmentFlow = supabaseRepository.getAssignmentById(assignmentId)
                val assignment = withTimeout(5000) {
                    assignmentFlow.first()
                }
                _assignment.value = assignment
                
                // Load all students (for General assignments, we might need to get all students)
                // For now, we'll get students from the class if assignment has a classId
                if (assignment?.classId != null) {
                    val studentsFlow = supabaseRepository.getStudentsInClass(assignment.classId!!)
                    val studentsList = withTimeout(5000) {
                        studentsFlow.first()
                    }
                    _students.value = studentsList
                } else {
                    // For General assignments, we might need a different approach
                    // For now, we'll use an empty list
                    _students.value = emptyList()
                }
                
                // Load submissions for this assignment
                val submissionsFlow = supabaseRepository.getSubmissionsForAssignment(assignmentId)
                val submissionsList = withTimeout(5000) {
                    submissionsFlow.first()
                }
                _submissions.value = submissionsList
                
                println("✅ Loaded assignment data: ${_students.value.size} students, ${_submissions.value.size} submissions")
            } catch (e: Exception) {
                _errorMessage.value = "Error loading assignment data: ${e.message}"
                println("❌ Error loading assignment data: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun gradeSubmission(studentEmail: String, submission: SubmissionEntity?) {
        _gradingStudentEmail.value = studentEmail
        _gradingSubmission.value = submission
        _showGradingDialog.value = true
        println("🔄 Opening grading dialog for $studentEmail")
    }
    
    fun submitGrade(
        submissionId: String,
        pointsEarned: Int,
        maxPoints: Int,
        grade: String,
        feedback: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                println("🔄 Grading professional submission: $submissionId")
                
                val result = supabaseRepository.gradeSubmission(
                    submissionId = submissionId,
                    pointsEarned = pointsEarned,
                    maxPoints = maxPoints,
                    grade = grade,
                    feedback = feedback
                )
                
                if (result.isSuccess) {
                    // Update local state
                    _submissions.value = _submissions.value.map { submission ->
                        if (submission.id == submissionId) {
                            submission.copy(
                                pointsEarned = pointsEarned,
                                maxPoints = maxPoints,
                                grade = grade,
                                feedback = feedback,
                                gradedAt = System.currentTimeMillis(),
                                status = "graded"
                            )
                        } else {
                            submission
                        }
                    }
                    
                    println("✅ Successfully graded professional submission")
                    _showGradingDialog.value = false
                } else {
                    _errorMessage.value = "Failed to grade submission: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error grading submission: ${e.message}"
                println("❌ Error grading submission: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun dismissGradingDialog() {
        _showGradingDialog.value = false
        _gradingStudentEmail.value = null
        _gradingSubmission.value = null
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
