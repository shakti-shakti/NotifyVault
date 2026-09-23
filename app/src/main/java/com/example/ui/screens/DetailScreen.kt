package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.NotificationEntity
import com.example.data.NotificationHistoryEntity
import com.example.data.ReplayExplainerStore
import com.example.data.ReplayResult
import com.example.ui.components.ActionCircleButton
import com.example.ui.components.AmbientMeshBackground
import com.example.ui.components.AppIconOrb
import com.example.ui.components.GlassCard
import com.example.ui.components.JsonViewer
import com.example.ui.components.KeyValueRow
import com.example.ui.components.PriorityRibbon
import com.example.ui.components.SectionHeader
import com.example.ui.components.instantTap
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
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
    val history by remember(notificationId) { viewModel.getNotificationHistory(notificationId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val liveKeys by viewModel.liveNotificationKeys.collectAsStateWithLifecycle()
    val replayStore = remember(context) { ReplayExplainerStore(context) }
    val hasSeenExplainer by replayStore.hasSeen.collectAsStateWithLifecycle(initialValue = false)
    val replayScope = rememberCoroutineScope()

    var contentExpanded by remember { mutableStateOf(true) }
    var actionsExpanded by remember { mutableStateOf(true) }
    var conversationExpanded by remember { mutableStateOf(true) }
    var channelExpanded by remember { mutableStateOf(true) }
    var flagsExpanded by remember { mutableStateOf(true) }
    var rawExpanded by remember { mutableStateOf(false) }
    var replayFeedback by remember(notificationId) { mutableStateOf<String?>(null) }
    var showExplainer by remember(notificationId) { mutableStateOf(false) }

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
    val exactReplayReady = remember(item, liveKeys) { viewModel.isExactReplayReady(item) }

    fun handleReplay(result: ReplayResult) {
        when (result) {
            ReplayResult.ExactReplay, ReplayResult.ActionReplay -> {
                replayFeedback = null
            }
            ReplayResult.DeepLinkReplay -> replayFeedback = "Opened via deep link"
            ReplayResult.AppLaunch -> replayFeedback = "Opened the app — original screen no longer available."
            ReplayResult.AppDetails -> replayFeedback = "Opened app settings."
            ReplayResult.Failed -> replayFeedback = "This notification can no longer be opened."
        }
        if (result !is ReplayResult.ExactReplay &&
            result !is ReplayResult.ActionReplay &&
            !hasSeenExplainer
        ) {
            showExplainer = true
        }
        if (result is ReplayResult.DeepLinkReplay ||
            result is ReplayResult.AppLaunch ||
            result is ReplayResult.AppDetails
        ) {
            replayScope.launch {
                delay(2200)
                replayFeedback = null
            }
        }
    }

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
                        .instantTap(onClick = onBack),
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

            // HERO HEADER (280dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppIconOrb(
                    appName = item.appName,
                    accentColor = appColor,
                    iconPath = item.appIconPath,
                    packageName = item.packageName,
                    size = 80
                )
                Spacer(modifier = Modifier.height(14.dp))
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
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = SimpleDateFormat("dd MMMM yyyy · HH:mm:ss", Locale.getDefault()).format(Date(item.captureTime)),
                    style = VaultCaption.copy(fontSize = 12.sp, color = colors.textSecondary)
                )
            }

            // OPEN ORIGINAL: exact live PendingIntent first, then honest fallbacks.
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = ShapePill,
                accentBorder = exactReplayReady
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .instantTap(
                            onLongClick = {
                                replayFeedback = "Exact replay works while the notification is live. After that, we open the app."
                                replayScope.launch {
                                    delay(3000)
                                    replayFeedback = null
                                }
                            },
                            onClick = { handleReplay(viewModel.replay(item)) }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(appColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.OpenInNew, null, tint = appColor, modifier = Modifier.size(19.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Open Original",
                            style = VaultTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Text(
                            if (exactReplayReady) "Exact replay ready" else "Opens the app",
                            style = VaultCaption.copy(color = if (exactReplayReady) Color(0xFF6EE7B7) else AurumPalette.base)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (exactReplayReady) Color(0xFF6EE7B7) else AurumPalette.base)
                    )
                }
            }

            if (showExplainer) {
                Spacer(modifier = Modifier.height(10.dp))
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = ShapeL,
                    accentBorder = true
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "About “Open Original”",
                            style = VaultTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "While a notification is still in your status bar, NotifyVault can reopen the exact same screen. Once it is dismissed or the device reboots, Android no longer allows that; NotifyVault opens the app instead.",
                            style = VaultBodyM.copy(fontSize = 12.sp),
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Got it",
                            style = VaultLabel.copy(color = colors.accent.base),
                            modifier = Modifier
                                .clip(ShapePill)
                                .clickable {
                                    showExplainer = false
                                    replayScope.launch { replayStore.markSeen() }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            replayFeedback?.let { feedback ->
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = ShapePill,
                    accentBorder = feedback.contains("no longer")
                ) {
                    Text(
                        feedback,
                        style = VaultCaption.copy(color = if (feedback.contains("no longer")) Color(0xFFFB7185) else colors.textSecondary),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }
            }

            // ACTION BAR: Star + Copy + Share + Delete
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

            Spacer(modifier = Modifier.height(18.dp))

            // PICTURE / LARGE IMAGE PREVIEW (IF ATTACHED)
            val pictureFile = item.picturePath?.let { File(it) }?.takeIf { it.exists() }
            val largeIconFile = item.largeIconPath?.let { File(it) }?.takeIf { it.exists() }

            if (pictureFile != null) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        shape = ShapeL
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(pictureFile)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Notification Picture Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // SECTION 1: NOTIFICATION CONTENT
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
                            // Title
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

                            // Body Text
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
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            // SubText
                            if (!item.subText.isNullOrBlank()) {
                                KeyValueRow(key = "SubText", value = item.subText)
                            }
                            // SummaryText
                            if (!item.summaryText.isNullOrBlank()) {
                                KeyValueRow(key = "SummaryText", value = item.summaryText)
                            }
                            // InfoText
                            if (!item.infoText.isNullOrBlank()) {
                                KeyValueRow(key = "InfoText", value = item.infoText)
                            }

                            // Large Icon preview if present and not full picture
                            if (largeIconFile != null && pictureFile == null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "LARGE ICON ATTACHED",
                                    style = VaultLabel.copy(fontSize = 10.sp, color = colors.textTertiary)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(largeIconFile)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Large Icon",
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, colors.cardStroke, RoundedCornerShape(12.dp))
                                )
                            }

                            // OTP Box if detected
                            if (item.hasOtp && item.otpCode != null) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(ShapeM)
                                        .background(CyanPulsePalette.glow)
                                        .border(1.dp, CyanPulsePalette.base, ShapeM)
                                        .instantTap(onClick = { clipboardManager.setText(AnnotatedString(item.otpCode)) })
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

            // SECTION: ACTION ITEMS (IF NOTIFICATION HAS ACTIONS)
            val parsedActions = remember(item.actionsJson) {
                try {
                    val arr = JSONArray(item.actionsJson)
                    val list = mutableListOf<String>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i)
                        val title = obj?.optString("title").orEmpty()
                        if (title.isNotBlank()) list.add(title)
                    }
                    list
                } catch (e: Exception) {
                    emptyList<String>()
                }
            }

            if (parsedActions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SectionHeader(
                        title = "Notification Actions (${parsedActions.size})",
                        isExpanded = actionsExpanded,
                        onToggle = { actionsExpanded = !actionsExpanded }
                    )
                    AnimatedVisibility(visible = actionsExpanded) {
                        GlassCard(modifier = Modifier.fillMaxWidth(), shape = ShapeL) {
                            FlowRow(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                parsedActions.forEach { actionTitle ->
                                    val actionLive = remember(actionTitle, liveKeys) {
                                        viewModel.isActionLive(item, actionTitle)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .clip(ShapePill)
                                                .background(
                                                    if (actionLive) appColor.copy(alpha = 0.15f)
                                                    else colors.surface.copy(alpha = 0.35f)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (actionLive) appColor.copy(alpha = 0.5f)
                                                    else colors.cardStroke,
                                                    ShapePill
                                                )
                                                .then(
                                                    if (actionLive) {
                                                        Modifier.instantTap {
                                                            handleReplay(viewModel.replayAction(item, actionTitle))
                                                        }
                                                    } else Modifier
                                                )
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = actionTitle,
                                                style = VaultLabel.copy(
                                                    color = if (actionLive) appColor else colors.textTertiary,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }
                                        if (!actionLive) {
                                            Text(
                                                "Action no longer available",
                                                style = VaultCaption.copy(fontSize = 9.sp, color = colors.textTertiary),
                                                modifier = Modifier.padding(top = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION: MESSAGING CONVERSATION (IF MESSAGINGSTYLE)
            val parsedMessages = remember(item.messagingMessagesJson) {
                try {
                    val arr = JSONArray(item.messagingMessagesJson)
                    val list = mutableListOf<Triple<String, String, Long>>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i)
                        if (obj != null) {
                            list.add(
                                Triple(
                                    obj.optString("sender"),
                                    obj.optString("text"),
                                    obj.optLong("time")
                                )
                            )
                        }
                    }
                    list
                } catch (e: Exception) {
                    emptyList<Triple<String, String, Long>>()
                }
            }

            if (parsedMessages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SectionHeader(
                        title = "Conversation Thread (${parsedMessages.size})",
                        isExpanded = conversationExpanded,
                        onToggle = { conversationExpanded = !conversationExpanded }
                    )
                    AnimatedVisibility(visible = conversationExpanded) {
                        GlassCard(modifier = Modifier.fillMaxWidth(), shape = ShapeL) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                parsedMessages.forEachIndexed { index, (sender, text, time) ->
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (sender.isNotBlank()) sender else "Sender",
                                                style = VaultLabel.copy(color = appColor, fontSize = 11.sp)
                                            )
                                            if (time > 0) {
                                                Text(
                                                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(time)),
                                                    style = VaultCaption.copy(fontSize = 10.sp, color = colors.textTertiary)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = text,
                                            style = VaultBodyM.copy(color = colors.textPrimary, fontSize = 13.sp)
                                        )
                                    }
                                    if (index < parsedMessages.size - 1) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (history.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SectionHeader(
                        title = "History (${history.size})",
                        isExpanded = true,
                        onToggle = {}
                    )
                    GlassCard(modifier = Modifier.fillMaxWidth(), shape = ShapeL) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            history.forEachIndexed { index, version ->
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = version.previousTitle ?: "Previous version",
                                            style = VaultTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault())
                                                .format(Date(version.replacedAt)),
                                            style = VaultCaption.copy(fontSize = 10.sp, color = colors.textTertiary)
                                        )
                                    }
                                    if (!version.previousText.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = version.previousText,
                                            style = VaultBodyM.copy(fontSize = 12.sp),
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                                if (index < history.lastIndex) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 2: APP, CHANNEL & ROUTING INFO
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionHeader(
                    title = "App & Routing Info",
                    isExpanded = channelExpanded,
                    onToggle = { channelExpanded = !channelExpanded }
                )
                AnimatedVisibility(visible = channelExpanded) {
                    GlassCard(modifier = Modifier.fillMaxWidth(), shape = ShapeL) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            KeyValueRow(key = "App Label", value = item.appName)
                            KeyValueRow(key = "Package Name", value = item.packageName)
                            KeyValueRow(key = "Notification ID", value = item.notificationId.toString())
                            KeyValueRow(key = "Notification Tag", value = item.tag ?: "None")
                            KeyValueRow(key = "Channel Name", value = item.channelName ?: "Default")
                            KeyValueRow(key = "Channel ID", value = item.channelId ?: "None")
                            KeyValueRow(key = "Category", value = item.category, isAccent = true)
                            KeyValueRow(key = "Importance", value = item.importance.toString())
                            KeyValueRow(key = "Priority", value = item.priority.toString())
                            KeyValueRow(key = "Group Key", value = item.groupKey ?: "None")
                            KeyValueRow(key = "Sort Key", value = item.sortKey ?: "None")
                            KeyValueRow(key = "Post Timestamp", value = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date(item.postTime)))
                            KeyValueRow(key = "Capture Timestamp", value = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date(item.captureTime)))
                            KeyValueRow(key = "Is Group Conversation", value = if (item.isGroupConversation) "YES" else "NO")
                        }
                    }
                }
            }

            // SECTION 3: SYSTEM FLAGS & SENSORY ALERTS
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionHeader(
                    title = "System Flags & Alerts",
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
                            KeyValueRow(key = "Sound Alert", value = if (item.hasSound) "YES" else "NO")
                            KeyValueRow(key = "Vibration Alert", value = if (item.hasVibrate) "YES" else "NO")
                            KeyValueRow(key = "LED Light Alert", value = if (item.hasLights) "YES" else "NO")
                        }
                    }
                }
            }

            // SECTION 4: PARTICIPANTS / PEOPLE (IF ANY)
            val parsedPeople = remember(item.peopleListJson) {
                try {
                    val arr = JSONArray(item.peopleListJson)
                    val list = mutableListOf<String>()
                    for (i in 0 until arr.length()) {
                        val p = arr.optString(i)
                        if (p.isNotBlank()) list.add(p)
                    }
                    list
                } catch (e: Exception) {
                    emptyList<String>()
                }
            }

            if (parsedPeople.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    GlassCard(modifier = Modifier.fillMaxWidth(), shape = ShapeL) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.People, "People", tint = appColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Participants & People (${parsedPeople.size})", style = VaultLabel.copy(color = colors.textPrimary, fontWeight = FontWeight.Bold))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            parsedPeople.forEach { person ->
                                Text("• $person", style = VaultBodyM.copy(color = colors.textSecondary, fontSize = 12.sp))
                            }
                        }
                    }
                }
            }

            // SECTION 5: RAW PAYLOAD JSON
            Spacer(modifier = Modifier.height(16.dp))
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
