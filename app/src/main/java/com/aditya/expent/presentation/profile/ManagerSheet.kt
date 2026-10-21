package com.aditya.expent.presentation.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya.expent.presentation.theme.ColorExpense

// A type an item can belong to, e.g. Pay Now / Pay Later or Expense / Income
data class ManagerType(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val color: Color
)

data class ManagerItem(
    val id: String,
    val name: String,
    val type: String
)

// Shared bottom sheet for the Payment Modes and Categories managers
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerSheet(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    itemNoun: String,
    itemNounPlural: String,
    placeholder: String,
    types: List<ManagerType>,
    items: List<ManagerItem>,
    onAdd: (name: String, type: String) -> Unit,
    onDelete: (id: String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var newType by remember { mutableStateOf(types.first().key) }
    var filter by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<ManagerItem?>(null) }
    val focusManager = LocalFocusManager.current

    val trimmed = newName.trim()
    val isDuplicate = trimmed.isNotEmpty() &&
        items.any { it.name.equals(trimmed, ignoreCase = true) && it.type == newType }
    val canAdd = trimmed.isNotEmpty() && !isDuplicate
    val selectedType = types.first { it.key == newType }
    fun typeOf(key: String) = types.firstOrNull { it.key == key } ?: types.first()

    val submit = {
        if (canAdd) {
            onAdd(trimmed, newType)
            newName = ""
            focusManager.clearFocus()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Add new item
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ADD NEW ${itemNoun.uppercase()}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        types.forEach { type ->
                            TypeToggle(
                                type = type,
                                selected = newType == type.key,
                                onClick = { newType = type.key },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.Top) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            placeholder = { Text(placeholder) },
                            leadingIcon = {
                                Icon(selectedType.icon, contentDescription = null, tint = selectedType.color)
                            },
                            isError = isDuplicate,
                            supportingText = if (isDuplicate) {
                                { Text("\"$trimmed\" already exists as ${selectedType.label}") }
                            } else null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { submit() }),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        FilledIconButton(
                            onClick = { submit() },
                            enabled = canAdd,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = accent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(52.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add $itemNoun")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Filter by type
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filter == null,
                        onClick = { filter = null },
                        label = { Text("All · ${items.size}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.copy(alpha = 0.14f),
                            selectedLabelColor = accent
                        )
                    )
                }
                items(types) { type ->
                    FilterChip(
                        selected = filter == type.key,
                        onClick = { filter = if (filter == type.key) null else type.key },
                        label = { Text("${type.label} · ${items.count { it.type == type.key }}") },
                        leadingIcon = {
                            Icon(type.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = type.color.copy(alpha = 0.14f),
                            selectedLabelColor = type.color,
                            selectedLeadingIconColor = type.color
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val visible = items.filter { filter == null || it.type == filter }
            val groups = types.mapNotNull { type ->
                visible.filter { it.type == type.key }
                    .sortedBy { it.name.lowercase() }
                    .takeIf { it.isNotEmpty() }
                    ?.let { type to it }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (groups.isEmpty()) {
                    item {
                        EmptyState(
                            accent = accent,
                            text = if (items.isEmpty()) "No $itemNounPlural yet" else "Nothing here yet",
                            hint = "Add one above to get started"
                        )
                    }
                }
                groups.forEach { (type, groupItems) ->
                    item(key = "header_${type.key}") {
                        Text(
                            text = "${type.label.uppercase()} · ${groupItems.size}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .animateItem()
                                .padding(top = 8.dp, bottom = 2.dp, start = 4.dp)
                        )
                    }
                    items(groupItems, key = { it.id }) { item ->
                        ManagerRow(
                            item = item,
                            type = typeOf(item.type),
                            onDelete = { pendingDelete = item },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = ColorExpense) },
            title = { Text("Delete $itemNoun?") },
            text = { Text("\"${item.name}\" will be removed from your list.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(item.id)
                    pendingDelete = null
                }) {
                    Text("Delete", color = ColorExpense, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TypeToggle(
    type: ManagerType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container by animateColorAsState(
        if (selected) type.color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        label = "typeToggleContainer"
    )
    val content = if (selected) type.color else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = container,
        border = BorderStroke(
            1.dp,
            if (selected) type.color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(type.icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(type.label, color = content, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ManagerRow(
    item: ManagerItem,
    type: ManagerType,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(type.color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(type.icon, contentDescription = null, tint = type.color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = type.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = type.color
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete ${item.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyState(accent: Color, text: String, hint: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Inbox, contentDescription = null, tint = accent, modifier = Modifier.size(30.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
