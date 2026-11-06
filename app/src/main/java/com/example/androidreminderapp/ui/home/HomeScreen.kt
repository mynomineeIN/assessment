package com.example.androidreminderapp.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.androidreminderapp.domain.model.Reminder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

private enum class AlarmFilter(val label: String) {
    ALL("All alarms"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    PAUSED("Paused")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddReminder: () -> Unit,
    onEditReminder: (Long) -> Unit,
    onLogout: () -> Unit
) {
    val reminders by viewModel.reminders.collectAsState()
    val cloudSyncError by viewModel.cloudSyncError.collectAsState()
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(AlarmFilter.ALL) }
    var reminderToDelete by remember { mutableStateOf<Reminder?>(null) }
    val now = System.currentTimeMillis()
    val today = remember(now) {
        Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val tomorrow = remember(today.timeInMillis) {
        (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
    }
    val activeReminders = reminders.filter { it.isActive && !it.isCompleted }
    val todayReminders = activeReminders.filter {
        it.timeInMillis >= today.timeInMillis && it.timeInMillis < tomorrow.timeInMillis
    }
    val upcomingReminders = activeReminders.filter { it.timeInMillis >= tomorrow.timeInMillis }
    val pausedReminders = reminders.filter { !it.isActive || it.isCompleted }
    val nextReminder = activeReminders
        .filter { it.timeInMillis >= now }
        .minByOrNull { it.timeInMillis }
    val visibleReminders = when (selectedFilter) {
        AlarmFilter.ALL -> reminders.sortedWith(
            compareBy<Reminder> { !it.isActive || it.isCompleted }.thenBy { it.timeInMillis }
        )
        AlarmFilter.TODAY -> todayReminders
        AlarmFilter.UPCOMING -> upcomingReminders
        AlarmFilter.PAUSED -> pausedReminders
    }

    LaunchedEffect(viewModel) {
        viewModel.synchronizeCloudReminders(context)
    }

    reminderToDelete?.let { reminder ->
        AlertDialog(
            onDismissRequest = { reminderToDelete = null },
            title = { Text("Delete this alarm?") },
            text = { Text("“${reminder.title}” will be removed from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteReminder(context, reminder)
                        reminderToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { reminderToDelete = null }) {
                    Text("Keep alarm")
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddReminder,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New alarm") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            StarryNightBackground()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 14.dp,
                    bottom = 104.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onLogout) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Sign out",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            "Alarm",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = onAddReminder) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add alarm",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                cloudSyncError?.let { message ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = message,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                TextButton(
                                    onClick = { viewModel.synchronizeCloudReminders(context) }
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CrescentMoon(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(174.dp)
                        )
                        Text(
                            "Next alarm",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = nextReminder?.let {
                                SimpleDateFormat("h:mm", Locale.getDefault()).format(Date(it.timeInMillis))
                            } ?: "All clear",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = nextReminder?.let {
                                "${SimpleDateFormat("a", Locale.getDefault()).format(Date(it.timeInMillis))}  ·  ${it.title}"
                            } ?: "Make a little room for what matters.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Alarm",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${activeReminders.size} active  ·  ${todayReminders.size} today",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { selectedFilter = AlarmFilter.ALL }) {
                            Text("See all")
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AlarmFilter.entries.forEach { filter ->
                            val count = when (filter) {
                                AlarmFilter.ALL -> reminders.size
                                AlarmFilter.TODAY -> todayReminders.size
                                AlarmFilter.UPCOMING -> upcomingReminders.size
                                AlarmFilter.PAUSED -> pausedReminders.size
                            }
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text("${filter.label}  $count") },
                                shape = RoundedCornerShape(50)
                            )
                        }
                    }
                }

                if (visibleReminders.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 30.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(34.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    if (reminders.isEmpty()) "A little reminder goes a long way"
                                    else "Nothing on this list",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    if (reminders.isEmpty()) {
                                        "Create an alarm for a check-in, a deadline, or anything worth remembering."
                                    } else {
                                        "Try another filter, or add a new alarm."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (reminders.isEmpty()) {
                                    Spacer(Modifier.height(12.dp))
                                    TextButton(onClick = onAddReminder) {
                                        Text("Create your first alarm")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(visibleReminders, key = { it.id }) { reminder ->
                        AlarmCard(
                            reminder = reminder,
                            onToggle = { viewModel.toggleReminderStatus(context, reminder) },
                            onEdit = { onEditReminder(reminder.id) },
                            onDelete = { reminderToDelete = reminder }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StarryNightBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF47084F),
                    Color(0xFF200D2A),
                    Color(0xFF100916),
                    Color(0xFF0B0810)
                )
            )
        )
        val random = Random(31)
        repeat(105) {
            val x = random.nextFloat() * size.width
            val y = random.nextFloat() * size.height
            val radius = random.nextFloat() * 1.5f + 0.35f
            drawCircle(
                color = Color(0xFFFFE9FF).copy(alpha = random.nextFloat() * 0.48f + 0.12f),
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun CrescentMoon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val moon = Path().apply {
            moveTo(size.width * 0.66f, size.height * 0.08f)
            cubicTo(
                size.width * 0.39f, size.height * 0.13f,
                size.width * 0.31f, size.height * 0.63f,
                size.width * 0.55f, size.height * 0.82f
            )
            cubicTo(
                size.width * 0.66f, size.height * 0.91f,
                size.width * 0.79f, size.height * 0.91f,
                size.width * 0.90f, size.height * 0.82f
            )
            cubicTo(
                size.width * 0.79f, size.height * 1.03f,
                size.width * 0.48f, size.height * 1.02f,
                size.width * 0.34f, size.height * 0.83f
            )
            cubicTo(
                size.width * 0.15f, size.height * 0.60f,
                size.width * 0.27f, size.height * 0.19f,
                size.width * 0.66f, size.height * 0.08f
            )
            close()
        }
        drawPath(
            path = moon,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFFD1F5), Color(0xFFE448D1), Color(0xFFB91AA4)),
                start = Offset(size.width * 0.25f, 0f),
                end = Offset(size.width * 0.78f, size.height)
            )
        )
        val random = Random(8)
        repeat(7) {
            val x = size.width * (0.12f + random.nextFloat() * 0.78f)
            val y = size.height * (0.08f + random.nextFloat() * 0.52f)
            val half = 2.dp.toPx()
            drawLine(
                Color.White.copy(alpha = 0.9f),
                Offset(x - half, y),
                Offset(x + half, y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                Color.White.copy(alpha = 0.9f),
                Offset(x, y - half),
                Offset(x, y + half),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

@Composable
private fun AlarmCard(
    reminder: Reminder,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()) }
    val repeatLabel = when (reminder.repeatFrequency) {
        "DAILY" -> "Daily"
        "WEEKLY" -> "Weekly"
        "MONTHLY" -> "Monthly"
        "YEARLY" -> "Yearly"
        else -> "Once"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    dateFormat.format(Date(reminder.timeInMillis)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (reminder.isCompleted) "Completed" else repeatLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!reminder.description.isNullOrBlank()) {
                    Text(
                        reminder.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = reminder.isActive,
                onCheckedChange = { onToggle() }
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete ${reminder.title}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
