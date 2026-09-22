package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrimsonPalette
import com.example.ui.theme.DisplayL
import com.example.ui.theme.LocalVaultColors
import com.example.ui.theme.ShapeL
import com.example.ui.theme.ShapePill
import com.example.ui.theme.ShapeS
import com.example.ui.theme.VaultCaption
import com.example.ui.theme.VaultHeadline
import com.example.ui.theme.VaultLabel
import com.example.ui.theme.VaultMotion
import com.example.ui.theme.VaultTitle
import kotlin.math.cos
import kotlin.math.sin

// 2.2 G. BOTTOM NAVIGATION
// Floating glass bar, 68dp, radius Pill, detached from edges (16dp margin)
// 4 destinations: Vault · Search · Insights · Studio
@Composable
fun BottomGlassNav(
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current

    val items = listOf(
        NavItem("vault", "Vault", Icons.Outlined.Dashboard, Icons.Filled.Archive),
        NavItem("search", "Search", Icons.Outlined.Search, Icons.Filled.Search),
        NavItem("insights", "Insights", Icons.Outlined.Insights, Icons.Filled.Insights),
        NavItem("studio", "Studio", Icons.Outlined.ColorLens, Icons.Filled.ColorLens)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = ShapePill
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = selectedRoute == item.route
                    val activeScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 1.0f,
                        animationSpec = VaultMotion.springSnappy(),
                        label = "navScale"
                    )

                    Box(
                        modifier = Modifier
                            .scale(activeScale)
                            .clip(ShapePill)
                            .background(if (isSelected) colors.accent.glow else Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onNavigate(item.route) }
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.activeIcon else item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) colors.accent.base else colors.textTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.label,
                                    style = VaultLabel.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.accent.base
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val activeIcon: ImageVector
)

// 2.2 F. PULSE FAB — "LIVE CAPTURE" TOGGLE
// Concentric pulsing rings (2s breathing loop), accent glow bloom
@Composable
fun PulseFab(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseRing by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringScale"
    )
    val ringAlpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringAlpha"
    )

    Box(
        modifier = modifier
            .size(64.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            // Concentric outer pulsing ring
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .scale(pulseRing)
                    .clip(CircleShape)
                    .border(1.5.dp, colors.accent.base.copy(alpha = ringAlpha), CircleShape)
            )
        }

        // Inner FAB button
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) colors.accent.brush()
                    else Brush.verticalGradient(listOf(colors.surface, colors.elevated))
                )
                .border(
                    width = 1.5.dp,
                    color = colors.accent.base,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.RadioButtonChecked,
                contentDescription = "Capture State",
                tint = if (isActive) {
                    if (colors.isLight) Color.White else Color(0xFF0B0B12)
                } else colors.accent.base,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// 2.6 ANIMATED AREA CHART (Canvas based)
@Composable
fun AnimatedAreaChart(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current
    var scrubbedIndex by remember { mutableStateOf<Int?>(null) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(dataPoints) {
        progress.animateTo(1f, animationSpec = tween(1200, easing = VaultMotion.EaseEnter))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val index = ((offset.x / size.width) * (dataPoints.size - 1)).toInt()
                        scrubbedIndex = index.coerceIn(0, dataPoints.size - 1)
                    },
                    onDrag = { change, _ ->
                        val index = ((change.position.x / size.width) * (dataPoints.size - 1)).toInt()
                        scrubbedIndex = index.coerceIn(0, dataPoints.size - 1)
                    },
                    onDragEnd = { scrubbedIndex = null },
                    onDragCancel = { scrubbedIndex = null }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (dataPoints.isEmpty()) return@Canvas
            val w = size.width
            val h = size.height
            val maxVal = (dataPoints.maxOrNull() ?: 1f).coerceAtLeast(1f)
            val stepX = w / (dataPoints.size - 1).coerceAtLeast(1)

            val linePath = Path()
            val fillPath = Path()

            dataPoints.forEachIndexed { i, value ->
                val x = i * stepX
                val normalizedY = (1f - (value / maxVal)) * h
                val y = h - ((h - normalizedY) * progress.value)

                if (i == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, h)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }

            fillPath.lineTo(w, h)
            fillPath.close()

            // Gradient Fill under curve
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.accent.base.copy(alpha = 0.28f),
                        colors.accent.base.copy(alpha = 0.02f)
                    ),
                    startY = 0f,
                    endY = h
                )
            )

            // Accent Line
            drawPath(
                path = linePath,
                color = colors.accent.base,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw Scrubber Line if touched
            scrubbedIndex?.let { idx ->
                val sx = idx * stepX
                val sy = (1f - (dataPoints[idx] / maxVal)) * h
                drawLine(
                    color = colors.textPrimary,
                    start = Offset(sx, 0f),
                    end = Offset(sx, h),
                    strokeWidth = 1.dp.toPx()
                )
                drawCircle(
                    color = colors.accent.base,
                    radius = 5.dp.toPx(),
                    center = Offset(sx, sy)
                )
            }
        }

        // Floating tooltip if scrubbed
        scrubbedIndex?.let { idx ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .clip(ShapePill)
                    .background(colors.elevated)
                    .border(1.dp, colors.accent.base, ShapePill)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${dataPoints[idx].toInt()} alerts",
                    style = VaultCaption.copy(fontWeight = FontWeight.Bold, color = colors.accent.base)
                )
            }
        }
    }
}

