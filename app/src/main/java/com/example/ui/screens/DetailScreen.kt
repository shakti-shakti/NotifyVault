package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.NotificationEntity
import com.example.ui.components.ActionCircleButton
import com.example.ui.components.AmbientMeshBackground
import com.example.ui.components.AppIconOrb
import com.example.ui.components.GlassCard
import com.example.ui.components.JsonViewer
import com.example.ui.components.KeyValueRow
import com.example.ui.components.PriorityRibbon
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AurumPalette
import com.example.ui.theme.CyanPulsePalette
import com.example.ui.theme.LocalVaultColors
import com.example.ui.theme.ShapeL
import com.example.ui.theme.ShapeM
import com.example.ui.theme.ShapePill
import com.example.ui.theme.VaultBodyL
import com.example.ui.theme.VaultBodyM
import com.example.ui.theme.VaultCaption
import com.example.ui.theme.VaultHeadline
import com.example.ui.theme.VaultLabel
import com.example.ui.theme.VaultTitle
import com.example.ui.theme.getCategoryPalette
import com.example.viewmodel.VaultViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DetailScreen(
    notificationId: Long,
    viewModel: VaultViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val notificationFlow = remember(notificationId) { viewModel.getNotificationById(notificationId) }
    val notification by notificationFlow.collectAsStateWithLifecycle(initialValue = null)

    var contentExpanded by remember { mutableStateOf(true) }
    var channelExpanded by remember { mutableStateOf(true) }
    var flagsExpanded by remember { mutableStateOf(false) }
    var rawExpanded by remember { mutableStateOf(false) }

    if (notification == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading notification...", color = colors.textSecondary)
        }
        return
    }

    val item = notification!!
    val categoryPalette = getCategoryPalette(item.category)
    val appColor = categoryPalette.base

    AmbientMeshBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 60.dp)
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
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

                PriorityRibbon(label = item.category, accentPalette = categoryPalette)
            }

            // 2.4 HERO HEADER (280dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppIconOrb(
                    appName = item.appName,
                    accentColor = appColor,
                    size = 80
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = item.appName,
                    style = VaultHeadline.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.packageName,
                    style = VaultCaption.copy(fontSize = 11.sp, color = colors.textTertiary)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = SimpleDateFormat("dd MMMM yyyy · HH:mm:ss", Locale.getDefault()).format(Date(item.captureTime)),
                    style = VaultCaption.copy(fontSize = 12.sp, color = colors.textSecondary)
                )
            }

            // 2.4 ACTION BAR
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = ShapePill
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActionCircleButton(
                        icon = if (item.isStarred) Icons.Default.Star else Icons.Outlined.StarBorder,
                        label = if (item.isStarred) "Starred" else "Star",
                        tint = if (item.isStarred) AurumPalette.base else colors.textPrimary,
                        onClick = { viewModel.toggleStar(item) }
                    )
                    ActionCircleButton(
                        icon = Icons.Default.ContentCopy,
                        label = "Copy",
                        onClick = {
                            val copyText = "${item.title ?: ""}\n${item.text ?: ""}"
                            clipboardManager.setText(AnnotatedString(copyText))
                        }
                    )
                    ActionCircleButton(
                        icon = Icons.Default.Share,
                        label = "Share",
                        onClick = {
                            try {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, item.title ?: "Notification")
                                    putExtra(Intent.EXTRA_TEXT, "${item.appName}: ${item.title}\n${item.text}")
                                }
                                val chooser = Intent.createChooser(sendIntent, "Share Notification").apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(chooser)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    )
                    ActionCircleButton(
                        icon = Icons.Default.OpenInNew,
                        label = "Open App",
                        onClick = {
                            try {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(item.packageName)
                                if (launchIntent != null) {
                                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(launchIntent)
                                } else {
                                    android.widget.Toast.makeText(context, "${item.appName} is not installed", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    )
                    ActionCircleButton(
                        icon = Icons.Default.Delete,
                        label = "Delete",
                        tint = Color(0xFFFB7185),
                        onClick = {
                            viewModel.deleteNotification(item.id)
                            onBack()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 1: CONTENT
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionHeader(
                    title = "Notification Content",
                    isExpanded = contentExpanded,
                    onToggle = { contentExpanded = !contentExpanded }
                )
                AnimatedVisibility(visible = contentExpanded) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = ShapeL
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            if (!item.title.isNullOrBlank()) {
                                Text(
                                    text = "TITLE",
                                    style = VaultLabel.copy(fontSize = 10.sp, color = colors.textTertiary)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.title,
                                    style = VaultTitle.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            val fullBody = item.bigText ?: item.text
                            if (!fullBody.isNullOrBlank()) {
                                Text(
                                    text = "BODY TEXT",
                                    style = VaultLabel.copy(fontSize = 10.sp, color = colors.textTertiary)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = fullBody,
                                    style = VaultBodyL.copy(fontSize = 14.sp, lineHeight = 22.sp),
                                    color = colors.textSecondary
                                )
                            }

                            if (item.hasOtp && item.otpCode != null) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(ShapeM)
                                        .background(CyanPulsePalette.glow)
                                        .border(1.dp, CyanPulsePalette.base, ShapeM)
                                        .clickable { clipboardManager.setText(AnnotatedString(item.otpCode)) }
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("ONE TIME PASSCODE DETECTED", style = VaultCaption.copy(fontSize = 9.sp, color = CyanPulsePalette.base))
                                            Text(item.otpCode, style = VaultHeadline.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                        }
                                        Icon(Icons.Default.ContentCopy, "Copy OTP", tint = CyanPulsePalette.base)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 2: APP & CHANNEL
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionHeader(
                    title = "App & Channel Info",
                    isExpanded = channelExpanded,
                    onToggle = { channelExpanded = !channelExpanded }
                )
                AnimatedVisibility(visible = channelExpanded) {
                    GlassCard(modifier = Modifier.fillMaxWidth(), shape = ShapeL) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            KeyValueRow(key = "App Name", value = item.appName)
                            KeyValueRow(key = "Package", value = item.packageName)
                            KeyValueRow(key = "Channel Name", value = item.channelName ?: "Default")
                            KeyValueRow(key = "Channel ID", value = item.channelId ?: "None")
                            KeyValueRow(key = "Category", value = item.category, isAccent = true)
                            KeyValueRow(key = "Importance", value = item.importance.toString())
                            KeyValueRow(key = "Priority", value = item.priority.toString())
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 3: FLAGS
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionHeader(
                    title = "System Flags",
                    isExpanded = flagsExpanded,
                    onToggle = { flagsExpanded = !flagsExpanded }
                )
                AnimatedVisibility(visible = flagsExpanded) {
                    GlassCard(modifier = Modifier.fillMaxWidth(), shape = ShapeL) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            KeyValueRow(key = "Ongoing Notification", value = if (item.isOngoing) "YES" else "NO")
                            KeyValueRow(key = "User Clearable", value = if (item.isClearable) "YES" else "NO")
                            KeyValueRow(key = "Auto-Cancel", value = if (item.isAutoCancel) "YES" else "NO")
                            KeyValueRow(key = "Only Alert Once", value = if (item.isOnlyAlertOnce) "YES" else "NO")
                            KeyValueRow(key = "Group Summary", value = if (item.isGroupSummary) "YES" else "NO")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 4: RAW EXTRAS JSON
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionHeader(
                    title = "Raw Payload",
                    isExpanded = rawExpanded,
                    onToggle = { rawExpanded = !rawExpanded }
                )
                AnimatedVisibility(visible = rawExpanded) {
                    JsonViewer(rawJson = item.rawExtrasJson)
                }
            }
        }
    }
}
