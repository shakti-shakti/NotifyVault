package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// 1.4 RADIUS SCALE
// XS 8 · S 12 · M 18 · L 24 · XL 32 · XXL 40 · Pill 999

object VaultRadius {
    val XS = 8.dp
    val S = 12.dp
    val M = 18.dp
    val L = 24.dp
    val XL = 32.dp
    val XXL = 40.dp
    val Pill = 999.dp
}

val ShapeXS = RoundedCornerShape(VaultRadius.XS)
val ShapeS = RoundedCornerShape(VaultRadius.S)
val ShapeM = RoundedCornerShape(VaultRadius.M)
val ShapeL = RoundedCornerShape(VaultRadius.L)
val ShapeXL = RoundedCornerShape(VaultRadius.XL)
val ShapeXXL = RoundedCornerShape(VaultRadius.XXL)
val ShapePill = RoundedCornerShape(percent = 50)
