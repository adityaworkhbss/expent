package com.aditya.expent.presentation.onboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya.expent.domain.model.OnboardCategory
import com.aditya.expent.domain.model.OnboardPaymentMode
import com.aditya.expent.presentation.component.DayOfMonthPicker
import com.aditya.expent.presentation.component.dayOfMonthLabel
import com.aditya.expent.presentation.theme.ExpentTheme
import com.aditya.expent.presentation.theme.LocalSoftColors
import com.aditya.expent.presentation.theme.softColors
import com.aditya.expent.utils.AppUtils
import com.aditya.expent.utils.CategoryUtils.getCategoryIconAndColor
import com.aditya.expent.utils.DayOfMonth

private const val TOTAL_SETUP_STEPS = 4

@Composable
fun OnboardRoute(
    viewModel: OnboardViewModel,
    onFinish: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val colors = softColors()

    CompositionLocalProvider(LocalSoftColors provides colors) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            OnboardBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                StepProgressHeader(step = state.currentStep)

                AnimatedContent(
                    targetState = state.currentStep,
                    transitionSpec = {
                        val isForward = targetState.ordinal > initialState.ordinal
                        val direction = if (isForward) 1 else -1
                        (slideInHorizontally(animationSpec = tween(450, easing = EaseInOutCubic)) { width -> direction * width } + fadeIn(animationSpec = tween(450))).togetherWith(
                            slideOutHorizontally(animationSpec = tween(450, easing = EaseInOutCubic)) { width -> -direction * width } + fadeOut(animationSpec = tween(450))
                        )
                    },
                    label = "onboarding_step",
                    modifier = Modifier.weight(1f)
                ) { step ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (step) {
                            OnboardStep.WELCOME -> WelcomeStep(onNext = { viewModel.nextStep() })
                            // Categories are already observed from Room in the ViewModel's init
                            OnboardStep.CATEGORIES -> CategoriesStep(
                                availableCategories = state.availableCategories,
                                selectedCategories = state.selectedCategories,
                                onCategoriesChange = { viewModel.onCategoriesSelected(it) },
                                onNext = { viewModel.nextStep() },
                                isLoading = state.isLoading
                            )
                            OnboardStep.PAYMENT_MODES -> PaymentModesStep(
                                paymentModes = state.paymentModes,
                                onPaymentModesChange = { viewModel.onPaymentModesChanged(it) },
                                onNext = { viewModel.nextStep() },
                                isLoading = state.isLoading
                            )
                            OnboardStep.INCOMING -> IncomingStep(
                                salary = state.salary,
                                customIncomes = state.customIncomes,
                                // Only categories the user saved (Room) with type INCOME
                                incomeCategories = state.savedCategories.filter { it.type.equals("INCOME", ignoreCase = true) },
                                onSalaryAmountChange = { viewModel.onSalaryAmountChanged(it) },
                                onSalaryCategoryChange = { viewModel.onSalaryCategoryChanged(it) },
                                onSalaryDateChange = { viewModel.onSalaryDateChanged(it) },
                                onCustomIncomesChange = { viewModel.onCustomIncomesChanged(it) },
                                onNext = { viewModel.nextStep() }
                            )
                            OnboardStep.OUTGOING -> OutgoingStep(
                                creditCardBill = state.creditCardBill,
                                nextMonthPendingPayment = state.nextMonthPendingPayment,
                                recurringExpenses = state.recurringExpenses,
                                subscriptions = state.subscriptions,
                                // Only categories the user saved (Room) with type EXPENSE
                                expenseCategories = state.savedCategories.filter { it.type.equals("EXPENSE", ignoreCase = true) },
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
            }

            state.error?.let { error ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 96.dp, start = 24.dp, end = 24.dp)
                        .shadow(10.dp, RoundedCornerShape(18.dp), spotColor = colors.expense.copy(alpha = 0.4f))
                        .clip(RoundedCornerShape(18.dp))
                        .background(lerp(colors.card, colors.expense, 0.10f))
                        .border(1.dp, colors.expense.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.expense.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = colors.expense,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = error,
                        color = colors.ink,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

// Soft colour blobs behind every step
@Composable
private fun OnboardBackground() {
    val colors = LocalSoftColors.current
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.primary.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(size.width, 0f),
                radius = 340.dp.toPx()
            ),
            center = Offset(size.width, 0f),
            radius = 340.dp.toPx()
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.peach.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(0f, size.height),
                radius = 320.dp.toPx()
            ),
            center = Offset(0f, size.height),
            radius = 320.dp.toPx()
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.ai.copy(alpha = 0.07f), Color.Transparent),
                center = Offset(0f, size.height * 0.35f),
                radius = 240.dp.toPx()
            ),
            center = Offset(0f, size.height * 0.35f),
            radius = 240.dp.toPx()
        )
    }
}

