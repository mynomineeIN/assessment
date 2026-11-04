package com.example.androidreminderapp.ui.home

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.androidreminderapp.domain.model.Reminder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF1F3037)
private val Mint = Color(0xFFBFE8D8)
private val Canvas = Color(0xFFF5F7F6)

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
    val dateFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }
    val periodFormat = remember { SimpleDateFormat("a", Locale.getDefault()) }

    if (reminderToDelete != null) {
        AlertDialog(
            onDismissRequest = { reminderToDelete = null },
            title = { Text("Delete this alarm?") },
            text = { Text("“${reminderToDelete?.title}” will be removed from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        reminderToDelete?.let { viewModel.deleteReminder(context, it) }
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
        containerColor = Canvas,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddReminder,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New alarm") },
                containerColor = Ink,
                contentColor = Color.White
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "NOTIFY",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Your alarms",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign out",
                            tint = Ink
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Ink)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                    ) {
                        Text(
                            dateFormat.format(Date(now)).uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelMedium,
                            color = Mint,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = nextReminder?.let { timeFormat.format(Date(it.timeInMillis)) } ?: "All clear",
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = nextReminder?.let {
                                "${periodFormat.format(Date(it.timeInMillis))}  ·  ${it.title}"
                            } ?: "Make a little room for what matters.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Mint,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(9.dp))
                            Text(
                                "${activeReminders.size} active  ·  ${todayReminders.size} today",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        label = "Active",
                        count = activeReminders.size,
                        accent = MaterialTheme.colorScheme.primary
                    )
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        label = "Completed",
                        count = reminders.count { it.isCompleted },
                        accent = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Your schedule",
                        style = MaterialTheme.typography.titleLarge,
                        color = Ink,
                        fontWeight = FontWeight.Bold
                    )
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
            }

            if (visibleReminders.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                                color = Ink
                            )
                            Text(
                                if (reminders.isEmpty()) "Create an alarm for a check-in, a deadline, or anything worth remembering."
                                else "Try another filter, or add a new alarm.",
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

@Composable
private fun SummaryCard(
    modifier: Modifier,
    label: String,
    count: Int,
    accent: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(3.dp))
            Text(count.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = accent)
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
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(if (reminder.isActive) Mint.copy(alpha = 0.55f) else Canvas, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (reminder.isActive) Ink else MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        reminder.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink
                    )
                    Text(
                        dateFormat.format(Date(reminder.timeInMillis)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = reminder.isActive,
                    onCheckedChange = { onToggle() }
                )
            }
            if (!reminder.description.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    reminder.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionChip(
                    onClick = onEdit,
                    label = { Text(if (reminder.isCompleted) "Completed" else repeatLabel) }
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete ${reminder.title}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
