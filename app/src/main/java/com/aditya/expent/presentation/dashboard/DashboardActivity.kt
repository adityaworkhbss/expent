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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya.expent.R
import com.aditya.expent.domain.model.Reminder
import com.aditya.expent.domain.model.Transaction
import com.aditya.expent.domain.model.TransactionType
import com.aditya.expent.data.remote.dto.CategoryResponseDto
import com.aditya.expent.data.remote.dto.PaymentModeResponseDto
import com.aditya.expent.utils.CategoryUtils.getCategoryIconAndColor
import com.aditya.expent.presentation.auth.AuthActivity
import com.aditya.expent.presentation.transactions.TransactionActivity
import com.aditya.expent.presentation.component.ExpentDatePicker
import com.aditya.expent.presentation.profile.ProfileActivity
import com.aditya.expent.presentation.widget.AddTransactionWidget
import com.aditya.expent.presentation.theme.ExpentTheme
import com.aditya.expent.presentation.theme.EmeraldPrimary
import com.aditya.expent.presentation.theme.ColorIncome
import com.aditya.expent.presentation.theme.ColorExpense
import com.aditya.expent.presentation.theme.DarkSoftColors
import com.aditya.expent.presentation.theme.LocalSoftColors
import com.aditya.expent.presentation.theme.softColors
import com.aditya.expent.utils.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue
import javax.inject.Inject
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.graphicsLayer
import com.aditya.expent.presentation.analysis.AnalysisActivity
import com.aditya.expent.presentation.cashflow.CashflowActivity
import com.aditya.expent.utils.AppUtils
import com.aditya.expent.utils.AppLogger
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@AndroidEntryPoint
class DashboardActivity : ComponentActivity() {
    
    @Inject
    lateinit var sessionManager: SessionManager

    private val viewModel: DashboardViewModel by viewModels()

