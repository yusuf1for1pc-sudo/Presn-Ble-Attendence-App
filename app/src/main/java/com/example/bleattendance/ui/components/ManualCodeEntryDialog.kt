package com.example.bleattendance.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * ✅ ENHANCED: Dialog for manual session code entry when BLE pairing fails
 * Now includes compatibility for older devices (Xiaomi, Vivo, Oppo) with fallback mechanisms
 */
@Composable
fun ManualCodeEntryDialog(
    showDialog: Boolean = true,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var sessionCode by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    // Reset session code when dialog is dismissed
    LaunchedEffect(showDialog) {
        if (!showDialog) {
            sessionCode = ""
            isError = false
        }
    }

    CompatibleDialog(
        showDialog = showDialog,
        onDismiss = {
            println("🛑 CompatibleDialog: onDismiss called")
            onDismiss()
        },
        title = "Enter Session Code",
        icon = {
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                tint = Color(0xFFA3E635),
                modifier = Modifier.size(48.dp)
            )
        },
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "BLE connection failed. Please ask your teacher for the session code and enter it manually.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                CompatibleTextField(
                    value = sessionCode,
                    onValueChange = { 
                        sessionCode = it.uppercase().take(6) // Limit to 6 characters, uppercase
                        isError = false
                    },
                    label = "Session Code (e.g., ABC123)",
                    placeholder = "Enter 6-character code",
                    isError = isError || errorMessage != null,
                    errorMessage = errorMessage,
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This code should be displayed on your teacher's device.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        },
        dismissButton = {
            CompatibleButton(
                onClick = {
                    println("🛑 ManualCodeEntryDialog: Cancel button clicked")
                    onDismiss()
                    println("🛑 ManualCodeEntryDialog: onDismiss() called")
                },
                text = "Cancel",
                isPrimary = false,
                enabled = !isLoading
            )
        },
        confirmButton = {
            CompatibleButton(
                onClick = {
                    if (sessionCode.length >= 3) {
                        onConfirm(sessionCode)
                    } else {
                        isError = true
                    }
                },
                text = if (isLoading) "Connecting..." else "Connect",
                isPrimary = true,
                enabled = sessionCode.isNotEmpty() && !isLoading,
                isLoading = isLoading
            )
        }
    )
}

