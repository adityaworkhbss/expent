package com.aditya.expent.presentation.onboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya.expent.R
import com.aditya.expent.domain.model.OnboardCategory
import com.aditya.expent.domain.model.OnboardPaymentMode
import com.aditya.expent.presentation.component.ExpentDatePicker
import com.aditya.expent.presentation.theme.ExpentTheme
import com.aditya.expent.presentation.theme.EmeraldPrimary
import com.aditya.expent.presentation.theme.ColorIncome
import com.aditya.expent.presentation.theme.ColorExpense
import com.aditya.expent.utils.AppUtils
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
            // Elegant background ambient glows
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(EmeraldPrimary.copy(alpha = 0.05f), Color.Transparent),
                        radius = 350.dp.toPx()
                    ),
                    center = androidx.compose.ui.geometry.Offset(x = size.width, y = 0f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF3F51B5).copy(alpha = 0.03f), Color.Transparent),
                        radius = 300.dp.toPx()
                    ),
                    center = androidx.compose.ui.geometry.Offset(x = 0f, y = size.height)
                )
            }

            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    val isForward = targetState.ordinal > initialState.ordinal
                    if (isForward) {
                        (slideInHorizontally(animationSpec = tween(450, easing = EaseInOutCubic)) { width -> width } + fadeIn(animationSpec = tween(450))).togetherWith(
                            slideOutHorizontally(animationSpec = tween(450, easing = EaseInOutCubic)) { width -> -width } + fadeOut(animationSpec = tween(450))
                        )
                    } else {
                        (slideInHorizontally(animationSpec = tween(450, easing = EaseInOutCubic)) { width -> -width } + fadeIn(animationSpec = tween(450))).togetherWith(
                            slideOutHorizontally(animationSpec = tween(450, easing = EaseInOutCubic)) { width -> width } + fadeOut(animationSpec = tween(450))
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

                    if (state.isLoading && step != OnboardStep.FINISH) {
                        AppUtils().ShowProgressAnimation()
                    }
                }
            }

            state.error?.let { error ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 90.dp, start = 24.dp, end = 24.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
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
        Spacer(modifier = Modifier.weight(1f))
        
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Ambient glowing background behind wallet icon
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                EmeraldPrimary.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(12.dp, CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(EmeraldPrimary, Color(0xFF004D40))
                        ),
                        shape = CircleShape
                    )
                    .padding(26.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "Welcome to Expent",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            ),
            color = EmeraldPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Let's set up your profile to give you a gorgeous, premium financial tracking experience.",
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.weight(1.2f))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Get Started", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
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
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Categories Setup",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Step 1 of 4 • Select categories you use",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Grid of Categories styled with glowing cards
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(allCategories) { category ->
                val isSelected = selectedCategories.any { it.name == category.name && it.type == category.type }
                
                Surface(
                    onClick = {
                        val newList = if (isSelected) {
                            selectedCategories.filterNot { it.name == category.name && it.type == category.type }
                        } else {
                            selectedCategories + category
                        }
                        onCategoriesChange(newList)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(if (isSelected) 1.02f else 1.0f)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(18.dp)
                        ),
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) EmeraldPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (category.type == "INCOME") ColorIncome.copy(alpha = 0.1f)
                                    else ColorExpense.copy(alpha = 0.1f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (category.type == "INCOME") Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = if (category.type == "INCOME") ColorIncome else ColorExpense,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Surface(
                            color = if (category.type == "INCOME") ColorIncome.copy(alpha = 0.15f) else ColorExpense.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = category.type,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (category.type == "INCOME") ColorIncome else ColorExpense,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom categories input drawer card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Add Custom Category",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newCategoryText,
                        onValueChange = { newCategoryText = it },
                        placeholder = { Text("e.g., Subscriptions, Books") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            focusedLabelColor = EmeraldPrimary
                        )
                    )
                    
                    Spacer(modifier = Modifier.width(10.dp))
                    
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
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(EmeraldPrimary),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("EXPENSE", "INCOME").forEach { type ->
                        val isSel = newCategoryType == type
                        Surface(
                            onClick = { newCategoryType = type },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (isSel) EmeraldPrimary else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            color = if (isSel) EmeraldPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSel) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = if (type == "EXPENSE") "Expense" else "Income",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 8.dp),
            enabled = selectedCategories.isNotEmpty() && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldPrimary,
                disabledContainerColor = EmeraldPrimary.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Continue", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
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
            .padding(horizontal = 24.dp)
    ) {
        Column(modifier = Modifier.padding(top = 12.dp)) {
            Text(
                text = "Payment Methods",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Step 2 of 4 • Customize accounts & cards",
                style = MaterialTheme.typography.bodySmall,
                color = EmeraldPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scrollable area showing cards and bank options
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Render existing payment methods as virtual cards!
            paymentModes.forEach { mode ->
                val isCredit = mode.type == "PAY_LATER"

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = if (isCredit) listOf(Color(0xFF3F51B5), Color(0xFFE91E63))
                                else listOf(EmeraldPrimary, Color(0xFF004D40))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Gold Chip logo
                        Box(
                            modifier = Modifier
                                .size(34.dp, 24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFFFFEE58), Color(0xFFF57F17))
                                    )
                                )
                                .border(0.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        )
                        
                        // Delete Card Button
                        IconButton(
                            onClick = { onPaymentModesChange(paymentModes.filter { it != mode }) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Card Label & Type badge (No dummy card number shown)
                    Column(modifier = Modifier.align(Alignment.BottomStart)) {
                        Text(
                            text = mode.name,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isCredit) "PAY LATER" else "PAY NOW",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (paymentModes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "💳 Add a wallet or card below to start",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Premium virtual card adder panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Add New Method",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Account / Card Name") },
                        placeholder = { Text("e.g., SBI Savings, Amex Card") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            focusedLabelColor = EmeraldPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("PAY_NOW", "PAY_LATER").forEach { type ->
                            val isSel = selectedType == type
                            Surface(
                                onClick = { selectedType = type },
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSel) EmeraldPrimary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                color = if (isSel) EmeraldPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Text(
                                    text = if (type == "PAY_NOW") "Pay Now" else "Pay Later",
                                    color = if (isSel) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
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
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Payment Mode", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = paymentModes.isNotEmpty() && !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            Text("Continue", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
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
    var showNewIncomeDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Column(modifier = Modifier.padding(top = 12.dp)) {
            Text(
                text = "Incoming Finances",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Step 3 of 4 • Set your income baseline",
                style = MaterialTheme.typography.bodySmall,
                color = EmeraldPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Main Salary card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "💼 Regular Monthly Salary",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldPrimary
                )
                
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = salary.amount,
                    onValueChange = onSalaryAmountChange,
                    label = { Text("Monthly Salary Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    prefix = { Text("$ ", fontWeight = FontWeight.Bold, color = EmeraldPrimary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        focusedLabelColor = EmeraldPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Frequency:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("WEEKLY", "MONTHLY").forEach { period ->
                        val isSel = salary.periodType == period
                        Surface(
                            onClick = { onSalaryPeriodChange(period) },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (isSel) EmeraldPrimary else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            color = if (isSel) EmeraldPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = period.lowercase().replaceFirstChar { it.uppercase() },
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Salary Category:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val incomeCategories = selectedCategories.filter { it.type == "INCOME" }
                    incomeCategories.forEach { category ->
                        val isSel = salary.categoryId == category.name
                        Surface(
                            onClick = { onSalaryCategoryChange(category.name) },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (isSel) EmeraldPrimary else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            color = if (isSel) EmeraldPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = category.name,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = salary.startDate,
                    onValueChange = { },
                    label = { Text("Start Date") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = {
                        IconButton(onClick = { showSalaryStartDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date", tint = EmeraldPrimary)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        focusedLabelColor = EmeraldPrimary
                    )
                )

                ExpentDatePicker(
                    showDialog = showSalaryStartDatePicker,
                    onDismiss = { showSalaryStartDatePicker = false },
                    onDateSelected = {
                        onSalaryStartDateChange(it)
                        showSalaryStartDatePicker = false
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Additional Incomes",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        customIncomes.forEach { customIncome ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(ColorIncome.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AddCard, contentDescription = null, tint = ColorIncome, modifier = Modifier.size(20.dp))
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = customIncome.name, 
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${customIncome.periodType.lowercase().replaceFirstChar { it.uppercase() }} | Category: ${customIncome.categoryId ?: "General"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "$ ${customIncome.amount}", 
                        fontWeight = FontWeight.Black,
                        color = ColorIncome,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        // Add custom income panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Add Secondary Income Source",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldPrimary
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newIncomeName,
                        onValueChange = { newIncomeName = it },
                        placeholder = { Text("Source e.g., Rent") },
                        modifier = Modifier.weight(1.4f),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            focusedLabelColor = EmeraldPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = newIncomeAmount,
                        onValueChange = { newIncomeAmount = it },
                        placeholder = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            focusedLabelColor = EmeraldPrimary
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("WEEKLY", "MONTHLY").forEach { period ->
                        val isSel = newIncomePeriod == period
                        Surface(
                            onClick = { newIncomePeriod = period },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (isSel) EmeraldPrimary else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            color = if (isSel) EmeraldPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = period.lowercase().replaceFirstChar { it.uppercase() },
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Category:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val incomeCategories = selectedCategories.filter { it.type == "INCOME" }
                    incomeCategories.forEach { category ->
                        val isSel = newIncomeCategory == category.name
                        Surface(
                            onClick = { newIncomeCategory = category.name },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (isSel) EmeraldPrimary else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            color = if (isSel) EmeraldPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = category.name,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = newIncomeStartDate,
                    onValueChange = { },
                    placeholder = { Text("Start Date") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = {
                        IconButton(onClick = { showNewIncomeDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date", tint = EmeraldPrimary)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        focusedLabelColor = EmeraldPrimary
                    )
                )

                ExpentDatePicker(
                    showDialog = showNewIncomeDatePicker,
                    onDismiss = { showNewIncomeDatePicker = false },
                    onDateSelected = {
                        newIncomeStartDate = it
                        showNewIncomeDatePicker = false
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = newIncomeName.isNotBlank() && newIncomeAmount.isNotBlank() && newIncomeStartDate.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Income Source", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = salary.amount.isNotBlank() && salary.startDate.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            Text("Continue", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
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
            .padding(horizontal = 24.dp)
    ) {
        Column(modifier = Modifier.padding(top = 12.dp)) {
            Text(
                text = "Outgoing Expenses",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Step 4 of 4 • Map your recurring bills",
                style = MaterialTheme.typography.bodySmall,
                color = EmeraldPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "Recurring EMIs & Loans",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Display existing loans as sleek summaries with custom progress bars!
        recurringExpenses.forEach { emi ->
            val paid = emi.monthsPaid.toFloatOrNull() ?: 0f
            val total = emi.totalMonths.toFloatOrNull() ?: 1f
            val progress = (paid / total).coerceIn(0f, 1f)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(ColorExpense.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Percent, contentDescription = null, tint = ColorExpense, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(emi.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        }
                        
                        Text(
                            text = "$ ${emi.amount} / mo", 
                            fontWeight = FontWeight.Black, 
                            color = ColorExpense,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Loan Progress: ${emi.monthsPaid} / ${emi.totalMonths} months",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Started: ${emi.startDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Custom horizontal progress line bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(ColorExpense)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Add Loan Form Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Add Recurring Loan / EMI",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldPrimary
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = emiName,
                        onValueChange = { emiName = it },
                        placeholder = { Text("EMI name e.g., Car Loan") },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            focusedLabelColor = EmeraldPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = emiAmount,
                        onValueChange = { emiAmount = it },
                        placeholder = { Text("Monthly") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            focusedLabelColor = EmeraldPrimary
                        )
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
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            focusedLabelColor = EmeraldPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = emiMonthsPaid,
                        onValueChange = { emiMonthsPaid = it },
                        placeholder = { Text("Months Paid") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            focusedLabelColor = EmeraldPrimary
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = emiStartDate,
                    onValueChange = { },
                    placeholder = { Text("Start Date") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = {
                        IconButton(onClick = { showEmiDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date", tint = EmeraldPrimary)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        focusedLabelColor = EmeraldPrimary
                    )
                )
                
                ExpentDatePicker(
                    showDialog = showEmiDatePicker,
                    onDismiss = { showEmiDatePicker = false },
                    onDateSelected = {
                        emiStartDate = it
                        showEmiDatePicker = false
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))
                
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
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    enabled = emiName.isNotBlank() && emiAmount.isNotBlank() && emiTotalMonths.isNotBlank() && emiMonthsPaid.isNotBlank() && emiStartDate.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Loan Account", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Active Subscriptions",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Existing subscriptions
        subscriptions.forEach { sub ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F7FA)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SmartScreen, contentDescription = null, tint = Color(0xFF00ACC1), modifier = Modifier.size(18.dp))
                    }
                    
                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(sub.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "Billing date: ${sub.billingDate} of every month", 
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "$ ${sub.amount} / mo", 
                        fontWeight = FontWeight.Black, 
                        color = Color(0xFF0097A7),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Add subscription card form
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Add Media / App Subscription",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldPrimary
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = subName,
                    onValueChange = { subName = it },
                    placeholder = { Text("Service name e.g., Netflix") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        focusedLabelColor = EmeraldPrimary
                    )
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Row {
                    OutlinedTextField(
                        value = subAmount,
                        onValueChange = { subAmount = it },
                        placeholder = { Text("Amount / mo") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            focusedLabelColor = EmeraldPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = subDate,
                        onValueChange = { },
                        placeholder = { Text("Billing Date") },
                        readOnly = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { showSubDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Select Date", tint = EmeraldPrimary)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            focusedLabelColor = EmeraldPrimary
                        )
                    )
                }
                
                ExpentDatePicker(
                    showDialog = showSubDatePicker,
                    onDismiss = { showSubDatePicker = false },
                    onDateSelected = {
                        subDate = it
                        showSubDatePicker = false
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        if (subName.isNotBlank() && subAmount.isNotBlank() && subDate.isNotBlank()) {
                            onSubscriptionsChange(subscriptions + Subscription(subName.trim(), subAmount.trim(), subDate.trim()))
                            subName = ""
                            subAmount = ""
                            subDate = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    enabled = subName.isNotBlank() && subAmount.isNotBlank() && subDate.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Subscription", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            Text("Complete Setup", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
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
                .size(110.dp)
                .scale(scale)
                .shadow(12.dp, CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(EmeraldPrimary, Color(0xFF004D40))
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(54.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        AnimatedVisibility(
            visible = startAnimation,
            enter = slideInVertically(
                initialOffsetY = { 50 },
                animationSpec = tween(600, delayMillis = 300)
            ) + fadeIn(animationSpec = tween(600, delayMillis = 300))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "You're all set!",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    color = EmeraldPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Expent is ready to help you master your finances and budgets.",
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        AnimatedVisibility(
            visible = startAnimation,
            enter = slideInVertically(
                initialOffsetY = { 50 },
                animationSpec = tween(600, delayMillis = 600)
            ) + fadeIn(animationSpec = tween(600, delayMillis = 600))
        ) {
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Go to Dashboard", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
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
                    OnboardCategory("Food & Dining", "EXPENSE"),
                    OnboardCategory("Transportation", "EXPENSE"),
                    OnboardCategory("Salary", "INCOME")
                ),
                selectedCategories = listOf(
                    OnboardCategory("Food & Dining", "EXPENSE")
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
                selectedCategories = listOf(OnboardCategory("Salary", "INCOME")),
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
