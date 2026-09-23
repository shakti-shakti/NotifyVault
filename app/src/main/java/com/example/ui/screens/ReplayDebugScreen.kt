package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.NotificationEntity
import com.example.service.LiveNotificationRegistry
import com.example.ui.components.AmbientMeshBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.instantTap
import com.example.ui.theme.LocalVaultColors
import com.example.ui.theme.ShapeL
import com.example.ui.theme.ShapePill
import com.example.ui.theme.VaultBodyM
import com.example.ui.theme.VaultCaption
import com.example.ui.theme.VaultHeadline
import com.example.ui.theme.VaultTitle
import com.example.viewmodel.VaultViewModel

@Composable
fun ReplayDebugScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current
    val registry = remember { LiveNotificationRegistry.getInstance() }
    val liveKeys by registry.liveKeys.collectAsStateWithLifecycle()
    val entries = remember(liveKeys) { registry.snapshot() }

    AmbientMeshBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(colors.surface, CircleShape)
                        .border(1.dp, colors.cardStroke, CircleShape)
                        .instantTap(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = colors.textPrimary)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text("Replay Diagnostics", style = VaultHeadline, color = colors.textPrimary)
                    Text(
                        "Debug-only live PendingIntent registry",
                        style = VaultCaption.copy(color = colors.textTertiary),
                    )
                }
            }

            Spacer(modifier = Modifier.size(18.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = ShapeL, accentBorder = true) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("REGISTRY SIZE", style = VaultCaption.copy(color = colors.textTertiary))
                    Text("${entries.size} / 500", style = VaultHeadline.copy(fontSize = 25.sp), color = colors.accent.base)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        if (entries.isEmpty()) "No notifications are currently live."
                        else "Each entry can be replayed while Android keeps its PendingIntent alive.",
                        style = VaultBodyM.copy(fontSize = 12.sp),
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.size(14.dp))
            entries.forEach { entry ->
                val testEntity = NotificationEntity(
                    notificationKey = entry.key,
                    notificationId = entry.notificationId,
                    tag = entry.tag,
                    packageName = entry.packageName,
                    appName = entry.packageName,
                    title = entry.fullNotification.extras?.getCharSequence("android.title")?.toString()
                )
                GlassCard(modifier = Modifier.fillMaxWidth(), shape = ShapeL) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(entry.packageName, style = VaultTitle.copy(fontSize = 14.sp), color = colors.textPrimary)
                        Text(
                            LiveNotificationRegistry.key(entry.packageName, entry.notificationId, entry.tag),
                            style = VaultCaption.copy(fontSize = 10.sp, color = colors.textTertiary)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Row(
                            modifier = Modifier
                                .background(colors.elevated, ShapePill)
                                .instantTap(onClick = { viewModel.replay(testEntity) })
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = colors.accent.base, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Replay content intent", style = VaultCaption.copy(color = colors.textPrimary))
                        }
                    }
                }
                Spacer(modifier = Modifier.size(8.dp))
            }
        }
    }
}