package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ExcludeRulesRepository
import com.example.data.ExcludeTextRule
import com.example.data.TextMatchType
import com.example.ui.components.AmbientMeshBackground
import com.example.ui.components.AppPickerMode
import com.example.ui.components.AppPickerSheet
import com.example.ui.components.GlassCard
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ExclusionRulesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current
    val context = LocalContext.current
    val repo = remember { ExcludeRulesRepository.getInstance(context) }

    val masterFiltering by repo.isMasterFilteringEnabled.collectAsState()
    val excludedPackages by repo.excludedPackages.collectAsState()
    val textRules by repo.textRules.collectAsState()
    val excludedCategories by repo.excludedCategories.collectAsState()
    val minPriority by repo.minPriority.collectAsState()
    val quietHoursEnabled by repo.isQuietHoursEnabled.collectAsState()
    val quietHoursStartMin by repo.quietHoursStartMinute.collectAsState()
    val quietHoursEndMin by repo.quietHoursEndMinute.collectAsState()
    val repeatWindowMin by repo.repeatWindowMinutes.collectAsState()
    val excludeOngoing by repo.excludeOngoing.collectAsState()
    val excludeMedia by repo.excludeMedia.collectAsState()
    val excludeSystem by repo.excludeSystem.collectAsState()
    val excludedTodayCount by repo.excludedTodayCount.collectAsState()
    val recentExclusions by repo.recentExclusions.collectAsState()

    var showAppPicker by remember { mutableStateOf(false) }
    var showAddTextRuleDialog by remember { mutableStateOf(false) }
    var showCategoriesDialog by remember { mutableStateOf(false) }
    var showQuietHoursDialog by remember { mutableStateOf(false) }
    var showExclusionLogDialog by remember { mutableStateOf(false) }

    if (showAppPicker) {
        AppPickerSheet(
            title = "Exclude Apps",
            mode = AppPickerMode.MULTI_SELECT_EXCLUDE,
            initiallySelected = excludedPackages,
            onClose = { showAppPicker = false },
            onConfirmSelection = { selected ->
                repo.setExcludedPackages(selected)
                showAppPicker = false
            }
        )
        return
    }

    AmbientMeshBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.surface.copy(alpha = 0.8f))
                            .border(1.dp, colors.cardStroke, CircleShape)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Capture Exclusion Rules", style = DisplayM.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = colors.textPrimary)
                        Text("Block unwanted notifications at capture", style = VaultCaption.copy(fontSize = 11.sp, color = colors.textTertiary))
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. MASTER FILTER TOGGLE & STATS CARD
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), accentBorder = masterFiltering) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Capture Filtering", style = VaultTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = colors.textPrimary)
                                    Text(
                                        if (masterFiltering) "Active · Filter rules are being applied" else "Disabled · All notifications are captured",
                                        style = VaultCaption.copy(color = if (masterFiltering) EmeraldPalette.base else colors.textTertiary)
                                    )
                                }
                                Switch(
                                    checked = masterFiltering,
                                    onCheckedChange = { repo.setMasterFilteringEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = colors.accent.base,
                                        checkedTrackColor = colors.accent.glow
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(ShapeM)
                                    .background(colors.surface)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Block, null, tint = colors.accent.base, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Excluded today: $excludedTodayCount", style = VaultBodyM.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium), color = colors.textPrimary)
                                }

                                Text(
                                    text = "View Log",
                                    style = VaultCaption.copy(color = colors.accent.base, fontWeight = FontWeight.Bold),
                                    modifier = Modifier
                                        .clip(ShapePill)
                                        .clickable { showExclusionLogDialog = true }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }

                // 2. EXCLUDE BY APP
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showAppPicker = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Apps, null, tint = colors.accent.base, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Excluded Apps", style = VaultTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                                    Text("${excludedPackages.size} apps excluded", style = VaultCaption.copy(color = colors.textTertiary))
                                }
                            }
                            Text("Manage", style = VaultCaption.copy(color = colors.accent.base, fontWeight = FontWeight.Bold))
                        }
                    }
                }

                // 3. EXCLUDE BY TEXT / KEYWORDS / REGEX
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Tune, null, tint = colors.accent.base, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Text & Keyword Rules", style = VaultTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                                        Text("${textRules.size} rules active", style = VaultCaption.copy(color = colors.textTertiary))
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(ShapePill)
                                        .background(colors.accent.glow)
                                        .border(1.dp, colors.accent.base, ShapePill)
                                        .clickable { showAddTextRuleDialog = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Add, null, tint = colors.accent.base, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add Rule", style = VaultCaption.copy(color = colors.accent.base, fontWeight = FontWeight.Bold))
                                    }
                                }
                            }

                            if (textRules.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    textRules.forEach { rule ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(ShapeM)
                                                .background(colors.surface)
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(rule.pattern, style = VaultTitle.copy(fontSize = 13.sp, fontFamily = FontFamily.Monospace), color = colors.textPrimary)
                                                Text("${rule.matchType.label} · ${if (rule.isCaseSensitive) "Case-sensitive" else "Case-insensitive"}", style = VaultCaption.copy(fontSize = 10.sp, color = colors.textTertiary))
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Switch(
                                                    checked = rule.isEnabled,
                                                    onCheckedChange = { repo.updateTextRule(rule.copy(isEnabled = it)) },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = colors.accent.base,
                                                        checkedTrackColor = colors.accent.glow
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = CrimsonPalette.base,
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clickable { repo.deleteTextRule(rule.id) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. EXCLUDE BY CATEGORY
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showCategoriesDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Category, null, tint = colors.accent.base, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Excluded Categories", style = VaultTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                                    Text("${excludedCategories.size} categories filtered", style = VaultCaption.copy(color = colors.textTertiary))
                                }
                            }
                            Text("Edit", style = VaultCaption.copy(color = colors.accent.base, fontWeight = FontWeight.Bold))
                        }
                    }
                }

                // 5. QUIET HOURS (TIME RANGE)
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showQuietHoursDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Nightlight, null, tint = colors.accent.base, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Quiet Hours", style = VaultTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                                    val startStr = String.format("%02d:%02d", quietHoursStartMin / 60, quietHoursStartMin % 60)
                                    val endStr = String.format("%02d:%02d", quietHoursEndMin / 60, quietHoursEndMin % 60)
                                    Text(if (quietHoursEnabled) "Active ($startStr to $endStr)" else "Disabled", style = VaultCaption.copy(color = if (quietHoursEnabled) EmeraldPalette.base else colors.textTertiary))
                                }
                            }
                            Switch(
                                checked = quietHoursEnabled,
                                onCheckedChange = { repo.setQuietHours(it, quietHoursStartMin, quietHoursEndMin) },
                                colors = SwitchDefaults.colors(checkedThumbColor = colors.accent.base, checkedTrackColor = colors.accent.glow)
                            )
                        }
                    }
                }

                // 6. DUPLICATE NOTIFICATION REPEAT WINDOW
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Repeat, null, tint = colors.accent.base, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Duplicate Repeat Window", style = VaultTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                                        Text(if (repeatWindowMin == 0) "Disabled (capture all repeats)" else "Ignore duplicates within $repeatWindowMin min", style = VaultCaption.copy(color = colors.textTertiary))
                                    }
                                }
                                Text("${repeatWindowMin}m", style = VaultLabel.copy(color = colors.accent.base, fontWeight = FontWeight.Bold))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Slider(
                                value = repeatWindowMin.toFloat(),
                                onValueChange = { repo.setRepeatWindowMinutes(it.roundToInt()) },
                                valueRange = 0f..60f,
                                steps = 11,
                                colors = SliderDefaults.colors(
                                    thumbColor = colors.accent.base,
                                    activeTrackColor = colors.accent.base,
                                    inactiveTrackColor = colors.surface
                                )
                            )
                        }
                    }
                }

                // 7. ONGOING, MEDIA, SYSTEM TOGGLES
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("SYSTEM & SPECIAL NOTIFICATIONS", style = VaultLabel.copy(fontSize = 10.sp, color = colors.textTertiary))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Exclude Ongoing Events", style = VaultTitle.copy(fontSize = 13.sp), color = colors.textPrimary)
                                    Text("Ignore persistent foreground alerts & downloads", style = VaultCaption.copy(fontSize = 10.sp, color = colors.textTertiary))
                                }
                                Switch(
                                    checked = excludeOngoing,
                                    onCheckedChange = { repo.setExcludeOngoing(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = colors.accent.base, checkedTrackColor = colors.accent.glow)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Exclude Media Playback", style = VaultTitle.copy(fontSize = 13.sp), color = colors.textPrimary)
                                    Text("Ignore Spotify, YouTube, and player controls", style = VaultCaption.copy(fontSize = 10.sp, color = colors.textTertiary))
                                }
                                Switch(
                                    checked = excludeMedia,
                                    onCheckedChange = { repo.setExcludeMedia(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = colors.accent.base, checkedTrackColor = colors.accent.glow)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Exclude Android System", style = VaultTitle.copy(fontSize = 13.sp), color = colors.textPrimary)
                                    Text("Ignore low-level OS notifications", style = VaultCaption.copy(fontSize = 10.sp, color = colors.textTertiary))
                                }
                                Switch(
                                    checked = excludeSystem,
                                    onCheckedChange = { repo.setExcludeSystem(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = colors.accent.base, checkedTrackColor = colors.accent.glow)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG: ADD TEXT RULE
    if (showAddTextRuleDialog) {
        var pattern by remember { mutableStateOf("") }
        var matchType by remember { mutableStateOf(TextMatchType.CONTAINS) }
        var caseSensitive by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showAddTextRuleDialog = false }) {
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = ShapeL, accentBorder = true) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Add Text Exclusion Rule", style = DisplayM.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("PATTERN OR PHRASE", style = VaultLabel.copy(fontSize = 10.sp, color = colors.textTertiary))
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ShapePill)
                            .background(colors.surface)
                            .border(1.dp, colors.cardStroke, ShapePill)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(
                            value = pattern,
                            onValueChange = { pattern = it },
                            textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp),
                            cursorBrush = SolidColor(colors.accent.base),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                if (pattern.isEmpty()) Text("e.g. 'verification code' or 'delivery'", style = VaultBodyM.copy(fontSize = 12.sp, color = colors.textTertiary))
                                inner()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("MATCH TYPE", style = VaultLabel.copy(fontSize = 10.sp, color = colors.textTertiary))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextMatchType.values().forEach { t ->
                            val isSel = matchType == t
                            Box(
                                modifier = Modifier
                                    .clip(ShapePill)
                                    .background(if (isSel) colors.accent.brush() else SolidColor(colors.surface))
                                    .border(1.dp, if (isSel) colors.accent.base else colors.cardStroke, ShapePill)
                                    .clickable { matchType = t }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(t.label, style = VaultCaption.copy(fontSize = 10.sp, color = if (isSel) Color(0xFF0A0A10) else colors.textSecondary, fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Case sensitive", style = VaultBodyM.copy(fontSize = 13.sp), color = colors.textPrimary)
                        Switch(
                            checked = caseSensitive,
                            onCheckedChange = { caseSensitive = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = colors.accent.base, checkedTrackColor = colors.accent.glow)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.weight(1f).height(42.dp).clip(ShapePill).background(colors.surface).border(1.dp, colors.cardStroke, ShapePill).clickable { showAddTextRuleDialog = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cancel", style = VaultTitle.copy(fontSize = 13.sp, color = colors.textSecondary))
                        }
                        Box(
                            modifier = Modifier.weight(1f).height(42.dp).clip(ShapePill).background(if (pattern.isNotBlank()) colors.accent.brush() else SolidColor(colors.surface)).clickable(enabled = pattern.isNotBlank()) {
                                repo.addTextRule(
                                    ExcludeTextRule(
                                        pattern = pattern.trim(),
                                        matchType = matchType,
                                        isCaseSensitive = caseSensitive
                                    )
                                )
                                showAddTextRuleDialog = false
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Save Rule", style = VaultTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (pattern.isNotBlank()) Color(0xFF0A0A10) else colors.textTertiary))
                        }
                    }
                }
            }
        }
    }

    // DIALOG: CATEGORIES
    if (showCategoriesDialog) {
        val allCats = listOf("OTP", "PAYMENT", "DELIVERY", "BANKING", "SOCIAL", "SYSTEM", "PROMOTIONS", "ONGOING", "GROUP_SUMMARY")
        val currentSelected = remember { mutableStateOf(excludedCategories.toMutableSet()) }

        Dialog(onDismissRequest = { showCategoriesDialog = false }) {
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = ShapeL) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Exclude Categories", style = DisplayM.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Checked categories will be ignored at capture", style = VaultCaption.copy(color = colors.textTertiary))
                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        allCats.forEach { cat ->
                            val isChecked = currentSelected.value.contains(cat)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(ShapeM)
                                    .clickable {
                                        val s = currentSelected.value.toMutableSet()
                                        if (isChecked) s.remove(cat) else s.add(cat)
                                        currentSelected.value = s
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        val s = currentSelected.value.toMutableSet()
                                        if (it) s.add(cat) else s.remove(cat)
                                        currentSelected.value = s
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = colors.accent.base,
                                        checkmarkColor = Color(0xFF0A0A10)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(cat, style = VaultBodyM.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium), color = colors.textPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(ShapePill)
                            .background(colors.accent.brush())
                            .clickable {
                                repo.setExcludedCategories(currentSelected.value)
                                showCategoriesDialog = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Save Categories", style = VaultTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A0A10)))
                    }
                }
            }
        }
    }

    // DIALOG: QUIET HOURS
    if (showQuietHoursDialog) {
        var startH by remember { mutableStateOf(quietHoursStartMin / 60) }
        var endH by remember { mutableStateOf(quietHoursEndMin / 60) }

        Dialog(onDismissRequest = { showQuietHoursDialog = false }) {
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = ShapeL) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Configure Quiet Hours", style = DisplayM.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Start Hour: ${String.format("%02d:00", startH)}", style = VaultTitle.copy(fontSize = 13.sp), color = colors.textPrimary)
                    Slider(
                        value = startH.toFloat(),
                        onValueChange = { startH = it.roundToInt() },
                        valueRange = 0f..23f,
                        steps = 22,
                        colors = SliderDefaults.colors(thumbColor = colors.accent.base, activeTrackColor = colors.accent.base)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("End Hour: ${String.format("%02d:00", endH)}", style = VaultTitle.copy(fontSize = 13.sp), color = colors.textPrimary)
                    Slider(
                        value = endH.toFloat(),
                        onValueChange = { endH = it.roundToInt() },
                        valueRange = 0f..23f,
                        steps = 22,
                        colors = SliderDefaults.colors(thumbColor = colors.accent.base, activeTrackColor = colors.accent.base)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(ShapePill)
                            .background(colors.accent.brush())
                            .clickable {
                                repo.setQuietHours(true, startH * 60, endH * 60)
                                showQuietHoursDialog = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Save Quiet Hours", style = VaultTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A0A10)))
                    }
                }
            }
        }
    }

    // DIALOG: RECENT EXCLUSIONS LOG
    if (showExclusionLogDialog) {
        val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

        Dialog(onDismissRequest = { showExclusionLogDialog = false }) {
            GlassCard(modifier = Modifier.fillMaxWidth().height(480.dp), shape = ShapeL) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Exclusion Log", style = DisplayM.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = colors.textPrimary)
                        Text("${recentExclusions.size} entries", style = VaultCaption.copy(color = colors.textTertiary))
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (recentExclusions.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No notifications excluded yet", style = VaultBodyM.copy(color = colors.textTertiary))
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(recentExclusions) { log ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(ShapeM)
                                        .background(colors.surface)
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(log.appName, style = VaultTitle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = colors.textPrimary)
                                            Text(dateFormat.format(Date(log.timestamp)), style = VaultCaption.copy(fontSize = 10.sp, color = colors.textTertiary))
                                        }
                                        Text(log.title, style = VaultCaption.copy(fontSize = 11.sp, color = colors.textSecondary), maxLines = 1)
                                        Text("Reason: ${log.reason}", style = VaultCaption.copy(fontSize = 10.sp, color = CrimsonPalette.base))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(ShapePill)
                            .background(colors.surface)
                            .border(1.dp, colors.cardStroke, ShapePill)
                            .clickable { showExclusionLogDialog = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Close", style = VaultTitle.copy(fontSize = 12.sp, color = colors.textPrimary))
                    }
                }
            }
        }
    }
}
