package com.example.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 1.4 SPACING, RADIUS, ELEVATION
// Spacing scale (dp): 2, 4, 8, 12, 16, 20, 24, 32, 40, 56, 72
// Screen gutter: 20dp | Card inner padding: 18dp
// List item gap: 12dp | Section gap: 28dp

object VaultSpacing {
    val Space2 = 2.dp
    val Space4 = 4.dp
    val Space8 = 8.dp
    val Space12 = 12.dp
    val Space16 = 16.dp
    val Space20 = 20.dp
    val Space24 = 24.dp
    val Space28 = 28.dp
    val Space32 = 32.dp
    val Space40 = 40.dp
    val Space56 = 56.dp
    val Space72 = 72.dp

    val ScreenGutter = 20.dp
    val CardInnerPadding = 18.dp
    val ListItemGap = 12.dp
    val SectionGap = 28.dp
}

// Elevation tokens (dp representation for depth offsets)
object VaultElevation {
    val Level0: Dp = 0.dp
    val Level1: Dp = 2.dp
    val Level2: Dp = 6.dp
    val Level3: Dp = 14.dp
    val Level4: Dp = 24.dp
}
