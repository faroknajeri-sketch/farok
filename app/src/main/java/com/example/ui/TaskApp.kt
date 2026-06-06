package com.example.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.TaskEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskApp(viewModel: TaskViewModel) {
    val context = LocalContext.current
    val tasks by viewModel.tasks.collectAsState()

    // Filter states
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedStatusFilter by remember { mutableStateOf("All") } // All, Pending, Completed

    // Bottom sheet states
    var isSheetOpen by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }

    // Alarm/Notification Permissions checks
    var showPermissionAlert by remember { mutableStateOf(false) }
    var hasPostNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPostNotificationPermission = isGranted
        if (!isGranted) {
            showPermissionAlert = true
        }
    }

    // Auto-trigger notification permissions request on Android 13+
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPostNotificationPermission) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Category preset arrays
    val categories = listOf("General", "Work", "Personal", "Shopping", "Health")
    val allCategoriesFilter = listOf("All") + categories

    // Filter tasks list locally based on selected filters
    val filteredTasks = remember(tasks, selectedCategoryFilter, selectedStatusFilter) {
        tasks.filter { task ->
            val matchesCategory = selectedCategoryFilter == "All" || task.category == selectedCategoryFilter
            val matchesStatus = when (selectedStatusFilter) {
                "Pending" -> !task.isDone
                "Completed" -> task.isDone
                else -> true
            }
            matchesCategory && matchesStatus
        }
    }

    // Calculation states for circular/linear progress counters
    val totalCount = tasks.size
    val doneCount = tasks.count { it.isDone }
    val completionProgress = if (totalCount > 0) doneCount.toFloat() / totalCount else 0.0f

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Daily Tasks",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        val dateString = remember {
                            SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
                        }
                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Interactive stats bubble icon
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { showPermissionAlert = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$doneCount/$totalCount",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress status slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's productivity",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(completionProgress * 100).toInt()}% Done",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { completionProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTask = null
                    isSheetOpen = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
                    .testTag("add_task_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add New Task",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Notification Warnings Alerts Panel
            if (showPermissionAlert) {
                AlertDialog(
                    onDismissRequest = { showPermissionAlert = false },
                    title = { Text("Task Alarms & Notifications") },
                    text = {
                        Text(
                            text = "To ensure you receive timely notifications for your scheduled tasks, please make sure Notifications permissions are enabled on your system settings."
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { showPermissionAlert = false }) {
                            Text("OK")
                        }
                    }
                )
            }

            // Segmented Status Filters Bar (Pending & Completed)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Pending", "Completed").forEach { status ->
                    val isSelected = selectedStatusFilter == status
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedStatusFilter = status },
                        label = { Text(status) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Categories Filter horizontal scrollbar
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allCategoriesFilter) { category ->
                    val isSelected = selectedCategoryFilter == category
                    InputChip(
                        selected = isSelected,
                        onClick = { selectedCategoryFilter = category },
                        label = { Text(category) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }

            // Core Items List
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Empty State",
                            modifier = Modifier
                                .size(64.dp)
                                .padding(bottom = 12.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "No tasks found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (tasks.isEmpty()) "Tap the '+' button to schedule your first event."
                                   else "Adjust your filters to see other logged entries.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskItemCard(
                            task = task,
                            onToggleDone = { isDone ->
                                viewModel.toggleTaskCompletion(task, isDone)
                            },
                            onEdit = {
                                editingTask = task
                                isSheetOpen = true
                            },
                            onDelete = {
                                viewModel.deleteTask(task)
                            }
                        )
                    }
                }
            }
        }
    }

    // Task Editing Bottom Sheet form
    if (isSheetOpen) {
        TaskFormBottomSheet(
            task = editingTask,
            categories = categories,
            onDismiss = { isSheetOpen = false },
            onSave = { title, description, category, priority, timeStr, timeMillis ->
                if (editingTask == null) {
                    // Create New
                    val newTask = TaskEntity(
                        title = title,
                        description = description,
                        category = category,
                        priority = priority,
                        time = timeStr,
                        timeInMillis = timeMillis
                    )
                    viewModel.addTask(newTask)
                } else {
                    // Update existing
                    val updatedTask = editingTask!!.copy(
                        title = title,
                        description = description,
                        category = category,
                        priority = priority,
                        time = timeStr,
                        timeInMillis = timeMillis
                    )
                    viewModel.updateTask(updatedTask)
                }
                isSheetOpen = false
            }
        )
    }
}

