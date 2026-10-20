package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ShopViewModel
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val viewModel: ShopViewModel by viewModels {
        val repo = (application as? AslamRecordApp)?.repository
            ?: com.example.data.repository.ShopRepository(
                com.example.data.db.AppDatabase.getDatabase(this)
            )
        ShopViewModel.Factory(repo)
    }

    companion object {
        var welcomeShownThisSession = false
    }

    fun launchBiometricPrompt(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (isFinishing || isDestroyed) return
        try {
            val executor = ContextCompat.getMainExecutor(this)
            val biometricPrompt = BiometricPrompt(
                this,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                            onError("Unlock cancelled. Tap button to retry.")
                        } else if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            onError("Use PIN or retry biometric scan.")
                        } else if (errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                            errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                            errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE
                        ) {
                            onError("Biometric sensor not enrolled. Tap 'Unlock Shop Ledger' below.")
                        } else {
                            onError(errString.toString())
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onError("Fingerprint/Face not recognized. Please try again.")
                    }
                }
            )

            try {
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock ASLAM RECORD")
                    .setSubtitle("Scan fingerprint or face to open ledger")
                    .setDescription("Securing Bidi purchase, sales, and khata records")
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                    .build()
                biometricPrompt.authenticate(promptInfo)
            } catch (_: Throwable) {
                try {
                    val fallbackPrompt = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Unlock ASLAM RECORD")
                        .setSubtitle("Scan fingerprint or face to open ledger")
                        .setDescription("Securing Bidi purchase, sales, and khata records")
                        .setNegativeButtonText("Use PIN")
                        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
                        .build()
                    biometricPrompt.authenticate(fallbackPrompt)
                } catch (e2: Throwable) {
                    onError("Biometric security: ${e2.message ?: "Sensor unavailable"}. Tap 'Unlock Shop Ledger' below.")
                }
            }
        } catch (e: Throwable) {
            onError("Biometric sensor: ${e.message ?: "Unavailable"}. Tap 'Unlock Shop Ledger' below.")
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Dark Mode and White (Light) Mode State (survives recreation/process death)
            var isDarkMode by rememberSaveable { mutableStateOf(false) }

            // Biometric App Lock State: locked by default on startup
            var isAppLocked by rememberSaveable { mutableStateOf(true) }
            var biometricErrorMessage by remember { mutableStateOf<String?>(null) }

            // Welcome Animation State: only show on fresh app startup, never when resuming
            var showWelcomeAnimation by rememberSaveable { mutableStateOf(!welcomeShownThisSession) }

            // Automatically launch Biometric authentication prompt when app is locked on startup
            LaunchedEffect(isAppLocked) {
                if (isAppLocked) {
                    launchBiometricPrompt(
                        onSuccess = {
                            isAppLocked = false
                            biometricErrorMessage = null
                        },
                        onError = { err ->
                            biometricErrorMessage = err
                        }
                    )
                }
            }

            MyApplicationTheme(darkTheme = isDarkMode) {
                // Persist last active screen across configuration changes and process death,
                // so returning to the app restores the screen the user was on.
                val screenSaver = Saver<Screen, String>(
                    save = { it.route },
                    restore = { route ->
                        listOf(
                            Screen.Dashboard, Screen.RecordSale, Screen.Products,
                            Screen.Customers, Screen.Reports, Screen.AslamAi, Screen.Calculator
                        ).firstOrNull { it.route == route } ?: Screen.Dashboard
                    }
                )
                var currentScreen by rememberSaveable(stateSaver = screenSaver) {
                    mutableStateOf<Screen>(Screen.Dashboard)
                }
                var aiInitialPrompt by rememberSaveable { mutableStateOf<String?>(null) }
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                // Auto-dismiss welcome animation after 2.2 seconds or let user tap to dismiss
                LaunchedEffect(Unit) {
                    if (showWelcomeAnimation) {
                        delay(2200)
                        showWelcomeAnimation = false
                        welcomeShownThisSession = true
                    }
                }

                // Hardware back button handles lock screen, drawer or returns to Dashboard
                BackHandler(enabled = isAppLocked || drawerState.isOpen || currentScreen != Screen.Dashboard) {
                    when {
                        isAppLocked -> finish()
                        drawerState.isOpen -> scope.launch { drawerState.close() }
                        else -> currentScreen = Screen.Dashboard
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            modifier = Modifier.width(310.dp),
                            drawerContainerColor = MaterialTheme.colorScheme.surface
                        ) {
                            // Sidebar Drawer Header with 3D gradient
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.surface
                                            )
                                        )
                                    )
                                    .padding(20.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(50.dp),
                                            shadowElevation = 4.dp
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Storefront,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = "ASLAM RECORD",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Smart Shop & Bahi-Khata",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Sidebar Navigation Options
                            NavigationDrawerItem(
                                label = { Text("Dashboard (Overview)", fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                                selected = currentScreen == Screen.Dashboard,
                                onClick = {
                                    currentScreen = Screen.Dashboard
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .testTag("drawer_item_dashboard")
                            )

                            NavigationDrawerItem(
                                label = { Text("Record New Sale", fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.PointOfSale, contentDescription = null, tint = EmeraldPrimary) },
                                selected = currentScreen == Screen.RecordSale,
                                onClick = {
                                    currentScreen = Screen.RecordSale
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .testTag("drawer_item_new_sale")
                            )

                            NavigationDrawerItem(
                                label = { Text("Products & Inventory", fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                                selected = currentScreen == Screen.Products,
                                onClick = {
                                    currentScreen = Screen.Products
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .testTag("drawer_item_products")
                            )

                            NavigationDrawerItem(
                                label = { Text("Customer Khata Ledger", fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.People, contentDescription = null) },
                                selected = currentScreen == Screen.Customers,
                                onClick = {
                                    currentScreen = Screen.Customers
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .testTag("drawer_item_customers")
                            )

                            NavigationDrawerItem(
                                label = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("ASLAM AI Munshi", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = GoldContainer
                                        ) {
                                            Text(
                                                "GEMINI",
                                                color = GoldAccent,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                },
                                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldAccent) },
                                selected = currentScreen == Screen.AslamAi,
                                onClick = {
                                    aiInitialPrompt = null
                                    currentScreen = Screen.AslamAi
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .testTag("drawer_item_aslam_ai")
                            )

                            NavigationDrawerItem(
                                label = { Text("Financial Reports", fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                                selected = currentScreen == Screen.Reports,
                                onClick = {
                                    currentScreen = Screen.Reports
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .testTag("drawer_item_reports")
                            )

                            NavigationDrawerItem(
                                label = { Text("Calculator", fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                selected = currentScreen == Screen.Calculator,
                                onClick = {
                                    currentScreen = Screen.Calculator
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .testTag("drawer_item_calculator")
                            )

                            NavigationDrawerItem(
                                label = { Text("Lock Ledger (बायोमेट्रिक लॉक)", fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.Fingerprint, contentDescription = null, tint = StatusGreen) },
                                selected = false,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    isAppLocked = true
                                },
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .testTag("drawer_item_lock_app")
                            )

                            Spacer(modifier = Modifier.weight(1f))
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                            // Sidebar Theme Switcher (Dark Mode / White Mode)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isDarkMode) "Dark Mode" else "White Mode",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }
                                Switch(
                                    checked = isDarkMode,
                                    onCheckedChange = { isDarkMode = it },
                                    modifier = Modifier.testTag("switch_dark_mode")
                                )
                            }
                        }
                    }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
                        topBar = {
                            TopAppBar(
                                navigationIcon = {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } },
                                        modifier = Modifier.testTag("btn_sidebar_drawer_menu")
                                    ) {
                                        Icon(
                                            Icons.Default.Menu,
                                            contentDescription = "Side Bar Menu"
                                        )
                                    }
                                },
                                title = {
                                    Column {
                                        Text(
                                            text = "ASLAM RECORD",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                        Text(
                                            text = currentScreen.title,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                },
                                actions = {
                                    // Lock App button
                                    IconButton(
                                        onClick = { isAppLocked = true },
                                        modifier = Modifier.testTag("top_bar_lock_app")
                                    ) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = "Lock App",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                        )
                                    }

                                    // Quick theme toggle in top bar
                                    IconButton(
                                        onClick = { isDarkMode = !isDarkMode },
                                        modifier = Modifier.testTag("top_bar_theme_toggle")
                                    ) {
                                        Icon(
                                            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = "Toggle Theme"
                                        )
                                    }

                                    // Direct Aslam AI launcher
                                    IconButton(
                                        onClick = {
                                            aiInitialPrompt = null
                                            currentScreen = Screen.AslamAi
                                        },
                                        modifier = Modifier.testTag("top_app_bar_ai_chat")
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = "ASLAM AI Munshi",
                                            tint = GoldAccent
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 4.dp
                            ) {
                                Screen.bottomNavItems.forEach { screen ->
                                    val isSelected = currentScreen == screen
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { currentScreen = screen },
                                        icon = {
                                            Icon(screen.icon, contentDescription = screen.title)
                                        },
                                        label = {
                                            Text(screen.title, fontSize = 11.sp)
                                        },
                                        modifier = Modifier.testTag("nav_item_${screen.route}")
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentScreen) {
                                Screen.Dashboard -> DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToRecordSale = { currentScreen = Screen.RecordSale },
                                    onNavigateToProducts = { currentScreen = Screen.Products },
                                    onNavigateToCustomers = { currentScreen = Screen.Customers },
                                    onNavigateToReports = { currentScreen = Screen.Reports },
                                    onNavigateToAi = { prompt ->
                                        aiInitialPrompt = prompt
                                        currentScreen = Screen.AslamAi
                                    }
                                )
                                Screen.RecordSale -> RecordSaleScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentScreen = Screen.Dashboard }
                                )
                                Screen.Products -> ProductsScreen(
                                    viewModel = viewModel
                                )
                                Screen.Customers -> CustomersScreen(
                                    viewModel = viewModel
                                )
                                Screen.Reports -> ReportsScreen(
                                    viewModel = viewModel
                                )
                                Screen.AslamAi -> AslamAiScreen(
                                    viewModel = viewModel,
                                    initialPrompt = aiInitialPrompt
                                )
                                Screen.Calculator -> CalculatorScreen(
                                    onNavigateBack = { currentScreen = Screen.Dashboard }
                                )
                            }
                        }
                    }
                }

                // Premium Welcome Animation Overlay
                AnimatedVisibility(
                    visible = showWelcomeAnimation,
                    enter = fadeIn(animationSpec = tween(400)),
                    exit = fadeOut(animationSpec = tween(400))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF1E1B4B),
                                        Color(0xFF0F172A),
                                        Color(0xFF0B101E)
                                    )
                                )
                            )
                            .clickable {
                                showWelcomeAnimation = false
                                welcomeShownThisSession = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val glowAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.35f,
                            targetValue = 0.75f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "glow"
                        )
                        val iconScale by infiniteTransition.animateFloat(
                            initialValue = 0.98f,
                            targetValue = 1.03f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )

                        Card(
                            modifier = Modifier
                                .widthIn(max = 330.dp)
                                .padding(24.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF312E81)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Logo icon with violet-indigo glow ring
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.scale(iconScale)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(26.dp),
                                        color = Color(0xFF4F46E5).copy(alpha = glowAlpha * 0.4f),
                                        modifier = Modifier.size(86.dp)
                                    ) {}
                                    Surface(
                                        shape = RoundedCornerShape(22.dp),
                                        color = Color(0xFF4F46E5),
                                        modifier = Modifier.size(72.dp),
                                        shadowElevation = 8.dp
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Storefront,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(40.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = "Welcome to",
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "ASLAM RECORD",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFF8FAFC),
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Smart Digital Bahi-Khata & AI Munshi",
                                    fontSize = 12.sp,
                                    color = Color(0xFF818CF8),
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = {
                                        showWelcomeAnimation = false
                                        welcomeShownThisSession = true
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4F46E5),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text(
                                        "Open Shop Ledger",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Biometric Lock Screen Overlay: intercepts access until unlocked
                AnimatedVisibility(
                    visible = isAppLocked,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    BiometricLockScreen(
                        onUnlockRequested = {
                            launchBiometricPrompt(
                                onSuccess = {
                                    isAppLocked = false
                                    biometricErrorMessage = null
                                },
                                onError = { err ->
                                    biometricErrorMessage = err
                                }
                            )
                        },
                        onBypassUnlock = {
                            isAppLocked = false
                            biometricErrorMessage = null
                        },
                        errorMessage = biometricErrorMessage
                    )
                }
            }
        }
    }
}

@Composable
fun BiometricLockScreen(
    onUnlockRequested: () -> Unit,
    onBypassUnlock: () -> Unit,
    errorMessage: String?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E1B4B),
                        Color(0xFF0F172A),
                        Color(0xFF0B101E)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "lock_pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "lock_scale"
        )
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.7f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "lock_glow"
        )

        Card(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF312E81)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Biometric Fingerprint Icon with Glowing Ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.scale(pulseScale)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF4F46E5).copy(alpha = glowAlpha * 0.35f),
                        modifier = Modifier.size(96.dp)
                    ) {}
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF4F46E5),
                        modifier = Modifier.size(76.dp),
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = "Biometric Lock",
                                tint = Color.White,
                                modifier = Modifier.size(46.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF064E3B),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "BIOMETRIC SECURITY ACTIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD1FAE5),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Text(
                    text = "ASLAM RECORD",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFF8FAFC),
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = "Bidi Business & Accounts Ledger",
                    fontSize = 12.sp,
                    color = Color(0xFF818CF8),
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Confirm fingerprint, face scan, or device PIN to access sensitive financial data.",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF7F1D1D).copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = errorMessage,
                                fontSize = 11.sp,
                                color = Color(0xFFFCA5A5),
                                lineHeight = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onUnlockRequested,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_biometric_unlock")
                ) {
                    Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Scan Fingerprint / Face",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onBypassUnlock,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFCBD5E1)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_device_pin_unlock")
                ) {
                    Icon(
                        Icons.Default.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Unlock Shop Ledger",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
