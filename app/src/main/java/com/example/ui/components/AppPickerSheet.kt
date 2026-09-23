package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AurumPalette
import com.example.ui.theme.DisplayM
import com.example.ui.theme.EmeraldPalette
import com.example.ui.theme.LocalVaultColors
import com.example.ui.theme.ShapeL
import com.example.ui.theme.ShapeM
import com.example.ui.theme.ShapePill
import com.example.ui.theme.VaultBodyM
import com.example.ui.theme.VaultCaption
import com.example.ui.theme.VaultLabel
import com.example.ui.theme.VaultMotion
import com.example.ui.theme.VaultTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val iconPath: String? = null
)

enum class AppPickerFilterTab {
    ALL, USER, SYSTEM
}

enum class AppPickerMode {
    FILTER_BUILDER,          // Multi-select + Name + 12 Color swatches + Save pill
    MULTI_SELECT_LIMITED,    // Multi-select with max limit (e.g. 6)
    MULTI_SELECT_EXCLUDE,    // Multi-select with struck-through styling
    MULTI_SELECT_CUSTOM_CHIP, // Multi-select apps for a custom filter chip
    SINGLE_SELECT            // Single app tap
}

val SWATCH_COLORS = listOf(
    Color(0xFFE2A84B), // Amber / Aurum
    Color(0xFF6C63FF), // Indigo
    Color(0xFF10B981), // Emerald
    Color(0xFF8B5CF6), // Violet
    Color(0xFFF43F5E), // Ruby / Crimson
    Color(0xFF06B6D4), // Cyan
    Color(0xFFF97316), // Orange
    Color(0xFFEC4899), // Pink
    Color(0xFF3B82F6), // Blue
    Color(0xFF84CC16), // Lime
    Color(0xFFEAB308), // Yellow
    Color(0xFFA855F7)  // Purple
)

