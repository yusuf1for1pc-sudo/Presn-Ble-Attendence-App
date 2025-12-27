package com.example.bleattendance.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bleattendance.ui.components.GlassCard
import com.example.bleattendance.data.db.AttendanceSessionEntity

import com.example.bleattendance.ui.viewmodels.TeacherDashboardViewModel

// Data class to hold parsed group ID information
data class ParsedGroupId(
    val department: String,
    val division: String,
    val batch: String
)

// Function to parse group ID (e.g., "IT_2025-2029_A_ALL_Sem1" -> department="IT", division="A", batch="ALL")
fun parseGroupId(groupId: String): ParsedGroupId {
    return try {
        val parts = groupId.split("_")
        if (parts.size >= 4) {
            val department = parts[0] // e.g., "IT"
            val division = parts[2] // e.g., "A"
            val batch = parts[3] // e.g., "ALL" or "1"
            ParsedGroupId(department, division, batch)
        } else {
            // Fallback for malformed group IDs
            ParsedGroupId("Unknown", "Unknown", "Unknown")
        }
    } catch (e: Exception) {
        // Fallback for any parsing errors
        ParsedGroupId("Unknown", "Unknown", "Unknown")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    navController: NavController,
    viewModel: TeacherDashboardViewModel = viewModel()
) {
    val teacher by viewModel.teacherProfile.collectAsState()
    val classes by viewModel.classList.collectAsState()
    val teacherName by viewModel.teacherName.collectAsState()
    val totalSessions by viewModel.totalSessions.collectAsState()
    val totalStudents by viewModel.totalStudents.collectAsState()
    val lastSessionInfo by viewModel.lastSessionInfo.collectAsState()
    
    // Get current date
    val currentDate = remember {
        val dateFormat = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
        dateFormat.format(Date())
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .padding(bottom = 80.dp), // Add bottom padding for the fixed bottom bar
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                                         Column {
                         Text(
                             text = currentDate,
                             style = MaterialTheme.typography.bodyLarge.copy(
                                 fontSize = 16.sp
                             ),
                             color = Color(0xFFA3E635)
                         )
                         Text(
                             text = teacherName,
                             style = MaterialTheme.typography.headlineMedium.copy(
                                 fontWeight = FontWeight.Bold,
                                 fontSize = 24.sp
                             ),
                             color = Color.White
                         )
                     }

                    // Refresh and Settings buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Refresh button
                        IconButton(
                            onClick = { 
                                viewModel.refreshClasses()
                            },
                            modifier = Modifier
                                .background(
                                    color = Color(0xFFA3E635).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Data",
                                tint = Color(0xFFA3E635)
                            )
                        }
                        
                        // Settings button
                        IconButton(
                            onClick = { navController.navigate("settings") }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = Color(0xFFA3E635).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(20.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = Color(0xFFA3E635),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

                                      // Stats Section
             item {
                 Row(
                     modifier = Modifier.fillMaxWidth(),
                     horizontalArrangement = Arrangement.spacedBy(12.dp)
                 ) {
                     StatCard(
                         title = "Total Classes",
                         value = classes.size.toString(),
                         icon = Icons.Default.List,
                         color = Color(0xFFA3E635),
                         modifier = Modifier.weight(1f)
                     )
                     StatCard(
                         title = "Total Sessions",
                         value = totalSessions.toString(),
                         icon = Icons.Default.PlayArrow,
                         color = Color(0xFFEF4444),
                         modifier = Modifier.weight(1f)
                     )
                 }
             }

             // Your Classes Section
             item {
                 Row(
                     modifier = Modifier.fillMaxWidth(),
                     horizontalArrangement = Arrangement.SpaceBetween,
                     verticalAlignment = Alignment.CenterVertically
                 ) {
                     Text(
                         text = "Your Classes",
                         style = MaterialTheme.typography.titleLarge.copy(
                             fontWeight = FontWeight.Bold,
                             fontSize = 20.sp
                         ),
                         color = Color.White
                     )

                     TextButton(
                         onClick = { navController.navigate("view_all_classes") }
                     ) {
                         Text(
                             text = "View All",
                             color = Color(0xFFA3E635),
                             style = MaterialTheme.typography.bodyMedium.copy(
                                 fontWeight = FontWeight.Medium
                             )
                         )
                     }
                 }
             }

             // Classes List
             if (classes.isEmpty()) {
                 item {
                     Box(
                         modifier = Modifier
                             .fillMaxWidth()
                             .padding(vertical = 40.dp),
                         contentAlignment = Alignment.Center
                     ) {
                         Column(
                             horizontalAlignment = Alignment.CenterHorizontally,
                             verticalArrangement = Arrangement.spacedBy(16.dp)
                         ) {
                             Icon(
                                 imageVector = Icons.Default.List,
                                 contentDescription = null,
                                 tint = Color.White.copy(alpha = 0.5f),
                                 modifier = Modifier.size(64.dp)
                             )
                             Text(
                                 text = "No classes yet",
                                 style = MaterialTheme.typography.titleMedium.copy(
                                     fontWeight = FontWeight.Medium
                                 ),
                                 color = Color.White.copy(alpha = 0.7f)
                             )
                             Text(
                                 text = "Create your first class to get started",
                                 style = MaterialTheme.typography.bodyMedium,
                                 color = Color.White.copy(alpha = 0.5f),
                                 textAlign = TextAlign.Center
                             )
                         }
                     }
                 }
             } else {
                 items(classes.take(3)) { classEntity ->
                     ClassCard(
                         classInfo = ClassInfo(
                             id = classEntity.classId,
                             department = parseGroupId(classEntity.groupId).department,
                             division = parseGroupId(classEntity.groupId).division,
                             batch = parseGroupId(classEntity.groupId).batch,
                             subjectName = classEntity.subjectName
                         ),
                         onClick = {
                             navController.navigate("class_options/${classEntity.classId}/${classEntity.subjectName}/${teacher!!.name}/${classEntity.groupId}/${teacher!!.email}")
                         },
                         onStartSession = {
                             navController.navigate("class_session/${classEntity.classId}")
                         }
                     )
                 }
             }
        }

        // YouTube-style bottom bar overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    color = Color(0xFF1A1A1A).copy(alpha = 0.98f),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
                .padding(horizontal = 20.dp, vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                                 // Left side - Start Session
                 QuickActionButton(
                     text = "Start Session",
                     icon = Icons.Default.PlayArrow,
                     onClick = { /* Handle start session */ },
                     modifier = Modifier.weight(1f)
                 )

                                 // Center - Add Class button (YouTube-style)
                 Box(
                     modifier = Modifier
                         .weight(1f)
                         .padding(horizontal = 8.dp),
                     contentAlignment = Alignment.Center
                 ) {
                     Button(
                         onClick = {
                             if (teacher != null) {
                                 navController.navigate("add_class/${teacher!!.email}")
                             }
                         },
                         modifier = Modifier
                             .background(
                                 color = Color(0xFFA3E635).copy(alpha = 0.15f),
                                 shape = RoundedCornerShape(12.dp)
                             )
                             .padding(vertical = 6.dp, horizontal = 12.dp),
                         colors = ButtonDefaults.buttonColors(
                             containerColor = Color.Transparent
                         ),
                         contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp)
                     ) {
                         Column(
                             horizontalAlignment = Alignment.CenterHorizontally,
                             verticalArrangement = Arrangement.spacedBy(4.dp)
                         ) {
                             Icon(
                                 imageVector = Icons.Default.Add,
                                 contentDescription = "Add Class",
                                 tint = Color(0xFFA3E635),
                                 modifier = Modifier.size(24.dp)
                             )

                             Text(
                                 text = "Add Class",
                                 style = MaterialTheme.typography.labelSmall.copy(
                                     fontWeight = FontWeight.Medium
                                 ),
                                 color = Color(0xFFA3E635),
                                 textAlign = TextAlign.Center,
                                 maxLines = 1,
                                 overflow = TextOverflow.Ellipsis
                             )
                         }
                     }
                 }

                                 // Right side - Assignments
                 QuickActionButton(
                     text = "Assignments",
                     icon = Icons.Default.Info,
                     onClick = { navController.navigate("teacher_assignments/${teacher!!.email}") },
                     modifier = Modifier.weight(1f)
                 )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = color.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = Color.White
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp
                ),
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ClassCard(
    classInfo: ClassInfo,
    onClick: () -> Unit,
    onStartSession: () -> Unit
) {
    // Get statistics for this specific class
    val viewModel: TeacherDashboardViewModel = viewModel()
    val sessionsForClass by viewModel.getSessionsForClass(classInfo.id).collectAsState(initial = emptyList())
    
    // Calculate statistics for this class
    val sessionCount = sessionsForClass.size
    val studentCount = 0 // Simplified - no longer available in new model
    val lastSessionText = if (sessionsForClass.isNotEmpty()) {
        val lastSession = sessionsForClass.maxByOrNull { it.markedAt }
        val date = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            .format(java.util.Date(lastSession!!.markedAt))
        "Last: $date"
    } else {
        "No sessions yet"
    }
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (classInfo.batch == "None") 
                                "${classInfo.department} • ${classInfo.division}"
                            else 
                                "${classInfo.department} • ${classInfo.division} • ${classInfo.batch}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // Status indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = Color(0xFF10B981),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }

                    Text(
                        text = classInfo.subjectName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp
                        ),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Students count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                                         Text(
                         text = "$studentCount Students",
                         style = MaterialTheme.typography.bodySmall,
                         color = Color.White.copy(alpha = 0.7f)
                     )
                }

                // Sessions count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                                         Text(
                         text = "$sessionCount Sessions",
                         style = MaterialTheme.typography.bodySmall,
                         color = Color.White.copy(alpha = 0.7f)
                     )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Last session info
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                                                 Text(
                             text = lastSessionText,
                             style = MaterialTheme.typography.bodySmall,
                             color = Color.White.copy(alpha = 0.7f)
                         )
                    }
                    Text(
                        text = "Last Session",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    Text("View Details")
                }

                Button(
                    onClick = onStartSession,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFA3E635)
                    )
                ) {
                    Text(
                        "Start Session",
                        color = Color.Black,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

data class ClassInfo(
    val id: Int,
    val department: String,
    val division: String,
    val batch: String,
    val subjectName: String
)
