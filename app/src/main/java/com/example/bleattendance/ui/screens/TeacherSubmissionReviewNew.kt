package com.example.bleattendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.bleattendance.data.db.AssignmentEntity
import com.example.bleattendance.data.db.StudentEntity
import com.example.bleattendance.data.db.SubmissionEntity
import com.example.bleattendance.ui.components.GradingDialog
import com.example.bleattendance.ui.components.GlassCard
import com.example.bleattendance.ui.viewmodels.TeacherSubmissionReviewViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherSubmissionReviewNew(
    navController: NavController,
    assignmentId: String,
    teacherEmail: String
) {
    val context = LocalContext.current
    val viewModel = remember { TeacherSubmissionReviewViewModel(context.applicationContext as android.app.Application) }
    val assignment by viewModel.assignment.collectAsState()
    val students by viewModel.students.collectAsState()
    val submissions by viewModel.submissions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val showGradingDialog by viewModel.showGradingDialog.collectAsState()
    val gradingStudentEmail by viewModel.gradingStudentEmail.collectAsState()
    val gradingSubmission by viewModel.gradingSubmission.collectAsState()

    LaunchedEffect(assignmentId, teacherEmail) {
        viewModel.loadAssignmentData(assignmentId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF000000),
                        Color(0xFF0A0A0A),
                        Color(0xFF111111),
                        Color(0xFF1A1A1A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            TopAppBar(
                title = {
                    Text(
                        text = "Submissions Review",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E293B)
                )
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF3B82F6)
                    )
                }
            } else if (assignment == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Assignment not found",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Assignment Info
                    AssignmentInfoCard(assignment = assignment!!)

                    // Submission Statistics
                    SubmissionStatsCard(
                        totalStudents = students.size,
                        submittedCount = submissions.count { it.status == "submitted" || it.status == "late" },
                        gradedCount = submissions.count { it.pointsEarned != null }
                    )

                    // Students List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(students) { student ->
                            val submission = submissions.find { it.studentId == student.email }
                            StudentSubmissionCard(
                                student = student,
                                submission = submission,
                                assignment = assignment!!,
                                onDownload = {
                                    // TODO: Download submission file
                                },
                                onGrade = {
                                    viewModel.gradeSubmission(student.email, submission)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Grading Dialog
        if (showGradingDialog && gradingStudentEmail != null) {
            val student = students.find { it.email == gradingStudentEmail }
            GradingDialog(
                studentName = student?.name ?: "Unknown Student",
                maxMarks = assignment?.maxPoints ?: 100,
                currentMarks = gradingSubmission?.pointsEarned,
                currentFeedback = gradingSubmission?.feedback,
                onDismiss = {
                    viewModel.dismissGradingDialog()
                },
                onGrade = { marks, feedback ->
                    // Find the submission for the grading student
                    val submission = submissions.find { it.studentId == gradingStudentEmail }
                    if (submission != null) {
                        viewModel.submitGrade(
                            submissionId = submission.id,
                            pointsEarned = marks,
                            maxPoints = assignment?.maxPoints ?: 100,
                            grade = "", // Will be calculated from percentage
                            feedback = feedback
                        )
                    }
                }
            )
        }

        // Error Message
        errorMessage?.let { error ->
            LaunchedEffect(error) {
                // Show error snackbar or dialog
                viewModel.clearError()
            }
        }
    }
}

@Composable
private fun AssignmentInfoCard(
    assignment: AssignmentEntity
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = assignment.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            assignment.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.8f)
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Due: ${formatDate(assignment.dueDate)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.6f)
                    )
                )
                Text(
                    text = "Max: ${assignment.maxPoints ?: 100} points",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFA3E635),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
private fun SubmissionStatsCard(
    totalStudents: Int,
    submittedCount: Int,
    gradedCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            title = "Total Students",
            value = totalStudents.toString(),
            icon = Icons.Default.Person,
            color = Color(0xFF3B82F6),
            modifier = Modifier.weight(1f)
        )

        StatCard(
            title = "Submitted",
            value = submittedCount.toString(),
            icon = Icons.Default.CheckCircle,
            color = Color(0xFF10B981),
            modifier = Modifier.weight(1f)
        )

        StatCard(
            title = "Graded",
            value = gradedCount.toString(),
            icon = Icons.Default.Edit,
            color = Color(0xFFF59E0B),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.7f)
                )
            )
        }
    }
}

@Composable
private fun StudentSubmissionCard(
    student: StudentEntity,
    submission: SubmissionEntity?,
    assignment: AssignmentEntity,
    onDownload: () -> Unit,
    onGrade: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Student Info and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = student.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = student.email,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }

                // Status Badge
                SubmissionStatusBadge(
                    submission = submission,
                    dueDate = assignment.dueDate
                )
            }

            // Submission Details
            if (submission != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Submitted: ${formatDate(submission.submittedAt ?: 0)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        )
                        
                        // Show marks if graded
                        submission.pointsEarned?.let { points ->
                            Text(
                                text = "Points: $points/${assignment.maxPoints ?: 100}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFFA3E635),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (submission.attachmentUrls != null) {
                            IconButton(
                                onClick = onDownload
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Download",
                                    tint = Color(0xFF3B82F6)
                                )
                            }
                        }

                        if (submission.status == "submitted" || submission.status == "late") {
                            IconButton(
                                onClick = onGrade
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Grade",
                                    tint = Color(0xFFA3E635)
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "No submission yet",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

@Composable
private fun SubmissionStatusBadge(
    submission: SubmissionEntity?,
    dueDate: Long
) {
    val currentTime = System.currentTimeMillis()
    val isOverdue = currentTime > dueDate

    val (status, color, icon) = when {
        submission == null -> Triple("Not Submitted", Color(0xFFEF4444), Icons.Default.Warning)
        submission.status == "submitted" && !isOverdue -> Triple("Submitted", Color(0xFF10B981), Icons.Default.CheckCircle)
        submission.status == "late" || (submission.status == "submitted" && isOverdue) -> Triple("Late", Color(0xFFF59E0B), Icons.Default.Info)
        else -> Triple("Not Submitted", Color(0xFFEF4444), Icons.Default.Warning)
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = color,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
