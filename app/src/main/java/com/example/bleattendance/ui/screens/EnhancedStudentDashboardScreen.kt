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
import com.example.bleattendance.ui.viewmodels.EnhancedStudentDashboardViewModel
import com.example.bleattendance.ui.viewmodels.ClassCardData
import com.example.bleattendance.ui.viewmodels.AttendanceStatus
import com.example.bleattendance.ui.components.AttendanceDialog
import com.example.bleattendance.ui.components.BiometricAuthDialog
import com.example.bleattendance.ui.components.GlassCard
import com.example.bleattendance.ui.components.ManualCodeEntryDialog
import com.example.bleattendance.ui.components.PermissionStatusCard
import com.example.bleattendance.ui.components.TimetableView
import com.example.bleattendance.ui.components.WeekTimetableView
import com.example.bleattendance.ui.components.ClassScheduleData
import com.example.bleattendance.utils.BlePermissionChecker
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedStudentDashboardScreen(
    navController: NavController,
    viewModel: EnhancedStudentDashboardViewModel = viewModel()
) {
    val selectedTab by viewModel.selectedTab
    val tabs = listOf("Today's Classes", "All Classes")
    
    // Get real data from ViewModel
    val classes by viewModel.classes.collectAsState(initial = emptyList())
    val studentProfile by viewModel.studentProfile.collectAsState(initial = null)
    
    // BLE scanning states
    val isScanning = viewModel.isScanning
    val showAttendanceDialog = viewModel.showAttendanceDialog
    val showBiometricAuthDialog = viewModel.showBiometricAuthDialog
    val detectedSessionCode = viewModel.detectedSessionCode
    val currentSubjectName = viewModel.currentSubjectName
    val currentTeacherName = viewModel.currentTeacherName
    
    // BLE permission checker
    val context = LocalContext.current
    val blePermissionChecker = remember { BlePermissionChecker(context) }
    val bleStatus = remember { blePermissionChecker.getBleStatus() }
    
    // For Today's Classes tab, we'll pass all classes to TimetableView and let it handle day selection
    // For All Classes tab, we'll pass all classes to WeekTimetableView
    val currentClasses = classes
    
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault()).format(Date()),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Debug button
                    IconButton(
                        onClick = { viewModel.debugData() },
                        modifier = Modifier
                            .background(
                                color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Debug",
                            tint = Color(0xFFEF4444)
                        )
                    }
                    
                    // Refresh button
                    IconButton(
                        onClick = { viewModel.refreshClasses() },
                        modifier = Modifier
                            .background(
                                color = Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Classes",
                            tint = Color.White
                        )
                    }
                    
                    // Assignments button
                    IconButton(
                        onClick = { 
                            // Navigate to assignments
                            navController.navigate("student_assignments")
                        },
                        modifier = Modifier
                            .background(
                                color = Color(0xFF3B82F6).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Assignments",
                            tint = Color(0xFF3B82F6)
                        )
                    }
                    
                    // Settings button
                    IconButton(
                        onClick = { 
                            // Navigate to student settings
                            navController.navigate("student_settings")
                        },
                        modifier = Modifier
                            .background(
                                color = Color(0xFFA3E635).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFFA3E635)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Tab Selector
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = selectedTab == index
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) Color(0xFF4CAF50) else Color.Transparent
                                )
                                .clickable { viewModel.updateSelectedTab(index) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Classes List
            if (viewModel.isLoading) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFA3E635)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading classes...",
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            } else if (currentClasses.isEmpty()) {
                EmptyStateCard()
            } else {
                // Show timetable views based on selected tab
                if (selectedTab == 0) {
                    // Today's Classes - Show Timetable View with day selection
                    TimetableView(
                        classes = convertToScheduleData(classes), // Pass all classes, let TimetableView handle day filtering
                        selectedDay = getCurrentDayIndex(), // Start with today's day selected
                        onClassClick = { classData ->
                            navController.navigate("class_options/${classData.classId}/${classData.subjectName}/${classData.teacherName}/${classData.groupId}/${classData.teacherEmail}")
                        }
                    )
                } else {
                    // All Classes - Show Week Timetable View
                    WeekTimetableView(
                        classes = convertToScheduleData(classes),
                        onClassClick = { classData ->
                            navController.navigate("class_options/${classData.classId}/${classData.subjectName}/${classData.teacherName}/${classData.groupId}/${classData.teacherEmail}")
                        }
                    )
                }
            }
        }
        
        // Floating Action Buttons for AI Features and BLE Scanning
        FloatingActionButtons(
            onAIChatbotClick = {
                navController.navigate("ai_chatbot")
            },
            onTodoListClick = {
                navController.navigate("ai_todo_list")
            },
            onScanningClick = {
                println("🔍 Dashboard: Start Scanning button clicked - navigating to scanning screen")
                navController.navigate("student_scanning")
            }
        )
    }
    
    // BLE Dialog Components
    if (showAttendanceDialog) {
        AttendanceDialog(
            onDismiss = { viewModel.onAttendanceDialogDismiss() },
            onConfirm = { 
                viewModel.onAttendanceDialogConfirmed()
                println("✅ Attendance dialog confirmed for session: $detectedSessionCode")
            },
            onManualCode = {
                viewModel.onManualConnectionRequested()
                println("🔗 Manual connection requested for session: $detectedSessionCode")
            },
            subjectName = currentSubjectName,
            teacherName = currentTeacherName
        )
    }
    
    if (showBiometricAuthDialog) {
        BiometricAuthDialog(
            onDismiss = { viewModel.onBiometricAuthDialogDismiss() },
            onSuccess = { 
                viewModel.onBiometricAuthSuccess()
            },
            onError = { error ->
                viewModel.onBiometricAuthDialogDismiss()
                // Handle biometric error
                println("❌ Biometric authentication error: $error")
            }
        )
    }
    
    // Manual Code Entry Dialog
    if (viewModel.showManualCodeDialog) {
        ManualCodeEntryDialog(
            showDialog = viewModel.showManualCodeDialog,
            onConfirm = { sessionCode ->
                viewModel.onManualCodeConfirmed(sessionCode)
            },
            onDismiss = { 
                viewModel.onManualCodeDismissed()
            },
            isLoading = viewModel.isManualCodeConnecting,
            errorMessage = viewModel.manualCodeError
        )
    }
}

