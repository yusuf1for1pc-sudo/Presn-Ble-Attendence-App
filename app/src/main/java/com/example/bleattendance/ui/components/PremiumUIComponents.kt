package com.example.bleattendance.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Premium color palette inspired by modern startup apps
object PremiumColors {
    // Premium emerald green palette for fintech/wellness apps
    val PrimaryGreen = Color(0xFF10B981)  // Emerald green
    val SecondaryGreen = Color(0xFF059669) // Darker emerald
    val AccentTeal = Color(0xFF0D9488)     // Teal accent
    val DarkGreen = Color(0xFF064E3B)      // Deep forest green
    val GlassWhite = Color(0xFFFFFFFF)
    val GlassBackground = Color(0x1AFFFFFF)
    val TextPrimary = Color(0xFF1A1A2E)
    val TextSecondary = Color(0xFF666666)
}

@Composable
fun PremiumGradientBackground(
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    
    val animatedGradient by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient"
    )
    
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            PremiumColors.DarkGreen.copy(alpha = 0.65f),  // Dark top ~65%
            PremiumColors.SecondaryGreen,                 // Middle
            PremiumColors.PrimaryGreen,                   // Lighter bottom
            PremiumColors.AccentTeal                      // Teal accent
        ),
        startY = 0f,
        endY = Float.POSITIVE_INFINITY
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {
        // Subtle animated particles for depth
        repeat(8) { index ->
            val particleProgress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 4000 + (index * 300),
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "particle$index"
            )
            
            Box(
                modifier = Modifier
                    .size(2.dp + (index % 3).dp)
                    .offset(
                        x = (animatedGradient * 300 + index * 50).dp,
                        y = (particleProgress * 600 + index * 100).dp
                    )
                    .background(
                        color = PremiumColors.GlassWhite.copy(alpha = 0.08f),
                        shape = CircleShape
                    )
            )
        }
        
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Color(0xFF1A1A1A), // Darker shade
                shape = RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A) // Darker shade
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Box(
            modifier = Modifier
                .background(
                    Color(0xFF1A1A1A), // Darker shade
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            content()
        }
    }
}

@Composable
fun GradientText(
    text: String,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "textGradient")
    
    val animatedGradient by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "textGradient"
    )
    
    val textGradient = Brush.linearGradient(
        colors = listOf(
            PremiumColors.PrimaryGreen,
            PremiumColors.SecondaryGreen,
            PremiumColors.AccentTeal
        ),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(
            x = 200f * animatedGradient,
            y = 0f
        )
    )
    
    Text(
        text = text,
        style = style.copy(
            fontWeight = FontWeight.Bold,
            brush = textGradient
        ),
        modifier = modifier,
        textAlign = TextAlign.Center
    )
}

