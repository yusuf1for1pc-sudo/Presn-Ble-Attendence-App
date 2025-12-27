package com.example.bleattendance.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bleattendance.AttendanceApp
import com.example.bleattendance.data.db.AssignmentEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class TeacherAssignmentDashboardViewModel(application: Application) : AndroidViewModel(application) {
    
    private val supabaseRepository = (application as AttendanceApp).supabaseRepository
    
    private val _assignments = MutableStateFlow<List<AssignmentEntity>>(emptyList())
    val assignments: StateFlow<List<AssignmentEntity>> = _assignments.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun loadAssignments(teacherEmail: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                println("🔄 Loading assignments for teacher: $teacherEmail")
                
                // Get assignments created by this teacher from local database (fast)
                val assignments = supabaseRepository.getAssignmentsByTeacher(teacherEmail).first()
                _assignments.value = assignments
                println("✅ Loaded ${assignments.size} assignments from local database")
                
                // If no assignments found locally, try to sync from Supabase
                if (assignments.isEmpty()) {
                    println("🔄 No local assignments found, syncing from Supabase...")
                    
                    // First, check if teacher has any classes - if not, create test classes
                    val teacherClasses = supabaseRepository.getClassesForTeacher(teacherEmail).first()
                    if (teacherClasses.isEmpty()) {
                        println("⚠️ Teacher has no classes, creating test classes first...")
                        val createClassesResult = supabaseRepository.createTestClassesForTeacher(teacherEmail)
                        if (createClassesResult.isSuccess) {
                            println("✅ Test classes created successfully")
                        } else {
                            println("❌ Failed to create test classes: ${createClassesResult.exceptionOrNull()?.message}")
                        }
                    }
                    
                    val syncResult = supabaseRepository.syncAssignmentsForCurrentTeacher(teacherEmail)
                    if (syncResult.isSuccess) {
                        println("✅ Assignments synced successfully")
                        
                        // Reload from local database after sync
                        val syncedAssignments = supabaseRepository.getAssignmentsByTeacher(teacherEmail).first()
                        _assignments.value = syncedAssignments
                        println("✅ Reloaded ${syncedAssignments.size} assignments after sync")
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
    
    fun syncAssignmentsFromSupabase(teacherEmail: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                println("🔄 Manual sync - syncing assignments from Supabase...")
                val syncResult = supabaseRepository.syncAssignmentsForCurrentTeacher(teacherEmail)
                if (syncResult.isSuccess) {
                    println("✅ Assignments synced successfully")
                    
                    // Reload from local database after sync
                    val assignments = supabaseRepository.getAssignmentsByTeacher(teacherEmail).first()
                    _assignments.value = assignments
                    println("✅ Reloaded ${assignments.size} assignments after sync")
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
