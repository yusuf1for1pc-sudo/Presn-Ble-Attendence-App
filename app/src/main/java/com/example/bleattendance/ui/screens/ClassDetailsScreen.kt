package com.example.bleattendance.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bleattendance.ui.viewmodels.ClassDetailsViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailsScreen(
    navController: NavController,
    classId: Int,
    subjectName: String,
    teacherName: String,
    groupId: String
) {
    // Get application context
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    
    // Create ViewModel inside Composable with SavedStateHandle containing classId
    val viewModel: ClassDetailsViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val savedStateHandle = SavedStateHandle().apply {
                    set("classId", classId)
                }
                return ClassDetailsViewModel(
                    application = application,
                    savedStateHandle = savedStateHandle
                ) as T
            }
        }
    )
    
    // Get attendance data from ViewModel
    val classAttendanceStats = viewModel.classAttendanceStats
    val classRecentSessions = viewModel.classRecentSessions
    val exportStatus = viewModel.exportStatus
    
    
    // Debug logging only (removed automatic sync to prevent continuous syncing)
    LaunchedEffect(classId, subjectName, teacherName, groupId) {
        println("📱 ClassDetailsScreen: classId = $classId")
        println("📱 ClassDetailsScreen: subjectName = $subjectName")
        println("📱 ClassDetailsScreen: teacherName = $teacherName")
        println("📱 ClassDetailsScreen: groupId = $groupId")
        
        // Note: Removed automatic sync to prevent continuous syncing
        // Users can manually refresh using the refresh button if needed
    }
    
    // Create class details with passed data
    val classDetails = remember(subjectName, teacherName, groupId, classAttendanceStats, classRecentSessions) {
        ClassDetailsData(
            subjectName = subjectName,
            teacherName = teacherName,
            groupId = groupId,
            totalLectures = classAttendanceStats.totalLectures,
            attendedLectures = classAttendanceStats.attendedLectures,
            missedLectures = classAttendanceStats.missedLectures,
            attendancePercentage = classAttendanceStats.attendancePercentage,
            recentSessions = classRecentSessions.map { session ->
                SessionData(
                    date = session.date,
                    time = session.time,
                    status = session.status.lowercase(),
                    sessionCode = session.sessionCode,
                    attendedStudents = session.attendedStudents.map { student ->
                        StudentAttendanceInfo(
                            name = student.name,
                            rollNumber = student.rollNumber,
                            email = student.email,
                            attendanceStatus = student.attendanceStatus
                        )
                    }
                )
            }
        )
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
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with back button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                                IconButton(
                        onClick = { navController.popBackStack() },
                                    modifier = Modifier
                                        .background(
                                            color = Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                                        tint = Color.White
                                    )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                            
                            Column {
                                Text(
                            text = classDetails.subjectName,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                            )
                                )
                                Text(
                            text = classDetails.teacherName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                        )
                    }
                }
                
                IconButton(
                    onClick = { /* Navigate to settings */ },
                    modifier = Modifier
                        .background(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Attendance Statistics Card
            AttendanceStatsCard(classDetails = classDetails)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Recent Sessions Header with Export Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Sessions",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                
                // Export Button
                IconButton(
                    onClick = {
                        viewModel.exportSessionsToCSV()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export Sessions",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Export Status
            exportStatus?.let { status ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (status.contains("failed", ignoreCase = true)) 
                            Color(0xFFF44336).copy(alpha = 0.2f) 
                        else 
                            Color(0xFF4CAF50).copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Sessions List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(classDetails.recentSessions) { session ->
                    SessionCard(session = session, navController = navController)
                }
            }
        }
    }
}

@Composable
private fun AttendanceStatsCard(classDetails: ClassDetailsData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Attendance Statistics",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Progress Ring
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                CircularProgressIndicator(
                    progress = classDetails.attendancePercentage / 100f,
                    modifier = Modifier.fillMaxSize(),
                    color = when {
                        classDetails.attendancePercentage >= 75f -> Color(0xFF4CAF50)
                        classDetails.attendancePercentage >= 60f -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    },
                    strokeWidth = 8.dp,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
                
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                        text = "${classDetails.attendancePercentage.toInt()}%",
                        style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                    )
                                    Text(
                        text = "Attendance",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Statistics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Total",
                    value = classDetails.totalLectures.toString(),
                    color = Color.White.copy(alpha = 0.8f)
                )
                StatItem(
                    label = "Attended",
                    value = classDetails.attendedLectures.toString(),
                    color = Color(0xFF4CAF50)
                )
                StatItem(
                    label = "Missed",
                    value = classDetails.missedLectures.toString(),
                    color = Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White.copy(alpha = 0.7f)
            )
        )
    }
}

@Composable
private fun SessionCard(
    session: SessionData,
    navController: NavController
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Navigate to session details screen
                navController.navigate("session_details/${session.date}/${session.time}/${session.sessionCode}/${session.status}")
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = session.date,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                )
                Text(
                    text = session.time,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )
                Text(
                    text = "Code: ${session.sessionCode}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.5f)
                    )
                )
                Text(
                    text = "Attended: ${session.attendedStudents.size} students",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.8f)
                    )
                )
            }
            
            // Status Chip
            val (backgroundColor, textColor, icon) = when (session.status) {
                "present" -> Triple(
                    Color(0xFF4CAF50).copy(alpha = 0.2f),
                    Color(0xFF4CAF50),
                    Icons.Default.CheckCircle
                )
                "absent" -> Triple(
                    Color(0xFFF44336).copy(alpha = 0.2f),
                    Color(0xFFF44336),
                    Icons.Default.Close
                )
                else -> Triple(
                    Color(0xFFFF9800).copy(alpha = 0.2f),
                    Color(0xFFFF9800),
                    Icons.Default.Info
                )
            }
            
            Card(
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = session.status,
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = session.status.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = textColor,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}



// Data classes
data class ClassDetailsData(
    val subjectName: String,
    val teacherName: String,
    val groupId: String,
    val totalLectures: Int,
    val attendedLectures: Int,
    val missedLectures: Int,
    val attendancePercentage: Float,
    val recentSessions: List<SessionData>
)

data class SessionData(
    val date: String,
    val time: String,
    val status: String,
    val sessionCode: String,
    val attendedStudents: List<StudentAttendanceInfo> = emptyList()
)

data class StudentAttendanceInfo(
    val name: String,
    val rollNumber: String,
    val email: String,
    val attendanceStatus: String
)