@Composable
private fun ClassCard(
    classData: ClassCardData,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = classData.subjectName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = classData.teacherName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Time",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${classData.startTime} - ${classData.endTime}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Days",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = classData.days,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
                
                // Attendance Status
                AttendanceStatusChip(status = classData.attendanceStatus, days = classData.days)
            }
        }
    }
}

@Composable
private fun AttendanceStatusChip(status: AttendanceStatus, days: String = "") {
    val (backgroundColor, textColor, icon) = when (status) {
        AttendanceStatus.ATTENDED -> Triple(
            Color(0xFF4CAF50).copy(alpha = 0.2f),
            Color(0xFF4CAF50),
            Icons.Default.CheckCircle
        )
        AttendanceStatus.MISSED -> Triple(
            Color(0xFFF44336).copy(alpha = 0.2f),
            Color(0xFFF44336),
            Icons.Default.Close
        )
        AttendanceStatus.UPCOMING -> Triple(
            Color(0xFFFF9800).copy(alpha = 0.2f),
            Color(0xFFFF9800),
            Icons.Default.Info
        )
    }
    
    val text = when (status) {
        AttendanceStatus.ATTENDED -> "Attended"
        AttendanceStatus.MISSED -> "Missed"
        AttendanceStatus.UPCOMING -> if (days.isNotBlank()) days else "Upcoming"
    }
    
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = textColor,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun EmptyStateCard() {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "No classes",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No classes found",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Your classes will appear here once they are scheduled",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.6f)
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BleScanningCard(
    isScanning: Boolean,
    bleStatus: com.example.bleattendance.utils.BlePermissionChecker.BleStatus,
    onStartScan: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📡 BLE Attendance Scanner",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (bleStatus.canUseBle) 
                    "Tap to open the BLE scanning interface for marking attendance" 
                else 
                    "Tap to open scanner (BLE setup required)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.7f)
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onStartScan,
                enabled = true, // Always enabled for navigation
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (bleStatus.canUseBle) Color(0xFFA3E635) else Color(0xFF666666)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Start Scan",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (bleStatus.canUseBle) "Start Scanning" else "Open Scanner",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            
        }
    }
}

