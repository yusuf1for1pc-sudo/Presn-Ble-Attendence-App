package com.example.bleattendance.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bleattendance.AttendanceApp
import com.example.bleattendance.model.UserRole
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

// This ViewModel connects to the UserPreferencesRepository to read the saved role.
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferencesRepository = (application as AttendanceApp).userPreferencesRepository

    // This StateFlow will emit the saved UserRole.
    // The SplashScreen observes this to decide where to navigate.
    val userRole = userPreferencesRepository.userRole
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserRole.UNKNOWN // It starts as UNKNOWN while loading.
        )
}