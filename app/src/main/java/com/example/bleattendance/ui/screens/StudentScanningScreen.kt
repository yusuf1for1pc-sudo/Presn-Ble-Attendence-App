package com.example.bleattendance.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bleattendance.ui.components.*
import com.example.bleattendance.ui.viewmodels.StudentDashboardViewModel
import com.example.bleattendance.utils.BlePermissionChecker
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentScanningScreen(
    navController: NavController,
    viewModel: StudentDashboardViewModel = viewModel()
) {
    val student by viewModel.studentProfile.collectAsState()
    val showDialog by viewModel.showAttendanceDialog
    val showBiometricDialog by viewModel.showBiometricAuthDialog
    val showManualCodeDialog by viewModel.showManualCodeDialog
    val authenticationStatus by viewModel.authenticationStatus
    val manualCodeError by viewModel.manualCodeError
    val isManualCodeConnecting by viewModel.isManualCodeConnecting
    val isScanning by viewModel.isScanning
    
    // Create BLE permission checker
    val context = LocalContext.current
    val blePermissionChecker = remember { BlePermissionChecker(context) }
    val bleStatus = remember { blePermissionChecker.getBleStatus() }

    // Animation states
    var headerVisible by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }

    // Initialize GattClient with activity context for universal BLE compatibility
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            try {
                val activity = context as? androidx.activity.ComponentActivity
                if (activity != null) {
                    viewModel.initializeGattClient(activity)
                }
            } catch (e: Exception) {
                println("StudentScanningScreen: Failed to initialize GattClient: ${e.message}")
            }
        }
    }

    // Auto-start scanning when screen loads (only if BLE is available and not already scanning)
    LaunchedEffect(bleStatus.canUseBle) {
        if (bleStatus.canUseBle && !isScanning) {
            delay(1500) // Small delay to ensure everything is initialized
            if (!isScanning) { // Double-check to avoid starting scan if it's already running
                viewModel.checkBluetoothAndStartScan()
            }
        }
    }

    // Trigger animations
    LaunchedEffect(Unit) {
        delay(300)
        headerVisible = true
        delay(200)
        contentVisible = true
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
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
                Column {
                    // Top bar with back button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconButton(
                                onClick = { navController.popBackStack() }
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
                                        Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color(0xFFA3E635),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            
                            Column {
                                Text(
                                    text = "BLE Attendance",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = "Scan for attendance sessions",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Student profile card
                    GlassCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(
                                        color = Color(0xFFA3E635).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(25.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Student",
                                    tint = Color(0xFFA3E635),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column {
                                Text(
                                    text = "Welcome,",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp
                                    ),
                                    color = Color(0xFFA3E635)
                                )
                                Text(
                                    text = student?.name ?: "Student",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 20.sp
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Roll No: ${student?.rollNumber ?: ""}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Content Section
            androidx.compose.animation.AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(
                    animationSpec = tween(800, delayMillis = 300)
                ) + slideInVertically(
                    animationSpec = tween(800, delayMillis = 300),
                    initialOffsetY = { 80 }
                )
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // BLE Status Card - Only show when BLE is not ready or has issues
                    if (!bleStatus.canUseBle) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PermissionStatusCard(
                                bleStatus = bleStatus,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                    
                    // Status card showing scanning status
                    GlassCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isScanning) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFFA3E635)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Scanning for attendance sessions...",
                                        color = Color.White
                                    )
                                }
                            } else if (bleStatus.canUseBle) {
                                Text(
                                    "Ready to scan - scanning will start automatically",
                                    textAlign = TextAlign.Center,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            } else {
                                Text(
                                    viewModel.attendanceStatus.value,
                                    textAlign = TextAlign.Center,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Authentication status card
                    if (authenticationStatus.isNotEmpty()) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Authentication Status",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    authenticationStatus,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                
                                // ✅ ADDED: 30-second countdown display for presentation
                                if (authenticationStatus.contains("Syncing with teacher")) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Extract countdown number from status
                                    val countdownRegex = "\\((\\d+)s remaining\\)".toRegex()
                                    val matchResult = countdownRegex.find(authenticationStatus)
                                    val remainingSeconds = matchResult?.groupValues?.get(1)?.toIntOrNull() ?: 0
                                    
                                    // Progress bar showing countdown
                                    val progress = (30 - remainingSeconds) / 30f
                                    
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "⏱️ Connecting to teacher...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Progress bar
                                        LinearProgressIndicator(
                                            progress = progress,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp),
                                            color = Color(0xFF4CAF50),
                                            trackColor = Color.White.copy(alpha = 0.3f)
                                        )
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        Text(
                                            "${remainingSeconds}s remaining",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // ✅ ADDED: Queue Status Card for Students
                    if (authenticationStatus.contains("queue") || authenticationStatus.contains("Queue")) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "📋 Queue Status",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Extract queue position from status
                                val queueRegex = "#(\\d+) in queue".toRegex()
                                val queueMatch = queueRegex.find(authenticationStatus)
                                val queuePosition = queueMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                                
                                // Calculate estimated wait time (5 seconds per student)
                                val estimatedWait = queuePosition * 5
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "👥",
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        "You are #$queuePosition in queue",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "⏰",
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        "Estimated wait: ${estimatedWait}s",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    "⏳ Please wait while we process students ahead of you...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }


                    // Main action buttons
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Toggle Scan Button
                        Button(
                            onClick = { 
                                if (isScanning) {
                                    viewModel.stopScanning(foundDevice = false)
                                } else {
                                    viewModel.checkBluetoothAndStartScan()
                                }
                            },
                            enabled = bleStatus.canUseBle,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isScanning) Color(0xFFEF4444) else Color(0xFFA3E635)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    if (isScanning) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    if (isScanning) "Stop Scanning" else "Start Scanning",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        // ✅ ADDED: Manual Entry Fallback Button (appears when BLE fails)
                        if (authenticationStatus.contains("manual entry") || authenticationStatus.contains("BLE error") || authenticationStatus.contains("not found")) {
                            Button(
                                onClick = { viewModel.showManualCodeEntryDialog() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF9800)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "⚠️ BLE Failed - Enter Code Manually",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        } else {
                            // Regular Manual Code Button
                        Button(
                            onClick = { viewModel.showManualCodeEntryDialog() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                2.dp,
                                Color.White.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Enter Code Manually",
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
            }
        }

        // Show attendance dialog when detected
        if (showDialog) {
            AttendanceDialog(
                onConfirm = {
                    viewModel.onAttendanceDialogConfirmed()
                },
                onDismiss = {
                    viewModel.onAttendanceDialogDismissed()
                }
            )
        }

        // Show biometric authentication dialog
        if (showBiometricDialog) {
            BiometricAuthDialog(
                onDismiss = {
                    viewModel.onBiometricAuthFailed()
                },
                onSuccess = {
                    viewModel.onBiometricAuthSuccess()
                },
                onError = { error ->
                    viewModel.onBiometricAuthError(error)
                },
                title = "Mark Attendance",
                subtitle = "Please authenticate to mark your attendance"
            )
        }

        // Show manual code entry dialog
        if (showManualCodeDialog) {
            ManualCodeEntryDialog(
                onConfirm = { sessionCode ->
                    viewModel.onManualCodeConfirmed(sessionCode)
                },
                onDismiss = {
                    viewModel.onManualCodeDismissed()
                },
                isLoading = isManualCodeConnecting,
                errorMessage = manualCodeError
            )
        }
    }
}
