package com.example.bleattendance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

@Composable
fun WeekTimetableView(
    classes: List<ClassScheduleData>,
    onClassClick: (ClassScheduleData) -> Unit = {}
) {
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    val todayIndex = if (currentDay == Calendar.SUNDAY) 6 else currentDay - 2
    
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(days.size) { dayIndex ->
                val dayName = days[dayIndex]
                val dayClasses = classes.filter { it.dayOfWeek == dayIndex }
                
                if (dayClasses.isNotEmpty()) {
                    DaySection(
                        dayName = dayName,
                        classes = dayClasses,
                        isToday = dayIndex == todayIndex,
                        onClassClick = onClassClick
                    )
                }
            }
        }
    }
}

@Composable
private fun DaySection(
    dayName: String,
    classes: List<ClassScheduleData>,
    isToday: Boolean,
    onClassClick: (ClassScheduleData) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Day header
        Text(
            text = dayName,
            color = if (isToday) Color(0xFFA3E635) else Color.White,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Classes list
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            classes.forEach { classData ->
                SimpleClassCard(
                    classData = classData,
                    onClick = { onClassClick(classData) }
                )
            }
        }
    }
}

@Composable
private fun SimpleClassCard(
    classData: ClassScheduleData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = classData.color.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(
                        color = classData.color,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = classData.subjectName,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = classData.teacherName,
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp
                )
                if (classData.room.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Room: ${classData.room}",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp
                    )
                }
            }
            
            // Time indicator
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${classData.startTime}-${classData.endTime}",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    fontSize = 14.sp
                )
            }
        }
    }
}
