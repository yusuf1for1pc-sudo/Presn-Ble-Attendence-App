package com.example.bleattendance.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bleattendance.ui.components.GlassCard
import com.example.bleattendance.ui.components.ModernButton
import com.example.bleattendance.ui.components.ModernTextField
import com.example.bleattendance.ui.components.GradientText
import com.example.bleattendance.ui.components.PremiumColors
import com.example.bleattendance.ui.viewmodels.LoginViewModel
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = viewModel()
) {
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val loginSuccess by viewModel.loginSuccess.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    
    var passwordVisible by remember { mutableStateOf(false) }
    
    // ===== ORB ANIMATION STATE MANAGEMENT =====
    var orbsPosition by remember { mutableStateOf(0) } // 0 = initial, 1 = moved
    var isAnimating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Animation states
    var headerVisible by remember { mutableStateOf(false) }
    var formVisible by remember { mutableStateOf(false) }
    var buttonVisible by remember { mutableStateOf(false) }
    
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
    
    // Orb 4 - Small gray orb - Left to Right
    val orb4X by animateDpAsState(
        targetValue = if (orbsPosition == 0) 50.dp else screenWidth - 50.dp,
        animationSpec = tween(
            durationMillis = 1000,
            easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f),
            delayMillis = 450
        ),
        label = "orb4X"
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
    
    val orb4Scale by animateFloatAsState(
        targetValue = if (orbsPosition == 0) 1f else 0.95f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing, delayMillis = 450),
        label = "orb4Scale"
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
    
    val orb4Alpha by animateFloatAsState(
        targetValue = if (orbsPosition == 0) 0.5f else 0.4f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing, delayMillis = 450),
        label = "orb4Alpha"
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
    
    val floatingOffset4 by floatingInfiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // Handle login success
    LaunchedEffect(loginSuccess, userRole) {
        println("🔍 LoginScreen - loginSuccess: $loginSuccess, userRole: $userRole")
        if (loginSuccess && userRole != null) {
            println("🚀 Login successful! Navigating to dashboard for role: $userRole")
            delay(1000) // Show success message briefly
            when (userRole) {
                com.example.bleattendance.model.UserRole.STUDENT -> {
                    println("📱 Navigating to student dashboard")
                    navController.navigate("student_dashboard") {
                        popUpTo("role_selection") { inclusive = true }
                    }
                }
                com.example.bleattendance.model.UserRole.TEACHER -> {
                    println("📱 Navigating to teacher dashboard")
                    navController.navigate("teacher_dashboard") {
                        popUpTo("role_selection") { inclusive = true }
                    }
                }
                else -> {
                    println("❌ Unknown user role: $userRole")
                }
            }
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
        
        // Orb 4 - Small gray orb - Left to Right
        Box(
            modifier = Modifier
                .size((100 * orb4Scale).dp)
                .offset(
                    x = orb4X,
                    y = (200.dp + floatingOffset4.dp)
                )
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF666666).copy(alpha = 0.6f),
                            Color(0xFF444444).copy(alpha = 0.4f),
                            Color(0xFF222222).copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        radius = 75f
                    ),
                    shape = CircleShape
                )
                .alpha(orb4Alpha)
        )
        
        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
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
                    GradientText(
                        text = "Welcome Back",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Sign in to your account",
                        style = MaterialTheme.typography.bodyLarge,
                        color = PremiumColors.GlassWhite.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
            
            // Login Form
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Email Field
                        ModernTextField(
                            value = email,
                            onValueChange = viewModel::updateEmail,
                            label = "Email Address",
                            placeholder = "Enter your email",
                            leadingIcon = Icons.Default.Email,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Password Field
                        ModernTextField(
                            value = password,
                            onValueChange = viewModel::updatePassword,
                            label = "Password",
                            placeholder = "Enter your password",
                            leadingIcon = Icons.Default.Lock,
                            trailingIcon = null,
                            onTrailingIconClick = null,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Simple visibility toggle button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { passwordVisible = !passwordVisible }
                            ) {
                                Text(
                                    text = if (passwordVisible) "Hide" else "Show",
                                    color = Color(0xFFA3E635),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        
                        // Error Message
                        if (errorMessage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = errorMessage,
                                color = Color(0xFFFF6B6B),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Login Button
                        ModernButton(
                            onClick = { viewModel.login() },
                            text = if (isLoading) "Signing In..." else "Sign In",
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty(),
                            isLoading = isLoading
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Success Message
                        if (loginSuccess) {
                            Text(
                                text = "Login successful! Redirecting...",
                                color = Color(0xFF4ADE80),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Footer Links
            androidx.compose.animation.AnimatedVisibility(
                visible = buttonVisible,
                enter = fadeIn(
                    animationSpec = tween(600, delayMillis = 600)
                ) + scaleIn(
                    animationSpec = tween(600, delayMillis = 600),
                    initialScale = 0.8f
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextButton(
                        onClick = { 
                            navController.navigate("role_selection") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    ) {
                        Text(
                            text = "Don't have an account? Register",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFA3E635),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TextButton(
                        onClick = { 
                            // TODO: Implement forgot password functionality
                            println("Forgot password clicked")
                        }
                    ) {
                        Text(
                            text = "Forgot Password?",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
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
        
        // Trigger orb animation after screen loads
        delay(500)
        isAnimating = true
        orbsPosition = 1
        
        // Reset animation flag after duration
        delay(1000)
        isAnimating = false
    }
}
