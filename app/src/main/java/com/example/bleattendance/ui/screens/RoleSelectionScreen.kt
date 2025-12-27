// In ui/screens/RoleSelectionScreen.kt

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
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bleattendance.model.UserRole
import com.example.bleattendance.ui.viewmodels.RoleViewModel
import com.example.bleattendance.ui.components.*
import com.example.bleattendance.utils.BlePermissionChecker
import com.example.bleattendance.utils.ActivityPermissionManager
import kotlinx.coroutines.delay
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun RoleSelectionScreen(
    navController: NavController,
    viewModel: RoleViewModel
) {
    val context = LocalContext.current
    val blePermissionChecker = remember { BlePermissionChecker(context) }
    var bleStatus by remember { mutableStateOf(blePermissionChecker.getBleStatus()) }
    
    // Animation states for staggered entrance
    var titleVisible by remember { mutableStateOf(false) }
    var subtitleVisible by remember { mutableStateOf(false) }
    var permissionVisible by remember { mutableStateOf(false) }
    var buttonsVisible by remember { mutableStateOf(false) }
    
    // Create permission manager for requesting permissions
    val permissionManager = remember { 
        try {
            ActivityPermissionManager(context as androidx.fragment.app.FragmentActivity)
        } catch (e: Exception) {
            null
        }
    }
    
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
        FloatingOrbsBackground()
        
        // Dropping dots animation for seamless transition
        DroppingDotsAnimation()
        
        // Subtle background blur overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 2.dp)
                .background(
                    Color.Black.copy(alpha = 0.1f)
                )
        )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
            // Brand logo and name
            androidx.compose.animation.AnimatedVisibility(
                visible = titleVisible,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 1200,
                        easing = androidx.compose.animation.core.EaseOutCubic
                    )
                ) + slideInVertically(
                    animationSpec = tween(
                        durationMillis = 1200,
                        easing = androidx.compose.animation.core.EaseOutCubic
                    ),
                    initialOffsetY = { -80 }
                ) + scaleIn(
                    animationSpec = tween(
                        durationMillis = 1200,
                        easing = androidx.compose.animation.core.EaseOutCubic
                    ),
                    initialScale = 0.8f
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Custom radar logo
                    GlassCard(
                        modifier = Modifier.size(120.dp)
                    ) {
                        AndroidView(
                            factory = { context ->
                                CompactRadarLogo(context).apply {
                                    layoutParams = android.view.ViewGroup.LayoutParams(
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // App name with gradient text - animated from background
                    androidx.compose.animation.AnimatedVisibility(
                        visible = titleVisible,
                        enter = fadeIn(
                            animationSpec = tween(
                                durationMillis = 2000,
                                delayMillis = 800,
                                easing = androidx.compose.animation.core.EaseOutCubic
                            )
                        ) + slideInVertically(
                            animationSpec = tween(
                                durationMillis = 2000,
                                delayMillis = 800,
                                easing = androidx.compose.animation.core.EaseOutCubic
                            ),
                            initialOffsetY = { 150 }
                        ) + scaleIn(
                            animationSpec = tween(
                                durationMillis = 2000,
                                delayMillis = 800,
                                easing = androidx.compose.animation.core.EaseOutCubic
                            ),
                            initialScale = 0.3f
                        )
                    ) {
                        GradientText(
                            text = "Presn",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
        Text(
                        text = "Smart Attendance System",
                        style = MaterialTheme.typography.bodyLarge,
                        color = PremiumColors.GlassWhite.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
                }
            }
        
            Spacer(modifier = Modifier.height(60.dp))
        
        // Show permission status if BLE is not ready
        if (!bleStatus.canUseBle) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = permissionVisible,
                    enter = fadeIn(animationSpec = tween(800, delayMillis = 400))
                ) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp)
                    ) {
            PermissionStatusCard(
                bleStatus = bleStatus,
                onRequestPermissions = {
                    permissionManager?.requestPermissions { _ ->
                        bleStatus = blePermissionChecker.refreshBleStatus()
                    }
                },
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            }
            
            // Modern role selection buttons
            androidx.compose.animation.AnimatedVisibility(
                visible = buttonsVisible,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 1000,
                        delayMillis = 200,
                        easing = androidx.compose.animation.core.EaseOutCubic
                    )
                ) + slideInVertically(
                    animationSpec = tween(
                        durationMillis = 1000,
                        delayMillis = 200,
                        easing = androidx.compose.animation.core.EaseOutCubic
                    ),
                    initialOffsetY = { 80 }
                )
            ) {
                TeacherStudentButtons(
                    onTeacherClick = {
                viewModel.saveRole(UserRole.TEACHER)
                navController.navigate("teacher_registration") {
                    popUpTo("role_selection") { inclusive = true }
                }
            },
                    onStudentClick = {
                viewModel.saveRole(UserRole.STUDENT)
                navController.navigate("student_registration") {
                    popUpTo("role_selection") { inclusive = true }
                }
            },
                    onLoginClick = {
                navController.navigate("login")
            },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    
    // Trigger staggered animations on screen load
    LaunchedEffect(Unit) {
        delay(500)
        titleVisible = true
        delay(400)
        subtitleVisible = true
        delay(300)
        permissionVisible = true
        delay(300)
        buttonsVisible = true
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

@Composable
private fun FloatingOrbsBackground(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Animation values for each orb - faster animation (4000ms instead of 8000ms)
    val orb1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -25f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val orb2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -25f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, delayMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val orb3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -25f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, delayMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(modifier = modifier.fillMaxSize()) {
        // Orb 1 - Green gradient, top right
        Box(
            modifier = Modifier
                .size(60.dp)
                .offset(x = (-30).dp, y = (80 + orb1Offset).dp)
                .align(Alignment.TopEnd)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFA3E635),
                            Color(0xFF84CC16)
                        )
                    ),
                    shape = CircleShape
                )
                .alpha(0.6f)
        )
        
        // Orb 2 - Darker green, middle left
        Box(
            modifier = Modifier
                .size(40.dp)
                .offset(x = (-20).dp, y = (300 + orb2Offset).dp)
                .align(Alignment.TopStart)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF84CC16),
                            Color(0xFF4D7C0F)
                        )
                    ),
                    shape = CircleShape
                )
                .alpha(0.6f)
        )
        
        // Orb 3 - Green gradient, bottom right
        Box(
            modifier = Modifier
                .size(80.dp)
                .offset(x = (-40).dp, y = (-120 + orb3Offset).dp)
                .align(Alignment.BottomEnd)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFA3E635),
                            Color(0xFF84CC16)
                        )
                    ),
                    shape = CircleShape
                )
                .alpha(0.6f)
        )
        
        // Optional: Gray orbs for variety
        Box(
            modifier = Modifier
                .size(50.dp)
                .offset(x = 30.dp, y = 200.dp)
                .align(Alignment.TopStart)
                .background(
                    color = Color(0xFF666666),
                    shape = CircleShape
                )
                .alpha(0.3f)
        )
    }
}