// Helper functions
private fun convertToScheduleData(classes: List<ClassCardData>): List<ClassScheduleData> {
    val colors = listOf(
        Color(0xFFFF9800), // Orange
        Color(0xFF2196F3), // Blue
        Color(0xFF4CAF50), // Green
        Color(0xFF9C27B0), // Purple
        Color(0xFFF44336), // Red
        Color(0xFF00BCD4), // Cyan
        Color(0xFFFFC107)  // Amber
    )
    
    return classes.mapIndexed { index, classData ->
        ClassScheduleData(
            subjectName = classData.subjectName,
            teacherName = classData.teacherName,
            teacherEmail = classData.teacherEmail,
            startTime = classData.startTime,
            endTime = classData.endTime,
            dayOfWeek = extractDayFromSchedule(classData.days), // Use days field
            color = colors[index % colors.size],
            room = "Room ${index + 1}",
            classId = classData.classId,
            groupId = classData.groupId
        )
    }
}

private fun getCurrentDayIndex(): Int {
    val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    return if (currentDay == Calendar.SUNDAY) 6 else currentDay - 2 // Convert to 0-6 (Mon-Sun)
}

private fun extractDayFromSchedule(schedule: String): Int {
    // Parse the schedule string to extract the day
    // Schedule format: "Tuesday 09:00-10:00" or "Wednesday 11:15-13:15"
    val trimmedSchedule = schedule.trim()
    
    // Look for day patterns at the start of the string
    val dayPattern = Regex("^(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday|Mon|Tue|Wed|Thu|Fri|Sat|Sun)")
    val dayMatch = dayPattern.find(trimmedSchedule)
    
    if (dayMatch != null) {
        val dayString = dayMatch.value
        return when (dayString.lowercase()) {
            "monday", "mon" -> 0
            "tuesday", "tue" -> 1
            "wednesday", "wed" -> 2
            "thursday", "thu" -> 3
            "friday", "fri" -> 4
            "saturday", "sat" -> 5
            "sunday", "sun" -> 6
            else -> 0 // Default to Monday
        }
    }
    
    // Fallback: try to extract from start time (old logic as backup)
    return when (schedule) {
        "09:00" -> 0 // Monday
        "10:00" -> 1 // Tuesday
        "11:00" -> 2 // Wednesday
        "12:00" -> 3 // Thursday
        "14:00" -> 4 // Friday
        "15:00" -> 5 // Saturday
        else -> 0 // Default to Monday
    }
}

// Data classes
data class ClassCardData(
    val subjectName: String,
    val teacherName: String,
    val startTime: String,
    val endTime: String,
    val days: String,
    val attendanceStatus: AttendanceStatus,
    val groupId: String,
    val classId: Int = 0,
    val teacherEmail: String = ""
)

@Composable
private fun FloatingActionButtons(
    onAIChatbotClick: () -> Unit,
    onTodoListClick: () -> Unit,
    onScanningClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        // AI Chatbot FAB
        FloatingActionButton(
            onClick = onAIChatbotClick,
            modifier = Modifier.size(56.dp),
            containerColor = Color(0xFF8B5CF6), // Purple
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "AI Chatbot",
                modifier = Modifier.size(24.dp)
            )
        }
        
        // AI Todo List FAB
        FloatingActionButton(
            onClick = onTodoListClick,
            modifier = Modifier.size(56.dp),
            containerColor = Color(0xFFEC4899), // Pink
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = "AI Todo List",
                modifier = Modifier.size(24.dp)
            )
        }
        
        // BLE Scanning FAB
        FloatingActionButton(
            onClick = onScanningClick,
            modifier = Modifier.size(56.dp),
            containerColor = Color(0xFFA3E635), // Green
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "BLE Scanning",
                modifier = Modifier.size(24.dp)
            )
        }
        }
    }
}

enum class AttendanceStatus {
    ATTENDED,
    MISSED,
    UPCOMING
}
