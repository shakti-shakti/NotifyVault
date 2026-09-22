package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Immutable
data class VaultColors(
    val background: Color,
    val surface: Color,
    val elevated: Color,
    val cardStroke: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    val accent: AccentPalette,
    val isLight: Boolean
)

val LocalVaultColors = compositionLocalOf {
    VaultColors(
        background = ColorAbyss,
        surface = ColorObsidian,
        elevated = ColorOnyx,
        cardStroke = Color(0x22FFFFFF),
        textPrimary = TextPrimaryDark,
        textSecondary = TextSecondaryDark,
        textTertiary = TextTertiaryDark,
        textDisabled = TextDisabledDark,
        accent = AurumPalette,
        isLight = false
    )
}

val LocalVaultCustomization = compositionLocalOf {
    VaultCustomizationState()
}

class VaultCustomizationState(
    initialTheme: ThemePreset = ThemePreset.MIDNIGHT_ONYX,
    initialAmoled: Boolean = false,
    initialGrain: Boolean = true,
    initialAmbientMesh: Boolean = true,
    initialGlassIntensity: Float = 0.85f,
    initialHapticIntensity: String = "Standard",
    initialListStyle: String = "Cards"
) {
    var currentTheme by mutableStateOf(initialTheme)
    var isAmoled by mutableStateOf(initialAmoled)
    var isGrainEnabled by mutableStateOf(initialGrain)
    var isAmbientMeshEnabled by mutableStateOf(initialAmbientMesh)
    var glassIntensity by mutableFloatStateOf(initialGlassIntensity)
    var hapticIntensity by mutableStateOf(initialHapticIntensity)
    var listStyle by mutableStateOf(initialListStyle)
}

@Composable
fun NotifyVaultTheme(
    customizationState: VaultCustomizationState = remember { VaultCustomizationState() },
    content: @Composable () -> Unit
) {
    val preset = customizationState.currentTheme
    val isLight = preset.isLight

    val bg = if (customizationState.isAmoled && !isLight) {
        Color.Black
    } else {
        preset.bg
    }

    val surface = if (customizationState.isAmoled && !isLight) {
        Color(0xFF050508)
    } else {
        preset.surface
    }

    val elevated = if (customizationState.isAmoled && !isLight) {
        Color(0xFF0B0B10)
    } else {
        preset.elevated
    }

    val textPrimary = if (isLight) TextPrimaryLight else TextPrimaryDark
    val textSecondary = if (isLight) TextSecondaryLight else TextSecondaryDark
    val textTertiary = if (isLight) TextTertiaryLight else TextTertiaryDark
    val textDisabled = if (isLight) Color(0xFFACACB8) else TextDisabledDark

    val vaultColors = VaultColors(
        background = bg,
        surface = surface,
        elevated = elevated,
        cardStroke = if (isLight) Color(0x18000000) else Color(0x22FFFFFF),
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        textTertiary = textTertiary,
        textDisabled = textDisabled,
        accent = preset.accent,
        isLight = isLight
    )

    val materialColors = if (isLight) {
        lightColorScheme(
            primary = preset.accent.base,
            background = bg,
            surface = surface,
            onPrimary = Color.White,
            onBackground = textPrimary,
            onSurface = textPrimary
        )
    } else {
        darkColorScheme(
            primary = preset.accent.base,
            background = bg,
            surface = surface,
            onPrimary = Color.Black,
            onBackground = textPrimary,
            onSurface = textPrimary
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = bg.toArgb()
                window.navigationBarColor = bg.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLight
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = isLight
            }
        }
    }

    CompositionLocalProvider(
        LocalVaultColors provides vaultColors,
        LocalVaultCustomization provides customizationState
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = VaultTypography,
            content = content
        )
    }
}

@Composable
fun rememberVaultCustomizationState(
    initialTheme: ThemePreset = ThemePreset.MIDNIGHT_ONYX,
    initialAmoled: Boolean = false
): VaultCustomizationState = remember {
    VaultCustomizationState(initialTheme = initialTheme, initialAmoled = initialAmoled)
}
