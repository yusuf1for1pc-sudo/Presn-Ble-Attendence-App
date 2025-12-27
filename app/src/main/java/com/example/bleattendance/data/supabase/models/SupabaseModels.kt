package com.example.bleattendance.data.supabase.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

// Professional Supabase models that match the actual database tables

@Serializable
data class SupabaseStudent(
    val idx: Int? = null, // Row index from Supabase
    val id: String? = null, // UUID
    val email: String,
    val name: String,
    val roll_number: String,
    val branch: String,
    val admission_year: Int,
    val graduation_year: Int,
    val division: String,
    val batch: Int,
    val current_semester: Int,
    val group_id: String? = null, // Group ID string (e.g., "IT_2024-2028_A_2_Sem3")
    val password: String, // Hashed password
    val created_at: String? = null,
    val updated_at: String? = null,
    val is_active: Boolean = true,
    val last_login_at: String? = null
)

@Serializable
data class SupabaseTeacher(
    val idx: Int? = null, // Row index from Supabase
    val id: String? = null, // UUID
    val email: String,
    val name: String,
    val subject: String,
    val department: String? = null,
    val password: String, // Hashed password
    val created_at: String? = null,
    val updated_at: String? = null,
    val is_active: Boolean = true,
    val last_login_at: String? = null
)

@Serializable
data class SupabaseStudentGroup(
    val idx: Int? = null, // Row index from Supabase
    val id: String? = null, // UUID
    val group_id: String,
    val group_name: String,
    val branch: String? = null,
    val admission_year: Int,
    val graduation_year: Int,
    val division: String? = null,
    val batch: Int? = null,
    val semester: Int? = null,
    val academic_config_id: Int? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val is_active: Boolean = true
)

