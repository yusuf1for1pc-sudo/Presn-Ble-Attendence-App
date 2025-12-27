package com.example.bleattendance.utils

import java.util.Calendar
import java.util.Date

object GroupUtils {
    
    // Default Engineering configuration
    private const val DEFAULT_DURATION_YEARS = 4
    private const val DEFAULT_TOTAL_SEMESTERS = 8
    private const val DEFAULT_SEMESTER_DURATION_MONTHS = 6
    
    /**
     * Calculate current semester based on admission year and current date
     * Indian Academic Calendar:
     * - Semester 1: July to December (6 months)
     * - Semester 2: January to May/June (6 months)
     * - Academic year starts in July
     */
    fun calculateCurrentSemester(
        admissionYear: Int,
        durationYears: Int = DEFAULT_DURATION_YEARS,
        totalSemesters: Int = DEFAULT_TOTAL_SEMESTERS,
        semesterDurationMonths: Int = DEFAULT_SEMESTER_DURATION_MONTHS
    ): Int {
        val currentDate = Calendar.getInstance()
        val currentYear = currentDate.get(Calendar.YEAR)
        val currentMonth = currentDate.get(Calendar.MONTH) + 1 // Calendar months are 0-based
        
        // Calculate academic year (starts in July)
        val academicYear = if (currentMonth >= 7) {
            currentYear // July-December: same year
        } else {
            currentYear - 1 // January-June: previous academic year
        }
        
        // Calculate years since admission (academic years)
        val academicYearsSinceAdmission = academicYear - admissionYear
        
        // Determine current semester based on month
        val currentSemesterInYear = if (currentMonth >= 7 && currentMonth <= 12) {
            1 // July-December: Semester 1
        } else {
            2 // January-June: Semester 2
        }
        
        // Calculate total semester number
        val totalSemesterNumber = (academicYearsSinceAdmission * 2) + currentSemesterInYear
        
        // Ensure semester is within valid range
        return totalSemesterNumber.coerceIn(1, totalSemesters)
    }
    
    /**
     * Calculate current semester with specific date (for testing)
     */
    fun calculateCurrentSemesterForDate(
        admissionYear: Int,
        testDate: Date,
        durationYears: Int = DEFAULT_DURATION_YEARS,
        totalSemesters: Int = DEFAULT_TOTAL_SEMESTERS,
        semesterDurationMonths: Int = DEFAULT_SEMESTER_DURATION_MONTHS
    ): Int {
        val calendar = Calendar.getInstance()
        calendar.time = testDate
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // Calendar months are 0-based
        
        // Calculate academic year (starts in July)
        val academicYear = if (currentMonth >= 7) {
            currentYear // July-December: same year
        } else {
            currentYear - 1 // January-June: previous academic year
        }
        
        // Calculate years since admission (academic years)
        val academicYearsSinceAdmission = academicYear - admissionYear
        
        // Determine current semester based on month
        val currentSemesterInYear = if (currentMonth >= 7 && currentMonth <= 12) {
            1 // July-December: Semester 1
        } else {
            2 // January-June: Semester 2
        }
        
        // Calculate total semester number
        val totalSemesterNumber = (academicYearsSinceAdmission * 2) + currentSemesterInYear
        
        // Ensure semester is within valid range
        return totalSemesterNumber.coerceIn(1, totalSemesters)
    }
    
    /**
     * Generate group ID in format: Branch_AdmissionYear-GraduationYear_Division_Batch_SemX
     * Example: CSE_2024-2028_A_1_Sem3
     */
    fun generateGroupId(
        branch: String,
        admissionYear: Int,
        graduationYear: Int,
        division: String,
        batch: Int,
        semester: Int
    ): String {
        val batchStr = if (batch == 0) "ALL" else batch.toString()
        return "${branch}_${admissionYear}-${graduationYear}_${division}_${batchStr}_Sem${semester}"
    }
    
    /**
     * Calculate graduation year based on admission year and duration
     */
    fun calculateGraduationYear(admissionYear: Int, durationYears: Int = DEFAULT_DURATION_YEARS): Int {
        return admissionYear + durationYears
    }
    
