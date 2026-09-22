package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.BottomGlassNav
import com.example.ui.screens.CustomizationStudioScreen
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.OnboardingScreen
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

    // Cold start lock check
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (lockManager.isLockConfigured()) {
            viewModel.lockVault()
        }
    }

    // Lifecycle observer for auto-lock timeouts and permission checks
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkNotificationPermission()
                if (lockManager.shouldLockOnResume()) {
                    viewModel.lockVault()
                }
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                lockManager.recordAppPaused()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
                    onBack = { currentRoute = "vault" }
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
