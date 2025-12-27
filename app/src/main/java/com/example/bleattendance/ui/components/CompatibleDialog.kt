package com.example.bleattendance.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

/**
 * ✅ ENHANCED: Compatible dialog wrapper for older devices (Xiaomi, Vivo, Oppo)
 * Provides fallback mechanisms and device-specific optimizations
 */
@Composable
fun CompatibleDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    title: String,
    content: @Composable () -> Unit,
    confirmButton: @Composable (() -> Unit)? = null,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isOldDevice = remember {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val apiLevel = Build.VERSION.SDK_INT
        (manufacturer.contains("xiaomi") || manufacturer.contains("vivo") || 
         manufacturer.contains("oppo") || manufacturer.contains("realme")) && apiLevel < 31
    }
    
    var useFallbackDialog by remember { mutableStateOf(false) }
    var dialogVisible by remember { mutableStateOf(false) }
    
    // ✅ FIXED: Handle dialog visibility without race conditions
    LaunchedEffect(showDialog) {
        if (showDialog) {
            dialogVisible = true
            // ✅ FIXED: Use fallback immediately for older devices (no delay)
            if (isOldDevice) {
                useFallbackDialog = true
            }
        } else {
            // ✅ FIXED: Add small delay before hiding to prevent race condition
            delay(50)
            dialogVisible = false
            useFallbackDialog = false
        }
    }
    
    if (dialogVisible) {
        if (useFallbackDialog || isOldDevice) {
            // ✅ FALLBACK: Custom dialog for older devices
            FallbackDialog(
                onDismiss = onDismiss,
                title = title,
                content = content,
                confirmButton = confirmButton,
                dismissButton = dismissButton,
                icon = icon,
                modifier = modifier
            )
        } else {
            // ✅ STANDARD: Material3 AlertDialog for newer devices
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(16.dp),
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        icon?.invoke()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                    }
                },
                text = content,
                confirmButton = confirmButton ?: {},
                dismissButton = dismissButton ?: {},
                modifier = modifier
            )
        }
    }
}

/**
 * ✅ FALLBACK: Custom dialog implementation for older devices
 * Uses Dialog composable instead of AlertDialog for better compatibility
 */
@Composable
private fun FallbackDialog(
    onDismiss: () -> Unit,
    title: String,
    content: @Composable () -> Unit,
    confirmButton: @Composable (() -> Unit)?,
    dismissButton: @Composable (() -> Unit)?,
    icon: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Close button for older devices
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // Icon
                icon?.invoke()
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Content
                content()
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    dismissButton?.let {
                        Box(modifier = Modifier.weight(1f)) {
                            it()
                        }
                    }
                    
                    confirmButton?.let {
                        Box(modifier = Modifier.weight(1f)) {
                            it()
                        }
                    }
                }
            }
        }
    }
}

/**
 * ✅ DEVICE-SPECIFIC: Enhanced button styling for older devices
 */
@Composable
fun CompatibleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String,
    isPrimary: Boolean = true,
    isLoading: Boolean = false
) {
    val context = LocalContext.current
    val isOldDevice = remember {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val apiLevel = Build.VERSION.SDK_INT
        (manufacturer.contains("xiaomi") || manufacturer.contains("vivo") || 
         manufacturer.contains("oppo") || manufacturer.contains("realme")) && apiLevel < 31
    }
    
    if (isOldDevice) {
        // ✅ FALLBACK: Simple button for older devices
        Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPrimary) Color(0xFFA3E635) else Color.Transparent
            ),
            border = if (!isPrimary) androidx.compose.foundation.BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.3f)
            ) else null,
            shape = RoundedCornerShape(8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = if (isPrimary) Color.Black else Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = if (isPrimary) Color.Black else Color.White,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    } else {
        // ✅ STANDARD: Material3 button for newer devices
        Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPrimary) Color(0xFFA3E635) else Color.Transparent
            ),
            border = if (!isPrimary) androidx.compose.foundation.BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.3f)
            ) else null
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = if (isPrimary) Color.Black else Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = if (isPrimary) Color.Black else Color.White,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

/**
 * ✅ DEVICE-SPECIFIC: Enhanced text field for older devices
 */
@Composable
fun CompatibleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true
) {
    val context = LocalContext.current
    val isOldDevice = remember {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val apiLevel = Build.VERSION.SDK_INT
        (manufacturer.contains("xiaomi") || manufacturer.contains("vivo") || 
         manufacturer.contains("oppo") || manufacturer.contains("realme")) && apiLevel < 31
    }
    
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = label?.let { { Text(it) } },
            placeholder = placeholder?.let { { Text(it) } },
            singleLine = singleLine,
            isError = isError,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFA3E635),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedLabelColor = Color(0xFFA3E635),
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFFA3E635)
            ),
            shape = RoundedCornerShape(8.dp)
        )
        
        if (isError && errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFEF4444),
                textAlign = TextAlign.Start
            )
        }
    }
}
