package com.aditya.expent.presentation.dashboard

import android.content.Context
import android.util.Log
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya.expent.domain.model.Transaction
import com.aditya.expent.domain.model.TransactionType
import com.aditya.expent.data.remote.dto.CategoryResponseDto
import com.aditya.expent.data.remote.dto.PaymentModeResponseDto
import com.aditya.expent.utils.CategoryUtils.getCategoryIconAndColor
import com.aditya.expent.presentation.auth.AuthActivity
import com.aditya.expent.presentation.transactions.TransactionActivity
import com.aditya.expent.presentation.component.ExpentDatePicker
import com.aditya.expent.presentation.profile.ProfileActivity
import com.aditya.expent.presentation.theme.ExpentTheme
import com.aditya.expent.presentation.theme.EmeraldPrimary
import com.aditya.expent.presentation.theme.ColorIncome
import com.aditya.expent.presentation.theme.ColorExpense
import com.aditya.expent.utils.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue
import javax.inject.Inject
import androidx.compose.ui.graphics.graphicsLayer
import com.aditya.expent.presentation.analysis.AnalysisActivity
import com.aditya.expent.presentation.cashflow.CashflowActivity
import com.aditya.expent.utils.AppUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@AndroidEntryPoint
class DashboardActivity : ComponentActivity() {
    
