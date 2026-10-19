package com.aditya.expent.presentation.cashflow

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya.expent.data.remote.dto.CategoryResponseDto
import com.aditya.expent.domain.model.Budget
import com.aditya.expent.domain.model.Expense
import com.aditya.expent.presentation.onboard.RecurringExpense
import com.aditya.expent.presentation.onboard.Subscription
import com.aditya.expent.presentation.theme.ColorExpense
import com.aditya.expent.presentation.theme.ColorIncome
import com.aditya.expent.presentation.theme.EmeraldPrimary
import com.aditya.expent.presentation.theme.ExpentTheme
import com.aditya.expent.presentation.component.DayOfMonthPicker
import com.aditya.expent.presentation.component.dayOfMonthLabel
import com.aditya.expent.utils.DayOfMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import com.aditya.expent.utils.CategoryUtils
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import java.text.NumberFormat
import java.util.Locale
import dagger.hilt.android.AndroidEntryPoint
import java.time.OffsetDateTime
import kotlin.getValue
import kotlin.time.Clock.System.now

enum class CashflowTab {
    INCOMING,
    OUTGOING
}

sealed interface CashflowBottomSheetMode {

    data object AddIncome : CashflowBottomSheetMode

    data class EditIncome(
        val income: Subscription
    ) : CashflowBottomSheetMode

    data object AddSubscription : CashflowBottomSheetMode

    data class EditSubscription(
        val subscription: Subscription
    ) : CashflowBottomSheetMode

    data object AddEmi : CashflowBottomSheetMode

    data class EditEmi(
        val emi: RecurringExpense
    ) : CashflowBottomSheetMode
}

@AndroidEntryPoint
class CashflowActivity : ComponentActivity() {

    private val viewModel: CashflowViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ExpentTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val state by viewModel.budgetState.collectAsState()
                    CashflowScreen(
                        income = state.budgets,
                        categories = state.categories,
                        expense = state.expense,
                        onBack = {
                            finish()
                        },
                        onDeleteBudget = { id -> viewModel.deleteBudget(id) },
                        onUpdateBudget = { id, categoryId, amount, date ->
                            viewModel.updateBudget(id, categoryId, amount, date)
                        },
                        onAddBudget = { categoryId, amount, date ->
                            viewModel.saveBudget(categoryId, amount, date)
                        },
                        onDeleteEmi = { id -> viewModel.deleteEmi(id) },
                        onUpdateEmi = { id, name, amount, date, categoryId, tenure, paid ->
                            viewModel.updateEmi(id, name, amount, date, categoryId, tenure, paid)
                        },
                        onAddEmi = { name, amount, date, categoryId, tenure, paid ->
                            viewModel.saveEmi(name, amount, date, categoryId, tenure, paid)
                        }
                    )
                }
            }
        }
    }
}

// Callback shapes: budget (id, categoryId, amount, day) and EMI (id, name, amount, day, tenure, paid).
// day = day of the month (1-31); tenure null = every month (subscription).
typealias UpdateBudget = (id: String, categoryId: String?, amount: Double, date: Int) -> Unit
typealias AddBudget = (categoryId: String?, amount: Double, date: Int) -> Unit
typealias UpdateEmi = (id: String, name: String, amount: Double, date: Int, categoryId: String?, tenure: Int?, paid: Int) -> Unit
typealias AddEmi = (name: String, amount: Double, date: Int, categoryId: String?, tenure: Int?, paid: Int) -> Unit

