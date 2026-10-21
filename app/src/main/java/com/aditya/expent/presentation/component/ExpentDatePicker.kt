package com.aditya.expent.presentation.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.aditya.expent.presentation.onboard.OutgoingStep
import com.aditya.expent.presentation.onboard.RecurringExpense
import com.aditya.expent.presentation.onboard.Subscription
import com.aditya.expent.presentation.theme.ExpentTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpentDatePicker(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit,
    allowFutureDates: Boolean = true
) {
    if (showDialog) {
        val datePickerState = rememberDatePickerState(
            selectableDates = if (allowFutureDates) {
                DatePickerDefaults.AllDates
            } else {
                object : SelectableDates {
                    private val endOfTodayUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        val local = Calendar.getInstance()
                        clear()
                        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 23, 59, 59)
                    }.timeInMillis

                    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= endOfTodayUtc

                    override fun isSelectableYear(year: Int): Boolean = year <= Calendar.getInstance().get(Calendar.YEAR)
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val selectedDateMillis = datePickerState.selectedDateMillis
                    if (selectedDateMillis != null) {
                        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val formattedDate = formatter.format(Date(selectedDateMillis))
                        onDateSelected(formattedDate)
                    } else {
                        onDismiss()
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ExpentDatePickerPreview() {
    ExpentTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ExpentDatePicker(
                showDialog = true,
                onDismiss = {},
                onDateSelected = {}
            )
        }
    }
}
