package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CrimsonPalette
import com.example.ui.theme.LocalVaultColors
import kotlin.math.hypot

@Composable
fun PatternLockView(
    onPatternComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    errorState: Boolean = false
) {
    val colors = LocalVaultColors.current
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }

    val selectedDots = remember { mutableStateListOf<Int>() }
    var currentTouchPos by remember { mutableStateOf<Offset?>(null) }

    fun triggerDotHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(20L)
            }
        } catch (e: Exception) {
            // Ignored
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(16.dp)
            .pointerInput(isEnabled, errorState) {
                if (!isEnabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        selectedDots.clear()
                        currentTouchPos = offset
                        val hitIndex = findDotIndex(offset, size.width.toFloat(), size.height.toFloat())
                        if (hitIndex != -1) {
                            selectedDots.add(hitIndex)
                            triggerDotHaptic()
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentTouchPos = change.position
                        val hitIndex = findDotIndex(change.position, size.width.toFloat(), size.height.toFloat())
                        if (hitIndex != -1 && !selectedDots.contains(hitIndex)) {
                            selectedDots.add(hitIndex)
                            triggerDotHaptic()
                        }
                    },
                    onDragEnd = {
                        currentTouchPos = null
                        if (selectedDots.isNotEmpty()) {
                            val patternString = selectedDots.joinToString("-")
                            onPatternComplete(patternString)
                        }
                    },
                    onDragCancel = {
                        currentTouchPos = null
                        selectedDots.clear()
                    }
                )
            }
    ) {
        val accentColor = if (errorState) CrimsonPalette.base else colors.accent.base
        val glowColor = if (errorState) CrimsonPalette.glow else colors.accent.glow
        val inactiveDotColor = colors.surface.copy(alpha = 0.75f)
        val inactiveStrokeColor = colors.cardStroke

        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            val width = size.width
            val height = size.height
            val cellW = width / 3f
            val cellH = height / 3f
            val baseDotRadius = 10.dp.toPx()
            val hitDotRadius = 14.dp.toPx()
            val hitOuterRadius = 30.dp.toPx()

            fun getDotCenter(index: Int): Offset {
                val row = index / 3
                val col = index % 3
                return Offset(
                    x = col * cellW + cellW / 2f,
                    y = row * cellH + cellH / 2f
                )
            }

            // Draw line trails between selected dots
            if (selectedDots.size > 1) {
                val path = Path().apply {
                    val first = getDotCenter(selectedDots[0])
                    moveTo(first.x, first.y)
                    for (i in 1 until selectedDots.size) {
                        val pt = getDotCenter(selectedDots[i])
                        lineTo(pt.x, pt.y)
                    }
                }

                // Soft outer glow stroke
                drawPath(
                    path = path,
                    color = glowColor,
                    style = Stroke(
                        width = 16.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Crisp inner accent stroke
                drawPath(
                    path = path,
                    color = accentColor,
                    style = Stroke(
                        width = 4.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Trail to current active touch position
            val lastDotIndex = selectedDots.lastOrNull()
            val currentTouch = currentTouchPos
            if (lastDotIndex != null && currentTouch != null) {
                val lastCenter = getDotCenter(lastDotIndex)
                // Glow
                drawLine(
                    color = glowColor,
                    start = lastCenter,
                    end = currentTouch,
                    strokeWidth = 14.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Inner line
                drawLine(
                    color = accentColor,
                    start = lastCenter,
                    end = currentTouch,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Draw all 9 dots
            for (i in 0..8) {
                val center = getDotCenter(i)
                val isSelected = selectedDots.contains(i)

                if (isSelected) {
                    // Outer diffuse aura
                    drawCircle(
                        color = glowColor,
                        radius = hitOuterRadius,
                        center = center
                    )
                    // Ring
                    drawCircle(
                        color = accentColor,
                        radius = hitDotRadius,
                        center = center,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                    // Inner bright core
                    drawCircle(
                        color = accentColor,
                        radius = 6.dp.toPx(),
                        center = center
                    )
                } else {
                    // Inactive dot ring
                    drawCircle(
                        color = inactiveStrokeColor,
                        radius = baseDotRadius,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    // Inactive dot fill
                    drawCircle(
                        color = inactiveDotColor,
                        radius = 5.dp.toPx(),
                        center = center
                    )
                }
            }
        }
    }
}

private fun findDotIndex(touch: Offset, width: Float, height: Float): Int {
    val cellW = width / 3f
    val cellH = height / 3f
    val threshold = cellW * 0.42f

    for (row in 0..2) {
        for (col in 0..2) {
            val cx = col * cellW + cellW / 2f
            val cy = row * cellH + cellH / 2f
            if (hypot(touch.x - cx, touch.y - cy) <= threshold) {
                return row * 3 + col
            }
        }
    }
    return -1
}