@Composable
fun CashflowScreen(
    income : List<Budget>,
    categories: List<CategoryResponseDto>,
    expense : List<Expense>,
    onBack : () -> Unit,
    onDeleteBudget: (String) -> Unit = {},
    onUpdateBudget: UpdateBudget = { _, _, _, _ -> },
    onAddBudget: AddBudget = { _, _, _ -> },
    onDeleteEmi: (String) -> Unit = {},
    onUpdateEmi: UpdateEmi = { _, _, _, _, _, _, _ -> },
    onAddEmi: AddEmi = { _, _, _, _, _, _ -> }
) {

    var selectedTab by remember {
        mutableStateOf(CashflowTab.INCOMING)
    }

    val categoriesType = when (selectedTab) {
        CashflowTab.INCOMING -> "INCOME"
        CashflowTab.OUTGOING -> "EXPENSE"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 0.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    NavigationBarItem(
                        selected = selectedTab == CashflowTab.INCOMING,
                        onClick = { selectedTab = CashflowTab.INCOMING },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Incoming"
                            )
                        },
                        label = {
                            Text(
                                "Incoming",
                                fontWeight = if (selectedTab == CashflowTab.INCOMING) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldPrimary.copy(alpha = 0.12f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )

                    NavigationBarItem(
                        selected = selectedTab == CashflowTab.OUTGOING,
                        onClick = { selectedTab = CashflowTab.OUTGOING },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Outgoing"
                            )
                        },
                        label = {
                            Text(
                                "Outgoing",
                                fontWeight = if (selectedTab == CashflowTab.OUTGOING) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldPrimary.copy(alpha = 0.12f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        val title = if (selectedTab == CashflowTab.INCOMING) "Incoming" else "Outgoing"
        CashflowContentScreen(
            modifier = Modifier.padding(paddingValues),
            title = title,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            income = income,
            expense = expense,
            categories = categories.filter { it.type.equals(categoriesType, ignoreCase = true) },
            allCategories = categories,
            onBack = onBack,
            onDeleteBudget = onDeleteBudget,
            onUpdateBudget = onUpdateBudget,
            onAddBudget = onAddBudget,
            onDeleteEmi = onDeleteEmi,
            onUpdateEmi = onUpdateEmi,
            onAddEmi = onAddEmi
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashflowContentScreen(
    modifier: Modifier = Modifier,
    title: String,
    selectedTab: CashflowTab = if (title == "Incoming") CashflowTab.INCOMING else CashflowTab.OUTGOING,
    onTabSelected: (CashflowTab) -> Unit = {},
    income: List<Budget>,
    expense: List<Expense>,
    categories: List<CategoryResponseDto> = emptyList(),
    allCategories: List<CategoryResponseDto> = emptyList(),
    themeColor: Color = EmeraldPrimary,
    onBack: () -> Unit,
    onDeleteBudget: (String) -> Unit = {},
    onUpdateBudget: UpdateBudget = { _, _, _, _ -> },
    onAddBudget: AddBudget = { _, _, _ -> },
    onDeleteEmi: (String) -> Unit = {},
    onUpdateEmi: UpdateEmi = { _, _, _, _, _, _, _ -> },
    onAddEmi: AddEmi = { _, _, _, _, _, _ -> }
) {
    // tenure == null → every month (subscription); otherwise an EMI
    val recurringExpenses: List<RecurringExpense> = expense
        .filterNot { it.isEveryMonth }
        .map { item ->
            RecurringExpense(
                name = item.name,
                amount = item.amount.toPlainAmount(),
                totalMonths = item.tenure.toString(),
                monthsPaid = item.paid.toString(),
                date = item.date,
                id = item.id,
                categoryId = item.categoryId
            )
        }

    val subscriptions: List<Subscription> = expense
        .filter { it.isEveryMonth }
        .map { item ->
            Subscription(
                name = item.name,
                amount = item.amount.toPlainAmount(),
                date = item.date,
                id = item.id,
                categoryId = item.categoryId
            )
        }

    val incomes = income.map { item ->
        val resolvedName = item.categoryName?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            ?: item.categoryId?.let { catId -> allCategories.firstOrNull { it.id == catId }?.name }
            ?: "Income"
        Subscription(
            name = resolvedName,
            amount = item.amount.toPlainAmount(),
            date = item.date,
            id = item.id,
            categoryId = item.categoryId
        )
    }

    val isIncoming = selectedTab == CashflowTab.INCOMING
    val activeList = if (isIncoming) incomes else subscriptions

    val totalIncome = income.sumOf { it.amount }
    val totalEmi = recurringExpenses.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    val totalSub = subscriptions.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    val totalOutgoing = totalEmi + totalSub

    var sheetMode by remember {
        mutableStateOf<CashflowBottomSheetMode?>(null)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 10.dp
            )
        ) {
            item {
                HeaderSection(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    onBack = onBack
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                val activeCount = if (isIncoming) incomes.size else (recurringExpenses.size + subscriptions.size)
                CashflowSummaryCard(
                    isIncoming = isIncoming,
                    totalAmount = if (isIncoming) totalIncome else totalOutgoing,
                    itemCount = activeCount,
                    secondaryInfo = if (isIncoming) {
                        "$activeCount active incoming flow${if (activeCount == 1) "" else "s"}"
                    } else {
                        "$activeCount active outgoing flow${if (activeCount == 1) "" else "s"}"
                    }
                )
            }

            if (!isIncoming) {
                item {
                    Spacer(modifier = Modifier.height(18.dp))

                    SectionHeader(
                        title = "Recurring EMIs & Loans",
                        count = recurringExpenses.size,
                        themeColor = Color(0xFFE53935),
                        onAddClick = {
                            sheetMode = CashflowBottomSheetMode.AddEmi
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (recurringExpenses.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No EMIs or Loans",
                            subtitle = "You haven't added any recurring EMIs or loans yet.",
                            icon = Icons.Default.CreditCard,
                            buttonText = "Add EMI / Loan",
                            onButtonClick = {
                                sheetMode = CashflowBottomSheetMode.AddEmi
                            }
                        )
                    }
                } else {
                    items(recurringExpenses.size) { index ->
                        val emi = recurringExpenses[index]
                        val paid = emi.monthsPaid.toFloatOrNull() ?: 0f
                        val total = emi.totalMonths.toFloatOrNull() ?: 1f
                        val progress = (paid / total).coerceIn(0f, 1f)

                        EnhancedEmiCard(
                            emi = emi,
                            progress = progress,
                            onEditClick = {
                                sheetMode = CashflowBottomSheetMode.EditEmi(emi)
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(18.dp))

                SectionHeader(
                    title = if (isIncoming) "Active Incomes" else "Active Subscriptions",
                    count = activeList.size,
                    themeColor = if (isIncoming) ColorIncome else Color(0xFFE53935),
                    onAddClick = {
                        sheetMode = if (isIncoming) CashflowBottomSheetMode.AddIncome else CashflowBottomSheetMode.AddSubscription
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))
            }

            if (activeList.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = if (isIncoming) "No Active Incomes" else "No Active Subscriptions",
                        subtitle = if (isIncoming) "You haven't added any recurring income sources yet." else "You haven't added any recurring subscriptions yet.",
                        icon = if (isIncoming) Icons.AutoMirrored.Filled.TrendingUp else Icons.Default.Subscriptions,
                        buttonText = if (isIncoming) "Add Income" else "Add Subscription",
                        onButtonClick = {
                            sheetMode = if (isIncoming) CashflowBottomSheetMode.AddIncome else CashflowBottomSheetMode.AddSubscription
                        }
                    )
                }
            } else {
                items(activeList.size) { index ->
                    val sub = activeList[index]
                    EnhancedSubscriptionCard(
                        subscription = sub,
                        isIncome = isIncoming,
                        onEditClick = {
                            sheetMode = if (isIncoming) {
                                CashflowBottomSheetMode.EditIncome(sub)
                            } else {
                                CashflowBottomSheetMode.EditSubscription(sub)
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        sheetMode?.let { mode ->
            CashflowBottomSheet(
                mode = mode,
                categories = categories,
                allCategories = allCategories,
                incomeList = income,
                onDismiss = { sheetMode = null },
                onSaveBudget = { id, categoryId, amount, date ->
                    if (id != null) {
                        onUpdateBudget(id, categoryId, amount, date)
                    } else {
                        onAddBudget(categoryId, amount, date)
                    }
                },
                onDeleteBudget = onDeleteBudget,
                onSaveEmi = { id, name, amount, date, categoryId, tenure, paid ->
                    if (id != null) {
                        onUpdateEmi(id, name, amount, date, categoryId, tenure, paid)
                    } else {
                        onAddEmi(name, amount, date, categoryId, tenure, paid)
                    }
                },
                onDeleteEmi = onDeleteEmi
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashflowBottomSheet(
    mode: CashflowBottomSheetMode,
    categories: List<CategoryResponseDto>,
    allCategories: List<CategoryResponseDto> = categories,
    incomeList: List<Budget>,
    onDismiss: () -> Unit,
    onSaveBudget: (id: String?, categoryId: String?, amount: Double, date: Int) -> Unit,
    onDeleteBudget: (id: String) -> Unit,
    onSaveEmi: (id: String?, name: String, amount: Double, date: Int, categoryId: String?, tenure: Int?, paid: Int) -> Unit,
    onDeleteEmi: (id: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isIncomeFlow = mode is CashflowBottomSheetMode.AddIncome || mode is CashflowBottomSheetMode.EditIncome
    val effectiveCategories = remember(categories, allCategories, isIncomeFlow) {
        val type = if (isIncomeFlow) "INCOME" else "EXPENSE"
        (categories + allCategories)
            .filter { it.type.equals(type, ignoreCase = true) }
            .distinctBy { it.id }
    }

    val initialName = when (mode) {
        is CashflowBottomSheetMode.EditEmi -> mode.emi.name
        is CashflowBottomSheetMode.EditSubscription -> mode.subscription.name
        else -> ""
    }
    val initialAmount = when (mode) {
        is CashflowBottomSheetMode.EditIncome -> mode.income.amount
        is CashflowBottomSheetMode.EditSubscription -> mode.subscription.amount
        is CashflowBottomSheetMode.EditEmi -> mode.emi.amount
        else -> ""
    }
    val initialDate: Int? = when (mode) {
        is CashflowBottomSheetMode.EditIncome -> mode.income.date
        is CashflowBottomSheetMode.EditSubscription -> mode.subscription.date
        is CashflowBottomSheetMode.EditEmi -> mode.emi.date
        else -> null
    }
    val initialCategoryId: String? = when (mode) {
        is CashflowBottomSheetMode.EditIncome -> mode.income.categoryId
        is CashflowBottomSheetMode.EditSubscription -> mode.subscription.categoryId
        is CashflowBottomSheetMode.EditEmi -> mode.emi.categoryId
        is CashflowBottomSheetMode.AddIncome ->
            (effectiveCategories.firstOrNull { it.name.equals("Salary", ignoreCase = true) } ?: effectiveCategories.firstOrNull())?.id
        else -> null
    }
    val initialCategoryName = effectiveCategories.firstOrNull { it.id == initialCategoryId }?.name.orEmpty()
    val initialTenure = when (mode) {
        is CashflowBottomSheetMode.EditEmi -> mode.emi.totalMonths
        else -> ""
    }
    val initialMonthsPaid = when (mode) {
        is CashflowBottomSheetMode.EditEmi -> mode.emi.monthsPaid
        else -> ""
    }

    var nameText by remember(mode) { mutableStateOf(initialName) }
    var amountText by remember(mode) { mutableStateOf(initialAmount) }
    var selectedCategoryId by remember(mode) { mutableStateOf(initialCategoryId) }
    var selectedCategoryName by remember(mode) { mutableStateOf(initialCategoryName) }
    var selectedDay by remember(mode) { mutableStateOf(initialDate) }
    var tenureText by remember(mode) { mutableStateOf(initialTenure) }
    var monthsPaidText by remember(mode) { mutableStateOf(initialMonthsPaid) }

    var showDatePicker by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val title = when (mode) {
        is CashflowBottomSheetMode.AddIncome -> "Add Recurring Income"
        is CashflowBottomSheetMode.EditIncome -> "Edit Recurring Income"
        is CashflowBottomSheetMode.AddSubscription -> "Add Active Subscription"
        is CashflowBottomSheetMode.EditSubscription -> "Edit Subscription"
        is CashflowBottomSheetMode.AddEmi -> "Add Recurring EMI / Loan"
        is CashflowBottomSheetMode.EditEmi -> "Edit EMI / Loan"
    }

    val isEdit = mode is CashflowBottomSheetMode.EditIncome ||
            mode is CashflowBottomSheetMode.EditSubscription ||
            mode is CashflowBottomSheetMode.EditEmi

    val isEmi = mode is CashflowBottomSheetMode.AddEmi || mode is CashflowBottomSheetMode.EditEmi
    val isSubscription = mode is CashflowBottomSheetMode.AddSubscription || mode is CashflowBottomSheetMode.EditSubscription

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = EmeraldPrimary,
        focusedLabelColor = EmeraldPrimary,
        cursorColor = EmeraldPrimary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.1.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Name Field (for EMI and Subscription)
            if (isEmi || isSubscription) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text(if (isEmi) "Name the EMI / Loan" else "Subscription Name (e.g. Netflix)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = textFieldColors
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 2. Category Dropdown
            Column(modifier = Modifier.fillMaxWidth()) {
                val arrowRotation by animateFloatAsState(
                    targetValue = if (categoryDropdownExpanded) 180f else 0f,
                    label = "ArrowRotation"
                )

                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = if (effectiveCategories.isEmpty()) "No Categories Found" else if (selectedCategoryName.isBlank()) "Select Category" else selectedCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(14.dp),
                        colors = textFieldColors,
                        leadingIcon = {
                            if (selectedCategoryName.isNotBlank() && effectiveCategories.isNotEmpty()) {
                                val iconAndColor = CategoryUtils.getCategoryIconAndColor(selectedCategoryName)
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
                                tint = EmeraldPrimary,
                                modifier = Modifier.graphicsLayer { rotationZ = arrowRotation }
                            )
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false },
                        modifier = Modifier
                            .background(Color.White)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    ) {
                        if (effectiveCategories.isEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Inbox,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "No Categories Found",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = { categoryDropdownExpanded = false }
                            )
                        } else {
                            effectiveCategories.forEach { category ->
                                val isSelected = selectedCategoryName == category.name
                                val iconAndColor = CategoryUtils.getCategoryIconAndColor(category.name)
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
                                                        .background(iconAndColor.second.copy(alpha = 0.12f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = iconAndColor.first,
                                                        contentDescription = category.name,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = iconAndColor.second
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = category.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    ),
                                                    color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = EmeraldPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedCategoryId = category.id
                                        selectedCategoryName = category.name
                                        categoryDropdownExpanded = false
                                    },
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) EmeraldPrimary.copy(alpha = 0.1f) else Color.Transparent
                                        )
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 3. Amount Field
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount ($)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Day of month
            OutlinedTextField(
                value = dayOfMonthLabel(selectedDay),
                onValueChange = { },
                label = {
                    Text(
                        when {
                            isEmi -> "Due on"
                            isSubscription -> "Billing on"
                            else -> "Credited on"
                        }
                    )
                },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = textFieldColors,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date", tint = EmeraldPrimary)
                    }
                }
            )

            // 5. Tenure and Months Paid Fields (only for EMI)
            if (isEmi) {
                Spacer(modifier = Modifier.height(12.dp))
                val tenureVal = tenureText.toIntOrNull()?.takeIf { it > 0 }
                val paidVal = monthsPaidText.toIntOrNull()
                val isPaidInvalid = tenureVal != null && paidVal != null && paidVal >= tenureVal

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = tenureText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) tenureText = input
                        },
                        label = { Text("Tenure (Months)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = textFieldColors,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = monthsPaidText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) monthsPaidText = input
                        },
                        label = { Text("Months Paid") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = if (isPaidInvalid) {
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ColorExpense,
                                unfocusedBorderColor = ColorExpense,
                                errorBorderColor = ColorExpense,
                                cursorColor = ColorExpense,
                                focusedLabelColor = ColorExpense,
                                unfocusedLabelColor = ColorExpense,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        } else textFieldColors,
                        isError = isPaidInvalid,
                        supportingText = if (isPaidInvalid) {
                            { Text("Must be < $tenureVal", color = ColorExpense) }
                        } else null,
                        singleLine = true
                    )
                }
            }

            DayOfMonthPicker(
                showDialog = showDatePicker,
                selectedDay = selectedDay,
                accentColor = EmeraldPrimary,
                onDismiss = { showDatePicker = false },
                onDaySelected = {
                    selectedDay = it
                    showDatePicker = false
                }
            )

            val amount = amountText.toDoubleOrNull()
            val day = selectedDay
            val tenure = tenureText.toIntOrNull()?.takeIf { it > 0 }
            val paid = monthsPaidText.toIntOrNull() ?: 0
            val isPaidExceeded = isEmi && tenure != null && paid >= tenure

            val canSave = amount != null && day != null &&
                (!isEmi || (tenure != null && !isPaidExceeded)) &&
                ((!isEmi && !isSubscription) || nameText.isNotBlank()) &&
                (effectiveCategories.isEmpty() || selectedCategoryId != null)

            fun save(id: String?) {
                if (amount == null || day == null) return
                when {
                    isEmi -> onSaveEmi(id, nameText.trim(), amount, day, selectedCategoryId, tenure, paid)
                    isSubscription -> onSaveEmi(id, nameText.trim(), amount, day, selectedCategoryId, null, 0)
                    else -> onSaveBudget(id, selectedCategoryId, amount, day)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val id = when (mode) {
                is CashflowBottomSheetMode.EditIncome -> mode.income.id
                is CashflowBottomSheetMode.EditSubscription -> mode.subscription.id
                is CashflowBottomSheetMode.EditEmi -> mode.emi.id
                else -> null
            }

            if (isEdit) {
                Button(
                    onClick = {
                        save(id)
                        onDismiss()
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                ) {
                    Text("Update Flow", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        id?.let {
                            if (mode is CashflowBottomSheetMode.EditIncome) {
                                onDeleteBudget(it)
                            } else {
                                onDeleteEmi(it)
                            }
                        }
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ColorExpense.copy(alpha = 0.35f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorExpense)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = ColorExpense
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Flow", fontWeight = FontWeight.Bold, color = ColorExpense)
                }
            } else {
                Button(
                    onClick = {
                        save(null)
                        onDismiss()
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                ) {
                    Text("Add Flow", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HeaderSection(
    selectedTab: CashflowTab,
    onTabSelected: (CashflowTab) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "Cashflow",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Recurring incomes, loans & subscriptions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Modern Segmented Tab Switcher
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Incoming Tab
                val isIncoming = selectedTab == CashflowTab.INCOMING
                Surface(
                    onClick = { onTabSelected(CashflowTab.INCOMING) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isIncoming) Color.White else Color.Transparent,
                    shadowElevation = if (isIncoming) 2.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (isIncoming) ColorIncome else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Incoming",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isIncoming) FontWeight.Bold else FontWeight.Medium,
                            color = if (isIncoming) ColorIncome else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // Outgoing Tab
                val isOutgoing = selectedTab == CashflowTab.OUTGOING
                Surface(
                    onClick = { onTabSelected(CashflowTab.OUTGOING) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isOutgoing) Color.White else Color.Transparent,
                    shadowElevation = if (isOutgoing) 2.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (isOutgoing) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Outgoing",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isOutgoing) FontWeight.Bold else FontWeight.Medium,
                            color = if (isOutgoing) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CashflowSummaryCard(
    isIncoming: Boolean,
    totalAmount: Double,
    itemCount: Int,
    secondaryInfo: String? = null
) {
    val flowColor = if (isIncoming) ColorIncome else Color(0xFFE53935)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isIncoming) "Total Monthly Inflow" else "Total Monthly Outflow",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                val formatted = try {
                    NumberFormat.getNumberInstance(Locale.US).apply {
                        minimumFractionDigits = 2
                        maximumFractionDigits = 2
                    }.format(totalAmount)
                } catch (e: Exception) {
                    String.format("%.2f", totalAmount)
                }
                Text(
                    text = if (isIncoming) "+$$formatted / mo" else "-$$formatted / mo",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = flowColor
                )
                if (secondaryInfo != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = secondaryInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(flowColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncoming) Icons.AutoMirrored.Filled.TrendingUp else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = flowColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    count: Int? = null,
    themeColor: Color = EmeraldPrimary,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.1.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (count != null && count > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = themeColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = themeColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = themeColor.copy(alpha = 0.1f),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { onAddClick() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = themeColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Add",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = themeColor
                )
            }
        }
    }
}

@Composable
fun EnhancedEmiCard(
    emi: RecurringExpense,
    progress: Float,
    themeColor: Color = Color(0xFFE53935),
    onEditClick: () -> Unit
) {
    var isPressed by remember {
        mutableStateOf(false)
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "card_scale"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = 800,
            easing = LinearEasing
        ),
        label = "progress_animation"
    )

    val formattedAmount = try {
        val parsed = emi.amount.toDoubleOrNull() ?: 0.0
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(parsed)
    } catch (e: Exception) {
        emi.amount
    }

    val emiColor = Color(0xFFE53935)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Row 1: Icon, Title & Badge, Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(emiColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = "EMI",
                        tint = emiColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = emi.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = emiColor.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = "EMI / Loan",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = emiColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "-$$formattedAmount",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.1.sp
                        ),
                        color = emiColor
                    )
                    Text(
                        text = "/ month",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                }
            }

            // Divider separating header from description
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                thickness = 0.8.dp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Description on its own dedicated line + Edit action button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(15.dp)
                    )
                    val dueText = emi.date?.let { day ->
                        "Due on ${DayOfMonth.withSuffix(day)} of every month"
                    } ?: "Recurring monthly payment"

                    Text(
                        text = dueText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onEditClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Row 3: Progress Section
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Repayment Progress",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
                Text(
                    text = "${emi.monthsPaid} of ${emi.totalMonths} months paid (${(progress * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = emiColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    emiColor,
                                    Color(0xFFFF7043)
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun EnhancedSubscriptionCard(
    subscription: Subscription,
    themeColor: Color = EmeraldPrimary,
    isIncome: Boolean = false,
    onEditClick: () -> Unit
) {
    var isPressed by remember {
        mutableStateOf(false)
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "subscription_scale"
    )

    val (categoryIcon, categoryColor) = CategoryUtils.getCategoryIconAndColor(subscription.name)

    val formattedAmount = try {
        val parsed = subscription.amount.toDoubleOrNull() ?: 0.0
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(parsed)
    } catch (e: Exception) {
        subscription.amount
    }

    val itemColor = if (isIncome) ColorIncome else Color(0xFFE53935)
    val iconColor = if (isIncome) ColorIncome else categoryColor

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Row 1: Icon, Title & Badge, Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = subscription.name,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = subscription.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = iconColor.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = if (isIncome) "Income" else "Subscription",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = iconColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isIncome) "+$$formattedAmount" else "-$$formattedAmount",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.1.sp
                        ),
                        color = itemColor
                    )
                    Text(
                        text = "/ month",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                }
            }

            // Divider separating header from description
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                thickness = 0.8.dp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Description on its own dedicated line + Edit action button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(15.dp)
                    )
                    val scheduleText = subscription.date?.let { day ->
                        (if (isIncome) "Credited on " else "Billing on ") + "${DayOfMonth.withSuffix(day)} of every month"
                    } ?: if (isIncome) "Recurring monthly credit" else "Recurring monthly auto-pay"

                    Text(
                        text = scheduleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onEditClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Default.Inbox,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(EmeraldPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = EmeraldPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
            if (buttonText != null && onButtonClick != null) {
                Spacer(modifier = Modifier.height(14.dp))
                FilledTonalButton(
                    onClick = onButtonClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = EmeraldPrimary.copy(alpha = 0.12f),
                        contentColor = EmeraldPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(buttonText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private val previewBudgets = listOf(
    Budget(id = "1", categoryId = "101", categoryName = "Salary", amount = 5000.0, date = 5),
    Budget(id = "2", categoryId = "102", categoryName = "Freelancing", amount = 1200.0, date = 15)
)

private val previewCategories = listOf(
    CategoryResponseDto(
        id = "2390148132-234891",
        name = "Salary",
        type = "INCOME"
    ),
    CategoryResponseDto(
        id = "239013246132-234891",
        name = "Drinks",
        type = "OUTCOME"
    )
)

private val previewExpense = listOf(
    Expense(id = "e1", name = "Parents Support", amount = 40000.0, date = 5, categoryId = null, tenure = null, paid = 0),
    Expense(id = "e2", name = "Rent", amount = 15000.0, date = 5, categoryId = null, tenure = null, paid = 0),
    Expense(id = "e3", name = "Car Loan", amount = 15000.0, date = 5, categoryId = null, tenure = 8, paid = 3)
)

@Preview(
    showBackground = true,
    showSystemUi = true,
    name = "Cashflow Screen"
)
@Composable
fun CashflowPreview() {

    ExpentTheme {

        Surface(
            modifier = Modifier.fillMaxSize()
        ) {

            CashflowScreen(
                income = previewBudgets,
                categories = previewCategories,
                expense = previewExpense,
                onBack = {}
            )
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    name = "Incoming Flow Screen"
)
@Composable
fun IncomingFlowPreview() {

    ExpentTheme {

        Surface(
            modifier = Modifier.fillMaxSize()
        ) {

            CashflowContentScreen(
                title = "Incoming",
                income = previewBudgets,
                expense = previewExpense,
                themeColor = Color(0xFF00ACC1),
                onBack = {}
            )
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    name = "Outgoing Flow Screen"
)
@Composable
fun OutgoingFlowPreview() {

    ExpentTheme {

        Surface(
            modifier = Modifier.fillMaxSize()
        ) {

            CashflowContentScreen(
                title = "Outgoing",
                income = previewBudgets,
                expense = previewExpense,
                themeColor = ColorExpense,
                onBack = {}
            )
        }
    }
}
// 1500.0 → "1500", 12.5 → "12.5"
private fun Double.toPlainAmount(): String =
    java.math.BigDecimal(toString()).stripTrailingZeros().toPlainString()
