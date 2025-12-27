// In model/StudentInfo.kt
package com.example.bleattendance.model

// A simple data class to hold student details for attendance lists
data class StudentInfo(
    val name: String,
    val rollNumber: String
)

// Enum to represent the user's role
enum class UserRole {
    TEACHER, STUDENT, UNKNOWN
}
