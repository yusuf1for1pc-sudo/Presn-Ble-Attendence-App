package com.example.bleattendance.data.adapter

import com.example.bleattendance.data.db.AttendanceSessionEntity
import com.example.bleattendance.data.db.ClassSessionEntity
import com.example.bleattendance.model.StudentInfo
import java.util.Date

/**
 * Adapter to convert between old and new data models
 * This allows the BLE service to continue working with the old model
 * while the rest of the app uses the new model
 */
object AttendanceDataAdapter {
    
    /**
     * Convert old AttendanceSessionEntity (with list of students) to new model
     * This creates individual attendance records for each student
     */
    fun convertOldToNew(
        oldSession: AttendanceSessionEntity,
        sessionId: Int,
        attendedStudents: List<StudentInfo>
    ): List<AttendanceSessionEntity> {
        return attendedStudents.map { student ->
            AttendanceSessionEntity(
                sessionId = sessionId,
                studentEmail = "", // Will need to be filled from student database
                attendanceStatus = "present", // Default to present since they were in attendedStudents
                markedAt = oldSession.markedAt,
                markedVia = "BLE"
            )
        }
    }
    
    /**
     * Convert new individual attendance records back to old format
     * This is useful for backward compatibility
     */
    fun convertNewToOld(
        newSessions: List<AttendanceSessionEntity>,
        classId: Int,
        sessionCode: String
    ): AttendanceSessionEntity {
        val students = newSessions.map { session ->
            StudentInfo(
                name = "", // Will need to be filled from student database
                rollNumber = "" // Will need to be filled from student database
            )
        }
        
        return AttendanceSessionEntity(
            sessionId = newSessions.firstOrNull()?.sessionId ?: 0,
            studentEmail = "", // Not used in old model
            attendanceStatus = "", // Not used in old model
            markedAt = newSessions.firstOrNull()?.markedAt ?: System.currentTimeMillis(),
            markedVia = "BLE"
        ).apply {
            // Add the attendedStudents list (this would need to be added to the entity)
            // attendedStudents = students
        }
    }
    
    /**
     * Create a ClassSessionEntity from old session data
     */
    fun createClassSession(
        classId: Int,
        sessionCode: String,
        timestamp: Long
    ): ClassSessionEntity {
        val date = Date(timestamp)
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        
        return ClassSessionEntity(
            classId = classId,
            scheduledDate = dateFormat.format(date),
            startTime = timeFormat.format(date),
            endTime = timeFormat.format(Date(timestamp + 60 * 60 * 1000)), // +1 hour
            sessionCode = sessionCode,
            status = "completed"
        )
    }
}
