package com.example.bleattendance.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bleattendance.utils.BiometricHelper
import android.os.Build
import kotlinx.coroutines.delay

/**
 * ✅ ENHANCED: Dialog that handles biometric authentication for attendance marking
 * Now includes compatibility for older devices (Xiaomi, Vivo, Oppo) with fallback mechanisms
 */
@Composable
fun BiometricAuthDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    title: String = "Biometric Authentication",
    subtitle: String = "Please authenticate to mark attendance"
) {
    val context = LocalContext.current
    var activity by remember { mutableStateOf<androidx.fragment.app.FragmentActivity?>(null) }
    var biometricHelper by remember { mutableStateOf<BiometricHelper?>(null) }
    var authStatus by remember { mutableStateOf(AuthStatus.IDLE) }
    var deviceInfo by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(true) }
    
    // ✅ FIXED: Improved context casting with retry mechanism
    LaunchedEffect(Unit) {
        deviceInfo = "Device: ${Build.MANUFACTURER} ${Build.MODEL}\nAndroid: ${Build.VERSION.RELEASE}"
        
        // Try to cast context to FragmentActivity with retry
        var retryCount = 0
        val maxRetries = 3
        
        while (retryCount < maxRetries && activity == null) {
            try {
                activity = context as? androidx.fragment.app.FragmentActivity
                if (activity != null) {
                    biometricHelper = BiometricHelper(activity!!)
                    println("BiometricAuthDialog: Successfully cast context to FragmentActivity")
                    break
                }
            } catch (e: Exception) {
                println("BiometricAuthDialog: Context casting attempt $retryCount failed: ${e.message}")
            }
            
            if (activity == null) {
                delay(100) // Wait 100ms before retry
                retryCount++
            }
        }
        
        // Check biometric availability
        val helper = biometricHelper
        if (helper != null && !helper.isBiometricAvailable()) {
            authStatus = AuthStatus.UNAVAILABLE
        } else if (helper == null) {
            authStatus = AuthStatus.UNAVAILABLE
        } else {
            authStatus = AuthStatus.IDLE
        }
    }

    // ✅ ENHANCED: Use CompatibleDialog for better device compatibility
    CompatibleDialog(
        showDialog = showDialog,
        onDismiss = {
            showDialog = false
            onDismiss()
        },
        title = title,
        icon = {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFFA3E635),
                modifier = Modifier.size(48.dp)
            )
        },
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Subtitle
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Device info (for debugging)
                Text(
                    text = deviceInfo,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.5f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Status message
                when (authStatus) {
                    AuthStatus.IDLE -> {
                        Text(
                            text = "Ready to authenticate",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFA3E635)
                        )
                    }
                    AuthStatus.AUTHENTICATING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFFA3E635)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Authenticating...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                    AuthStatus.SUCCESS -> {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFFA3E635),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Authentication successful!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFA3E635)
                        )
                    }
                    AuthStatus.ERROR -> {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Authentication failed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFEF4444)
                        )
                    }
                    AuthStatus.UNAVAILABLE -> {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Unavailable",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Biometric authentication not available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFEF4444),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        dismissButton = {
            CompatibleButton(
                onClick = {
                    showDialog = false
                    onDismiss()
                },
                text = "Cancel",
                isPrimary = false
            )
        },
        confirmButton = {
            CompatibleButton(
                onClick = {
                    println("BiometricAuthDialog: Authenticate button clicked")
                    println("BiometricAuthDialog: Activity available: ${activity != null}")
                    println("BiometricAuthDialog: BiometricHelper available: ${biometricHelper != null}")
                    
                    authStatus = AuthStatus.AUTHENTICATING
                    biometricHelper?.authenticate(
                        title = title,
                        subtitle = subtitle,
                        onSuccess = {
                            println("BiometricAuthDialog: Authentication success")
                            authStatus = AuthStatus.SUCCESS
                            onSuccess()
                        },
                        onError = { error ->
                            println("BiometricAuthDialog: Authentication error - $error")
                            authStatus = AuthStatus.ERROR
                            onError(error)
                        }
                    )
                },
                enabled = authStatus != AuthStatus.UNAVAILABLE && biometricHelper != null,
                text = "Authenticate",
                isPrimary = true,
                isLoading = authStatus == AuthStatus.AUTHENTICATING
            )
        }
    )
}

// ✅ NEW: Enum for authentication status
enum class AuthStatus {
    IDLE,
    AUTHENTICATING,
    SUCCESS,
    ERROR,
    UNAVAILABLE
}