@Composable
fun AppPickerSheet(
    title: String,
    mode: AppPickerMode = AppPickerMode.FILTER_BUILDER,
    maxLimit: Int = 6,
    initiallySelected: Set<String> = emptySet(),
    onClose: () -> Unit,
    onConfirmSelection: (Set<String>) -> Unit = {},
    onSingleAppSelected: (InstalledAppItem) -> Unit = {},
    onSaveFilter: (name: String, colorHex: String, packages: Set<String>) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(AppPickerFilterTab.ALL) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val allApps = remember { mutableStateListOf<InstalledAppItem>() }
    val selectedPackages = remember { mutableStateListOf<String>().apply { addAll(initiallySelected) } }

    // Filter builder specific state
    var filterName by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableStateOf(0) }

    fun triggerHapticTick() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(15L)
            }
        } catch (e: Exception) {
            // Ignored
        }
    }

    LaunchedEffect(context) {
        try {
            val list = withContext(Dispatchers.IO) { loadInstalledApps(context) }
            allApps.clear()
            allApps.addAll(list)
        } catch (error: Exception) {
            loadError = error.message ?: "Installed apps could not be loaded."
        } finally {
            isLoading = false
        }
    }

    // Read the snapshot list directly instead of memoizing against its stable
    // list identity. This ensures the UI renders as soon as the IO load ends.
    val filteredApps = allApps.filter { app ->
        val matchesTab = when (selectedTab) {
            AppPickerFilterTab.ALL -> true
            AppPickerFilterTab.USER -> !app.isSystemApp
            AppPickerFilterTab.SYSTEM -> app.isSystemApp
        }
        val matchesSearch = if (searchQuery.isBlank()) {
            true
        } else {
            app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
            }
        matchesTab && matchesSearch
    }

    AmbientMeshBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 1. HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
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
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = title,
                            style = DisplayM.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        if (mode == AppPickerMode.MULTI_SELECT_LIMITED) {
                            Text(
                                text = "${selectedPackages.size} / $maxLimit selected",
                                style = VaultCaption.copy(
                                    color = if (selectedPackages.size >= maxLimit) colors.accent.base else colors.textTertiary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                if (mode != AppPickerMode.SINGLE_SELECT && mode != AppPickerMode.FILTER_BUILDER) {
                    // Done pill button
                    Box(
                        modifier = Modifier
                            .clip(ShapePill)
                            .background(colors.accent.brush())
                            .clickable {
                                triggerHapticTick()
                                onConfirmSelection(selectedPackages.toSet())
                            }
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Done (${selectedPackages.size})",
                            style = VaultLabel.copy(color = Color(0xFF0A0A10), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        )
                    }
                }
            }

            // 2. GLASS SEARCH BAR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(ShapePill)
                    .background(colors.surface.copy(alpha = 0.8f))
                    .border(1.dp, colors.cardStroke, ShapePill)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = colors.textTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(colors.accent.base),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search installed apps or packages…",
                                    style = VaultBodyM.copy(fontSize = 13.sp, color = colors.textTertiary)
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = colors.textTertiary,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            // 3. FILTER TABS (All / User / System) + Select/Deselect All Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        AppPickerFilterTab.ALL to "All Apps",
                        AppPickerFilterTab.USER to "User Apps",
                        AppPickerFilterTab.SYSTEM to "System Apps"
                    ).forEach { (tab, label) ->
                        val isSelected = selectedTab == tab
                        val bg = if (isSelected) colors.accent.brush() else Brush.verticalGradient(
                            listOf(colors.surface.copy(alpha = 0.6f), colors.elevated.copy(alpha = 0.4f))
                        )
                        val textCol = if (isSelected) Color(0xFF0A0A10) else colors.textSecondary

                        Box(
                            modifier = Modifier
                                .clip(ShapePill)
                                .background(bg)
                                .border(1.dp, if (isSelected) colors.accent.base else colors.cardStroke, ShapePill)
                                .clickable {
                                    selectedTab = tab
                                    triggerHapticTick()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = VaultLabel.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = textCol
                                )
                            )
                        }
                    }
                }

                if (mode != AppPickerMode.SINGLE_SELECT && mode != AppPickerMode.MULTI_SELECT_LIMITED) {
                    val allCurrentSelected = filteredApps.isNotEmpty() && filteredApps.all { selectedPackages.contains(it.packageName) }
                    Text(
                        text = if (allCurrentSelected) "Deselect All" else "Select All",
                        style = VaultCaption.copy(color = colors.accent.base, fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        modifier = Modifier
                            .clip(ShapePill)
                            .clickable {
                                triggerHapticTick()
                                if (allCurrentSelected) {
                                    val currentPkgSet = filteredApps.map { it.packageName }.toSet()
                                    selectedPackages.removeAll(currentPkgSet)
                                } else {
                                    val toAdd = filteredApps.map { it.packageName }
                                    selectedPackages.addAll(toAdd.filter { !selectedPackages.contains(it) })
                                }
                            }
                            .padding(4.dp)
                    )
                }
            }

            // 4. APP LIST / LOADING / EMPTY STATE
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = colors.accent.base,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading installed apps…",
                            style = VaultCaption.copy(color = colors.textTertiary)
                        )
                    }
                }
            } else if (loadError != null) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Could not load installed apps",
                            style = VaultBodyM.copy(color = colors.textSecondary)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = loadError.orEmpty(),
                            style = VaultCaption.copy(color = colors.textTertiary),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No apps match '$searchQuery'",
                            style = VaultBodyM.copy(color = colors.textSecondary)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(ShapePill)
                                .background(colors.surface.copy(alpha = 0.8f))
                                .border(1.dp, colors.cardStroke, ShapePill)
                                .clickable {
                                    searchQuery = ""
                                    selectedTab = AppPickerFilterTab.ALL
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Reset Search", style = VaultCaption.copy(color = colors.accent.base))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { appItem ->
                        val isSelected = selectedPackages.contains(appItem.packageName)
                        val isExcludedStyle = mode == AppPickerMode.MULTI_SELECT_EXCLUDE && isSelected

                        AppPickerRow(
                            item = appItem,
                            isSelected = isSelected,
                            isExcludedStyle = isExcludedStyle,
                            isSingleSelect = mode == AppPickerMode.SINGLE_SELECT,
                            onClick = {
                                triggerHapticTick()
                                if (mode == AppPickerMode.SINGLE_SELECT) {
                                    onSingleAppSelected(appItem)
                                } else {
                                    if (isSelected) {
                                        selectedPackages.remove(appItem.packageName)
                                    } else {
                                        if (mode == AppPickerMode.MULTI_SELECT_LIMITED && selectedPackages.size >= maxLimit) {
                                            // Max limit reached
                                            return@AppPickerRow
                                        }
                                        selectedPackages.add(appItem.packageName)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // 5. BOTTOM BAR FOR FILTER BUILDER (Name this filter, 12 colors, Save pill)
            if (mode == AppPickerMode.FILTER_BUILDER) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = ShapeL,
                    accentBorder = true
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "NAME THIS FILTER",
                            style = VaultLabel.copy(fontSize = 10.sp, color = colors.textTertiary)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Glass input with character limit 20
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ShapePill)
                                .background(colors.surface.copy(alpha = 0.9f))
                                .border(1.dp, colors.cardStroke, ShapePill)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = filterName,
                                    onValueChange = { if (it.length <= 20) filterName = it },
                                    textStyle = TextStyle(
                                        color = colors.textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = SolidColor(colors.accent.base),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { inner ->
                                        if (filterName.isEmpty()) {
                                            Text(
                                                "e.g., Banking & Payments",
                                                style = VaultBodyM.copy(fontSize = 13.sp, color = colors.textTertiary)
                                            )
                                        }
                                        inner()
                                    }
                                )
                                Text(
                                    text = "${filterName.length}/20",
                                    style = VaultCaption.copy(fontSize = 10.sp, color = colors.textTertiary)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "ACCENT COLOR",
                            style = VaultLabel.copy(fontSize = 10.sp, color = colors.textTertiary)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // 12 curated swatches
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(SWATCH_COLORS.indices.toList()) { index ->
                                val swatchColor = SWATCH_COLORS[index]
                                val isColorSelected = selectedColorIndex == index
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(
                                            width = if (isColorSelected) 2.5.dp else 1.dp,
                                            color = if (isColorSelected) Color.White else Color(0x33FFFFFF),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            selectedColorIndex = index
                                            triggerHapticTick()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isColorSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val canSave = filterName.isNotBlank() && selectedPackages.isNotEmpty()
                        val saveButtonBg = if (canSave) colors.accent.brush() else Brush.verticalGradient(
                            listOf(colors.surface.copy(alpha = 0.5f), colors.elevated.copy(alpha = 0.3f))
                        )

                        // Save filter pill button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(ShapePill)
                                .background(saveButtonBg)
                                .border(
                                    1.dp,
                                    if (canSave) colors.accent.base else colors.cardStroke,
                                    ShapePill
                                )
                                .clickable(enabled = canSave) {
                                    triggerHapticTick()
                                    val hex = String.format("#%06X", (0xFFFFFF and SWATCH_COLORS[selectedColorIndex].value.toInt()))
                                    onSaveFilter(filterName.trim(), hex, selectedPackages.toSet())
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectedPackages.isEmpty()) "Select at least 1 app" else "Save Filter (${selectedPackages.size} apps)",
                                style = VaultTitle.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (canSave) Color(0xFF0A0A10) else colors.textTertiary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun loadInstalledApps(context: Context): List<InstalledAppItem> {
    val pm = context.packageManager

    fun toItem(appInfo: ApplicationInfo): InstalledAppItem {
        val packageName = appInfo.packageName
        val label = runCatching { pm.getApplicationLabel(appInfo).toString() }
            .getOrDefault(packageName)
            .ifBlank { packageName }
        return InstalledAppItem(
            packageName = packageName,
            appName = label,
            isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        )
    }

    val installed = runCatching {
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .map(::toItem)
            .toList()
    }.getOrDefault(emptyList())

    // Some Android builds and managed profiles restrict the broad package
    // query even when QUERY_ALL_PACKAGES is declared. A launcher query still
    // gives the user the apps they can actually open and select.
    val launchable = if (installed.isNotEmpty()) {
        emptyList()
    } else {
        runCatching {
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
                .asSequence()
                .map { it.activityInfo.applicationInfo }
                .map(::toItem)
                .toList()
        }.getOrDefault(emptyList())
    }

    return (installed + launchable)
        .distinctBy { it.packageName }
        .sortedBy { it.appName.lowercase() }
}

@Composable
private fun AppPickerRow(
    item: InstalledAppItem,
    isSelected: Boolean,
    isExcludedStyle: Boolean,
    isSingleSelect: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalVaultColors.current
    val rowBg by animateColorAsState(
        targetValue = if (isSelected) colors.surface.copy(alpha = 0.9f) else colors.surface.copy(alpha = 0.5f),
        label = "rowBg"
    )
    val strokeColor by animateColorAsState(
        targetValue = if (isSelected) colors.accent.base.copy(alpha = 0.8f) else colors.cardStroke,
        label = "rowStroke"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeM)
            .background(rowBg)
            .border(1.dp, strokeColor, ShapeM)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                AppIconOrb(
                    appName = item.appName,
                    accentColor = colors.accent.base,
                    iconPath = item.iconPath,
                    packageName = item.packageName,
                    size = 36
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.appName,
                        style = VaultTitle.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (isExcludedStyle) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (isExcludedStyle) colors.textTertiary else colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.packageName,
                        style = VaultCaption.copy(
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textTertiary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (!isSingleSelect) {
                // Animated Checkbox
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) colors.accent.base else Color.Transparent)
                        .border(
                            width = 1.5.dp,
                            color = if (isSelected) colors.accent.base else colors.cardStroke,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF0A0A10),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
