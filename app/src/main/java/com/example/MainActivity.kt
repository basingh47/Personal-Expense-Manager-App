package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.AppLockManager
import com.example.sms.SmsNotificationHelper
import com.example.subscription.SubscriptionNotificationHelper
import com.example.ui.*
import com.example.ui.theme.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private val viewModel: FinanceViewModel by viewModels()
    private lateinit var appLockManager: AppLockManager

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (::appLockManager.isInitialized) {
            appLockManager.onUserInteraction()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::appLockManager.isInitialized) {
            appLockManager.onAppForegrounded()
            appLockManager.applyPrivacyFlag(this)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::appLockManager.isInitialized) {
            appLockManager.onAppBackgrounded()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appLockManager = AppLockManager.getInstance(this)
        appLockManager.applyPrivacyFlag(this)

        SmsNotificationHelper.createNotificationChannel(this)
        SubscriptionNotificationHelper.createNotificationChannel(this)
        viewModel.triggerUpcomingSubscriptionCheck(this)
        
        val initialScreen = intent?.getStringExtra("OPEN_SCREEN") ?: "dashboard"

        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val sharedPref = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = remember {
                val hasSaved = sharedPref.contains("is_dark_theme")
                val defaultVal = if (hasSaved) sharedPref.getBoolean("is_dark_theme", systemDark) else systemDark
                mutableStateOf(defaultVal)
            }

            val isPinLockEnabled by appLockManager.isPinLockEnabled.collectAsStateWithLifecycle()
            val isAppLocked by appLockManager.isAppLocked.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = isDarkTheme.value) {
                if (isPinLockEnabled && isAppLocked) {
                    PinLockScreen(appLockManager = appLockManager)
                } else {
                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    val scope = rememberCoroutineScope()
                    var currentScreen by remember { mutableStateOf(initialScreen) }
                    var showAuthDialog by remember { mutableStateOf(false) }
                    val currentUserState by viewModel.currentUser.collectAsState()

                val screens = listOf(
                    DrawerItem("dashboard", "Dashboard Overview", Icons.Default.Dashboard),
                    DrawerItem("transactions", "Transactions Ledger", Icons.Default.Receipt),
                    DrawerItem("bank_accounts", "Bank Accounts", Icons.Default.AccountBalance),
                    DrawerItem("categories", "Category Manager", Icons.Default.Category),
                    DrawerItem("budgets", "Monthly Budgets", Icons.Default.PieChart),
                    DrawerItem("assets", "Assets & Vehicles", Icons.Default.TwoWheeler),
                    DrawerItem("subscriptions", "Subscription Tracker", Icons.Default.CloudSync),
                    DrawerItem("credit_cards", "Credit Cards Tracker", Icons.Default.CreditCard),
                    DrawerItem("savings", "Savings & Goals", Icons.Default.Savings),
                    DrawerItem("borrow_lend", "Borrow & Lend Book", Icons.Default.Handshake),
                    DrawerItem("wishlist", "Wishlist (Not Bought)", Icons.Default.CardGiftcard),
                    DrawerItem("analytics", "Analytics & Insights", Icons.Default.QueryStats),
                    DrawerItem("settings", "App Settings", Icons.Default.Settings)
                )

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = DarkSurface,
                            modifier = Modifier.width(300.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Scrollable portion
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    // Drawer Banner Header
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(PrimaryEmerald, DarkBackground)
                                                )
                                            )
                                            .padding(24.dp),
                                        contentAlignment = Alignment.BottomStart
                                    ) {
                                        Column {
                                            Text(
                                                text = "Expense Manager",
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = AccentGold,
                                                letterSpacing = 0.5.sp
                                            )
                                            Text(
                                                text = "Personal Expense Manager",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SmoothWhite,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Drawer items
                                    screens.forEach { item ->
                                        val isSelected = currentScreen == item.id
                                        NavigationDrawerItem(
                                            label = { 
                                                Text(
                                                    text = item.label, 
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 14.sp
                                                ) 
                                            },
                                            selected = isSelected,
                                            onClick = {
                                                currentScreen = item.id
                                                scope.launch { drawerState.close() }
                                            },
                                            icon = { 
                                                Icon(
                                                    imageVector = item.icon, 
                                                    contentDescription = null,
                                                    tint = if (isSelected) AccentGold else MutedText
                                                ) 
                                            },
                                            modifier = Modifier
                                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                                .testTag("drawer_item_${item.id}"),
                                            colors = NavigationDrawerItemDefaults.colors(
                                                unselectedContainerColor = Color.Transparent,
                                                selectedContainerColor = DarkSurfaceVariant,
                                                selectedTextColor = SmoothWhite,
                                                unselectedTextColor = MutedText
                                            )
                                        )
                                    }
                                }

                                // Sticky User Info Footer
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (currentUserState == null) {
                                                showAuthDialog = true
                                            }
                                        }
                                        .padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val user = currentUserState
                                    if (user != null) {
                                        val initial = if (user.email != null && user.email!!.length >= 2) {
                                            user.email!!.substring(0, 2).uppercase()
                                        } else {
                                            "U"
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(PrimaryEmerald),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(initial, fontWeight = FontWeight.Bold, color = SmoothWhite, fontSize = 14.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(user.email ?: "Cloud User", style = MaterialTheme.typography.bodySmall, color = SmoothWhite, fontWeight = FontWeight.Bold, maxLines = 1)
                                            Text("Cloud Synced \u2705", style = MaterialTheme.typography.labelSmall, color = PrimaryEmerald)
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.logout()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                                contentDescription = "Log Out",
                                                tint = RedExpense,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(DarkSurfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text("Sync to Cloud", style = MaterialTheme.typography.bodySmall, color = AccentGold, fontWeight = FontWeight.Bold)
                                            Text("Log in to secure data", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = {
                                    Text(
                                        text = screens.firstOrNull { it.id == currentScreen }?.label ?: "Expense Manager",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = SmoothWhite
                                    )
                                },
                                navigationIcon = {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } },
                                        modifier = Modifier.testTag("navigation_drawer_toggle")
                                    ) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = SmoothWhite)
                                    }
                                },
                                actions = {
                                    IconButton(
                                        onClick = {
                                            val newValue = !isDarkTheme.value
                                            isDarkTheme.value = newValue
                                            sharedPref.edit().putBoolean("is_dark_theme", newValue).commit()
                                        },
                                        modifier = Modifier.testTag("theme_toggle_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isDarkTheme.value) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = "Toggle Theme",
                                            tint = SmoothWhite
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = DarkBackground
                                )
                            )
                        },
                        containerColor = DarkBackground
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentScreen) {
                                "dashboard" -> DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateTo = { currentScreen = it }
                                )
                                "transactions" -> TransactionsScreen(viewModel = viewModel)
                                "bank_accounts" -> BankAccountsScreen(viewModel = viewModel)
                                "categories" -> CategoriesScreen(viewModel = viewModel)
                                "budgets" -> BudgetsScreen(viewModel = viewModel)
                                "assets" -> AssetsScreen(viewModel = viewModel)
                                "subscriptions" -> SubscriptionsScreen(viewModel = viewModel)
                                "credit_cards" -> CreditCardsScreen(viewModel = viewModel)
                                "savings" -> SavingsGoalsScreen(viewModel = viewModel)
                                "borrow_lend" -> BorrowLendScreen(viewModel = viewModel)
                                "wishlist" -> WishlistScreen(viewModel = viewModel)
                                "analytics" -> AnalyticsScreen(viewModel = viewModel)
                                "settings" -> SettingsScreen(viewModel = viewModel, appLockManager = appLockManager)
                            }
                        }
                    }
                }

                // Global Delete Confirmation Dialog
                val deleteConfirmation by viewModel.deleteConfirmation.collectAsState()
                deleteConfirmation?.let { request ->
                    AlertDialog(
                        onDismissRequest = { viewModel.dismissDeleteConfirmation() },
                        title = {
                            Text(
                                text = request.title,
                                fontWeight = FontWeight.Bold,
                                color = SmoothWhite
                            )
                        },
                        text = {
                            Text(
                                text = request.message,
                                color = MutedText,
                                fontSize = 14.sp
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    request.onConfirm()
                                    viewModel.dismissDeleteConfirmation()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RedExpense),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Delete", color = SmoothWhite, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            OutlinedButton(
                                onClick = { viewModel.dismissDeleteConfirmation() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SmoothWhite),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel")
                            }
                        },
                        containerColor = DarkSurface,
                        shape = RoundedCornerShape(20.dp),
                        tonalElevation = 6.dp
                    )
                }

                if (showAuthDialog) {
                    AuthDialog(
                        viewModel = viewModel,
                        onDismiss = { showAuthDialog = false }
                    )
                }
                }
            }
        }
    }
}

