package com.example.bleattendance.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Responsive design helpers for consistent UI across all screens
@Composable
fun responsivePadding(): Dp {
    val configuration = LocalConfiguration.current
    return when {
        configuration.screenWidthDp >= 840 -> 32.dp  // Tablet
        configuration.screenWidthDp >= 600 -> 24.dp  // Large phone
        else -> 20.dp  // Small phone
    }
}

@Composable
fun responsiveSpacing(): Dp {
    val configuration = LocalConfiguration.current
    return when {
        configuration.screenWidthDp >= 840 -> 24.dp  // Tablet
        configuration.screenWidthDp >= 600 -> 20.dp  // Large phone
        else -> 16.dp  // Small phone
    }
}

@Composable
fun responsiveTextSize(baseSize: TextUnit): TextUnit {
    val configuration = LocalConfiguration.current
    val scaleFactor = when {
        configuration.screenWidthDp >= 840 -> 1.2f  // Tablet
        configuration.screenWidthDp >= 600 -> 1.1f  // Large phone
        else -> 1.0f  // Small phone
    }
    return (baseSize.value * scaleFactor).sp
}

@Composable
fun isTablet(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp >= 840
}

@Composable
fun isLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
}

