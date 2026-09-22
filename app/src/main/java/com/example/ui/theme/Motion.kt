package com.example.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

// 1.6 MOTION LANGUAGE
// Spring.Snappy : damping 0.75, stiffness 400 (taps, toggles)
// Spring.Smooth : damping 0.85, stiffness 250 (card expand)
// Spring.Gentle : damping 0.90, stiffness 180 (page transitions)
// Ease.Enter : cubic-bezier(0.16, 1, 0.3, 1) (elements arriving)
// Ease.Exit : cubic-bezier(0.7, 0, 0.84, 0) (elements leaving)
// Ease.Emphasis : cubic-bezier(0.34, 1.56, 0.64, 1) (celebration)
// Durations: 120ms micro · 220ms standard · 380ms emphasis · 600ms cinematic

object VaultMotion {
    const val DurationMicro = 120
    const val DurationStandard = 220
    const val DurationEmphasis = 380
    const val DurationCinematic = 600

    val EaseEnter = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)
    val EaseExit = CubicBezierEasing(0.7f, 0.0f, 0.84f, 0.0f)
    val EaseEmphasis = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)

    fun <T> springSnappy() = spring<T>(
        dampingRatio = 0.75f,
        stiffness = 400f
    )

    fun <T> springSmooth() = spring<T>(
        dampingRatio = 0.85f,
        stiffness = 250f
    )

    fun <T> springGentle() = spring<T>(
        dampingRatio = 0.90f,
        stiffness = 180f
    )
}
