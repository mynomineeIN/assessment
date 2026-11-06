package com.example.androidreminderapp.ui.reminder

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (reminderId != null && reminderId > 0L) "Edit time" else "Time",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { timePickerDialog.show() }) {
                        Text("Set")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Button(
                    onClick = {
                        viewModel.saveReminder(context) {
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .height(54.dp),
                    enabled = !viewModel.isSaving && viewModel.title.isNotBlank(),
                    shape = RoundedCornerShape(50)
                ) {
                    if (viewModel.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("◷   Set alarm")
                    }
                }
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NightGradient()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimeChoiceCard(
                        modifier = Modifier.weight(1f),
                        label = "Date",
                        value = dateFormat.format(Date(viewModel.timeInMillis)),
                        selected = false,
                        onClick = { datePickerDialog.show() }
                    )
                    TimeChoiceCard(
                        modifier = Modifier.weight(1f),
                        label = "Alarm time",
                        value = timeFormat.format(Date(viewModel.timeInMillis)),
                        selected = true,
                        onClick = { timePickerDialog.show() }
                    )
                }

                TimeDial(
                    time = timeFormat.format(Date(viewModel.timeInMillis)),
                    onClick = { timePickerDialog.show() }
                )

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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
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

            Text("Repeat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
            }
        }
    }
}

@Composable
private fun TimeChoiceCard(
    modifier: Modifier,
    label: String,
    value: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                Color(0xFFCF2DBD)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TimeDial(time: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(276.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(238.dp)) {
            val stroke = 16.dp.toPx()
            drawArc(
                color = Color(0xFF34283A),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(Color(0xFFE956D6), Color(0xFFB91AA4), Color(0xFFE956D6))
                ),
                startAngle = 135f,
                sweepAngle = 228f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            val center = Offset(size.width / 2f, size.height / 2f)
            for (index in 0 until 12) {
                val angle = Math.toRadians((index * 30 - 90).toDouble())
                val innerRadius = size.minDimension * 0.36f
                val outerRadius = size.minDimension * 0.39f
                val start = Offset(
                    center.x + kotlin.math.cos(angle).toFloat() * innerRadius,
                    center.y + kotlin.math.sin(angle).toFloat() * innerRadius
                )
                val end = Offset(
                    center.x + kotlin.math.cos(angle).toFloat() * outerRadius,
                    center.y + kotlin.math.sin(angle).toFloat() * outerRadius
                )
                drawLine(Color(0xFFBFAFC5), start, end, strokeWidth = 1.5.dp.toPx())
            }
            val markerAngle = Math.toRadians(43.0)
            val markerRadius = size.minDimension * 0.5f
            drawCircle(
                color = Color(0xFFFF79E8),
                radius = 11.dp.toPx(),
                center = Offset(
                    center.x + kotlin.math.cos(markerAngle).toFloat() * markerRadius,
                    center.y + kotlin.math.sin(markerAngle).toFloat() * markerRadius
                )
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "REMINDER TIME",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(time, style = MaterialTheme.typography.headlineMedium)
            Text("Tap to change", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NightGradient() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF3A0B46), Color(0xFF1E0F28), Color(0xFF100916))
            )
        )
        val random = kotlin.random.Random(31)
        repeat(75) {
            drawCircle(
                color = Color(0xFFFFE9FF).copy(alpha = random.nextFloat() * 0.4f + 0.1f),
                radius = random.nextFloat() * 1.4f + 0.35f,
                center = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height)
            )
        }
    }
}
