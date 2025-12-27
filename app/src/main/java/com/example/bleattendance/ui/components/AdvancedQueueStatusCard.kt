package com.example.bleattendance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bleattendance.ble.QueueStatus
import kotlinx.coroutines.delay

/**
 * ✅ ADVANCED QUEUE STATUS CARD
 * Real-time queue status display with animations and detailed metrics
 */
@Composable
fun AdvancedQueueStatusCard(
    queueStatus: QueueStatus?,
    isConnected: Boolean,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    var animationOffset by remember { mutableStateOf(0f) }
    
    // Animation for pulsing effect
    LaunchedEffect(isProcessing) {
        while (isProcessing) {
            animationOffset = 1f
            delay(1000)
            animationOffset = 0f
            delay(1000)
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(
                width = 2.dp,
                color = when {
                    isProcessing -> Color(0xFF4CAF50)
                    isConnected -> Color(0xFF2196F3)
                    queueStatus?.queueSize ?: 0 > 0 -> Color(0xFFFF9800)
                    else -> Color(0xFF9E9E9E)
                },
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with status icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        Icon(
                            imageVector = when {
                                isProcessing -> Icons.Default.CheckCircle
                                isConnected -> Icons.Default.CheckCircle
                                queueStatus?.queueSize ?: 0 > 0 -> Icons.Default.Info
                                else -> Icons.Default.Close
                            },
                        contentDescription = "Status",
                        tint = when {
                            isProcessing -> Color(0xFF4CAF50)
                            isConnected -> Color(0xFF2196F3)
                            queueStatus?.queueSize ?: 0 > 0 -> Color(0xFFFF9800)
                            else -> Color(0xFF9E9E9E)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Text(
                        text = when {
                            isProcessing -> "Processing Attendance"
                            isConnected -> "Connected to Teacher"
                            queueStatus?.queueSize ?: 0 > 0 -> "In Queue"
                            else -> "Disconnected"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isProcessing -> Color(0xFF4CAF50)
                            isConnected -> Color(0xFF2196F3)
                            queueStatus?.queueSize ?: 0 > 0 -> Color(0xFFFF9800)
                            else -> Color(0xFF9E9E9E)
                        }
                    )
                }
                
                // Processing animation
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF4CAF50),
                        strokeWidth = 2.dp
                    )
                }
            }
            
            // Queue position and wait time
            if (queueStatus != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Queue Position",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (queueStatus.queueSize > 0) "#${queueStatus.queueSize}" else "Ready",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (queueStatus.queueSize > 0) Color(0xFFFF9800) else Color(0xFF4CAF50)
                        )
                    }
                    
                    Column {
                        Text(
                            text = "Estimated Wait",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${queueStatus.estimatedWait}s",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (queueStatus.estimatedWait > 30) Color(0xFFFF5722) else Color(0xFF4CAF50)
                        )
                    }
                }
                
                // Progress bar for queue position
                if (queueStatus.queueSize > 0) {
                    val progress = (queueStatus.maxConnections - queueStatus.queueSize).toFloat() / queueStatus.maxConnections
                    LinearProgressIndicator(
                        progress = progress.coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = Color(0xFF4CAF50),
                        trackColor = Color(0xFFE0E0E0)
                    )
                }
                
                // Connection metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(
                        label = "Active",
                        value = "${queueStatus.activeConnections}",
                        icon = Icons.Default.Person,
                        color = Color(0xFF2196F3)
                    )
                    
                    MetricItem(
                        label = "Processing",
                        value = "${queueStatus.processingConnections}",
                        icon = Icons.Default.Refresh,
                        color = Color(0xFF4CAF50)
                    )
                    
                    MetricItem(
                        label = "Success Rate",
                        value = "${(queueStatus.successRate * 100).toInt()}%",
                        icon = Icons.Default.Info,
                        color = Color(0xFF4CAF50)
                    )
                }
                
                // Processing rate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Processing Rate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(queueStatus.processingRate * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50)
                    )
                }
            } else {
                // No queue status available
                Text(
                    text = "Waiting for connection...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * ✅ CONNECTION QUALITY INDICATOR
 * Visual indicator for connection quality
 */
@Composable
fun ConnectionQualityIndicator(
    quality: Float,
    modifier: Modifier = Modifier
) {
    val qualityColor = when {
        quality > 0.8f -> Color(0xFF4CAF50) // Excellent
        quality > 0.6f -> Color(0xFF8BC34A) // Good
        quality > 0.4f -> Color(0xFFFF9800) // Fair
        quality > 0.2f -> Color(0xFFFF5722) // Poor
        else -> Color(0xFFF44336) // Very Poor
    }
    
    val qualityText = when {
        quality > 0.8f -> "Excellent"
        quality > 0.6f -> "Good"
        quality > 0.4f -> "Fair"
        quality > 0.2f -> "Poor"
        else -> "Very Poor"
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Quality bars
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            repeat(5) { index ->
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height((8 + index * 4).dp)
                        .background(
                            color = if (quality > (index + 1) * 0.2f) qualityColor else Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
        
        Text(
            text = qualityText,
            style = MaterialTheme.typography.bodySmall,
            color = qualityColor,
            fontWeight = FontWeight.Medium
        )
    }
}