@Composable
private fun StepProgressHeader(step: OnboardStep) {
    val colors = LocalSoftColors.current
    val isSetupStep = step.index in 1..TOTAL_SETUP_STEPS

    AnimatedVisibility(
        visible = isSetupStep,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Step ${step.index.coerceAtLeast(1)} of $TOTAL_SETUP_STEPS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary
                )
                Text(
                    text = "Setup",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.muted
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(TOTAL_SETUP_STEPS) { i ->
                    val segmentColor by animateColorAsState(
                        targetValue = if (i < step.index) colors.primary else colors.cardBorder,
                        animationSpec = tween(400),
                        label = "ProgressSegment"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(segmentColor)
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    val colors = LocalSoftColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "welcome")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.8f))

        // Wallet hero with floating badges
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(colors.primary.copy(alpha = 0.22f), Color.Transparent)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .shadow(18.dp, CircleShape, spotColor = colors.primary)
                    .background(
                        Brush.linearGradient(listOf(colors.primaryEnd, colors.primary)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = Color.White
                )
            }
            FloatingBadge(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                accent = colors.income,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 24.dp)
                    .graphicsLayer { translationY = floatOffset.dp.toPx() }
            )
            FloatingBadge(
                icon = Icons.Default.AutoAwesome,
                accent = colors.ai,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp, top = 8.dp)
                    .graphicsLayer { translationY = -floatOffset.dp.toPx() }
            )
            FloatingBadge(
                icon = Icons.Default.Receipt,
                accent = colors.expense,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 20.dp)
                    .graphicsLayer { translationY = floatOffset.dp.toPx() }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = buildAnnotatedString {
                append("Welcome to ")
                withStyle(SpanStyle(color = colors.primary)) { append("Expent") }
            },
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            ),
            color = colors.ink,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Let's set up your money space in four quick steps.",
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
            color = colors.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        SoftCard(contentSpacing = 12.dp) {
            WelcomeFeatureRow(Icons.Default.Category, colors.primary, "Pick the categories you use")
            WelcomeFeatureRow(Icons.Default.CreditCard, colors.sky, "Add your accounts and cards")
            WelcomeFeatureRow(Icons.Default.DateRange, colors.expense, "Map income, bills and subscriptions")
        }

        Spacer(modifier = Modifier.weight(1f))

        OnboardPrimaryButton(
            text = "Get Started",
            enabled = true,
            onClick = onNext,
            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Takes about 2 minutes",
            style = MaterialTheme.typography.labelMedium,
            color = colors.muted
        )
    }
}

