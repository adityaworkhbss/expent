package com.aditya.expent.presentation.cashflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
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
import com.aditya.expent.data.remote.dto.BudgetResponseDto
import com.aditya.expent.presentation.dashboard.DashboardViewModel
import com.aditya.expent.presentation.onboard.RecurringExpense
import com.aditya.expent.presentation.onboard.Subscription
import com.aditya.expent.presentation.theme.ColorExpense
import com.aditya.expent.presentation.theme.ExpentTheme
import com.aditya.expent.utils.AppUtils
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import kotlin.getValue


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
                        onBack = {
                            finish()
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
    onBack : () -> Unit
) {

    var selectedTab by remember {
        mutableStateOf(CashflowTab.INCOMING)
    }

    val themeColor = when (selectedTab) {
        CashflowTab.INCOMING -> Color(0xFF00ACC1)
        CashflowTab.OUTGOING -> ColorExpense
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
            themeColor = themeColor,
            onBack = onBack
        )
    }
}

@Composable
fun CashflowContentScreen(
    modifier: Modifier = Modifier,
    title: String,
    income: List<BudgetResponseDto>,
    themeColor: Color,
    onBack: () -> Unit
) {

    val recurringExpenses = remember {
        listOf(
            RecurringExpense(
                name = "Car Loan",
                amount = "300",
                totalMonths = "48",
                monthsPaid = "12",
                startDate = "01/01/2023"
            ),
            RecurringExpense(
                name = "Home Loan",
                amount = "850",
                totalMonths = "120",
                monthsPaid = "30",
                startDate = "12/03/2022"
            )
        )
    }

    val subscriptions = remember {
        listOf(
            Subscription(
                name = "Netflix",
                amount = "15",
                billingDate = "12th"
            ),
            Subscription(
                name = "Spotify",
                amount = "10",
                billingDate = "25th"
            )
        )
    }

    val incomes = income.map { it ->
        Subscription(
            name = it.category?.name.toString(),
            amount = it.limitAmount,
            billingDate = AppUtils().getDayWithSuffix(it.startDate)
        )
    }

    val activeList = if (title.contentEquals("Incoming")) incomes else subscriptions

    LazyColumn(
        modifier = modifier
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
                onAddClick = {}
            )

            Spacer(modifier = Modifier.height(10.dp))
        }

        items(activeList.size) { index ->
            EnhancedSubscriptionCard(
                subscription = activeList[index],
                themeColor = themeColor
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
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
    themeColor: Color
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
                    text = "Billing on ${subscription.billingDate}",
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
                        ),
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
//
//@Preview(showBackground = true)
//@Composable
//fun CashflowPreview() {
//
//    ExpentTheme {
//
//        Surface(
//            modifier = Modifier.fillMaxSize()
//        ) {
//
//            CashflowScreen(
//                onBack = {}
//            )
//        }
//    }
//}
//
//@Preview(
//    showBackground = true,
//    showSystemUi = true,
//    name = "Incoming Flow Screen"
//)
//@Composable
////fun IncomingFlowPreview() {
////
////    ExpentTheme {
////
////        Surface(
////            modifier = Modifier.fillMaxSize()
////        ) {
////
////            CashflowContentScreen(
////                title = "Incoming",
////                themeColor = Color(0xFF00ACC1),
////
////                onBack = {}
////            )
////        }
////    }
////}
//
//@Preview(
//    showBackground = true,
//    showSystemUi = true,
//    name = "Outgoing Flow Screen"
//)
//@Composable
//fun OutgoingFlowPreview() {
//
//    ExpentTheme {
//
//        Surface(
//            modifier = Modifier.fillMaxSize()
//        ) {
//
//            CashflowContentScreen(
//                title = "Outgoing",
//                themeColor = ColorExpense,
//                onBack = {}
//            )
//        }
//    }
//}