// In data/repository/UserPreferencesRepository.kt

package com.example.bleattendance.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.bleattendance.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val USER_ROLE_KEY = stringPreferencesKey("user_role")
        val USER_EMAIL_KEY = stringPreferencesKey("user_email")
    }

    suspend fun saveUserRole(role: UserRole) {
        dataStore.edit { preferences ->
            preferences[USER_ROLE_KEY] = role.name
        }
    }
    
    suspend fun saveUserEmail(email: String) {
        dataStore.edit { preferences ->
            preferences[USER_EMAIL_KEY] = email
        }
    }
    
    suspend fun getUserEmail(): String? {
        return dataStore.data.map { preferences ->
            preferences[USER_EMAIL_KEY]
        }.first()
    }
    
    suspend fun getUserRole(): UserRole? {
        return dataStore.data.map { preferences ->
            val roleString = preferences[USER_ROLE_KEY]
            if (roleString != null) {
                try {
                    UserRole.valueOf(roleString)
                } catch (e: Exception) {
                    UserRole.UNKNOWN
                }
            } else {
                null
            }
        }.first()
    }

    val userRole: Flow<UserRole> = dataStore.data.map { preferences ->
        UserRole.valueOf(preferences[USER_ROLE_KEY] ?: UserRole.UNKNOWN.name)
    }

    // ✅ ADD THIS FUNCTION TO LOG OUT
    suspend fun clearUserRole() {
        dataStore.edit { preferences ->
            preferences.remove(USER_ROLE_KEY)
        }
    }
    
    // ✅ ADD COMPREHENSIVE CLEAR FUNCTION
    suspend fun clearAllPreferences() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    // ✅ ADD CLEAR EMAIL FUNCTION
    suspend fun clearUserEmail() {
        dataStore.edit { preferences ->
            preferences.remove(USER_EMAIL_KEY)
        }
    }
}