@Composable
fun TaskItemCard(
    task: TaskEntity,
    onToggleDone: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // Dynamic theme colors matching selected category
    val categoryColor = when (task.category) {
        "Work" -> Color(0xFF3F51B5)
        "Personal" -> Color(0xFF9C27B0)
        "Shopping" -> Color(0xFFFF9800)
        "Health" -> Color(0xFF009688)
        else -> Color(0xFF607D8B) // General
    }

    val priorityColor = when (task.priority) {
        "High" -> MaterialTheme.colorScheme.error
        "Medium" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isDone) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (task.isDone) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Task status circle checkbox
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (task.isDone) categoryColor else Color.Transparent
                    )
                    .border(
                        width = 2.dp,
                        color = if (task.isDone) categoryColor else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                    .clickable { onToggleDone(!task.isDone) }
                    .testTag("checkbox_${task.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (task.isDone) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Status Toggle Completed",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Body text area
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (task.isDone) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                    )

                    // Priority Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(priorityColor.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.priority,
                            style = MaterialTheme.typography.labelSmall,
                            color = priorityColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Time and Category tags footer row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category Badge Custom Tag color
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(categoryColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.category,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = categoryColor
                        )
                    }

                    // Schedule label tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notification Alarm",
                            tint = if (task.isDone) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = task.time,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (task.isDone) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Admin edit / delete actions
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("edit_task_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Task",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_task_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Task",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormBottomSheet(
    task: TaskEntity?,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, category: String, priority: String, timeStr: String, timeMillis: Long) -> Unit
) {
    val context = LocalContext.current

    // Local form parameters state
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var selectedCategory by remember { mutableStateOf(task?.category ?: "General") }
    var selectedPriority by remember { mutableStateOf(task?.priority ?: "Medium") }

    // Alarm Millis local parameters
    val calendar = remember {
        Calendar.getInstance().apply {
            if (task != null) {
                timeInMillis = task.timeInMillis
            } else {
                // Set initial default alarm to one hour ahead of current time
                add(Calendar.HOUR_OF_DAY, 1)
            }
        }
    }

    var selectedDateStr by remember {
        mutableStateOf(
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(calendar.time)
        )
    }

    var selectedTimeStr by remember {
        mutableStateOf(
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("task_form_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 44.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (task == null) "Schedule New Task" else "Update Slotted Task",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title") },
                placeholder = { Text("What needs to be done?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input"),
                singleLine = true,
                isError = title.trim().isEmpty()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                placeholder = { Text("Add details about this task") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )

            // Category selector buttons row
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Category Tag",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isSel = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSel) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Priority selection options
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Priority Level",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Low", "Medium", "High").forEach { priority ->
                        val isSelected = selectedPriority == priority
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .clickable { selectedPriority = priority }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = priority,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Due Time and Trigger Date buttons selection layout
            Text(
                text = "Reminder Schedule",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Trigger date button Picker
                OutlinedButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                calendar.set(Calendar.YEAR, year)
                                calendar.set(Calendar.MONTH, month)
                                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                selectedDateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(calendar.time)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(selectedDateStr, fontSize = 14.sp)
                }

                // Trigger timer button Picker
                OutlinedButton(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                calendar.set(Calendar.MINUTE, minute)
                                calendar.set(Calendar.SECOND, 0)
                                calendar.set(Calendar.MILLISECOND, 0)
                                selectedTimeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            false
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(selectedTimeStr, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action save button
            Button(
                onClick = {
                    if (title.trim().isNotEmpty()) {
                        val displayScheduleString = "$selectedDateStr @ $selectedTimeStr"
                        onSave(
                            title.trim(),
                            description.trim(),
                            selectedCategory,
                            selectedPriority,
                            displayScheduleString,
                            calendar.timeInMillis
                        )
                    }
                },
                enabled = title.trim().isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_task_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (task == null) "Schedule Alarm" else "Commit Updates",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
