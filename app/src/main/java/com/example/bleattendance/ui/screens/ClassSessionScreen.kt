// In ui/screens/ClassSessionScreen.kt

package com.example.bleattendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bleattendance.model.StudentInfo
import com.example.bleattendance.ui.viewmodels.ClassSessionViewModel
import com.example.bleattendance.ui.components.PermissionStatusCard
import com.example.bleattendance.ui.components.GlassCard
import com.example.bleattendance.utils.BlePermissionChecker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassSessionScreen(
    navController: NavController,
    classId: Int,
    permissionsGranted: Boolean = false,
    viewModel: ClassSessionViewModel = viewModel()
) {
    // Create BLE permission checker
    val context = LocalContext.current
    val blePermissionChecker = remember { BlePermissionChecker(context) }
    val bleStatus = remember { blePermissionChecker.getBleStatus() }
    
    // State for confirmation dialog
    var showEndSessionDialog by remember { mutableStateOf(false) }
    
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Live Class Session",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Show detailed permission status if needed
            if (!bleStatus.canUseBle) {
                PermissionStatusCard(
                    bleStatus = bleStatus,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            if (!viewModel.isSessionActive) {
                // Screen before class starts
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Ready to start your class?",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            OutlinedButton(
                                onClick = { 
                                    if (bleStatus.canUseBle) {
                                        viewModel.startClassSession(classId) 
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                enabled = bleStatus.canUseBle,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFA3E635)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color(0xFFA3E635)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Start Class",
                                        fontSize = 18.sp,
                                        color = Color(0xFFA3E635)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Screen while class is active
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Session Code Card
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Share this code with your students:",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFFA3E635),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = viewModel.sessionCode.chunked(3).joinToString("  "),
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 8.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Server Status Card
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (viewModel.serverStatus == "Connected") 
                                            Color(0xFFA3E635) 
                                        else 
                                            Color(0xFFEF4444),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Server Status: ${viewModel.serverStatus}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                    }

                    // Attendance Section
                    GlassCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                "Live Attendance (${viewModel.attendedStudents.size})",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            )

                            if (viewModel.attendedStudents.isEmpty()) {
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Waiting for students to connect...",
                                        color = Color(0xFFA3E635),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(viewModel.attendedStudents) { student ->
                                        StudentAttendanceRow(student)
                                    }
                                }
                            }
                        }
                    }

                    // End Session Button
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { showEndSessionDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFEF4444)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFFEF4444)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "End Session & Save",
                                fontSize = 18.sp,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }
        }
        
        // Confirmation Dialog
        if (showEndSessionDialog) {
            AlertDialog(
                onDismissRequest = { showEndSessionDialog = false },
                title = {
                    Text(
                        "End Session?",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                text = {
                    Text(
                        "This will end the session and save attendance data.",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    OutlinedButton(
                        onClick = {
                            viewModel.stopClassSession()
                            navController.popBackStack()
                            showEndSessionDialog = false
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFEF4444)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFFEF4444)
                        )
                    ) {
                        Text(
                            "End Session",
                            color = Color(0xFFEF4444)
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showEndSessionDialog = false },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFA3E635)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFFA3E635)
                        )
                    ) {
                        Text(
                            "Cancel",
                            color = Color(0xFFA3E635)
                        )
                    }
                },
                containerColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun StudentAttendanceRow(student: StudentInfo) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                student.rollNumber,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFFA3E635)
            )
            Text(
                student.name,
                modifier = Modifier.weight(2f),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
        }
    }
}