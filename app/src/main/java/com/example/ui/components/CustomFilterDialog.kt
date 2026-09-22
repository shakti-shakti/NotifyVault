package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterAlt
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.CustomFilterChip
import com.example.ui.theme.CrimsonPalette
import com.example.ui.theme.DisplayM
import com.example.ui.theme.LocalVaultColors
import com.example.ui.theme.ShapeL
import com.example.ui.theme.ShapeM
import com.example.ui.theme.ShapePill
import com.example.ui.theme.VaultBodyM
import com.example.ui.theme.VaultCaption
import com.example.ui.theme.VaultLabel
import com.example.ui.theme.VaultTitle

enum class FilterRuleType(val label: String) {
    APPS("Target Apps"),
    KEYWORDS("Keywords"),
    REGEX("Regex")
}

@Composable
fun CustomFilterDialog(
    initialChip: CustomFilterChip? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, packages: Set<String>, keywords: List<String>, regex: String?, colorHex: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val colors = LocalVaultColors.current

    var name by remember { mutableStateOf(initialChip?.name ?: "") }
    var selectedColor by remember { mutableStateOf(initialChip?.colorHex ?: "#E2A84B") }
    var ruleType by remember {
        mutableStateOf(
            when {
                initialChip?.regex != null -> FilterRuleType.REGEX
                initialChip?.keywords?.isNotEmpty() == true -> FilterRuleType.KEYWORDS
                else -> FilterRuleType.APPS
            }
        )
    }

    var selectedPackages by remember { mutableStateOf(initialChip?.packages ?: emptySet()) }
    var keywordsInput by remember { mutableStateOf(initialChip?.keywords?.joinToString(", ") ?: "") }
    var regexInput by remember { mutableStateOf(initialChip?.regex ?: "") }

    var showAppPicker by remember { mutableStateOf(false) }

    val paletteHexes = listOf(
        "#E2A84B", // Gold / Aurum
        "#10B981", // Emerald
        "#A855F7", // Amethyst
        "#06B6D4", // Cyan
        "#FB7185", // Crimson
        "#94A3B8"  // Slate
    )

    if (showAppPicker) {
        AppPickerSheet(
            title = "Select Filter Apps",
            mode = AppPickerMode.MULTI_SELECT_CUSTOM_CHIP,
            initiallySelected = selectedPackages,
            onClose = { showAppPicker = false },
            onConfirmSelection = { pkgs ->
                selectedPackages = pkgs
                showAppPicker = false
            }
        )
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = ShapeL,
            accentBorder = true
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialChip == null) "Create Custom Filter" else "Edit Filter",
                        style = DisplayM.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )

                    if (onDelete != null) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CrimsonPalette.glow)
                                .clickable { onDelete() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Delete, null, tint = CrimsonPalette.base, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. FILTER NAME
                Text("FILTER NAME", style = VaultLabel.copy(fontSize = 10.sp, color = colors.textTertiary))
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
                        value = name,
                        onValueChange = { if (it.length <= 16) name = it },
                        textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        cursorBrush = SolidColor(colors.accent.base),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (name.isEmpty()) Text("e.g. Finance, Work, Amazon", style = VaultBodyM.copy(fontSize = 12.sp, color = colors.textTertiary))
                            inner()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. ACCENT COLOR
                Text("COLOR ACCENT", style = VaultLabel.copy(fontSize = 10.sp, color = colors.textTertiary))
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    paletteHexes.forEach { hex ->
                        val parsed = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Gray }
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(parsed)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. MATCHING CRITERIA TYPE
                Text("MATCHING CRITERIA", style = VaultLabel.copy(fontSize = 10.sp, color = colors.textTertiary))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterRuleType.values().forEach { t ->
                        val isSel = ruleType == t
                        Box(
                            modifier = Modifier
                                .clip(ShapePill)
                                .background(if (isSel) colors.accent.brush() else SolidColor(colors.surface))
                                .border(1.dp, if (isSel) colors.accent.base else colors.cardStroke, ShapePill)
                                .clickable { ruleType = t }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = t.label,
                                style = VaultCaption.copy(
                                    fontSize = 11.sp,
                                    color = if (isSel) Color(0xFF0A0A10) else colors.textSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (ruleType) {
                    FilterRuleType.APPS -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ShapeM)
                                .background(colors.surface)
                                .border(1.dp, colors.cardStroke, ShapeM)
                                .clickable { showAppPicker = true }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Apps, null, tint = colors.accent.base, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (selectedPackages.isEmpty()) "Tap to select target apps…" else "${selectedPackages.size} apps selected",
                                        style = VaultBodyM.copy(fontSize = 12.sp, color = colors.textPrimary)
                                    )
                                }
                                Text("Choose", style = VaultCaption.copy(color = colors.accent.base, fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    FilterRuleType.KEYWORDS -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ShapeM)
                                .background(colors.surface)
                                .border(1.dp, colors.cardStroke, ShapeM)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            BasicTextField(
                                value = keywordsInput,
                                onValueChange = { keywordsInput = it },
                                textStyle = TextStyle(color = colors.textPrimary, fontSize = 13.sp),
                                cursorBrush = SolidColor(colors.accent.base),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { inner ->
                                    if (keywordsInput.isEmpty()) {
                                        Text("Comma-separated e.g. receipt, invoice, order", style = VaultBodyM.copy(fontSize = 11.sp, color = colors.textTertiary))
                                    }
                                    inner()
                                }
                            )
                        }
                    }

                    FilterRuleType.REGEX -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ShapeM)
                                .background(colors.surface)
                                .border(1.dp, colors.cardStroke, ShapeM)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            BasicTextField(
                                value = regexInput,
                                onValueChange = { regexInput = it },
                                textStyle = TextStyle(color = colors.textPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                                cursorBrush = SolidColor(colors.accent.base),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { inner ->
                                    if (regexInput.isEmpty()) {
                                        Text("Regular expression e.g. .*(refund|credited).*", style = VaultBodyM.copy(fontSize = 11.sp, color = colors.textTertiary))
                                    }
                                    inner()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                val canSave = name.isNotBlank() && (
                    (ruleType == FilterRuleType.APPS && selectedPackages.isNotEmpty()) ||
                    (ruleType == FilterRuleType.KEYWORDS && keywordsInput.isNotBlank()) ||
                    (ruleType == FilterRuleType.REGEX && regexInput.isNotBlank())
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(ShapePill)
                            .background(colors.surface)
                            .border(1.dp, colors.cardStroke, ShapePill)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancel", style = VaultTitle.copy(fontSize = 13.sp, color = colors.textSecondary))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(ShapePill)
                            .background(if (canSave) colors.accent.brush() else SolidColor(colors.surface))
                            .clickable(enabled = canSave) {
                                val kwList = if (ruleType == FilterRuleType.KEYWORDS) {
                                    keywordsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                } else emptyList()

                                val reg = if (ruleType == FilterRuleType.REGEX) regexInput.trim() else null
                                val pkgs = if (ruleType == FilterRuleType.APPS) selectedPackages else emptySet()

                                onSave(name.trim(), pkgs, kwList, reg, selectedColor)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Save Filter",
                            style = VaultTitle.copy(
                                fontSize = 13.sp,
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
