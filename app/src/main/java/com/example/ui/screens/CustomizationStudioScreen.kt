package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.VaultViewModel
import com.example.data.NotificationEntity
import com.example.ui.components.AmbientMeshBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.MorphSlider
import com.example.ui.components.NotificationCard
import com.example.ui.components.SegmentedGlassControl
import com.example.ui.components.StatPill
import com.example.ui.components.ThemeTile
import com.example.ui.components.VaultChip
import com.example.ui.theme.DisplayM
import com.example.ui.theme.LocalVaultColors
import com.example.ui.theme.LocalVaultCustomization
import com.example.ui.theme.ShapeL
import com.example.ui.theme.ShapePill
import com.example.ui.theme.ThemePreset
import com.example.ui.theme.VaultBodyM
import com.example.ui.theme.VaultCaption
import com.example.ui.theme.VaultLabel
import com.example.ui.theme.VaultTitle

@Composable
fun CustomizationStudioScreen(
    viewModel: VaultViewModel? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current
    val customization = LocalVaultCustomization.current

    val allNotifications = viewModel?.allNotifications?.collectAsStateWithLifecycle()?.value ?: emptyList()
    val previewNotification = allNotifications.firstOrNull()

    AmbientMeshBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp)
        ) {
            // Header with reset preset
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Studio",
                        style = DisplayM.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Obsidian Glass Customization Engine",
                        style = VaultCaption.copy(fontSize = 12.sp, color = colors.textTertiary)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.surface.copy(alpha = 0.8f))
                        .border(1.dp, colors.cardStroke, CircleShape)
                        .clickable {
                            customization.currentTheme = ThemePreset.MIDNIGHT_ONYX
                            customization.isAmoled = false
                            customization.isGrainEnabled = true
                            customization.isAmbientMeshEnabled = true
                            customization.glassIntensity = 0.85f
                            customization.listStyle = "Cards"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset Theme",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2.7 STICKY LIVE PREVIEW STAGE
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    text = "LIVE RENDER PREVIEW",
                    style = VaultLabel.copy(fontSize = 10.sp, color = colors.accent.base)
                )
                Spacer(modifier = Modifier.height(10.dp))
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ShapeL,
                    accentBorder = true
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (previewNotification != null) {
                            NotificationCard(
                                notification = previewNotification,
                                listStyle = customization.listStyle,
                                onClick = {}
                            )
                        } else {
                            Text(
                                text = "Theme & Card Preview",
                                style = VaultTitle.copy(fontSize = 15.sp, color = colors.textPrimary)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Adjust blur, glow, and theme colors below. Your settings apply in real time to all captured notifications.",
                                style = VaultCaption.copy(fontSize = 12.sp, color = colors.textSecondary)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        val currentOtpCount = viewModel?.otpCount?.collectAsStateWithLifecycle()?.value ?: 0
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatPill(
                                icon = Icons.Default.Security,
                                count = currentOtpCount,
                                label = "OTPs",
                                accentPalette = colors.accent,
                                isSelected = true,
                                onClick = {}
                            )
                            VaultChip(
                                text = "Active Preset",
                                isSelected = true,
                                onClick = {}
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. THEME PRESETS (8 THEMES)
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "THEME SELECTION",
                    style = VaultLabel.copy(fontSize = 11.sp, color = colors.textTertiary)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemePreset.entries.forEach { preset ->
                        ThemeTile(
                            preset = preset,
                            isSelected = customization.currentTheme == preset,
                            onClick = {
                                customization.currentTheme = preset
                                if (preset.isLight) customization.isAmoled = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. APPEARANCE (Dark / Light / AMOLED)
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "DISPLAY MODE",
                    style = VaultLabel.copy(fontSize = 11.sp, color = colors.textTertiary)
                )
                Spacer(modifier = Modifier.height(10.dp))
                SegmentedGlassControl(
                    options = listOf("Midnight Luxe", "AMOLED Black", "Ivory White"),
                    selectedIndex = when {
                        customization.currentTheme == ThemePreset.IVORY_LUXE -> 2
                        customization.isAmoled -> 1
                        else -> 0
                    },
                    onSelect = { idx ->
                        when (idx) {
                            0 -> {
                                customization.currentTheme = ThemePreset.MIDNIGHT_ONYX
                                customization.isAmoled = false
                            }
                            1 -> {
                                customization.currentTheme = ThemePreset.MIDNIGHT_ONYX
                                customization.isAmoled = true
                            }
                            2 -> {
                                customization.currentTheme = ThemePreset.IVORY_LUXE
                                customization.isAmoled = false
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. LIST STYLES (Cards / Timeline / Bubbles / Compact / Magazine)
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "VAULT LIST PRESENTATION",
                    style = VaultLabel.copy(fontSize = 11.sp, color = colors.textTertiary)
                )
                Spacer(modifier = Modifier.height(10.dp))
                SegmentedGlassControl(
                    options = listOf("Cards", "Timeline", "Bubbles", "Compact", "Magazine"),
                    selectedIndex = when (customization.listStyle) {
                        "Timeline" -> 1
                        "Bubbles" -> 2
                        "Compact" -> 3
                        "Magazine" -> 4
                        else -> 0
                    },
                    onSelect = { idx ->
                        customization.listStyle = when (idx) {
                            1 -> "Timeline"
                            2 -> "Bubbles"
                            3 -> "Compact"
                            4 -> "Magazine"
                            else -> "Cards"
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. ATMOSPHERIC SHADER CONTROLS (Glass Intensity, Grain, Ambient Mesh)
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "ATMOSPHERIC SHADERS",
                    style = VaultLabel.copy(fontSize = 11.sp, color = colors.textTertiary)
                )
                Spacer(modifier = Modifier.height(10.dp))
                GlassCard(modifier = Modifier.fillMaxWidth(), shape = ShapeL) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Glass Intensity Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Glass Blur & Translucency", style = VaultBodyM.copy(color = colors.textPrimary))
                            Text("${(customization.glassIntensity * 100).toInt()}%", style = VaultCaption.copy(color = colors.accent.base))
                        }
                        MorphSlider(
                            value = customization.glassIntensity,
                            onValueChange = { customization.glassIntensity = it }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Grain Texture Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Tactile Print Grain", style = VaultBodyM.copy(color = colors.textPrimary))
                                Text("Subtle analog noise shader", style = VaultCaption.copy(color = colors.textTertiary))
                            }
                            Switch(
                                checked = customization.isGrainEnabled,
                                onCheckedChange = { customization.isGrainEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.accent.base,
                                    checkedTrackColor = colors.accent.glow
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Ambient Mesh Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Drifting Ambient Mesh", style = VaultBodyM.copy(color = colors.textPrimary))
                                Text("40s fluid gradient animation", style = VaultCaption.copy(color = colors.textTertiary))
                            }
                            Switch(
                                checked = customization.isAmbientMeshEnabled,
                                onCheckedChange = { customization.isAmbientMeshEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.accent.base,
                                    checkedTrackColor = colors.accent.glow
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
