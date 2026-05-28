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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.aditya.expent.data.remote.dto.BudgetCategoryDto
import com.aditya.expent.data.remote.dto.CategoryResponseDto
import com.aditya.expent.data.remote.dto.BudgetResponseDto
import com.aditya.expent.presentation.onboard.RecurringExpense
import com.aditya.expent.presentation.onboard.Subscription
import com.aditya.expent.presentation.theme.ColorExpense
import com.aditya.expent.presentation.theme.ExpentTheme
import com.aditya.expent.utils.AppUtils
import com.aditya.expent.presentation.component.ExpentDatePicker
import androidx.compose.material.icons.filled.DateRange
import com.aditya.expent.data.remote.dto.AccountDto
import com.aditya.expent.data.remote.dto.ExpenseIncomeResponseDto
import dagger.hilt.android.AndroidEntryPoint
import java.time.OffsetDateTime
import kotlin.getValue
import kotlin.math.exp
import kotlin.time.Clock.System.now


enum class CashflowTab {
    INCOMING,
    OUTGOING
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
                        state.budgets,
                        state.categories,
                        state.expense,
                        onBack = {
                            finish()
                        },
                        onDeleteBudget = { id -> viewModel.deleteBudget(id) },
                        onUpdateBudget = { id, categoryId, periodType, amount, startDate, endDate ->
                            viewModel.updateBudget(id, categoryId, periodType, amount, startDate, endDate)
                        },
                        onAddBudget = { categoryId, periodType, amount, startDate, endDate ->
                            viewModel.saveBudget(categoryId, periodType, amount, startDate, endDate)
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
    onAddBudget: (String?, String, Double, String, String?) -> Unit = { _, _, _, _, _ -> }
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
            categories = categories.filter { it.type == categoriesType },
            themeColor = themeColor,
            onBack = onBack,
            onDeleteBudget = onDeleteBudget,
            onUpdateBudget = onUpdateBudget,
            onAddBudget = onAddBudget
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
    onAddBudget: (String?, String, Double, String, String?) -> Unit = { _, _, _, _, _ -> }
) {


    val recurringExpenses: List<RecurringExpense> = expense.mapNotNull { item ->

        if (!item.endDate.isNullOrBlank()) {

            RecurringExpense(
                name = item.name,
                amount = item.monthlyEmi,
                totalMonths = item.tenure.toString(),
                monthsPaid = item.monthsPaid.toString(),
                startDate = AppUtils().getDayWithSuffix(item.startDate),
            )

        } else {
            null
        }
    }

    val subscriptions: List<Subscription> = expense.mapNotNull { item ->

        if (item.endDate.isNullOrBlank()) {

            Subscription(
                name = item.name,
                amount = item.principal,
                billingDate = item.startDate,
                id = item.id
            )

        } else {
            null
        }
    }

    val incomes = income.map { it ->
        Subscription(
            name = it.category?.name.toString(),
            amount = it.limitAmount,
            billingDate = it.startDate,
            id = it.id
        )
    }

    val activeList = if (title.contentEquals("Incoming")) incomes else subscriptions

    var selectedSubscriptionForEdit by remember { mutableStateOf<Subscription?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

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
                        onAddClick = {}
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                }

                items(recurringExpenses.size) { index ->
                    val emi = recurringExpenses[index]
                    val paid = emi.monthsPaid.toFloatOrNull() ?: 0f
                    val total = emi.totalMonths.toFloatOrNull() ?: 1f
                    val progress = (paid / total).coerceIn(0f, 1f)

                    EnhancedEmiCard(
                        emi = emi,
                        progress = progress,
                        themeColor = themeColor
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))

                SectionHeader(
                    title = if (title.contentEquals("Incoming")) "Active Incomes" else "Active Subscriptions",
                    themeColor = themeColor,
                    onAddClick = {
                        selectedSubscriptionForEdit = null
                        showBottomSheet = true
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))
            }

