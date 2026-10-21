package com.aditya.expent.presentation.profile

import android.util.Log
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aditya.expent.config.AppText
import com.aditya.expent.domain.model.User
import com.aditya.expent.presentation.auth.AuthActivity
import com.aditya.expent.presentation.theme.ExpentTheme
import com.aditya.expent.presentation.theme.EmeraldPrimary
import com.aditya.expent.presentation.theme.ColorExpense
import com.aditya.expent.presentation.theme.ColorIncome
import com.aditya.expent.utils.AppUtils
import com.aditya.expent.utils.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    private val viewModel : ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ExpentTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val state by viewModel.state.collectAsState()
                    ProfileScreen(
                        state = state,
                        sessionManager = sessionManager,
                        viewModel = viewModel,
                        onBack = { finish() },
                        onLogout = {
                            viewModel.logout {
                                val intent = Intent(this, AuthActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                startActivity(intent)
                                finish()
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileState,
    viewModel: ProfileViewModel? = null,
    sessionManager: SessionManager? = null,
    currentUser: User? = null,
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {

    var currentUserState by remember { mutableStateOf(currentUser ?: sessionManager?.getUser()) }
    val displayUser = currentUserState ?: User("0", "aditya@expent.com", "Aditya Sharma", "tok", "ref", 1)
    Log.d("ProfileActivity", "ProfileScreen Composing: state.reminder = ${state.reminder}, state.aiTransaction = ${state.aiTransaction}")

    // Option Dialog States
    var showEditProfile by remember { mutableStateOf(false) }
    var showPaymentModes by remember { mutableStateOf(false) }
    var showCategoriesManager by remember { mutableStateOf(false) }
    var showAppSettings by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    // Shared preferences states (handled via state)
    var isDarkModeEnabled by remember { mutableStateOf(false) }
    var selectedCurrency by remember { mutableStateOf("USD ($)") }

    var paymentModes = state.accounts
    var categories = state.categories

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item { Spacer(modifier = Modifier.height(20.dp)) }

        // Top Navigation Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = AppText.get("profile.contentDescriptions.back", "Back"),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = AppText.get("profile.title", "Profile Settings"),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // User Avatar Profile Card with sleek gradient background and Canvas shapes
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                EmeraldPrimary,
                                Color(0xFF00695C)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                // Background artistic decorative circle shapes
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = 120.dp.toPx(),
                        center = Offset(x = size.width - 20.dp.toPx(), y = 30.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.03f),
                        radius = 80.dp.toPx(),
                        center = Offset(x = 10.dp.toPx(), y = size.height - 20.dp.toPx())
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Initials Avatar
                    val initials = displayUser.name.split(" ")
                        .mapNotNull { it.firstOrNull() }
                        .take(2)
                        .joinToString("")
                        .uppercase()

                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayUser.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = displayUser.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

//                        // Custom glass "Edit Profile" pill button
//                        Surface(
//                            modifier = Modifier
//                                .clip(RoundedCornerShape(12.dp))
//                                .clickable { showEditProfile = true },
//                            color = Color.White.copy(alpha = 0.15f),
//                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
//                        ) {
//                            Row(
//                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
//                                verticalAlignment = Alignment.CenterVertically
//                            ) {
//                                Icon(
//                                    imageVector = Icons.Default.Edit,
//                                    contentDescription = null,
//                                    tint = Color.White,
//                                    modifier = Modifier.size(14.dp)
//                                )
//                                Spacer(modifier = Modifier.width(6.dp))
//                                Text(
//                                    text = "Edit Profile",
//                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
//                                    color = Color.White
//                                )
//                            }
//                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(28.dp)) }

        // Section: Account Management Options Panel
        item {
            Text(
                text = AppText.get("profile.sections.accountManagement", "Account Management"),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column {
                    ProfileOptionRow(
                        title = AppText.get("profile.options.paymentModes.title", "Payment Modes"),
                        subtitle = AppText.get("profile.options.paymentModes.subtitle", "Manage credit cards, banks & cash wallets"),
                        icon = Icons.Default.CreditCard,
                        iconColor = Color(0xFF0288D1),
                        onClick = { showPaymentModes = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                    ProfileOptionRow(
                        title = AppText.get("profile.options.categoryManager.title", "Categories Manager"),
                        subtitle = AppText.get("profile.options.categoryManager.subtitle", "Customise expense & income categories"),
                        icon = Icons.Default.Category,
                        iconColor = Color(0xFFAB47BC),
                        onClick = { showCategoriesManager = true }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Section: Preferences Options Panel
        item {
            Text(
                text = AppText.get("profile.sections.preferences", "Preferences"),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column {
                    // Daily reminders with a beautiful interactive M3 switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFB300).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = AppText.get("profile.options.dailyReminders.title", "Daily Reminders"),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = AppText.get("profile.options.dailyReminders.subtitle", "Daily notifications to log expenses"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.reminder,
                            onCheckedChange = { 
                                Log.d("ProfileActivity", "Daily Reminders switch toggled: $it")
                                viewModel?.updateCustomization(aiTransaction = state.aiTransaction, reminder = it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldPrimary
                            )
                        )
                    }
 
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
 
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF3F51B5).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cable ,
                                contentDescription = null,
                                tint = Color(0xFF3F51B5),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = AppText.get("profile.options.aiTransaction.title", "AI Based Transaction"),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = AppText.get("profile.options.aiTransaction.subtitle", "Add transaction with AI"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.aiTransaction,
                            onCheckedChange = { 
                                Log.d("ProfileActivity", "AI Based Transaction switch toggled: $it")
                                viewModel?.updateCustomization(aiTransaction = it, reminder = state.reminder)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldPrimary
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))

                    ProfileOptionRow(
                        title = AppText.get("profile.options.feedback.title", "Help us improve"),
                        subtitle = AppText.get("profile.options.feedback.subtitle", "Share your feedback & suggest features"),
                        icon = Icons.Default.Feedback,
                        iconColor = Color(0xFF00ACC1),
                        onClick = { showFeedbackDialog = true }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Section: Security & System Panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                ProfileOptionRow(
                    title = AppText.get("profile.options.logout.title", "Log Out"),
                    subtitle = AppText.get("profile.options.logout.subtitle", "Safely exit your account"),
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    iconColor = ColorExpense,
                    textColor = ColorExpense,
                    showArrow = false,
                    onClick = { showLogoutConfirm = true }
                )
            }
        }
    }

    // 1. EDIT PROFILE POPUP DIALOG
    if (showEditProfile) {
        var tempName by remember { mutableStateOf(displayUser.name) }
        var tempEmail by remember { mutableStateOf(displayUser.email) }

        Dialog(
            onDismissRequest = { showEditProfile = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .shadow(16.dp, RoundedCornerShape(28.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = AppText.get("profile.editProfile.title", "Edit Profile Info"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text(AppText.get("profile.editProfile.nameLabel", "Full Name")) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldPrimary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            focusedLabelColor = EmeraldPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = tempEmail,
                        onValueChange = { tempEmail = it },
                        label = { Text(AppText.get("profile.editProfile.emailLabel", "Email Address")) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = EmeraldPrimary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            focusedLabelColor = EmeraldPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showEditProfile = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(AppText.get("profile.editProfile.cancelButton", "Cancel"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (tempName.isNotBlank() && tempEmail.isNotBlank()) {
                                    val newUser = displayUser.copy(name = tempName, email = tempEmail)
                                    sessionManager?.saveUser(newUser)
                                    currentUserState = newUser
                                    showEditProfile = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(AppText.get("profile.editProfile.saveButton", "Save Changes"), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // 2. PAYMENT MODES MANAGER BOTTOM SHEET
    if (showPaymentModes) {
        ManagerSheet(
            title = AppText.get("profile.paymentModes.title", "Payment Modes"),
            subtitle = AppText.get("profile.paymentModes.subtitle", "Cards, banks & wallets you pay with"),
            icon = Icons.Default.CreditCard,
            accent = Color(0xFF0288D1),
            itemNoun = AppText.get("profile.paymentModes.itemNoun", "payment mode"),
            itemNounPlural = AppText.get("profile.paymentModes.itemNounPlural", "payment modes"),
            placeholder = AppText.get("profile.paymentModes.placeholder", "e.g. UPI, ICICI Card..."),
            types = listOf(
                ManagerType("PAY_NOW", AppText.get("profile.paymentModes.types.payNow", "Pay Now"), Icons.Default.AccountBalanceWallet, Color(0xFF0288D1)),
                ManagerType("PAY_LATER", AppText.get("profile.paymentModes.types.payLater", "Pay Later"), Icons.Default.CreditCard, Color(0xFFE91E63))
            ),
            items = paymentModes.map { ManagerItem(it.id, it.name, it.type) },
            onAdd = { name, type -> viewModel?.savePaymentMode(name, type) },
            onDelete = { id -> viewModel?.deletePaymentMode(id) },
            onDismiss = { showPaymentModes = false }
        )
    }

    // 3. CATEGORIES MANAGER BOTTOM SHEET
    if (showCategoriesManager) {
        ManagerSheet(
            title = AppText.get("profile.categoryManager.title", "Categories"),
            subtitle = AppText.get("profile.categoryManager.subtitle", "Organise your expenses & income"),
            icon = Icons.Default.Category,
            accent = Color(0xFFAB47BC),
            itemNoun = AppText.get("profile.categoryManager.itemNoun", "category"),
            itemNounPlural = AppText.get("profile.categoryManager.itemNounPlural", "categories"),
            placeholder = AppText.get("profile.categoryManager.placeholder", "e.g. Gift, Fuel..."),
            types = listOf(
                ManagerType("EXPENSE", AppText.get("profile.categoryManager.types.expense", "Expense"), Icons.AutoMirrored.Filled.TrendingDown, ColorExpense),
                ManagerType("INCOME", AppText.get("profile.categoryManager.types.income", "Income"), Icons.AutoMirrored.Filled.TrendingUp, ColorIncome)
            ),
            items = categories.map { ManagerItem(it.id, it.name, it.type) },
            onAdd = { name, type -> viewModel?.saveCategory(name, type) },
            onDelete = { id -> viewModel?.deleteCategory(id) },
            onDismiss = { showCategoriesManager = false }
        )
    }

    // 4. APP SETTINGS DIALOG (Dark Theme & Currency Selector)
    if (showAppSettings) {
        Dialog(
            onDismissRequest = { showAppSettings = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .shadow(16.dp, RoundedCornerShape(28.dp))
                    .border(1.dp, Color(0xFF558B2F).copy(alpha = 0.3f), RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = AppText.get("profile.appSettings.title", "App Settings"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // Dark Theme Switch Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = Color(0xFF558B2F))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(AppText.get("profile.appSettings.darkThemeLabel", "Dark Theme"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Switch(
                            checked = isDarkModeEnabled,
                            onCheckedChange = { isDarkModeEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF558B2F)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Currency Selector Row
                    Text(
                        text = AppText.get("profile.appSettings.currencyLabel", "Preferred Currency:"),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val currencies = listOf("USD ($)", "INR (₹)", "EUR (€)", "GBP (£)")
                    // Display labels only; selection logic keeps using the fixed values above
                    val currencyLabels = AppText.list("profile.appSettings.currencies", listOf("USD ($)", "INR (₹)", "EUR (€)", "GBP (£)"))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currencies.forEachIndexed { index, cur ->
                            val isSel = selectedCurrency == cur
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedCurrency = cur },
                                color = if (isSel) Color(0xFF558B2F).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (isSel) Color(0xFF558B2F) else Color.Transparent)
                            ) {
                                Text(
                                    text = currencyLabels.getOrElse(index) { cur },
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSel) Color(0xFF558B2F) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showAppSettings = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF558B2F)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(AppText.get("profile.appSettings.saveButton", "Save & Close"), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 5. LOGOUT CONFIRMATION DIALOG
    if (showLogoutConfirm) {
        Dialog(
            onDismissRequest = { showLogoutConfirm = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .shadow(16.dp, RoundedCornerShape(24.dp))
                    .border(1.dp, ColorExpense.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(ColorExpense.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = ColorExpense,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = AppText.get("profile.logout.title", "Are you sure?"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = AppText.get("profile.logout.message", "Logging out will securely exit your profile. Your local sessions will be cleared."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showLogoutConfirm = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(AppText.get("profile.logout.cancelButton", "Cancel"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                showLogoutConfirm = false
                                onLogout()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ColorExpense),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(AppText.get("profile.logout.confirmButton", "Log Out"), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // 6. HELP US IMPROVE DIALOG
    if (showFeedbackDialog) {
        FeedbackDialog(
            onSubmit = { rating, message -> viewModel?.submitFeedback(rating, message) },
            onDismiss = { showFeedbackDialog = false }
        )
    }

    if (state.isLoading) {
        AppUtils().ShowProgressAnimation()
    }

}

@Composable
fun ProfileOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    showArrow: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (showArrow) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileActivityPreview() {
    ExpentTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ProfileScreen(
                state = ProfileState(),
                currentUser = User(
                    id = "1",
                    name = "John Doe",
                    email = "john.doe@example.com",
                    accessToken = "preview_token",
                    refreshToken = "preview_refresh",
                    onboardingStep = 1
                )
            )
        }
    }
}