package com.aditya.expent.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aditya.expent.config.AppText
import com.aditya.expent.presentation.theme.ColorExpense

/**
 * Confirms permanent account deletion. The user types a confirmation word so a stray tap can't
 * delete everything. Can't be dismissed while the request is running.
 */
@Composable
fun DeleteAccountDialog(
    isDeleting: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val confirmWord = AppText.get("profile.deleteAccount.dialog.confirmWord", "DELETE")
    var typed by remember { mutableStateOf("") }
    val confirmed = typed.trim().equals(confirmWord, ignoreCase = true)
    val dismiss = { if (!isDeleting) onDismiss() }

    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isDeleting,
            dismissOnClickOutside = !isDeleting
        )
    ) {
        // Same look as the Logout dialog: plain surface (no tonal tint), shadow and a soft red border
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .imePadding()
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .border(1.dp, ColorExpense.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ColorExpense.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.DeleteForever,
                            contentDescription = null,
                            tint = ColorExpense,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = AppText.get("profile.deleteAccount.dialog.title", "Delete your account?"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 8.dp)
                    )
                    IconButton(onClick = dismiss, enabled = !isDeleting) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = AppText.get("profile.deleteAccount.dialog.closeContentDescription", "Close"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = AppText.get(
                        "profile.deleteAccount.dialog.message",
                        "This permanently deletes your Expent account and everything in it, on this phone and on our servers. This can't be undone."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ColorExpense.copy(alpha = 0.06f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = AppText.get("profile.deleteAccount.dialog.itemsTitle", "What gets deleted"),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = ColorExpense
                        )
                        AppText.list(
                            "profile.deleteAccount.dialog.items",
                            listOf(
                                "All transactions and history",
                                "Accounts, cards and categories",
                                "EMIs, subscriptions, budgets and reminders",
                                "Your profile, settings and feedback"
                            )
                        ).forEach { item ->
                            Text(
                                text = "• $item",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    enabled = !isDeleting,
                    singleLine = true,
                    label = {
                        Text(AppText.get("profile.deleteAccount.dialog.confirmLabel", "Type {word} to confirm", "word" to confirmWord))
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorExpense,
                        focusedLabelColor = ColorExpense,
                        cursorColor = ColorExpense
                    )
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = ColorExpense,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = error, style = MaterialTheme.typography.bodyMedium, color = ColorExpense)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onConfirm,
                    enabled = confirmed && !isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = ColorExpense),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            AppText.get("profile.deleteAccount.dialog.deletingButton", "Deleting…"),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            AppText.get("profile.deleteAccount.dialog.confirmButton", "Delete account permanently"),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = dismiss,
                    enabled = !isDeleting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        AppText.get("profile.deleteAccount.dialog.cancelButton", "Keep my account"),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
