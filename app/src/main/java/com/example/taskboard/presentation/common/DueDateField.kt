package com.example.taskboard.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.taskboard.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * A due-date row shared by the add and edit screens: shows the current date (or "no
 * due date") with a Material date picker to set or clear it. [onDueDateChange] is
 * called with the chosen date, or null when cleared.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DueDateField(dueDate: LocalDate?, onDueDateChange: (LocalDate?) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.due_date_label), style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = dueDate?.toString() ?: stringResource(R.string.due_date_none),
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { showPicker = true }) { Text(stringResource(R.string.due_date_set)) }
            if (dueDate != null) {
                TextButton(onClick = { onDueDateChange(null) }) { Text(stringResource(R.string.due_date_clear)) }
            }
        }
    }
    if (showPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate?.toUtcMillis())
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onDueDateChange(it.toLocalDate()) }
                    showPicker = false
                }) { Text(stringResource(R.string.dialog_add)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.dialog_cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** The date picker works in UTC-midnight millis; convert both ways so [LocalDate] round-trips. */
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
