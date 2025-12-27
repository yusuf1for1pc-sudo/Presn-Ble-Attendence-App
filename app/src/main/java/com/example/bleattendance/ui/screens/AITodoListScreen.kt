package com.example.bleattendance.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bleattendance.ui.components.GlassCard
import com.example.bleattendance.ui.components.GradientText
import com.example.bleattendance.ui.components.PremiumColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AITodoListScreen(
    onNavigateBack: () -> Unit
) {
    var selectedPriority by remember { mutableStateOf("all") }
    var todoItems by remember { mutableStateOf(getMockTodoItems()) }
    var showAddTaskDialog by remember { mutableStateOf(false) }

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
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(28.dp)
                        )
                        GradientText(
                            text = "Smart Planner",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddTaskDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Task",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Today's Plan Section
                item {
                    TodaysPlanCard(
                        onAddTask = { showAddTaskDialog = true }
                    )
                }

                // Priority Filter
                item {
                    PriorityFilter(
                        selectedPriority = selectedPriority,
                        onPrioritySelected = { selectedPriority = it }
                    )
                }

                // Todo Items by Priority
                val filteredItems = when (selectedPriority) {
                    "high" -> todoItems.filter { it.priority == TodoPriority.HIGH }
                    "medium" -> todoItems.filter { it.priority == TodoPriority.MEDIUM }
                    "low" -> todoItems.filter { it.priority == TodoPriority.LOW }
                    "week" -> todoItems.filter { it.priority == TodoPriority.WEEK }
                    else -> todoItems
                }

                items(filteredItems) { todoItem ->
                    TodoItemCard(
                        todoItem = todoItem,
                        onToggleComplete = { item ->
                            todoItems = todoItems.map { 
                                if (it.id == item.id) it.copy(isCompleted = !it.isCompleted) 
                                else it 
                            }
                        },
                        onDelete = { item ->
                            todoItems = todoItems.filter { it.id != item.id }
                        }
                    )
                }

                // Empty state
                if (filteredItems.isEmpty()) {
                    item {
                        EmptyStateCard(selectedPriority = selectedPriority)
                    }
                }
            }
        }

        // Add Task Dialog
        if (showAddTaskDialog) {
            AddTaskDialog(
                onDismiss = { showAddTaskDialog = false },
                onAddTask = { title, priority, dueDate ->
                    val newTask = TodoItem(
                        id = System.currentTimeMillis().toString(),
                        title = title,
                        priority = priority,
                        dueDate = dueDate,
                        isCompleted = false,
                        createdAt = System.currentTimeMillis()
                    )
                    todoItems = todoItems + newTask
                    showAddTaskDialog = false
                }
            )
        }
    }
}

@Composable
private fun TodaysPlanCard(
    onAddTask: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Plan",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "AI-generated priorities",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
                
                Button(
                    onClick = onAddTask,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8B5CF6)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Task",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun PriorityFilter(
    selectedPriority: String,
    onPrioritySelected: (String) -> Unit
) {
    val priorities = listOf(
        "all" to "All Tasks",
        "high" to "High Priority",
        "medium" to "Medium Priority", 
        "low" to "Low Priority",
        "week" to "This Week"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(priorities) { (key, label) ->
            FilterChip(
                onClick = { onPrioritySelected(key) },
                label = {
                    Text(
                        text = label,
                        color = if (selectedPriority == key) Color.White else Color.White.copy(alpha = 0.7f)
                    )
                },
                selected = selectedPriority == key,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF8B5CF6),
                    containerColor = Color.White.copy(alpha = 0.1f)
                )
            )
        }
    }
}

