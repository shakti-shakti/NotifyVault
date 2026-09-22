package com.example.ui.components

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext

fun Modifier.instantTap(
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val context = LocalContext.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "instantPressScale"
    )

    this
        .scale(scale)
        .pointerInput(onClick, onLongClick) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    // Haptic tick immediately on press-down (<16ms)
                    try {
                        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
                        if (vibrator != null && vibrator.hasVibrator()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(10)
                            }
                        }
                    } catch (e: Exception) {
                        // Ignored
                    }

                    tryAwaitRelease()
                    isPressed = false
                },
                onLongPress = {
                    onLongClick?.invoke()
                },
                onTap = {
                    onClick()
                }
            )
        }
}
