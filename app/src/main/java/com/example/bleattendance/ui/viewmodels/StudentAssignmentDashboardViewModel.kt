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
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class StudentAssignmentDashboardViewModel(application: Application) : AndroidViewModel(application) {
    
    private val supabaseRepository = (application as AttendanceApp).supabaseRepository
    
    private val _assignments = MutableStateFlow<List<AssignmentEntity>>(emptyList())
    val assignments: StateFlow<List<AssignmentEntity>> = _assignments.asStateFlow()
    
    private val _submissions = MutableStateFlow<List<SubmissionEntity>>(emptyList())
    val submissions: StateFlow<List<SubmissionEntity>> = _submissions.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun loadAssignments(studentEmail: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                println("🔄 Loading assignments for student: $studentEmail")
                
                // Load from local database first (fast)
                val assignmentsFlow = supabaseRepository.getAssignmentsForStudent(studentEmail)
                assignmentsFlow.collect { assignments ->
                    println("🔍 DEBUG: Found ${assignments.size} assignments in local database")
                    assignments.forEach { assignment ->
                        println("   - Assignment: ${assignment.title} (Class ID: ${assignment.classId})")
                    }
                    _assignments.value = assignments
                }
                
                // Get student's submissions from local database
                val submissionsFlow = supabaseRepository.getSubmissionsByStudent(studentEmail)
                submissionsFlow.collect { submissions ->
                    _submissions.value = submissions
                }
                
                println("✅ Loaded ${_assignments.value.size} assignments and ${_submissions.value.size} submissions from local database")
                
                // If no assignments found locally, try to sync from Supabase
                if (_assignments.value.isEmpty()) {
                    println("🔄 No local assignments found, syncing from Supabase...")
                    val syncResult = supabaseRepository.syncAssignmentsForCurrentStudent(studentEmail)
                    if (syncResult.isSuccess) {
                        println("✅ Assignments synced successfully")
                        
                        // Reload from local database after sync
                        val assignmentsFlowAfterSync = supabaseRepository.getAssignmentsForStudent(studentEmail)
                        assignmentsFlowAfterSync.collect { assignments ->
                            _assignments.value = assignments
                        }
                        
                        val submissionsFlowAfterSync = supabaseRepository.getSubmissionsByStudent(studentEmail)
                        submissionsFlowAfterSync.collect { submissions ->
                            _submissions.value = submissions
                        }
                        
                        println("✅ Reloaded ${_assignments.value.size} assignments and ${_submissions.value.size} submissions after sync")
                    } else {
                        println("⚠️ Assignment sync failed: ${syncResult.exceptionOrNull()?.message}")
                    }
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "Error loading assignments: ${e.message}"
                println("❌ Error loading assignments: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun syncAssignmentsFromSupabase(studentEmail: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                println("🔄 Manual sync - syncing assignments from Supabase...")
                val syncResult = supabaseRepository.syncAssignmentsForCurrentStudent(studentEmail)
                if (syncResult.isSuccess) {
                    println("✅ Assignments synced successfully")
                    
                    // Reload from local database after sync
                    val assignmentsFlow = supabaseRepository.getAssignmentsForStudent(studentEmail)
                    assignmentsFlow.collect { assignments ->
                        _assignments.value = assignments
                    }
                    
                    val submissionsFlow = supabaseRepository.getSubmissionsByStudent(studentEmail)
                    submissionsFlow.collect { submissions ->
                        _submissions.value = submissions
                    }
                } else {
                    println("⚠️ Assignment sync failed: ${syncResult.exceptionOrNull()?.message}")
                    _errorMessage.value = "Sync failed: ${syncResult.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error syncing assignments: ${e.message}"
                println("❌ Error syncing assignments: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
