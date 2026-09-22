package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// 1.3 TYPOGRAPHY
// Scale (sp / line-height / letter-spacing / weight):
// Display L 40 / 44 / -0.02em / 600
// Display M 32 / 38 / -0.02em / 600
// Headline 24 / 30 / -0.01em / 600
// Title 18 / 24 / -0.01em / 600
// Body L 16 / 24 / 0 / 400
// Body M 14 / 20 / 0 / 400
// Label 12 / 16 / 0.08em / 600 (ALL CAPS for section labels)
// Caption 11 / 14 / 0.04em / 500
// Mono 13 / 18 / 0 / 400

val DisplayL = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 40.sp,
    lineHeight = 44.sp,
    letterSpacing = (-0.02).em
)

val DisplayM = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 32.sp,
    lineHeight = 38.sp,
    letterSpacing = (-0.02).em
)

val VaultHeadline = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp,
    lineHeight = 30.sp,
    letterSpacing = (-0.01).em
)

val VaultTitle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp,
    lineHeight = 24.sp,
    letterSpacing = (-0.01).em
)

val VaultBodyL = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.em
)

val VaultBodyM = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.em
)

val VaultLabel = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.08.em
)

val VaultCaption = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.04.em
)

val VaultMono = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.em
)

val VaultTypography = Typography(
    displayLarge = DisplayL,
    displayMedium = DisplayM,
    headlineMedium = VaultHeadline,
    titleMedium = VaultTitle,
    bodyLarge = VaultBodyL,
    bodyMedium = VaultBodyM,
    labelSmall = VaultLabel,
    bodySmall = VaultCaption
)
