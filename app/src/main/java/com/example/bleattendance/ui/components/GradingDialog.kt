package com.example.bleattendance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun GradingDialog(
    studentName: String,
    maxMarks: Int,
    currentMarks: Int? = null,
    currentFeedback: String? = null,
    onDismiss: () -> Unit,
    onGrade: (marks: Int, feedback: String) -> Unit
) {
    var marks by remember { mutableStateOf(currentMarks?.toString() ?: "") }
    var feedback by remember { mutableStateOf(currentFeedback ?: "") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Grade Assignment",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    
                    IconButton(
                        onClick = onDismiss
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                
                // Student Info
                Text(
                    text = "Student: $studentName",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color(0xFFA3E635),
                        fontWeight = FontWeight.Medium
                    )
                )
                
                Text(
                    text = "Maximum Marks: $maxMarks",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.8f)
                    )
                )
                
                // Marks Input
                OutlinedTextField(
                    value = marks,
                    onValueChange = { 
                        marks = it
                        showError = false
                    },
                    label = {
                        Text(
                            text = "Marks Obtained",
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFF3B82F6),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                    ),
                    singleLine = true
                )
                
                // Feedback Input
                OutlinedTextField(
                    value = feedback,
                    onValueChange = { 
                        feedback = it
                        showError = false
                    },
                    label = {
                        Text(
                            text = "Feedback (Optional)",
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFF3B82F6),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                    ),
                    maxLines = 4
                )
                
                // Error Message
                if (showError) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFEF4444)
                        )
                    )
                }
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, 
                            Color.White.copy(alpha = 0.3f)
                        )
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            val marksValue = marks.toIntOrNull()
                            when {
                                marks.isBlank() -> {
                                    showError = true
                                    errorMessage = "Please enter marks"
                                }
                                marksValue == null -> {
                                    showError = true
                                    errorMessage = "Please enter valid marks"
                                }
                                marksValue < 0 -> {
                                    showError = true
                                    errorMessage = "Marks cannot be negative"
                                }
                                marksValue > maxMarks -> {
                                    showError = true
                                    errorMessage = "Marks cannot exceed maximum ($maxMarks)"
                                }
                                else -> {
                                    onGrade(marksValue, feedback.trim())
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B82F6)
                        )
                    ) {
                        Text(
                            text = if (currentMarks != null) "Update Grade" else "Submit Grade",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
