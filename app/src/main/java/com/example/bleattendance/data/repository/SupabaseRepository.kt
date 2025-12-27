package com.example.bleattendance.data.repository

import com.example.bleattendance.data.db.*
import com.example.bleattendance.data.supabase.SupabaseApiService
import com.example.bleattendance.data.supabase.models.*
import com.example.bleattendance.model.StudentInfo
import com.example.bleattendance.utils.GroupUtils
import com.example.bleattendance.utils.PasswordUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout

class SupabaseRepository(
    private val localDatabase: AppDatabase,
    private val supabaseApiService: SupabaseApiService
) {
    private val teacherDao = localDatabase.teacherDao()
    private val studentDao = localDatabase.studentDao()
    private val attendanceDao = localDatabase.attendanceDao()
    private val academicConfigDao = localDatabase.academicConfigDao()
    private val studentGroupDao = localDatabase.studentGroupDao()
    private val classSessionDao = localDatabase.classSessionDao()
    private val assignmentDao = localDatabase.assignmentDao()
    private val submissionDao = localDatabase.submissionDao()

    // Teacher operations with sync
    suspend fun createTeacher(name: String, email: String, subject: String, password: String): Result<Unit> {
        return try {
            println("🔍 Creating teacher locally: $email")
            
            // Clear any existing teacher data first to prevent conflicts
            println("🧹 Clearing existing teacher data...")
            teacherDao.deleteAllTeachers()
            
            // Also clear related data to ensure clean slate
            println("🧹 Clearing related data...")
            studentDao.deleteStudentProfile()
            studentGroupDao.deleteAllStudentGroups()
            classSessionDao.deleteAllSessions()
            assignmentDao.deleteAllAssignments()
            submissionDao.deleteAllSubmissions()
            
            // Hash the password
            val hashedPassword = com.example.bleattendance.utils.PasswordUtils.hashPassword(password)
            
            // Create locally first
            val teacherEntity = TeacherEntity(email, name, subject, hashedPassword)
            teacherDao.insertTeacher(teacherEntity)
            println("✅ Teacher created locally successfully")
            
            // Sync to Supabase
            println("🔄 Syncing teacher to Supabase...")
            val request = CreateTeacherRequest(email, name, subject, hashedPassword)
            val supabaseResult = supabaseApiService.createTeacher(request)
            if (supabaseResult.isSuccess) {
                println("✅ Teacher synced to Supabase successfully")
            } else {
                println("⚠️ Failed to sync teacher to Supabase: ${supabaseResult.exceptionOrNull()?.message}")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Failed to create teacher: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getTeacher(): Flow<TeacherEntity?> {
        return teacherDao.getTeacher()
    }
    
    suspend fun getStudentByEmail(email: String): Result<StudentEntity?> {
        return try {
            val student = studentDao.getStudentByEmail(email).first()
            Result.success(student)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTeacherByEmail(email: String): Result<TeacherEntity?> {
        return try {
            val teacher = teacherDao.getTeacherByEmail(email).first()
            Result.success(teacher)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncTeacherFromSupabase(email: String): Result<Unit> {
        return try {
            println("🔄 Syncing teacher data from Supabase: $email")
            val result = supabaseApiService.getTeacher(email)
            if (result.isSuccess) {
                val supabaseTeacher = result.getOrNull()
                if (supabaseTeacher != null) {
                    println("📝 Found teacher in Supabase: ${supabaseTeacher.name} (${supabaseTeacher.email})")
                    val teacherEntity = TeacherEntity(
                        email = supabaseTeacher.email,
                        name = supabaseTeacher.name,
                        subject = supabaseTeacher.subject,
                        password = supabaseTeacher.password
                    )
                    teacherDao.insertTeacher(teacherEntity)
                    println("✅ Teacher synced to local database: ${supabaseTeacher.name}")
                    Result.success(Unit)
                } else {
                    println("⚠️ Teacher not found in Supabase: $email")
                    Result.failure(Exception("Teacher not found in Supabase"))
                }
            } else {
                println("❌ Failed to get teacher from Supabase: ${result.exceptionOrNull()?.message}")
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            println("❌ Exception during teacher sync: ${e.message}")
            Result.failure(e)
        }
    }

    // Class operations with sync
    suspend fun createClass(
        teacherEmail: String,
        subjectName: String,
        groupId: String,
        classSchedule: String? = null
    ): Result<Int> {
        return try {
            // First, ensure the student group exists
            val groupExists = studentGroupDao.getGroupById(groupId).first() != null
            if (!groupExists) {
                println("🔍 Student group doesn't exist, creating it...")
                // Parse groupId to extract components (e.g., "IT_2025-2029_A_ALL_Sem1")
                val parts = groupId.split("_")
                if (parts.size >= 5) {
                    val branch = parts[0]
                    val yearRange = parts[1].split("-")
                    val admissionYear = yearRange[0].toIntOrNull() ?: 2025
                    val graduationYear = yearRange[1].toIntOrNull() ?: 2029
                    val division = parts[2]
                    // Handle "ALL" batch case (when batch was "None")
                    val batch = if (parts[3] == "ALL") 0 else parts[3].toIntOrNull() ?: 1
                    val semester = parts[4].replace("Sem", "").toIntOrNull() ?: 1
                    
                    println("🔍 Parsed groupId components:")
                    println("   - Branch: $branch")
                    println("   - Admission Year: $admissionYear")
                    println("   - Graduation Year: $graduationYear")
                    println("   - Division: $division")
                    println("   - Batch: $batch")
                    println("   - Semester: $semester")
                    
                    // Ensure default academic config exists and get its ID
                    var academicConfigId = 1 // Default fallback
                    val activeConfig = academicConfigDao.getActiveConfig().first()
                    if (activeConfig == null) {
                        println("🔍 Creating default academic config...")
                        val createConfigResult = createAcademicConfig(
                            programName = "Engineering",
                            durationYears = 4,
                            totalSemesters = 8,
                            semesterDurationMonths = 6
                        )
                        if (createConfigResult.isFailure) {
                            println("❌ Failed to create academic config: ${createConfigResult.exceptionOrNull()?.message}")
                            return Result.failure(createConfigResult.exceptionOrNull() ?: Exception("Failed to create academic config"))
                        }
                        println("✅ Default academic config created")
                        // Get the newly created config ID
                        val newConfig = academicConfigDao.getActiveConfig().first()
                        academicConfigId = newConfig?.configId ?: 1
                    } else {
                        academicConfigId = activeConfig.configId
                        println("✅ Using existing academic config: ${activeConfig.programName}")
                    }
                    
                    // Create the student group with the academic config
                    println("🔍 Creating student group with:")
                    println("   - groupId: $groupId")
                    println("   - branch: $branch")
                    println("   - admissionYear: $admissionYear")
                    println("   - graduationYear: $graduationYear")
                    println("   - division: $division")
                    println("   - batch: $batch")
                    println("   - semester: $semester")
                    println("   - academicConfigId: $academicConfigId")
                    
                    val createGroupResult = createStudentGroup(
                        groupId = groupId,
                        branch = branch,
                        admissionYear = admissionYear,
                        graduationYear = graduationYear,
                        division = division,
                        batch = batch,
                        semester = semester,
                        academicConfigId = academicConfigId
                    )
                    
                    if (createGroupResult.isFailure) {
                        println("❌ Failed to create student group: ${createGroupResult.exceptionOrNull()?.message}")
                        return Result.failure(createGroupResult.exceptionOrNull() ?: Exception("Failed to create student group"))
                    }
                    println("✅ Student group created successfully: $groupId")
                } else {
                    println("❌ Invalid groupId format: $groupId")
                    return Result.failure(Exception("Invalid groupId format"))
                }
            } else {
                println("✅ Student group already exists: $groupId")
            }
            
            // Check if class already exists to prevent duplicates
            val existingClasses = teacherDao.getClassesForTeacher(teacherEmail).first()
            val duplicateClass = existingClasses.find { 
                it.subjectName == subjectName && it.groupId == groupId 
            }
            
            if (duplicateClass != null) {
                println("⚠️ Class already exists: $subjectName for group $groupId by teacher $teacherEmail")
                return Result.failure(Exception("Class '$subjectName' already exists for this group"))
            }
            
            // Create locally first with a generated classId
            val classId = (teacherEmail + subjectName + groupId).hashCode()
            val classEntity = ClassEntity(
                classId = classId,
                teacherEmail = teacherEmail,
                subjectName = subjectName,
                groupId = groupId,
                classSchedule = classSchedule
            )
            val insertedId = try {
                teacherDao.insertClass(classEntity)
            } catch (e: Exception) {
                if (e.message?.contains("UNIQUE constraint failed") == true) {
                    println("❌ Class already exists (database constraint violation)")
                    return Result.failure(Exception("Class '$subjectName' already exists for this group"))
                } else {
                    throw e
                }
            }
            println("✅ Class inserted with ID: $insertedId (generated classId: $classId)")
            
                                    // Sync to Supabase
                        println("🔄 Syncing class to Supabase...")
                        
                        // Get teacher UUID from Supabase
                        val teacher = teacherDao.getTeacher().first()
                        if (teacher == null) {
                            println("⚠️ Teacher not found locally, skipping Supabase sync")
                        } else {
                            // Get teacher UUID from Supabase
                            val teacherResult = supabaseApiService.getTeacher(teacher.email)
                            if (teacherResult.isSuccess) {
                                val supabaseTeacher = teacherResult.getOrNull()
                                val teacherUuid = supabaseTeacher?.id
                                
                                if (teacherUuid == null) {
                                    println("⚠️ Teacher UUID not found in Supabase, skipping sync")
                                } else {
                                    val request = CreateClassRequest(
                                        class_name = subjectName,
                                        subject = subjectName,
                                        teacher_id = teacherUuid, // Use teacher UUID from Supabase
                                        group_id = groupId,
                                        semester = 1, // Default semester, should be calculated
                                        class_days = extractDaysFromSchedule(classSchedule),
                                        start_time = extractStartTimeFromSchedule(classSchedule),
                                        end_time = extractEndTimeFromSchedule(classSchedule)
                                    )
                                    val supabaseResult = supabaseApiService.createClass(request)
                                    if (supabaseResult.isSuccess) {
                                        println("✅ Class synced to Supabase successfully")
                                    } else {
                                        println("⚠️ Failed to sync class to Supabase: ${supabaseResult.exceptionOrNull()?.message}")
                                    }
                                }
                            } else {
                                println("⚠️ Failed to get teacher from Supabase: ${teacherResult.exceptionOrNull()?.message}")
                            }
                        }
            
            Result.success(classId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getClassesForTeacher(teacherEmail: String): Flow<List<ClassEntity>> {
        return teacherDao.getClassesForTeacher(teacherEmail)
    }

    suspend fun syncClassesFromSupabase(teacherEmail: String): Result<Unit> {
        return try {
            val result = supabaseApiService.getClassesByTeacher(teacherEmail)
            result.getOrNull()?.forEach { supabaseClass ->
                val classId = supabaseClass.id?.hashCode() ?: 0
                val classEntity = ClassEntity(
                    classId = classId, // Convert UUID to Int for local storage
                    teacherEmail = teacherEmail, // Use the teacherEmail parameter
                    subjectName = supabaseClass.subject,
                    groupId = supabaseClass.group_id,
                    classSchedule = buildScheduleFromComponents(
                        supabaseClass.class_days,
                        supabaseClass.start_time,
                        supabaseClass.end_time
                    )
                )
                println("🔍 DEBUG: About to insert class with classId: $classId (UUID: ${supabaseClass.id})")
                val insertedId = teacherDao.insertClass(classEntity)
                println("✅ Synced class '${supabaseClass.subject}' with local ID: $insertedId (UUID hashCode: $classId)")
                println("🔍 DEBUG: Inserted classId should match hashCode: ${insertedId.toInt() == classId}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // NEW: Sync classes by groupId (for students)
    suspend fun syncClassesFromSupabaseByGroup(groupId: String): Result<Unit> {
        return try {
            println("🔄 Syncing classes for group: $groupId")
            
            // Debug: Check student data before class sync
            val studentBeforeClassSync = studentDao.getStudent().first()
            println("🔍 DEBUG: Before class sync - Student: ${studentBeforeClassSync?.name} (${studentBeforeClassSync?.email}) - GroupId: '${studentBeforeClassSync?.groupId}'")
            
            // First sync all teachers to ensure they exist locally
            val teachersResult = supabaseApiService.getAllTeachers()
            if (teachersResult.isSuccess) {
                val teachers = teachersResult.getOrNull() ?: emptyList()
                teachers.forEach { supabaseTeacher ->
                    val teacherEntity = TeacherEntity(
                        email = supabaseTeacher.email,
                        name = supabaseTeacher.name,
                        subject = supabaseTeacher.subject,
                        password = supabaseTeacher.password
                    )
                    teacherDao.insertTeacher(teacherEntity)
                }
                println("✅ Synced ${teachers.size} teachers")
            }
            
            // Create a mapping from teacher UUID to email for class insertion
            val teacherUuidToEmailMap = mutableMapOf<String, String>()
            if (teachersResult.isSuccess) {
                val teachers = teachersResult.getOrNull() ?: emptyList()
                teachers.forEach { teacher ->
                    teacher.id?.let { uuid ->
                        teacherUuidToEmailMap[uuid] = teacher.email
                    }
                }
                println("📝 Created teacher UUID to email mapping: ${teacherUuidToEmailMap.size} entries")
            }
            
            // Sync the specific student group
            println("🔄 Fetching student group: $groupId")
            val groupResult = supabaseApiService.getStudentGroupById(groupId)
            if (groupResult.isSuccess) {
                val supabaseGroup = groupResult.getOrNull()
                if (supabaseGroup != null) {
                    println("📝 Found student group in Supabase: ${supabaseGroup.group_id}")
                    
                    // Parse group_id to extract branch, division, batch, semester
                    // Format: IT_2024-2027_A_1_Sem3
                    val groupParts = supabaseGroup.group_id.split("_")
                    val branch = groupParts[0] // IT
                    val division = groupParts[2] // A
                    val batch = groupParts[3].toInt() // 1
                    val semester = groupParts[4].replace("Sem", "").toInt() // 3
                    
                    val groupEntity = StudentGroupEntity(
                        groupId = supabaseGroup.group_id,
                        branch = branch,
                        admissionYear = supabaseGroup.admission_year,
                        graduationYear = supabaseGroup.graduation_year,
                        division = division,
                        batch = batch,
                        semester = semester,
                        academicConfigId = 1 // Default config ID
                    )
                    studentGroupDao.insertGroup(groupEntity)
                    println("✅ Synced student group: ${supabaseGroup.group_id} (${branch}, ${division}, batch ${batch}, sem ${semester})")
                } else {
                    println("⚠️ Student group not found in Supabase: $groupId")
                }
            } else {
                println("❌ Failed to fetch student group: ${groupResult.exceptionOrNull()?.message}")
            }
            
            // Now sync all classes and filter by groupId
            val classesResult = supabaseApiService.getAllClasses()
            if (classesResult.isSuccess) {
                val classes = classesResult.getOrNull() ?: emptyList()
                val groupClasses = classes.filter { it.group_id == groupId }
                println("📝 Found ${groupClasses.size} classes for group $groupId")
                
                groupClasses.forEach { supabaseClass ->
                    try {
                        // Convert teacher UUID to email
                        val teacherEmail = teacherUuidToEmailMap[supabaseClass.teacher_id]
                        if (teacherEmail == null) {
                            println("⚠️ Teacher UUID not found in mapping: ${supabaseClass.teacher_id}")
                            return@forEach
                        }
                        
                        val schedule = buildScheduleFromComponents(
                            supabaseClass.class_days,
                            supabaseClass.start_time,
                            supabaseClass.end_time
                        )
                        
                        // Create a more unique class ID by combining UUID with schedule info
                        val classId = if (supabaseClass.id != null) {
                            // Use UUID hashCode + schedule hashCode to ensure uniqueness
                            val uuidHash = supabaseClass.id.hashCode()
                            val scheduleHash = schedule.hashCode()
                            // Combine them in a way that avoids collisions
                            (uuidHash.toLong() + scheduleHash.toLong()).toInt()
                        } else {
                            // Fallback: use timestamp-based ID
                            (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
                        }
                        
                        println("🔍 DEBUG: Processing class:")
                        println("   - Subject: ${supabaseClass.subject}")
                        println("   - UUID: ${supabaseClass.id}")
                        println("   - ClassId (hashCode): $classId")
                        println("   - Schedule: $schedule")
                        println("   - Teacher: $teacherEmail")
                        
                        // Check if class already exists before inserting
                        val existingClass = teacherDao.getClassById(classId).first()
                        if (existingClass != null) {
                            println("⚠️ Class with ID $classId already exists, skipping insertion")
                            println("   - Existing: ${existingClass.subjectName} - ${existingClass.classSchedule}")
                            println("   - New: ${supabaseClass.subject} - $schedule")
                            return@forEach
                        }
                        
                        val classEntity = ClassEntity(
                            classId = classId,
                            teacherEmail = teacherEmail,
                            subjectName = supabaseClass.subject,
                            groupId = supabaseClass.group_id,
                            classSchedule = schedule
                        )
                        
                        val insertedId = teacherDao.insertClass(classEntity)
                        println("✅ Synced class '${supabaseClass.subject}' for group $groupId with local ID: $insertedId (UUID hashCode: $classId)")
                        println("🔍 DEBUG: Inserted classId should match hashCode: ${insertedId.toInt() == classId}")
                        println("   - Teacher: $teacherEmail (UUID: ${supabaseClass.teacher_id})")
                    } catch (e: Exception) {
                        println("❌ Failed to insert class '${supabaseClass.subject}': ${e.message}")
                        println("   - Teacher UUID: ${supabaseClass.teacher_id}")
                        println("   - Group ID: ${supabaseClass.group_id}")
                        e.printStackTrace()
                    }
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Error syncing classes for group $groupId: ${e.message}")
            Result.failure(e)
        }
    }

    // Student operations with sync
    suspend fun createStudent(
        email: String,
        name: String,
        rollNumber: String,
        branch: String,
        admissionYear: Int,
        graduationYear: Int,
        division: String,
        batch: Int,
        currentSemester: Int,
        groupId: String,
        password: String
    ): Result<Unit> {
        return try {
            // Validate group ID format first
            if (!GroupUtils.isValidGroupId(groupId)) {
                println("❌ Invalid group ID format: $groupId")
                return Result.failure(Exception("Invalid group ID format. Expected: Branch_AdmissionYear-GraduationYear_Division_Batch_SemX"))
            }
            
            // First, ensure the student group exists
            val groupExists = studentGroupDao.getGroupById(groupId).first() != null
            if (!groupExists) {
                println("🔍 Student group doesn't exist, creating it...")
                // Parse groupId using the updated GroupUtils.parseGroupId function
                val groupComponents = GroupUtils.parseGroupId(groupId)
                if (groupComponents != null) {
                    val branchFromGroup = groupComponents.branch
                    val admissionYearFromGroup = groupComponents.admissionYear
                    val graduationYearFromGroup = groupComponents.graduationYear
                    val divisionFromGroup = groupComponents.division
                    val batchFromGroup = groupComponents.batch
                    val semesterFromGroup = groupComponents.semester
                    
                    println("🔍 Parsed groupId components:")
                    println("   - Branch: $branchFromGroup")
                    println("   - Admission Year: $admissionYearFromGroup")
                    println("   - Graduation Year: $graduationYearFromGroup")
                    println("   - Division: $divisionFromGroup")
                    println("   - Batch: $batchFromGroup")
                    println("   - Semester: $semesterFromGroup")
                    
                    // Ensure default academic config exists and get its ID
                    var academicConfigId = 1 // Default fallback
                    val activeConfig = academicConfigDao.getActiveConfig().first()
                    if (activeConfig == null) {
                        println("🔍 Creating default academic config...")
                        val createConfigResult = createAcademicConfig(
                            programName = "Engineering",
                            durationYears = 4,
                            totalSemesters = 8,
                            semesterDurationMonths = 6
                        )
                        if (createConfigResult.isFailure) {
                            println("❌ Failed to create academic config: ${createConfigResult.exceptionOrNull()?.message}")
                            return Result.failure(createConfigResult.exceptionOrNull() ?: Exception("Failed to create academic config"))
                        }
                        println("✅ Default academic config created")
                        // Get the newly created config ID
                        val newConfig = academicConfigDao.getActiveConfig().first()
                        academicConfigId = newConfig?.configId ?: 1
                    } else {
                        academicConfigId = activeConfig.configId
                        println("✅ Using existing academic config: ${activeConfig.programName}")
                    }
                    
                    // Create the student group with the academic config
                    println("🔍 Creating student group with:")
                    println("   - groupId: $groupId")
                    println("   - branch: $branchFromGroup")
                    println("   - admissionYear: $admissionYearFromGroup")
                    println("   - graduationYear: $graduationYearFromGroup")
                    println("   - division: $divisionFromGroup")
                    println("   - batch: $batchFromGroup")
                    println("   - semester: $semesterFromGroup")
                    println("   - academicConfigId: $academicConfigId")
                    
                    val createGroupResult = createStudentGroup(
                        groupId = groupId,
                        branch = branchFromGroup,
                        admissionYear = admissionYearFromGroup,
                        graduationYear = graduationYearFromGroup,
                        division = divisionFromGroup,
                        batch = batchFromGroup,
                        semester = semesterFromGroup,
                        academicConfigId = academicConfigId
                    )
                    
                    if (createGroupResult.isFailure) {
                        println("❌ Failed to create student group: ${createGroupResult.exceptionOrNull()?.message}")
                        return Result.failure(createGroupResult.exceptionOrNull() ?: Exception("Failed to create student group"))
                    }
                    println("✅ Student group created successfully: $groupId")
                } else {
                    println("❌ Invalid groupId format: $groupId")
                    return Result.failure(Exception("Invalid groupId format"))
                }
            } else {
                println("✅ Student group already exists: $groupId")
            }
            
            // Hash the password
            val hashedPassword = com.example.bleattendance.utils.PasswordUtils.hashPassword(password)
            
            // Create locally first
            val studentEntity = StudentEntity(
                email = email,
                name = name,
                rollNumber = rollNumber,
                branch = branch,
                admissionYear = admissionYear,
                graduationYear = graduationYear,
                division = division,
                batch = batch,
                currentSemester = currentSemester,
                groupId = groupId,
                password = hashedPassword
            )
            studentDao.insertStudent(studentEntity)
            
            // Sync to Supabase
            val request = CreateStudentRequest(
                email = email,
                name = name,
                roll_number = rollNumber, // Keep as String to support alphanumeric roll numbers
                branch = branch,
                admission_year = admissionYear,
                graduation_year = graduationYear,
                division = division,
                batch = batch,
                current_semester = currentSemester,
                group_id = groupId, // Include the group_id since the column exists
                password = hashedPassword
            )
            println("🔄 Syncing student to Supabase...")
            println("   - Request data: $request")
            
            // Test if student_profile table is accessible
            val tableTestResult = supabaseApiService.testStudentProfileTable()
            if (tableTestResult.isFailure) {
                println("❌ Student profile table is not accessible: ${tableTestResult.exceptionOrNull()?.message}")
            }
            
            // Test if we can insert a student record
            val insertTestResult = supabaseApiService.testStudentInsert()
            if (insertTestResult.isFailure) {
                println("❌ Student insert test failed: ${insertTestResult.exceptionOrNull()?.message}")
            }
            
            val supabaseResult = supabaseApiService.createStudent(request)
            if (supabaseResult.isSuccess) {
                println("✅ Student synced to Supabase successfully")
                Result.success(Unit)
            } else {
                val error = supabaseResult.exceptionOrNull()
                println("❌ Failed to sync student to Supabase: ${error?.message}")
                error?.printStackTrace()
                // Still return success for local storage, but log the Supabase failure
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStudent(): Flow<StudentEntity?> {
        return studentDao.getStudent()
    }

    suspend fun getStudentsByGroup(groupId: String): Flow<List<StudentEntity>> {
        return studentDao.getStudentsByGroup(groupId)
    }
    
    // NEW: Sync individual student from Supabase
    suspend fun syncStudentFromSupabase(studentEmail: String): Result<Unit> {
        return try {
            println("🔄 Syncing student data from Supabase: $studentEmail")
            
            val result = supabaseApiService.getStudent(studentEmail)
            if (result.isSuccess) {
                val supabaseStudent = result.getOrNull()
                if (supabaseStudent != null) {
                    println("📝 Found student in Supabase: ${supabaseStudent.name} (${supabaseStudent.email})")
                    println("   - Group ID: ${supabaseStudent.group_id}")
                    
                    val studentEntity = StudentEntity(
                        email = supabaseStudent.email,
                        name = supabaseStudent.name,
                        rollNumber = supabaseStudent.roll_number,
                        branch = supabaseStudent.branch,
                        admissionYear = supabaseStudent.admission_year,
                        graduationYear = supabaseStudent.graduation_year,
                        division = supabaseStudent.division,
                        batch = supabaseStudent.batch,
                        currentSemester = supabaseStudent.current_semester,
                        groupId = supabaseStudent.group_id,
                        password = supabaseStudent.password
                    )
                    
                    studentDao.insertStudent(studentEntity)
                    println("✅ Student synced to local database: ${supabaseStudent.name}")
                    
                    // Debug: Verify the student was inserted correctly
                    val insertedStudent = studentDao.getStudent().first()
                    println("🔍 DEBUG: After insert - Student: ${insertedStudent?.name} (${insertedStudent?.email}) - GroupId: '${insertedStudent?.groupId}'")
                    
                    Result.success(Unit)
                } else {
                    println("⚠️ Student not found in Supabase: $studentEmail")
                    Result.failure(Exception("Student not found in Supabase"))
                }
            } else {
                println("❌ Failed to fetch student from Supabase: ${result.exceptionOrNull()?.message}")
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            println("❌ Error syncing student from Supabase: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun syncStudentsFromSupabase(groupId: String): Result<Unit> {
        return try {
            val result = supabaseApiService.getStudentsByGroup(groupId)
            result.getOrNull()?.forEach { supabaseStudent ->
                val studentEntity = StudentEntity(
                    email = supabaseStudent.email,
                    name = supabaseStudent.name,
                    rollNumber = supabaseStudent.roll_number,
                    branch = supabaseStudent.branch,
                    admissionYear = supabaseStudent.admission_year,
                    graduationYear = supabaseStudent.graduation_year,
                    division = supabaseStudent.division,
                    batch = supabaseStudent.batch,
                    currentSemester = supabaseStudent.current_semester,
                    groupId = supabaseStudent.group_id,
                    password = supabaseStudent.password
                )
                studentDao.insertStudent(studentEntity)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Class Session operations
    suspend fun createClassSession(
        classId: Int,
        scheduledDate: String,
        startTime: String,
        endTime: String,
        sessionCode: String
    ): Result<Int> {
        return try {
            println("🔄 Creating class session: classId=$classId, date=$scheduledDate, time=$startTime-$endTime, code=$sessionCode")
            
            // First, save to local database
            val sessionEntity = ClassSessionEntity(
                classId = classId,
                scheduledDate = scheduledDate,
                startTime = startTime,
                endTime = endTime,
                sessionCode = sessionCode
            )
            classSessionDao.insertSession(sessionEntity)
            println("✅ Class session saved locally with ID: ${sessionEntity.sessionId}")
            
            // Then, sync to Supabase
            val request = CreateClassSessionRequest(
                class_id = classId,
                scheduled_date = scheduledDate,
                start_time = startTime,
                end_time = endTime,
                session_code = sessionCode
            )
            val supabaseResult = supabaseApiService.createClassSession(request)
            if (supabaseResult.isSuccess) {
                println("✅ Class session synced to Supabase successfully")
            } else {
                println("⚠️ Failed to sync class session to Supabase: ${supabaseResult.exceptionOrNull()?.message}")
            }
            
            Result.success(sessionEntity.sessionId)
        } catch (e: Exception) {
            println("❌ Error creating class session: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getClassSessionsForClass(classId: Int): Flow<List<ClassSessionEntity>> {
        return classSessionDao.getSessionsForClass(classId)
    }

    suspend fun getTodaysSessions(date: String): Flow<List<ClassSessionEntity>> {
        return classSessionDao.getTodaysSessions(date)
    }
    
    // Helper functions to extract schedule components
    private fun extractDaysFromSchedule(schedule: String?): String? {
        if (schedule.isNullOrBlank()) return null
        
        // Look for day patterns at the start of the string
        val dayPattern = Regex("^([A-Za-z,\\s]+?)(?=\\s+\\d{2}:\\d{2}|$)")
        val dayMatch = dayPattern.find(schedule.trim())
        
        if (dayMatch != null) {
            val daysString = dayMatch.value.trim()
            val cleanedDays = daysString.replace(Regex("\\s+"), " ").trim()
            if (cleanedDays.isNotBlank()) {
                return cleanedDays
            }
        }
        
        // Fallback: look for any day patterns in the entire string
        val anyDayPattern = Regex("(Mon|Tue|Wed|Thu|Fri|Sat|Sun|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)(?:\\s*,\\s*(Mon|Tue|Wed|Thu|Fri|Sat|Sun|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday))*")
        val anyMatch = anyDayPattern.find(schedule)
        return anyMatch?.value
    }
    
    private fun extractStartTimeFromSchedule(schedule: String?): String? {
        if (schedule.isNullOrBlank()) return null
        
        val timePattern = Regex("(\\d{1,2}:\\d{2})-(\\d{1,2}:\\d{2})")
        val match = timePattern.find(schedule)
        return match?.groupValues?.get(1)
    }
    
    private fun extractEndTimeFromSchedule(schedule: String?): String? {
        if (schedule.isNullOrBlank()) return null
        
        val timePattern = Regex("(\\d{1,2}:\\d{2})-(\\d{1,2}:\\d{2})")
        val match = timePattern.find(schedule)
        return match?.groupValues?.get(2)
    }
    
    private fun buildScheduleFromComponents(
        days: String?,
        startTime: String?,
        endTime: String?
    ): String? {
        // If we have separate components, build the schedule string
        if (!days.isNullOrBlank() && !startTime.isNullOrBlank() && !endTime.isNullOrBlank()) {
            return "$days $startTime-$endTime"
        }
        
        // Return null if components are missing
        return null
    }

    suspend fun updateClassSessionStatus(sessionId: Int, status: String): Result<Unit> {
        return try {
            println("🔄 Updating class session status: sessionId=$sessionId, status=$status")
            
            // First, update local database
            val session = classSessionDao.getSessionById(sessionId).first()
            if (session != null) {
                val updatedSession = session.copy(status = status)
                classSessionDao.updateSession(updatedSession)
                println("✅ Class session status updated locally")
            } else {
                println("⚠️ Class session not found locally: $sessionId")
            }
            
            // Then, sync to Supabase
            val supabaseResult = supabaseApiService.updateClassSessionStatus(sessionId, status)
            if (supabaseResult.isSuccess) {
                println("✅ Class session status synced to Supabase successfully")
            } else {
                println("⚠️ Failed to sync class session status to Supabase: ${supabaseResult.exceptionOrNull()?.message}")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Error updating class session status: ${e.message}")
            Result.failure(e)
        }
    }

    // Attendance session operations with sync
    suspend fun createAttendanceSession(
        sessionId: Int,
        studentEmail: String,
        attendanceStatus: String,
        markedVia: String? = null,
        classId: String? = null,
        sessionCode: String? = null,
        scheduledDate: String? = null,
        startTime: String? = null,
        endTime: String? = null
    ): Result<Unit> {
        return try {
            val sessionEntity = AttendanceSessionEntity(
                sessionId = sessionId,
                studentEmail = studentEmail,
                attendanceStatus = attendanceStatus,
                markedAt = System.currentTimeMillis(),
                markedVia = markedVia
            )
            attendanceDao.insertSession(sessionEntity)
            println("✅ Attendance session saved locally with ID: $sessionId")
            
            // Sync to Supabase
            val request = CreateAttendanceRecordRequest(
                session_id = sessionId,
                student_email = studentEmail,
                attendance_status = attendanceStatus,
                marked_via = markedVia ?: "BLE",
                marked_at = System.currentTimeMillis().toString()
            )
            
            val supabaseResult = supabaseApiService.createAttendanceRecord(request)
            if (supabaseResult.isSuccess) {
                println("✅ Attendance record synced to Supabase successfully")
            } else {
                println("⚠️ Failed to sync attendance record to Supabase: ${supabaseResult.exceptionOrNull()?.message}")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Error creating attendance session: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getAttendanceSessionsForClass(classId: Int): Flow<List<AttendanceSessionEntity>> {
        return attendanceDao.getSessionsForClass(classId)
    }

    suspend fun syncAttendanceSessionsFromSupabase(sessionId: Int): Result<Unit> {
        return try {
            val result = supabaseApiService.getAttendanceSessionsBySession(sessionId)
            result.getOrNull()?.forEach { supabaseSession ->
                val sessionEntity = AttendanceSessionEntity(
                    sessionId = supabaseSession.session_id ?: 0,
                    studentEmail = supabaseSession.student_email,
                    attendanceStatus = supabaseSession.attendance_status,
                    markedAt = supabaseSession.marked_at?.toLongOrNull() ?: System.currentTimeMillis(),
                    markedVia = supabaseSession.marked_via
                )
                attendanceDao.insertSession(sessionEntity)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Student Group operations
    suspend fun createStudentGroup(
        groupId: String,
        branch: String,
        admissionYear: Int,
        graduationYear: Int,
        division: String,
        batch: Int,
        semester: Int,
        academicConfigId: Int
    ): Result<Unit> {
        return try {
            val groupEntity = StudentGroupEntity(
                groupId = groupId,
                branch = branch,
                admissionYear = admissionYear,
                graduationYear = graduationYear,
                division = division,
                batch = batch,
                semester = semester,
                academicConfigId = academicConfigId
            )
            
            // Create locally first
            studentGroupDao.insertGroup(groupEntity)
            println("✅ Student group created locally: $groupId")
            
            // Sync to Supabase
            println("🔄 Syncing student group to Supabase...")
            val request = CreateStudentGroupRequest(
                group_id = groupId,
                group_name = "$branch $admissionYear-$graduationYear $division Batch $batch",
                branch = branch,
                division = division,
                batch = batch,
                semester = semester,
                admission_year = admissionYear,
                graduation_year = graduationYear
            )
            println("🔍 Supabase request data:")
            println("   - group_id: ${request.group_id}")
            println("   - group_name: ${request.group_name}")
            println("   - branch: ${request.branch}")
            println("   - division: ${request.division}")
            println("   - batch: ${request.batch}")
            println("   - semester: ${request.semester}")
            println("   - admission_year: ${request.admission_year}")
            println("   - graduation_year: ${request.graduation_year}")
            
            val supabaseResult = supabaseApiService.createStudentGroup(request)
            if (supabaseResult.isSuccess) {
                println("✅ Student group synced to Supabase successfully")
            } else {
                println("⚠️ Failed to sync student group to Supabase: ${supabaseResult.exceptionOrNull()?.message}")
                supabaseResult.exceptionOrNull()?.printStackTrace()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Failed to create student group: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getGroupById(groupId: String): Flow<StudentGroupEntity?> {
        return studentGroupDao.getGroupById(groupId)
    }

    // Academic Config operations
    suspend fun createAcademicConfig(
        programName: String,
        durationYears: Int,
        totalSemesters: Int,
        semesterDurationMonths: Int
    ): Result<Unit> {
        return try {
            val configEntity = AcademicConfigEntity(
                programName = programName,
                durationYears = durationYears,
                totalSemesters = totalSemesters,
                semesterDurationMonths = semesterDurationMonths
            )
            
            // Create locally first
            academicConfigDao.insertConfig(configEntity)
            println("✅ Academic config created locally: $programName")
            
            // Sync to Supabase
            println("🔄 Syncing academic config to Supabase...")
            val request = CreateAcademicConfigRequest(
                program_name = programName,
                duration_years = durationYears,
                total_semesters = totalSemesters,
                semester_duration_months = semesterDurationMonths
            )
            val supabaseResult = supabaseApiService.createAcademicConfig(request)
            if (supabaseResult.isSuccess) {
                println("✅ Academic config synced to Supabase successfully")
            } else {
                println("⚠️ Failed to sync academic config to Supabase: ${supabaseResult.exceptionOrNull()?.message}")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Failed to create academic config: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getActiveConfig(): Flow<AcademicConfigEntity?> {
        return academicConfigDao.getActiveConfig()
    }

    // Utility methods for offline/online sync
    suspend fun refreshCurrentTeacherData(): Result<Unit> {
        return try {
            println("🔄 Refreshing current teacher's data from Supabase...")
            
            // Get current teacher
            val currentTeacher = teacherDao.getTeacher().first()
            if (currentTeacher == null) {
                println("❌ No current teacher found to refresh")
                return Result.failure(Exception("No current teacher found"))
            }
            
            println("📝 Refreshing data for teacher: ${currentTeacher.name} (${currentTeacher.email})")
            
            // First sync all student groups to ensure they exist locally
            val groupsResult = withTimeout(5000) {
                supabaseApiService.getAllStudentGroups()
            }
            if (groupsResult.isSuccess) {
                val groups = groupsResult.getOrNull() ?: emptyList()
                println("📝 Syncing ${groups.size} student groups to local database...")
                groups.forEach { supabaseGroup ->
                    try {
                        // Parse group_id to extract branch, division, batch, semester
                        // Format: IT_2024-2027_A_1_Sem3
                        val groupParts = supabaseGroup.group_id.split("_")
                        val branch = groupParts[0] // IT
                        val division = groupParts[2] // A
                        val batch = groupParts[3].toInt() // 1
                        val semester = groupParts[4].replace("Sem", "").toInt() // 3
                        
                        val groupEntity = StudentGroupEntity(
                            groupId = supabaseGroup.group_id,
                            branch = branch,
                            admissionYear = supabaseGroup.admission_year,
                            graduationYear = supabaseGroup.graduation_year,
                            division = division,
                            batch = batch,
                            semester = semester,
                            academicConfigId = 1 // Default config ID
                        )
                        studentGroupDao.insertGroup(groupEntity)
                        println("   - ✅ Synced student group: ${supabaseGroup.group_id}")
                    } catch (e: Exception) {
                        println("   - ❌ Failed to sync student group ${supabaseGroup.group_id}: ${e.message}")
                    }
                }
                println("✅ Student groups synced successfully")
            } else {
                println("⚠️ Failed to sync student groups: ${groupsResult.exceptionOrNull()?.message}")
            }
            
            // Use existing sync methods to refresh data
            val classesResult = syncClassesFromSupabase(currentTeacher.email)
            if (classesResult.isSuccess) {
                println("✅ Classes refreshed successfully for current teacher")
            } else {
                println("⚠️ Failed to refresh classes: ${classesResult.exceptionOrNull()?.message}")
            }
            
            // Sync assignments for current teacher's classes only
            val assignmentsResult = syncAssignmentsForCurrentTeacher(currentTeacher.email)
            if (assignmentsResult.isSuccess) {
                println("✅ Assignments refreshed successfully for current teacher")
            } else {
                println("⚠️ Failed to refresh assignments: ${assignmentsResult.exceptionOrNull()?.message}")
            }
            
            // Sync class sessions for current teacher's classes only
            val sessionsResult = syncClassSessionsForCurrentTeacher(currentTeacher.email)
            if (sessionsResult.isSuccess) {
                println("✅ Class sessions refreshed successfully for current teacher")
            } else {
                println("⚠️ Failed to refresh class sessions: ${sessionsResult.exceptionOrNull()?.message}")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Failed to refresh current teacher data: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun syncAssignmentsForCurrentTeacher(teacherEmail: String): Result<Unit> {
        return try {
            println("🔄 Syncing assignments for current teacher: $teacherEmail")
            
            // Get current teacher's classes first
            val teacherClasses = teacherDao.getClassesForTeacher(teacherEmail).first()
            if (teacherClasses.isEmpty()) {
                println("⚠️ No local classes found for teacher, but will sync all assignments from Supabase")
                // Don't return early - continue to sync all assignments
            }
            
            // Sync all assignments from Supabase regardless of local classes
            println("🔄 Syncing all assignments from Supabase...")
            
            // Get all assignments from Supabase
            val assignmentsResult = supabaseApiService.getAllAssignments()
            if (assignmentsResult.isSuccess) {
                val assignments = assignmentsResult.getOrNull() ?: emptyList()
                println("📝 Found ${assignments.size} assignments from Supabase")
                if (assignments.isEmpty()) {
                    println("🔍 DEBUG: No assignments found in Supabase")
                    return Result.success(Unit)
                }
                
                // Get all classes from Supabase to create local classes if needed
                val allClassesResult = supabaseApiService.getAllClasses()
                if (allClassesResult.isSuccess) {
                    val allSupabaseClasses = allClassesResult.getOrNull() ?: emptyList()
                    println("📝 Found ${allSupabaseClasses.size} classes in Supabase")
                    
                    // Get teacher UUID to email mapping
                    val teachersResult = supabaseApiService.getAllTeachers()
                    val teacherUuidToEmailMap = if (teachersResult.isSuccess) {
                        val teachers = teachersResult.getOrNull() ?: emptyList()
                        teachers.associate { it.id to it.email }
                    } else {
                        emptyMap()
                    }
                    
                    // Create local classes for all Supabase classes
                    val classUuidToLocalIdMap = mutableMapOf<String, Int>()
                    allSupabaseClasses.forEach { supabaseClass ->
                        supabaseClass.id?.let { classUuid ->
                            val teacherEmail = teacherUuidToEmailMap[supabaseClass.teacher_id] ?: "unknown@teacher.com"
                            val classId = classUuid.hashCode()
                            val classEntity = ClassEntity(
                                classId = classId,
                                teacherEmail = teacherEmail,
                                subjectName = supabaseClass.subject,
                                groupId = supabaseClass.group_id,
                                classSchedule = buildScheduleFromComponents(
                                    supabaseClass.class_days,
                                    supabaseClass.start_time,
                                    supabaseClass.end_time
                                ),
                                isActive = true
                            )
                            
                            try {
                                teacherDao.insertClass(classEntity)
                                classUuidToLocalIdMap[classUuid] = classId
                            } catch (e: Exception) {
                                // Class might already exist, try to get existing one
                                val existingClass = teacherDao.getClassById(classId).first()
                                if (existingClass != null) {
                                    classUuidToLocalIdMap[classUuid] = classId
                                }
                            }
                        }
                    }
                
                    // Sync assignments using the class mapping
                    var syncedCount = 0
                    assignments.forEach { supabaseAssignment ->
                        val localClassId = classUuidToLocalIdMap[supabaseAssignment.class_id]
                        val originalTeacherEmail = teacherUuidToEmailMap[supabaseAssignment.teacher_id]
                        
                        if (localClassId != null && originalTeacherEmail != null) {
                            try {
                                val assignmentEntity = AssignmentEntity(
                                    id = supabaseAssignment.id ?: java.util.UUID.randomUUID().toString(),
                                    title = supabaseAssignment.title,
                                    description = supabaseAssignment.description ?: "",
                                    instructions = supabaseAssignment.instructions ?: "",
                                    assignmentType = supabaseAssignment.assignment_type ?: "homework",
                                    maxPoints = supabaseAssignment.max_points ?: 100,
                                    dueDate = try {
                                        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(supabaseAssignment.due_date)?.time ?: System.currentTimeMillis()
                                    } catch (e: Exception) {
                                        System.currentTimeMillis()
                                    },
                                    allowLateSubmission = supabaseAssignment.allow_late_submission ?: false,
                                    latePenaltyPercentage = supabaseAssignment.late_penalty_percentage ?: 0,
                                    attachmentUrls = supabaseAssignment.attachment_urls?.joinToString(","),
                                    teacherId = originalTeacherEmail,
                                    classId = localClassId
                                )
                                assignmentDao.insertAssignment(assignmentEntity)
                                syncedCount++
                            } catch (e: Exception) {
                                // Assignment might already exist, skip silently
                            }
                        }
                    }
                    
                    println("✅ Synced $syncedCount assignments for teacher $teacherEmail")
                } else {
                    println("⚠️ Failed to get classes from Supabase: ${allClassesResult.exceptionOrNull()?.message}")
                }
            } else {
                println("⚠️ Failed to get assignments from Supabase: ${assignmentsResult.exceptionOrNull()?.message}")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Failed to sync assignments for current teacher: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun syncClassSessionsForCurrentTeacher(teacherEmail: String): Result<Unit> {
        return try {
            println("🔄 Syncing class sessions for current teacher: $teacherEmail")
            
            // Get current teacher's classes first
            val teacherClasses = teacherDao.getClassesForTeacher(teacherEmail).first()
            if (teacherClasses.isEmpty()) {
                println("⚠️ No classes found for teacher, skipping session sync")
                return Result.success(Unit)
            }
            
            val classIds = teacherClasses.map { it.classId }
            println("📝 Found ${classIds.size} classes for teacher, syncing sessions for these classes only")
            
            var totalSessionsSynced = 0
            var totalAttendanceSynced = 0
            
            // Sync sessions for each of the teacher's classes
            classIds.forEach { classId ->
                val sessionsResult = supabaseApiService.getClassSessionsByClass(classId)
                if (sessionsResult.isSuccess) {
                    val sessions = sessionsResult.getOrNull() ?: emptyList()
                    println("   - Syncing ${sessions.size} sessions for class $classId")
                    
                    sessions.forEach { supabaseSession ->
                        // Generate sessionId if not provided (use hashCode of UUID or generate new one)
                        val sessionId = supabaseSession.session_id ?: supabaseSession.id?.hashCode() ?: java.util.UUID.randomUUID().hashCode()
                        
                        val sessionEntity = ClassSessionEntity(
                            sessionId = sessionId,
                            classId = supabaseSession.class_id.hashCode(),
                            scheduledDate = supabaseSession.session_date,
                            startTime = supabaseSession.start_time,
                            endTime = supabaseSession.end_time,
                            sessionCode = supabaseSession.session_code ?: "SESSION_${supabaseSession.id?.take(8) ?: "UNKNOWN"}",
                            status = supabaseSession.status ?: "scheduled"
                        )
                        classSessionDao.insertSession(sessionEntity)
                        totalSessionsSynced++
                        println("     - ✅ Synced session: ${sessionEntity.sessionCode} (${sessionEntity.scheduledDate})")
                        
                        // Sync attendance for this session
                        val attendanceResult = supabaseApiService.getAttendanceSessionsBySession(sessionId)
                        if (attendanceResult.isSuccess) {
                            val attendanceSessions = attendanceResult.getOrNull() ?: emptyList()
                            println("     - Syncing ${attendanceSessions.size} attendance records for session $sessionId")
                            attendanceSessions.forEach { supabaseAttendance ->
                                val attendanceEntity = AttendanceSessionEntity(
                                    sessionId = supabaseAttendance.session_id ?: 0,
                                    studentEmail = supabaseAttendance.student_email,
                                    attendanceStatus = supabaseAttendance.attendance_status,
                                    markedAt = supabaseAttendance.marked_at?.toLongOrNull() ?: System.currentTimeMillis(),
                                    markedVia = supabaseAttendance.marked_via
                                )
                                attendanceDao.insertSession(attendanceEntity)
                                totalAttendanceSynced++
                            }
                        } else {
                            println("     - ⚠️ Failed to sync attendance for session $sessionId: ${attendanceResult.exceptionOrNull()?.message}")
                        }
                    }
                } else {
                    println("   - ⚠️ Failed to get sessions for class $classId: ${sessionsResult.exceptionOrNull()?.message}")
                }
            }
            
            println("✅ Synced $totalSessionsSynced sessions and $totalAttendanceSynced attendance records for current teacher's classes")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Failed to sync class sessions for current teacher: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun refreshCurrentStudentData(): Result<Unit> {
        return try {
            println("🔄 Refreshing current student's data from Supabase...")
            
            // Get current student
            val currentStudent = studentDao.getStudent().first()
            if (currentStudent == null) {
                println("❌ No current student found to refresh")
                return Result.failure(Exception("No current student found"))
            }
            
            println("📝 Refreshing data for student: ${currentStudent.name} (${currentStudent.email})")
            
            // First, sync the student data from Supabase to update groupId
            val studentSyncResult = syncStudentFromSupabase(currentStudent.email)
            if (studentSyncResult.isSuccess) {
                println("✅ Student data synced from Supabase")
                // Get the updated student data
                val updatedStudent = studentDao.getStudent().first()
                if (updatedStudent?.groupId != null) {
                    println("📝 Updated student groupId: ${updatedStudent.groupId}")
                } else {
                    println("⚠️ Student groupId is still null after sync")
                }
            } else {
                println("⚠️ Failed to sync student data from Supabase: ${studentSyncResult.exceptionOrNull()?.message}")
            }
            
            // Wait a moment to ensure database operations complete
            kotlinx.coroutines.delay(50)
            
            // Sync classes for current student's group
            val finalStudent = studentDao.getStudent().first()
            if (finalStudent?.groupId != null) {
                val classesResult = syncClassesFromSupabaseByGroup(finalStudent.groupId)
                if (classesResult.isSuccess) {
                    println("✅ Classes refreshed successfully for current student")
                    
                    // Debug: Check student data after class sync
                    val studentAfterClassSync = studentDao.getStudent().first()
                    println("🔍 DEBUG: After class sync - Student: ${studentAfterClassSync?.name} (${studentAfterClassSync?.email}) - GroupId: '${studentAfterClassSync?.groupId}'")
                } else {
                    println("⚠️ Failed to refresh classes: ${classesResult.exceptionOrNull()?.message}")
                }
            } else {
                println("⚠️ Student has no groupId, skipping class sync")
            }
            
            // Sync assignments for current student's classes only
            val assignmentsResult = syncAssignmentsForCurrentStudent(currentStudent.email)
            if (assignmentsResult.isSuccess) {
                println("✅ Assignments refreshed successfully for current student")
            } else {
                println("⚠️ Failed to refresh assignments: ${assignmentsResult.exceptionOrNull()?.message}")
            }
            
            // Sync class sessions for current student's classes only
            val sessionsResult = syncClassSessionsForCurrentStudent(currentStudent.email)
            if (sessionsResult.isSuccess) {
                println("✅ Class sessions refreshed successfully for current student")
            } else {
                println("⚠️ Failed to refresh class sessions: ${sessionsResult.exceptionOrNull()?.message}")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Failed to refresh current student data: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun syncAssignmentsForCurrentStudent(studentEmail: String): Result<Unit> {
        return try {
            println("🔄 Syncing assignments for current student: $studentEmail")
            
            // First, get the student's group information
            val student = studentDao.getStudentByEmail(studentEmail).first()
            if (student == null) {
                println("❌ Student not found: $studentEmail")
                return Result.failure(Exception("Student not found"))
            }
            
            println("🔍 DEBUG: Student details:")
            println("   - Name: ${student.name}")
            println("   - Email: ${student.email}")
            println("   - GroupId: '${student.groupId}'")
            println("   - Branch: ${student.branch}")
            println("   - Division: ${student.division}")
            println("   - Batch: ${student.batch}")
            println("   - Semester: ${student.currentSemester}")
            
            // First, ensure student's classes are synced from Supabase
            println("🔄 Syncing student classes from Supabase first...")
            val syncClassesResult = syncClassesFromSupabaseByGroup(student.groupId ?: "")
            if (syncClassesResult.isSuccess) {
                println("✅ Student classes synced successfully")
            } else {
                println("⚠️ Failed to sync student classes: ${syncClassesResult.exceptionOrNull()?.message}")
            }
            
            // Get current student's classes after sync
            val studentClasses = getClassesForStudent(studentEmail).first()
            println("🔍 DEBUG: Student classes found after sync: ${studentClasses.size}")
            studentClasses.forEach { classEntity ->
                println("   - Class: ${classEntity.subjectName} (ID: ${classEntity.classId}, Group: ${classEntity.groupId})")
            }
            if (studentClasses.isEmpty()) {
                println("⚠️ No classes found for student after sync, skipping assignment sync")
                return Result.success(Unit)
            }
            
            val classIds = studentClasses.map { it.classId }
            println("📝 Found ${classIds.size} classes for student, syncing assignments for these classes only")
            
            var totalAssignmentsSynced = 0
            
            // Get all assignments from Supabase and filter by student's classes
            val allAssignmentsResult = supabaseApiService.getAllAssignments()
            if (allAssignmentsResult.isSuccess) {
                val allAssignments = allAssignmentsResult.getOrNull() ?: emptyList()
                println("📝 Found ${allAssignments.size} total assignments in Supabase")
                
                // Get all classes from Supabase to map UUIDs to local class IDs
                val allClassesResult = supabaseApiService.getAllClasses()
                if (allClassesResult.isSuccess) {
                    val allSupabaseClasses = allClassesResult.getOrNull() ?: emptyList()
                    println("📝 Found ${allSupabaseClasses.size} classes in Supabase")
                    
                    // Create a mapping from Supabase class UUID to local class ID
                    val classUuidToLocalIdMap = mutableMapOf<String, Int>()
                    // Create a mapping from teacher UUID to email
                    val teacherUuidToEmailMap = mutableMapOf<String, String>()
                    
                    // First, build teacher UUID to email mapping
                    allSupabaseClasses.forEach { supabaseClass ->
                        supabaseClass.teacher_id?.let { teacherUuid ->
                            // Find the teacher email from the local class using improved matching
                            val supabaseSchedule = buildScheduleFromComponents(
                                supabaseClass.class_days,
                                supabaseClass.start_time,
                                supabaseClass.end_time
                            )
                            
                            val matchingLocalClass = studentClasses.find { localClass ->
                                localClass.subjectName == supabaseClass.subject && 
                                localClass.groupId == supabaseClass.group_id &&
                                localClass.classSchedule == supabaseSchedule
                            }
                            matchingLocalClass?.let { localClass ->
                                teacherUuidToEmailMap[teacherUuid] = localClass.teacherEmail
                            }
                        }
                    }
                    
                    allSupabaseClasses.forEach { supabaseClass ->
                        supabaseClass.id?.let { classUuid ->
                            // Find the corresponding local class by subject, group, day, and time
                            val supabaseSchedule = buildScheduleFromComponents(
                                supabaseClass.class_days,
                                supabaseClass.start_time,
                                supabaseClass.end_time
                            )
                            
                            val matchingLocalClass = studentClasses.find { localClass ->
                                localClass.subjectName == supabaseClass.subject && 
                                localClass.groupId == supabaseClass.group_id &&
                                localClass.classSchedule == supabaseSchedule
                            }
                            matchingLocalClass?.let { localClass ->
                                classUuidToLocalIdMap[classUuid] = localClass.classId
                                println("   - Mapped Supabase class ${supabaseClass.subject} (${classUuid}) to local ID ${localClass.classId}")
                                println("     - Schedule: ${supabaseSchedule}")
                            } ?: run {
                                println("   - ⚠️ No local class found for Supabase class ${supabaseClass.subject} (${classUuid}) with group ${supabaseClass.group_id}")
                                println("     - Looking for schedule: ${supabaseSchedule}")
                            }
                        }
                    }
                    
                    // Filter assignments that belong to the student's classes
                    val relevantAssignments = allAssignments.filter { supabaseAssignment ->
                        val hasMapping = classUuidToLocalIdMap.containsKey(supabaseAssignment.class_id)
                        println("   - Assignment '${supabaseAssignment.title}' (Class ID: ${supabaseAssignment.class_id}) - Has mapping: $hasMapping")
                        hasMapping
                    }
                    
                    println("📝 Found ${relevantAssignments.size} relevant assignments for student")
                    println("🔍 DEBUG: Class UUID to Local ID mapping:")
                    classUuidToLocalIdMap.forEach { (uuid, localId) ->
                        println("   - $uuid -> $localId")
                    }
                    println("🔍 DEBUG: All assignments in Supabase:")
                    allAssignments.forEach { assignment ->
                        println("   - ${assignment.title} (Class ID: ${assignment.class_id})")
                    }
                    
                    // Debug: Check what class the assignment belongs to
                    val assignmentClassId = "ae7d1b50-6a90-4660-88ae-cc046a78fd21"
                    val assignmentClass = allSupabaseClasses.find { it.id == assignmentClassId }
                    if (assignmentClass != null) {
                        println("🔍 DEBUG: Assignment class details:")
                        println("   - Subject: ${assignmentClass.subject}")
                        println("   - Group ID: ${assignmentClass.group_id}")
                        println("   - Teacher ID: ${assignmentClass.teacher_id}")
                        println("   - Class UUID: ${assignmentClass.id}")
                    } else {
                        println("⚠️ DEBUG: Assignment class not found in Supabase classes!")
                    }
                    
                    relevantAssignments.forEach { supabaseAssignment ->
                        try {
                            val localClassId = classUuidToLocalIdMap[supabaseAssignment.class_id]
                            val teacherEmail = teacherUuidToEmailMap[supabaseAssignment.teacher_id]
                            
                            if (localClassId != null && teacherEmail != null) {
                                val assignmentEntity = AssignmentEntity(
                                    id = supabaseAssignment.id ?: java.util.UUID.randomUUID().toString(),
                                    title = supabaseAssignment.title,
                                    description = supabaseAssignment.description ?: "",
                                    instructions = supabaseAssignment.instructions ?: "",
                                    assignmentType = supabaseAssignment.assignment_type ?: "homework",
                                    maxPoints = supabaseAssignment.max_points ?: 100,
                                    dueDate = try {
                                        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(supabaseAssignment.due_date)?.time ?: System.currentTimeMillis()
                                    } catch (e: Exception) {
                                        System.currentTimeMillis()
                                    },
                                    allowLateSubmission = supabaseAssignment.allow_late_submission ?: false,
                                    latePenaltyPercentage = supabaseAssignment.late_penalty_percentage ?: 0,
                                    attachmentUrls = supabaseAssignment.attachment_urls?.joinToString(","),
                                    teacherId = teacherEmail, // Use teacher email instead of UUID
                                    classId = localClassId
                                )
                                assignmentDao.insertAssignment(assignmentEntity)
                                totalAssignmentsSynced++
                                println("     - ✅ Synced assignment: ${assignmentEntity.title} for class ID ${localClassId} with teacher ${teacherEmail}")
                            } else {
                                println("     - ⚠️ Skipping assignment ${supabaseAssignment.title} - missing mapping:")
                                println("       - Class ID mapping: ${localClassId != null}")
                                println("       - Teacher email mapping: ${teacherEmail != null}")
                            }
                        } catch (e: Exception) {
                            println("     - ❌ Failed to sync assignment: ${supabaseAssignment.title} - ${e.message}")
                        }
                    }
                } else {
                    println("⚠️ Failed to get classes from Supabase: ${allClassesResult.exceptionOrNull()?.message}")
                }
            } else {
                println("⚠️ Failed to get assignments from Supabase: ${allAssignmentsResult.exceptionOrNull()?.message}")
            }
            
            println("✅ Synced $totalAssignmentsSynced assignments for current student's classes")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Failed to sync assignments for current student: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun syncClassSessionsForCurrentStudent(studentEmail: String): Result<Unit> {
        return try {
            println("🔄 Syncing class sessions for current student: $studentEmail")
            
            // Get current student's classes first
            val studentClasses = getClassesForStudent(studentEmail).first()
            if (studentClasses.isEmpty()) {
                println("⚠️ No classes found for student, skipping session sync")
                return Result.success(Unit)
            }
            
            val classIds = studentClasses.map { it.classId }
            println("📝 Found ${classIds.size} classes for student, syncing sessions for these classes only")
            
            var totalSessionsSynced = 0
            var totalAttendanceSynced = 0
            
            // Sync sessions for each of the student's classes
            classIds.forEach { classId ->
                val sessionsResult = supabaseApiService.getClassSessionsByClass(classId)
                if (sessionsResult.isSuccess) {
                    val sessions = sessionsResult.getOrNull() ?: emptyList()
                    println("   - Syncing ${sessions.size} sessions for class $classId")
                    
                    sessions.forEach { supabaseSession ->
                        // Generate sessionId if not provided (use hashCode of UUID or generate new one)
                        val sessionId = supabaseSession.session_id ?: supabaseSession.id?.hashCode() ?: java.util.UUID.randomUUID().hashCode()
                        
                        val sessionEntity = ClassSessionEntity(
                            sessionId = sessionId,
                            classId = supabaseSession.class_id.hashCode(),
                            scheduledDate = supabaseSession.session_date,
                            startTime = supabaseSession.start_time,
                            endTime = supabaseSession.end_time,
                            sessionCode = supabaseSession.session_code ?: "SESSION_${supabaseSession.id?.take(8) ?: "UNKNOWN"}",
                            status = supabaseSession.status ?: "scheduled"
                        )
                        classSessionDao.insertSession(sessionEntity)
                        totalSessionsSynced++
                        println("     - ✅ Synced session: ${sessionEntity.sessionCode} (${sessionEntity.scheduledDate}) - ID: $sessionId")
                        
                        // Sync attendance for this session (only for this student)
                        val attendanceResult = supabaseApiService.getAttendanceSessionsBySession(sessionId)
                        if (attendanceResult.isSuccess) {
                            val attendanceSessions = attendanceResult.getOrNull() ?: emptyList()
                            // Filter to only this student's attendance records
                            val studentAttendance = attendanceSessions.filter { it.student_email == studentEmail }
                            println("     - Syncing ${studentAttendance.size} attendance records for student in session $sessionId")
                            studentAttendance.forEach { supabaseAttendance ->
                                val attendanceEntity = AttendanceSessionEntity(
                                    sessionId = supabaseAttendance.session_id ?: 0,
                                    studentEmail = supabaseAttendance.student_email,
                                    attendanceStatus = supabaseAttendance.attendance_status,
                                    markedAt = supabaseAttendance.marked_at?.toLongOrNull() ?: System.currentTimeMillis(),
                                    markedVia = supabaseAttendance.marked_via
                                )
                                attendanceDao.insertSession(attendanceEntity)
                                totalAttendanceSynced++
                            }
                        } else {
                            println("     - ⚠️ Failed to sync attendance for session $sessionId: ${attendanceResult.exceptionOrNull()?.message}")
                        }
                    }
                } else {
                    println("   - ⚠️ Failed to get sessions for class $classId: ${sessionsResult.exceptionOrNull()?.message}")
                }
            }
            
            println("✅ Synced $totalSessionsSynced sessions and $totalAttendanceSynced attendance records for current student's classes")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Failed to sync class sessions for current student: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun syncAllDataFromSupabase(): Result<Unit> {
        return try {
            println("🔄 Starting full sync from Supabase...")
            
            // Sync all teachers with timeout
            val teachersResult = withTimeout(5000) {
                supabaseApiService.getAllTeachers()
            }
            if (teachersResult.isSuccess) {
                val teachers = teachersResult.getOrNull() ?: emptyList()
                println("📝 Syncing ${teachers.size} teachers to local database...")
                val teacherEntities = teachers.map { supabaseTeacher ->
                    println("   - Syncing teacher: ${supabaseTeacher.name} (${supabaseTeacher.email})")
                    TeacherEntity(
                        email = supabaseTeacher.email,
                        name = supabaseTeacher.name,
                        subject = supabaseTeacher.subject,
                        password = supabaseTeacher.password
                    )
                }
                teacherEntities.forEach { teacherDao.insertTeacher(it) }
                println("✅ Teachers synced successfully")
            } else {
                println("⚠️ Failed to sync teachers: ${teachersResult.exceptionOrNull()?.message}")
            }
            
            // Create a mapping from teacher UUID to email for class insertion
            val teacherUuidToEmailMap = mutableMapOf<String, String>()
            if (teachersResult.isSuccess) {
                val teachers = teachersResult.getOrNull() ?: emptyList()
                teachers.forEach { teacher ->
                    teacher.id?.let { uuid ->
                        teacherUuidToEmailMap[uuid] = teacher.email
                    }
                }
                println("📝 Created teacher UUID to email mapping: ${teacherUuidToEmailMap.size} entries")
            }
            
            // Sync all student groups with timeout
            val groupsResult = withTimeout(5000) {
                supabaseApiService.getAllStudentGroups()
            }
            if (groupsResult.isSuccess) {
                val groups = groupsResult.getOrNull() ?: emptyList()
                println("📝 Syncing ${groups.size} student groups to local database...")
                groups.forEach { supabaseGroup ->
                    try {
                        // Parse group_id to extract branch, division, batch, semester
                        // Format: IT_2024-2027_A_1_Sem3
                        val groupParts = supabaseGroup.group_id.split("_")
                        val branch = groupParts[0] // IT
                        val division = groupParts[2] // A
                        val batch = groupParts[3].toInt() // 1
                        val semester = groupParts[4].replace("Sem", "").toInt() // 3
                        
                        val groupEntity = StudentGroupEntity(
                            groupId = supabaseGroup.group_id,
                            branch = branch,
                            admissionYear = supabaseGroup.admission_year,
                            graduationYear = supabaseGroup.graduation_year,
                            division = division,
                            batch = batch,
                            semester = semester,
                            academicConfigId = 1 // Default config ID
                        )
                        studentGroupDao.insertGroup(groupEntity)
                        println("   - ✅ Synced student group: ${supabaseGroup.group_id}")
                    } catch (e: Exception) {
                        println("   - ❌ Failed to sync student group ${supabaseGroup.group_id}: ${e.message}")
                    }
                }
                println("✅ Student groups synced successfully")
            } else {
                println("⚠️ Failed to sync student groups: ${groupsResult.exceptionOrNull()?.message}")
            }
            
            // Sync all classes with timeout
            val classesResult = withTimeout(5000) {
supabaseApiService.getAllClasses()
            }
            if (classesResult.isSuccess) {
                val classes = classesResult.getOrNull() ?: emptyList()
                println("📝 Syncing ${classes.size} classes to local database...")
                classes.forEach { supabaseClass ->
                    println("   - Syncing class: ${supabaseClass.subject} | Teacher ID: ${supabaseClass.teacher_id}")
                    
                    // Convert teacher UUID to email
                    val teacherEmail = teacherUuidToEmailMap[supabaseClass.teacher_id]
                    if (teacherEmail == null) {
                        println("   - ⚠️ Teacher UUID ${supabaseClass.teacher_id} not found in mapping, skipping class")
                        return@forEach
                    }
                    
                    // Check if group exists locally
                    val groupExists = try {
                        studentDao.getStudentGroupById(supabaseClass.group_id) != null
                    } catch (e: Exception) {
                        false
                    }
                    
                    if (!groupExists) {
                        println("   - ⚠️ Group ${supabaseClass.group_id} not found locally, skipping class")
                        return@forEach
                    }
                    
                    val classId = supabaseClass.id?.hashCode() ?: 0
                    val classEntity = ClassEntity(
                        classId = classId, // Convert UUID to Int
                        teacherEmail = teacherEmail, // Use the mapped teacher email
                        subjectName = supabaseClass.subject,
                        groupId = supabaseClass.group_id,
                        classSchedule = buildScheduleFromComponents(
                            supabaseClass.class_days,
                            supabaseClass.start_time,
                            supabaseClass.end_time
                        )
                    )
                    val insertedId = teacherDao.insertClass(classEntity)
                    println("   - ✅ Synced class with local ID: $insertedId (UUID hashCode: $classId)")
                }
                println("✅ Classes synced successfully")
            } else {
                println("⚠️ Failed to sync classes: ${classesResult.exceptionOrNull()?.message}")
            }
            
            // Sync assignments for each teacher individually (more targeted approach)
            val allTeachers = teacherDao.getAllTeachers().first()
            println("📝 Syncing assignments for ${allTeachers.size} teachers individually...")
            allTeachers.forEach { teacher ->
                try {
                    val teacherAssignmentsResult = withTimeout(3000) {
                        syncAssignmentsForCurrentTeacher(teacher.email)
                    }
                    if (teacherAssignmentsResult.isSuccess) {
                        println("✅ Assignments synced for teacher: ${teacher.name}")
                    } else {
                        println("⚠️ Failed to sync assignments for teacher ${teacher.name}: ${teacherAssignmentsResult.exceptionOrNull()?.message}")
                    }
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    println("⚠️ Timeout syncing assignments for teacher ${teacher.name}")
                }
            }
            
            // Sync class sessions for each teacher individually (more targeted approach)
            println("📝 Syncing class sessions for each teacher individually...")
            allTeachers.forEach { teacher ->
                try {
                    val teacherSessionsResult = withTimeout(3000) {
                        syncClassSessionsForCurrentTeacher(teacher.email)
                    }
                    if (teacherSessionsResult.isSuccess) {
                        println("✅ Class sessions synced for teacher: ${teacher.name}")
                    } else {
                        println("⚠️ Failed to sync class sessions for teacher ${teacher.name}: ${teacherSessionsResult.exceptionOrNull()?.message}")
                    }
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    println("⚠️ Timeout syncing sessions for teacher ${teacher.name}")
                }
            }
            println("✅ Class sessions and attendance synced successfully")
            
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Full sync failed: ${e.message}")
            Result.failure(e)
        }
    }

    // Assignment Repository Methods - Reusing existing patterns from class and attendance operations
    
    suspend fun createAssignment(
        classId: Int,
        title: String,
        description: String,
        instructions: String? = null,
        assignmentType: String = "homework",
        maxPoints: Int = 100,
        dueDate: Long,
        allowLateSubmission: Boolean = false,
        latePenaltyPercentage: Int = 0,
        attachmentUrls: List<String>? = null
    ): Result<String> {
        return try {
            val teacher = teacherDao.getTeacher().first()
            if (teacher == null) {
                return Result.failure(Exception("No teacher found"))
            }
            
            // Validate that the class exists
            val classExists = teacherDao.getClassById(classId).first() != null
            if (!classExists) {
                // Show available classes for debugging
                val availableClasses = teacherDao.getClassesForTeacher(teacher.email).first()
                println("❌ Class with ID $classId does not exist")
                println("📋 Available classes for teacher ${teacher.email}:")
                availableClasses.forEach { classEntity ->
                    println("   - ID: ${classEntity.classId}, Subject: ${classEntity.subjectName}, Group: ${classEntity.groupId}")
                }
                return Result.failure(Exception("Class with ID $classId does not exist. Please create a class first."))
            }
            
            println("🔄 Creating professional assignment: title=$title, classId=$classId, teacher=${teacher.email}")
            
            val assignmentId = java.util.UUID.randomUUID().toString()
            
            // Create local assignment with new structure
            val assignment = AssignmentEntity(
                id = assignmentId,
                classId = classId,
                teacherId = teacher.email,
                title = title,
                description = description,
                instructions = instructions,
                assignmentType = assignmentType,
                maxPoints = maxPoints,
                dueDate = dueDate,
                allowLateSubmission = allowLateSubmission,
                latePenaltyPercentage = latePenaltyPercentage,
                attachmentUrls = attachmentUrls?.joinToString(",")
            )
            
            assignmentDao.insertAssignment(assignment)
            println("✅ Professional assignment created locally with ID: $assignmentId")
            
            // Sync to Supabase with timeout
            try {
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                
                val request = CreateAssignmentRequest(
                    class_id = classId.toString(),
                    title = title,
                    description = description,
                    instructions = instructions,
                    assignment_type = assignmentType,
                    max_points = maxPoints,
                    due_date = dateFormat.format(java.util.Date(dueDate)),
                    allow_late_submission = allowLateSubmission,
                    late_penalty_percentage = latePenaltyPercentage,
                    attachment_urls = attachmentUrls
                )
                
                val supabaseResult = withTimeout(10000) {
                    supabaseApiService.createAssignment(request)
                }
                
                if (supabaseResult.isSuccess) {
                    println("✅ Assignment synced to Supabase successfully")
                } else {
                    val error = supabaseResult.exceptionOrNull()
                    println("⚠️ Failed to sync assignment to Supabase: ${error?.message}")
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                println("⚠️ Supabase sync timed out - assignment saved locally only")
            } catch (e: Exception) {
                println("⚠️ Supabase sync failed: ${e.message} - assignment saved locally only")
            }
            
            Result.success(assignmentId)
        } catch (e: Exception) {
            println("❌ Error creating assignment: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getAssignmentsForClass(classId: Int): Flow<List<AssignmentEntity>> {
        return assignmentDao.getAssignmentsForClass(classId)
    }
    
    suspend fun getAssignmentsByTeacher(teacherEmail: String): Flow<List<AssignmentEntity>> {
        return assignmentDao.getAssignmentsByTeacher(teacherEmail)
    }
    
    // Professional Submission Management
    suspend fun submitAssignment(
        assignmentId: String,
        studentId: String,
        submissionText: String? = null,
        attachmentUrls: List<String>? = null
    ): Result<String> {
        return try {
            val assignment = assignmentDao.getAssignmentById(assignmentId).first()
            if (assignment == null) {
                return Result.failure(Exception("Assignment not found"))
            }
            
            val submissionId = java.util.UUID.randomUUID().toString()
            val currentTime = System.currentTimeMillis()
            val isLate = currentTime > assignment.dueDate
            
            // Create local submission
            val localSubmission = SubmissionEntity(
                id = submissionId,
                assignmentId = assignmentId,
                studentId = studentId,
                submissionText = submissionText,
                attachmentUrls = attachmentUrls?.joinToString(","),
                submittedAt = currentTime,
                isLate = isLate,
                maxPoints = assignment.maxPoints,
                status = "submitted"
            )
            
            submissionDao.insertSubmission(localSubmission)
            println("✅ Assignment submitted locally with ID: $submissionId")
            
            // Sync to Supabase
            try {
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                
                val supabaseRequest = SubmitAssignmentRequest(
                    assignment_id = assignmentId,
                    submission_text = submissionText,
                    attachment_urls = attachmentUrls
                )
                
                val supabaseResult = withTimeout(10000) {
                    supabaseApiService.submitAssignment(supabaseRequest)
                }
                
                if (supabaseResult.isSuccess) {
                    println("✅ Submission synced to Supabase successfully")
                } else {
                    println("⚠️ Failed to sync submission to Supabase: ${supabaseResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                println("⚠️ Supabase sync failed: ${e.message} - submission saved locally only")
            }
            
            Result.success(submissionId)
        } catch (e: Exception) {
            println("❌ Error submitting assignment: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun getSubmissionsForAssignment(assignmentId: String): Flow<List<SubmissionEntity>> {
        return submissionDao.getSubmissionsForAssignment(assignmentId)
    }
    
    suspend fun getSubmissionsByStudent(studentId: String): Flow<List<SubmissionEntity>> {
        return submissionDao.getSubmissionsByStudent(studentId)
    }
    
    // Function to create test classes for assignment testing
    suspend fun createTestClassesForTeacher(teacherEmail: String): Result<Unit> {
        return try {
            println("🔄 Creating test classes for teacher: $teacherEmail")
            
            // First, ensure the teacher exists in the database
            val existingTeacher = teacherDao.getTeacherByEmail(teacherEmail)
            if (existingTeacher == null) {
                println("📝 Teacher not found, creating teacher first...")
                val teacherEntity = TeacherEntity(
                    email = teacherEmail,
                    name = "Test Teacher",
                    subject = "Computer Science",
                    password = PasswordUtils.hashPassword("default123")
                )
                teacherDao.insertTeacher(teacherEntity)
                println("✅ Created teacher: $teacherEmail")
            } else {
                println("✅ Teacher already exists: $teacherEmail")
            }
            
            // Get all student groups to create classes for them
            val studentGroups = studentGroupDao.getAllGroups().first()
            println("📝 Found ${studentGroups.size} student groups")
            
            var classesCreated = 0
            
            for (group in studentGroups) {
                // Check if the group exists in the database
                val existingGroup = studentGroupDao.getGroupById(group.groupId).first()
                if (existingGroup == null) {
                    println("⚠️ Group ${group.groupId} not found in database, skipping class creation")
                    continue
                }
                
                // Create a class for each group with a unique subject name
                val classId = (teacherEmail + "TestClass" + group.groupId + System.currentTimeMillis()).hashCode()
                val subjectName = "Test Subject ${System.currentTimeMillis() % 10000} for ${group.groupId}"
                val classEntity = ClassEntity(
                    classId = classId,
                    subjectName = subjectName,
                    teacherEmail = teacherEmail,
                    groupId = group.groupId,
                    classSchedule = "Monday 09:00-10:30",
                    isActive = true
                )
                
                try {
                    // Check if class already exists
                    val existingClass = teacherDao.getClassById(classEntity.classId).first()
                    if (existingClass != null) {
                        println("⚠️ Class with ID ${classEntity.classId} already exists, skipping creation")
                        continue
                    }
                    
                    // Ensure teacher exists
                    val teacherInDb = teacherDao.getTeacherByEmail(teacherEmail).first()
                    if (teacherInDb == null) {
                        val teacherEntity = TeacherEntity(
                            email = teacherEmail,
                            name = "Test Teacher",
                            subject = "Computer Science",
                            password = PasswordUtils.hashPassword("default123")
                        )
                        try {
                            teacherDao.insertTeacher(teacherEntity)
                        } catch (e: Exception) {
                            // Teacher might already exist
                        }
                    }
                    
                    teacherDao.insertClass(classEntity)
                    classesCreated++
                    println("✅ Created test class: ${classEntity.subjectName} for group ${group.groupId}")
                } catch (e: Exception) {
                    // Class might already exist or have constraint issues, skip silently
                }
            }
            
            println("✅ Created $classesCreated test classes for teacher $teacherEmail")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Failed to create test classes: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun getSubmissionByStudent(assignmentId: String, studentId: String): SubmissionEntity? {
        return submissionDao.getSubmissionByStudent(assignmentId, studentId)
    }
    
    // Professional Grading System
    suspend fun gradeSubmission(
        submissionId: String,
        pointsEarned: Int,
        maxPoints: Int,
        grade: String,
        feedback: String? = null
    ): Result<Unit> {
        return try {
            val teacher = teacherDao.getTeacher().first()
            if (teacher == null) {
                return Result.failure(Exception("No teacher found"))
            }
            
            val submission = submissionDao.getSubmissionsForAssignment("").first().find { it.id == submissionId }
            if (submission == null) {
                return Result.failure(Exception("Submission not found"))
            }
            
            // Update local submission
            val updatedSubmission = submission.copy(
                pointsEarned = pointsEarned,
                maxPoints = maxPoints,
                grade = grade,
                feedback = feedback,
                gradedBy = teacher.email,
                gradedAt = System.currentTimeMillis(),
                status = "graded"
            )
            
            submissionDao.updateSubmission(updatedSubmission)
            println("✅ Submission graded locally")
            
            // Sync to Supabase
            try {
                val supabaseRequest = GradeAssignmentRequest(
                    submission_id = submissionId,
                    points_earned = pointsEarned,
                    max_points = maxPoints,
                    grade = grade,
                    feedback = feedback
                )
                
                val supabaseResult = withTimeout(10000) {
                    supabaseApiService.gradeSubmission(supabaseRequest)
                }
                
                if (supabaseResult.isSuccess) {
                    println("✅ Grade synced to Supabase successfully")
                } else {
                    println("⚠️ Failed to sync grade to Supabase: ${supabaseResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                println("⚠️ Supabase sync failed: ${e.message} - grade saved locally only")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Error grading submission: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun getAssignmentById(assignmentId: String): Flow<AssignmentEntity?> {
        return assignmentDao.getAssignmentById(assignmentId)
    }
    
    suspend fun getStudentsInClass(classId: Int): Flow<List<StudentEntity>> {
        return studentDao.getStudentsByClass(classId)
    }
    
    suspend fun getClassesForStudent(studentEmail: String): Flow<List<ClassEntity>> {
        return try {
            // First get the student to find their groupId
            val student = studentDao.getStudentByEmail(studentEmail).first()
            if (student != null && student.groupId != null) {
                // Then get all classes for that groupId
                teacherDao.getClassesByGroupId(student.groupId)
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        } catch (e: Exception) {
            println("❌ Error getting classes for student $studentEmail: ${e.message}")
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    suspend fun getAllAssignments(): Flow<List<AssignmentEntity>> {
        return assignmentDao.getAllAssignments()
    }
    
    suspend fun getAssignmentsForStudent(studentEmail: String): Flow<List<AssignmentEntity>> {
        return try {
            // Get student's classes first
            val classes = getClassesForStudent(studentEmail).first()
            val classIds = classes.map { it.classId }
            
            if (classIds.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                // Get assignments for all the student's classes
                assignmentDao.getAssignmentsByClassIds(classIds)
            }
        } catch (e: Exception) {
            println("❌ Error getting assignments for student $studentEmail: ${e.message}")
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    // Submission Repository Methods - Reusing patterns from attendance operations
    
    suspend fun createSubmission(
        assignmentId: String,
        studentEmail: String,
        fileUrl: String?,
        status: String = "submitted"
    ): Result<String> {
        return try {
            println("🔄 Creating submission: assignmentId=$assignmentId, student=$studentEmail")
            
            // First, save to local database
            val submissionEntity = SubmissionEntity(
                assignmentId = assignmentId,
                studentId = studentEmail,
                attachmentUrls = fileUrl,
                submittedAt = if (status == "submitted" || status == "late") System.currentTimeMillis() else System.currentTimeMillis(),
                status = status
            )
            submissionDao.insertSubmission(submissionEntity)
            println("✅ Submission saved locally")
            
            // Then, sync to Supabase
            val request = CreateSubmissionRequest(
                assignment_id = assignmentId,
                student_email = studentEmail,
                file_url = fileUrl
            )
            val supabaseResult = supabaseApiService.createSubmission(request)
            if (supabaseResult.isSuccess) {
                println("✅ Submission synced to Supabase successfully")
            } else {
                println("⚠️ Failed to sync submission to Supabase: ${supabaseResult.exceptionOrNull()?.message}")
            }
            
            Result.success("Submission created successfully")
        } catch (e: Exception) {
            println("❌ Error creating submission: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateSubmission(submission: SubmissionEntity): Result<Unit> {
        return try {
            submissionDao.updateSubmission(submission)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun gradeSubmission(
        assignmentId: String,
        studentId: String,
        marksObtained: Int,
        feedback: String,
        gradedBy: String
    ): Result<Unit> {
        return try {
            val submission = submissionDao.getSubmissionByStudent(assignmentId, studentId)
            if (submission != null) {
                val updatedSubmission = submission.copy(
                    pointsEarned = marksObtained,
                    feedback = feedback,
                    gradedAt = System.currentTimeMillis(),
                    gradedBy = gradedBy,
                    updatedAt = System.currentTimeMillis()
                )
                submissionDao.updateSubmission(updatedSubmission)
                println("✅ Graded submission for $studentId with $marksObtained marks")
                Result.success(Unit)
            } else {
                println("❌ Submission not found for $studentId")
                Result.failure(Exception("Submission not found"))
            }
        } catch (e: Exception) {
            println("❌ Error grading submission: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun syncAllDataForTeacher(teacherEmail: String): Result<Unit> {
        return try {
            // Sync teacher profile
            syncTeacherFromSupabase(teacherEmail)
            
            // Sync classes
            syncClassesFromSupabase(teacherEmail)
            
            // For each class, sync class sessions and attendance
            val classesResult = supabaseApiService.getClassesByTeacher(teacherEmail)
            classesResult.getOrNull()?.forEach { supabaseClass ->
                supabaseClass.id?.let { classId ->
                    // Sync class sessions
                    val sessionsResult = supabaseApiService.getClassSessionsByClass(classId.hashCode())
                    sessionsResult.getOrNull()?.forEach { supabaseSession ->
                        // Generate sessionId if not provided (use hashCode of UUID or generate new one)
                        val sessionId = supabaseSession.session_id ?: supabaseSession.id?.hashCode() ?: java.util.UUID.randomUUID().hashCode()
                        
                        val sessionEntity = ClassSessionEntity(
                            sessionId = sessionId,
                            classId = supabaseSession.class_id.hashCode(),
                            scheduledDate = supabaseSession.session_date,
                            startTime = supabaseSession.start_time,
                            endTime = supabaseSession.end_time,
                            sessionCode = supabaseSession.session_code ?: "SESSION_${supabaseSession.id?.take(8) ?: "UNKNOWN"}",
                            status = supabaseSession.status ?: "scheduled"
                        )
                        classSessionDao.insertSession(sessionEntity)
                        
                        // Sync attendance for this session
                        syncAttendanceSessionsFromSupabase(sessionId)
                    }
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}