@Composable
private fun FloatingBadge(icon: ImageVector, accent: Color, modifier: Modifier = Modifier) {
    val colors = LocalSoftColors.current
    Box(
        modifier = modifier
            .size(44.dp)
            .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = accent.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card)
            .border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun WelcomeFeatureRow(icon: ImageVector, accent: Color, text: String) {
    val colors = LocalSoftColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon = icon, accent = accent, size = 36.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = colors.ink
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoriesStep(
    availableCategories: List<OnboardCategory>,
    selectedCategories: List<OnboardCategory>,
    onCategoriesChange: (List<OnboardCategory>) -> Unit,
    onNext: () -> Unit,
    isLoading: Boolean = false
) {
    val colors = LocalSoftColors.current
    val customCategories = remember { mutableStateListOf<OnboardCategory>() }
    val allCategories = availableCategories + customCategories
    var newCategoryText by remember { mutableStateOf("") }
    var newCategoryType by remember { mutableStateOf("EXPENSE") }

    fun isSelected(category: OnboardCategory) =
        selectedCategories.any { it.name == category.name && it.type == category.type }

    fun toggle(category: OnboardCategory) {
        val newList = if (isSelected(category)) {
            selectedCategories.filterNot { it.name == category.name && it.type == category.type }
        } else {
            selectedCategories + category
        }
        onCategoriesChange(newList)
    }

    fun addCustomCategory() {
        val newCatName = newCategoryText.trim()
        if (newCatName.isNotBlank() && allCategories.none { it.name == newCatName && it.type == newCategoryType }) {
            val newCat = OnboardCategory(newCatName, newCategoryType)
            customCategories.add(newCat)
            onCategoriesChange(selectedCategories + newCat)
            newCategoryText = ""
        }
    }

    val canContinue = selectedCategories.isNotEmpty() && !isLoading

    StepLayout(
        icon = Icons.Default.Category,
        accent = colors.primary,
        title = "Pick your categories",
        subtitle = "Choose what you spend on and earn from. You can change these later.",
        bottomBar = {
            OnboardPrimaryButton(
                text = if (selectedCategories.isEmpty()) "Select at least one category" else "Continue · ${selectedCategories.size} selected",
                enabled = canContinue,
                onClick = onNext,
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward
            )
        }
    ) {
        listOf(
            Triple("EXPENSE", "Expenses", colors.expense),
            Triple("INCOME", "Income", colors.income)
        ).forEach { (type, label, accent) ->
            val sectionCategories = allCategories.filter { it.type.equals(type, ignoreCase = true) }
            if (sectionCategories.isNotEmpty()) {
                val allSelected = sectionCategories.all { isSelected(it) }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = label,
                        accent = accent,
                        icon = if (type == "INCOME") Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        actionLabel = if (allSelected) "Clear" else "Select all",
                        onAction = {
                            onCategoriesChange(
                                if (allSelected) {
                                    selectedCategories.filterNot { sel -> sectionCategories.any { it.name == sel.name && it.type == sel.type } }
                                } else {
                                    selectedCategories + sectionCategories.filterNot { isSelected(it) }
                                }
                            )
                        }
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sectionCategories.forEach { category ->
                            val (icon, catColor) = getCategoryIconAndColor(category.name)
                            SoftChip(
                                label = category.name,
                                icon = icon,
                                accent = catColor,
                                selected = isSelected(category),
                                onClick = { toggle(category) }
                            )
                        }
                    }
                }
            }
        }

        // Custom category form
        SoftCard {
            FormTitle(icon = Icons.Default.Add, accent = colors.primary, title = "Add your own")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                SoftTextField(
                    value = newCategoryText,
                    onValueChange = { newCategoryText = it },
                    placeholder = "e.g. Books, Gym, Pets",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                val canAdd = newCategoryText.isNotBlank()
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (canAdd) Brush.linearGradient(listOf(colors.primary, colors.primaryEnd))
                            else Brush.linearGradient(listOf(colors.cardBorder, colors.cardBorder))
                        )
                        .clickable(enabled = canAdd) { addCustomCategory() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = if (canAdd) Color.White else colors.muted
                    )
                }
            }
            SegmentedChoice(
                options = listOf(
                    ChoiceOption("EXPENSE", "Expense", Icons.AutoMirrored.Filled.TrendingDown, colors.expense),
                    ChoiceOption("INCOME", "Income", Icons.AutoMirrored.Filled.TrendingUp, colors.income)
                ),
                selected = newCategoryType,
                onSelect = { newCategoryType = it }
            )
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
    val colors = LocalSoftColors.current
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("PAY_NOW") }

    StepLayout(
        icon = Icons.Default.AccountBalanceWallet,
        accent = colors.sky,
        title = "Your accounts",
        subtitle = "Add the wallets, bank accounts and cards you pay with.",
        bottomBar = {
            OnboardPrimaryButton(
                text = if (paymentModes.isEmpty()) "Add at least one account" else "Continue",
                enabled = paymentModes.isNotEmpty() && !isLoading,
                onClick = onNext,
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward
            )
        }
    ) {
        if (paymentModes.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Default.CreditCard,
                accent = colors.sky,
                text = "No accounts yet. Add your first one below."
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                paymentModes.forEach { mode ->
                    PaymentModeCard(
                        mode = mode,
                        onDelete = { onPaymentModesChange(paymentModes.filter { it != mode }) }
                    )
                }
            }
        }

        SoftCard {
            FormTitle(icon = Icons.Default.Add, accent = colors.sky, title = "Add an account")
            SoftTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "e.g. SBI Savings, Amex Card",
                accent = colors.sky
            )
            SegmentedChoice(
                options = listOf(
                    ChoiceOption("PAY_NOW", "Pay Now", Icons.Default.AccountBalance, colors.income),
                    ChoiceOption("PAY_LATER", "Pay Later", Icons.Default.CreditCard, colors.ai)
                ),
                selected = selectedType,
                onSelect = { selectedType = it }
            )
            Text(
                text = if (selectedType == "PAY_NOW") {
                    "Money leaves right away: bank account, UPI, cash."
                } else {
                    "Paid off later: credit cards and buy-now-pay-later."
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted
            )
            OnboardAddButton(
                text = "Add account",
                accent = colors.sky,
                enabled = name.isNotBlank(),
                onClick = {
                    if (name.isNotBlank()) {
                        onPaymentModesChange(paymentModes + OnboardPaymentMode(name.trim(), selectedType))
                        name = ""
                    }
                }
            )
        }
    }
}

