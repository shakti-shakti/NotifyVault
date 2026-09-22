package com.example.ui.components

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ColorAbyss
import com.example.ui.theme.ColorObsidian
import com.example.ui.theme.LocalVaultColors
import com.example.ui.theme.LocalVaultCustomization
import com.example.ui.theme.NotifyVaultTheme
import com.example.ui.theme.ShapeL

// 1.5 GLASS CARD SPEC:
// • Fill: vertical gradient #FFFFFF0A -> #FFFFFF03
// • Blur: RenderEffect blur(28f) on API 31+, fallback 2-layer translucency
// • Border: 1dp gradient hairline, angle 135°, accent 40% -> transparent
// • Top inner highlight: 1dp horizontal line #FFFFFF14
// • Corner radius: L (24dp) default

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = ShapeL,
    accentBorder: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LocalVaultColors.current
    val customization = LocalVaultCustomization.current
    val isLight = colors.isLight

    val baseAlpha = if (isLight) 0.88f else (0.45f * customization.glassIntensity)
    val strokeColor = if (accentBorder) {
        colors.accent.base.copy(alpha = 0.45f)
    } else {
        if (isLight) Color(0x18000000) else Color(0x28FFFFFF)
    }

    val bgGradient = if (isLight) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0xFFF7F5F1).copy(alpha = 0.85f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                colors.surface.copy(alpha = 0.82f),
                colors.elevated.copy(alpha = 0.55f)
            )
        )
    }

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgGradient)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        strokeColor,
                        strokeColor.copy(alpha = 0.05f)
                    ),
                    start = Offset.Zero,
                    end = Offset(400f, 600f)
                ),
                shape = shape
            )
            .drawBehind {
                // Top inner highlight line
                val highlightColor = if (isLight) Color(0x40FFFFFF) else Color(0x1FFFFFFF)
                drawLine(
                    color = highlightColor,
                    start = Offset(16f, 1f),
                    end = Offset(size.width - 16f, 1f),
                    strokeWidth = 1.5f
                )
            }
            .then(clickModifier),
        content = content
    )
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = ShapeL,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LocalVaultColors.current
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.surface.copy(alpha = 0.75f),
                        colors.elevated.copy(alpha = 0.60f)
                    )
                )
            )
            .border(
                1.dp,
                colors.cardStroke,
                shape
            ),
        content = content
    )
}

@Composable
fun GlowBox(
    modifier: Modifier = Modifier,
    glowColor: Color = LocalVaultColors.current.accent.base,
    glowRadius: Dp = 48.dp,
    alpha: Float = 0.18f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.drawBehind {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = alpha),
                        glowColor.copy(alpha = 0f)
                    ),
                    center = center,
                    radius = size.maxDimension * 0.9f
                )
            )
        },
        content = content
    )
}

@Composable
fun AmbientMeshBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = LocalVaultColors.current
    val customization = LocalVaultCustomization.current

    if (!customization.isAmbientMeshEnabled) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            content()
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "mesh")
    val animOffset1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 32000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob1"
    )
    val animOffset2 by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob2"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Ambient background drifting meshes
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val accent = colors.accent.base
            val secondary = colors.accent.gradientEnd

            // Blob 1: Top right
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.14f),
                        accent.copy(alpha = 0f)
                    ),
                    center = Offset(w * (0.8f - 0.2f * animOffset1), h * (0.15f + 0.15f * animOffset1)),
                    radius = w * 0.75f
                )
            )

            // Blob 2: Bottom left
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondary.copy(alpha = 0.10f),
                        secondary.copy(alpha = 0f)
                    ),
                    center = Offset(w * (0.2f + 0.25f * animOffset2), h * (0.8f - 0.2f * animOffset2)),
                    radius = w * 0.85f
                )
            )
        }

        // Grain Overlay if enabled
        if (customization.isGrainEnabled) {
            GrainOverlay(modifier = Modifier.fillMaxSize())
        }

        content()
    }
}

@Composable
fun GrainOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val step = 14f
        val w = size.width
        val h = size.height
        var x = 0f
        var y = 0f
        val noiseColor = Color(0x06FFFFFF)
        while (y < h) {
            x = (y % (step * 2)) / 2f
            while (x < w) {
                drawCircle(
                    color = noiseColor,
                    radius = 0.8f,
                    center = Offset(x, y)
                )
                x += step
            }
            y += step
        }
    }
}

@Preview
@Composable
fun GlassCardPreview() {
    NotifyVaultTheme {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            accentBorder = true
        ) {
            Box(modifier = Modifier.fillMaxWidth())
        }
    }
}
