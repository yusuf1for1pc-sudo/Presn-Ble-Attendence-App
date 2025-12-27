package com.example.bleattendance.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TimetableView(
    classes: List<ClassScheduleData>,
    selectedDay: Int = 0, // 0 = Today, 1-6 = Mon-Sun
    onClassClick: (ClassScheduleData) -> Unit = {}
) {
    // Generate time slots dynamically based on all class times
    val timeSlots = remember(classes) {
        val allTimes = classes.flatMap { classData ->
            val startHour = classData.startTime.substringBefore(":").toInt()
            val endHour = classData.endTime.substringBefore(":").toInt()
            (startHour..endHour).map { "${it}:00" }
        }.distinct().sorted()
        
        // Ensure we always start from 9:00 and include all necessary time slots
        val minHour = minOf(9, allTimes.minOfOrNull { it.substringBefore(":").toInt() } ?: 9)
        val maxHour = allTimes.maxOfOrNull { it.substringBefore(":").toInt() } ?: 17
        
        // Generate time slots from 9:00 to max hour, ensuring 9:00 is always included
        val generatedTimes = (minHour..maxHour).map { "${it}:00" }
        generatedTimes.ifEmpty { listOf("9:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00") }
    }
    
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    val todayIndex = if (currentDay == Calendar.SUNDAY) 6 else currentDay - 2 // Convert to 0-6 (Mon-Sun)
    
    var selectedDayIndex by remember { mutableStateOf(selectedDay) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF000000),
                        Color(0xFF0A0A0A),
                        Color(0xFF111111),
                        Color(0xFF1A1A1A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with day selection
            DaySelector(
                days = days,
                selectedDay = selectedDayIndex,
                todayIndex = todayIndex,
                onDaySelected = { selectedDayIndex = it }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Check if there are any classes for the selected day
            val dayClasses = classes.filter { it.dayOfWeek == selectedDayIndex }
            
            if (dayClasses.isEmpty()) {
                // No classes message
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.White.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No classes today",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Enjoy your free time!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                // Timetable grid
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(timeSlots.size) { timeIndex ->
                    TimetableRow(
                        timeSlot = timeSlots[timeIndex],
                        classes = classes.filter { 
                            it.dayOfWeek == selectedDayIndex && 
                            isTimeInRange(timeSlots[timeIndex], it.startTime, it.endTime)
                        },
                        onClassClick = onClassClick
                    )
                    }
                }
            }
        }
    }
}

@Composable
private fun DaySelector(
    days: List<String>,
    selectedDay: Int,
    todayIndex: Int,
    onDaySelected: (Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(days.size) { index ->
            val isSelected = index == selectedDay
            val isToday = index == todayIndex
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        color = when {
                            isSelected -> Color(0xFFA3E635).copy(alpha = 0.2f)
                            isToday -> Color(0xFF2196F3).copy(alpha = 0.2f)
                            else -> Color.Transparent
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = when {
                            isSelected -> Color(0xFFA3E635)
                            isToday -> Color(0xFF2196F3)
                            else -> Color.White.copy(alpha = 0.1f)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onDaySelected(index) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = days[index],
                    color = when {
                        isSelected -> Color(0xFFA3E635)
                        isToday -> Color(0xFF2196F3)
                        else -> Color.White.copy(alpha = 0.7f)
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }
        }
    }
}

@Composable
private fun TimetableRow(
    timeSlot: String,
    classes: List<ClassScheduleData>,
    onClassClick: (ClassScheduleData) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        // Time column
        Box(
            modifier = Modifier
                .width(60.dp)
                .fillMaxHeight()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = timeSlot,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp
            )
        }
        
        // Time line
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.1f))
        )
        
        // Classes column
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (classes.isNotEmpty()) {
                classes.forEach { classData ->
                    ClassCard(
                        classData = classData,
                        onClick = { onClassClick(classData) }
                    )
                }
            } else {
                // Empty slot indicator
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun ClassCard(
    classData: ClassScheduleData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = classData.color.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .background(
                        color = classData.color,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = classData.subjectName,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    fontSize = 14.sp
                )
                Text(
                    text = classData.teacherName,
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp
                )
            }
            
            // Time indicator
            Text(
                text = "${classData.startTime}-${classData.endTime}",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp
            )
        }
    }
}

// Helper function to check if a time slot is within a class time range
private fun isTimeInRange(timeSlot: String, startTime: String, endTime: String): Boolean {
    return timeSlot == startTime || (timeSlot > startTime && timeSlot < endTime)
}

// Data class for timetable
data class ClassScheduleData(
    val subjectName: String,
    val teacherName: String,
    val teacherEmail: String,
    val startTime: String,
    val endTime: String,
    val dayOfWeek: Int, // 0-6 (Mon-Sun)
    val color: Color,
    val room: String = "",
    val classId: Int = 0,
    val groupId: String = ""
)
