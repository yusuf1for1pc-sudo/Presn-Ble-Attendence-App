package com.example.bleattendance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FileUploadComponent(
    onFileSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Upload File",
    acceptedFileTypes: String = "PDF, DOC, DOCX"
) {
    var isUploading by remember { mutableStateOf(false) }
    var uploadedFileName by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f)
            )
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 2.dp,
                    color = Color(0xFFA3E635),
                    shape = RoundedCornerShape(12.dp)
                )
                .background(Color(0xFF2A2A2A))
                .clickable {
                    // TODO: Implement actual file picker
                    // For now, simulate file upload
                    isUploading = true
                    uploadedFileName = "question_paper.pdf"
                    onFileSelected("https://example.com/uploads/question_paper.pdf")
                    isUploading = false
                },
            contentAlignment = Alignment.Center
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    color = Color(0xFFA3E635),
                    modifier = Modifier.size(24.dp)
                )
            } else if (uploadedFileName != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFFA3E635),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = uploadedFileName ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFFA3E635),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Tap to upload $acceptedFileTypes",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        }
    }
}