@Composable
private fun TeacherStudentButtons(
    onTeacherClick: () -> Unit,
    onStudentClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var teacherPressed by remember { mutableStateOf(false) }
    var studentPressed by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Teacher Button - Lime green gradient (matching the floating orbs)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .scale(if (teacherPressed) 0.96f else 1f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            teacherPressed = true
                            tryAwaitRelease()
                            teacherPressed = false
                        },
                        onTap = { onTeacherClick() }
                    )
                },
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (teacherPressed) 4.dp else 8.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFA3E635), // Lime green (matching floating orbs)
                                Color(0xFF84CC16)  // Darker lime
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(vertical = 20.dp, horizontal = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Continue as Teacher",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A) // Dark text on lime background
                )
            }
        }
        
        // Student Button - Glassmorphism effect
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .scale(if (studentPressed) 0.96f else 1f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            studentPressed = true
                            tryAwaitRelease()
                            studentPressed = false
                        },
                        onTap = { onStudentClick() }
                    )
                },
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (studentPressed) 4.dp else 8.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White.copy(alpha = 0.15f), // Glassmorphism background
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(vertical = 20.dp, horizontal = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Continue as Student",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
        
        // Login Button - Subtle outline style
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onLoginClick() }
                    )
                },
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(vertical = 16.dp, horizontal = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Already have an account? Sign In",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun DroppingDotsAnimation() {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Create multiple dots with different animation delays
    repeat(8) { index ->
        val dropOffset by infiniteTransition.animateFloat(
            initialValue = -100f,
            targetValue = 1200f, // Drop below screen
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 3000 + (index * 200), // Different speeds
                    delayMillis = index * 300, // Staggered start
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            )
        )
        
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1000,
                    delayMillis = index * 300,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            )
        )
        
        Box(
            modifier = Modifier
                .offset(
                    x = (index * 50).dp,
                    y = dropOffset.dp
                )
                .size(8.dp)
                .background(
                    color = Color(0xFF4ADE80).copy(alpha = alpha * 0.6f),
                    shape = CircleShape
                )
        )
    }
}