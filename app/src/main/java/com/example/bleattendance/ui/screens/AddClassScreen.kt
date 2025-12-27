// In ui/screens/AddClassScreen.kt

package com.example.bleattendance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bleattendance.ui.viewmodels.AddClassViewModel
import com.example.bleattendance.ui.components.*
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClassScreen(
    navController: NavController,
    teacherEmail: String,
    viewModel: AddClassViewModel = viewModel()
) {
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    
    // Load teacher profile when screen is created
    LaunchedEffect(Unit) {
        // The ViewModel will automatically load the teacher profile in init
        println("AddClassScreen: Loading teacher profile...")
        viewModel.refreshTeacherProfile()
    }
    
    // Handle system UI
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as android.app.Activity).window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        onDispose {
            // Restore default behavior if needed
        }
    }
    
    // Responsive design helpers
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    
    fun responsiveSpacing(): Dp = if (isTablet) 24.dp else 16.dp
    fun responsiveTextSize(size: TextUnit): TextUnit = if (isTablet) (size.value * 1.2).sp else size
    
    // Glass card component
    @Composable
    fun GlassCard(
        modifier: Modifier = Modifier,
        content: @Composable ColumnScope.() -> Unit
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.1f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.2f)
            )
        ) {
            content()
        }
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
                .verticalScroll(rememberScrollState())
                .padding(if (isTablet) 32.dp else 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.background(
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
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Create New Class",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = responsiveTextSize(24.sp)
                        ),
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(responsiveSpacing()))

            // Form container
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(if (isTablet) 32.dp else 24.dp),
                    verticalArrangement = Arrangement.spacedBy(responsiveSpacing())
                ) {
                    // Form fields
                    ModernTextField(
                        value = viewModel.subjectName,
                        onValueChange = { viewModel.subjectName = it },
                        label = "Subject Name",
                        placeholder = "e.g., Data Structures (auto-filled from your profile)",
                        enabled = !isSaving
                    )
                    
                    ModernDropdownWithTyping(
                        value = viewModel.branch,
                        onValueChange = { viewModel.branch = it },
                        label = "Department",
                        placeholder = "Select or type department",
                        options = viewModel.departmentOptions,
                        leadingIcon = Icons.Default.Info,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Division selector (editable dropdown)
                    ModernDivisionSelector(
                        selectedDivision = viewModel.division,
                        onDivisionSelected = { viewModel.division = it },
                        enabled = !isSaving
                    )
                    
                    // Batch selection dropdown
                    ModernBatchSelector(
                        selectedBatch = viewModel.batch,
                        onBatchSelected = { batchStr -> 
                            viewModel.batch = batchStr
                        },
                        enabled = !isSaving
                    )
                    
                    // Semester selection dropdown
                    ModernSemesterSelector(
                        selectedSemester = "Semester ${viewModel.semester}",
                        onSemesterSelected = { semesterStr -> 
                            viewModel.semester = semesterStr.replace("Semester ", "").toIntOrNull() ?: 1
                        },
                        enabled = !isSaving
                    )
                    
                    // Class Schedule Section
                    Text(
                        text = "Class Schedule",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = responsiveTextSize(14.sp)
                        ),
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    
                    // Class Days Selection
                    ModernDropdownWithTyping(
                        value = viewModel.classDays,
                        onValueChange = { viewModel.classDays = it },
                        label = "Class Days",
                        placeholder = "Select days (e.g., Mon, Wed, Fri)",
                        options = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday", "Mon, Wed, Fri", "Tue, Thu", "Daily"),
                        leadingIcon = Icons.Default.Info,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Time Selection Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Start Time
                        ModernTimePicker(
                            value = viewModel.classStartTime,
                            onValueChange = { viewModel.classStartTime = it },
                            label = "Start Time",
                            placeholder = "09:00",
                            modifier = Modifier.weight(1f),
                            enabled = !isSaving
                        )
                        
                        // End Time
                        ModernTimePicker(
                            value = viewModel.classEndTime,
                            onValueChange = { viewModel.classEndTime = it },
                            label = "End Time",
                            placeholder = "10:30",
                            modifier = Modifier.weight(1f),
                            enabled = !isSaving
                        )
                    }
                    
                    // Date Selection
                    ModernDatePicker(
                        value = viewModel.classDate,
                        onValueChange = { viewModel.classDate = it },
                        label = "Class Date (Optional)",
                        placeholder = "2025-01-15",
                        enabled = !isSaving
                    )
                    
                    // Manual Schedule Override
                    ModernTextField(
                        value = viewModel.classSchedule,
                        onValueChange = { viewModel.classSchedule = it },
                        label = "Custom Schedule (Optional)",
                        placeholder = "Override with custom format",
                        enabled = !isSaving
                    )
                }
            }

            Spacer(modifier = Modifier.height(responsiveSpacing()))

            // Error message
            if (saveError != null) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = saveError!!,
                        color = Color(0xFFEF4444),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = responsiveTextSize(14.sp)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(responsiveSpacing()))
            }

            // Create Class button (styled like Start Session button)
            OutlinedButton(
                onClick = {
                    if (!isSaving) {
                        isSaving = true
                        saveError = null
                        viewModel.saveClass(teacherEmail) { success, error ->
                            isSaving = false
                            if (success) {
                                navController.navigateUp()
                            } else {
                                saveError = error ?: "Failed to create class"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving && viewModel.subjectName.isNotBlank() && 
                         viewModel.branch.isNotBlank() && 
                         viewModel.division.isNotBlank() && 
                         viewModel.batch.isNotBlank(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFA3E635)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0xFFA3E635)
                )
            ) {
                if (isSaving) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFA3E635),
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Creating Class...",
                            color = Color(0xFFA3E635)
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Create Class",
                            color = Color(0xFFA3E635)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    enabled: Boolean = true
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            ),
            color = Color.White.copy(alpha = 0.9f)
        )
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFA3E635),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFFA3E635)
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernDivisionSelector(
    selectedDivision: String,
    onDivisionSelected: (String) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val divisions = listOf("A", "B", "C", "D", "E", "F")

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Division",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            ),
            color = Color.White.copy(alpha = 0.9f)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (enabled) expanded = it }
        ) {
            OutlinedTextField(
                value = selectedDivision,
                onValueChange = {},
                readOnly = true,
                placeholder = {
                    Text(
                        text = "Select division",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                },
                trailingIcon = { 
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .background(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                enabled = enabled,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFA3E635),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFA3E635)
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp
                )
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(
                    color = Color(0xFF1A1A1A).copy(alpha = 0.98f),
                    shape = RoundedCornerShape(12.dp)
                )
            ) {
                divisions.forEach { division ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = division,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        },
                        onClick = {
                            onDivisionSelected(division)
                            expanded = false
                        },
                        modifier = Modifier.background(
                            color = if (selectedDivision == division) 
                                Color(0xFFA3E635).copy(alpha = 0.2f) 
                            else Color.Transparent
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernBatchSelector(
    selectedBatch: String,
    onBatchSelected: (String) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val batches = listOf("None", "1", "2", "3")

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Batch",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            ),
            color = Color.White.copy(alpha = 0.9f)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (enabled) expanded = it }
        ) {
            OutlinedTextField(
                value = selectedBatch,
                onValueChange = {},
                readOnly = true,
                placeholder = {
                    Text(
                        text = "Select batch",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                },
                trailingIcon = { 
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .background(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                enabled = enabled,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFA3E635),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFA3E635)
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp
                )
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(
                    color = Color(0xFF1A1A1A).copy(alpha = 0.98f),
                    shape = RoundedCornerShape(12.dp)
                )
            ) {
                batches.forEach { batch ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = batch,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        },
                        onClick = {
                            onBatchSelected(batch)
                            expanded = false
                        },
                        modifier = Modifier.background(
                            color = if (selectedBatch == batch) 
                                Color(0xFFA3E635).copy(alpha = 0.2f) 
                            else Color.Transparent
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernSemesterSelector(
    selectedSemester: String,
    onSemesterSelected: (String) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val semesters = listOf("Semester 1", "Semester 2", "Semester 3", "Semester 4", "Semester 5", "Semester 6", "Semester 7", "Semester 8")

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Semester",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            ),
            color = Color.White.copy(alpha = 0.9f)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (enabled) expanded = it }
        ) {
            OutlinedTextField(
                value = selectedSemester,
                onValueChange = {},
                readOnly = true,
                placeholder = {
                    Text(
                        text = "Select semester",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                },
                trailingIcon = { 
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .background(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                enabled = enabled,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFA3E635),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFA3E635)
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp
                )
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(
                    color = Color(0xFF1A1A1A).copy(alpha = 0.98f),
                    shape = RoundedCornerShape(12.dp)
                )
            ) {
                semesters.forEach { semester ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = semester,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        },
                        onClick = {
                            onSemesterSelected(semester)
                            expanded = false
                        },
                        modifier = Modifier.background(
                            color = if (selectedSemester == semester) 
                                Color(0xFFA3E635).copy(alpha = 0.2f) 
                            else Color.Transparent
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernTimePicker(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            ),
            color = Color.White.copy(alpha = 0.9f)
        )
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFA3E635),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFFA3E635)
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color(0xFFA3E635),
                    modifier = Modifier.size(20.dp)
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernDatePicker(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            ),
            color = Color.White.copy(alpha = 0.9f)
        )
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFA3E635),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFFA3E635)
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Color(0xFFA3E635),
                    modifier = Modifier.size(20.dp)
                )
            }
        )
    }
}