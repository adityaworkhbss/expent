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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
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
import com.aditya.expent.presentation.theme.ExpentTheme

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
                navigationIcon = {
                    if (state.currentStep != OnboardStep.WELCOME && state.currentStep != OnboardStep.FINISH) {
                        IconButton(onClick = { viewModel.previousStep() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
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
                when (step) {
                    OnboardStep.WELCOME -> WelcomeStep(onNext = { viewModel.nextStep() })
                    OnboardStep.CATEGORIES -> CategoriesStep(
                        selectedCategories = state.selectedCategories,
                        onCategoriesChange = { viewModel.onCategoriesSelected(it) },
                        onNext = { viewModel.nextStep() }
                    )
                    OnboardStep.PAYMENT_MODES -> PaymentModesStep(
                        paymentModes = state.paymentModes,
                        onPaymentModesChange = { viewModel.onPaymentModesChanged(it) },
                        onNext = { viewModel.nextStep() }
                    )
                    OnboardStep.INCOMING -> IncomingStep(
                        salary = state.salary,
                        bankBalance = state.bankBalance,
                        customIncomes = state.customIncomes,
                        onSalaryChange = { viewModel.onSalaryChanged(it) },
                        onBankBalanceChange = { viewModel.onBankBalanceChanged(it) },
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
            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun CategoriesStep(
    selectedCategories: List<String>,
    onCategoriesChange: (List<String>) -> Unit,
    onNext: () -> Unit
) {
    val predefinedCategories = listOf(
        "Food", "Transport", "Shopping", "Entertainment", "Health",
        "Education", "Bills", "Groceries", "Rent", "Travel"
    )

    val customCategories = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        val initialCustom = selectedCategories.filter { it !in predefinedCategories }
        customCategories.addAll(initialCustom)
    }

    val allCategories = predefinedCategories + customCategories
    var newCategoryText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "What do you spend on?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Select categories you want to track or add your own.",
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
                val isSelected = selectedCategories.contains(category)
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
                                selectedCategories - category
                            } else {
                                selectedCategories + category
                            }
                            onCategoriesChange(newList)
                        }
                        .padding(vertical = 16.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom Category Input
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = newCategoryText,
                onValueChange = { newCategoryText = it },
                placeholder = { Text("Add custom category") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val newCat = newCategoryText.trim()
                    if (newCat.isNotBlank() && !allCategories.contains(newCat)) {
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

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = selectedCategories.isNotEmpty()
        ) {
            Text("Continue", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun PaymentModesStep(
    paymentModes: List<PaymentMode>,
    onPaymentModesChange: (List<PaymentMode>) -> Unit,
    onNext: () -> Unit
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
                            onPaymentModesChange(paymentModes + PaymentMode(name.trim(), selectedType))
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
            enabled = paymentModes.isNotEmpty()
        ) {
            Text("Continue", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun IncomingStep(
    salary: String,
    bankBalance: String,
    customIncomes: List<Pair<String, String>>,
    onSalaryChange: (String) -> Unit,
    onBankBalanceChange: (String) -> Unit,
    onCustomIncomesChange: (List<Pair<String, String>>) -> Unit,
    onNext: () -> Unit
) {
    var newIncomeName by remember { mutableStateOf("") }
    var newIncomeAmount by remember { mutableStateOf("") }

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
            value = salary,
            onValueChange = onSalaryChange,
            label = { Text("Monthly Salary") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            prefix = { Text("$ ") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = bankBalance,
            onValueChange = onBankBalanceChange,
            label = { Text("Current Bank Balance") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            prefix = { Text("$ ") }
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
                Text(
                    text = customIncome.first, 
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "$ ${customIncome.second}", 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        
        if (customIncomes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Add new income form
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = newIncomeName,
                onValueChange = { newIncomeName = it },
                placeholder = { Text("Name (e.g. Freelance)") },
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
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (newIncomeName.isNotBlank() && newIncomeAmount.isNotBlank()) {
                        onCustomIncomesChange(customIncomes + Pair(newIncomeName.trim(), newIncomeAmount.trim()))
                        newIncomeName = ""
                        newIncomeAmount = ""
                    }
                },
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = "Add Income", 
                    tint = MaterialTheme.colorScheme.onPrimary
                )
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

        OutlinedTextField(
            value = nextMonthPendingPayment,
            onValueChange = onNextMonthPendingPaymentChange,
            label = { Text("Next Month Pending Payment (Optional)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            prefix = { Text("$ ") }
        )

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
                selectedCategories = listOf("Food", "Transport"),
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
                    PaymentMode("HDFC Bank", "PAY_NOW"),
                    PaymentMode("Amex Card", "PAY_LATER")
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
                salary = "5000",
                bankBalance = "12000",
                customIncomes = listOf(Pair("Freelance", "1500")),
                onSalaryChange = {},
                onBankBalanceChange = {},
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
