// In data/db/entities.kt

package com.example.bleattendance.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.bleattendance.model.StudentInfo

// Academic Configuration Entity
@Entity(tableName = "academic_config")
data class AcademicConfigEntity(
    @PrimaryKey(autoGenerate = true) val configId: Int = 0,
    val programName: String, // "Engineering", "MBA", "Diploma", "School"
    val durationYears: Int, // 4, 2, 3, 12
    val totalSemesters: Int, // 8, 4, 6, 12
    val semesterDurationMonths: Int, // 6, 6, 6, 12
    val isActive: Boolean = true
)

// Student Groups Entity - Enterprise Grade
@Entity(
    tableName = "student_groups",
    indices = [
        Index(value = ["branch", "admissionYear", "division", "batch", "semester"], unique = true),
        Index("branch"),
        Index("admissionYear"),
        Index("semester")
    ]
)
data class StudentGroupEntity(
    @PrimaryKey val groupId: String, // "CSE_2024-2028_A_1_Sem3"
    val branch: String,
    val admissionYear: Int,
    val graduationYear: Int,
    val division: String,
    val batch: Int,
    val semester: Int,
    val academicConfigId: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

// Teacher profile - Enterprise Grade
@Entity(
    tableName = "teacher_profile",
    indices = [
        Index("name"),
        Index("subject"),
        Index("isActive")
    ]
)
data class TeacherEntity(
    @PrimaryKey val email: String,
    val name: String,
    val subject: String, // The main subject they teach
    val password: String, // Hashed password
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val lastLoginAt: Long? = null
)

// Enhanced Student profile with group assignment
@Entity(
    tableName = "student_profile",
    indices = [Index("groupId")]
)
data class StudentEntity(
    @PrimaryKey val email: String,
    val name: String,
    val rollNumber: String,
    val branch: String,
    val admissionYear: Int,
    val graduationYear: Int,
    val division: String,
    val batch: Int,
    val currentSemester: Int, // Auto-calculated
    val groupId: String?, // Foreign key to StudentGroupEntity
    val password: String // Hashed password
)

// Enhanced Classes Entity linked to groups
@Entity(
    tableName = "classes",
    foreignKeys = [
        ForeignKey(
            entity = TeacherEntity::class,
            parentColumns = ["email"],
            childColumns = ["teacherEmail"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StudentGroupEntity::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("teacherEmail"), 
        Index("groupId"),
        Index(value = ["teacherEmail", "subjectName", "groupId"], unique = true)
    ]
)
data class ClassEntity(
    @PrimaryKey val classId: Int, // Changed from autoGenerate = true to allow custom IDs
    val teacherEmail: String,
    val subjectName: String,
    val groupId: String, // Foreign key to StudentGroupEntity
    val classSchedule: String? = null, // JSON string for recurring schedule
    val isActive: Boolean = true
)

// Class Sessions Entity for individual class instances
@Entity(
    tableName = "class_sessions",
    foreignKeys = [ForeignKey(
        entity = ClassEntity::class,
        parentColumns = ["classId"],
        childColumns = ["classId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("classId"), Index("scheduledDate")]
)
data class ClassSessionEntity(
    @PrimaryKey(autoGenerate = true) val sessionId: Int = 0,
    val classId: Int, // Foreign key to ClassEntity
    val scheduledDate: String, // Date in YYYY-MM-DD format
    val startTime: String, // Time in HH:MM format
    val endTime: String, // Time in HH:MM format
    val sessionCode: String, // Unique code for this session
    val status: String = "scheduled" // 'scheduled', 'ongoing', 'completed', 'cancelled'
)

// Enhanced Attendance Sessions Entity
@Entity(
    tableName = "attendance_sessions",
    foreignKeys = [
        ForeignKey(
            entity = ClassSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["email"],
            childColumns = ["studentEmail"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("studentEmail")],
    primaryKeys = ["sessionId", "studentEmail"]
)
data class AttendanceSessionEntity(
    val sessionId: Int, // Foreign key to ClassSessionEntity
    val studentEmail: String, // Foreign key to StudentEntity
    val attendanceStatus: String, // 'present', 'absent', 'late'
    val markedAt: Long, // Timestamp when marked
    val markedVia: String? = null // 'BLE', 'QR', 'manual'
)

// Assignment Entities - Reusing existing ClassEntity and TeacherEntity relationships
@Entity(
    tableName = "assignments",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["classId"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TeacherEntity::class,
            parentColumns = ["email"],
            childColumns = ["teacherId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("teacherId"), Index("dueDate"), Index("classId")]
)
data class AssignmentEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val instructions: String? = null,
    val assignmentType: String = "homework", // homework, project, quiz, exam
    val maxPoints: Int = 100,
    val dueDate: Long, // Timestamp
    val allowLateSubmission: Boolean = false,
    val latePenaltyPercentage: Int = 0,
    val attachmentUrls: String? = null, // JSON string for multiple files
    val teacherId: String, // Teacher email
    val classId: Int, // Class ID
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val totalSubmissions: Int = 0,
    val gradedSubmissions: Int = 0
)

@Entity(
    tableName = "submissions",
    foreignKeys = [
        ForeignKey(
            entity = AssignmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["assignmentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["email"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("assignmentId"), Index("studentId")]
)
data class SubmissionEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val assignmentId: String, // Foreign key to AssignmentEntity
    val studentId: String, // Student email
    val submissionText: String? = null, // Text submission
    val attachmentUrls: String? = null, // JSON string for multiple files
    val submittedAt: Long = System.currentTimeMillis(),
    val isLate: Boolean = false,
    val pointsEarned: Int? = null,
    val maxPoints: Int? = null,
    val grade: String? = null, // A+, A, B+, B, C+, C, D, F
    val feedback: String? = null,
    val gradedBy: String? = null, // Teacher email
    val gradedAt: Long? = null,
    val status: String = "submitted", // submitted, graded, returned
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
