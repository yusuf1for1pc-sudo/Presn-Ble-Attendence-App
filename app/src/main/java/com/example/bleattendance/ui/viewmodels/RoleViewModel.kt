package com.example.bleattendance.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bleattendance.AttendanceApp
import com.example.bleattendance.data.repository.UserPreferencesRepository
import com.example.bleattendance.model.UserRole
import kotlinx.coroutines.launch

// This ViewModel connects to the UserPreferencesRepository to save the role.
class RoleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: UserPreferencesRepository = (application as AttendanceApp).userPreferencesRepository

    // This function is called when the user taps a button on the RoleSelectionScreen.
    fun saveRole(role: UserRole) {
        // We launch a coroutine because saving to DataStore is an asynchronous operation.
        viewModelScope.launch {
            repository.saveUserRole(role)
        }
    }
}