    @Inject
    lateinit var sessionManager: SessionManager

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (sessionManager.getUser() == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        setContent {
            ExpentTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val state by viewModel.state.collectAsState()
                    DashboardScreen(
                        state = state,
                        onAddTransaction = { title, amount, category, type, date, accountName, accountId ->
                            viewModel.addTransaction(title, amount, category, type, date, accountName, accountId)
                        },
                        onDeleteTransaction = { id ->
                            viewModel.deleteTransaction(id)
                        },
                        onAddAiTransaction = { text ->
                            viewModel.addAiTransaction(text)
                        }
                    )
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        Log.d("DashboardActivity", "onResume: Reloading customizations via viewModel.loadCustomization()")
        // Reload customizations in case they were updated in ProfileActivity
        viewModel.loadCustomization()
        viewModel.loadAccounts()

        if (sessionManager.getUser() == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }
    }
}

data class Reminder(
    val id: String,
    val title: String,
    val description: String,
    val amount: Double,
    val dueDate: String,
    val category: String,
    val type: TransactionType
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardState,
    onAddTransaction: (String, Double, String, TransactionType, String, String, String) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onAddAiTransaction: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    Log.d("DashboardActivity", "DashboardScreen Composing: state.reminder = ${state.reminder}, state.aiTransaction = ${state.aiTransaction}")
    var isExpanded by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var selectedReminderForDialog by remember { mutableStateOf<Reminder?>(null) }
    var showEditReminderSheet by remember { mutableStateOf(false) }
    var selectedReminderForEdit by remember { mutableStateOf<Reminder?>(null) }
    
    val dismissingReminderIds = remember { mutableStateListOf<String>() }
    val reminders = remember {
        mutableStateListOf(
            Reminder(
                id = "1",
                title = "Electricity Bill",
                description = "Electricity bill due in 3 days",
                amount = 45.50,
                dueDate = "Sep 30, 2026",
                category = "Bills",
                type = TransactionType.EXPENSE
            ),
            Reminder(
                id = "2",
                title = "Netflix Subscription",
                description = "Standard plan renewal",
                amount = 15.99,
                dueDate = "Oct 05, 2026",
                category = "Subscription",
                type = TransactionType.EXPENSE
            ),
            Reminder(
                id = "3",
                title = "Salary / Paycheck",
                description = "Monthly direct deposit",
                amount = 2500.00,
                dueDate = "Oct 01, 2026",
                category = "Job",
                type = TransactionType.INCOME
            )
        )
    }
    
    val pagerState = rememberPagerState(pageCount = { reminders.size })
    
    val visibleTransactions = if (isExpanded) state.recentTransactions else state.recentTransactions.take(3)
    val groupedTransactions = visibleTransactions.groupBy { formatDisplayDate(it.date) }

    Scaffold(
        floatingActionButton = {
            GlowFloatingActionButton(
                onClick = { showBottomSheet = true },
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .animateContentSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = state.greetingMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = state.userName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    context.startActivity(Intent(context, ProfileActivity::class.java))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Balance Card
                    BalanceCard(
                        totalBalance = state.totalBalance,
                        income = state.totalIncome,
                        expense = state.totalExpense,
                        context = context
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    if (state.reminder && reminders.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            // Enhanced section header with icon and count badge
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, end = 4.dp, bottom = 12.dp, top = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = "Upcoming Reminders",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldPrimary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = "${reminders.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Animated snap-scrolling pager
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 28.dp),
                                pageSpacing = 16.dp
                            ) { page ->
                                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                                val absPageOffset = pageOffset.absoluteValue
                                val reminder = reminders[page]

                                val isDismissing = reminder.id in dismissingReminderIds
                                val animatedAlpha by animateFloatAsState(
                                    targetValue = if (isDismissing) 0f else 1f,
                                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                    label = "DismissAlpha"
                                )
                                val animatedScale by animateFloatAsState(
                                    targetValue = if (isDismissing) 0.6f else 1f,
                                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                    label = "DismissScale"
                                )
                                val animatedTranslationY by animateFloatAsState(
                                    targetValue = if (isDismissing) (-100f) else 0f,
                                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                    label = "DismissTranslationY"
                                )

                                Box(
                                    modifier = Modifier
                                        .graphicsLayer {
                                            cameraDistance = 12f * density
                                            rotationY = pageOffset * -22f
                                            rotationZ = pageOffset * -3f
                                            
                                            val scaleVal = (0.88f + (1f - absPageOffset.coerceIn(0f, 1f)) * 0.12f) * animatedScale
                                            scaleX = scaleVal
                                            scaleY = scaleVal
                                            
                                            alpha = (0.5f + (1f - absPageOffset.coerceIn(0f, 1f)) * 0.5f) * animatedAlpha
                                            translationY = animatedTranslationY
                                            translationX = pageOffset * -12.dp.toPx()
                                        }
                                ) {
                                    ReminderCard(
                                        reminder = reminder,
                                        onTick = {
                                            selectedReminderForDialog = reminder
                                            showConfirmDialog = true
                                        },
                                        onEdit = {
                                            selectedReminderForEdit = reminder
                                            showEditReminderSheet = true
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            ReminderDotsIndicator(
                                count = reminders.size.coerceAtMost(5),
                                activeIndex = pagerState.currentPage,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    // Transactions Section Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Transactions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        IconButton(
                            onClick = {
                                context.startActivity(Intent(context, TransactionActivity::class.java))
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = "Transaction History", modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (state.recentTransactions.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No transactions yet!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the 'Add New' button to add your first record.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    groupedTransactions.forEach { (friendlyDate, transactionsForDay) ->
                        item(key = "header_$friendlyDate") {
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
                            TransactionItem(
                                transaction = transaction,
                                onDelete = { onDeleteTransaction(transaction.id) }
                            )
                        }
                    }

                    if (state.recentTransactions.size > 3) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            SeeMoreButton(
                                isExpanded = isExpanded,
                                onClick = { isExpanded = !isExpanded }
                            )
                        }
                    }
                }
            }

            if (showBottomSheet) {
                AddTransactionBottomSheet(
                    categories = state.categories,
                    accounts = state.accounts,
                    aiTransactionEnabled = state.aiTransaction,
                    onDismiss = { showBottomSheet = false },
                    onSave = { title, amount, category, type, date, accountName, accountId ->
                        onAddTransaction(title, amount, category, type, date, accountName, accountId)
                        showBottomSheet = false
                    },
                    onAiSave = { aiText ->
                        onAddAiTransaction(aiText)
                        showBottomSheet = false
                    }
                )
            }

            // REMINDER CONFIRMATION DIALOG
            if (showConfirmDialog && selectedReminderForDialog != null) {
                val reminder = selectedReminderForDialog!!
                AlertDialog(
                    onDismissRequest = { 
                        showConfirmDialog = false
                        selectedReminderForDialog = null
                    },
                    shape = RoundedCornerShape(28.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    title = {
                        Text(
                            text = "Add to Transactions?",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Do you want to add this reminder into your transactions?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (reminder.type == TransactionType.INCOME) ColorIncome.copy(alpha = 0.15f)
                                                else ColorExpense.copy(alpha = 0.15f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val icon = when (reminder.category.lowercase()) {
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
                                            contentDescription = reminder.category,
                                            tint = if (reminder.type == TransactionType.INCOME) ColorIncome else ColorExpense,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = reminder.title,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = reminder.category,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Text(
                                        text = if (reminder.type == TransactionType.INCOME) "+ $ ${String.format("%.2f", reminder.amount)}" else "- $ ${String.format("%.2f", reminder.amount)}",
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (reminder.type == TransactionType.INCOME) ColorIncome else ColorExpense
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val firstAccount = state.accounts.firstOrNull()
                                val accountName = firstAccount?.name ?: "Cash"
                                val accountId = firstAccount?.id ?: ""
                                
                                onAddTransaction(
                                    reminder.title,
                                    reminder.amount,
                                    reminder.category,
                                    reminder.type,
                                    getTodayDateString(),
                                    accountName,
                                    accountId
                                )
                                
                                coroutineScope.launch {
                                    dismissingReminderIds.add(reminder.id)
                                    delay(350)
                                    reminders.remove(reminder)
                                    dismissingReminderIds.remove(reminder.id)
                                }
                                
                                showConfirmDialog = false
                                selectedReminderForDialog = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (reminder.type == TransactionType.INCOME) ColorIncome else ColorExpense
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Yes, Add", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showConfirmDialog = false
                                selectedReminderForDialog = null
                            }
                        ) {
                            Text(
                                "Cancel",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }

            // EDIT REMINDER BOTTOM SHEET
            if (showEditReminderSheet && selectedReminderForEdit != null) {
                EditReminderBottomSheet(
                    reminder = selectedReminderForEdit!!,
                    categories = state.categories,
                    onDismiss = {
                        showEditReminderSheet = false
                        selectedReminderForEdit = null
                    },
                    onSave = { updatedReminder ->
                        val index = reminders.indexOfFirst { it.id == updatedReminder.id }
                        if (index != -1) {
                            reminders[index] = updatedReminder
                        }
                        showEditReminderSheet = false
                        selectedReminderForEdit = null
                    },
                    onDelete = {
                        val reminderToDelete = selectedReminderForEdit
                        if (reminderToDelete != null) {
                            coroutineScope.launch {
                                dismissingReminderIds.add(reminderToDelete.id)
                                delay(350)
                                reminders.remove(reminderToDelete)
                                dismissingReminderIds.remove(reminderToDelete.id)
                            }
                        }
                        showEditReminderSheet = false
                        selectedReminderForEdit = null
                    }
                )
            }
        }
    }

    if (state.isLoading) {
        AppUtils().ShowProgressAnimation()
    }
}

@Composable
fun ReminderCard(
    reminder: Reminder,
    onTick: () -> Unit,
    onEdit: () -> Unit
) {
    val categoryColor = when (reminder.category.lowercase()) {
        "food" -> Color(0xFFFF7043)
        "subscription" -> Color(0xFFAB47BC)
        "work" -> Color(0xFF42A5F5)
        "job" -> Color(0xFF009688)
        "shopping" -> Color(0xFFEC407A)
        "travel" -> Color(0xFF26A69A)
        "entertainment", "leisure" -> Color(0xFFFFCA28)
        else -> MaterialTheme.colorScheme.primary
    }

    val accentColor = if (reminder.type == TransactionType.INCOME) ColorIncome else ColorExpense
    val (daysLeftText, badgeColor) = getDaysLeftInfo(reminder.dueDate)

    val icon = when (reminder.category.lowercase()) {
        "food" -> Icons.Default.Restaurant
        "subscription" -> Icons.Default.Subscriptions
        "work" -> Icons.Default.Work
        "job" -> Icons.Default.BusinessCenter
        "shopping" -> Icons.Default.ShoppingCart
        "travel" -> Icons.Default.Flight
        "entertainment", "leisure" -> Icons.Default.LocalPlay
        else -> Icons.Default.Alarm
    }

    Card(
        modifier = Modifier
            .width(300.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = accentColor.copy(alpha = 0.2f),
                spotColor = accentColor.copy(alpha = 0.15f)
            )
            .clickable { onEdit() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Icon + Title/Description + Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(categoryColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = reminder.category,
                        tint = categoryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title + Description (weighted to fill)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = reminder.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Amount
                Text(
                    text = if (reminder.type == TransactionType.INCOME)
                        "+$${String.format("%.2f", reminder.amount)}"
                    else
                        "-$${String.format("%.2f", reminder.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )

            // Bottom Row: Due Badge + Done Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Due date badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                    )
                    Text(
                        text = daysLeftText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = badgeColor
                    )
                }

                // Done button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.1f))
                        .clickable { onTick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Mark Done",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReminderDotsIndicator(
    count: Int,
    activeIndex: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )

        val coercedActiveIndex = activeIndex.coerceIn(0, (count - 1).coerceAtLeast(0))

        for (i in 0 until count) {
            key(i) {
                ReminderDot(
                    isSelected = i == coercedActiveIndex,
                    isReached = i < coercedActiveIndex,
                    pulseScale = pulseScale
                )
            }
        }
    }
}

@Composable
fun ReminderDot(
    isSelected: Boolean,
    isReached: Boolean,
    pulseScale: Float
) {
    val dotWidth by animateDpAsState(
        targetValue = if (isSelected) 24.dp else 8.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "width"
    )

    val dotColor by animateColorAsState(
        targetValue = when {
            isSelected -> EmeraldPrimary
            isReached -> EmeraldPrimary.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
        },
        animationSpec = tween(durationMillis = 300),
        label = "color"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "scale"
    )

    val appliedPulse = if (isSelected) pulseScale else 1f

    Box(
        modifier = Modifier
            .scale(scale * appliedPulse)
            .size(width = dotWidth, height = 8.dp)
            .background(
                color = dotColor,
                shape = CircleShape
            )
            .shadow(
                elevation = if (isSelected) 3.dp else 0.dp,
                shape = CircleShape
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReminderBottomSheet(
    reminder: Reminder,
    categories: List<CategoryResponseDto>,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var title by remember { mutableStateOf(reminder.title) }
    var description by remember { mutableStateOf(reminder.description) }
    var amountText by remember { mutableStateOf(reminder.amount.toString()) }
    var dueDate by remember { mutableStateOf(reminder.dueDate) }
    var selectedCategory by remember { mutableStateOf(reminder.category) }
    var selectedType by remember { mutableStateOf(reminder.type) }

    val activeColor = if (selectedType == TransactionType.INCOME) ColorIncome else ColorExpense

    val filteredCategories = remember(categories, selectedType) {
        categories.filter { cat ->
            if (selectedType == TransactionType.INCOME) {
                cat.type.equals("income", ignoreCase = true)
            } else {
                cat.type.equals("expense", ignoreCase = true)
            }
        }.map { it.name }
    }

    LaunchedEffect(filteredCategories) {
        if (selectedCategory !in filteredCategories && filteredCategories.isNotEmpty()) {
            selectedCategory = filteredCategories.first()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Reminder",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { type ->
                    val isSelected = selectedType == type
                    val typeColor = if (type == TransactionType.INCOME) ColorIncome else ColorExpense
                    Surface(
                        onClick = { selectedType = type },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) typeColor else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            ),
                        color = if (isSelected) typeColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (type == TransactionType.INCOME) "Income" else "Expense",
                                color = if (isSelected) typeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                placeholder = { Text("e.g. Rent, Electricity...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = activeColor,
                    focusedLabelColor = activeColor
                )
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("Short reminder details") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = activeColor,
                    focusedLabelColor = activeColor
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($)") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = activeColor,
                        focusedLabelColor = activeColor
                    )
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date") },
                    placeholder = { Text("e.g. Sep 30, 2026") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = activeColor,
                        focusedLabelColor = activeColor
                    )
                )
            }

            if (filteredCategories.isNotEmpty()) {
                var expandedCategoryDropdown by remember { mutableStateOf(false) }
                val arrowRotation by animateFloatAsState(
                    targetValue = if (expandedCategoryDropdown) 180f else 0f,
                    label = "AiArrowRotation"
                )

                ExposedDropdownMenuBox(
                    expanded = expandedCategoryDropdown,
                    onExpandedChange = { expandedCategoryDropdown = it }
                ) {
                    OutlinedTextField(
                        value = if (selectedCategory.isBlank()) "Select Category" else selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        leadingIcon = {
                            if (selectedCategory.isNotBlank()) {
                                val iconAndColor = getCategoryIconAndColor(selectedCategory)
                                Icon(
                                    imageVector = iconAndColor.first,
                                    contentDescription = null,
                                    tint = iconAndColor.second,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Category",
                                tint = activeColor,
                                modifier = Modifier.graphicsLayer { rotationZ = arrowRotation }
                            )
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activeColor,
                            focusedLabelColor = activeColor
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategoryDropdown,
                        onDismissRequest = { expandedCategoryDropdown = false },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    ) {
                        filteredCategories.forEach { catName ->
                            val isSelected = selectedCategory == catName
                            val iconAndColor = getCategoryIconAndColor(catName)
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(iconAndColor.second.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = iconAndColor.first,
                                                    contentDescription = catName,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = iconAndColor.second
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = catName,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = activeColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedCategory = catName
                                    expandedCategoryDropdown = false
                                },
                                modifier = Modifier
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) activeColor.copy(alpha = 0.1f) else Color.Transparent
                                    )
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "No Categories Found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val amountVal = amountText.toDoubleOrNull() ?: reminder.amount
                        onSave(
                            reminder.copy(
                                title = title.trim(),
                                description = description.trim(),
                                amount = amountVal,
                                dueDate = dueDate.trim(),
                                category = selectedCategory,
                                type = selectedType
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                    modifier = Modifier
                        .weight(2f)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save Changes",
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BalanceCard(
    totalBalance: Double,
    income: Double,
    expense: Double,
    context: Context
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .shadow(16.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            EmeraldPrimary,
                            Color(0xFF004D40)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f),
                    radius = 350f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.1f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.03f),
                    radius = 200f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.9f)
                )
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Balance",
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable(
                                    onClick = {
//                                        context.startActivity(Intent(context, AnalysisActivity::class.java))
                                        Toast.makeText(context, "Analysis screen coming soon!", Toast.LENGTH_SHORT).show()
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }


                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable(
                                    onClick = {
                                        context.startActivity(Intent(context, CashflowActivity::class.java))
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                
                val displayBalance = if (totalBalance >= 0) {
                    "$ ${String.format("%.2f", totalBalance)}"
                } else {
                    "-$ ${String.format("%.2f", Math.abs(totalBalance))}"
                }
                
                Text(
                    text = displayBalance,
                    color = Color.White,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp
                )
                
                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        icon = Icons.Default.ArrowDownward,
                        label = "Income",
                        amount = income,
                        color = ColorIncome
                    )
                    StatItem(
                        icon = Icons.Default.ArrowUpward,
                        label = "Expense",
                        amount = expense,
                        color = ColorExpense
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(
    icon: ImageVector,
    label: String,
    amount: Double,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "$ ${String.format("%.2f", amount)}",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TransactionItem(
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
                        contentDescription = "Delete Transaction",
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

@Composable
fun SeeMoreButton(
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "ArrowRotation"
    )

    // Premium breathing scale animation to make the transparent button feel alive
    val infiniteTransition = rememberInfiniteTransition(label = "SeeMoreBreathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SeeMoreScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.scale(scale),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant text vertical slide-in and fade-in/out transition on toggling
            AnimatedContent(
                targetState = isExpanded,
                transitionSpec = {
                    if (targetState) {
                        (slideInVertically { height -> height } + fadeIn() togetherWith
                                slideOutVertically { height -> -height } + fadeOut())
                    } else {
                        (slideInVertically { height -> -height } + fadeIn() togetherWith
                                slideOutVertically { height -> height } + fadeOut())
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
                label = "SeeMoreTextAnimation"
            ) { expanded ->
                Text(
                    text = if (expanded) "See Less" else "See More",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(rotationAngle)
            )
        }
    }
}


@Composable
fun GlowFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Smooth press scaling with bouncy spring
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "FabPressScale"
    )

    // Infinite gentle pulse animation for the icon
    val infiniteTransition = rememberInfiniteTransition(label = "FabPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FabIconPulse"
    )

    // Rich gradient: glowing cyan to neon green
    val gradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF009688), // Brand Teal / Emerald Primary
            Color(0xFF075730)  // Glowing Neon Green
        )
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = EmeraldPrimary,
                spotColor = Color(0xFF004D40)
            )
            .background(
                brush = gradient,
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.5f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White),
                onClick = onClick
            )
            .padding(horizontal = 22.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AddTask,
                contentDescription = "Add Transaction",
                tint = Color.White,
                modifier = Modifier
                    .size(22.dp)
                    .scale(pulseScale)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionBottomSheet(
    categories: List<CategoryResponseDto>,
    accounts: List<PaymentModeResponseDto>,
    aiTransactionEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, TransactionType, String, String, String) -> Unit,
    onAiSave: (String) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    Log.d("DashboardActivity", "AddTransactionBottomSheet: aiTransactionEnabled = $aiTransactionEnabled")
    var amountText by remember { mutableStateOf("") }

    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }

    var selectedCategory by remember(categories, selectedType) {
        val firstFiltered = categories.firstOrNull { cat ->
            if (selectedType == TransactionType.INCOME) {
                cat.type.equals("income", ignoreCase = true)
            } else {
                cat.type.equals("expense", ignoreCase = true)
            }
        }
        mutableStateOf(firstFiltered?.name ?: "Others")
    }

    // Account filtering: INCOME shows PAY_NOW only, EXPENSE shows all accounts
    val filteredAccounts = remember(accounts, selectedType) {
        if (selectedType == TransactionType.INCOME) {
            accounts.filter { it.type.equals("PAY_NOW", ignoreCase = true) }
        } else {
            accounts
        }
    }

    var selectedAccountName by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf("") }

    LaunchedEffect(filteredAccounts) {
        val currentExists = filteredAccounts.any { it.name == selectedAccountName }
        if (!currentExists) {
            filteredAccounts.firstOrNull()?.let {
                selectedAccountName = it.name
                selectedAccountId = it.id
            }
        }
    }

    var selectedDateText by remember { mutableStateOf(getTodayDateString()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val activeColor = if (selectedType == TransactionType.INCOME) ColorIncome else ColorExpense

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            val finalInputMode = if (aiTransactionEnabled) "Smart" else "Manual"
            Log.d("DashboardActivity", "AddTransactionBottomSheet: Rendering with finalInputMode = $finalInputMode, aiTransactionEnabled = $aiTransactionEnabled")

            AnimatedContent(
                targetState = finalInputMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "InputModeTransition"
            ) { mode ->
                if (mode == "Smart") {
                    var aiText by remember { mutableStateOf("") }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = aiText,
                            onValueChange = { aiText = it },
                            placeholder = { Text("e.g. Spent $25 on pizza yesterday using cash") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6200EA),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                focusedContainerColor = Color(0xFF6200EA).copy(alpha = 0.03f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                            maxLines = 6
                        )

                        Text(
                            text = "AI will automatically extract amount, category, type, and date from your sentence.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val btnScale by animateFloatAsState(targetValue = if (isPressed) 0.96f else 1f, label = "AiBtnScale")

                        Button(
                            onClick = { onAiSave(aiText) },
                            enabled = aiText.isNotBlank(),
                            interactionSource = interactionSource,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .scale(btnScale)
                                .shadow(
                                    elevation = if (aiText.isNotBlank()) 12.dp else 0.dp,
                                    shape = RoundedCornerShape(28.dp),
                                    ambientColor = Color(0xFF6200EA),
                                    spotColor = Color(0xFF6200EA)
                                ),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6200EA),
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            )
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription=null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Parse & Save Transaction", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(22.dp)
                    ) {
                        // Interactive Segment Switcher (Income vs Expense)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                val isExpense = selectedType == TransactionType.EXPENSE
                
                // Income Option
                val incomeBgColor by animateColorAsState(
                    targetValue = if (!isExpense) ColorIncome else Color.Transparent,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "IncomeBg"
                )
                val incomeTextColor by animateColorAsState(
                    targetValue = if (!isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "IncomeText"
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(22.dp))
                        .background(incomeBgColor)
                        .clickable { selectedType = TransactionType.INCOME },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Income",
                        color = incomeTextColor,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Expense Option
                val expenseBgColor by animateColorAsState(
                    targetValue = if (isExpense) ColorExpense else Color.Transparent,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "ExpenseBg"
                )
                val expenseTextColor by animateColorAsState(
                    targetValue = if (isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "ExpenseText"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(22.dp))
                        .background(expenseBgColor)
                        .clickable { selectedType = TransactionType.EXPENSE },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Expense",
                        color = expenseTextColor,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Big interactive amount indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(activeColor.copy(alpha = 0.04f))
                    .border(1.dp, activeColor.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .padding(vertical = 20.dp, horizontal = 16.dp)
            ) {
                Text(
                    text = "Amount",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$",
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 44.sp),
                        fontWeight = FontWeight.Black,
                        color = activeColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = amountText,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                amountText = it
                            }
                        },
                        textStyle = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 44.sp,
                            textAlign = TextAlign.Start,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        cursorBrush = SolidColor(activeColor),
                        modifier = Modifier
                            .width(IntrinsicSize.Min)
                            .defaultMinSize(minWidth = 100.dp)
                    )
                }
            }

            // Category Selection
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                val filteredCategory = categories.filter { cat ->
                    if (selectedType == TransactionType.INCOME) {
                        cat.type.equals("income", ignoreCase = true)
                    } else {
                        cat.type.equals("expense", ignoreCase = true)
                    }
                }.map { cat ->
                    val iconAndColor = getCategoryIconAndColor(cat.name)
                    Triple(cat.name, iconAndColor.first, iconAndColor.second)
                }

                if (filteredCategory.isEmpty()) {
                    Text(
                        text = "No Categories Found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(filteredCategory) { (catName, icon, catColor) ->
                            val isSelected = selectedCategory == catName
                            val containerColor by animateColorAsState(
                                targetValue = if (isSelected) catColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                label = "ChipBg"
                            )
                            val contentColor by animateColorAsState(
                                targetValue = if (isSelected) catColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "ChipText"
                            )
                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1.04f else 1f,
                                label = "ChipScale"
                            )
                            
                            Surface(
                                modifier = Modifier
                                    .scale(scale)
                                    .clickable { selectedCategory = catName },
                                shape = RoundedCornerShape(16.dp),
                                color = containerColor,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) catColor.copy(alpha = 0.4f) else Color.Transparent
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = catName,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isSelected) catColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
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
                }
            }

            // Select Account Selection (Filtered based on INCOME PAY_NOW / EXPENSE All)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select Account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (filteredAccounts.isEmpty()) {
                    Text(
                        text = "No applicable accounts found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(filteredAccounts) { acc ->
                            val isSelected = selectedAccountName == acc.name
                            
                            val accColor = when (acc.type.uppercase()) {
                                "BANK" -> Color(0xFF2196F3)
                                "CREDIT" -> Color(0xFFE91E63)
                                "CASH" -> Color(0xFF4CAF50)
                                else -> MaterialTheme.colorScheme.secondary
                            }
                            
                            val containerColor by animateColorAsState(
                                targetValue = if (isSelected) accColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                label = "AccChipBg"
                            )
                            val contentColor by animateColorAsState(
                                targetValue = if (isSelected) accColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "AccChipText"
                            )
                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1.04f else 1f,
                                label = "AccChipScale"
                            )
                            
                            Surface(
                                modifier = Modifier
                                    .scale(scale)
                                    .clickable { 
                                        selectedAccountName = acc.name
                                        selectedAccountId = acc.id
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = containerColor,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) accColor.copy(alpha = 0.4f) else Color.Transparent
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    val icon = when (acc.type.uppercase()) {
                                        "BANK" -> Icons.Default.AccountBalance
                                        "CREDIT" -> Icons.Default.CreditCard
                                        else -> Icons.Default.AccountBalanceWallet
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = acc.name,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isSelected) accColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = acc.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = contentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Beautiful Interactive Date Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Date",
                        tint = activeColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Transaction Date",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = selectedDateText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Save Trigger Button
            val isEnabled = amountText.toDoubleOrNull()?.let { it > 0 } == true
            
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val buttonScale by animateFloatAsState(
                targetValue = if (isPressed) 0.96f else 1f,
                label = "ButtonPress"
            )

                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull()
                            if (amt != null && amt > 0) {
                                onSave(selectedCategory, amt, selectedCategory, selectedType, selectedDateText, selectedAccountName, selectedAccountId)
                            }
                        },
                        enabled = isEnabled,
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .scale(buttonScale)
                            .shadow(
                                elevation = if (isEnabled) 12.dp else 0.dp,
                                shape = RoundedCornerShape(28.dp),
                                ambientColor = activeColor,
                                spotColor = activeColor
                            ),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeColor,
                            contentColor = Color.White,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = "Save Transaction",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

    ExpentDatePicker(
        showDialog = showDatePicker,
        onDismiss = { showDatePicker = false },
        onDateSelected = {
            selectedDateText = it
            showDatePicker = false
        }
    )
}


fun getDaysLeftInfo(dueDateStr: String): Pair<String, Color> {
    try {
        val parser = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val dueDate = parser.parse(dueDateStr) ?: return Pair(dueDateStr, EmeraldPrimary)
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val diffTime = dueDate.time - today.time
        val diffDays = (diffTime / (1000 * 60 * 60 * 24)).toInt()
        
        return when {
            diffDays < 0 -> Pair("Overdue", Color(0xFFEF5350))
            diffDays == 0 -> Pair("Due Today", Color(0xFFFF7043))
            diffDays == 1 -> Pair("Due Tomorrow", Color(0xFFFFB74D))
            diffDays <= 3 -> Pair("Due in $diffDays days", Color(0xFFFFCA28))
            else -> Pair("In $diffDays days", EmeraldPrimary)
        }
    } catch (e: Exception) {
        return Pair(dueDateStr, EmeraldPrimary)
    }
}

fun getTodayDateString(): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(Date())
}

fun formatDisplayDate(dateStr: String): String {
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
fun DashboardActivityPreview() {
    ExpentTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            DashboardScreen(
                state = DashboardState(
                    userName = "Aditya",
                    totalBalance = 1250.75,
                    totalIncome = 2000.00,
                    totalExpense = 749.25,
                    recentTransactions = listOf(
                        Transaction("1", "Salary Credited", 2000.00, "15/05/2026", "Job", TransactionType.INCOME),
                        Transaction("2", "Weekly Groceries", -150.50, "14/05/2026", "Food", TransactionType.EXPENSE),
                        Transaction("3", "Netflix Subscription", -12.99, "14/05/2026", "Subscription", TransactionType.EXPENSE)
                    )
                ),
                onAddTransaction = { _, _, _, _, _, _, _ -> },
                onDeleteTransaction = {},
                onAddAiTransaction = {}
            )
        }
    }
}