@Composable
fun AuthDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = {
            Text(
                text = if (isLoginMode) "Log In to Sync" else "Create Sync Account",
                fontWeight = FontWeight.Bold,
                color = SmoothWhite,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isLoginMode) 
                        "Sign in to synchronize your expense records, assets, budgets, and cards with the secure cloud database so you never lose your data." 
                        else "Register an account to securely sync your transactions across multiple devices.",
                    color = MutedText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = RedExpense,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MutedText) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SmoothWhite,
                        unfocusedTextColor = SmoothWhite,
                        focusedBorderColor = PrimaryEmerald,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedLabelColor = PrimaryEmerald,
                        unfocusedLabelColor = MutedText
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("auth_email_input"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !loading
                )

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text("Password (min 6 chars)") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MutedText) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = MutedText
                            )
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SmoothWhite,
                        unfocusedTextColor = SmoothWhite,
                        focusedBorderColor = PrimaryEmerald,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedLabelColor = PrimaryEmerald,
                        unfocusedLabelColor = MutedText
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !loading
                )

                // Mode switch
                TextButton(
                    onClick = { 
                        isLoginMode = !isLoginMode
                        errorMessage = null
                    },
                    modifier = Modifier.align(Alignment.End).testTag("auth_mode_toggle"),
                    enabled = !loading
                ) {
                    Text(
                        text = if (isLoginMode) "New user? Create account" else "Already have an account? Log in",
                        color = AccentGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (email.isBlank() || password.length < 6) {
                        errorMessage = "Please enter a valid email and minimum 6-character password"
                        return@Button
                    }
                    loading = true
                    errorMessage = null
                    if (isLoginMode) {
                        viewModel.signIn(email, password, 
                            onSuccess = {
                                loading = false
                                onDismiss()
                            },
                            onFailure = { err ->
                                loading = false
                                errorMessage = err
                            }
                        )
                    } else {
                        viewModel.signUp(email, password,
                            onSuccess = {
                                loading = false
                                onDismiss()
                            },
                            onFailure = { err ->
                                loading = false
                                errorMessage = err
                            }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("auth_submit_button"),
                enabled = !loading
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = SmoothWhite,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isLoginMode) "Log In" else "Sign Up",
                        color = SmoothWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            if (!loading) {
                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SmoothWhite),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(24.dp)
    )
}

data class DrawerItem(
    val id: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
