package com.aditya.expent.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aditya.expent.data.remote.dto.PaymentModeResponseDto
import com.aditya.expent.domain.model.Reminder
import com.aditya.expent.domain.model.TransactionType
import com.aditya.expent.presentation.theme.ColorExpense
import com.aditya.expent.presentation.theme.ColorIncome

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderPaySheet(
    reminder: Reminder,
    accounts: List<PaymentModeResponseDto>,
    onDismiss: () -> Unit,
    onSave: (amount: Double, accountId: String, accountName: String) -> Unit
) {
    val isIncome = reminder.type == TransactionType.INCOME
    val accent = if (isIncome) ColorIncome else ColorExpense

    val usableAccounts = remember(accounts, isIncome) {
        if (isIncome) accounts.filter { it.type.equals("PAY_NOW", ignoreCase = true) } else accounts
    }

    var amountText by remember(reminder.id) {
        mutableStateOf(java.math.BigDecimal(reminder.amount.toString()).stripTrailingZeros().toPlainString())
    }
    var selectedAccount by remember(reminder.id, usableAccounts) { mutableStateOf(usableAccounts.firstOrNull()) }

    val amount = amountText.toDoubleOrNull()?.takeIf { it > 0 }
    val account = selectedAccount

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    text = if (isIncome) "Mark as received" else "Mark as paid",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${reminder.title} • ${reminder.description} • due ${reminder.dueDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ReadOnlyRow(label = "Category", value = reminder.category)

            OutlinedTextField(
                value = amountText,
                onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amountText = it },
                label = { Text("Amount") },
                prefix = { Text("$ ", fontWeight = FontWeight.Bold, color = accent) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedLabelColor = accent)
            )

            Text("Account", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            if (usableAccounts.isEmpty()) {
                Text(
                    text = if (isIncome) "Add a Pay Now account in Profile first." else "Add a payment mode in Profile first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    usableAccounts.forEach { item ->
                        val isSelected = item.id == account?.id
                        Surface(
                            onClick = { selectedAccount = item },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isSelected) accent.copy(alpha = 0.5f) else Color.Transparent)
                        ) {
                            Text(
                                text = item.name,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = { if (amount != null && account != null) onSave(amount, account.id, account.name) },
                enabled = amount != null && account != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ReadOnlyRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
