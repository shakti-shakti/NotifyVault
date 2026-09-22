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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.ui.components.AmbientMeshBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.PrimaryPillButton
import com.example.ui.theme.CrimsonPalette
import com.example.ui.theme.DisplayM
import com.example.ui.theme.EmeraldPalette
import com.example.ui.theme.LocalVaultColors
import com.example.ui.theme.ShapeL
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
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current
    val context = LocalContext.current

    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val isCaptureActive by viewModel.isCaptureActive.collectAsStateWithLifecycle()

    var biometricLockEnabled by remember { mutableStateOf(false) }
    var showPanicDialog by remember { mutableStateOf(false) }

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
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. PRIVACY & SECURITY
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    text = "PRIVACY & LOCAL ENCRYPTION",
                    style = VaultLabel.copy(fontSize = 11.sp, color = colors.textTertiary)
                )
                Spacer(modifier = Modifier.height(10.dp))
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ShapeL,
                    accentBorder = true
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
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
                                    text = "Zero-Knowledge Local Archive",
                                    style = VaultTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Encrypted at rest on this device. Never uploaded to any cloud.",
                                    style = VaultCaption.copy(color = EmeraldPalette.base, fontSize = 11.sp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Biometric / PIN Screen Lock", style = VaultBodyM.copy(color = colors.textPrimary))
                                Text("Require PIN to open vault", style = VaultCaption.copy(color = colors.textTertiary))
                            }
                            Switch(
                                checked = biometricLockEnabled,
                                onCheckedChange = {
                                    biometricLockEnabled = it
                                    if (it) viewModel.lockVault()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.accent.base,
                                    checkedTrackColor = colors.accent.glow
                                )
                            )
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
    }
}
