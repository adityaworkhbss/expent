package com.aditya.expent.presentation.onboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aditya.expent.presentation.component.ExpentDatePicker
import com.aditya.expent.domain.model.OnboardCategory
import com.aditya.expent.domain.model.OnboardPaymentMode
import com.aditya.expent.R
import com.aditya.expent.presentation.theme.ExpentTheme
import com.airbnb.lottie.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardRoute(
    viewModel: OnboardViewModel,
    onFinish: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    val isForward = targetState.ordinal > initialState.ordinal
                    if (isForward) {
                        (slideInHorizontally(animationSpec = tween(400)) { width -> width } + fadeIn(animationSpec = tween(400))).togetherWith(
                            slideOutHorizontally(animationSpec = tween(400)) { width -> -width } + fadeOut(animationSpec = tween(400))
                        )
                    } else {
                        (slideInHorizontally(animationSpec = tween(400)) { width -> -width } + fadeIn(animationSpec = tween(400))).togetherWith(
                            slideOutHorizontally(animationSpec = tween(400)) { width -> width } + fadeOut(animationSpec = tween(400))
                        )
                    }
                },
                label = "onboarding_step"
            ) { step ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when (step) {
                        OnboardStep.WELCOME -> WelcomeStep(onNext = { viewModel.nextStep() })
                        OnboardStep.CATEGORIES -> {
                            LaunchedEffect(Unit) {
                                viewModel.loadCategories()
                            }
                            CategoriesStep(
                                availableCategories = state.availableCategories,
                                selectedCategories = state.selectedCategories,
                                onCategoriesChange = { viewModel.onCategoriesSelected(it) },
                                onNext = { viewModel.nextStep() },
                                isLoading = state.isLoading
                            )
                        }
                        OnboardStep.PAYMENT_MODES -> PaymentModesStep(
                            paymentModes = state.paymentModes,
                            onPaymentModesChange = { viewModel.onPaymentModesChanged(it) },
                            onNext = { viewModel.nextStep() },
                            isLoading = state.isLoading
                        )
                        OnboardStep.INCOMING -> IncomingStep(
                            salary = state.salary,
                            customIncomes = state.customIncomes,
                            selectedCategories = state.selectedCategories,
                            onSalaryAmountChange = { viewModel.onSalaryAmountChanged(it) },
                            onSalaryCategoryChange = { viewModel.onSalaryCategoryChanged(it) },
                            onSalaryPeriodChange = { viewModel.onSalaryPeriodChanged(it) },
                            onSalaryStartDateChange = { viewModel.onSalaryStartDateChanged(it) },
                            onSalaryEndDateChange = { viewModel.onSalaryEndDateChanged(it) },
                            onCustomIncomesChange = { viewModel.onCustomIncomesChanged(it) },
                            onNext = { viewModel.nextStep() }
                        )
                        OnboardStep.OUTGOING -> OutgoingStep(
                            creditCardBill = state.creditCardBill,
                            nextMonthPendingPayment = state.nextMonthPendingPayment,
                            recurringExpenses = state.recurringExpenses,
                            subscriptions = state.subscriptions,
                            onCreditCardBillChange = { viewModel.onCreditCardBillChanged(it) },
                            onNextMonthPendingPaymentChange = { viewModel.onNextMonthPendingPaymentChanged(it) },
                            onRecurringExpensesChange = { viewModel.onRecurringExpensesChanged(it) },
                            onSubscriptionsChange = { viewModel.onSubscriptionsChanged(it) },
                            onNext = { viewModel.nextStep() }
                        )
                        OnboardStep.FINISH -> FinishStep(onFinish = onFinish)
                    }

                    if (state.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f))
                                .clickable(enabled = false) { },
                            contentAlignment = Alignment.Center
                        ) {
                            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.sandy_loading))
                            val progress by animateLottieCompositionAsState(
                                composition,
                                iterations = LottieConstants.IterateForever
                            )
                            LottieAnimation(
                                composition = composition,
                                progress = { progress },
                                modifier = Modifier.size(200.dp)
                            )
                        }
                    }
                }
            }

            state.error?.let { error ->
                LaunchedEffect(error) {

                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp, start = 24.dp, end = 24.dp)
                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = CircleShape
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome to Expent",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Let's set up your profile to give you the best financial tracking experience.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Get Started", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun CategoriesStep(
    availableCategories: List<OnboardCategory>,
    selectedCategories: List<OnboardCategory>,
    onCategoriesChange: (List<OnboardCategory>) -> Unit,
    onNext: () -> Unit,
    isLoading: Boolean = false
) {
    val customCategories = remember { mutableStateListOf<OnboardCategory>() }

    val allCategories = availableCategories + customCategories
    var newCategoryText by remember { mutableStateOf("") }
    var newCategoryType by remember { mutableStateOf("EXPENSE") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Categories Setup",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Select or add categories for your income and expenses.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(allCategories) { category ->
                val isSelected = selectedCategories.any { it.name == category.name && it.type == category.type }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            val newList = if (isSelected) {
                                selectedCategories.filterNot { it.name == category.name && it.type == category.type }
                            } else {
                                selectedCategories + category
                            }
                            onCategoriesChange(newList)
                        }
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = when (category.type) {
                                "INCOME" -> if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color(0xFFE8F5E9)
                                else -> if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color(0xFFFFEBEE)
                            },
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = category.type,
                                style = MaterialTheme.typography.labelSmall,
                                color = when (category.type) {
                                    "INCOME" -> if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF2E7D32)
                                    else -> if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFC62828)
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = newCategoryText,
                    onValueChange = { newCategoryText = it },
                    placeholder = { Text("Custom category name") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val newCatName = newCategoryText.trim()
                        if (newCatName.isNotBlank() && allCategories.none { it.name == newCatName && it.type == newCategoryType }) {
                            val newCat = OnboardCategory(newCatName, newCategoryType)
                            customCategories.add(newCat)
                            onCategoriesChange(selectedCategories + newCat)
                            newCategoryText = ""
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        Icons.Default.Add, 
                        contentDescription = "Add", 
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("EXPENSE", "INCOME").forEach { type ->
                    val isTypeSelected = newCategoryType == type
                    FilterChip(
                        selected = isTypeSelected,
                        onClick = { newCategoryType = type },
                        label = { Text(type) },
                        modifier = Modifier.weight(1f),
                        leadingIcon = if (isTypeSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = selectedCategories.isNotEmpty() && !isLoading
        ) {
            Text("Continue", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun PaymentModesStep(
    paymentModes: List<OnboardPaymentMode>,
    onPaymentModesChange: (List<OnboardPaymentMode>) -> Unit,
    onNext: () -> Unit,
    isLoading: Boolean = false
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("PAY_NOW") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Payment Methods",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add your bank accounts, credit cards, or wallets.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            paymentModes.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mode.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (mode.type == "PAY_NOW") "Debit / Wallet" else "Credit / Pay Later",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        onPaymentModesChange(paymentModes.filter { it != mode })
                    }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Added",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Add new payment mode form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        RoundedCornerShape(16.dp)
                    )
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Add New Method",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    placeholder = { Text("e.g. HDFC Bank, Amex Card") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("PAY_NOW", "PAY_LATER").forEach { type ->
                        val isSelected = selectedType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { selectedType = type }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (type == "PAY_NOW") "Pay Now" else "Pay Later",
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onPaymentModesChange(paymentModes + OnboardPaymentMode(name.trim(), selectedType))
                            name = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = name.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Method")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = paymentModes.isNotEmpty() && !isLoading
        ) {
            Text("Continue", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun IncomingStep(
    salary: RecurringIncome,
    customIncomes: List<RecurringIncome>,
    selectedCategories: List<OnboardCategory>,
    onSalaryAmountChange: (String) -> Unit,
    onSalaryCategoryChange: (String?) -> Unit,
    onSalaryPeriodChange: (String) -> Unit,
    onSalaryStartDateChange: (String) -> Unit,
    onSalaryEndDateChange: (String) -> Unit,
    onCustomIncomesChange: (List<RecurringIncome>) -> Unit,
    onNext: () -> Unit
) {
    var newIncomeName by remember { mutableStateOf("") }
    var newIncomeAmount by remember { mutableStateOf("") }
    var newIncomeCategory by remember { mutableStateOf<String?>(null) }
    var newIncomePeriod by remember { mutableStateOf("MONTHLY") }
    var newIncomeStartDate by remember { mutableStateOf("") }
    
    var showSalaryStartDatePicker by remember { mutableStateOf(false) }
    var showSalaryEndDatePicker by remember { mutableStateOf(false) }
    var showNewIncomeDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Incoming Finances",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Let's set your baseline so we can track your overall wealth.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = salary.amount,
            onValueChange = onSalaryAmountChange,
            label = { Text("Monthly Salary Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            prefix = { Text("$ ") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("WEEKLY", "MONTHLY").forEach { period ->
                FilterChip(
                    selected = salary.periodType == period,
                    onClick = { onSalaryPeriodChange(period) },
                    label = { Text(period.lowercase().replaceFirstChar { it.uppercase() }) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Salary Category", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            val incomeCategories = selectedCategories.filter { it.type == "INCOME" }
            for (category in incomeCategories) {
                FilterChip(
                    selected = salary.categoryId == category.name,
                    onClick = { onSalaryCategoryChange(category.name) },
                    label = { Text(category.name) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = salary.startDate,
            onValueChange = { },
            label = { Text("Start Date") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                IconButton(onClick = { showSalaryStartDatePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                }
            }
        )

        ExpentDatePicker(
            showDialog = showSalaryStartDatePicker,
            onDismiss = { showSalaryStartDatePicker = false },
            onDateSelected = {
                onSalaryStartDateChange(it)
                showSalaryStartDatePicker = false
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Additional Incomes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        customIncomes.forEachIndexed { _, customIncome ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customIncome.name, 
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${customIncome.periodType.lowercase().capitalize()} | Category: ${customIncome.categoryId ?: "General"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = "$ ${customIncome.amount}", 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        
        if (customIncomes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = newIncomeName,
                    onValueChange = { newIncomeName = it },
                    placeholder = { Text("Income Name") },
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = newIncomeAmount,
                    onValueChange = { newIncomeAmount = it },
                    placeholder = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("WEEKLY", "MONTHLY").forEach { period ->
                    FilterChip(
                        selected = newIncomePeriod == period,
                        onClick = { newIncomePeriod = period },
                        label = { Text(period.lowercase().capitalize()) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Category", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val incomeCategories = selectedCategories.filter { it.type == "INCOME" }
                for (category in incomeCategories) {
                    FilterChip(
                        selected = newIncomeCategory == category.name,
                        onClick = { newIncomeCategory = category.name },
                        label = { Text(category.name) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = newIncomeStartDate,
                onValueChange = { },
                placeholder = { Text("Start Date") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = { showNewIncomeDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                }
            )

            ExpentDatePicker(
                showDialog = showNewIncomeDatePicker,
                onDismiss = { showNewIncomeDatePicker = false },
                onDateSelected = {
                    newIncomeStartDate = it
                    showNewIncomeDatePicker = false
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (newIncomeName.isNotBlank() && newIncomeAmount.isNotBlank() && newIncomeStartDate.isNotBlank()) {
                        onCustomIncomesChange(customIncomes + RecurringIncome(
                            name = newIncomeName.trim(),
                            amount = newIncomeAmount.trim(),
                            periodType = newIncomePeriod,
                            startDate = newIncomeStartDate,
                            categoryId = newIncomeCategory
                        ))
                        newIncomeName = ""
                        newIncomeAmount = ""
                        newIncomeStartDate = ""
                        newIncomeCategory = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Income Source")
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = salary.amount.isNotBlank() && salary.startDate.isNotBlank()
        ) {
            Text("Continue", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutgoingStep(
    creditCardBill: String,
    nextMonthPendingPayment: String,
    recurringExpenses: List<RecurringExpense>,
    subscriptions: List<Subscription>,
    onCreditCardBillChange: (String) -> Unit,
    onNextMonthPendingPaymentChange: (String) -> Unit,
    onRecurringExpensesChange: (List<RecurringExpense>) -> Unit,
    onSubscriptionsChange: (List<Subscription>) -> Unit,
    onNext: () -> Unit
) {
    // Local states for forms
    var emiName by remember { mutableStateOf("") }
    var emiAmount by remember { mutableStateOf("") }
    var emiTotalMonths by remember { mutableStateOf("") }
    var emiMonthsPaid by remember { mutableStateOf("") }
    var emiStartDate by remember { mutableStateOf("") }
    var showEmiDatePicker by remember { mutableStateOf(false) }

    var subName by remember { mutableStateOf("") }
    var subAmount by remember { mutableStateOf("") }
    var subDate by remember { mutableStateOf("") }
    var showSubDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Outgoing Expenses",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Do you have any recurring deductions or outstanding bills?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))

//        OutlinedTextField(
//            value = nextMonthPendingPayment,
//            onValueChange = onNextMonthPendingPaymentChange,
//            label = { Text("Next Month Pending Payment (Optional)") },
//            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//            modifier = Modifier.fillMaxWidth(),
//            shape = RoundedCornerShape(12.dp),
//            prefix = { Text("$ ") }
//        )

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Recurring EMIs",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        recurringExpenses.forEach { emi ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(emi.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text("$ ${emi.amount}/mo", color = MaterialTheme.colorScheme.primary)
                }
                Text("Paid ${emi.monthsPaid} out of ${emi.totalMonths} months | Start: ${emi.startDate}", style = MaterialTheme.typography.bodySmall)
            }
        }

        // Add EMI Form
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = emiName,
                    onValueChange = { emiName = it },
                    placeholder = { Text("EMI Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = emiAmount,
                    onValueChange = { emiAmount = it },
                    placeholder = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = emiTotalMonths,
                    onValueChange = { emiTotalMonths = it },
                    placeholder = { Text("Total Months") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = emiMonthsPaid,
                    onValueChange = { emiMonthsPaid = it },
                    placeholder = { Text("Months Paid") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = emiStartDate,
                onValueChange = { },
                placeholder = { Text("Start Date") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showEmiDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                }
            )
            
            ExpentDatePicker(
                showDialog = showEmiDatePicker,
                onDismiss = { showEmiDatePicker = false },
                onDateSelected = {
                    emiStartDate = it
                    showEmiDatePicker = false
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (emiName.isNotBlank() && emiAmount.isNotBlank() && emiTotalMonths.isNotBlank() && emiMonthsPaid.isNotBlank() && emiStartDate.isNotBlank()) {
                        onRecurringExpensesChange(recurringExpenses + RecurringExpense(emiName.trim(), emiAmount.trim(), emiTotalMonths.trim(), emiMonthsPaid.trim(), emiStartDate.trim()))
                        emiName = ""
                        emiAmount = ""
                        emiTotalMonths = ""
                        emiMonthsPaid = ""
                        emiStartDate = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add EMI")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Subscriptions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        subscriptions.forEach { sub ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(sub.name, fontWeight = FontWeight.Bold)
                    Text("Bills on ${sub.billingDate} every month", style = MaterialTheme.typography.bodySmall)
                }
                Text("$ ${sub.amount}/mo", color = MaterialTheme.colorScheme.primary)
            }
        }

        // Add Sub Form
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = subName,
                    onValueChange = { subName = it },
                    placeholder = { Text("Sub Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    OutlinedTextField(
                        value = subAmount,
                        onValueChange = { subAmount = it },
                        placeholder = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = subDate,
                        onValueChange = { },
                        placeholder = { Text("Date") },
                        readOnly = true,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { showSubDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                            }
                        }
                    )
                }
            }
            
            ExpentDatePicker(
                showDialog = showSubDatePicker,
                onDismiss = { showSubDatePicker = false },
                onDateSelected = {
                    subDate = it
                    showSubDatePicker = false
                }
            )

            Spacer(modifier = Modifier.width(12.dp))
            IconButton(
                onClick = {
                    if (subName.isNotBlank() && subAmount.isNotBlank() && subDate.isNotBlank()) {
                        onSubscriptionsChange(subscriptions + Subscription(subName.trim(), subAmount.trim(), subDate.trim()))
                        subName = ""
                        subAmount = ""
                        subDate = ""
                    }
                },
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Subscription", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Complete Setup", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun FinishStep(onFinish: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(scale)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedVisibility(
            visible = startAnimation,
            enter = slideInVertically(
                initialOffsetY = { 50 },
                animationSpec = tween(500, delayMillis = 300)
            ) + fadeIn(animationSpec = tween(500, delayMillis = 300))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "You're all set!",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Expent is ready to help you master your finances.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        AnimatedVisibility(
            visible = startAnimation,
            enter = slideInVertically(
                initialOffsetY = { 50 },
                animationSpec = tween(500, delayMillis = 600)
            ) + fadeIn(animationSpec = tween(500, delayMillis = 600))
        ) {
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Go to Dashboard", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeStepPreview() {
    ExpentTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            WelcomeStep(onNext = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CategoriesStepPreview() {
    ExpentTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            CategoriesStep(
                availableCategories = listOf(
                    OnboardCategory("Food", "EXPENSE"),
                    OnboardCategory("Transport", "EXPENSE"),
                    OnboardCategory("Salary", "INCOME")
                ),
                selectedCategories = listOf(
                    OnboardCategory("Food", "EXPENSE")
                ),
                onCategoriesChange = {},
                onNext = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PaymentModesStepPreview() {
    ExpentTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            PaymentModesStep(
                paymentModes = listOf(
                    OnboardPaymentMode("HDFC Bank", "PAY_NOW"),
                    OnboardPaymentMode("Amex Card", "PAY_LATER")
                ),
                onPaymentModesChange = {},
                onNext = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun IncomingStepPreview() {
    ExpentTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            IncomingStep(
                salary = RecurringIncome(name = "Salary", amount = "5000", startDate = "01/01/2023"),
                customIncomes = listOf(
                    RecurringIncome(name = "Freelance", amount = "1500", startDate = "01/01/2023")
                ),
                selectedCategories = listOf(OnboardCategory("Food", "EXPENSE")),
                onSalaryAmountChange = {},
                onSalaryCategoryChange = {},
                onSalaryPeriodChange = {},
                onSalaryStartDateChange = {},
                onSalaryEndDateChange = {},
                onCustomIncomesChange = {},
                onNext = {}
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun OutgoingStepPreview() {
    ExpentTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            OutgoingStep(
                creditCardBill = "200",
                nextMonthPendingPayment = "150",
                recurringExpenses = listOf(RecurringExpense("Car Loan", "300", "48", "12", "01/01/2023")),
                subscriptions = listOf(Subscription("Netflix", "15", "12th")),
                onCreditCardBillChange = {},
                onNextMonthPendingPaymentChange = {},
                onRecurringExpensesChange = {},
                onSubscriptionsChange = {},
                onNext = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FinishStepPreview() {
    ExpentTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            FinishStep(onFinish = {})
        }
    }
}
