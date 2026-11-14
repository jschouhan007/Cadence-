package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.time.DateTimeUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertTimePickerDialog(
  initialTimestampMillis: Long,
  onDismiss: () -> Unit,
  onConfirm: (Long) -> Unit,
  title: String = "Set alert time",
) {
  val now = remember { System.currentTimeMillis() }
  val todayStart = remember(now) { DateTimeUtils.startOfDay(now) }
  val tomorrowStart = remember(now) { DateTimeUtils.startOfDay(DateTimeUtils.addDays(now, 1)) }

  val initialCal = remember(initialTimestampMillis) {
    Calendar.getInstance().apply { timeInMillis = initialTimestampMillis }
  }
  var selectedDateMillis by remember(initialTimestampMillis) {
    mutableLongStateOf(DateTimeUtils.startOfDay(initialTimestampMillis))
  }
  val timeState =
    rememberTimePickerState(
      initialHour = initialCal.get(Calendar.HOUR_OF_DAY),
      initialMinute = initialCal.get(Calendar.MINUTE),
    )
  var showDatePicker by remember { mutableStateOf(false) }

  val previewMillis =
    DateTimeUtils.combineDateAndTime(selectedDateMillis, timeState.hour, timeState.minute)
  val isPast = previewMillis <= System.currentTimeMillis()

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    shape = RoundedCornerShape(24.dp),
    title = { Text(title, fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = DateTimeUtils.isSameDay(selectedDateMillis, todayStart),
            onClick = { selectedDateMillis = todayStart },
            label = { Text("Today") },
            shape = RoundedCornerShape(12.dp),
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
              ),
            modifier = Modifier.testTag("alert_picker_date_today"),
          )
          FilterChip(
            selected = DateTimeUtils.isSameDay(selectedDateMillis, tomorrowStart),
            onClick = { selectedDateMillis = tomorrowStart },
            label = { Text("Tomorrow") },
            shape = RoundedCornerShape(12.dp),
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
              ),
            modifier = Modifier.testTag("alert_picker_date_tomorrow"),
          )
          val isCustomDate =
            !DateTimeUtils.isSameDay(selectedDateMillis, todayStart) &&
              !DateTimeUtils.isSameDay(selectedDateMillis, tomorrowStart)
          FilterChip(
            selected = isCustomDate,
            onClick = { showDatePicker = true },
            label = {
              Text(
                if (isCustomDate) DateTimeUtils.formatShortDate(selectedDateMillis)
                else "Pick date"
              )
            },
            shape = RoundedCornerShape(12.dp),
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
              ),
            leadingIcon = {
              Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            modifier = Modifier.testTag("alert_picker_date_custom"),
          )
        }

        TimePicker(state = timeState)

        Text(
          text =
            if (isPast) "Choose a future time for this alert"
            else "Alert scheduled for ${DateTimeUtils.formatDateTimeShort(previewMillis)}",
          style = MaterialTheme.typography.labelMedium,
          color =
            if (isPast) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(previewMillis) },
        enabled = !isPast,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.testTag("alert_picker_confirm"),
      ) {
        Text("Set Alert")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("alert_picker_cancel"),
      ) {
        Text("Cancel")
      }
    },
  )

  if (showDatePicker) {
    val dateState =
      rememberDatePickerState(
        initialSelectedDateMillis = DateTimeUtils.localDateToUtcMidnight(selectedDateMillis)
      )
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            dateState.selectedDateMillis?.let { millis ->
              selectedDateMillis = DateTimeUtils.utcMidnightToLocalStartOfDay(millis)
            }
            showDatePicker = false
          }
        ) {
          Text("Select")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
      },
    ) {
      DatePicker(state = dateState)
    }
  }
}
