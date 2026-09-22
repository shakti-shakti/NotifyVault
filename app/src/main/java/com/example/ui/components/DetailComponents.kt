package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalVaultColors
import com.example.ui.theme.ShapeL
import com.example.ui.theme.ShapeM
import com.example.ui.theme.ShapePill
import com.example.ui.theme.ShapeS
import com.example.ui.theme.ThemePreset
import com.example.ui.theme.VaultBodyL
import com.example.ui.theme.VaultBodyM
import com.example.ui.theme.VaultCaption
import com.example.ui.theme.VaultLabel
import com.example.ui.theme.VaultMono
import com.example.ui.theme.VaultMotion
import com.example.ui.theme.VaultTitle

// Section Header with rotating chevron for collapsible sections
@Composable
fun SectionHeader(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = VaultMotion.springSnappy(),
        label = "chevronRotation"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title.uppercase(),
            style = VaultLabel.copy(
                fontSize = 12.sp,
                letterSpacing = 0.1.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textTertiary
            )
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Expand",
            tint = colors.textTertiary,
            modifier = Modifier
                .size(18.dp)
                .rotate(rotation)
        )
    }
}

// Key-Value Row for Metadata Inspection
@Composable
fun KeyValueRow(
    key: String,
    value: String,
    isAccent: Boolean = false,
    onCopy: (() -> Unit)? = null
) {
    val colors = LocalVaultColors.current
    val clipboardManager = LocalClipboardManager.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onCopy != null) {
                clipboardManager.setText(AnnotatedString(value))
                onCopy?.invoke()
            }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = key,
            style = VaultCaption.copy(
                color = colors.textTertiary,
                fontSize = 12.sp
            )
        )
        Text(
            text = value,
            style = VaultBodyM.copy(
                fontSize = 13.sp,
                fontWeight = if (isAccent) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isAccent) colors.accent.base else colors.textPrimary
            )
        )
    }
}

// JSON Viewer with syntax highlight and copy
@Composable
fun JsonViewer(
    rawJson: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeM
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RAW EXTRAS PAYLOAD",
                    style = VaultLabel.copy(fontSize = 10.sp, color = colors.textTertiary)
                )
                Row(
                    modifier = Modifier.clickable {
                        clipboardManager.setText(AnnotatedString(rawJson))
                        copied = true
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy JSON",
                        tint = colors.accent.base,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (copied) "COPIED" else "COPY",
                        style = VaultCaption.copy(
                            color = colors.accent.base,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ShapeS)
                    .background(Color(0xFF07070B))
                    .padding(12.dp)
            ) {
                Text(
                    text = if (rawJson.isBlank()) "{}" else rawJson,
                    style = VaultMono.copy(
                        fontSize = 11.sp,
                        color = colors.accent.gradientStart,
                        lineHeight = 16.sp
                    )
                )
            }
        }
    }
}

// 44dp Circular Action Button with Glass and Spring Press
@Composable
fun ActionCircleButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = LocalVaultColors.current.textPrimary
) {
    val colors = LocalVaultColors.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = VaultMotion.springSnappy(),
        label = "btnScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(scale)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.surface.copy(alpha = 0.8f))
                .border(1.dp, colors.cardStroke, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = VaultCaption.copy(
                fontSize = 10.sp,
                color = colors.textTertiary
            )
        )
    }
}

// Full-width Gradient Pill Button (Primary CTA)
@Composable
fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val colors = LocalVaultColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(ShapePill)
            .background(colors.accent.brush())
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (colors.isLight) Color.White else Color(0xFF0B0B12),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = VaultTitle.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (colors.isLight) Color.White else Color(0xFF0B0B12)
                )
            )
        }
    }
}

// Text-Only Accent Ghost Button
@Composable
fun GhostTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LocalVaultColors.current.accent.base
) {
    Box(
        modifier = modifier
            .clip(ShapePill)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = VaultLabel.copy(
                color = color,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        )
    }
}

// Segmented Glass Control
@Composable
fun SegmentedGlassControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(ShapePill)
            .background(colors.elevated.copy(alpha = 0.5f))
            .border(1.dp, colors.cardStroke, ShapePill)
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                val segmentBgModifier = if (isSelected) {
                    Modifier.background(colors.accent.brush())
                } else {
                    Modifier
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(ShapePill)
                        .then(segmentBgModifier)
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        style = VaultCaption.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            color = if (isSelected) {
                                if (colors.isLight) Color.White else Color(0xFF0B0B12)
                            } else {
                                colors.textSecondary
                            }
                        )
                    )
                }
            }
        }
    }
}

// Morph Slider
@Composable
fun MorphSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        colors = SliderDefaults.colors(
            thumbColor = colors.accent.base,
            activeTrackColor = colors.accent.base,
            inactiveTrackColor = colors.cardStroke
        ),
        modifier = modifier
    )
}

// Theme Tile with live preview and active ring
@Composable
fun ThemeTile(
    preset: ThemePreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalVaultColors.current
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(ShapeL)
            .background(preset.bg)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) preset.accent.base else colors.cardStroke,
                shape = ShapeL
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(preset.accent.brush())
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = preset.title.take(7),
                style = VaultCaption.copy(
                    fontSize = 9.sp,
                    color = if (preset.isLight) Color.Black else Color.White
                )
            )
        }
    }
}

// Shimmer Loader Box with sweep
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -100f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            colors.surface.copy(alpha = 0.6f),
            colors.accent.base.copy(alpha = 0.18f),
            colors.surface.copy(alpha = 0.6f)
        ),
        start = Offset(translateAnim, translateAnim),
        end = Offset(translateAnim + 200f, translateAnim + 200f)
    )

    Box(
        modifier = modifier
            .clip(ShapeM)
            .background(brush)
    )
}

// Empty State View
@Composable
fun EmptyState(
    title: String,
    description: String,
    icon: ImageVector,
    ctaText: String? = null,
    onCtaClick: (() -> Unit)? = null
) {
    val colors = LocalVaultColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(colors.accent.glow)
                .border(1.dp, colors.accent.base.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent.base,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = VaultTitle.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            style = VaultBodyM.copy(fontSize = 13.sp, color = colors.textTertiary),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        if (ctaText != null && onCtaClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            GhostTextButton(text = ctaText, onClick = onCtaClick)
        }
    }
}