@Composable
private fun PaymentModeCard(mode: OnboardPaymentMode, onDelete: () -> Unit) {
    val isCredit = mode.type == "PAY_LATER"
    val gradient = if (isCredit) {
        listOf(Color(0xFF8B7CFF), Color(0xFFF07FB5))
    } else {
        listOf(Color(0xFF34C3A0), Color(0xFF1E9EA8))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = gradient.first())
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(gradient))
            .drawBehind {
                // Decorative circles for a card-like feel
                drawCircle(Color.White.copy(alpha = 0.12f), radius = size.height * 0.75f, center = Offset(size.width, 0f))
                drawCircle(Color.White.copy(alpha = 0.08f), radius = size.height * 0.5f, center = Offset(size.width * 0.75f, size.height))
            }
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCredit) Icons.Default.CreditCard else Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(
                text = mode.name,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isCredit) "PAY LATER" else "PAY NOW",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.22f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IncomingStep(
    salary: RecurringIncome,
    customIncomes: List<RecurringIncome>,
    incomeCategories: List<OnboardCategory>,
    onSalaryAmountChange: (String) -> Unit,
    onSalaryCategoryChange: (String?) -> Unit,
    onSalaryDateChange: (Int) -> Unit,
    onCustomIncomesChange: (List<RecurringIncome>) -> Unit,
    onNext: () -> Unit
) {
    val colors = LocalSoftColors.current
    var newIncomeAmount by remember { mutableStateOf("") }
    var newIncomeCategory by remember { mutableStateOf<String?>(null) }
    var newIncomeDate by remember { mutableStateOf<Int?>(null) }

    var showSalaryDatePicker by remember { mutableStateOf(false) }
    var showNewIncomeDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(incomeCategories) {
        if (salary.categoryId.isNullOrBlank() && incomeCategories.isNotEmpty()) {
            val defaultCat = incomeCategories.firstOrNull { it.name.equals("Salary", ignoreCase = true) }
                ?: incomeCategories.first()
            val catIdentifier = if (defaultCat.id.isNotBlank()) defaultCat.id else defaultCat.name
            onSalaryCategoryChange(catIdentifier)
        }
    }

    StepLayout(
        icon = Icons.AutoMirrored.Filled.TrendingUp,
        accent = colors.income,
        title = "Money coming in",
        subtitle = "Tell us about your regular income so we can plan your month.",
        bottomBar = {
            OnboardPrimaryButton(
                text = when {
                    salary.amount.isBlank() -> "Enter your salary to continue"
                    salary.date == null -> "Pick the day your salary arrives"
                    else -> "Continue"
                },
                enabled = salary.amount.isNotBlank() && salary.date != null,
                onClick = onNext,
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward
            )
        }
    ) {
        // Main salary card
        SoftCard {
            FormTitle(icon = Icons.Default.Work, accent = colors.income, title = "Monthly salary")
            SoftTextField(
                value = salary.amount,
                onValueChange = onSalaryAmountChange,
                placeholder = "0",
                prefix = "$",
                keyboardType = KeyboardType.Number,
                accent = colors.income,
                large = true
            )
            if (incomeCategories.isNotEmpty()) {
                FieldLabel("Category")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    incomeCategories.forEach { category ->
                        val isSel = salary.categoryId != null && (
                            salary.categoryId == category.id ||
                                salary.categoryId.equals(category.name, ignoreCase = true)
                            )
                        SoftChip(
                            label = category.name,
                            icon = getCategoryIconAndColor(category.name).first,
                            accent = colors.income,
                            selected = isSel,
                            onClick = {
                                val catIdentifier = if (category.id.isNotBlank()) category.id else category.name
                                onSalaryCategoryChange(catIdentifier)
                            }
                        )
                    }
                }
            }
            DayPickerField(
                label = "Credited on",
                day = salary.date,
                accent = colors.income,
                onClick = { showSalaryDatePicker = true }
            )
        }

        DayOfMonthPicker(
            showDialog = showSalaryDatePicker,
            selectedDay = salary.date,
            accentColor = colors.income,
            onDismiss = { showSalaryDatePicker = false },
            onDaySelected = {
                onSalaryDateChange(it)
                showSalaryDatePicker = false
            }
        )

        SectionHeader(
            title = "Other income",
            subtitle = "Freelance, rent, interest and more",
            accent = colors.income,
            icon = Icons.Default.Savings
        )

        if (customIncomes.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                customIncomes.forEach { customIncome ->
                    RecurringItemCard(
                        icon = getCategoryIconAndColor(customIncome.name).first,
                        accent = colors.income,
                        title = customIncome.name,
                        subtitle = "Credited on ${dayOfMonthLabel(customIncome.date)}",
                        amount = "$${customIncome.amount}",
                        onRemove = { onCustomIncomesChange(customIncomes - customIncome) }
                    )
                }
            }
        }

        // Add custom income form
        SoftCard {
            FormTitle(icon = Icons.Default.Add, accent = colors.income, title = "Add another income")
            // The category is the income's name, so there is no separate name field
            SoftTextField(
                value = newIncomeAmount,
                onValueChange = { newIncomeAmount = it },
                placeholder = "Amount",
                prefix = "$",
                keyboardType = KeyboardType.Number,
                accent = colors.income
            )
            if (incomeCategories.isNotEmpty()) {
                FieldLabel("Category")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    incomeCategories.forEach { category ->
                        val isSel = newIncomeCategory != null && (
                            newIncomeCategory == category.id ||
                                newIncomeCategory.equals(category.name, ignoreCase = true)
                            )
                        SoftChip(
                            label = category.name,
                            icon = getCategoryIconAndColor(category.name).first,
                            accent = colors.income,
                            selected = isSel,
                            onClick = {
                                val catIdentifier = if (category.id.isNotBlank()) category.id else category.name
                                newIncomeCategory = catIdentifier
                            }
                        )
                    }
                }
            }
            DayPickerField(
                label = "Credited on",
                day = newIncomeDate,
                accent = colors.income,
                onClick = { showNewIncomeDatePicker = true }
            )

            val newIncomeCategoryItem = incomeCategories.firstOrNull { it.id == newIncomeCategory }
            val canAddIncome = newIncomeCategoryItem != null && newIncomeAmount.isNotBlank() && newIncomeDate != null
            OnboardAddButton(
                text = "Add income source",
                accent = colors.income,
                enabled = canAddIncome,
                onClick = {
                    if (newIncomeCategoryItem != null && canAddIncome) {
                        onCustomIncomesChange(customIncomes + RecurringIncome(
                            name = newIncomeCategoryItem.name,
                            amount = newIncomeAmount.trim(),
                            categoryId = newIncomeCategoryItem.id,
                            date = newIncomeDate
                        ))
                        newIncomeAmount = ""
                        newIncomeDate = null
                        newIncomeCategory = null
                    }
                }
            )
        }

        DayOfMonthPicker(
            showDialog = showNewIncomeDatePicker,
            selectedDay = newIncomeDate,
            accentColor = colors.income,
            onDismiss = { showNewIncomeDatePicker = false },
            onDaySelected = {
                newIncomeDate = it
                showNewIncomeDatePicker = false
            }
        )
    }
}