    private var widgetAddType by mutableStateOf<TransactionType?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        AppLogger.debug("DashboardActivity", "POST_NOTIFICATIONS permission granted: $isGranted")
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (sessionManager.getUser() == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        checkNotificationPermission()

        if (savedInstanceState == null) {
            widgetAddType = AddTransactionWidget.requestedType(intent)
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
                        },
                        requestedAddType = widgetAddType,
                        onAddRequestHandled = { widgetAddType = null },
                        onAiMessageShown = { viewModel.clearAiMessage() },
                        onCompleteReminder = { reminder, amount, accountId, accountName ->
                            viewModel.completeReminder(reminder, amount, accountId, accountName)
                        },
                        onCompletedEmiShown = { viewModel.clearCompletedEmi() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        AddTransactionWidget.requestedType(intent)?.let { widgetAddType = it }
    }
    override fun onResume() {
        super.onResume()
        Log.d("DashboardActivity", "onResume: Reloading customizations via viewModel.loadCustomization()")

        viewModel.loadCustomization()
        viewModel.loadAccounts()

        if (sessionManager.getUser() == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardState,
    onAddTransaction: (String, Double, String, TransactionType, String, String, String) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onAddAiTransaction: (String) -> Unit,
    requestedAddType: TransactionType? = null,
    onAddRequestHandled: () -> Unit = {},
    onAiMessageShown: () -> Unit = {},
    onCompleteReminder: (Reminder, Double, String, String) -> Unit = { _, _, _, _ -> },
    onCompletedEmiShown: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    Log.d("DashboardActivity", "DashboardScreen Composing: state.reminder = ${state.reminder}, state.aiTransaction = ${state.aiTransaction}")
    var isExpanded by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var addSheetInitialType by remember { mutableStateOf(TransactionType.EXPENSE) }

    LaunchedEffect(state.aiMessage) {
        state.aiMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            onAiMessageShown()
        }
    }

    LaunchedEffect(requestedAddType) {
        if (requestedAddType != null) {
            addSheetInitialType = requestedAddType
            showBottomSheet = true
            onAddRequestHandled()
        }
    }

    var reminderToPay by remember { mutableStateOf<Reminder?>(null) }
    val dismissingReminderIds = remember { mutableStateListOf<String>() }

    val reminders = state.reminders

    val pagerState = rememberPagerState(pageCount = { reminders.size })
    
    val visibleTransactions = if (isExpanded) state.recentTransactions else state.recentTransactions.take(3)
    val groupedTransactions = visibleTransactions.groupBy { formatDisplayDate(it.date) }

    Scaffold(
        floatingActionButton = {
            GlowFloatingActionButton(
                onClick = {
                    addSheetInitialType = TransactionType.EXPENSE
                    showBottomSheet = true
                },
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
                                    shape = RoundedCornerShape(10.dp),
                                    color = EmeraldPrimary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "${reminders.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // Smooth snap-scrolling pager with optimized spacing
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                pageSpacing = 12.dp
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
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            val scaleVal = (0.94f + (1f - absPageOffset.coerceIn(0f, 1f)) * 0.06f) * animatedScale
                                            scaleX = scaleVal
                                            scaleY = scaleVal
                                            
                                            alpha = (0.65f + (1f - absPageOffset.coerceIn(0f, 1f)) * 0.35f) * animatedAlpha
                                            translationY = animatedTranslationY
                                        }
                                ) {
                                    ReminderCard(
                                        reminder = reminder,
                                        onTick = { reminderToPay = reminder },
                                        onEdit = { reminderToPay = reminder }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            ReminderDotsIndicator(
                                count = reminders.size.coerceAtMost(5),
                                activeIndex = pagerState.currentPage,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                        Spacer(modifier = Modifier.height(28.dp))
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
                    incomeAmounts = state.incomeAmountByCategory,
                    initialType = addSheetInitialType,
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

            // Pay / receive a reminder: records the transaction (and EMI month) from the sheet
            reminderToPay?.let { reminder ->
                ReminderPaySheet(
                    reminder = reminder,
                    accounts = state.accounts,
                    onDismiss = { reminderToPay = null },
                    onSave = { amount, accountId, accountName ->
                        reminderToPay = null
                        coroutineScope.launch {
                            dismissingReminderIds.add(reminder.id)
                            delay(350)
                            onCompleteReminder(reminder, amount, accountId, accountName)
                            dismissingReminderIds.remove(reminder.id)
                        }
                    }
                )
            }

            // Last EMI of a loan paid
            state.completedEmiName?.let { emiName ->
                AlertDialog(
                    onDismissRequest = onCompletedEmiShown,
                    shape = RoundedCornerShape(28.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Celebration,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "Congratulations!",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    text = {
                        Text(
                            text = "You paid all monthly EMIs of \"$emiName\".",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = onCompletedEmiShown,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                        }
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
    val (matchedIcon, matchedColor) = getCategoryIconAndColor(reminder.category.ifBlank { reminder.title })
    val categoryColor = matchedColor
    val icon = when {
        reminder.kind == Reminder.Kind.EMI -> Icons.Default.AccountBalance
        reminder.kind == Reminder.Kind.INCOME && reminder.category.isBlank() -> Icons.AutoMirrored.Filled.TrendingUp
        else -> matchedIcon
    }

    val accentColor = if (reminder.type == TransactionType.INCOME) ColorIncome else ColorExpense
    val (daysLeftText, badgeColor) = getDaysLeftInfo(reminder.dueDate)

    val formattedAmount = try {
        java.text.NumberFormat.getNumberInstance(java.util.Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(reminder.amount)
    } catch (e: Exception) {
        String.format("%.2f", reminder.amount)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                spotColor = accentColor.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(22.dp))
            .clickable { onEdit() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Category Icon + Title & Type + Due Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(categoryColor.copy(alpha = 0.14f)),
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

                // Title + Category/Kind Chip
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = categoryColor.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = reminder.category.ifBlank {
                                    when (reminder.kind) {
                                        Reminder.Kind.INCOME -> "Income"
                                        Reminder.Kind.EMI -> "EMI"
                                        Reminder.Kind.SUBSCRIPTION -> "Subscription"
                                    }
                                }.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = categoryColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Due status badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                        )
                        Text(
                            text = daysLeftText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }
            }

            // Description (Full width, readable, up to 2 lines)
            if (reminder.description.isNotBlank()) {
                Text(
                    text = reminder.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Divider
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 1.dp
            )

            // Bottom Row: Amount + Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (reminder.type == TransactionType.INCOME) "Expected Income" else "Amount Due",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (reminder.type == TransactionType.INCOME) "+$$formattedAmount" else "-$$formattedAmount",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )
                }

                // Interactive Action Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.12f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTick() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = if (reminder.type == TransactionType.INCOME) "Receive" else "Pay",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (reminder.type == TransactionType.INCOME) "Receive" else "Pay",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
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
            
            val paymentMode = transaction.accountName ?: "Cash"
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
                painter = painterResource(id = R.drawable.ic_add_expense),
                contentDescription = "Add Expense",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .scale(pulseScale)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionBottomSheet(
    categories: List<CategoryResponseDto>,
    accounts: List<PaymentModeResponseDto>,
    aiTransactionEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, TransactionType, String, String, String) -> Unit,
    onAiSave: (String) -> Unit = {},
    incomeAmounts: Map<String, Double> = emptyMap(),
    initialType: TransactionType = TransactionType.EXPENSE
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    Log.d("DashboardActivity", "AddTransactionBottomSheet: aiTransactionEnabled = $aiTransactionEnabled")
    var amountText by remember { mutableStateOf("") }

    var selectedType by remember(initialType) { mutableStateOf(initialType) }
    val isIncome = selectedType == TransactionType.INCOME

    // Every saved category of the selected type
    val typeCategories = remember(categories, selectedType) {
        val type = if (selectedType == TransactionType.INCOME) "income" else "expense"
        categories.filter { it.type.equals(type, ignoreCase = true) }
    }

    var selectedCategory by remember(typeCategories) {
        mutableStateOf(typeCategories.firstOrNull()?.name ?: if (isIncome) "" else "Others")
    }

    // Income amount is pre-filled when the category has an income source in Cashflow, and stays editable
    val incomePrefill = if (isIncome) incomeAmounts[selectedCategory] else null
    LaunchedEffect(selectedType, selectedCategory, incomeAmounts) {
        if (isIncome) {
            amountText = incomePrefill
                ?.toBigDecimal()?.stripTrailingZeros()?.toPlainString()
                .orEmpty()
        }
    }
    // Don't carry a pre-filled income amount over into an expense
    LaunchedEffect(selectedType) {
        if (!isIncome) amountText = ""
    }

    val resolvedAmount: Double? = amountText.toDoubleOrNull()

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

    val colors = softColors()
    val isDark = colors == DarkSoftColors

    val finalInputMode = if (aiTransactionEnabled) "Smart" else "Manual"
    Log.d("DashboardActivity", "AddTransactionBottomSheet: Rendering with finalInputMode = $finalInputMode, aiTransactionEnabled = $aiTransactionEnabled")

    val activeColor by animateColorAsState(
        targetValue = when {
            finalInputMode == "Smart" -> colors.ai
            isIncome -> colors.income
            else -> colors.expense
        },
        animationSpec = tween(400),
        label = "ActiveColor"
    )
    // Sheet background picks up a faint wash of the active colour
    val sheetBackground = lerp(colors.background, activeColor, if (isDark) 0.06f else 0.05f)

    CompositionLocalProvider(LocalSoftColors provides colors) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = sheetBackground,
            contentColor = colors.ink,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            tonalElevation = 0.dp,
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = activeColor.copy(alpha = 0.35f))
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (finalInputMode == "Smart") "Quick Add" else "Add Transaction",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.ink
                        )
                        Text(
                            text = when {
                                finalInputMode == "Smart" -> "Describe it, AI fills in the details"
                                isIncome -> "Record money coming in"
                                else -> "Record money going out"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.muted
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = colors.card,
                            contentColor = colors.muted
                        ),
                        modifier = Modifier
                            .size(36.dp)
                            .border(1.dp, colors.cardBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                AnimatedContent(
                    targetState = finalInputMode,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "InputModeTransition"
                ) { mode ->
                    if (mode == "Smart") {
                        SmartTransactionInput(onAiSave = onAiSave)
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            TransactionTypeToggle(
                                selectedType = selectedType,
                                onTypeSelected = { selectedType = it }
                            )

                            // Amount card, tapping anywhere on it focuses the input
                            val amountFocusRequester = remember { FocusRequester() }
                            val amountTextStyle = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 44.sp,
                                color = colors.ink
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 10.dp,
                                        shape = RoundedCornerShape(24.dp),
                                        ambientColor = activeColor.copy(alpha = 0.25f),
                                        spotColor = activeColor.copy(alpha = 0.25f)
                                    )
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(colors.card)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(activeColor.copy(alpha = 0.10f), Color.Transparent)
                                        )
                                    )
                                    .border(1.dp, activeColor.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { amountFocusRequester.requestFocus() }
                                    .padding(vertical = 22.dp, horizontal = 16.dp)
                            ) {
                                Text(
                                    text = if (isIncome) "How much did you receive?" else "How much did you spend?",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.muted,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "$",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeColor
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    BasicTextField(
                                        value = amountText,
                                        onValueChange = {
                                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                                amountText = it
                                            }
                                        },
                                        textStyle = amountTextStyle,
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        cursorBrush = SolidColor(activeColor),
                                        decorationBox = { innerTextField ->
                                            Box(contentAlignment = Alignment.CenterStart) {
                                                if (amountText.isEmpty()) {
                                                    Text(
                                                        text = "0.00",
                                                        style = amountTextStyle,
                                                        color = colors.muted.copy(alpha = 0.35f)
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        },
                                        modifier = Modifier
                                            .width(IntrinsicSize.Min)
                                            .defaultMinSize(minWidth = 24.dp)
                                            .focusRequester(amountFocusRequester)
                                    )
                                }
                                AnimatedVisibility(visible = incomePrefill != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .padding(top = 10.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(activeColor.copy(alpha = 0.12f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = activeColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Pre-filled from $selectedCategory, tap to edit",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = activeColor
                                        )
                                    }
                                }
                            }

                            // Category Selection
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SheetSectionLabel(text = "Category")
                                if (typeCategories.isEmpty()) {
                                    SheetEmptyHint(
                                        text = if (isIncome) "No income categories yet" else "No expense categories yet"
                                    )
                                } else {
                                    // Wrap onto several lines so every category is visible without sideways scrolling
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        typeCategories.forEach { cat ->
                                            val (icon, catColor) = getCategoryIconAndColor(cat.name)
                                            SheetSelectionChip(
                                                label = cat.name,
                                                icon = icon,
                                                accent = catColor,
                                                selected = selectedCategory == cat.name,
                                                onClick = { selectedCategory = cat.name }
                                            )
                                        }
                                    }
                                }
                            }

                            // Account Selection (Filtered based on INCOME PAY_NOW / EXPENSE All)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SheetSectionLabel(text = if (isIncome) "Received in" else "Paid from")
                                if (filteredAccounts.isEmpty()) {
                                    SheetEmptyHint(text = "No applicable accounts found")
                                } else {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(filteredAccounts) { acc ->
                                            val accColor = when (acc.type.uppercase()) {
                                                "BANK" -> Color(0xFF5B8DEF)
                                                "CREDIT" -> Color(0xFFF06292)
                                                "CASH" -> Color(0xFF2EB67D)
                                                else -> Color(0xFFF5A623)
                                            }
                                            val icon = when (acc.type.uppercase()) {
                                                "BANK" -> Icons.Default.AccountBalance
                                                "CREDIT" -> Icons.Default.CreditCard
                                                else -> Icons.Default.AccountBalanceWallet
                                            }
                                            SheetSelectionChip(
                                                label = acc.name,
                                                icon = icon,
                                                accent = accColor,
                                                selected = selectedAccountName == acc.name,
                                                onClick = {
                                                    selectedAccountName = acc.name
                                                    selectedAccountId = acc.id
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Date card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(colors.card)
                                    .border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp))
                                    .clickable { showDatePicker = true }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(activeColor.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Date",
                                        tint = activeColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Date",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.muted
                                    )
                                    Text(
                                        text = formatDisplayDate(selectedDateText),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.ink
                                    )
                                }
                                Text(
                                    text = "Change",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = activeColor
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = activeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Save Trigger Button
                            val isEnabled = resolvedAmount?.let { it > 0 } == true
                            SheetPrimaryButton(
                                text = if (isEnabled) {
                                    "Add ${if (isIncome) "Income" else "Expense"} · $$amountText"
                                } else {
                                    "Enter an amount to continue"
                                },
                                icon = if (isEnabled) Icons.Default.Check else null,
                                gradient = listOf(activeColor, lerp(activeColor, Color.White, 0.25f)),
                                enabled = isEnabled,
                                onClick = {
                                    val amt = resolvedAmount
                                    if (amt != null && amt > 0) {
                                        onSave(selectedCategory, amt, selectedCategory, selectedType, selectedDateText, selectedAccountName, selectedAccountId)
                                    }
                                }
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
        },
        allowFutureDates = false
    )
}

private val AiExamples = listOf(
    "Spent $25 on pizza yesterday using cash",
    "Got $3000 salary today",
    "Paid $60 for Uber with card"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmartTransactionInput(onAiSave: (String) -> Unit) {
    val colors = LocalSoftColors.current
    var aiText by remember { mutableStateOf("") }
    val canSubmit = aiText.isNotBlank()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = aiText,
            onValueChange = { aiText = it },
            placeholder = {
                Text(
                    text = "What did you spend or earn?",
                    color = colors.muted.copy(alpha = 0.7f)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = colors.ai.copy(alpha = 0.2f),
                    spotColor = colors.ai.copy(alpha = 0.2f)
                ),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.ai,
                unfocusedBorderColor = colors.cardBorder,
                focusedContainerColor = colors.card,
                unfocusedContainerColor = colors.card,
                focusedTextColor = colors.ink,
                unfocusedTextColor = colors.ink,
                cursorColor = colors.ai
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
            maxLines = 6
        )

        // What the AI picks up from the sentence
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.ai.copy(alpha = 0.08f))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = colors.ai,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "AI detects the amount, category, type, account and date for you.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.ink.copy(alpha = 0.75f)
            )
        }

        // Tap an example to fill the input
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SheetSectionLabel(text = "Try an example")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiExamples.forEach { example ->
                    Surface(
                        onClick = { aiText = example },
                        shape = RoundedCornerShape(14.dp),
                        color = colors.card,
                        border = BorderStroke(1.dp, colors.cardBorder)
                    ) {
                        Text(
                            text = example,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.ink.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        SheetPrimaryButton(
            text = "Parse & Save",
            icon = Icons.Default.AutoAwesome,
            gradient = listOf(colors.ai, colors.aiEnd),
            enabled = canSubmit,
            onClick = { onAiSave(aiText) }
        )
    }
}

@Composable
private fun SheetPrimaryButton(
    text: String,
    icon: ImageVector?,
    gradient: List<Color>,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalSoftColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        label = "ButtonPress"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        contentPadding = PaddingValues(),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(buttonScale)
            .shadow(
                elevation = if (enabled) 12.dp else 0.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = gradient.first(),
                spotColor = gradient.first()
            ),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = colors.cardBorder,
            disabledContentColor = colors.muted
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (enabled) Modifier.background(Brush.horizontalGradient(gradient)) else Modifier
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TransactionTypeToggle(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit
) {
    val colors = LocalSoftColors.current
    val isExpense = selectedType == TransactionType.EXPENSE
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(colors.card)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(26.dp))
            .padding(4.dp)
    ) {
        // 0 = Income half, 1 = Expense half
        val indicatorPosition by animateFloatAsState(
            targetValue = if (isExpense) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
            label = "ToggleOffset"
        )
        val indicatorColor by animateColorAsState(
            targetValue = if (isExpense) colors.expense else colors.income,
            label = "ToggleColor"
        )

        // Sliding pill behind the selected option
        Box(
            modifier = Modifier
                .graphicsLayer { translationX = size.width * indicatorPosition }
                .fillMaxWidth(0.5f)
                .fillMaxHeight()
                .shadow(6.dp, RoundedCornerShape(22.dp), spotColor = indicatorColor)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(indicatorColor, lerp(indicatorColor, Color.White, 0.25f))
                    )
                )
        )

        Row(modifier = Modifier.fillMaxSize()) {
            TypeToggleOption(
                label = "Income",
                icon = Icons.Default.ArrowDownward,
                selected = !isExpense,
                onClick = { onTypeSelected(TransactionType.INCOME) }
            )
            TypeToggleOption(
                label = "Expense",
                icon = Icons.Default.ArrowUpward,
                selected = isExpense,
                onClick = { onTypeSelected(TransactionType.EXPENSE) }
            )
        }
    }
}

@Composable
private fun RowScope.TypeToggleOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else LocalSoftColors.current.muted,
        label = "ToggleText"
    )
    Row(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SheetSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = LocalSoftColors.current.muted
    )
}

@Composable
private fun SheetEmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = LocalSoftColors.current.muted,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun SheetSelectionChip(
    label: String,
    icon: ImageVector,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalSoftColors.current
    val containerColor by animateColorAsState(
        targetValue = if (selected) lerp(colors.card, accent, 0.12f) else colors.card,
        label = "ChipBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) accent else colors.ink,
        label = "ChipText"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.55f) else colors.cardBorder,
        label = "ChipBorder"
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (selected) accent else accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else accent,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor
            )
            AnimatedVisibility(visible = selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(16.dp)
                )
            }
        }
    }
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