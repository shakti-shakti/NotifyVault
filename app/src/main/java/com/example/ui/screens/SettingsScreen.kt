package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.security.BiometricAuthHelper
import com.example.security.LockManager
import com.example.security.LockMethod
import com.example.ui.components.AmbientMeshBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.instantTap
import com.example.ui.components.PrimaryPillButton
import com.example.ui.theme.CrimsonPalette
import com.example.ui.theme.DisplayM
import com.example.ui.theme.EmeraldPalette
import com.example.ui.theme.LocalVaultColors
import com.example.ui.theme.ShapeL
import com.example.ui.theme.ShapeM
import com.example.ui.theme.ShapePill
import com.example.ui.theme.VaultBodyM
import com.example.ui.theme.VaultCaption
import com.example.ui.theme.VaultLabel
import com.example.ui.theme.VaultTitle
import com.example.viewmodel.VaultViewModel

@Composable
fun SettingsScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit,
    onNavigateToExclusionRules: () -> Unit = {},
    onNavigateToReplayDebug: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current
    val context = LocalContext.current
    val lockManager = remember { LockManager.getInstance(context) }

    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val isCaptureActive by viewModel.isCaptureActive.collectAsStateWithLifecycle()

    val lockMethod by lockManager.lockMethodFlow.collectAsState()
    val isBiometricEnabled by lockManager.biometricEnabledFlow.collectAsState()
    val isHideRecents by lockManager.hideRecentsFlow.collectAsState()

    var showChangeLockFlow by remember { mutableStateOf(false) }
    var showDisableLockDialog by remember { mutableStateOf(false) }
    var showLockSetup by remember { mutableStateOf(false) }
    var showTimeoutMenu by remember { mutableStateOf(false) }
    var showPanicDialog by remember { mutableStateOf(false) }
    var showEmergencyWipeDialog by remember { mutableStateOf(false) }

    var lockOnScreenOff by remember { mutableStateOf(lockManager.isLockOnScreenOff()) }
    var lockOnAppClose by remember { mutableStateOf(lockManager.isLockOnAppClose()) }
    var blurSensitiveContent by remember { mutableStateOf(lockManager.isBlurSensitiveContent()) }
    var currentTimeoutMs by remember { mutableStateOf(lockManager.getAutoLockTimeout()) }

    if (showLockSetup) {
        LockSetupScreen(
            onSetupComplete = {
                lockManager.markInitialSetupComplete()
                showLockSetup = false
            }
        )
        return
    }

    if (showChangeLockFlow) {
        ChangeLockFlow(onDismiss = { showChangeLockFlow = false })
        return
    }

    if (showDisableLockDialog) {
        DisableLockDialog(
            onDismiss = { showDisableLockDialog = false },
            onLockDisabled = { showDisableLockDialog = false }
        )
    }

    AmbientMeshBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 60.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(colors.surface.copy(alpha = 0.8f))
                        .border(1.dp, colors.cardStroke, CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Settings",
                        style = DisplayM.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Security, Storage & System Control",
                        style = VaultCaption.copy(fontSize = 11.sp, color = colors.textTertiary)
                    )
                }
            }

            // 1. CAPTURE ENGINE STATUS
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    text = "CAPTURE ENGINE",
                    style = VaultLabel.copy(fontSize = 11.sp, color = colors.textTertiary)
                )
                Spacer(modifier = Modifier.height(10.dp))
                GlassCard(modifier = Modifier.fillMaxWidth(), shape = ShapeL) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isCaptureActive) "Background Archiver Active" else "Archiver Paused",
                                    style = VaultTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Monitoring status bar broadcasts in real time",
                                    style = VaultCaption.copy(fontSize = 11.sp, color = colors.textTertiary)
                                )
                            }
                            Switch(
                                checked = isCaptureActive,
                                onCheckedChange = { viewModel.toggleCaptureActive() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.accent.base,
                                    checkedTrackColor = colors.accent.glow
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // System Notification Listener Access Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ShapePill)
                                .background(colors.elevated)
                                .clickable {
                                    try {
                                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = colors.accent.base,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Android Notification Access Permission",
                                        style = VaultCaption.copy(fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = colors.textTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Capture Exclusion Rules Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ShapePill)
                                .background(colors.elevated)
                                .clickable { onNavigateToExclusionRules() }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null,
                                        tint = colors.accent.base,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Capture Exclusion Rules & Filters",
                                        style = VaultCaption.copy(fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = colors.textTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. PRIVACY & SECURITY
            FeaturePackSettings(viewModel.featurePreferences)

            Spacer(modifier = Modifier.height(14.dp))

            // 3. PRIVACY & SECURITY
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    text = "PRIVACY & SECURITY",
                    style = VaultLabel.copy(fontSize = 11.sp, color = colors.textTertiary)
                )
                Spacer(modifier = Modifier.height(10.dp))
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ShapeL,
                    accentBorder = true
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Shield Header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPalette.glow)
                                    .border(1.dp, EmeraldPalette.base, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Shield, null, tint = EmeraldPalette.base, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Zero-Knowledge Vault Lock",
                                    style = VaultTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "PBKDF2 encrypted credentials · Hardware keystore",
                                    style = VaultCaption.copy(color = EmeraldPalette.base, fontSize = 11.sp)
                                )
                            }
                        }

                        // Current Lock Method Status Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ShapeM)
                                .background(colors.surface)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Active Lock Method", style = VaultCaption.copy(fontSize = 10.sp, color = colors.textTertiary))
                                Text(lockMethod.title, style = VaultTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = colors.textPrimary)
                            }
                            if (lockMethod != LockMethod.NONE) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Change",
                                        style = VaultCaption.copy(color = colors.accent.base, fontWeight = FontWeight.Bold),
                                        modifier = Modifier
                                            .clip(ShapePill)
                                            .clickable { showChangeLockFlow = true }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                    Text(
                                        text = "Disable",
                                        style = VaultCaption.copy(color = CrimsonPalette.base, fontWeight = FontWeight.Bold),
                                        modifier = Modifier
                                            .clip(ShapePill)
                                            .clickable { showDisableLockDialog = true }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Fast Biometric Unlock Toggle
                        if (BiometricAuthHelper.isBiometricAvailable(context) && lockMethod != LockMethod.NONE) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Fast Biometric Unlock", style = VaultBodyM.copy(color = colors.textPrimary, fontSize = 13.sp))
                                    Text("Use fingerprint or face recognition on launch", style = VaultCaption.copy(color = colors.textTertiary, fontSize = 10.sp))
                                }
                                Switch(
                                    checked = isBiometricEnabled,
                                    onCheckedChange = { lockManager.setBiometricEnabled(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = colors.accent.base, checkedTrackColor = colors.accent.glow)
                                )
                            }
                        }

                        // Auto-lock Timeout Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-Lock Timeout", style = VaultBodyM.copy(color = colors.textPrimary, fontSize = 13.sp))
                                Text("Lock vault when inactive in background", style = VaultCaption.copy(color = colors.textTertiary, fontSize = 10.sp))
                            }
                            if (lockMethod != LockMethod.NONE) {
                                Box {
                                val timeoutLabels = mapOf(
                                    0L to "Immediately",
                                    15_000L to "15s",
                                    30_000L to "30s",
                                    60_000L to "1m",
                                    300_000L to "5m",
                                    -1L to "Never"
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(ShapePill)
                                        .background(colors.surface)
                                        .border(1.dp, colors.cardStroke, ShapePill)
                                        .clickable { showTimeoutMenu = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = timeoutLabels[currentTimeoutMs] ?: "Immediately",
                                        style = VaultCaption.copy(color = colors.accent.base, fontWeight = FontWeight.Bold)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showTimeoutMenu,
                                    onDismissRequest = { showTimeoutMenu = false }
                                ) {
                                    timeoutLabels.forEach { (ms, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                currentTimeoutMs = ms
                                                lockManager.setAutoLockTimeout(ms)
                                                showTimeoutMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                            } else {
                                Text(
                                    text = "Enable",
                                    style = VaultCaption.copy(color = colors.accent.base, fontWeight = FontWeight.Bold),
                                    modifier = Modifier
                                        .clip(ShapePill)
                                        .clickable { showLockSetup = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Lock on Screen Off
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Lock on Screen-Off", style = VaultBodyM.copy(color = colors.textPrimary, fontSize = 13.sp))
                                Text("Require unlock immediately when device display turns off", style = VaultCaption.copy(color = colors.textTertiary, fontSize = 10.sp))
                            }
                            Switch(
                                checked = lockOnScreenOff,
                                onCheckedChange = {
                                    lockOnScreenOff = it
                                    lockManager.setLockOnScreenOff(it)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = colors.accent.base, checkedTrackColor = colors.accent.glow)
                            )
                        }

                        // Lock on App Close
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Lock on App Close", style = VaultBodyM.copy(color = colors.textPrimary, fontSize = 13.sp))
                                Text("Require unlock whenever returning from background", style = VaultCaption.copy(color = colors.textTertiary, fontSize = 10.sp))
                            }
                            Switch(
                                checked = lockOnAppClose,
                                onCheckedChange = {
                                    lockOnAppClose = it
                                    lockManager.setLockOnAppClose(it)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = colors.accent.base, checkedTrackColor = colors.accent.glow)
                            )
                        }

                        // Hide in Recent Apps (FLAG_SECURE)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hide in App Switcher (FLAG_SECURE)", style = VaultBodyM.copy(color = colors.textPrimary, fontSize = 13.sp))
                                Text("Prevents screenshots and obscures vault thumbnail", style = VaultCaption.copy(color = colors.textTertiary, fontSize = 10.sp))
                            }
                            Switch(
                                checked = isHideRecents,
                                onCheckedChange = { lockManager.setHideContentInRecents(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = colors.accent.base, checkedTrackColor = colors.accent.glow)
                            )
                        }

                        // Blur Sensitive Content
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Blur Sensitive Content", style = VaultBodyM.copy(color = colors.textPrimary, fontSize = 13.sp))
                                Text("Apply frosted mask over OTPs and confidential alerts", style = VaultCaption.copy(color = colors.textTertiary, fontSize = 10.sp))
                            }
                            Switch(
                                checked = blurSensitiveContent,
                                onCheckedChange = {
                                    blurSensitiveContent = it
                                    lockManager.setBlurSensitiveContent(it)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = colors.accent.base, checkedTrackColor = colors.accent.glow)
                            )
                        }

                        // Emergency Vault Wipe Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ShapePill)
                                .background(CrimsonPalette.glow)
                                .border(1.dp, CrimsonPalette.base.copy(alpha = 0.6f), ShapePill)
                                .clickable { showEmergencyWipeDialog = true }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DeleteForever, null, tint = CrimsonPalette.base, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Emergency Security Wipe", style = VaultCaption.copy(color = CrimsonPalette.base, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. STORAGE & DATA MANAGEMENT
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    text = "STORAGE & EXPORT",
                    style = VaultLabel.copy(fontSize = 11.sp, color = colors.textTertiary)
                )
                Spacer(modifier = Modifier.height(10.dp))
                GlassCard(modifier = Modifier.fillMaxWidth(), shape = ShapeL) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Storage, null, tint = colors.accent.base, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Database Size", style = VaultBodyM.copy(color = colors.textPrimary))
                            }
                            Text("$totalCount alerts (~${totalCount * 2} KB)", style = VaultCaption.copy(color = colors.accent.base))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Export as JSON
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ShapePill)
                                .background(colors.elevated)
                                .clickable {
                                    try {
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "NotifyVault Encrypted Archive Export")
                                            putExtra(Intent.EXTRA_TEXT, "Exported $totalCount notifications from NotifyVault Obsidian Archive.")
                                        }
                                        val chooser = Intent.createChooser(sendIntent, "Export Vault Archive").apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(chooser)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Download, null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Export Archive as JSON / CSV", style = VaultCaption.copy(color = colors.textPrimary))
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = colors.textTertiary, modifier = Modifier.size(16.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Panic Wipe Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ShapePill)
                                .background(CrimsonPalette.glow)
                                .border(1.dp, CrimsonPalette.base.copy(alpha = 0.5f), ShapePill)
                                .clickable { showPanicDialog = true }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DeleteForever, null, tint = CrimsonPalette.base, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Panic Wipe (Permanent Purge)", style = VaultCaption.copy(color = CrimsonPalette.base, fontWeight = FontWeight.Bold))
                            }
                            Text("PURGE", style = VaultCaption.copy(color = CrimsonPalette.base, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. ABOUT NOTIFYVAULT
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "NOTIFYVAULT",
                    modifier = Modifier.instantTap(
                        onLongClick = onNavigateToReplayDebug,
                        onClick = {}
                    ),
                    style = VaultLabel.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp,
                        color = colors.accent.base
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "v2.0 \"Obsidian Glass\" · Master Release",
                    style = VaultCaption.copy(fontSize = 11.sp, color = colors.textTertiary)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Zero Trackers. Zero Telemetry. 100% On-Device.",
                    style = VaultCaption.copy(fontSize = 10.sp, color = colors.textDisabled)
                )
            }
        }

        // Panic Wipe Confirmation Dialog
        if (showPanicDialog) {
            AlertDialog(
                onDismissRequest = { showPanicDialog = false },
                title = {
                    Text("Permanent Panic Wipe", style = VaultTitle.copy(color = Color(0xFFFB7185)))
                },
                text = {
                    Text(
                        "Are you sure you want to permanently erase all archived notifications? This action cannot be reversed.",
                        style = VaultBodyM.copy(color = colors.textPrimary)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearAll()
                            showPanicDialog = false
                        }
                    ) {
                        Text("PURGE VAULT", color = Color(0xFFFB7185), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPanicDialog = false }) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                },
                containerColor = colors.surface
            )
        }

        // Emergency Security Wipe Confirmation Dialog
        if (showEmergencyWipeDialog) {
            AlertDialog(
                onDismissRequest = { showEmergencyWipeDialog = false },
                title = {
                    Text("Emergency Security Wipe", style = VaultTitle.copy(color = CrimsonPalette.base, fontWeight = FontWeight.Bold))
                },
                text = {
                    Text(
                        "This will immediately destroy all archived notifications AND reset all vault lock credentials and security configurations. Continue?",
                        style = VaultBodyM.copy(color = colors.textPrimary)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearAll()
                            lockManager.resetAllLockData()
                            showEmergencyWipeDialog = false
                        }
                    ) {
                        Text("DESTROY ALL DATA", color = CrimsonPalette.base, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmergencyWipeDialog = false }) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                },
                containerColor = colors.surface
            )
        }
    }
}