    /**
     * Parse group ID to extract components
     */
    fun parseGroupId(groupId: String): GroupComponents? {
        return try {
            // Format: Branch_AdmissionYear-GraduationYear_Division_Batch_SemX
            // Handle cases where branch name contains underscores (e.g., "Information_Technology")
            val parts = groupId.split("_")
            if (parts.size < 4) return null
            
            // Find the year range part (contains "-")
            var yearRangeIndex = -1
            for (i in parts.indices) {
                if (parts[i].contains("-")) {
                    yearRangeIndex = i
                    break
                }
            }
            if (yearRangeIndex == -1) return null
            
            // Branch is everything before the year range
            val branch = parts.subList(0, yearRangeIndex).joinToString("_")
            
            // Parse year range
            val yearRange = parts[yearRangeIndex].split("-")
            if (yearRange.size != 2) return null
            
            val admissionYear = yearRange[0].toInt()
            val graduationYear = yearRange[1].toInt()
            
            // Division and batch/semester are after the year range
            if (parts.size < yearRangeIndex + 3) return null
            val division = parts[yearRangeIndex + 1]
            
            // Find the semester part (contains "Sem")
            var semesterIndex = -1
            for (i in yearRangeIndex + 2 until parts.size) {
                if (parts[i].startsWith("Sem")) {
                    semesterIndex = i
                    break
                }
            }
            if (semesterIndex == -1) return null
            
            // Batch is everything between division and semester
            val batch = if (semesterIndex == yearRangeIndex + 2) {
                parts[yearRangeIndex + 2].replace("Sem", "").toIntOrNull() ?: return null
            } else {
                parts[yearRangeIndex + 2].toIntOrNull() ?: return null
            }
            
            val semester = parts[semesterIndex].replace("Sem", "").toInt()
            
            GroupComponents(
                branch = branch,
                admissionYear = admissionYear,
                graduationYear = graduationYear,
                division = division,
                batch = batch,
                semester = semester
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Check if a student belongs to a specific group
     */
    fun isStudentInGroup(
        studentBranch: String,
        studentAdmissionYear: Int,
        studentDivision: String,
        studentBatch: Int,
        studentCurrentSemester: Int,
        targetGroupId: String
    ): Boolean {
        val targetGroup = parseGroupId(targetGroupId) ?: return false
        
        return studentBranch == targetGroup.branch &&
                studentAdmissionYear == targetGroup.admissionYear &&
                studentDivision == targetGroup.division &&
                studentBatch == targetGroup.batch &&
                studentCurrentSemester == targetGroup.semester
    }
    
    /**
     * Get all possible group IDs for a student based on their details
     * This includes all semesters from 1 to current semester
     */
    fun getAllStudentGroupIds(
        branch: String,
        admissionYear: Int,
        graduationYear: Int,
        division: String,
        batch: Int,
        currentSemester: Int
    ): List<String> {
        return (1..currentSemester).map { semester ->
            generateGroupId(branch, admissionYear, graduationYear, division, batch, semester)
        }
    }
    
    /**
     * Validate if group ID format is correct
     */
    fun isValidGroupId(groupId: String): Boolean {
        return parseGroupId(groupId) != null
    }
    
    /**
     * Get semester start and end dates for Indian academic calendar
     */
    fun getSemesterDates(year: Int, semester: Int): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        
        return if (semester == 1) {
            // Semester 1: July to December
            calendar.set(year, 6, 1) // July 1st (month is 0-based)
            val startDate = calendar.time
            
            calendar.set(year, 11, 31) // December 31st
            val endDate = calendar.time
            
            Pair(startDate, endDate)
        } else {
            // Semester 2: January to May
            calendar.set(year + 1, 0, 1) // January 1st of next year
            val startDate = calendar.time
            
            calendar.set(year + 1, 4, 31) // May 31st of next year
            val endDate = calendar.time
            
            Pair(startDate, endDate)
        }
    }
    
    /**
     * Check if current date falls within a specific semester
     */
    fun isDateInSemester(date: Date, year: Int, semester: Int): Boolean {
        val (startDate, endDate) = getSemesterDates(year, semester)
        return date >= startDate && date <= endDate
    }
    
    /**
     * Get current academic year based on date
     */
    fun getCurrentAcademicYear(date: Date = Date()): Int {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        
        return if (currentMonth >= 7) {
            currentYear // July-December: same year
        } else {
            currentYear - 1 // January-June: previous academic year
        }
    }
    
    /**
     * Get current semester number (1 or 2) based on date
     */
    fun getCurrentSemesterInYear(date: Date = Date()): Int {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        
        return if (currentMonth >= 7 && currentMonth <= 12) {
            1 // July-December: Semester 1
        } else {
            2 // January-June: Semester 2
        }
    }
}

data class GroupComponents(
    val branch: String,
    val admissionYear: Int,
    val graduationYear: Int,
    val division: String,
    val batch: Int,
    val semester: Int
)
