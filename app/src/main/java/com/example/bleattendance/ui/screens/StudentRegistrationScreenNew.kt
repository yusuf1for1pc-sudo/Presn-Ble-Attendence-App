// In ui/screens/StudentRegistrationScreenNew.kt

package com.example.bleattendance.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.core.CubicBezierEasing
import androidx.navigation.NavController
import com.example.bleattendance.ui.viewmodels.StudentRegistrationViewModel
import com.example.bleattendance.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun YearScrollWheel(
    selectedYear: Int,
    onYearSelected: (Int) -> Unit,
    yearOptions: List<Int>,
    modifier: Modifier = Modifier
) {
    val itemHeight = 50.dp
    val visibleItems = 3 // Show only 3 items at once (compact wheel)
    val totalHeight = itemHeight * visibleItems
    
    // Calculate initial offset to center the selected year
    val selectedIndex = yearOptions.indexOf(selectedYear)
    val initialOffset = if (selectedIndex >= 0) {
        -(selectedIndex * itemHeight.value) + (totalHeight.value / 2) - (itemHeight.value / 2)
    } else 0f
    
    var offsetY by remember { mutableStateOf(initialOffset) }
    var isDragging by remember { mutableStateOf(false) }
    
    // Calculate which year is currently centered
    val centerIndex = ((-offsetY + totalHeight.value / 2) / itemHeight.value).toInt().coerceIn(0, yearOptions.size - 1)
    val centeredYear = yearOptions[centerIndex]
    
    // Auto-select the centered year when not dragging
    LaunchedEffect(centeredYear, isDragging) {
        if (!isDragging) {
            onYearSelected(centeredYear)
        }
    }
    
    Box(
        modifier = modifier
            .height(totalHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { 
                        println("🎯 YearScrollWheel: Drag started!")
                        isDragging = true 
                    },
                    onDragEnd = { 
                        println("🎯 YearScrollWheel: Drag ended!")
                        isDragging = false
                        // Snap to nearest item with smooth animation
                        val snapIndex = ((-offsetY + totalHeight.value / 2) / itemHeight.value).toInt().coerceIn(0, yearOptions.size - 1)
                        val snapOffset = -(snapIndex * itemHeight.value) + (totalHeight.value / 2) - (itemHeight.value / 2)
                        offsetY = snapOffset
                        println("🎯 YearScrollWheel: Snapped to index $snapIndex, year ${yearOptions[snapIndex]}")
                    }
                ) { _, dragAmount ->
                    println("🎯 YearScrollWheel: Dragging with amount ${dragAmount.y}")
                    val newOffset = offsetY + (dragAmount.y * 1.5f) // Increase sensitivity
                    val maxOffset = (totalHeight.value / 2) - (itemHeight.value / 2)
                    val minOffset = -(yearOptions.size - 1) * itemHeight.value + (totalHeight.value / 2) - (itemHeight.value / 2)
                    offsetY = newOffset.coerceIn(minOffset, maxOffset)
                }
            }
    ) {
        // Top and bottom fade gradients (like clock apps)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A1A),
                            Color(0xFF1A1A1A).copy(alpha = 0.0f)
                        )
                    )
                )
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .offset(y = totalHeight - itemHeight)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A1A).copy(alpha = 0.0f),
                            Color(0xFF1A1A1A)
                        )
                    )
                )
        )
        
        // Center selection indicator (like clock apps)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .offset(y = (totalHeight / 2) - (itemHeight / 2))
                .background(
                    Color(0xFFA3E635).copy(alpha = 0.1f),
                    RoundedCornerShape(8.dp)
                )
                .border(
                    1.dp,
                    Color(0xFFA3E635).copy(alpha = 0.3f),
                    RoundedCornerShape(8.dp)
                )
        )
        
        // Year list - render all years but only show 3 at a time
        yearOptions.forEachIndexed { index, year ->
            val distanceFromCenter = kotlin.math.abs(index - centerIndex)
            
            // Only render items that are within visible range (center ± 1)
            if (distanceFromCenter <= 1) {
                val alpha = when {
                    distanceFromCenter == 0 -> 1.0f // Center item - fully visible
                    distanceFromCenter == 1 -> 0.7f // Adjacent items - semi-transparent
                    else -> 0.0f // Not visible
                }
                
                val scale = when {
                    distanceFromCenter == 0 -> 1.0f // Center item - normal size
                    distanceFromCenter == 1 -> 0.9f // Adjacent items - slightly smaller
                    else -> 0.8f // Not visible
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .offset(y = (index * itemHeight.value + offsetY).dp)
                        .alpha(alpha)
                        .scale(scale)
                        .clickable { 
                            val targetOffset = -(index * itemHeight.value) + (totalHeight.value / 2) - (itemHeight.value / 2)
                            offsetY = targetOffset
                            onYearSelected(year)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = year.toString(),
                        color = if (distanceFromCenter == 0) Color(0xFFA3E635) else Color.White.copy(alpha = 0.8f),
                        fontSize = if (distanceFromCenter == 0) 20.sp else 16.sp,
                        fontWeight = if (distanceFromCenter == 0) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingOrbsBackground(
    modifier: Modifier = Modifier,
    orbPositions: Int = 0,
    orbsPosition: Int = 0,
    isAnimating: Boolean = false
) {
    // Screen configuration for responsive positioning
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    
    // ===== ORB POSITION CALCULATIONS =====
    
    // Orb 1 - Large top orb (lime green) - Top-right to Top-left
    val orb1X by animateDpAsState(
        targetValue = if (orbsPosition == 0) screenWidth - 60.dp else 60.dp,
        animationSpec = tween(
            durationMillis = 1000,
            easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f),
            delayMillis = 0
        ),
        label = "orb1X"
    )
    
    // Orb 2 - Medium middle orb (green) - Middle-left to Middle-right
    val orb2X by animateDpAsState(
        targetValue = if (orbsPosition == 0) 40.dp else screenWidth - 40.dp,
        animationSpec = tween(
            durationMillis = 1000,
            easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f),
            delayMillis = 150
        ),
        label = "orb2X"
    )
    
    // Orb 3 - Large bottom orb (lime green) - Bottom-right to Bottom-left
    val orb3X by animateDpAsState(
        targetValue = if (orbsPosition == 0) screenWidth - 80.dp else 80.dp,
        animationSpec = tween(
            durationMillis = 1000,
            easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f),
            delayMillis = 300
        ),
        label = "orb3X"
    )
    
    // ===== SCALE ANIMATIONS =====
    
    val orb1Scale by animateFloatAsState(
        targetValue = if (orbsPosition == 0) 1f else 1.1f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing, delayMillis = 0),
        label = "orb1Scale"
    )
    
    val orb2Scale by animateFloatAsState(
        targetValue = if (orbsPosition == 0) 1f else 0.9f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing, delayMillis = 150),
        label = "orb2Scale"
    )
    
    val orb3Scale by animateFloatAsState(
        targetValue = if (orbsPosition == 0) 1f else 1.1f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing, delayMillis = 300),
        label = "orb3Scale"
    )
    
    // ===== OPACITY ANIMATIONS =====
    
    val orb1Alpha by animateFloatAsState(
        targetValue = if (orbsPosition == 0) 0.8f else 0.6f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing, delayMillis = 0),
        label = "orb1Alpha"
    )
    
    val orb2Alpha by animateFloatAsState(
        targetValue = if (orbsPosition == 0) 0.8f else 0.7f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing, delayMillis = 150),
        label = "orb2Alpha"
    )
    
    val orb3Alpha by animateFloatAsState(
        targetValue = if (orbsPosition == 0) 0.8f else 0.6f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing, delayMillis = 300),
        label = "orb3Alpha"
    )
    
    // ===== FLOATING ANIMATION =====
    
    val floatingInfiniteTransition = rememberInfiniteTransition()
    
    val floatingOffset1 by floatingInfiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val floatingOffset2 by floatingInfiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val floatingOffset3 by floatingInfiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(modifier = modifier.fillMaxSize()) {
        // ===== ENHANCED ORB RENDERING =====
        
        // Orb 1 - Large top orb (lime green) - Top-right to Top-left
        Box(
            modifier = Modifier
                .size((120 * orb1Scale).dp)
                .offset(
                    x = orb1X,
                    y = (80.dp + floatingOffset1.dp)
                )
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFA3E635).copy(alpha = 0.9f),
                            Color(0xFF84CC16).copy(alpha = 0.7f),
                            Color(0xFF4D7C0F).copy(alpha = 0.5f),
                            Color.Transparent
                        ),
                        radius = 150f
                    ),
                    shape = CircleShape
                )
                .alpha(orb1Alpha)
        )
        
        // Orb 2 - Medium middle orb (green) - Middle-left to Middle-right
        Box(
            modifier = Modifier
                .size((80 * orb2Scale).dp)
                .offset(
                    x = orb2X,
                    y = (screenHeight * 0.4f + floatingOffset2.dp)
                )
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF84CC16).copy(alpha = 0.8f),
                            Color(0xFF4D7C0F).copy(alpha = 0.6f),
                            Color(0xFF365314).copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        radius = 100f
                    ),
                    shape = CircleShape
                )
                .alpha(orb2Alpha)
        )
        
        // Orb 3 - Large bottom orb (lime green) - Bottom-right to Bottom-left
        Box(
            modifier = Modifier
                .size((160 * orb3Scale).dp)
                .offset(
                    x = orb3X,
                    y = (screenHeight - 200.dp + floatingOffset3.dp)
                )
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFA3E635).copy(alpha = 0.8f),
                            Color(0xFF84CC16).copy(alpha = 0.6f),
                            Color(0xFF4D7C0F).copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        radius = 200f
                    ),
                    shape = CircleShape
                )
                .alpha(orb3Alpha)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentRegistrationScreenNew(
    navController: NavController,
    viewModel: StudentRegistrationViewModel = viewModel()
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    
    // Form state
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(1) } // 1 or 2
    
    // Animation states
    var headerVisible by remember { mutableStateOf(false) }
    var formVisible by remember { mutableStateOf(false) }
    var buttonVisible by remember { mutableStateOf(false) }
    
    // Year picker states
    var showAdmissionYearPicker by remember { mutableStateOf(false) }
    var showGraduationYearPicker by remember { mutableStateOf(false) }
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val admissionYearOptions = (2015..2040).toList() // Fixed range from 2015 to 2040
    val graduationYearOptions = (2015..2040).toList() // Fixed range from 2015 to 2040
    
    // Debug: Print year ranges
    LaunchedEffect(Unit) {
        println("🎯 Current year: $currentYear")
        println("🎯 Admission years: ${admissionYearOptions.first()} to ${admissionYearOptions.last()} (${admissionYearOptions.size} years)")
        println("🎯 Graduation years: ${graduationYearOptions.first()} to ${graduationYearOptions.last()} (${graduationYearOptions.size} years)")
    }
    
    // Focus requesters for step 1
    val fullNameFocus = remember { FocusRequester() }
    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    
    // Focus requesters for step 2
    val rollNumberFocus = remember { FocusRequester() }
    val departmentFocus = remember { FocusRequester() }
    val divisionFocus = remember { FocusRequester() }
    val batchFocus = remember { FocusRequester() }
    
    // Validation for step 1
    val isStep1Valid = viewModel.name.isNotBlank() && 
                      viewModel.email.isNotBlank() && 
                      password.isNotBlank()
    
    // Validation for step 2
    val isStep2Valid = viewModel.rollNumber.isNotBlank() &&
                      viewModel.department.isNotBlank() &&
                      viewModel.division.isNotBlank() &&
                      viewModel.batch.isNotBlank() &&
                      viewModel.admissionYear.isNotBlank() &&
                      viewModel.graduationYear.isNotBlank()
    
    // Debug validation state
    LaunchedEffect(isStep2Valid, currentStep) {
        if (currentStep == 2) {
            println("🔍 Step 2 Validation Debug:")
            println("   - Roll Number: '${viewModel.rollNumber}' (${viewModel.rollNumber.isNotBlank()})")
            println("   - Department: '${viewModel.department}' (${viewModel.department.isNotBlank()})")
            println("   - Division: '${viewModel.division}' (${viewModel.division.isNotBlank()})")
            println("   - Batch: '${viewModel.batch}' (${viewModel.batch.isNotBlank()})")
            println("   - Admission Year: '${viewModel.admissionYear}' (${viewModel.admissionYear.isNotBlank()})")
            println("   - Graduation Year: '${viewModel.graduationYear}' (${viewModel.graduationYear.isNotBlank()})")
            println("   - isStep2Valid: $isStep2Valid")
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A1A1A),
                        Color(0xFF2A2A2A),
                        Color(0xFF1E1E1E),
                        Color(0xFF262626)
                    )
                )
            )
    ) {
        // Background floating orbs
        FloatingOrbsBackground(
            orbPositions = 0,
            orbsPosition = 0,
            isAnimating = false
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            
            // Header Section
            androidx.compose.animation.AnimatedVisibility(
                visible = headerVisible,
                enter = fadeIn(
                    animationSpec = tween(1000, easing = EaseOutCubic)
                ) + slideInVertically(
                    animationSpec = tween(1000, easing = EaseOutCubic),
                    initialOffsetY = { -50 }
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Back button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        IconButton(
                            onClick = { 
                                if (currentStep == 1) {
                                    navController.navigate("role_selection") {
                                        popUpTo("role_selection") { inclusive = true }
                                    }
                                } else {
                                    currentStep = 1
                                }
                            },
                            modifier = Modifier
                                .background(
                                    color = Color.White.copy(alpha = 0.1f),
                                    shape = CircleShape
                                )
                                .size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Title
                    GradientText(
                        text = "Student Registration",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = if (currentStep == 1) "Step 1: Basic Information" else "Step 2: Academic Details",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Sign-in option
                    TextButton(
                        onClick = { 
                            navController.navigate("login")
                        }
                    ) {
                        Text(
                            text = "Already have an account? Sign In",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFA3E635),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Form Section
            androidx.compose.animation.AnimatedVisibility(
                visible = formVisible,
                enter = fadeIn(
                    animationSpec = tween(800, delayMillis = 300)
                ) + slideInVertically(
                    animationSpec = tween(800, delayMillis = 300),
                    initialOffsetY = { 80 }
                )
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        if (currentStep == 1) {
                            // Step 1: Basic Information
                            
                            // Full Name Field
                            ModernTextField(
                                value = viewModel.name,
                                onValueChange = { viewModel.name = it },
                                label = "Full Name",
                                placeholder = "Enter your full name",
                                leadingIcon = Icons.Default.Person,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { emailFocus.requestFocus() }
                                ),
                                modifier = Modifier.focusRequester(fullNameFocus)
                            )
                            
                            // Email Field
                            ModernTextField(
                                value = viewModel.email,
                                onValueChange = { viewModel.email = it },
                                label = "Email Address",
                                placeholder = "Enter your email",
                                leadingIcon = Icons.Default.Email,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { passwordFocus.requestFocus() }
                                ),
                                modifier = Modifier.focusRequester(emailFocus)
                            )
                            
                            // Password Field
                            ModernTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = "Password",
                                placeholder = "Create a password",
                                leadingIcon = Icons.Default.Lock,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { 
                                        keyboardController?.hide()
                                        if (isStep1Valid) {
                                            currentStep = 2
                                        }
                                    }
                                ),
                                modifier = Modifier.focusRequester(passwordFocus)
                            )
                            
                        } else {
                            // Step 2: Academic Details
                            
                            // Roll Number Field
                            ModernTextField(
                                value = viewModel.rollNumber,
                                onValueChange = { viewModel.rollNumber = it },
                                label = "Roll Number",
                                placeholder = "Enter your roll number",
                                leadingIcon = Icons.Default.Person,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { departmentFocus.requestFocus() }
                                ),
                                modifier = Modifier.focusRequester(rollNumberFocus)
                            )
                            
                            // Department Dropdown
                            ModernDropdownWithTyping(
                                value = viewModel.department,
                                onValueChange = { viewModel.department = it },
                                label = "Department",
                                placeholder = "Select Department",
                                options = viewModel.departmentOptions,
                                leadingIcon = Icons.Default.Info,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            // Division and Batch in a Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Division Dropdown
                                ModernDropdown(
                                    value = viewModel.division,
                                    onValueChange = { viewModel.division = it },
                                    label = "Division",
                                    placeholder = "Select Division",
                                    options = listOf("A", "B", "C", "D", "E", "F"),
                                    leadingIcon = Icons.Default.Info,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                // Batch Dropdown
                                ModernDropdown(
                                    value = viewModel.batch,
                                    onValueChange = { viewModel.batch = it },
                                    label = "Batch",
                                    placeholder = "Select Batch",
                                    options = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
                                    leadingIcon = Icons.Default.Info,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            // Admission and Graduation Year in a Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Admission Year Field (Clickable)
                                Box(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = viewModel.admissionYear.ifBlank { currentYear.toString() },
                                        onValueChange = { },
                                        label = { Text("Admission Year") },
                                        placeholder = { Text("2023") },
                                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                                        readOnly = true,
                                        enabled = false, // Disable the field completely
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFFA3E635),
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedLabelColor = Color(0xFFA3E635),
                                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                                            disabledBorderColor = Color.White.copy(alpha = 0.3f),
                                            disabledTextColor = Color.White,
                                            disabledLabelColor = Color.White.copy(alpha = 0.7f)
                                        )
                                    )
                                    
                                    // Transparent overlay to capture clicks - precise size matching the text field
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp) // Match OutlinedTextField height
                                            .clickable { 
                                                println("🎯 Admission year field clicked!")
                                                showAdmissionYearPicker = true 
                                            }
                                    )
                                }
                                
                                // Graduation Year Field (Clickable)
                                Box(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = viewModel.graduationYear.ifBlank { currentYear.toString() },
                                        onValueChange = { },
                                        label = { Text("Graduation Year") },
                                        placeholder = { Text("2027") },
                                        leadingIcon = { 
                                            Icon(
                                                Icons.Default.DateRange, 
                                                contentDescription = null
                                            ) 
                                        },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Select Year"
                                            )
                                        },
                                        readOnly = true,
                                        enabled = false, // Disable the field completely
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFFA3E635),
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedLabelColor = Color(0xFFA3E635),
                                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                                            disabledBorderColor = Color.White.copy(alpha = 0.3f),
                                            disabledTextColor = Color.White,
                                            disabledLabelColor = Color.White.copy(alpha = 0.7f)
                                        )
                                    )
                                    
                                    // Transparent overlay to capture clicks - precise size matching the text field
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp) // Match OutlinedTextField height
                                            .clickable { 
                                                println("🎯 Graduation year field clicked!")
                                                showGraduationYearPicker = true 
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Register Button
            androidx.compose.animation.AnimatedVisibility(
                visible = buttonVisible,
                enter = fadeIn(
                    animationSpec = tween(600, delayMillis = 600)
                ) + scaleIn(
                    animationSpec = tween(600, delayMillis = 600),
                    initialScale = 0.8f
                )
            ) {
                if (currentStep == 1) {
                    // Next button for step 1
                    ModernButton(
                        text = "Next",
                        onClick = {
                            if (isStep1Valid) {
                                currentStep = 2
                            }
                        },
                        enabled = isStep1Valid,
                        isLoading = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    )
                } else {
                    // Create button for step 2
                    ModernButton(
                        text = if (isLoading) "Creating..." else "Create",
                        onClick = {
                            println("🔘 Create button clicked!")
                            println("🔘 isStep2Valid: $isStep2Valid")
                            println("🔘 isLoading: $isLoading")
                            println("🔘 buttonVisible: $buttonVisible")
                            if (isStep2Valid && !isLoading) {
                                println("🔘 Starting registration...")
                                isLoading = true
                                viewModel.registerStudent { success, error ->
                                    isLoading = false
                                    if (success) {
                                        println("🔘 Registration successful, navigating...")
                                        navController.navigate("student_dashboard") {
                                            popUpTo("role_selection") { inclusive = true }
                                        }
                                    } else {
                                        println("🔘 Registration failed: $error")
                                    }
                                }
                            } else {
                                println("🔘 Button click ignored - validation failed or loading")
                            }
                        },
                        enabled = isStep2Valid && !isLoading,
                        isLoading = isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Terms and conditions
            androidx.compose.animation.AnimatedVisibility(
                visible = buttonVisible,
                enter = fadeIn(
                    animationSpec = tween(400, delayMillis = 800)
                )
            ) {
                Text(
                    text = "By creating an account, you agree to our Terms of Service and Privacy Policy",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
        
        // Admission Year Picker Dialog
        if (showAdmissionYearPicker) {
            AlertDialog(
                onDismissRequest = { showAdmissionYearPicker = false },
                title = { 
                    Text(
                        text = "Select Admission Year",
                        color = Color.White
                    ) 
                },
                text = {
                    YearScrollWheel(
                        selectedYear = viewModel.admissionYear.toIntOrNull() ?: currentYear,
                        onYearSelected = { year ->
                            viewModel.admissionYear = year.toString()
                        },
                        yearOptions = admissionYearOptions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp) // Compact height for 3 visible items
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { showAdmissionYearPicker = false }
                    ) {
                        Text("OK", color = Color(0xFFA3E635))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showAdmissionYearPicker = false }
                    ) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                },
                containerColor = Color(0xFF1A1A1A).copy(alpha = 0.98f)
            )
        }
        
        // Graduation Year Picker Dialog
        if (showGraduationYearPicker) {
            println("🎯 Graduation year picker dialog is showing!")
            AlertDialog(
                onDismissRequest = { 
                    println("🎯 Dialog dismissed")
                    showGraduationYearPicker = false 
                },
                title = { 
                    Text(
                        text = "Select Graduation Year",
                        color = Color.White
                    ) 
                },
                text = {
                    YearScrollWheel(
                        selectedYear = viewModel.graduationYear.toIntOrNull() ?: currentYear,
                        onYearSelected = { year ->
                            viewModel.graduationYear = year.toString()
                        },
                        yearOptions = graduationYearOptions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp) // Compact height for 3 visible items
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { showGraduationYearPicker = false }
                    ) {
                        Text("OK", color = Color(0xFFA3E635))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showGraduationYearPicker = false }
                    ) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                },
                containerColor = Color(0xFF1A1A1A).copy(alpha = 0.98f)
            )
        }
    }
    
    // Trigger staggered animations
    LaunchedEffect(Unit) {
        delay(300)
        headerVisible = true
        delay(400)
        formVisible = true
        delay(300)
        buttonVisible = true
    }
}
