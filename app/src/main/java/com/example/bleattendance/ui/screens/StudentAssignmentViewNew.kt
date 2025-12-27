package com.example.bleattendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.bleattendance.data.db.AssignmentEntity
import com.example.bleattendance.data.db.SubmissionEntity
import com.example.bleattendance.ui.components.FileUploadComponent
import com.example.bleattendance.ui.viewmodels.StudentAssignmentViewViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentAssignmentViewNew(
    navController: NavController,
    assignmentId: String,
    studentEmail: String
) {
    val context = LocalContext.current
    val viewModel = remember { StudentAssignmentViewViewModel(context.applicationContext as android.app.Application) }
    val assignment by viewModel.assignment.collectAsState()
    val submission by viewModel.submission.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    LaunchedEffect(assignmentId, studentEmail) {
        viewModel.loadAssignment(assignmentId, studentEmail)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            TopAppBar(
                title = {
                    Text(
                        text = "Assignment Details",
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
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Assignment Header
                    AssignmentHeaderCard(assignment = assignment!!)

                    // Reference Materials (Teacher's Files)
                    if (!assignment!!.attachmentUrls.isNullOrBlank()) {
                        ReferenceMaterialCard(assignment = assignment!!)
                    }

                    // My Work Section
                    MyWorkSection(
                        assignment = assignment!!,
                        submission = submission,
                        onUploadFile = { fileUrl ->
                            viewModel.submitAssignment(
                                assignmentId = assignmentId,
                                studentId = studentEmail,
                                attachmentUrls = listOf(fileUrl)
                            )
                        }
                    )

                    // Submission Status
                    SubmissionStatusCard(
                        assignment = assignment!!,
                        submission = submission
                    )

                    // Error/Success Messages
                    errorMessage?.let { error ->
                        ErrorCard(message = error)
                    }

                    successMessage?.let { success ->
                        SuccessCard(message = success)
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignmentHeaderCard(
    assignment: AssignmentEntity
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = assignment.title,
                style = MaterialTheme.typography.headlineSmall.copy(
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Due: ${formatDate(assignment.dueDate)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

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
private fun ReferenceMaterialCard(
    assignment: AssignmentEntity
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Reference Materials",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F172A)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(24.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Question Paper - ${assignment.title}.pdf",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            text = "PDF Document",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        )
                    }
                    TextButton(
                        onClick = { /* TODO: Download file */ }
                    ) {
                        Text(
                            text = "Download",
                            color = Color(0xFF3B82F6)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MyWorkSection(
    assignment: AssignmentEntity,
    submission: SubmissionEntity?,
    onUploadFile: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "My Work",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            // Upload Section
            FileUploadComponent(
                onFileSelected = onUploadFile,
                label = "Upload Your Work (PDF/DOC/Images)",
                acceptedFileTypes = "PDF, DOC, Images"
            )

            // Submitted Files
            if (submission != null && !submission.attachmentUrls.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F172A)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(24.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "My Submission.pdf",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "Submitted: ${formatDate(submission.submittedAt ?: 0)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            )
                        }
                        TextButton(
                            onClick = { /* TODO: Download/view file */ }
                        ) {
                            Text(
                                text = "View",
                                color = Color(0xFF3B82F6)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubmissionStatusCard(
    assignment: AssignmentEntity,
    submission: SubmissionEntity?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Submission Status",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            val currentTime = System.currentTimeMillis()
            val isOverdue = currentTime > assignment.dueDate

            val (status, color, icon, message) = when {
                submission == null -> {
                    if (isOverdue) {
                        Quadruple("Not Submitted", Color(0xFFEF4444), Icons.Default.Warning, "Assignment is overdue")
                    } else {
                        Quadruple("Not Submitted", Color(0xFFF59E0B), Icons.Default.Info, "Assignment pending")
                    }
                }
                submission.status == "submitted" && !isOverdue -> {
                    Quadruple("Submitted on Time", Color(0xFF10B981), Icons.Default.CheckCircle, "Great job! Submitted before deadline")
                }
                submission.status == "late" || (submission.status == "submitted" && isOverdue) -> {
                    Quadruple("Submitted Late", Color(0xFFF59E0B), Icons.Default.Info, "Submitted after deadline")
                }
                else -> {
                    Quadruple("Not Submitted", Color(0xFFEF4444), Icons.Default.Warning, "Assignment not submitted")
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = color,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
            }

            // Show marks if graded
            submission?.pointsEarned?.let { points ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFFA3E635),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Points: $points/${assignment.maxPoints ?: 100}",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color(0xFFA3E635),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                submission.feedback?.let { feedback ->
                    Text(
                        text = "Feedback: $feedback",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)
        )
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFFEF4444)
            ),
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun SuccessCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF10B981).copy(alpha = 0.1f)
        )
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF10B981)
            ),
            modifier = Modifier.padding(12.dp)
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

// Helper data class for multiple return values
private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
