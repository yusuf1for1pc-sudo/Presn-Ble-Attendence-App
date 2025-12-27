// In AttendanceApp.kt (at the root of your package)
package com.example.bleattendance

import android.app.Application
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.example.bleattendance.data.db.AppDatabase
import com.example.bleattendance.data.repository.UserPreferencesRepository
import com.example.bleattendance.data.repository.SupabaseRepository
import com.example.bleattendance.data.supabase.SupabaseApiService

class AttendanceApp : Application() {
    // Lazily initialize the database and repositories so they are only created when needed
    val database by lazy { AppDatabase.getDatabase(this) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }
    
    // Supabase integration
    val supabaseApiService by lazy { SupabaseApiService() }
    val supabaseRepository by lazy { SupabaseRepository(database, supabaseApiService) }
    
    // Coroutine scope for background operations
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        // Auto-sync data when app starts
        autoSyncData()
    }
    
    private fun autoSyncData() {
        applicationScope.launch {
            try {
                println("🚀 Auto-syncing data on app startup...")
                
                // Check if user is logged in
                val userRole = userPreferencesRepository.getUserRole()
                if (userRole != null) {
                    println("📱 User is logged in as: $userRole")
                    
                    when (userRole) {
                        com.example.bleattendance.model.UserRole.TEACHER -> {
                            // Sync teacher-specific data
                            val teacherEmail = userPreferencesRepository.getUserEmail()
                            if (teacherEmail != null) {
                                println("🔄 Auto-syncing teacher data for: $teacherEmail")
                                val refreshResult = supabaseRepository.refreshCurrentTeacherData()
                                if (refreshResult.isSuccess) {
                                    println("✅ Teacher auto-sync completed successfully")
                                } else {
                                    println("⚠️ Teacher auto-sync had issues: ${refreshResult.exceptionOrNull()?.message}")
                                }
                            }
                        }
                        com.example.bleattendance.model.UserRole.STUDENT -> {
                            // Sync student-specific data
                            val studentEmail = userPreferencesRepository.getUserEmail()
                            if (studentEmail != null) {
                                println("🔄 Auto-syncing student data for: $studentEmail")
                                val refreshResult = supabaseRepository.refreshCurrentStudentData()
                                if (refreshResult.isSuccess) {
                                    println("✅ Student auto-sync completed successfully")
                                } else {
                                    println("⚠️ Student auto-sync had issues: ${refreshResult.exceptionOrNull()?.message}")
                                }
                            }
                        }
                        com.example.bleattendance.model.UserRole.UNKNOWN -> {
                            println("📱 Unknown user role, skipping auto-sync")
                        }
                    }
                } else {
                    println("📱 No user logged in, skipping auto-sync")
                }
            } catch (e: Exception) {
                println("❌ Auto-sync failed: ${e.message}")
                // Don't crash the app if sync fails
            }
        }
    }
    
    // Public method to manually trigger sync (for testing)
    fun triggerManualSync() {
        autoSyncData()
    }
}