@Composable
fun OutgoingStep(
    creditCardBill: String,
    nextMonthPendingPayment: String,
    recurringExpenses: List<RecurringExpense>,
    subscriptions: List<Subscription>,
    expenseCategories: List<OnboardCategory>,
    onCreditCardBillChange: (String) -> Unit,
    onNextMonthPendingPaymentChange: (String) -> Unit,
    onRecurringExpensesChange: (List<RecurringExpense>) -> Unit,
    onSubscriptionsChange: (List<Subscription>) -> Unit,
    onNext: () -> Unit
) {
    val colors = LocalSoftColors.current
    var emiCategory by remember { mutableStateOf<String?>(null) }
    var subCategory by remember { mutableStateOf<String?>(null) }

    // Local states for forms
    var emiName by remember { mutableStateOf("") }
    var emiAmount by remember { mutableStateOf("") }
    var emiTotalMonths by remember { mutableStateOf("") }
    var emiMonthsPaid by remember { mutableStateOf("") }
    var emiDate by remember { mutableStateOf<Int?>(null) }
    var showEmiDatePicker by remember { mutableStateOf(false) }

    var subName by remember { mutableStateOf("") }
    var subAmount by remember { mutableStateOf("") }
    var subDate by remember { mutableStateOf<Int?>(null) }
    var showSubDatePicker by remember { mutableStateOf(false) }

    StepLayout(
        icon = Icons.AutoMirrored.Filled.TrendingDown,
        accent = colors.expense,
        title = "Money going out",
        subtitle = "Add loans and subscriptions that repeat every month. You can skip this.",
        bottomBar = {
            OnboardPrimaryButton(
                text = "Complete Setup",
                enabled = true,
                onClick = onNext,
                trailingIcon = Icons.Default.Check
            )
        }
    ) {
        SectionHeader(
            title = "Loans & EMIs",
            accent = colors.expense,
            icon = Icons.Default.Percent
        )

        // Existing loans with their repayment progress
        if (recurringExpenses.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                recurringExpenses.forEach { emi ->
                    val paid = emi.monthsPaid.toFloatOrNull() ?: 0f
                    val total = emi.totalMonths.toFloatOrNull() ?: 1f
                    val progress = (paid / total).coerceIn(0f, 1f)

                    RecurringItemCard(
                        icon = Icons.Default.Percent,
                        accent = colors.expense,
                        title = emi.name,
                        subtitle = listOfNotNull(
                            "Due ${emi.date?.let { DayOfMonth.withSuffix(it) }.orEmpty()}",
                            expenseCategories.firstOrNull { it.id == emi.categoryId }?.name
                        ).joinToString(" • "),
                        amount = "$${emi.amount}/mo",
                        onRemove = { onRecurringExpensesChange(recurringExpenses - emi) }
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(colors.expense.copy(alpha = 0.12f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(Brush.horizontalGradient(listOf(colors.peach, colors.expense)))
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${emi.monthsPaid} of ${emi.totalMonths} months paid",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.muted
                        )
                    }
                }
            }
        }

        // Add loan form
        SoftCard {
            FormTitle(icon = Icons.Default.Add, accent = colors.expense, title = "Add a loan or EMI")
            SoftTextField(
                value = emiName,
                onValueChange = { emiName = it },
                placeholder = "Name, e.g. Car Loan",
                accent = colors.expense
            )
            SoftTextField(
                value = emiAmount,
                onValueChange = { emiAmount = it },
                placeholder = "Monthly amount",
                prefix = "$",
                keyboardType = KeyboardType.Number,
                accent = colors.expense
            )

            val totalMonthsVal = emiTotalMonths.toIntOrNull() ?: 0
            val monthsPaidVal = emiMonthsPaid.toIntOrNull() ?: 0
            val isPaidExceeded = totalMonthsVal > 0 && emiMonthsPaid.isNotBlank() && monthsPaidVal >= totalMonthsVal

            Row(modifier = Modifier.fillMaxWidth()) {
                SoftTextField(
                    value = emiTotalMonths,
                    onValueChange = { emiTotalMonths = it },
                    placeholder = "Total months",
                    keyboardType = KeyboardType.Number,
                    accent = colors.expense,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                SoftTextField(
                    value = emiMonthsPaid,
                    onValueChange = { emiMonthsPaid = it },
                    placeholder = "Months paid",
                    keyboardType = KeyboardType.Number,
                    accent = colors.expense,
                    isError = isPaidExceeded,
                    supportingText = if (isPaidExceeded) "Must be less than $totalMonthsVal" else null,
                    modifier = Modifier.weight(1f)
                )
            }

            DayPickerField(
                label = "Due on",
                day = emiDate,
                accent = colors.expense,
                onClick = { showEmiDatePicker = true }
            )

            ExpenseCategoryChips(
                categories = expenseCategories,
                selectedId = emiCategory,
                accent = colors.expense,
                onSelect = { emiCategory = it }
            )

            // An EMI needs a tenure; something that never ends belongs under subscriptions
            val canAddEmi = emiName.isNotBlank() && emiAmount.isNotBlank() &&
                totalMonthsVal > 0 && emiMonthsPaid.isNotBlank() && !isPaidExceeded && emiDate != null &&
                (expenseCategories.isEmpty() || emiCategory != null)
            OnboardAddButton(
                text = "Add loan",
                accent = colors.expense,
                enabled = canAddEmi,
                onClick = {
                    if (canAddEmi) {
                        onRecurringExpensesChange(
                            recurringExpenses + RecurringExpense(
                                name = emiName.trim(),
                                amount = emiAmount.trim(),
                                totalMonths = emiTotalMonths.trim(),
                                monthsPaid = emiMonthsPaid.trim(),
                                date = emiDate,
                                categoryId = emiCategory
                            )
                        )
                        emiName = ""
                        emiAmount = ""
                        emiTotalMonths = ""
                        emiMonthsPaid = ""
                        emiDate = null
                        emiCategory = null
                    }
                }
            )
        }

        DayOfMonthPicker(
            showDialog = showEmiDatePicker,
            selectedDay = emiDate,
            accentColor = colors.expense,
            onDismiss = { showEmiDatePicker = false },
            onDaySelected = {
                emiDate = it
                showEmiDatePicker = false
            }
        )

        SectionHeader(
            title = "Subscriptions",
            accent = colors.ai,
            icon = Icons.Default.Subscriptions
        )

        if (subscriptions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                subscriptions.forEach { sub ->
                    RecurringItemCard(
                        icon = Icons.Default.Subscriptions,
                        accent = colors.ai,
                        title = sub.name,
                        subtitle = listOfNotNull(
                            "Billing on ${dayOfMonthLabel(sub.date)}",
                            expenseCategories.firstOrNull { it.id == sub.categoryId }?.name
                        ).joinToString(" • "),
                        amount = "$${sub.amount}/mo",
                        onRemove = { onSubscriptionsChange(subscriptions - sub) }
                    )
                }
            }
        }

        // Add subscription form
        SoftCard {
            FormTitle(icon = Icons.Default.Add, accent = colors.ai, title = "Add a subscription")
            SoftTextField(
                value = subName,
                onValueChange = { subName = it },
                placeholder = "Service, e.g. Netflix",
                accent = colors.ai
            )
            SoftTextField(
                value = subAmount,
                onValueChange = { subAmount = it },
                placeholder = "Amount per month",
                prefix = "$",
                keyboardType = KeyboardType.Number,
                accent = colors.ai
            )
            DayPickerField(
                label = "Billing day",
                day = subDate,
                accent = colors.ai,
                onClick = { showSubDatePicker = true }
            )

            ExpenseCategoryChips(
                categories = expenseCategories,
                selectedId = subCategory,
                accent = colors.ai,
                onSelect = { subCategory = it }
            )

            val canAddSubscription = subName.isNotBlank() && subAmount.isNotBlank() && subDate != null &&
                (expenseCategories.isEmpty() || subCategory != null)
            OnboardAddButton(
                text = "Add subscription",
                accent = colors.ai,
                enabled = canAddSubscription,
                onClick = {
                    if (canAddSubscription) {
                        onSubscriptionsChange(
                            subscriptions + Subscription(
                                name = subName.trim(),
                                amount = subAmount.trim(),
                                date = subDate,
                                categoryId = subCategory
                            )
                        )
                        subName = ""
                        subAmount = ""
                        subDate = null
                        subCategory = null
                    }
                }
            )
        }

        DayOfMonthPicker(
            showDialog = showSubDatePicker,
            selectedDay = subDate,
            accentColor = colors.ai,
            onDismiss = { showSubDatePicker = false },
            onDaySelected = {
                subDate = it
                showSubDatePicker = false
            }
        )
    }
}

