package com.aditya.expent.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aditya.expent.presentation.theme.ExpentTheme
import com.aditya.expent.utils.DayOfMonth

/**
 * Budgets and expenses repeat every month, so only the day of the month (1-31) is picked.
 * Days past the end of a short month fall on its last day.
 */
@Composable
fun DayOfMonthPicker(
    showDialog: Boolean,
    selectedDay: Int?,
    onDismiss: () -> Unit,
    onDaySelected: (Int) -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    if (!showDialog) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Day of every month", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                (DayOfMonth.MIN..DayOfMonth.MAX).chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        week.forEach { day ->
                            val isSelected = day == selectedDay
                            Surface(
                                onClick = { onDaySelected(day) },
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                shape = CircleShape,
                                color = if (isSelected) accentColor else Color.Transparent
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = day.toString(),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        // Keep the last row's cells the same width as the others
                        repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Field text for a picked day, e.g. "5th of every month". */
fun dayOfMonthLabel(day: Int?): String =
    day?.let { "${DayOfMonth.withSuffix(it)} of every month" }.orEmpty()

@Preview(showBackground = true)
@Composable
fun DayOfMonthPickerPreview() {
    ExpentTheme {
        DayOfMonthPicker(showDialog = true, selectedDay = 5, onDismiss = {}, onDaySelected = {})
    }
}
