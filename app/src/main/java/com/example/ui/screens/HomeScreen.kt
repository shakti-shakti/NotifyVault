package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AllInbox
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.provider.Settings
import com.example.ui.theme.ShapeL
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppInfoResolver
import com.example.data.CustomFilterChip
import com.example.data.FilterChipRepository
import com.example.data.NotificationEntity
import com.example.ui.components.AmbientMeshBackground
import com.example.ui.components.AppIconOrb
import com.example.ui.components.AppPickerMode
import com.example.ui.components.AppPickerSheet
import com.example.ui.components.CustomFilterDialog
import com.example.ui.components.EmptyState
import com.example.ui.components.GlassCard
import com.example.ui.components.NotificationCard
import com.example.ui.components.PulseFab
import com.example.ui.components.StatPill
import com.example.ui.components.VaultChip
import com.example.ui.theme.AmethystPalette
import com.example.ui.theme.AurumPalette
import com.example.ui.theme.CyanPulsePalette
import com.example.ui.theme.DisplayM
import com.example.ui.theme.EmeraldPalette
import com.example.ui.theme.LocalVaultColors
import com.example.ui.theme.LocalVaultCustomization
import com.example.ui.theme.ShapePill
import com.example.ui.theme.VaultBodyM
import com.example.ui.theme.VaultCaption
import com.example.ui.theme.VaultHeadline
import com.example.ui.theme.VaultLabel
import com.example.ui.theme.VaultTitle
import com.example.viewmodel.VaultViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: VaultViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current
    val customization = LocalVaultCustomization.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val isListenerPermissionGranted by viewModel.isListenerPermissionGranted.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val todayCount by viewModel.todayCount.collectAsStateWithLifecycle()
    val otpCount by viewModel.otpCount.collectAsStateWithLifecycle()
    val paymentCount by viewModel.paymentCount.collectAsStateWithLifecycle()
    val starredCount by viewModel.starredCount.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val isCaptureActive by viewModel.isCaptureActive.collectAsStateWithLifecycle()

    val chipRepo = remember { FilterChipRepository.getInstance(context) }
    val customChips by chipRepo.customChips.collectAsStateWithLifecycle()
    val hiddenBuiltIns by chipRepo.hiddenBuiltInChips.collectAsStateWithLifecycle()
    val quickChipPackages by chipRepo.quickChipPackages.collectAsStateWithLifecycle()
    val temporaryQuickChip by chipRepo.temporaryQuickChip.collectAsStateWithLifecycle()
    val selectedAppPackage by viewModel.selectedAppPackage.collectAsStateWithLifecycle()
    val selectedCustomChip by viewModel.selectedCustomChip.collectAsStateWithLifecycle()

    var showCustomFilterDialog by remember { mutableStateOf(false) }
    var editingCustomChip by remember { mutableStateOf<CustomFilterChip?>(null) }
    var chipActionMenu by remember { mutableStateOf<CustomFilterChip?>(null) }
    var chipToHide by remember { mutableStateOf<String?>(null) }
    var chipToDelete by remember { mutableStateOf<CustomFilterChip?>(null) }
    var showAppPickerForQuickChips by remember { mutableStateOf(false) }
    var showAppPickerForSingleApp by remember { mutableStateOf(false) }
    var appToUnpin by remember { mutableStateOf<String?>(null) }
    var appToPin by remember { mutableStateOf<String?>(null) }

    val effectiveQuickChips = remember(quickChipPackages, temporaryQuickChip) {
        (quickChipPackages + listOfNotNull(temporaryQuickChip))
            .distinct()
            .take(7)
    }

    // Rotating search placeholders
    val placeholders = listOf(
        "Search captured alerts…",
        "Search OTP codes…",
        "Search amounts & payments…",
        "Search by app name…",
        "Search archived notifications…"
    )
    var placeholderIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            placeholderIndex = (placeholderIndex + 1) % placeholders.size
        }
    }

    // Live capture breathing dot
    val transition = rememberInfiniteTransition(label = "pulseDot")
    val dotAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    AmbientMeshBackground(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // 2.2 A. HEADER
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Vault",
                                    style = DisplayM.copy(
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.02).sp
                                    ),
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                // Live status line with pulsing accent dot
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isCaptureActive) colors.accent.base.copy(alpha = dotAlpha)
                                                else colors.textDisabled
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isCaptureActive) "Capturing · $todayCount today" else "Capture Paused",
                                        style = VaultCaption.copy(
                                            fontSize = 12.sp,
                                            color = if (isCaptureActive) colors.accent.base else colors.textTertiary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }

                            // Circular glass settings/lock button
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(colors.surface.copy(alpha = 0.8f))
                                        .border(1.dp, colors.cardStroke, CircleShape)
                                        .clickable { viewModel.lockVault() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = "Lock Vault",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(colors.surface.copy(alpha = 0.8f))
                                        .border(1.dp, colors.cardStroke, CircleShape)
                                        .clickable { onNavigateToSettings() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // PERMISSION BANNER (Real device notification listener requirement)
                if (!isListenerPermissionGranted) {
                    item {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            shape = ShapeL,
                            accentBorder = true
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(colors.accent.glow),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = colors.accent.base,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Notification Access Required",
                                            style = VaultTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = "Grant NotifyVault listener access in Android Settings to capture 100% real device alerts (WhatsApp, Bank OTPs, SMS).",
                                            style = VaultCaption.copy(fontSize = 12.sp, color = colors.textSecondary)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .clip(ShapePill)
                                        .background(colors.accent.brush())
                                        .clickable {
                                            try {
                                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Enable in Android Settings",
                                        style = VaultLabel.copy(color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2.2 B. STAT PILLS ROW
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatPill(
                            icon = Icons.Outlined.AllInbox,
                            count = totalCount,
                            label = "Total",
                            accentPalette = colors.accent,
                            isSelected = selectedFilter == "All",
                            onClick = { viewModel.setFilter("All") }
                        )
                        StatPill(
                            icon = Icons.Outlined.Today,
                            count = todayCount,
                            label = "Today",
                            accentPalette = CyanPulsePalette,
                            isSelected = false,
                            onClick = { viewModel.setFilter("All") }
                        )
                        StatPill(
                            icon = Icons.Default.Security,
                            count = otpCount,
                            label = "OTPs",
                            accentPalette = CyanPulsePalette,
                            isSelected = selectedFilter == "OTPs",
                            onClick = { viewModel.setFilter("OTPs") }
                        )
                        StatPill(
                            icon = Icons.Outlined.AccountBalanceWallet,
                            count = paymentCount,
                            label = "Payments",
                            accentPalette = EmeraldPalette,
                            isSelected = selectedFilter == "Payments",
                            onClick = { viewModel.setFilter("Payments") }
                        )
                        StatPill(
                            icon = Icons.Default.Star,
                            count = starredCount,
                            label = "Starred",
                            accentPalette = AurumPalette,
                            isSelected = selectedFilter == "Starred",
                            onClick = { viewModel.setFilter("Starred") }
                        )
                    }
                }

                // 2.2 C. SEARCH BAR PILL
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clickable { onNavigateToSearch() },
                            shape = ShapePill
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = colors.accent.base,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = placeholders[placeholderIndex],
                                    style = VaultBodyM.copy(
                                        color = colors.textTertiary,
                                        fontSize = 14.sp
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filter",
                                    tint = colors.textTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 2.2 D. FILTER CHIP ROW (Feature 2)
                item {
                    val allBuiltIn = listOf(
                        "All",
                        "OTPs",
                        "Payments",
                        "Deliveries",
                        "Messages",
                        "Social",
                        "System",
                        "Starred"
                    )
                    val visibleBuiltIns = allBuiltIn.filter { it == "All" || !hiddenBuiltIns.contains(it) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        // Built-in Chips
                        visibleBuiltIns.forEach { filter ->
                            val isSelected = selectedFilter == filter && selectedCustomChip == null
                            VaultChip(
                                text = filter,
                                isSelected = isSelected,
                                onClick = {
                                    viewModel.selectCustomChip(null)
                                    viewModel.setFilter(filter)
                                },
                                onLongClick = if (filter != "All") {
                                    { chipToHide = filter }
                                } else null
                            )
                        }

                        // Custom Created Chips
                        customChips.forEach { chip ->
                            val isSelected = selectedCustomChip?.id == chip.id
                            VaultChip(
                                text = chip.name,
                                isSelected = isSelected,
                                customColor = try {
                                    Color(android.graphics.Color.parseColor(chip.colorHex))
                                } catch (e: Exception) {
                                    null
                                },
                                onClick = {
                                    if (isSelected) {
                                        viewModel.selectCustomChip(null)
                                        viewModel.setFilter("All")
                                    } else {
                                        viewModel.selectCustomChip(chip)
                                    }
                                },
                                onLongClick = {
                                    chipActionMenu = chip
                                }
                            )
                        }

                        }

                        // Create Custom Filter "+" Button stays pinned at the end.
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clip(ShapePill)
                                .background(colors.surface.copy(alpha = 0.6f))
                                .border(1.dp, colors.cardStroke, ShapePill)
                                .clickable { showCustomFilterDialog = true }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Filter",
                                    tint = colors.accent.base,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Filter",
                                    style = VaultCaption.copy(
                                        color = colors.accent.base,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

                // 2.2 E. DASHBOARD APP QUICK-CHIPS ROW (Feature 5)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // The app-grid picker is always visible, including when
                        // the user skipped initial quick-chip setup.
                        Box(
                            modifier = Modifier
                                .clip(ShapePill)
                                .background(colors.surface.copy(alpha = 0.6f))
                                .border(1.dp, colors.cardStroke, ShapePill)
                                .clickable { showAppPickerForSingleApp = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Apps,
                                    contentDescription = "Choose app",
                                    tint = colors.accent.base,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Apps",
                                    style = VaultCaption.copy(
                                        color = colors.textSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                            effectiveQuickChips.forEach { pkg ->
                                val appInfo = remember(pkg) {
                                    AppInfoResolver.resolveSync(context, pkg)
                                }
                                val isSelected = selectedAppPackage == pkg

                                Box(
                                    modifier = Modifier
                                        .clip(ShapePill)
                                        .background(
                                            if (isSelected) colors.accent.base.copy(alpha = 0.22f)
                                            else colors.surface.copy(alpha = 0.6f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) colors.accent.base else colors.cardStroke,
                                            ShapePill
                                        )
                                        .combinedClickable(
                                            onClick = { viewModel.selectAppPackage(pkg) },
                                             onLongClick = {
                                                 if (quickChipPackages.contains(pkg)) appToUnpin = pkg
                                                 else appToPin = pkg
                                             }
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        AppIconOrb(
                                            appName = appInfo.appName,
                                            accentColor = colors.accent.base,
                                            packageName = pkg,
                                            size = 18
                                        )
                                        Text(
                                            text = appInfo.appName,
                                            style = VaultCaption.copy(
                                                color = if (isSelected) colors.accent.base else colors.textPrimary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }

                        }
                    }
                }

                // Section Title with sticky count
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedFilter.uppercase()} ARCHIVE",
                            style = VaultLabel.copy(
                                fontSize = 11.sp,
                                color = colors.textTertiary,
                                letterSpacing = 0.1.sp
                            )
                        )
                        Text(
                            text = "${notifications.size} records",
                            style = VaultCaption.copy(
                                fontSize = 11.sp,
                                color = colors.textTertiary
                            )
                        )
                    }
                }

                // 2.2 E. NOTIFICATION LIST
                if (notifications.isEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            shape = ShapeL,
                            accentBorder = true
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(colors.accent.glow),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = colors.accent.base,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = if (selectedFilter != "All") "No $selectedFilter Captured" else "Awaiting Live Notifications",
                                    style = VaultTitle.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                                    color = colors.textPrimary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = if (selectedFilter != "All")
                                        "No records matched the '$selectedFilter' filter in your local vault."
                                    else if (!isListenerPermissionGranted)
                                        "Android notification listener access is required to capture live alerts from WhatsApp, Bank apps, and SMS."
                                    else
                                        "NotifyVault is actively monitoring your device. Incoming alerts, OTPs, and receipts from any app will be cataloged here in real time.",
                                    style = VaultBodyM.copy(fontSize = 13.sp, color = colors.textSecondary),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                if (selectedFilter != "All") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .clip(ShapePill)
                                            .background(colors.accent.brush())
                                            .clickable { viewModel.setFilter("All") },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Show All Notifications",
                                            style = VaultLabel.copy(color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        )
                                    }
                                } else if (!isListenerPermissionGranted) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .clip(ShapePill)
                                            .background(colors.accent.brush())
                                            .clickable {
                                                try {
                                                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Enable Notification Access",
                                            style = VaultLabel.copy(color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Sync active status bar notifications
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                                .clip(ShapePill)
                                                .background(colors.surface)
                                                .border(1.dp, colors.cardStroke, ShapePill)
                                                .clickable { viewModel.syncActiveNotifications() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Sync,
                                                    contentDescription = null,
                                                    tint = colors.accent.base,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Sync Active",
                                                    style = VaultLabel.copy(color = colors.textPrimary, fontSize = 12.sp)
                                                )
                                            }
                                        }

                                        // Send test live alert to verify real capture
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                                .clip(ShapePill)
                                                .background(colors.accent.brush())
                                                .clickable { viewModel.sendVerificationNotification() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Test Live Alert",
                                                    style = VaultLabel.copy(color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(
                        items = notifications,
                        key = { it.id }
                    ) { notification ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            NotificationCard(
                                notification = notification,
                                isSelected = selectedIds.contains(notification.id),
                                isSelectionMode = isSelectionMode,
                                listStyle = customization.listStyle,
                                onClick = {
                                    if (isSelectionMode) {
                                        viewModel.toggleSelectId(notification.id)
                                    } else {
                                        onNavigateToDetail(notification.id)
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleSelectionMode()
                                    viewModel.toggleSelectId(notification.id)
                                },
                                onStarClick = { viewModel.toggleStar(notification) },
                                onCopyOtpClick = { otp ->
                                    clipboardManager.setText(AnnotatedString(otp))
                                }
                            )
                        }
                    }
                }
            }

            // 2.10 BULK SELECTION CONTEXTUAL TOP BAR
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp)
                ) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = ShapePill
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${selectedIds.size} selected",
                                style = VaultTitle.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent.base
                                )
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "SELECT ALL",
                                    style = VaultLabel.copy(
                                        color = colors.textPrimary,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.clickable {
                                        viewModel.selectAll(notifications.map { it.id })
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.clickable { viewModel.toggleSelectionMode() }
                                )
                            }
                        }
                    }
                }
            }

            // 2.10 BULK SELECTION BOTTOM ACTION BAR
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp, start = 20.dp, end = 20.dp)
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = ShapePill
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.clickable { viewModel.starSelected() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, "Star", tint = AurumPalette.base, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Star", style = VaultCaption.copy(color = AurumPalette.base))
                        }
                        Row(
                            modifier = Modifier.clickable { viewModel.archiveSelected() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Archive, "Archive", tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Archive", style = VaultCaption.copy(color = colors.textPrimary))
                        }
                        Row(
                            modifier = Modifier.clickable { viewModel.deleteSelected() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFFB7185), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", style = VaultCaption.copy(color = Color(0xFFFB7185)))
                        }
                    }
                }
            }

            // 2.2 F. PULSE FAB
            PulseFab(
                isActive = isCaptureActive,
                onClick = { viewModel.toggleCaptureActive() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 90.dp, end = 20.dp)
            )
        }

        // Custom Filter Dialog (Create or Edit)
        if (chipActionMenu != null) {
            val chip = chipActionMenu!!
            AlertDialog(
                onDismissRequest = { chipActionMenu = null },
                title = { Text(chip.name, style = VaultTitle) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        TextButton(
                            onClick = {
                                editingCustomChip = chip
                                chipActionMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Rename / edit apps", modifier = Modifier.fillMaxWidth(), color = colors.textPrimary)
                        }
                        TextButton(
                            onClick = {
                                val next = if (chip.colorHex == "#E2A84B") "#6C63FF" else "#E2A84B"
                                chipRepo.changeCustomChipColor(chip.id, next)
                                chipActionMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Change color", modifier = Modifier.fillMaxWidth(), color = colors.textPrimary)
                        }
                        TextButton(
                            onClick = {
                                chipRepo.moveCustomChip(chip.id, -1)
                                chipActionMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Move earlier", modifier = Modifier.fillMaxWidth(), color = colors.textPrimary)
                        }
                        TextButton(
                            onClick = {
                                chipRepo.duplicateCustomChip(chip.id)
                                chipActionMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Duplicate", modifier = Modifier.fillMaxWidth(), color = colors.textPrimary)
                        }
                        TextButton(
                            onClick = {
                                chipRepo.deleteCustomChip(chip.id)
                                if (selectedCustomChip?.id == chip.id) viewModel.selectCustomChip(null)
                                chipActionMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Delete", modifier = Modifier.fillMaxWidth(), color = Color(0xFFFB7185))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { chipActionMenu = null }) {
                        Text("Close", color = colors.accent.base)
                    }
                },
                containerColor = colors.surface
            )
        }

        if (showCustomFilterDialog || editingCustomChip != null) {
            CustomFilterDialog(
                initialChip = editingCustomChip,
                onDismiss = {
                    showCustomFilterDialog = false
                    editingCustomChip = null
                },
                onSave = { name, pkgs, keywords, regex, colorHex ->
                    val chipToSave = (editingCustomChip ?: CustomFilterChip(
                        name = name,
                        packages = pkgs,
                        keywords = keywords,
                        regex = regex,
                        colorHex = colorHex
                    )).copy(
                        name = name,
                        packages = pkgs,
                        keywords = keywords,
                        regex = regex,
                        colorHex = colorHex
                    )
                    chipRepo.saveCustomChip(chipToSave)
                    showCustomFilterDialog = false
                    editingCustomChip = null
                },
                onDelete = editingCustomChip?.let { chip ->
                    {
                        chipRepo.deleteCustomChip(chip.id)
                        if (selectedCustomChip?.id == chip.id) {
                            viewModel.selectCustomChip(null)
                        }
                        showCustomFilterDialog = false
                        editingCustomChip = null
                    }
                }
            )
        }

        // App Picker Sheet for Quick Chips (limited to 6)
        if (showAppPickerForQuickChips) {
            AppPickerSheet(
                mode = AppPickerMode.MULTI_SELECT_LIMITED,
                title = "Pin App Quick-Chips",
                maxLimit = 6,
                initiallySelected = effectiveQuickChips.toSet(),
                onConfirmSelection = { packages ->
                    chipRepo.setQuickChips(packages.toList())
                    showAppPickerForQuickChips = false
                },
                onClose = { showAppPickerForQuickChips = false }
            )
        }

        if (showAppPickerForSingleApp) {
            AppPickerSheet(
                mode = AppPickerMode.SINGLE_SELECT,
                title = "Choose App Scope",
                onSingleAppSelected = { app ->
                    chipRepo.setTemporaryQuickChip(app.packageName)
                    viewModel.selectAppPackage(app.packageName)
                    showAppPickerForSingleApp = false
                },
                onClose = { showAppPickerForSingleApp = false }
            )
        }

        // Dialog to Hide Built-in Chip
        if (chipToHide != null) {
            AlertDialog(
                onDismissRequest = { chipToHide = null },
                title = { Text("Hide Filter Chip?", style = VaultTitle) },
                text = {
                    Text(
                        "Are you sure you want to hide the \"${chipToHide}\" filter chip? You can restore it anytime in Settings.",
                        style = VaultBodyM.copy(color = colors.textPrimary)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            chipToHide?.let { chipRepo.hideBuiltInChip(it) }
                            chipToHide = null
                        }
                    ) {
                        Text("Hide Chip", color = colors.accent.base, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { chipToHide = null }) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                },
                containerColor = colors.surface
            )
        }

        // Dialog to Unpin Quick-Chip
        if (appToUnpin != null) {
            val unpinAppName = remember(appToUnpin) {
                AppInfoResolver.resolveSync(context, appToUnpin!!).appName
            }
            AlertDialog(
                onDismissRequest = { appToUnpin = null },
                title = { Text("Unpin Quick-Chip?", style = VaultTitle) },
                text = {
                    Text(
                        "Remove \"$unpinAppName\" from your dashboard quick-access chips?",
                        style = VaultBodyM.copy(color = colors.textPrimary)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            appToUnpin?.let { chipRepo.removeQuickChip(it) }
                            if (selectedAppPackage == appToUnpin) {
                                viewModel.selectAppPackage(null)
                            }
                            appToUnpin = null
                        }
                    ) {
                        Text("Unpin", color = Color(0xFFFB7185), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { appToUnpin = null }) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                },
                containerColor = colors.surface
            )
        }

        if (appToPin != null) {
            val temporaryAppName = remember(appToPin) {
                AppInfoResolver.resolveSync(context, appToPin!!).appName
            }
            AlertDialog(
                onDismissRequest = { appToPin = null },
                title = { Text("Pin Quick-Chip?", style = VaultTitle) },
                text = {
                    Text(
                        "Pin \"$temporaryAppName\" so it stays on your dashboard?",
                        style = VaultBodyM.copy(color = colors.textPrimary)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            appToPin?.let { chipRepo.pinTemporaryQuickChip(it) }
                            appToPin = null
                        }
                    ) {
                        Text("Pin", color = colors.accent.base, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { appToPin = null }) {
                        Text("Not now", color = colors.textSecondary)
                    }
                },
                containerColor = colors.surface
            )
        }
    }
}
