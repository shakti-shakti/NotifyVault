package com.example

import android.os.Bundle
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.example.data.FilterChipRepository
import com.example.ui.components.AppPickerMode
import com.example.ui.components.AppPickerSheet
import com.example.ui.components.BottomGlassNav
import com.example.ui.screens.CustomizationStudioScreen
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ExclusionRulesScreen
import com.example.ui.screens.LockSetupScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LocalVaultColors
import com.example.ui.theme.LocalVaultCustomization
import com.example.ui.theme.NotifyVaultTheme
import com.example.ui.theme.rememberVaultCustomizationState
import com.example.viewmodel.VaultViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val customizationState = rememberVaultCustomizationState()

            NotifyVaultTheme(customizationState = customizationState) {
                val viewModel: VaultViewModel = viewModel(
                    factory = VaultViewModel.provideFactory(application)
                )
                NotifyVaultApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun NotifyVaultApp(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val colors = LocalVaultColors.current
    val lockManager = remember { com.example.security.LockManager.getInstance(context) }
    val isLocked by viewModel.isVaultLocked.collectAsStateWithLifecycle()
    val isSecureRecents by lockManager.hideRecentsFlow.collectAsStateWithLifecycle()
    val chipRepository = remember { FilterChipRepository.getInstance(context) }
    val hasPromptedQuickChips by chipRepository.hasPromptedQuickChipsSetup.collectAsStateWithLifecycle()

    var currentRoute by remember { mutableStateOf("vault") }
    var selectedNotificationId by remember { mutableLongStateOf(1L) }
    var isOnboardingCompleted by remember { mutableStateOf(true) }

    // Dynamically apply or remove FLAG_SECURE
    DisposableEffect(isSecureRecents) {
        val window = (context as? ComponentActivity)?.window
        if (isSecureRecents) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose { }
    }

    // Process-level lifecycle observer ensures cold starts and background returns
    // are handled even when Android recreates the activity.
    val processLifecycleOwner = ProcessLifecycleOwner.get()
    DisposableEffect(processLifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    viewModel.checkNotificationPermission()
                    if (lockManager.shouldLockOnResume()) viewModel.lockVault()
                }
                Lifecycle.Event.ON_STOP -> lockManager.recordAppPaused()
                else -> Unit
            }
        }
        processLifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            processLifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Lock immediately when the display turns off, independent of app process state.
    DisposableEffect(lockManager) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(receiverContext: android.content.Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF && lockManager.isLockOnScreenOff()) {
                    viewModel.lockVault()
                    lockManager.lock()
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    // Setup is mandatory after onboarding and before any vault data is shown.
    if (!lockManager.isInitialSetupComplete()) {
        LockSetupScreen(
            onSetupComplete = {
                lockManager.markInitialSetupComplete()
                lockManager.unlock()
                viewModel.unlockVault()
            }
        )
        return
    }

    // If locked, present full-screen LockScreen
    if (isLocked) {
        LockScreen(
            onUnlock = {
                viewModel.unlockVault()
                lockManager.unlock()
            }
        )
        return
    }

    // The quick-chip choice is optional, but is offered once immediately after
    // the mandatory lock setup.
    if (!hasPromptedQuickChips) {
        AppPickerSheet(
            title = "Pick Your Quick-Chips",
            mode = AppPickerMode.MULTI_SELECT_LIMITED,
            maxLimit = 6,
            onConfirmSelection = { packages ->
                chipRepository.setQuickChips(packages.toList())
                chipRepository.markQuickChipsSetupCompleted()
            },
            onClose = { chipRepository.markQuickChipsSetupCompleted() }
        )
        return
    }

    if (!isOnboardingCompleted) {
        OnboardingScreen(
            onFinish = { isOnboardingCompleted = true }
        )
        return
    }

    val isTopLevelRoute = currentRoute in listOf("vault", "search", "insights", "studio")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        AnimatedContent(
            targetState = currentRoute,
            transitionSpec = {
                fadeIn(animationSpec = androidx.compose.animation.core.tween(250)) togetherWith
                        fadeOut(animationSpec = androidx.compose.animation.core.tween(200))
            },
            label = "screenTransition"
        ) { targetRoute ->
            when (targetRoute) {
                "vault" -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { id ->
                        selectedNotificationId = id
                        currentRoute = "detail"
                    },
                    onNavigateToSearch = { currentRoute = "search" },
                    onNavigateToSettings = { currentRoute = "settings" }
                )
                "search" -> SearchScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { id ->
                        selectedNotificationId = id
                        currentRoute = "detail"
                    },
                    onBack = { currentRoute = "vault" }
                )
                "insights" -> InsightsScreen(
                    viewModel = viewModel
                )
                "studio" -> CustomizationStudioScreen(
                    viewModel = viewModel
                )
                "settings" -> SettingsScreen(
                    viewModel = viewModel,
                    onBack = { currentRoute = "vault" },
                    onNavigateToExclusionRules = { currentRoute = "exclusions" }
                )
                "exclusions" -> ExclusionRulesScreen(
                    onBack = { currentRoute = "settings" }
                )
                "detail" -> DetailScreen(
                    notificationId = selectedNotificationId,
                    viewModel = viewModel,
                    onBack = { currentRoute = "vault" }
                )
                else -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { id ->
                        selectedNotificationId = id
                        currentRoute = "detail"
                    },
                    onNavigateToSearch = { currentRoute = "search" },
                    onNavigateToSettings = { currentRoute = "settings" }
                )
            }
        }

        // Floating Bottom Glass Navigation Bar
        if (isTopLevelRoute) {
            BottomGlassNav(
                selectedRoute = currentRoute,
                onNavigate = { route -> currentRoute = route },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
