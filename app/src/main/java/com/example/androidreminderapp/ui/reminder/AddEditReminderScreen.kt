package com.example.androidreminderapp.ui.reminder

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReminderScreen(
    reminderId: Long? = null,
    viewModel: AddEditReminderViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(reminderId) {
        if (reminderId != null && reminderId > 0L) {
            viewModel.loadReminder(reminderId)
        }
    }

    val calendar = remember(viewModel.timeInMillis) {
        Calendar.getInstance().apply { timeInMillis = viewModel.timeInMillis }
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val updatedCal = Calendar.getInstance().apply {
                    timeInMillis = viewModel.timeInMillis
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                viewModel.timeInMillis = updatedCal.timeInMillis
                viewModel.startDate = updatedCal.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val timePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val updatedCal = Calendar.getInstance().apply {
                    timeInMillis = viewModel.timeInMillis
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }
                viewModel.timeInMillis = updatedCal.timeInMillis
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        )
    }

    val repeatUntilDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val updatedCal = Calendar.getInstance().apply {
                    timeInMillis = viewModel.repeatUntil ?: System.currentTimeMillis()
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                viewModel.repeatUntil = updatedCal.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (reminderId != null && reminderId > 0L) "Edit reminder" else "New reminder")
                        Text(
                            "Make time for what matters",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Plan ahead, effortlessly.",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Choose a time and we’ll make sure it’s on your schedule.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (viewModel.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = viewModel.errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Reminder details", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = viewModel.title,
                        onValueChange = { viewModel.title = it },
                        label = { Text("Reminder name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )

                    OutlinedTextField(
                        value = viewModel.description,
                        onValueChange = { viewModel.description = it },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }

            // Date and Time selectors
            Text("Date & Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(dateFormat.format(Date(viewModel.timeInMillis)))
                }

                OutlinedButton(
                    onClick = { timePickerDialog.show() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(timeFormat.format(Date(viewModel.timeInMillis)))
                }
            }

            // Repeat Frequency
            Text("Repeat Frequency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val frequencies = listOf("NONE" to "None", "DAILY" to "Daily", "WEEKLY" to "Weekly", "MONTHLY" to "Monthly", "YEARLY" to "Yearly")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                frequencies.forEach { (code, label) ->
                    FilterChip(
                        selected = viewModel.repeatFrequency == code,
                        onClick = { viewModel.repeatFrequency = code },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            if (viewModel.repeatFrequency == "WEEKLY") {
                Text("Select Weekdays", style = MaterialTheme.typography.titleSmall)
                val days = listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    days.forEach { (dayCode, dayLabel) ->
                        FilterChip(
                            selected = viewModel.selectedWeekDays.contains(dayCode),
                            onClick = {
                                viewModel.selectedWeekDays = if (viewModel.selectedWeekDays.contains(dayCode)) {
                                    viewModel.selectedWeekDays - dayCode
                                } else {
                                    viewModel.selectedWeekDays + dayCode
                                }
                            },
                            label = { Text(dayLabel, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Repeat Until
            Text("Repeat Until (Optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { repeatUntilDialog.show() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (viewModel.repeatUntil != null) dateFormat.format(Date(viewModel.repeatUntil!!)) else "No End Date")
                }
                if (viewModel.repeatUntil != null) {
                    TextButton(onClick = { viewModel.repeatUntil = null }) {
                        Text("Clear")
                    }
                }
            }

            // Snooze
            Text("Snooze Duration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val snoozeOptions = listOf(5, 10, 15, 30)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                snoozeOptions.forEach { minutes ->
                    FilterChip(
                        selected = viewModel.snoozeMinutes == minutes,
                        onClick = { viewModel.snoozeMinutes = minutes },
                        label = { Text("$minutes mins") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.saveReminder(context) {
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isSaving && viewModel.title.isNotBlank()
            ) {
                if (viewModel.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save alarm")
                }
            }
        }
    }
}