@Serializable
data class SupabaseClass(
    val idx: Int? = null, // Row index from Supabase
    val id: String? = null, // UUID
    val class_name: String,
    val subject: String,
    val teacher_id: String, // UUID reference to teacher_profile
    val group_id: String, // TEXT reference to student_groups(group_id)
    val semester: Int,
    val class_days: String? = null, // Days of the week (e.g., "Monday", "Mon, Wed, Fri")
    val start_time: String? = null, // Start time (e.g., "09:00")
    val end_time: String? = null, // End time (e.g., "10:30")
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class SupabaseClassEnrollment(
    val id: String? = null, // UUID
    val class_id: String, // UUID reference to classes
    val student_id: String, // UUID reference to students
    val enrolled_at: String? = null,
    val is_active: Boolean = true
)

@Serializable
data class SupabaseAssignment(
    val id: String? = null, // UUID
    val class_id: String, // UUID reference to classes
    val teacher_id: String, // UUID reference to teachers
    val title: String,
    val description: String,
    val instructions: String? = null,
    val assignment_type: String = "homework", // homework, project, quiz, exam
    val max_points: Int = 100,
    val due_date: String, // TIMESTAMP WITH TIME ZONE
    val allow_late_submission: Boolean = false,
    val late_penalty_percentage: Int = 0,
    val attachment_urls: List<String>? = null, // TEXT[] array
    val created_at: String? = null,
    val updated_at: String? = null,
    val is_active: Boolean = true,
    val total_submissions: Int = 0,
    val graded_submissions: Int = 0
)

@Serializable
data class SupabaseAssignmentSubmission(
    val id: String? = null, // UUID
    val assignment_id: String, // UUID reference to assignments
    val student_id: String, // UUID reference to students
    val submission_text: String? = null,
    val attachment_urls: List<String>? = null, // TEXT[] array
    val submitted_at: String? = null,
    val is_late: Boolean = false,
    val points_earned: Int? = null,
    val max_points: Int? = null,
    val grade: String? = null, // A+, A, B+, B, C+, C, D, F
    val feedback: String? = null,
    val graded_by: String? = null, // UUID reference to teachers
    val graded_at: String? = null,
    val status: String = "submitted", // submitted, graded, returned
    val created_at: String? = null,
    val updated_at: String? = null
)

// Request Models for API calls
@Serializable
data class CreateAssignmentRequest(
    val class_id: String, // UUID
    val title: String,
    val description: String,
    val instructions: String? = null,
    val assignment_type: String = "homework",
    val max_points: Int = 100,
    val due_date: String,
    val allow_late_submission: Boolean = false,
    val late_penalty_percentage: Int = 0,
    val attachment_urls: List<String>? = null // TEXT[] array
)

@Serializable
data class SubmitAssignmentRequest(
    val assignment_id: String, // UUID
    val submission_text: String? = null,
    val attachment_urls: List<String>? = null // TEXT[] array
)

@Serializable
data class GradeAssignmentRequest(
    val submission_id: String, // UUID
    val points_earned: Int,
    val max_points: Int,
    val grade: String,
    val feedback: String? = null
)

// Response Models
@Serializable
data class AssignmentWithSubmissions(
    val assignment: SupabaseAssignment,
    val submissions: List<SupabaseAssignmentSubmission>,
    val total_students: Int,
    val submitted_count: Int,
    val graded_count: Int
)

@Serializable
data class StudentSubmissionWithAssignment(
    val submission: SupabaseAssignmentSubmission,
    val assignment: SupabaseAssignment,
    val class_info: SupabaseClass
)

// Legacy models for backward compatibility (if needed)

@Serializable
data class SupabaseClassSession(
    val id: String? = null, // UUID from database
    val session_id: Int? = null, // Legacy field for compatibility
    val class_id: String, // UUID from database
    val session_date: String, // Actual database field name (required)
    val start_time: String, // Required
    val end_time: String, // Required
    val topic: String? = null,
    val notes: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    // Legacy fields for compatibility
    val scheduled_date: String? = null, // Alternative field name
    val session_code: String? = null, // Not in database, but kept for compatibility
    val status: String? = null // Added to handle JSON response with status field
)

@Serializable
data class CreateClassRequest(
    val class_name: String,
    val subject: String,
    val teacher_id: String,
    val group_id: String,
    val semester: Int,
    val class_days: String? = null, // Days of the week (e.g., "Monday", "Mon, Wed, Fri")
    val start_time: String? = null, // Start time (e.g., "09:00")
    val end_time: String? = null // End time (e.g., "10:30")
)

@Serializable
data class CreateStudentGroupRequest(
    val group_id: String, // Added missing group_id field
    val group_name: String,
    val branch: String,
    val division: String,
    val batch: Int,
    val semester: Int,
    val admission_year: Int, // Added missing admission_year field
    val graduation_year: Int // Added missing graduation_year field
)

@Serializable
data class CreateTeacherRequest(
    val email: String,
    val name: String,
    val subject: String,
    val password: String
)

@Serializable
data class CreateStudentRequest(
    val email: String,
    val name: String,
    val roll_number: String,
    val branch: String,
    val admission_year: Int,
    val graduation_year: Int,
    val division: String,
    val batch: Int,
    val current_semester: Int,
    val group_id: String? = null, // Group ID string (e.g., "IT_2024-2028_A_2_Sem3")
    val password: String
)

@Serializable
data class CreateSubmissionRequest(
    val assignment_id: String,
    val student_email: String,
    val file_url: String? = null
)

// Missing models for legacy compatibility
@Serializable
data class CreateClassSessionRequest(
    val class_id: Int,
    val scheduled_date: String,
    val start_time: String,
    val end_time: String,
    val session_code: String
)

@Serializable
data class CreateAttendanceSessionRequest(
    val class_id: Int,
    val session_code: String,
    val scheduled_date: String,
    val start_time: String,
    val end_time: String
)

@Serializable
data class CreateAttendanceRecordRequest(
    val session_id: Int,
    val student_email: String,
    val attendance_status: String,
    val marked_via: String? = null,
    val marked_at: String? = null
)

@Serializable
data class CreateAcademicConfigRequest(
    val program_name: String,
    val duration_years: Int,
    val total_semesters: Int,
    val semester_duration_months: Int
)

@Serializable
data class SupabaseAttendanceSession(
    val session_id: Int? = null,
    val class_id: String, // Changed from Int to String to match UUID from database
    val session_code: String,
    val scheduled_date: String,
    val start_time: String,
    val end_time: String,
    val status: String = "scheduled",
    val created_at: String? = null
)

@Serializable
data class SupabaseSubmission(
    val id: String? = null,
    val assignment_id: String,
    val student_email: String,
    val file_url: String? = null,
    val submitted_at: String? = null,
    val status: String = "submitted"
)

// Additional missing models for attendance and other features
@Serializable
data class SupabaseAttendanceRecord(
    val id: String? = null,
    val session_id: Int? = null,
    val student_email: String,
    val attendance_status: String,
    val marked_via: String,
    val marked_at: String? = null,
    val class_id: String, // Changed from Int to String to match UUID from database
    val session_code: String,
    val scheduled_date: String,
    val start_time: String,
    val end_time: String,
    val status: String? = null // Added missing status field that appears in JSON response
)


@Serializable
data class SupabaseAcademicConfig(
    val id: String? = null,
    val program_name: String,
    val duration_years: Int,
    val total_semesters: Int,
    val semester_duration_months: Int,
    val is_active: Boolean = true
)