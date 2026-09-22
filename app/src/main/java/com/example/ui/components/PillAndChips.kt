package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentPalette
import com.example.ui.theme.AurumPalette
import com.example.ui.theme.DisplayM
import com.example.ui.theme.LocalVaultColors
import com.example.ui.theme.NotifyVaultTheme
import com.example.ui.theme.ShapeL
import com.example.ui.theme.ShapeM
import com.example.ui.theme.ShapePill
import com.example.ui.theme.VaultCaption
import com.example.ui.theme.VaultLabel
import com.example.ui.theme.VaultMotion
import com.example.ui.theme.VaultSpacing

// 2.2 B. STAT PILLS ROW
// Each pill = mini glass card, 92dp tall, radius L:
// • Icon in accent glow circle
// • Big number (tabular, Display M)
// • Tiny ALL-CAPS label (Total / Today / OTPs / Payments / Starred)
// • On tap -> filters the list instantly with animated scroll
// • Distinct accent glow per category

@Composable
fun StatPill(
    icon: ImageVector,
    count: Int,
    label: String,
    accentPalette: AccentPalette,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalVaultColors.current
    val targetScale = if (isSelected) 1.04f else 1.0f
    val scale by animateFloatAsState(targetValue = targetScale, animationSpec = VaultMotion.springSnappy(), label = "scale")

    val bgBorderColor by animateColorAsState(
        targetValue = if (isSelected) accentPalette.base else colors.cardStroke,
        label = "border"
    )

    GlassCard(
        modifier = Modifier
            .widthIn(min = 108.dp)
            .height(92.dp)
            .scale(scale),
        shape = ShapeL,
        accentBorder = isSelected,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.width(96.dp)
            ) {
                // Icon in accent glow circle
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentPalette.glow)
                        .border(1.dp, accentPalette.base.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = accentPalette.base,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Tabular Big Count
                Text(
                    text = count.toString(),
                    style = DisplayM.copy(
                        fontSize = 24.sp,
                        lineHeight = 26.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.textPrimary
                )
            }

            // Tiny ALL-CAPS label
            Text(
                text = label.uppercase(),
                style = VaultLabel.copy(
                    fontSize = 10.sp,
                    color = if (isSelected) accentPalette.base else colors.textTertiary,
                    letterSpacing = 0.1.sp
                )
            )
        }
    }
}

// 2.2 D. FILTER CHIP ROW
// Chips: All · OTPs · Payments · Deliveries · Banking · Messages · Social · System · Starred
// Selected: accent gradient fill, dark text, subtle glow, scale 1.04
// Unselected: glass fill, hairline border, secondary text

@Composable
fun VaultChip(
    text: String,
    isSelected: Boolean,
    accentPalette: AccentPalette = LocalVaultColors.current.accent,
    onClick: () -> Unit
) {
    val colors = LocalVaultColors.current
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1.0f,
        animationSpec = VaultMotion.springSnappy(),
        label = "chipScale"
    )

    val chipBg = if (isSelected) {
        accentPalette.brush()
    } else {
        Brush.verticalGradient(
            listOf(
                colors.surface.copy(alpha = 0.7f),
                colors.elevated.copy(alpha = 0.5f)
            )
        )
    }

    val textColor = if (isSelected) {
        if (colors.isLight) Color.White else Color(0xFF0B0B12)
    } else {
        colors.textSecondary
    }

    Box(
        modifier = Modifier
            .height(36.dp)
            .scale(scale)
            .clip(ShapePill)
            .background(chipBg)
            .border(
                width = 1.dp,
                color = if (isSelected) accentPalette.base else colors.cardStroke,
                shape = ShapePill
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = VaultLabel.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                fontSize = 12.sp
            )
        )
    }
}

// Left 3dp vertical accent bar
@Composable
fun AccentBar(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(3.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(color)
    )
}

// Priority Ribbon for Categories
@Composable
fun PriorityRibbon(
    label: String,
    accentPalette: AccentPalette,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(ShapePill)
            .background(accentPalette.glow)
            .border(0.75.dp, accentPalette.base.copy(alpha = 0.5f), ShapePill)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = VaultCaption.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = accentPalette.base,
                letterSpacing = 0.08.sp
            )
        )
    }
}

// App Icon Orb with Hairline Ring + Glow
@Composable
fun AppIconOrb(
    appName: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    size: Int = 40
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        accentColor.copy(alpha = 0.25f),
                        Color(0xFF101018)
                    )
                )
            )
            .border(1.dp, accentColor.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = appName.take(1).uppercase(),
            style = VaultLabel.copy(
                fontSize = (size / 2.2).sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
    }
}

@Preview
@Composable
fun VaultChipPreview() {
    NotifyVaultTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VaultChip(text = "All", isSelected = true, onClick = {})
            VaultChip(text = "OTPs", isSelected = false, onClick = {})
        }
    }
}
