// In data/db/dao.kt
// In data/db/dao.kt
package com.example.bleattendance.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Data class for student attendance information
data class StudentAttendanceInfo(
    val name: String,
    val rollNumber: String,
    val email: String,
    val attendanceStatus: String
)

@Dao
interface TeacherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: TeacherEntity)

    @Query("SELECT * FROM teacher_profile LIMIT 1")
    fun getTeacher(): Flow<TeacherEntity?>
    
    @Query("SELECT * FROM teacher_profile WHERE email = :email")
    fun getTeacherByEmail(email: String): Flow<TeacherEntity?>
    
    @Query("DELETE FROM teacher_profile")
    suspend fun deleteAllTeachers()
    
    // NEW: Get all classes for a specific group
    @Query("SELECT * FROM classes WHERE groupId = :groupId ORDER BY subjectName")
    fun getClassesByGroupId(groupId: String): Flow<List<ClassEntity>>
    
    // Get all classes for a specific teacher
    @Query("SELECT * FROM classes WHERE teacherEmail = :teacherEmail ORDER BY subjectName")
    fun getClassesForTeacher(teacherEmail: String): Flow<List<ClassEntity>>

    // ✅ ADD THIS FUNCTION
    @Query("DELETE FROM teacher_profile")
    suspend fun deleteTeacherProfile()
    
    @Query("DELETE FROM teacher_profile WHERE email = :oldEmail")
    suspend fun deleteTeacherByEmail(oldEmail: String)
    
    @Query("UPDATE teacher_profile SET name = :name, subject = :subject WHERE email = :email")
    suspend fun updateTeacherFields(email: String, name: String, subject: String)

    // NEW: Insert a new class
    @Insert
    suspend fun insertClass(classEntity: ClassEntity): Long

    // NEW: Delete a class by ID
    @Query("DELETE FROM classes WHERE classId = :classId")
    suspend fun deleteClassById(classId: Int)
    
    // NEW: Get all classes
    @Query("SELECT * FROM classes ORDER BY subjectName, groupId")
    fun getAllClasses(): Flow<List<ClassEntity>>
    
    // NEW: Get all teachers
    @Query("SELECT * FROM teacher_profile ORDER BY name")
    fun getAllTeachers(): Flow<List<TeacherEntity>>
    
    // NEW: Get class by ID
    @Query("SELECT * FROM classes WHERE classId = :classId")
    fun getClassById(classId: Int): Flow<ClassEntity?>
}

// StudentDao remains the same
@Dao
interface StudentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    // ✅ ADD THIS FUNCTION
    @Query("DELETE FROM student_profile")
    suspend fun deleteStudentProfile()

    @Query("SELECT * FROM student_profile ORDER BY rowid DESC LIMIT 1")
    fun getStudent(): Flow<StudentEntity?>
    
    @Query("SELECT * FROM student_profile WHERE groupId = :groupId")
    fun getStudentsByGroup(groupId: String): Flow<List<StudentEntity>>
    
    @Query("SELECT * FROM student_profile WHERE groupId IN (SELECT groupId FROM classes WHERE classId = :classId)")
    fun getStudentsByClass(classId: Int): Flow<List<StudentEntity>>
    
    @Query("SELECT * FROM student_groups WHERE groupId = :groupId")
    suspend fun getStudentGroupById(groupId: String): StudentGroupEntity?
    
    @Query("SELECT * FROM student_profile WHERE email = :email")
    fun getStudentByEmail(email: String): Flow<StudentEntity?>
    
    @Query("SELECT * FROM student_profile ORDER BY rowid DESC")
    fun getAllStudents(): Flow<List<StudentEntity>>
}

@Dao
interface AttendanceDao {
    @Insert
    suspend fun insertSession(session: AttendanceSessionEntity)

    // Get all sessions for a specific class
    @Query("SELECT * FROM attendance_sessions WHERE sessionId IN (SELECT sessionId FROM class_sessions WHERE classId = :classId) ORDER BY markedAt DESC")
    fun getSessionsForClass(classId: Int): Flow<List<AttendanceSessionEntity>>

    // Delete a specific session
    @Query("DELETE FROM attendance_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: Int)

    // Get total sessions count for a teacher's classes
    @Query("SELECT COUNT(DISTINCT sessionId) FROM attendance_sessions WHERE sessionId IN (SELECT sessionId FROM class_sessions WHERE classId IN (SELECT classId FROM classes WHERE teacherEmail = :teacherEmail))")
    suspend fun getTotalSessionsForTeacher(teacherEmail: String): Int

    // Get total students count for a teacher's classes
    @Query("SELECT COUNT(*) FROM attendance_sessions WHERE sessionId IN (SELECT sessionId FROM class_sessions WHERE classId IN (SELECT classId FROM classes WHERE teacherEmail = :teacherEmail))")
    suspend fun getTotalStudentsForTeacher(teacherEmail: String): Int

    // Get last session info for a teacher
    @Query("SELECT * FROM attendance_sessions WHERE sessionId IN (SELECT sessionId FROM class_sessions WHERE classId IN (SELECT classId FROM classes WHERE teacherEmail = :teacherEmail)) ORDER BY markedAt DESC LIMIT 1")
    suspend fun getLastSessionForTeacher(teacherEmail: String): AttendanceSessionEntity?
    
    @Query("SELECT * FROM attendance_sessions ORDER BY markedAt DESC")
    fun getAllAttendance(): Flow<List<AttendanceSessionEntity>>
    
    // Get students who attended a specific session
    @Query("""
        SELECT sp.name, sp.rollNumber, sp.email, att.attendanceStatus 
        FROM student_profile sp 
        INNER JOIN attendance_sessions att ON sp.email = att.studentEmail 
        WHERE att.sessionId = :sessionId 
        AND att.attendanceStatus IN ('PRESENT', 'ATTENDED')
        ORDER BY sp.rollNumber
    """)
    suspend fun getStudentsWhoAttendedSession(sessionId: Int): List<StudentAttendanceInfo>
}

// Academic Config DAO
@Dao
interface AcademicConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: AcademicConfigEntity)
    
    @Query("SELECT * FROM academic_config WHERE isActive = 1 LIMIT 1")
    fun getActiveConfig(): Flow<AcademicConfigEntity?>
    
    @Query("SELECT * FROM academic_config")
    fun getAllConfigs(): Flow<List<AcademicConfigEntity>>
}

