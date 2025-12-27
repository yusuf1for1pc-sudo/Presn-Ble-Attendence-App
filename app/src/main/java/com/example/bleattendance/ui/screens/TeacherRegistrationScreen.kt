// In ui/screens/TeacherRegistrationScreen.kt

package com.example.bleattendance.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bleattendance.ui.viewmodels.TeacherRegistrationViewModel
import com.example.bleattendance.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalConfiguration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherRegistrationScreen(
    navController: NavController,
    viewModel: TeacherRegistrationViewModel = viewModel()
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // ===== ORB ANIMATION STATE MANAGEMENT =====
    var orbsPosition by remember { mutableStateOf(0) } // 0 = initial, 1 = moved
    var isAnimating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Form state - use ViewModel properties
    var isLoading by remember { mutableStateOf(false) }
    
    // Animation states
    var headerVisible by remember { mutableStateOf(false) }
    var formVisible by remember { mutableStateOf(false) }
    var buttonVisible by remember { mutableStateOf(false) }
    
    // Focus requesters
    val fullNameFocus = remember { FocusRequester() }
    val emailFocus = remember { FocusRequester() }
    val subjectFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    
    // Password visibility state
    var passwordVisible by remember { mutableStateOf(false) }
    
    // Validation
    val isFormValid = viewModel.name.isNotBlank() && 
                     viewModel.email.isNotBlank() && 
                     viewModel.subject.isNotBlank() &&
                     viewModel.password.isNotBlank()
    
    // Aggressive fullscreen experience - completely remove all system UI
    DisposableEffect(Unit) {
        val activity = context as? androidx.activity.ComponentActivity
        activity?.let {
            // Force fullscreen layout
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(it.window, false)
            
            // Hide all system bars
            val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(it.window, it.window.decorView)
            if (windowInsetsController != null) {
                windowInsetsController.systemBarsBehavior = 
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
            
            // Set transparent system bars
            it.window.statusBarColor = android.graphics.Color.TRANSPARENT
            it.window.navigationBarColor = android.graphics.Color.TRANSPARENT
            it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            
            // Force fullscreen with all possible flags
            it.window.decorView.systemUiVisibility = 
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                android.view.View.SYSTEM_UI_FLAG_LOW_PROFILE or
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE
            
            // Additional window flags for fullscreen
            it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
            it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }
        
        onDispose {
            activity?.let {
                it.window.statusBarColor = android.graphics.Color.TRANSPARENT
                it.window.navigationBarColor = android.graphics.Color.TRANSPARENT
                it.window.decorView.systemUiVisibility = 
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
        }
    }
    
    // ===== ORB ANIMATION SYSTEM =====
    
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
                                navController.navigate("role_selection") {
                                    popUpTo("role_selection") { inclusive = true }
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
                        text = "Teacher Registration",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Create your teacher account",
                        style = MaterialTheme.typography.bodyLarge,
                        color = PremiumColors.GlassWhite.copy(alpha = 0.7f),
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
                                onNext = { subjectFocus.requestFocus() }
                            ),
                            modifier = Modifier.focusRequester(emailFocus)
                        )
                        
                        // Subject Field
                        ModernTextField(
                            value = viewModel.subject,
                            onValueChange = { viewModel.subject = it },
                            label = "Subject",
                            placeholder = "Enter your subject",
                            leadingIcon = Icons.Default.Info,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { passwordFocus.requestFocus() }
                            ),
                            modifier = Modifier.focusRequester(subjectFocus)
                        )
                        
                        // Password Field
                        ModernTextField(
                            value = viewModel.password,
                            onValueChange = { viewModel.password = it },
                            label = "Password",
                            placeholder = "Create a password",
                            leadingIcon = Icons.Default.Lock,
                            trailingIcon = null,
                            onTrailingIconClick = null,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { 
                                    keyboardController?.hide()
                                    if (isFormValid && !isLoading) {
                                        // Handle registration
                                        viewModel.registerTeacher { success, error ->
                                            if (success) {
                                                navController.navigate("teacher_dashboard") {
                                                    popUpTo("role_selection") { inclusive = true }
                                                }
                                            } else {
                                                // Handle error - could show a snackbar or error message
                                                println("Registration failed: $error")
                                            }
                                        }
                                    }
                                }
                            ),
                            modifier = Modifier.focusRequester(passwordFocus)
                        )
                        
                        // Password visibility toggle
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
                ModernButton(
                    text = if (isLoading) "Creating..." else "Create",
                    onClick = {
                        if (isFormValid && !isLoading && !isAnimating) {
                            // Trigger enhanced orb animation
                            isAnimating = true
                            orbsPosition = if (orbsPosition == 0) 1 else 0
                            
                            // Reset animation flag after duration
                            scope.launch {
                                delay(1000) // animation duration
                                isAnimating = false
                            }
                            
                            // Register teacher
                            isLoading = true
                            viewModel.registerTeacher { success, error ->
                                isLoading = false
                                if (success) {
                                    navController.navigate("teacher_dashboard") {
                                        popUpTo("role_selection") { inclusive = true }
                                    }
                                } else {
                                    // Handle error - could show a snackbar or error message
                                    println("Registration failed: $error")
                                }
                            }
                        }
                    },
                    enabled = isFormValid && !isLoading && !isAnimating,
                    isLoading = isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )
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
                    color = PremiumColors.GlassWhite.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
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
        
        // Trigger orb animation after screen loads (simulating "Continue as Teacher" click)
        delay(500)
        isAnimating = true
        orbsPosition = 1
        
        // Reset animation flag after duration
        delay(1000)
        isAnimating = false
    }
    
    // Continuously apply aggressive fullscreen settings to ensure they persist
    LaunchedEffect(Unit) {
        val activity = context as? androidx.activity.ComponentActivity
        activity?.let {
            while (true) {
                // Force fullscreen layout
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(it.window, false)
                
                // Hide all system bars
                val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(it.window, it.window.decorView)
                if (windowInsetsController != null) {
                    windowInsetsController.systemBarsBehavior = 
                        androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                }
                
                // Set transparent system bars
                it.window.statusBarColor = android.graphics.Color.TRANSPARENT
                it.window.navigationBarColor = android.graphics.Color.TRANSPARENT
                it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                
                // Force fullscreen with all possible flags
                it.window.decorView.systemUiVisibility = 
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    android.view.View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE
                
                // Additional window flags for fullscreen
                it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
                it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                
                delay(300) // More frequent updates
            }
        }
    }
}
