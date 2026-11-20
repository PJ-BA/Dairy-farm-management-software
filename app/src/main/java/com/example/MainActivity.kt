package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.GoogleAuthHelper
import com.example.notifications.FeedingReminderManager
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FeedingScreen
import com.example.ui.screens.FinanceScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MilkScreen
import com.example.ui.screens.ReportsBottomSheet
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.state.FarmAnalytics
import com.example.ui.theme.DairyLedgerTheme
import com.example.ui.viewmodel.DairyDashboardData
import com.example.ui.viewmodel.DairyViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FeedingReminderManager.ensureChannel(this)

        setContent {
            DairyLedgerTheme {
                DairyAppNavigation()
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

private data class NavDestination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val FARM_DESTINATIONS = listOf(
    NavDestination("Home", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    NavDestination("Milk", Icons.Rounded.WaterDrop, Icons.Outlined.WaterDrop, "nav_milk"),
    NavDestination("Feeding", Icons.Rounded.Restaurant, Icons.Outlined.Restaurant, "nav_feeding"),
    NavDestination("Finance", Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_finance"),
    NavDestination("Settings", Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings")
)

@Composable
fun DairyAppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var isSplashDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            GoogleAuthHelper.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
        delay(650L)
        isSplashDone = true
    }

    if (!isSplashDone) {
        SplashScreen()
    } else if (currentUser == null) {
        LoginScreen(onAuthSuccess = {})
    } else {
        val user = currentUser!!
        key(user.uid) {
            AuthenticatedDairyShell(
                userId = user.uid,
                userName = user.displayName.orEmpty(),
                userEmail = user.email.orEmpty()
            )
        }
    }
}

@Composable
private fun AuthenticatedDairyShell(
    userId: String,
    userName: String,
    userEmail: String
) {
    val context = LocalContext.current
    val viewModel: DairyViewModel = viewModel(
        key = userId,
        factory = DairyViewModel.provideFactory(
            context = context,
            userId = userId,
            displayName = userName,
            email = userEmail
        )
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var milkSubTab by rememberSaveable { mutableIntStateOf(0) }
    var addMilkTrigger by rememberSaveable { mutableIntStateOf(0) }
    var addCattleTrigger by rememberSaveable { mutableIntStateOf(0) }
    var addTxTrigger by rememberSaveable { mutableIntStateOf(0) }
    var showReportsSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        val mainContent: @Composable (Modifier) -> Unit = { contentModifier ->
            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(modifier = contentModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Loading your farm ledger...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = contentModifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Unable to sync farm records",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Button(onClick = { selectedTab = 0 }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is UiState.Success -> {
                    val data: DairyDashboardData = state.data
                    when (selectedTab) {
                        0 -> DashboardScreen(
                            data = data,
                            onNavigateTab = { selectedTab = it },
                            onQuickLogMilk = {
                                milkSubTab = 0
                                selectedTab = 1
                                addMilkTrigger++
                            },
                            onQuickAddCattle = {
                                milkSubTab = 1
                                selectedTab = 1
                                addCattleTrigger++
                            },
                            onQuickLogTransaction = {
                                selectedTab = 3
                                addTxTrigger++
                            },
                            onQuickCompleteSchedule = { schedule ->
                                val feed = data.feedItems.firstOrNull { it.id == schedule.feedId }
                                if (feed != null) {
                                    viewModel.completeFeedingActivity(
                                        schedule = schedule,
                                        feedItem = feed,
                                        quantityConsumed = schedule.quantity,
                                        date = FarmAnalytics.todayDateStr(),
                                        time = FarmAnalytics.currentTimeStr(),
                                        notes = "Logged from Dashboard"
                                    )
                                } else {
                                    selectedTab = 2
                                }
                            },
                            modifier = contentModifier
                        )
                        1 -> MilkScreen(
                            data = data,
                            initialSubTab = milkSubTab,
                            openAddMilkTrigger = addMilkTrigger,
                            openAddCattleTrigger = addCattleTrigger,
                            onSaveCattle = viewModel::saveCattle,
                            onDeleteCattle = viewModel::deleteCattle,
                            onSaveMilkRecord = viewModel::saveMilkRecord,
                            onDeleteMilkRecord = viewModel::deleteMilkRecord,
                            modifier = contentModifier
                        )
                        2 -> FeedingScreen(
                            data = data,
                            onSaveFeedItem = viewModel::saveFeedItem,
                            onRestockFeedItem = viewModel::restockFeedItem,
                            onDeleteFeedItem = viewModel::deleteFeedItem,
                            onSaveSchedule = viewModel::saveFeedingSchedule,
                            onDeleteSchedule = viewModel::deleteFeedingSchedule,
                            onCompleteFeeding = viewModel::completeFeedingActivity,
                            onMarkMissed = viewModel::markFeedingMissed,
                            modifier = contentModifier
                        )
                        3 -> FinanceScreen(
                            data = data,
                            openAddTxTrigger = addTxTrigger,
                            onSaveTransaction = viewModel::saveTransaction,
                            onDeleteTransaction = viewModel::deleteTransaction,
                            onOpenReports = { showReportsSheet = true },
                            modifier = contentModifier
                        )
                        4 -> SettingsScreen(
                            data = data,
                            userEmail = userEmail,
                            userName = userName,
                            onSaveProfile = viewModel::updateProfile,
                            onOpenReports = { showReportsSheet = true },
                            modifier = contentModifier
                        )
                    }

                    if (showReportsSheet) {
                        ReportsBottomSheet(
                            data = data,
                            onDismiss = { showReportsSheet = false }
                        )
                    }
                }
            }
        }

        if (useRail) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    windowInsets = WindowInsets.safeDrawing
                ) {
                    FARM_DESTINATIONS.forEachIndexed { index, dest ->
                        NavigationRailItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == index) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label
                                )
                            },
                            label = { Text(dest.label) },
                            modifier = Modifier.testTag(dest.testTag)
                        )
                    }
                }
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    contentWindowInsets = WindowInsets.statusBars
                ) { innerPadding ->
                    mainContent(Modifier.padding(innerPadding))
                }
            }
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets.statusBars,
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        FARM_DESTINATIONS.forEachIndexed { index, dest ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == index) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, maxLines = 1) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                mainContent(
                    Modifier
                        .padding(innerPadding)
                        .windowInsetsPadding(WindowInsets(0))
                )
            }
        }
    }
}