            items(activeList.size) { index ->
                val sub = activeList[index]
                EnhancedSubscriptionCard(
                    subscription = sub,
                    themeColor = themeColor,
                    onEditClick = {
                        selectedSubscriptionForEdit = sub
                        showBottomSheet = true
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (showBottomSheet) {
            var subscription: Subscription? = null
            if (selectedSubscriptionForEdit != null) subscription = selectedSubscriptionForEdit!!

            val editSheetState = rememberModalBottomSheetState()
            val addSheetState = rememberModalBottomSheetState()

            var amountText by remember { mutableStateOf(subscription?.amount ?: "") }
            var selectedCategoryId by remember {
                mutableStateOf(
                    if (subscription != null) {
                        val origBudget = income.find { it.id == subscription.id }
                        origBudget?.categoryId
                    } else null
                )
            }
            var selectedCategoryName by remember {
                mutableStateOf(subscription?.name ?: "")
            }
            var dateText by remember { mutableStateOf(subscription?.billingDate ?: "") }
            var showDatePicker by remember { mutableStateOf(false) }
            var categoryDropdownExpanded by remember { mutableStateOf(false) }

            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = if (subscription != null)
                                editSheetState
                            else
                                addSheetState,
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
                        text = if (subscription != null) {
                                    "Edit Active Flow"
                                } else {
                                    "Add new incoming flow"
                                },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategoryName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Category",
                                    tint = themeColor
                                )
                            }
                        )

                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        selectedCategoryId = category.id
                                        selectedCategoryName = category.name
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount ($)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = AppUtils().formatIsoDate(dateText),
                        onValueChange = { },
                        label = { Text("Billing Date / Start Date") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Select Date", tint = themeColor)
                            }
                        }
                    )

                    ExpentDatePicker(
                        showDialog = showDatePicker,
                        onDismiss = { showDatePicker = false },
                        onDateSelected = {
                            dateText = it
                            showDatePicker = false
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if(subscription?.id != null){
                        Button(
                            onClick = {
                                val budgetId = subscription?.id
                                if (budgetId != null) {
                                    val doubleAmount = amountText.toDoubleOrNull() ?: 0.0
                                    val origBudget = income.find { it.id == budgetId }
                                    val startDate = if (dateText.isBlank()) origBudget?.startDate ?: now().toString() else dateText
                                    onUpdateBudget(
                                        budgetId,
                                        selectedCategoryId,
                                        origBudget?.periodType ?: "MONTHLY",
                                        doubleAmount,
                                        startDate,
                                        origBudget?.endDate
                                    )
                                }
                                showBottomSheet = false
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

                    }

                    if (subscription?.id != null) {
                        TextButton(
                            onClick = {
                                onDeleteBudget(subscription.id)
                                showBottomSheet = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = ColorExpense)
                        ) {
                            Text("Delete Flow", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        TextButton(
                            onClick = {
                                val doubleAmount = amountText.toDoubleOrNull() ?: 0.0
                                val startDate = if (dateText.isBlank()) OffsetDateTime.now().toString() else dateText
                                onAddBudget(
                                    selectedCategoryId,
                                    "MONTHLY",
                                    doubleAmount,
                                    startDate,
                                    null
                                )
                                showBottomSheet = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF00ACC1))
                        ) {
                            Text("Add Flow", fontWeight = FontWeight.Bold)
                        }
                    }
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
    themeColor: Color
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
                            .background(themeColor.copy(alpha = 0.1f)),
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
                        text = "Started",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 0.3.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = emi.startDate,
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

private val previewBudgets = listOf(
    BudgetResponseDto(
        id = "1",
        userId = "1278t301",
        limitAmount = "5000",
        startDate = "2026-01-05T00:00:00.000Z",
        periodType = "MONTHLY",
        categoryId = "943287908312",
        category = BudgetCategoryDto(
            id = "101",
            name = "Salary"
        )
    ),
    BudgetResponseDto(
        id = "2",
        userId = "12390-121",
        limitAmount = "1200",
        startDate = "2026-01-15T00:00:00.000Z",
        periodType = "MONTHLY",
        categoryId = "943287908312",
        category = BudgetCategoryDto(
            id = "102",
            name = "Freelancing",

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