package com.example.bleattendance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.bleattendance.ble.QueuedStudent
import kotlinx.coroutines.delay

/**
 * ✅ ADVANCED TEACHER DASHBOARD
 * Comprehensive dashboard with real-time stats and queue management
 */
@Composable
fun AdvancedTeacherDashboard(
    queueStatus: QueueStatus?,
    queuedStudents: List<QueuedStudent>,
    processedCount: Int,
    isServerRunning: Boolean,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
    onClearQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    var animationOffset by remember { mutableStateOf(0f) }
    
    // Animation for active processing
    LaunchedEffect(isServerRunning) {
        while (isServerRunning) {
            animationOffset = 1f
            delay(1000)
            animationOffset = 0f
            delay(1000)
        }
    }
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Server Status Card
        item {
            ServerStatusCard(
                isRunning = isServerRunning,
                onStart = onStartServer,
                onStop = onStopServer,
                animationOffset = animationOffset
            )
        }
        
        // Real-time Stats Card
        item {
            RealTimeStatsCard(
                queueStatus = queueStatus,
                processedCount = processedCount
            )
        }
        
        // Queue Management Card
        item {
            QueueManagementCard(
                queueStatus = queueStatus,
                queuedStudents = queuedStudents,
                onClearQueue = onClearQueue
            )
        }
        
        // Connection Quality Card
        item {
            ConnectionQualityCard(
                queueStatus = queueStatus
            )
        }
        
        // Performance Metrics Card
        item {
            PerformanceMetricsCard(
                queueStatus = queueStatus
            )
        }
    }
}

@Composable
private fun ServerStatusCard(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    animationOffset: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = if (isRunning) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
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
                        imageVector = if (isRunning) Icons.Default.PlayArrow else Icons.Default.Close,
                        contentDescription = "Server Status",
                        tint = if (isRunning) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Text(
                        text = if (isRunning) "Server Running" else "Server Stopped",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                    )
                }
                
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF4CAF50),
                        strokeWidth = 2.dp
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = if (isRunning) onStop else onStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) Color(0xFFF44336) else Color(0xFF4CAF50)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Stop" else "Start",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRunning) "Stop Server" else "Start Server")
                }
            }
        }
    }
}

@Composable
private fun RealTimeStatsCard(
    queueStatus: QueueStatus?,
    processedCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Real-time Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (queueStatus != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        label = "Active",
                        value = "${queueStatus.activeConnections}",
                        icon = Icons.Default.Person,
                        color = Color(0xFF2196F3)
                    )
                    
                    StatItem(
                        label = "Processing",
                        value = "${queueStatus.processingConnections}",
                        icon = Icons.Default.Refresh,
                        color = Color(0xFF4CAF50)
                    )
                    
                    StatItem(
                        label = "Queue",
                        value = "${queueStatus.queueSize}",
                        icon = Icons.Default.Info,
                        color = Color(0xFFFF9800)
                    )
                    
                    StatItem(
                        label = "Processed",
                        value = "$processedCount",
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF4CAF50)
                    )
                }
                
                // Success rate and processing rate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Success Rate: ${(queueStatus.successRate * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50)
                    )
                    
                    Text(
                        text = "Processing Rate: ${(queueStatus.processingRate * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2196F3)
                    )
                }
            } else {
                Text(
                    text = "No data available",
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
private fun QueueManagementCard(
    queueStatus: QueueStatus?,
    queuedStudents: List<QueuedStudent>,
    onClearQueue: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Queue Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (queuedStudents.isNotEmpty()) {
                    Button(
                        onClick = onClearQueue,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Queue",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear", fontSize = 12.sp)
                    }
                }
            }
            
            if (queuedStudents.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(queuedStudents) { student ->
                        QueuedStudentItem(student = student)
                    }
                }
            } else {
                Text(
                    text = "No students in queue",
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
private fun QueuedStudentItem(
    student: QueuedStudent
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = student.studentInfo.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = student.studentInfo.rollNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Position #${student.queuePosition}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFFF9800)
                )
                Text(
                    text = "Wait: ${student.estimatedWait}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConnectionQualityCard(
    queueStatus: QueueStatus?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Connection Quality",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (queueStatus != null) {
                // Overall quality indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Overall Quality",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    ConnectionQualityIndicator(
                        quality = queueStatus.successRate,
                        modifier = Modifier
                    )
                }
                
                // Processing efficiency
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Processing Efficiency",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    ConnectionQualityIndicator(
                        quality = queueStatus.processingRate,
                        modifier = Modifier
                    )
                }
            } else {
                Text(
                    text = "No connection data available",
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
private fun PerformanceMetricsCard(
    queueStatus: QueueStatus?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Performance Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (queueStatus != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(
                        label = "Max Connections",
                        value = "${queueStatus.maxConnections}",
                        icon = Icons.Default.Settings,
                        color = Color(0xFF2196F3)
                    )
                    
                    MetricItem(
                        label = "Total Connections",
                        value = "${queueStatus.totalConnections}",
                        icon = Icons.Default.Share,
                        color = Color(0xFF4CAF50)
                    )
                    
                    MetricItem(
                        label = "Estimated Wait",
                        value = "${queueStatus.estimatedWait}s",
                        icon = Icons.Default.Info,
                        color = Color(0xFFFF9800)
                    )
                }
            } else {
                Text(
                    text = "No performance data available",
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
private fun StatItem(
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
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
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