@Composable
private fun TodoItemCard(
    todoItem: TodoItem,
    onToggleComplete: (TodoItem) -> Unit,
    onDelete: (TodoItem) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Priority indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(getPriorityColor(todoItem.priority))
            )

            // Checkbox
            Checkbox(
                checked = todoItem.isCompleted,
                onCheckedChange = { onToggleComplete(todoItem) },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF8B5CF6),
                    uncheckedColor = Color.White.copy(alpha = 0.5f)
                )
            )

            // Task content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = todoItem.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = if (todoItem.isCompleted) 
                            Color.White.copy(alpha = 0.6f) 
                        else Color.White,
                        fontWeight = if (todoItem.isCompleted) FontWeight.Normal else FontWeight.Medium,
                        textDecoration = if (todoItem.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = getPriorityIcon(todoItem.priority),
                        contentDescription = null,
                        tint = getPriorityColor(todoItem.priority),
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Text(
                        text = getPriorityLabel(todoItem.priority),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = getPriorityColor(todoItem.priority),
                            fontWeight = FontWeight.Medium
                        )
                    )
                    
                    if (todoItem.dueDate != null) {
                        Text(
                            text = "•",
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Text(
                            text = formatDueDate(todoItem.dueDate),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }

            // Status badge (only show for overdue tasks)
            if (!todoItem.isCompleted && todoItem.dueDate != null && isOverdue(todoItem.dueDate)) {
                Text(
                    text = "Overdue",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            // Delete button
            IconButton(onClick = { onDelete(todoItem) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(selectedPriority: String) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            
            Text(
                text = when (selectedPriority) {
                    "high" -> "No high priority tasks"
                    "medium" -> "No medium priority tasks"
                    "low" -> "No low priority tasks"
                    "week" -> "No tasks for this week"
                    else -> "No tasks yet"
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White.copy(alpha = 0.7f)
                ),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Add a new task to get started!",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.5f)
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAddTask: (String, TodoPriority, Long?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(TodoPriority.MEDIUM) }
    var dueDate by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add New Task",
                color = Color.White
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title", color = Color.White.copy(alpha = 0.7f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF8B5CF6),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Priority selection
                Text(
                    text = "Priority",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TodoPriority.values().forEach { priority ->
                        FilterChip(
                            onClick = { selectedPriority = priority },
                            label = {
                                Text(
                                    text = getPriorityLabel(priority),
                                    color = if (selectedPriority == priority) Color.White else Color.White.copy(alpha = 0.7f)
                                )
                            },
                            selected = selectedPriority == priority,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = getPriorityColor(priority),
                                containerColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onAddTask(title, selectedPriority, dueDate)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B5CF6)
                )
            ) {
                Text("Add Task", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        },
        containerColor = Color(0xFF1E293B),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

// Helper functions
private fun getPriorityColor(priority: TodoPriority): Color {
    return when (priority) {
        TodoPriority.HIGH -> Color(0xFFEF4444)
        TodoPriority.MEDIUM -> Color(0xFFF59E0B)
        TodoPriority.LOW -> Color(0xFF10B981)
        TodoPriority.WEEK -> Color(0xFF3B82F6)
    }
}

private fun getPriorityIcon(priority: TodoPriority): androidx.compose.ui.graphics.vector.ImageVector {
    return when (priority) {
        TodoPriority.HIGH -> Icons.Default.Favorite
        TodoPriority.MEDIUM -> Icons.Default.Star
        TodoPriority.LOW -> Icons.Default.CheckCircle
        TodoPriority.WEEK -> Icons.Default.DateRange
    }
}

private fun getPriorityLabel(priority: TodoPriority): String {
    return when (priority) {
        TodoPriority.HIGH -> "High Priority"
        TodoPriority.MEDIUM -> "Medium Priority"
        TodoPriority.LOW -> "Low Priority"
        TodoPriority.WEEK -> "This Week"
    }
}

private fun formatDueDate(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val formatter = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
    return formatter.format(date)
}

private fun isOverdue(timestamp: Long): Boolean {
    return timestamp < System.currentTimeMillis()
}

private fun getMockTodoItems(): List<TodoItem> {
    return listOf(
        TodoItem(
            id = "1",
            title = "Complete Quadratic Equations Practice",
            priority = TodoPriority.HIGH,
            dueDate = System.currentTimeMillis() + 86400000, // Tomorrow
            isCompleted = true,
            createdAt = System.currentTimeMillis() - 86400000
        ),
        TodoItem(
            id = "2",
            title = "Review Network Security Notes",
            priority = TodoPriority.HIGH,
            dueDate = System.currentTimeMillis() + 172800000, // Day after tomorrow
            isCompleted = true,
            createdAt = System.currentTimeMillis() - 172800000
        ),
        TodoItem(
            id = "3",
            title = "Practice CSS Flexbox Layouts",
            priority = TodoPriority.MEDIUM,
            dueDate = System.currentTimeMillis() + 3600000, // 1 hour from now
            isCompleted = false,
            createdAt = System.currentTimeMillis() - 3600000
        ),
        TodoItem(
            id = "4",
            title = "Read Chapter 5 - Database Design",
            priority = TodoPriority.MEDIUM,
            dueDate = System.currentTimeMillis() + 259200000, // 3 days from now
            isCompleted = true,
            createdAt = System.currentTimeMillis() - 259200000
        ),
        TodoItem(
            id = "5",
            title = "Prepare for Network Security Quiz",
            priority = TodoPriority.WEEK,
            dueDate = System.currentTimeMillis() + 432000000, // 5 days from now
            isCompleted = false,
            createdAt = System.currentTimeMillis() - 432000000
        )
    )
}

// Data classes
enum class TodoPriority {
    HIGH, MEDIUM, LOW, WEEK
}

data class TodoItem(
    val id: String,
    val title: String,
    val priority: TodoPriority,
    val dueDate: Long?, // timestamp
    val isCompleted: Boolean,
    val createdAt: Long
)
