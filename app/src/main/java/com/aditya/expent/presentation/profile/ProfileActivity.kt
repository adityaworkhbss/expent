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
                            sessionManager.logout()
                            val intent = Intent(this, AuthActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            finish()
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
    // Current user state loaded directly from sessionManager or passed preview user
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
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Profile Settings",
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
                text = "Account Management",
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
                        title = "Payment Modes",
                        subtitle = "Manage credit cards, banks & cash wallets",
                        icon = Icons.Default.CreditCard,
                        iconColor = Color(0xFF0288D1),
                        onClick = { showPaymentModes = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                    ProfileOptionRow(
                        title = "Categories Manager",
                        subtitle = "Customise expense & income categories",
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
                text = "Preferences",
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
                                text = "Daily Reminders",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Daily notifications to log expenses",
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
                                text = "AI Based Transaction",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Add transaction with AI",
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
                        title = "Help us improve",
                        subtitle = "Share your feedback & suggest features",
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
                    title = "Log Out",
                    subtitle = "Safely exit your account",
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
                        text = "Edit Profile Info",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Full Name") },
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
                        label = { Text("Email Address") },
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
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
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
                            Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // 2. PAYMENT MODES MANAGER DIALOG
    if (showPaymentModes) {
        var newPaymentModeText by remember { mutableStateOf("") }
        var newPaymentModeType by remember { mutableStateOf("PAY_NOW") }

        Dialog(
            onDismissRequest = { showPaymentModes = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.7f)
                    .shadow(16.dp, RoundedCornerShape(28.dp))
                    .border(1.dp, Color(0xFF0288D1).copy(alpha = 0.3f), RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Payment Modes Manager",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Input & selector column to add new payment mode
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newPaymentModeText,
                                onValueChange = { newPaymentModeText = it },
                                placeholder = { Text("e.g. UPI, ICICI Card...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF0288D1)
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            IconButton(
                                onClick = {
                                    if (newPaymentModeText.isNotBlank()) {
                                        viewModel?.savePaymentMode(newPaymentModeText.trim(), newPaymentModeType)
                                        newPaymentModeText = ""
                                    }
                                },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF0288D1), contentColor = Color.White),
                                modifier = Modifier.size(50.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Mode")
                            }
                        }

                        // Type selector for new payment mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("PAY_NOW", "PAY_LATER").forEach { type ->
                                val isSel = newPaymentModeType == type
                                Surface(
                                    onClick = { newPaymentModeType = type },
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSel) Color(0xFF0288D1) else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    color = if (isSel) Color(0xFF0288D1).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSel) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF0288D1),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = if (type == "PAY_NOW") "Pay Now" else "Pay Later",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color(0xFF0288D1) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Available Cards & Accounts:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // List of current cards
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (paymentModes.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No payment methods added yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(paymentModes) { mode ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = mode.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            color = if (mode.type == "PAY_LATER") Color(0xFFE91E63).copy(alpha = 0.15f) else Color(0xFF0288D1).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (mode.type == "PAY_LATER") "PAY LATER" else "PAY NOW",
                                                color = if (mode.type == "PAY_LATER") Color(0xFFE91E63) else Color(0xFF0288D1),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel?.deletePaymentMode(mode.id)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = ColorExpense,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showPaymentModes = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 3. CATEGORIES MANAGER DIALOG
    if (showCategoriesManager) {
        var newCategoryText by remember { mutableStateOf("") }
        var newCategoryType by remember { mutableStateOf("EXPENSE") }

        Dialog(
            onDismissRequest = { showCategoriesManager = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.72f)
                    .shadow(16.dp, RoundedCornerShape(28.dp))
                    .border(1.dp, Color(0xFFAB47BC).copy(alpha = 0.3f), RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Categories Manager",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Input & selector column to add new category
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newCategoryText,
                                onValueChange = { newCategoryText = it },
                                placeholder = { Text("e.g. Gift, Fuel...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFAB47BC)
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            IconButton(
                                onClick = {
                                    if (newCategoryText.isNotBlank()) {
                                        viewModel?.saveCategory(newCategoryText.trim(), newCategoryType)
                                        newCategoryText = ""
                                    }
                                },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFAB47BC), contentColor = Color.White),
                                modifier = Modifier.size(50.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Category")
                            }
                        }

                        // Type selector for new category
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
                                            color = if (isSel) Color(0xFFAB47BC) else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    color = if (isSel) Color(0xFFAB47BC).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSel) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFFAB47BC),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = if (type == "EXPENSE") "Expense" else "Income",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color(0xFFAB47BC) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Current Categories:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (categories.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No categories added yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(categories) { cat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (cat.type == "INCOME") ColorIncome.copy(alpha = 0.15f)
                                                    else ColorExpense.copy(alpha = 0.15f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (cat.type == "INCOME") Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                                contentDescription = null,
                                                tint = if (cat.type == "INCOME") ColorIncome else ColorExpense,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = cat.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Surface(
                                                color = if (cat.type == "INCOME") ColorIncome.copy(alpha = 0.15f) else ColorExpense.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = if (cat.type == "INCOME") "INCOME" else "EXPENSE",
                                                    color = if (cat.type == "INCOME") ColorIncome else ColorExpense,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel?.deleteCategory(cat.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = ColorExpense,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showCategoriesManager = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAB47BC)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
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
                        text = "App Settings",
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
                            Text("Dark Theme", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
                        text = "Preferred Currency:",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val currencies = listOf("USD ($)", "INR (₹)", "EUR (€)", "GBP (£)")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currencies.forEach { cur ->
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
                                    text = cur,
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
                        Text("Save & Close", color = Color.White, fontWeight = FontWeight.Bold)
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
                        text = "Are you sure?",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Logging out will securely exit your profile. Your local sessions will be cleared.",
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
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
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
                            Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // 6. HELP US IMPROVE DIALOG
    if (showFeedbackDialog) {
        var feedbackRating by remember { mutableStateOf(5) }
        var feedbackText by remember { mutableStateOf("") }
        var feedbackSubmittedState by remember { mutableStateOf(false) }

        Dialog(
            onDismissRequest = { showFeedbackDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .shadow(16.dp, RoundedCornerShape(28.dp))
                    .border(1.dp, Color(0xFF00ACC1).copy(alpha = 0.3f), RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!feedbackSubmittedState) {
                        Text(
                            text = "Help Us Improve",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "How would you rate your experience so far?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Star rating selector
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            (1..5).forEach { starIndex ->
                                val isSelected = starIndex <= feedbackRating
                                val starScale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.2f else 1f,
                                    label = "StarScale"
                                )
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Star $starIndex",
                                    tint = if (isSelected) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .scale(starScale)
                                        .clickable { feedbackRating = starIndex }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = feedbackText,
                            onValueChange = { feedbackText = it },
                            label = { Text("What can we improve?") },
                            placeholder = { Text("Describe your issues or suggest new features here...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00ACC1),
                                focusedLabelColor = Color(0xFF00ACC1)
                            ),
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showFeedbackDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    feedbackSubmittedState = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00ACC1)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Submit", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Success State
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Thank You!",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Your feedback is incredibly valuable to us. We will use it to build a better experience for everyone!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { showFeedbackDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00ACC1)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
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