@Composable
fun ModernButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    // Modern gradient with neon orb colors
    val buttonGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFA3E635), // Bright lime green (neon orb)
            Color(0xFF84CC16), // Darker lime green (orb gradient)
            Color(0xFF4D7C0F)  // Deep green for depth
        ),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY)
    )
    
    // Animation for button press
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100)
    )
    
    Button(
        onClick = {
            if (!isLoading) {
                onClick()
            }
        },
        modifier = modifier
            .height(56.dp) // Height
            .width(200.dp) // Much wider button for testing
            .scale(scale)
            .clip(RoundedCornerShape(16.dp)) // Less rounded
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(buttonGradient, RoundedCornerShape(18.dp))
                .padding(horizontal = 20.dp, vertical = 16.dp), // More padding for wider button
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Creating...",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp,
                        fontSize = 16.sp
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { 
                Text(
                    text = label,
                    color = PremiumColors.GlassWhite.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                ) 
            },
            placeholder = { 
                Text(
                    text = placeholder,
                    color = PremiumColors.GlassWhite.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                ) 
            },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = PremiumColors.GlassWhite.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = trailingIcon?.let { icon ->
                {
                    IconButton(
                        onClick = { onTrailingIconClick?.invoke() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = PremiumColors.GlassWhite.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            isError = isError,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFF3A3A3A),
                    shape = RoundedCornerShape(12.dp)
                ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = PremiumColors.GlassWhite,
                fontWeight = FontWeight.Medium
            ),
            shape = RoundedCornerShape(12.dp)
        )
        
        if (isError && errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFFEF4444)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernDropdown(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    options: List<String>,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { 
                Text(
                    text = label,
                    color = PremiumColors.GlassWhite.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                ) 
            },
            placeholder = { 
                Text(
                    text = placeholder,
                    color = PremiumColors.GlassWhite.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                ) 
            },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = PremiumColors.GlassWhite.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFF3A3A3A),
                    shape = RoundedCornerShape(12.dp)
                )
                .menuAnchor(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = PremiumColors.GlassWhite,
                fontWeight = FontWeight.Medium
            ),
            shape = RoundedCornerShape(12.dp)
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(
                    Color(0xFF2A2A2A).copy(alpha = 0.98f),
                    RoundedCornerShape(16.dp)
                )
                .heightIn(max = 250.dp) // Increased height to show more options
        ) {
            options.forEach { option ->
                val isSelected = value == option
                
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = option,
                            color = PremiumColors.GlassWhite,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        ) 
                    },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                    modifier = Modifier.background(
                        if (isSelected) Color(0xFFA3E635).copy(alpha = 0.2f) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernDropdownWithTyping(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    options: List<String>,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var expanded by remember { mutableStateOf(false) }
    var textFieldValue by remember(value) { mutableStateOf(value) }
    
    // Filter options based on current text - show all when empty, filter when typing
    val filteredOptions = remember(textFieldValue) {
        if (textFieldValue.isBlank()) {
            options // Show all options when field is empty
        } else {
            options.filter { it.contains(textFieldValue, ignoreCase = true) }
        }
    }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { /* Don't let the box control expansion - we control it manually */ },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                onValueChange(newValue)
                // Always keep dropdown open while typing or when field is empty
                expanded = true
            },
            label = { 
                Text(
                    text = label,
                    color = PremiumColors.GlassWhite.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                ) 
            },
            placeholder = { 
                Text(
                    text = placeholder,
                    color = PremiumColors.GlassWhite.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                ) 
            },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = PremiumColors.GlassWhite.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = { 
                Row {
                    // Clear button when there's text
                    if (textFieldValue.isNotBlank()) {
                        IconButton(
                            onClick = {
                                textFieldValue = ""
                                onValueChange("")
                                expanded = true // Show all options when cleared
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = PremiumColors.GlassWhite.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    // Dropdown arrow
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFF3A3A3A),
                    shape = RoundedCornerShape(12.dp)
                )
                .menuAnchor(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = PremiumColors.GlassWhite,
                fontWeight = FontWeight.Medium
            ),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions
        )
        
        // Always show dropdown when expanded - show all options or filtered ones
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { /* Don't close on dismiss - only close on selection */ },
            modifier = Modifier
                .background(
                    Color(0xFF2A2A2A).copy(alpha = 0.98f),
                    RoundedCornerShape(16.dp)
                )
                .heightIn(max = 300.dp) // Increased height to show more options
        ) {
            // Show all options when field is empty, filtered when typing
            val optionsToShow = if (textFieldValue.isBlank()) options else filteredOptions
            
            if (optionsToShow.isEmpty()) {
                // Show "No matches found" when filtering returns no results
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = "No matches found",
                            color = PremiumColors.GlassWhite.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyMedium
                        ) 
                    },
                    onClick = { /* Do nothing */ }
                )
            } else {
                optionsToShow.forEach { option ->
                    val isSelected = textFieldValue == option
                    val isHighlighted = textFieldValue.isNotBlank() && option.contains(textFieldValue, ignoreCase = true)
                    
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = option,
                                color = if (isHighlighted) PremiumColors.PrimaryGreen else PremiumColors.GlassWhite,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        onClick = {
                            textFieldValue = option
                            onValueChange(option)
                            expanded = false
                        },
                        modifier = Modifier.background(
                            when {
                                isSelected -> PremiumColors.PrimaryGreen.copy(alpha = 0.2f)
                                isHighlighted -> PremiumColors.PrimaryGreen.copy(alpha = 0.1f)
                                else -> Color.Transparent
                            },
                            RoundedCornerShape(8.dp)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumIcon(
    iconText: String,
    size: androidx.compose.ui.unit.Dp = 64.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "iconGlow")
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconGlow"
    )
    
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        PremiumColors.PrimaryGreen.copy(alpha = glowAlpha),
                        Color.Transparent
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = iconText,
            fontSize = (size.value * 0.6).sp,
            color = PremiumColors.GlassWhite,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PremiumSectionTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GradientText(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 32.sp,
                letterSpacing = 1.sp
            )
        )
        
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = PremiumColors.GlassWhite.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}
