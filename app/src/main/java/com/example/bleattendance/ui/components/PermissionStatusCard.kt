package com.example.bleattendance.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bleattendance.utils.BlePermissionChecker

@Composable
fun PermissionStatusCard(
    bleStatus: BlePermissionChecker.BleStatus,
    onRequestPermissions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                bleStatus.canUseBle -> MaterialTheme.colorScheme.primaryContainer
                bleStatus.isAvailable && bleStatus.isEnabled -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        bleStatus.canUseBle -> Icons.Default.CheckCircle
                        bleStatus.isAvailable && bleStatus.isEnabled -> Icons.Default.Warning
                        else -> Icons.Default.Close
                    },
                    contentDescription = null,
                    tint = when {
                        bleStatus.canUseBle -> Color.Green
                        bleStatus.isAvailable && bleStatus.isEnabled -> Color(0xFFFF9800)
                        else -> Color.Red
                    },
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.size(8.dp))
                
                Text(
                    text = "BLE Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Status message
            Text(
                text = bleStatus.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Detailed status items
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusItem(
                    icon = if (bleStatus.isAvailable) Icons.Default.CheckCircle else Icons.Default.Close,
                    text = "Bluetooth Available",
                    isAvailable = bleStatus.isAvailable,
                    iconTint = if (bleStatus.isAvailable) Color.Green else Color.Red
                )
                
                StatusItem(
                    icon = if (bleStatus.isEnabled) Icons.Default.CheckCircle else Icons.Default.Close,
                    text = "Bluetooth Enabled",
                    isAvailable = bleStatus.isEnabled,
                    iconTint = if (bleStatus.isEnabled) Color.Green else Color.Red
                )
                
                StatusItem(
                    icon = if (bleStatus.isSupported) Icons.Default.CheckCircle else Icons.Default.Close,
                    text = "BLE Supported",
                    isAvailable = bleStatus.isSupported,
                    iconTint = if (bleStatus.isSupported) Color.Green else Color.Red
                )
                
                StatusItem(
                    icon = if (bleStatus.missingPermissions.isEmpty()) Icons.Default.CheckCircle else Icons.Default.LocationOn,
                    text = "Permissions Granted",
                    isAvailable = bleStatus.missingPermissions.isEmpty(),
                    iconTint = if (bleStatus.missingPermissions.isEmpty()) Color.Green else Color(0xFFFF9800)
                )
            }
            
            // Show missing permissions if any
            if (bleStatus.missingPermissions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Missing Permissions:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                bleStatus.missingPermissions.forEach { permission ->
                    Text(
                        text = "• ${getPermissionDisplayName(permission)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Add permission request button
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Permissions")
                }
            }
        }
    }
}

@Composable
private fun StatusItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isAvailable: Boolean,
    iconTint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.size(8.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getPermissionDisplayName(permission: String): String {
    return when (permission) {
        "android.permission.BLUETOOTH_SCAN" -> "Bluetooth Scan"
        "android.permission.BLUETOOTH_ADVERTISE" -> "Bluetooth Advertise"
        "android.permission.BLUETOOTH_CONNECT" -> "Bluetooth Connect"
        "android.permission.BLUETOOTH" -> "Bluetooth"
        "android.permission.BLUETOOTH_ADMIN" -> "Bluetooth Admin"
        "android.permission.ACCESS_FINE_LOCATION" -> "Location"
        else -> permission
    }
}
