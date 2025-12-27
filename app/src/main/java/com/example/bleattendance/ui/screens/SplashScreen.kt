// In ui/screens/SplashScreen.kt

package com.example.bleattendance.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bleattendance.model.UserRole
import com.example.bleattendance.ui.viewmodels.MainViewModel
import com.example.bleattendance.ui.components.*
import kotlinx.coroutines.delay
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val userRole by viewModel.userRole.collectAsState()
    val context = LocalContext.current

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

    LaunchedEffect(userRole) {
        // We add a small delay to prevent a jarring flash if the role is loaded instantly.
        delay(500)

        // ✅ THE FIX IS HERE
        // We remove the `if (userRole != UserRole.UNKNOWN)` check.
        // Now, the `when` block will run as soon as the role is loaded,
        // even if the loaded role is UNKNOWN (meaning the user is logged out).

        val destination = when (userRole) {
            UserRole.TEACHER -> "teacher_dashboard"
            UserRole.STUDENT -> "student_dashboard"
            // This case will now be triggered correctly on logout.
            UserRole.UNKNOWN -> "role_selection"
        }

        navController.navigate(destination) {
            popUpTo(navController.graph.startDestinationId) {
                inclusive = true
            }
        }
    }

    // Continuously apply fullscreen settings to ensure they persist
    LaunchedEffect(Unit) {
        val activity = context as? androidx.activity.ComponentActivity
        activity?.let {
            while (true) {
                // Re-apply fullscreen settings every 500ms to ensure they persist
                it.window.statusBarColor = android.graphics.Color.TRANSPARENT
                it.window.navigationBarColor = android.graphics.Color.TRANSPARENT
                it.window.decorView.systemUiVisibility = 
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    android.view.View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE
                it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
                it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                delay(500)
            }
        }
    }

    // This is the UI for the splash screen with dark gradient, floating orbs, and dropping dots
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
        // Background floating orbs with faster animation
        FastFloatingOrbsBackground()
        
        // Dropping dots animation
        DroppingDotsAnimation()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Custom radar logo - centered without any text
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

@Composable
private fun FastFloatingOrbsBackground(
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
