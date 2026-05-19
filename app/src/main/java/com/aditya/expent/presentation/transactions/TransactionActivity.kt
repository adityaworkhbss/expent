package com.aditya.expent.presentation.transactions

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya.expent.domain.model.Transaction
import com.aditya.expent.domain.model.TransactionType
import com.aditya.expent.presentation.theme.ExpentTheme
import com.aditya.expent.presentation.theme.EmeraldPrimary
import com.aditya.expent.presentation.theme.ColorIncome
import com.aditya.expent.presentation.theme.ColorExpense
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class TransactionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ExpentTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TransactionScreen(onBack = { finish() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(onBack: () -> Unit) {
    val focusManager = LocalFocusManager.current
    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedTypeFilter by remember { mutableStateOf("All") }

    // Rich mock transaction history list
    val mockTransactions = remember {
        mutableStateListOf(
            Transaction("1", "Salary Credited", 5000.0, "15/05/2026", "Job", TransactionType.INCOME),
            Transaction("2", "Weekly Grocery", -120.0, "14/05/2026", "Food", TransactionType.EXPENSE),
            Transaction("3", "Netflix Subscription", -15.0, "14/05/2026", "Subscription", TransactionType.EXPENSE),
            Transaction("4", "Freelance Coding", 800.0, "10/05/2026", "Work", TransactionType.INCOME),
            Transaction("5", "Dinner at Bistro", -45.50, "10/05/2026", "Food", TransactionType.EXPENSE),
            Transaction("6", "Gym Membership", -50.00, "08/05/2026", "Subscription", TransactionType.EXPENSE),
            Transaction("7", "Train Ticket", -32.00, "05/05/2026", "Travel", TransactionType.EXPENSE),
            Transaction("8", "Consulting Fee", 1200.0, "03/05/2026", "Work", TransactionType.INCOME),
            Transaction("9", "Fresh Groceries", -18.75, "03/05/2026", "Food", TransactionType.EXPENSE),
            Transaction("10", "Sneakers Purchase", -110.0, "01/05/2026", "Shopping", TransactionType.EXPENSE),
            Transaction("11", "Movie Ticket", -15.0, "01/05/2026", "Leisure", TransactionType.EXPENSE),
            Transaction("12", "Electricity Bill", -85.00, "28/04/2026", "Others", TransactionType.EXPENSE)
        )
    }

    // Dynamic search and filter computations
    val filteredTransactions = remember(searchText, selectedCategory, selectedTypeFilter, mockTransactions.size) {
        mockTransactions.filter { transaction ->
            val matchesSearch = transaction.title.contains(searchText, ignoreCase = true) ||
                    transaction.category.contains(searchText, ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || transaction.category.equals(selectedCategory, ignoreCase = true)
            val matchesType = selectedTypeFilter == "All" ||
                    (selectedTypeFilter == "Income" && transaction.type == TransactionType.INCOME) ||
                    (selectedTypeFilter == "Expense" && transaction.type == TransactionType.EXPENSE)
            matchesSearch && matchesCategory && matchesType
        }
    }

    val groupedTransactions = filteredTransactions.groupBy { formatDisplayDate(it.date) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Top Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Transaction History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Search Bar with rounded glass style
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text("Search transactions...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { searchText = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Custom Sliding Type Segment Switcher (All / Income / Expense)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val options = listOf("All", "Income", "Expense")
            options.forEach { opt ->
                val isSelected = selectedTypeFilter == opt
                val bg by animateColorAsState(
                    targetValue = if (isSelected) {
                        when (opt) {
                            "Income" -> ColorIncome
                            "Expense" -> ColorExpense
                            else -> MaterialTheme.colorScheme.primary
                        }
                    } else Color.Transparent,
                    label = "TabBg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "TabText"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(19.dp))
                        .background(bg)
                        .clickable { selectedTypeFilter = opt },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = opt,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            val categories = listOf(
                Pair("All", Icons.Default.FilterList),
                Pair("Food", Icons.Default.Restaurant),
                Pair("Subscription", Icons.Default.Subscriptions),
                Pair("Work", Icons.Default.Work),
                Pair("Job", Icons.Default.BusinessCenter),
                Pair("Shopping", Icons.Default.ShoppingCart),
                Pair("Travel", Icons.Default.Flight),
                Pair("Leisure", Icons.Default.LocalPlay),
                Pair("Others", Icons.Default.Category)
            )
            items(categories) { (catName, icon) ->
                val isSelected = selectedCategory == catName
                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    label = "HistChipBg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "HistChipText"
                )
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.04f else 1f,
                    label = "HistChipScale"
                )

                Surface(
                    modifier = Modifier
                        .scale(scale)
                        .clickable { selectedCategory = catName },
                    shape = RoundedCornerShape(16.dp),
                    color = containerColor,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = catName,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = catName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = contentColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // History day-grouped list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .animateContentSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (groupedTransactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterAlt,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No matching transactions!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try adjusting your filters or search keywords.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                groupedTransactions.forEach { (friendlyDate, transactionsForDay) ->
                    item(key = "hist_header_$friendlyDate") {
                        val netTotal = transactionsForDay.sumOf { it.amount }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 18.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = friendlyDate,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val isPositive = netTotal >= 0
                            val totalColor = if (isPositive) ColorIncome else ColorExpense
                            val sign = if (isPositive) "+" else "-"

                            Text(
                                text = "$sign $ ${String.format("%.2f", Math.abs(netTotal))}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = totalColor,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(totalColor.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    items(transactionsForDay, key = { it.id }) { transaction ->
                        HistoryTransactionItem(
                            transaction = transaction,
                            onDelete = { mockTransactions.remove(transaction) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryTransactionItem(
    transaction: Transaction,
    onDelete: () -> Unit
) {
    var showDeleteIcon by remember { mutableStateOf(false) }

    val borderBrush = Brush.horizontalGradient(
        colors = if (transaction.type == TransactionType.INCOME) {
            listOf(ColorIncome.copy(alpha = 0.4f), Color.Transparent)
        } else {
            listOf(ColorExpense.copy(alpha = 0.4f), Color.Transparent)
        }
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { showDeleteIcon = !showDeleteIcon }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val categoryColor = when (transaction.category.lowercase()) {
            "food" -> Color(0xFFFF7043)
            "subscription" -> Color(0xFFAB47BC)
            "work" -> Color(0xFF42A5F5)
            "job" -> Color(0xFF009688)
            "shopping" -> Color(0xFFEC407A)
            "travel" -> Color(0xFF26A69A)
            "entertainment", "leisure" -> Color(0xFFFFCA28)
            else -> MaterialTheme.colorScheme.primary
        }

        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(categoryColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            val icon = when (transaction.category.lowercase()) {
                "food" -> Icons.Default.Restaurant
                "subscription" -> Icons.Default.Subscriptions
                "work" -> Icons.Default.Work
                "job" -> Icons.Default.BusinessCenter
                "shopping" -> Icons.Default.ShoppingCart
                "travel" -> Icons.Default.Flight
                "entertainment", "leisure" -> Icons.Default.LocalPlay
                else -> Icons.Default.Category
            }
            Icon(
                imageVector = icon,
                contentDescription = transaction.category,
                tint = categoryColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.category,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            val paymentMode = transaction.paymentMethod ?: "Cash"
            Text(
                text = "via $paymentMode",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.width(10.dp))
        
        Box(
            contentAlignment = Alignment.CenterEnd
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = showDeleteIcon,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = !showDeleteIcon,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = if (transaction.type == TransactionType.INCOME) "+ $ ${String.format("%.2f", transaction.amount)}" else "- $ ${String.format("%.2f", Math.abs(transaction.amount))}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.type == TransactionType.INCOME) ColorIncome else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatDisplayDate(dateStr: String): String {
    if (dateStr.equals("Today", ignoreCase = true) || dateStr.equals("Yesterday", ignoreCase = true)) {
        return dateStr
    }

    try {
        val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        isoParser.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = isoParser.parse(dateStr)
        if (date != null) {
            val localFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formattedInput = localFormatter.format(date)
            
            val todayStr = localFormatter.format(Date())
            val yesterdayStr = localFormatter.format(Date(Date().time - 24 * 60 * 60 * 1000))
            
            if (formattedInput == todayStr) return "Today"
            if (formattedInput == yesterdayStr) return "Yesterday"
            
            val displayFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            return displayFormatter.format(date)
        }
    } catch (e: Exception) {
        // ignore
    }

    try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = parser.parse(dateStr)
        if (date != null) {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val yesterdayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(Date().time - 24 * 60 * 60 * 1000))
            val formattedInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
            if (formattedInput == todayStr) return "Today"
            if (formattedInput == yesterdayStr) return "Yesterday"

            val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            return formatter.format(date)
        }
    } catch (e: Exception) {
        // ignore
    }

    try {
        val parser = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = parser.parse(dateStr)
        if (date != null) {
            val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val yesterdayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(Date().time - 24 * 60 * 60 * 1000))
            if (dateStr == todayStr) return "Today"
            if (dateStr == yesterdayStr) return "Yesterday"
            
            val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            return formatter.format(date)
        }
    } catch (e: Exception) {
        // ignore
    }

    return dateStr
}

@Preview(showBackground = true)
@Composable
fun TransactionScreenPreview() {
    ExpentTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            TransactionScreen(onBack = {})
        }
    }
}