// Category chips for EMIs / subscriptions; only EXPENSE categories the user saved are passed in
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpenseCategoryChips(
    categories: List<OnboardCategory>,
    selectedId: String?,
    accent: Color,
    onSelect: (String) -> Unit
) {
    if (categories.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Category")
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                SoftChip(
                    label = category.name,
                    icon = getCategoryIconAndColor(category.name).first,
                    accent = accent,
                    selected = selectedId == category.id,
                    onClick = { onSelect(category.id) }
                )
            }
        }
    }
}

@Composable
fun FinishStep(onFinish: () -> Unit) {
    val colors = LocalSoftColors.current
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
    val confettiColors = listOf(colors.primary, colors.peach, colors.ai, colors.expense, colors.sky, colors.income)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            // Little confetti dots that pop in with the badge
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale)
            ) {
                val radius = size.minDimension / 2.2f
                repeat(12) { i ->
                    val angle = Math.toRadians(i * 30.0 + 10.0)
                    val distance = radius * if (i % 2 == 0) 0.95f else 0.75f
                    drawCircle(
                        color = confettiColors[i % confettiColors.size],
                        radius = if (i % 3 == 0) 6.dp.toPx() else 4.dp.toPx(),
                        center = Offset(
                            x = center.x + (distance * kotlin.math.cos(angle)).toFloat(),
                            y = center.y + (distance * kotlin.math.sin(angle)).toFloat()
                        )
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.12f))
            )
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .scale(scale)
                    .shadow(18.dp, CircleShape, spotColor = colors.primary)
                    .background(
                        Brush.linearGradient(listOf(colors.primaryEnd, colors.primary)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = colors.ink
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Your money space is ready. Start tracking and watch your budget come to life.",
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                    color = colors.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1.2f))

        AnimatedVisibility(
            visible = startAnimation,
            enter = slideInVertically(
                initialOffsetY = { 50 },
                animationSpec = tween(600, delayMillis = 600)
            ) + fadeIn(animationSpec = tween(600, delayMillis = 600))
        ) {
            OnboardPrimaryButton(
                text = "Go to Dashboard",
                enabled = true,
                onClick = onFinish,
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward
            )
        }
    }
}

