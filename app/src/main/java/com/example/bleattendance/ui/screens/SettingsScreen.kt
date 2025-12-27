package com.example.bleattendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.example.bleattendance.ui.components.GlassCard
import com.example.bleattendance.AttendanceApp
import com.example.bleattendance.ui.viewmodels.TeacherDashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: TeacherDashboardViewModel = viewModel()
) {
    val teacherName by viewModel.teacherName.collectAsState()
    val teacherEmail by viewModel.teacherEmail.collectAsState()
    val teacherSubject by viewModel.teacherSubject.collectAsState()
    
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isEditingProfile by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    
    // Profile editing state - update when teacher data changes
    var editedName by remember { mutableStateOf("") }
    var editedSubject by remember { mutableStateOf("") }
    
    // Update edited values when teacher data changes
    LaunchedEffect(teacherName, teacherSubject) {
        editedName = teacherName
        editedSubject = teacherSubject
        println("Teacher data updated: $teacherName, $teacherEmail, $teacherSubject")
    }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
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
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Section
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            // Profile Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Profile",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                                
                                IconButton(
                                    onClick = { 
                                        if (isEditingProfile) {
                                            // Save changes
                                            println("Saving profile changes: $editedName, $teacherEmail, $editedSubject")
                                            viewModel.updateTeacherProfile(
                                                editedName,
                                                teacherEmail,
                                                editedSubject
                                            )
                                            showSuccessMessage = true
                                            // Hide success message after 2 seconds
                                            coroutineScope.launch {
                                                kotlinx.coroutines.delay(2000)
                                                showSuccessMessage = false
                                            }
                                        }
                                        isEditingProfile = !isEditingProfile
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isEditingProfile) Icons.Default.Check else Icons.Default.Edit,
                                        contentDescription = if (isEditingProfile) "Save" else "Edit",
                                        tint = Color(0xFFA3E635)
                                    )
                                }
                            }
                            
                                                         Spacer(modifier = Modifier.height(16.dp))
                             
                             // Success Message
                             if (showSuccessMessage) {
                                 Box(
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .background(
                                             color = Color(0xFF10B981).copy(alpha = 0.2f),
                                             shape = RoundedCornerShape(8.dp)
                                         )
                                         .padding(12.dp)
                                 ) {
                                     Row(
                                         verticalAlignment = Alignment.CenterVertically
                                     ) {
                                         Icon(
                                             imageVector = Icons.Default.CheckCircle,
                                             contentDescription = null,
                                             tint = Color(0xFF10B981),
                                             modifier = Modifier.size(20.dp)
                                         )
                                         Spacer(modifier = Modifier.width(8.dp))
                                         Text(
                                             text = "Profile updated successfully!",
                                             color = Color(0xFF10B981),
                                             style = MaterialTheme.typography.bodyMedium
                                         )
                                     }
                                 }
                                 Spacer(modifier = Modifier.height(16.dp))
                             }
                             
                             // Profile Avatar
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(
                                        color = Color(0xFFA3E635).copy(alpha = 0.2f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = teacherName.take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFFA3E635)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Profile Fields
                            if (isEditingProfile) {
                                // Editable fields
                                OutlinedTextField(
                                    value = editedName,
                                    onValueChange = { editedName = it },
                                    label = { Text("Name", color = Color.White.copy(alpha = 0.7f)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFA3E635),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = Color(0xFFA3E635),
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                                    )
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                OutlinedTextField(
                                    value = editedSubject,
                                    onValueChange = { editedSubject = it },
                                    label = { Text("Subject", color = Color.White.copy(alpha = 0.7f)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFA3E635),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = Color(0xFFA3E635),
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                                    )
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                

                            } else {
                                // Display fields
                                ProfileField("Name", teacherName, Icons.Default.Person)
                                ProfileField("Email", teacherEmail, Icons.Default.Email)
                                ProfileField("Subject", teacherSubject, Icons.Default.Star)
                            }
                        }
                    }
                }
                
                
                // Account Actions Section
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "Account",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Logout Button
                            OutlinedButton(
                                onClick = { showLogoutDialog = true },
                                modifier = Modifier.fillMaxWidth(),
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
                                        imageVector = Icons.Default.ExitToApp,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Logout")
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Logout Confirmation Dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = {
                    Text(
                        "Logout",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                text = {
                    Text(
                        "Are you sure you want to logout?",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    OutlinedButton(
                        onClick = {
                            viewModel.logout()
                            navController.navigate("role_selection") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFEF4444)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFFEF4444)
                        )
                    ) {
                        Text("Logout")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showLogoutDialog = false },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFA3E635)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFFA3E635)
                        )
                    ) {
                        Text("Cancel")
                    }
                },
                containerColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(16.dp)
            )
        }
        
    }
}

@Composable
fun ProfileField(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFA3E635),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}
