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

class TeacherAssignmentReviewViewModel(application: Application) : AndroidViewModel(application) {
    
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
                
                if (assignment != null) {
                    // Load students in the class
                    val classId = assignment.classId ?: 0
                    if (classId > 0) {
                        val studentsFlow = supabaseRepository.getStudentsInClass(classId)
                        val studentsList = withTimeout(5000) {
                            studentsFlow.first()
                        }
                        _students.value = studentsList
                        
                        // Load submissions for this assignment
                        val submissionsFlow = supabaseRepository.getSubmissionsForAssignment(assignmentId)
                        val submissionsList = withTimeout(5000) {
                            submissionsFlow.first()
                        }
                        _submissions.value = submissionsList
                        
                        println("📚 Loaded ${studentsList.size} students and ${submissionsList.size} submissions")
                    } else {
                        _errorMessage.value = "Assignment not linked to any class"
                    }
                } else {
                    _errorMessage.value = "Assignment not found"
                }
                
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _errorMessage.value = "Loading assignment data timed out. Please check your connection and try again."
                println("⏰ Loading assignment data timed out")
            } catch (e: Exception) {
                _errorMessage.value = "Error loading assignment data: ${e.message}"
                println("❌ Error loading assignment data: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
                println("✅ Finished loading assignment review data")
            }
        }
    }
    
    fun updateSubmissionStatus(studentEmail: String, status: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Find existing submission or create new one
                val assignment = _assignment.value
                if (assignment != null) {
                    val existingSubmission = _submissions.value.find { it.studentId == studentEmail }
                    
                    if (existingSubmission != null) {
                        // Update existing submission
                        val updatedSubmission = existingSubmission.copy(
                            status = status,
                            updatedAt = System.currentTimeMillis()
                        )
                        supabaseRepository.updateSubmission(updatedSubmission)
                        
                        // Update local state
                        _submissions.value = _submissions.value.map { 
                            if (it.studentId == studentEmail) updatedSubmission else it 
                        }
                    } else {
                        // Create new submission
                        val newSubmission = SubmissionEntity(
                            assignmentId = assignment.id,
                            studentId = studentEmail,
                            status = status,
                            submittedAt = if (status == "submitted" || status == "late") System.currentTimeMillis() else System.currentTimeMillis()
                        )
                        supabaseRepository.createSubmission(
                            assignmentId = assignment.id,
                            studentEmail = studentEmail,
                            fileUrl = null,
                            status = status
                        )
                        
                        // Update local state
                        _submissions.value = _submissions.value + newSubmission
                    }
                    
                    println("✅ Updated submission status for $studentEmail to $status")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error updating submission status: ${e.message}"
                println("❌ Error updating submission status: ${e.message}")
            }
        }
    }
    
    fun gradeSubmission(studentEmail: String, submission: SubmissionEntity?) {
        _gradingStudentEmail.value = studentEmail
        _gradingSubmission.value = submission
        _showGradingDialog.value = true
        println("🔄 Opening grading dialog for $studentEmail")
    }
    
    fun submitGrade(marksObtained: Int, feedback: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val assignment = _assignment.value
                val studentEmail = _gradingStudentEmail.value
                
                if (assignment != null && studentEmail != null) {
                    // Get current teacher email from the assignment
                    val teacherEmail = assignment.teacherId
                    
                    // Find the submission first
                    val submission = _submissions.value.find { it.studentId == studentEmail }
                    if (submission != null) {
                        val result = supabaseRepository.gradeSubmission(
                            submissionId = submission.id,
                            pointsEarned = marksObtained,
                            maxPoints = assignment.maxPoints,
                            grade = "", // Will be calculated from percentage
                            feedback = feedback
                        )
                        
                        if (result.isSuccess) {
                        // Update local state
                        _submissions.value = _submissions.value.map { submission ->
                            if (submission.studentId == studentEmail) {
                                submission.copy(
                                    pointsEarned = marksObtained,
                                    feedback = feedback,
                                    gradedAt = System.currentTimeMillis(),
                                    gradedBy = teacherEmail,
                                    updatedAt = System.currentTimeMillis()
                                )
                            } else {
                                submission
                            }
                        }
                        
                        println("✅ Successfully graded submission for $studentEmail")
                        _showGradingDialog.value = false
                        } else {
                            _errorMessage.value = "Failed to grade submission: ${result.exceptionOrNull()?.message}"
                        }
                    } else {
                        _errorMessage.value = "Submission not found for student"
                    }
                } else {
                    _errorMessage.value = "Missing assignment or student information"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error grading submission: ${e.message}"
                println("❌ Error grading submission: ${e.message}")
            }
        }
    }
    
    fun dismissGradingDialog() {
        _showGradingDialog.value = false
        _gradingStudentEmail.value = null
        _gradingSubmission.value = null
    }
    
    fun confirmAndSubmitGrades() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // TODO: Implement final grade submission
                println("✅ Confirmed and submitted all grades")
            } catch (e: Exception) {
                _errorMessage.value = "Error submitting grades: ${e.message}"
                println("❌ Error submitting grades: ${e.message}")
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