// region Shared onboarding building blocks

@Composable
private fun StepLayout(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    bottomBar: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalSoftColors.current
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(10.dp, RoundedCornerShape(16.dp), spotColor = accent)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(lerp(accent, Color.White, 0.3f), accent))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.ink
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted
                    )
                }
            }
            content()
        }

        // Pinned action area, fades into the page above it
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(colors.background.copy(alpha = 0f), colors.background)))
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 16.dp)
        ) {
            bottomBar()
        }
    }
}

@Composable
private fun SoftCard(
    modifier: Modifier = Modifier,
    contentSpacing: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalSoftColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = colors.ink.copy(alpha = 0.08f),
                spotColor = colors.ink.copy(alpha = 0.10f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(colors.card)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(contentSpacing),
        content = content
    )
}

@Composable
private fun IconTile(icon: ImageVector, accent: Color, size: Dp = 32.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
private fun FormTitle(icon: ImageVector, accent: Color, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon = icon, accent = accent)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = LocalSoftColors.current.ink
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = LocalSoftColors.current.muted
    )
}

@Composable
private fun SectionHeader(
    title: String,
    accent: Color,
    icon: ImageVector,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    val colors = LocalSoftColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon = icon, accent = accent, size = 28.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.ink
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted
                )
            }
        }
        if (actionLabel != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onAction)
                    .background(accent.copy(alpha = 0.10f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun SoftTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    accent: Color = LocalSoftColors.current.primary,
    prefix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    supportingText: String? = null,
    large: Boolean = false
) {
    val colors = LocalSoftColors.current
    val textStyle = if (large) {
        MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
    } else {
        MaterialTheme.typography.bodyLarge
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, style = textStyle, color = colors.muted.copy(alpha = 0.7f)) },
        prefix = if (prefix != null) {
            { Text("$prefix ", style = textStyle, fontWeight = FontWeight.Bold, color = accent) }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        isError = isError,
        supportingText = if (supportingText != null) {
            { Text(supportingText, color = colors.expense) }
        } else null,
        singleLine = true,
        textStyle = textStyle,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent,
            unfocusedBorderColor = colors.cardBorder,
            errorBorderColor = colors.expense,
            focusedContainerColor = colors.card,
            unfocusedContainerColor = colors.background,
            errorContainerColor = colors.card,
            focusedTextColor = colors.ink,
            unfocusedTextColor = colors.ink,
            cursorColor = accent,
            errorCursorColor = colors.expense
        )
    )
}

