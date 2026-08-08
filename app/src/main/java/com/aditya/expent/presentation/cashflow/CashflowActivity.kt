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
import com.aditya.expent.data.remote.dto.BudgetResponseDto
import com.aditya.expent.presentation.onboard.RecurringExpense
import com.aditya.expent.presentation.onboard.Subscription
import com.aditya.expent.presentation.theme.ColorExpense
import com.aditya.expent.presentation.theme.ExpentTheme
import com.aditya.expent.utils.AppUtils
import com.aditya.expent.presentation.component.ExpentDatePicker
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import com.aditya.expent.utils.CategoryUtils
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.aditya.expent.data.remote.dto.ExpenseIncomeResponseDto
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
                        onUpdateBudget = { id, categoryId, periodType, amount, startDate, endDate ->
                            viewModel.updateBudget(id, categoryId, periodType, amount, startDate, endDate)
                        },
                        onAddBudget = { categoryId, periodType, amount, startDate, endDate ->
                            viewModel.saveBudget(categoryId, periodType, amount, startDate, endDate)
                        },
                        onDeleteEmi = { id -> viewModel.deleteEmi(id) },
                        onUpdateEmi = { id, type, name, amount, startDate, tenure, monthsPaid ->
                            viewModel.updateEmi(id, type, name, amount, startDate, tenure, monthsPaid)
                        },
                        onAddEmi = { type, name, amount, startDate, tenure, monthsPaid ->
                            viewModel.saveEmi(type, name, amount, startDate, tenure, monthsPaid)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CashflowScreen(
    income : List<BudgetResponseDto>,
    categories: List<CategoryResponseDto>,
    expense : List<ExpenseIncomeResponseDto>,
    onBack : () -> Unit,
    onDeleteBudget: (String) -> Unit = {},
    onUpdateBudget: (String, String?, String, Double, String, String?) -> Unit = { _, _, _, _, _, _ -> },
    onAddBudget: (String?, String, Double, String, String?) -> Unit = { _, _, _, _, _ -> },
    onDeleteEmi: (String) -> Unit = {},
    onUpdateEmi: (String, String, String, String, String, String?, String?) -> Unit = { _, _, _, _, _, _, _ -> },
    onAddEmi: (String, String, String, String, String?, String?) -> Unit = { _, _, _, _, _, _ -> }
) {

    var selectedTab by remember {
        mutableStateOf(CashflowTab.INCOMING)
    }

    val themeColor = when (selectedTab) {
        CashflowTab.INCOMING -> Color(0xFF00ACC1)
        CashflowTab.OUTGOING -> ColorExpense
    }

    val categoriesType = when (selectedTab) {
        CashflowTab.INCOMING -> "INCOME"
        CashflowTab.OUTGOING -> "EXPENSE"
    }

    Scaffold(
        bottomBar = {

            NavigationBar {

                NavigationBarItem(
                    selected = selectedTab == CashflowTab.INCOMING,
                    onClick = {
                        selectedTab = CashflowTab.INCOMING
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Incoming"
                        )
                    },
                    label = {
                        Text("Incoming")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == CashflowTab.OUTGOING,
                    onClick = {
                        selectedTab = CashflowTab.OUTGOING
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Outgoing"
                        )
                    },
                    label = {
                        Text("Outgoing")
                    }
                )
            }
        }
    ) { paddingValues ->
        val title = if (selectedTab == CashflowTab.INCOMING) "Incoming" else "Outgoing"
        CashflowContentScreen(
            modifier = Modifier.padding(paddingValues),
            title = title,
            income = income,
            expense = expense,
            categories = categories.filter { it.type.equals(categoriesType, ignoreCase = true) },
            themeColor = themeColor,
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
    income: List<BudgetResponseDto>,
    expense: List<ExpenseIncomeResponseDto>,
    categories: List<CategoryResponseDto> = emptyList(),
    themeColor: Color,
    onBack: () -> Unit,
    onDeleteBudget: (String) -> Unit = {},
    onUpdateBudget: (String, String?, String, Double, String, String?) -> Unit = { _, _, _, _, _, _ -> },
    onAddBudget: (String?, String, Double, String, String?) -> Unit = { _, _, _, _, _ -> },
    onDeleteEmi: (String) -> Unit = {},
    onUpdateEmi: (String, String, String, String, String, String?, String?) -> Unit = { _, _, _, _, _, _, _ -> },
    onAddEmi: (String, String, String, String, String?, String?) -> Unit = { _, _, _, _, _, _ -> }
) {


    val recurringExpenses: List<RecurringExpense> = expense.mapNotNull { item ->
        if (!item.endDate.isNullOrBlank()) {
            RecurringExpense(
                name = item.resolvedName,
                amount = item.resolvedAmount,
                totalMonths = (item.tenure ?: 0).toString(),
                monthsPaid = (item.monthsPaid ?: 0).toString(),
                startDate = item.resolvedStartDate,
                id = item.id,
                endDate = item.endDate
            )
        } else {
            null
        }
    }

    val subscriptions: List<Subscription> = expense.mapNotNull { item ->
        if (item.endDate.isNullOrBlank()) {
            Subscription(
                name = item.resolvedName,
                amount = item.principal ?: item.resolvedAmount,
                billingDate = item.resolvedStartDate,
                id = item.id
            )
        } else {
            null
        }
    }

    val incomes = income.map { item ->
        Subscription(
            name = item.category?.name.toString(),
            amount = item.limitAmount.toString(),
            billingDate = item.startDate ?: "",
            id = item.id
        )
    }

    val activeList = if (title.contentEquals("Incoming")) incomes else subscriptions

    var sheetMode by remember {
        mutableStateOf<CashflowBottomSheetMode?>(null)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.98f)
                        )
                    )
                ),
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp
            )
        ) {

            item {
                HeaderSection(title, onBack)
            }

            if (title.contentEquals("Outgoing")) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    SectionHeader(
                        title = "Recurring EMIs & Loans",
                        themeColor = themeColor,
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
                            subtitle = "You haven't added any recurring EMIs or loans yet. Tap '+' above to add one.",
                            icon = Icons.Default.CreditCard
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
                            themeColor = themeColor,
                            onEditClick = {
                                sheetMode = CashflowBottomSheetMode.EditEmi(emi)
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))

                SectionHeader(
                    title = if (title.contentEquals("Incoming")) "Active Incomes" else "Active Subscriptions",
                    themeColor = themeColor,
                    onAddClick = {
                        sheetMode = if (title == "Incoming") CashflowBottomSheetMode.AddIncome else CashflowBottomSheetMode.AddSubscription
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))
            }

            if (activeList.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = if (title.contentEquals("Incoming")) "No Active Incomes" else "No Active Subscriptions",
                        subtitle = if (title.contentEquals("Incoming")) "You haven't added any income sources yet. Tap '+' above to add one." else "You haven't added any recurring subscriptions yet. Tap '+' above to add one.",
                        icon = if (title.contentEquals("Incoming")) Icons.AutoMirrored.Filled.TrendingUp else Icons.Default.Subscriptions
                    )
                }
            } else {
                items(activeList.size) { index ->
                    val sub = activeList[index]
                    EnhancedSubscriptionCard(
                        subscription = sub,
                        themeColor = themeColor,
                        onEditClick = {
                            sheetMode = if (title == "Incoming") {
                                CashflowBottomSheetMode.EditIncome(sub)
                            } else {
                                CashflowBottomSheetMode.EditSubscription(sub)
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        sheetMode?.let { mode ->
            CashflowBottomSheet(
                mode = mode,
                categories = categories,
                incomeList = income,
                themeColor = themeColor,
                onDismiss = { sheetMode = null },
                onSaveBudget = { id, categoryId, amount, startDate, endDate ->
                    if (id != null) {
                        onUpdateBudget(id, categoryId, "MONTHLY", amount, startDate, endDate)
                    } else {
                        onAddBudget(categoryId, "MONTHLY", amount, startDate, endDate)
                    }
                },
                onDeleteBudget = onDeleteBudget,
                onSaveEmi = { id, typeStr, name, amount, startDate, tenure, monthsPaid ->
                    if (id != null) {
                        onUpdateEmi(id, typeStr, name, amount, startDate, tenure, monthsPaid)
                    } else {
                        onAddEmi(typeStr, name, amount, startDate, tenure, monthsPaid)
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
    incomeList: List<BudgetResponseDto>,
    themeColor: Color,
    onDismiss: () -> Unit,
    onSaveBudget: (id: String?, categoryId: String?, amount: Double, startDate: String, endDate: String?) -> Unit,
    onDeleteBudget: (id: String) -> Unit,
    onSaveEmi: (id: String?, type: String, name: String, amount: String, startDate: String, tenure: String?, monthsPaid: String?) -> Unit,
    onDeleteEmi: (id: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    val initialName = when (mode) {
        is CashflowBottomSheetMode.EditEmi -> mode.emi.name
        else -> ""
    }
    val initialAmount = when (mode) {
        is CashflowBottomSheetMode.EditIncome -> mode.income.amount
        is CashflowBottomSheetMode.EditSubscription -> mode.subscription.amount
        is CashflowBottomSheetMode.EditEmi -> mode.emi.amount
        else -> ""
    }
    val initialStartDate = when (mode) {
        is CashflowBottomSheetMode.EditIncome -> mode.income.billingDate
        is CashflowBottomSheetMode.EditSubscription -> mode.subscription.billingDate
        is CashflowBottomSheetMode.EditEmi -> mode.emi.startDate
        else -> ""
    }
    val initialEndDate = when (mode) {
        is CashflowBottomSheetMode.EditEmi -> mode.emi.endDate ?: ""
        else -> ""
    }
    val initialCategoryId = when (mode) {
        is CashflowBottomSheetMode.EditIncome -> incomeList.find { it.id == mode.income.id }?.categoryId
        else -> null
    }
    val initialCategoryName = when (mode) {
        is CashflowBottomSheetMode.EditIncome -> mode.income.name
        is CashflowBottomSheetMode.EditSubscription -> mode.subscription.name
        else -> ""
    }
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
    var dateText by remember(mode) { mutableStateOf(initialStartDate) }
    var endDateText by remember(mode) { mutableStateOf(initialEndDate) }
    var tenureText by remember(mode) { mutableStateOf(initialTenure) }
    var monthsPaidText by remember(mode) { mutableStateOf(initialMonthsPaid) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val title = when (mode) {
        is CashflowBottomSheetMode.AddIncome -> "Add new incoming flow"
        is CashflowBottomSheetMode.EditIncome -> "Edit Active Flow"
        is CashflowBottomSheetMode.AddSubscription -> "Add new outgoing flow"
        is CashflowBottomSheetMode.EditSubscription -> "Edit Active Flow"
        is CashflowBottomSheetMode.AddEmi -> "Add new EMI/Loan"
        is CashflowBottomSheetMode.EditEmi -> "Edit EMI/Loan"
    }

    val isEdit = mode is CashflowBottomSheetMode.EditIncome ||
            mode is CashflowBottomSheetMode.EditSubscription ||
            mode is CashflowBottomSheetMode.EditEmi

    val isEmi = mode is CashflowBottomSheetMode.AddEmi || mode is CashflowBottomSheetMode.EditEmi

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
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

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Name the EMI Field (only for EMI)
            if (isEmi) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Name the EMI / Loan") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 2. Category Dropdown (only for Income and Subscription)
            if (!isEmi) {
                val arrowRotation by animateFloatAsState(
                    targetValue = if (categoryDropdownExpanded) 180f else 0f,
                    label = "ArrowRotation"
                )

                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = if (categories.isEmpty()) "No Categories Found" else if (selectedCategoryName.isBlank()) "Select Category" else selectedCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColor,
                            focusedLabelColor = themeColor
                        ),
                        leadingIcon = {
                            if (selectedCategoryName.isNotBlank() && categories.isNotEmpty()) {
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
                                tint = themeColor,
                                modifier = Modifier.graphicsLayer { rotationZ = arrowRotation }
                            )
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    ) {
                        if (categories.isEmpty()) {
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
                            categories.forEach { category ->
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
                                                        .background(iconAndColor.second.copy(alpha = 0.15f)),
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
                                                    color = if (isSelected) themeColor else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = themeColor,
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
                                            if (isSelected) themeColor.copy(alpha = 0.1f) else Color.Transparent
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
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Start Date Field
            OutlinedTextField(
                value = AppUtils().formatIsoDate(dateText),
                onValueChange = { },
                label = { Text(if (isEmi) "Start Date" else "Billing Date / Start Date") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date", tint = themeColor)
                    }
                }
            )

            // 5. Tenure and Months Paid Fields (only for EMI)
            if (isEmi) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = tenureText,
                        onValueChange = { tenureText = it },
                        label = { Text("Tenure (Months)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = monthsPaidText,
                        onValueChange = { monthsPaidText = it },
                        label = { Text("Months Paid") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 6. End Date Field
                OutlinedTextField(
                    value = AppUtils().formatIsoDate(endDateText),
                    onValueChange = { },
                    label = { Text("End Date") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = { showEndDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date", tint = themeColor)
                        }
                    }
                )
            }

            ExpentDatePicker(
                showDialog = showDatePicker,
                onDismiss = { showDatePicker = false },
                onDateSelected = {
                    dateText = it
                    showDatePicker = false
                }
            )
            ExpentDatePicker(
                showDialog = showEndDatePicker,
                onDismiss = { showEndDatePicker = false },
                onDateSelected = {
                    endDateText = it
                    showEndDatePicker = false
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons (Update / Delete / Add)
            val id = when (mode) {
                is CashflowBottomSheetMode.EditIncome -> mode.income.id
                is CashflowBottomSheetMode.EditSubscription -> mode.subscription.id
                is CashflowBottomSheetMode.EditEmi -> mode.emi.id
                else -> null
            }

            if (isEdit) {
                Button(
                    onClick = {
                        if (mode is CashflowBottomSheetMode.EditIncome) {
                            onSaveBudget(id, selectedCategoryId, amountText.toDoubleOrNull() ?: 0.0, dateText, null)
                        } else if (mode is CashflowBottomSheetMode.EditSubscription) {
                            onSaveEmi(id, "subscription", selectedCategoryName, amountText, dateText, null, null)
                        } else if (mode is CashflowBottomSheetMode.EditEmi) {
                            onSaveEmi(id, "expense", nameText, amountText, dateText, tenureText, monthsPaidText)
                        }
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                ) {
                    Text("Update", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
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
                    colors = ButtonDefaults.textButtonColors(contentColor = ColorExpense)
                ) {
                    Text("Delete Flow", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        if (mode is CashflowBottomSheetMode.AddIncome) {
                            onSaveBudget(null, selectedCategoryId, amountText.toDoubleOrNull() ?: 0.0, dateText, null)
                        } else if (mode is CashflowBottomSheetMode.AddSubscription) {
                            onSaveEmi(null, "subscription", selectedCategoryName, amountText, dateText, null, null)
                        } else if (mode is CashflowBottomSheetMode.AddEmi) {
                            onSaveEmi(null, "expense", nameText, amountText, dateText, tenureText, monthsPaidText)
                        }
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                ) {
                    Text("Add Flow", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HeaderSection(title: String, onBack: () -> Unit) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    themeColor: Color,
    onAddClick: () -> Unit
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.1.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        IconButton(
            onClick = onAddClick,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            themeColor.copy(alpha = 0.15f),
                            themeColor.copy(alpha = 0.05f)
                        )
                    )
                )
        ) {

            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = themeColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun EnhancedEmiCard(
    emi: RecurringExpense,
    progress: Float,
    themeColor: Color,
    onEditClick : () -> Unit
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
            durationMillis = 1000,
            easing = LinearEasing
        ),
        label = "progress_animation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = emi.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$${emi.amount}/mo",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.1.sp
                        ),
                        color = themeColor
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(themeColor.copy(alpha = 0.1f))
                            .clickable{
                                onEditClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = themeColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column {
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 0.3.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${emi.monthsPaid}/${emi.totalMonths} months",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Day of Month",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 0.3.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = AppUtils().getDayWithSuffix(emi.startDate),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    themeColor,
                                    themeColor.copy(alpha = 0.7f)
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${(animatedProgress * 100).toInt()}% Complete",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                ),
                color = themeColor
            )
        }
    }
}

@Composable
fun EnhancedSubscriptionCard(
    subscription: Subscription,
    themeColor: Color,
    onEditClick: () -> Unit
) {

    var isPressed by remember {
        mutableStateOf(false)
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "subscription_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = subscription.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Billing on ${AppUtils().getDayWithSuffix(subscription.billingDate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${subscription.amount}/mo",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.1.sp
                    ),
                    color = themeColor
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            themeColor.copy(alpha = 0.1f)
                        )
                        .clickable{
                            onEditClick()
                        },
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = themeColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Default.Inbox
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
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
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

private val previewBudgets = listOf(
    BudgetResponseDto(
        id = "1",
        userId = "1278t301",
        limitAmount = 5000.0,
        startDate = "2026-01-05T00:00:00.000Z",
        periodType = "MONTHLY",
        categoryId = "943287908312",
        category = CategoryResponseDto(
            id = "101",
            name = "Salary",
            type = "INCOME"
        )
    ),
    BudgetResponseDto(
        id = "2",
        userId = "12390-121",
        limitAmount = 1200.0,
        startDate = "2026-01-15T00:00:00.000Z",
        periodType = "MONTHLY",
        categoryId = "943287908312",
        category = CategoryResponseDto(
            id = "102",
            name = "Freelancing",
            type = "INCOME"
        )
    )
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
    ExpenseIncomeResponseDto(
        id = "6c5441a4-20eb-4f5e-b9df-b82ccdf12365",
        userId = "bcbe170f-801b-4092-93d0-348c27d32aef",
        accountId = null,
        transactionId = null,
        name = "Parents Support",
        principal = "0",
        tenure = 0,
        monthlyEmi = "40000",
        startDate = "2026-01-05T00:00:00.000Z",
        endDate = null,
        nextDueDate = "2026-02-05T00:00:00.000Z",
        remainingBalance = "0",
        monthsPaid = 0,
        active = true,
        createdAt = "2026-05-17T07:31:12.281Z",
        updatedAt = "2026-05-17T07:31:12.281Z",
        account = null
    ),

    ExpenseIncomeResponseDto(
        id = "0bcb8bee-261c-4a8d-9bbc-71c24811915a",
        userId = "bcbe170f-801b-4092-93d0-348c27d32aef",
        accountId = null,
        transactionId = null,
        name = "Rent",
        principal = "0",
        tenure = 0,
        monthlyEmi = "15000",
        startDate = "2026-01-05T00:00:00.000Z",
        endDate = null,
        nextDueDate = "2026-02-05T00:00:00.000Z",
        remainingBalance = "0",
        monthsPaid = 0,
        active = true,
        createdAt = "2026-05-17T07:31:12.495Z",
        updatedAt = "2026-05-17T07:31:12.495Z",
        account = null
    ),

    ExpenseIncomeResponseDto(
        id = "0bcb8bee-261c-4a8d-9bbc-71c24811915a",
        userId = "bcbe170f-801b-4092-93d0-348c27d32aef",
        accountId = null,
        transactionId = null,
        name = "Rent",
        principal = "0",
        tenure = 0,
        monthlyEmi = "15000",
        startDate = "2026-01-05T00:00:00.000Z",
        endDate = "2026-09-05T00:00:00.000Z",
        nextDueDate = "2026-02-05T00:00:00.000Z",
        remainingBalance = "0",
        monthsPaid = 0,
        active = true,
        createdAt = "2026-05-17T07:31:12.495Z",
        updatedAt = "2026-05-17T07:31:12.495Z",
        account = null
    )
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