// Student Groups DAO
@Dao
interface StudentGroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: StudentGroupEntity)
    
    @Query("SELECT * FROM student_groups WHERE groupId = :groupId")
    fun getGroupById(groupId: String): Flow<StudentGroupEntity?>
    
    @Query("DELETE FROM student_groups")
    suspend fun deleteAllStudentGroups()
    
    @Query("SELECT * FROM student_groups WHERE branch = :branch AND division = :division AND batch = :batch AND semester = :semester")
    fun getGroupByDetails(branch: String, division: String, batch: Int, semester: Int): Flow<StudentGroupEntity?>
    
    @Query("SELECT * FROM student_groups")
    fun getAllGroups(): Flow<List<StudentGroupEntity>>
}

// Class Sessions DAO
@Dao
interface ClassSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ClassSessionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<ClassSessionEntity>)
    
    @Query("SELECT * FROM class_sessions WHERE classId = :classId ORDER BY scheduledDate DESC")
    fun getSessionsForClass(classId: Int): Flow<List<ClassSessionEntity>>
    
    @Query("SELECT * FROM class_sessions WHERE sessionId = :sessionId")
    fun getSessionById(sessionId: Int): Flow<ClassSessionEntity?>
    
    @Query("SELECT * FROM class_sessions WHERE scheduledDate = :date AND status = 'scheduled'")
    fun getTodaysSessions(date: String): Flow<List<ClassSessionEntity>>
    
    @Update
    suspend fun updateSession(session: ClassSessionEntity)
    
    @Delete
    suspend fun deleteSession(session: ClassSessionEntity)
    
    @Query("DELETE FROM class_sessions")
    suspend fun deleteAllSessions()
}

// Assignment DAO - Reusing existing patterns from TeacherDao and StudentDao
@Dao
interface AssignmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: AssignmentEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignments(assignments: List<AssignmentEntity>)
    
    // Professional assignment queries
    @Query("SELECT * FROM assignments WHERE teacherId = :teacherEmail ORDER BY dueDate DESC")
    fun getAssignmentsByTeacher(teacherEmail: String): Flow<List<AssignmentEntity>>
    
    @Query("SELECT * FROM assignments WHERE classId = :classId ORDER BY dueDate DESC")
    fun getAssignmentsForClass(classId: Int): Flow<List<AssignmentEntity>>
    
    @Query("SELECT * FROM assignments WHERE classId = :classId AND isActive = 1 ORDER BY dueDate DESC")
    fun getActiveAssignmentsForClass(classId: Int): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments WHERE id = :assignmentId")
    fun getAssignmentById(assignmentId: String): Flow<AssignmentEntity?>
    
    // Get all assignments (similar to getAllClasses pattern)
    @Query("SELECT * FROM assignments ORDER BY dueDate ASC")
    fun getAllAssignments(): Flow<List<AssignmentEntity>>
    
    // Get assignments for multiple classes
    @Query("SELECT * FROM assignments WHERE classId IN (:classIds) ORDER BY dueDate ASC")
    fun getAssignmentsByClassIds(classIds: List<Int>): Flow<List<AssignmentEntity>>
    
    @Update
    suspend fun updateAssignment(assignment: AssignmentEntity)
    
    @Delete
    suspend fun deleteAssignment(assignment: AssignmentEntity)
    
    @Query("DELETE FROM assignments")
    suspend fun deleteAllAssignments()
}

// Submission DAO - Reusing existing patterns from AttendanceDao
@Dao
interface SubmissionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmission(submission: SubmissionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmissions(submissions: List<SubmissionEntity>)
    
    @Update
    suspend fun updateSubmission(submission: SubmissionEntity)
    
    // Professional submission queries
    @Query("SELECT * FROM submissions WHERE assignmentId = :assignmentId ORDER BY submittedAt DESC")
    fun getSubmissionsForAssignment(assignmentId: String): Flow<List<SubmissionEntity>>
    
    @Query("SELECT * FROM submissions WHERE studentId = :studentId ORDER BY submittedAt DESC")
    fun getSubmissionsByStudent(studentId: String): Flow<List<SubmissionEntity>>
    
    @Query("SELECT * FROM submissions WHERE assignmentId = :assignmentId AND studentId = :studentId")
    suspend fun getSubmissionByStudent(assignmentId: String, studentId: String): SubmissionEntity?
    
    @Query("SELECT * FROM submissions WHERE assignmentId = :assignmentId AND status = :status ORDER BY submittedAt DESC")
    fun getSubmissionsByStatus(assignmentId: String, status: String): Flow<List<SubmissionEntity>>
    
    @Query("SELECT COUNT(*) FROM submissions WHERE assignmentId = :assignmentId")
    suspend fun getSubmissionCount(assignmentId: String): Int
    
    @Delete
    suspend fun deleteSubmission(submission: SubmissionEntity)
    
    @Query("DELETE FROM submissions")
    suspend fun deleteAllSubmissions()
}