// Whole row is tappable, opening the day-of-month picker
@Composable
private fun DayPickerField(
    label: String,
    day: Int?,
    accent: Color,
    onClick: () -> Unit
) {
    val colors = LocalSoftColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.background)
            .border(1.dp, if (day != null) accent.copy(alpha = 0.35f) else colors.cardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        IconTile(icon = Icons.Default.DateRange, accent = accent, size = 36.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.muted
            )
            Text(
                text = if (day != null) dayOfMonthLabel(day) else "Pick a day",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (day != null) FontWeight.SemiBold else FontWeight.Normal),
                color = if (day != null) colors.ink else colors.muted.copy(alpha = 0.8f)
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Select day",
            tint = accent
        )
    }
}

@Composable
private fun SoftChip(
    label: String,
    icon: ImageVector?,
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
            modifier = Modifier.padding(
                start = if (icon != null) 6.dp else 14.dp,
                end = 12.dp,
                top = 6.dp,
                bottom = 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
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
            }
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

private data class ChoiceOption(
    val value: String,
    val label: String,
    val icon: ImageVector,
    val accent: Color
)

@Composable
private fun SegmentedChoice(
    options: List<ChoiceOption>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val colors = LocalSoftColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSel = selected == option.value
            val bg by animateColorAsState(
                targetValue = if (isSel) lerp(colors.card, option.accent, 0.12f) else colors.background,
                label = "ChoiceBg"
            )
            Surface(
                onClick = { onSelect(option.value) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = bg,
                border = BorderStroke(1.dp, if (isSel) option.accent.copy(alpha = 0.55f) else colors.cardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null,
                        tint = if (isSel) option.accent else colors.muted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = option.label,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSel) option.accent else colors.muted,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RecurringItemCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    amount: String,
    onRemove: () -> Unit,
    extraContent: @Composable ColumnScope.() -> Unit = {}
) {
    val colors = LocalSoftColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.card)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp))
            .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon = icon, accent = accent, size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.ink
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted
                )
            }
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = accent
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = colors.muted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Column(modifier = Modifier.padding(end = 8.dp), content = extraContent)
    }
}

@Composable
private fun EmptyStateCard(icon: ImageVector, accent: Color, text: String) {
    val colors = LocalSoftColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(accent.copy(alpha = 0.06f))
            .border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            .padding(vertical = 24.dp, horizontal = 16.dp)
    ) {
        IconTile(icon = icon, accent = accent, size = 44.dp)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OnboardPrimaryButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    trailingIcon: ImageVector? = null
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
                ambientColor = colors.primary,
                spotColor = colors.primary
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
                    if (enabled) Modifier.background(Brush.horizontalGradient(listOf(colors.primary, colors.primaryEnd)))
                    else Modifier
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (trailingIcon != null && enabled) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// Tinted secondary button used inside the "add" forms
@Composable
private fun OnboardAddButton(
    text: String,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accent,
            contentColor = Color.White,
            disabledContainerColor = accent.copy(alpha = 0.10f),
            disabledContentColor = accent.copy(alpha = 0.5f)
        )
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, fontWeight = FontWeight.Bold)
    }
}

// endregion

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
                salary = RecurringIncome(name = "Salary", amount = "5000", date = 1),
                customIncomes = listOf(
                    RecurringIncome(name = "Freelance", amount = "1500", date = 10)
                ),
                incomeCategories = listOf(OnboardCategory("Salary", "INCOME")),
                onSalaryAmountChange = {},
                onSalaryCategoryChange = {},
                onSalaryDateChange = {},
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
                recurringExpenses = listOf(RecurringExpense("Car Loan", "300", "48", "12", 5)),
                subscriptions = listOf(Subscription("Netflix", "15", 12)),
                expenseCategories = listOf(OnboardCategory("Bills & Utilities", "EXPENSE", "c1"), OnboardCategory("Entertainment", "EXPENSE", "c2")),
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