// 2.6 RADIAL HEATMAP (24 wedges for hourly frequency)
@Composable
fun RadialHeatmap(
    hourlyCounts: List<Int>,
    peakHour: Int = 14,
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current
    val maxCount = (hourlyCounts.maxOrNull() ?: 1).coerceAtLeast(1)

    Box(
        modifier = modifier
            .size(200.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            val radius = size.minDimension / 2f
            val innerRadius = radius * 0.45f
            val wedgeAngle = 360f / 24f

            for (i in 0 until 24) {
                val count = hourlyCounts.getOrElse(i) { 0 }
                val intensity = (count.toFloat() / maxCount).coerceIn(0.12f, 1f)
                val startAngle = i * wedgeAngle - 90f

                drawArc(
                    color = colors.accent.base.copy(alpha = intensity * 0.85f),
                    startAngle = startAngle,
                    sweepAngle = wedgeAngle - 2f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = radius - innerRadius, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${peakHour}:00",
                style = VaultHeadline.copy(fontWeight = FontWeight.Bold, color = colors.accent.base, fontSize = 20.sp)
            )
            Text(
                text = "PEAK HOUR",
                style = VaultCaption.copy(fontSize = 9.sp, color = colors.textTertiary)
            )
        }
    }
}

// 2.6 CATEGORY DONUT CHART
@Composable
fun DonutChart(
    slices: List<Pair<String, Float>>,
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current
    val total = slices.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(1f)

    Box(
        modifier = modifier
            .size(170.dp)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 22.dp.toPx()
            var currentAngle = -90f

            slices.forEach { slice ->
                val sweep = (slice.second / total) * 360f
                val color = when (slice.first.uppercase()) {
                    "OTP" -> Color(0xFF5EEAD4)
                    "PAYMENT" -> Color(0xFF6EE7B7)
                    "SOCIAL" -> Color(0xFFA78BFA)
                    "DELIVERY" -> Color(0xFFF3DFA8)
                    else -> Color(0xFF818CF8)
                }

                drawArc(
                    color = color,
                    startAngle = currentAngle,
                    sweepAngle = sweep - 2.5f,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                currentAngle += sweep
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${total.toInt()}",
                style = VaultHeadline.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            )
            Text(
                text = "CATALOGED",
                style = VaultCaption.copy(fontSize = 9.sp, color = colors.textTertiary)
            )
        }
    }
}

// 2.9 LOCK SCREEN KEYPAD
@Composable
fun LockKeypad(
    pinLength: Int,
    currentLength: Int,
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // PIN Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            for (i in 0 until pinLength) {
                val isFilled = i < currentLength
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (isFilled) colors.accent.base else colors.cardStroke)
                        .border(1.dp, colors.accent.base.copy(alpha = 0.5f), CircleShape)
                )
            }
        }

        // Circular numeric keys
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "DEL")
        )

        keys.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                modifier = Modifier.padding(vertical = 10.dp)
            ) {
                row.forEach { key ->
                    if (key.isEmpty()) {
                        Spacer(modifier = Modifier.size(68.dp))
                    } else if (key == "DEL") {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .clickable { onDeleteClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backspace,
                                contentDescription = "Delete",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        KeypadButton(digit = key, onClick = { onDigitClick(key) })
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    digit: String,
    onClick: () -> Unit
) {
    val colors = LocalVaultColors.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = VaultMotion.springSnappy(),
        label = "keyScale"
    )

    Box(
        modifier = Modifier
            .size(68.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(colors.surface.copy(alpha = 0.7f))
            .border(1.dp, colors.cardStroke, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit,
            style = DisplayL.copy(
                fontSize = 24.sp,
                color = colors.textPrimary,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

// Rolling Animated Number
@Composable
fun RollingNumber(
    targetNumber: Int,
    modifier: Modifier = Modifier
) {
    val colors = LocalVaultColors.current
    val animatedNumber by animateIntAsState(
        targetValue = targetNumber,
        animationSpec = tween(durationMillis = 1000, easing = VaultMotion.EaseEnter),
        label = "rollingNumber"
    )

    Text(
        text = "%,d".format(animatedNumber),
        style = DisplayL.copy(
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold
        ),
        modifier = modifier
    )
}
