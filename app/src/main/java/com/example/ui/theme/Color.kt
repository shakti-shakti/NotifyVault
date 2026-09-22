package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// 01 — THE DESIGN SOUL: "OBSIDIAN GLASS" COLOR SYSTEM

// Base surfaces — NEVER pure #000000 (except AMOLED), NEVER flat grey:
val ColorVoid = Color(0xFF07070B)
val ColorAbyss = Color(0xFF0B0B12)
val ColorObsidian = Color(0xFF101018)
val ColorOnyx = Color(0xFF16161F)
val ColorGraphite = Color(0xFF1E1E2A)
val ColorSlate = Color(0xFF2A2A38)

// Text hierarchy
val TextPrimaryDark = Color(0xFFF4F4F7)
val TextSecondaryDark = Color(0xFFA8A8B8)
val TextTertiaryDark = Color(0xFF6E6E80)
val TextDisabledDark = Color(0xFF44444F)
val TextInverseDark = Color(0xFF0B0B12)

// Light theme surfaces (Ivory Luxe)
val IvoryVoid = Color(0xFFEDE9E3)
val IvoryAbyss = Color(0xFFF7F5F1)
val IvoryObsidian = Color(0xFFFFFFFF)
val IvoryOnyx = Color(0xFFF0ECE4)
val IvoryGraphite = Color(0xFFE4DFD6)
val IvorySlate = Color(0xFFCEC7BC)
val TextPrimaryLight = Color(0xFF1A1A24)
val TextSecondaryLight = Color(0xFF5A5A6E)
val TextTertiaryLight = Color(0xFF8E8E9E)

// Signature Accents
val AurumBase = Color(0xFFE8C77E)
val AurumGlow = Color(0x33E8C77E)
val AurumGradStart = Color(0xFFF3DFA8)
val AurumGradEnd = Color(0xFFC79A4A)

val AmethystBase = Color(0xFFA78BFA)
val AmethystGlow = Color(0x33A78BFA)
val AmethystGradStart = Color(0xFFC4B5FD)
val AmethystGradEnd = Color(0xFF7C5CF5)

val CyanPulseBase = Color(0xFF5EEAD4)
val CyanPulseGlow = Color(0x335EEAD4)
val CyanPulseGradStart = Color(0xFFA7F3E4)
val CyanPulseGradEnd = Color(0xFF14B8A6)

val CrimsonBase = Color(0xFFFB7185)
val CrimsonGlow = Color(0x33FB7185)
val CrimsonGradStart = Color(0xFFFDA4AF)
val CrimsonGradEnd = Color(0xFFE11D48)

val EmeraldBase = Color(0xFF6EE7B7)
val EmeraldGlow = Color(0x336EE7B7)
val EmeraldGradStart = Color(0xFFA7F3D0)
val EmeraldGradEnd = Color(0xFF059669)

val AuroraBase = Color(0xFF818CF8)
val AuroraGlow = Color(0x33818CF8)
val AuroraGradStart = Color(0xFFA5B4FC)
val AuroraGradEnd = Color(0xFF4F46E5)

// Additional accents
val NeonCyanBase = Color(0xFF22D3EE)
val NeonCyanGlow = Color(0x3322D3EE)
val ChampagneBase = Color(0xFFF3DFA8)
val ChampagneGlow = Color(0x33F3DFA8)
val BronzeBase = Color(0xFFB08D4F)
val BronzeGlow = Color(0x33B08D4F)

@Immutable
data class AccentPalette(
    val base: Color,
    val glow: Color,
    val gradientStart: Color,
    val gradientEnd: Color
) {
    fun brush(): Brush = Brush.linearGradient(listOf(gradientStart, gradientEnd))
}

val AurumPalette = AccentPalette(AurumBase, AurumGlow, AurumGradStart, AurumGradEnd)
val AmethystPalette = AccentPalette(AmethystBase, AmethystGlow, AmethystGradStart, AmethystGradEnd)
val CyanPulsePalette = AccentPalette(CyanPulseBase, CyanPulseGlow, CyanPulseGradStart, CyanPulseGradEnd)
val CrimsonPalette = AccentPalette(CrimsonBase, CrimsonGlow, CrimsonGradStart, CrimsonGradEnd)
val EmeraldPalette = AccentPalette(EmeraldBase, EmeraldGlow, EmeraldGradStart, EmeraldGradEnd)
val AuroraPalette = AccentPalette(AuroraBase, AuroraGlow, AuroraGradStart, AuroraGradEnd)
val NeonCyanPalette = AccentPalette(NeonCyanBase, NeonCyanGlow, Color(0xFF67E8F9), Color(0xFF0891B2))
val ChampagnePalette = AccentPalette(ChampagneBase, ChampagneGlow, Color(0xFFFFF3D0), Color(0xFFD4AF37))
val BronzePalette = AccentPalette(BronzeBase, BronzeGlow, Color(0xFFD9B26A), Color(0xFF8C682A))

// Semantic Category Color Mapping
fun getCategoryPalette(category: String): AccentPalette {
    return when (category.uppercase()) {
        "OTP", "SECURITY", "AUTH" -> CyanPulsePalette
        "PAYMENT", "MONEY", "FINANCE", "BANKING", "UPI" -> EmeraldPalette
        "URGENT", "ALERT", "CRITICAL" -> CrimsonPalette
        "SOCIAL", "CHAT", "MESSAGES" -> AmethystPalette
        "STARRED", "FAVORITE" -> AurumPalette
        else -> AuroraPalette
    }
}

// 8 Premium Theme Definitions
enum class ThemePreset(
    val title: String,
    val subtitle: String,
    val bg: Color,
    val surface: Color,
    val elevated: Color,
    val accent: AccentPalette,
    val isLight: Boolean = false
) {
    MIDNIGHT_ONYX(
        title = "Midnight Onyx",
        subtitle = "Signature dark luxury with Aurum gold",
        bg = Color(0xFF0B0B12),
        surface = Color(0xFF101018),
        elevated = Color(0xFF16161F),
        accent = AurumPalette
    ),
    ROYAL_AMETHYST(
        title = "Royal Amethyst",
        subtitle = "Deep violet smoke & royal gems",
        bg = Color(0xFF0D0A18),
        surface = Color(0xFF130F22),
        elevated = Color(0xFF1A142D),
        accent = AmethystPalette
    ),
    CHAMPAGNE_GOLD(
        title = "Champagne Gold",
        subtitle = "Warm vintage high-society aesthetic",
        bg = Color(0xFF0F0D08),
        surface = Color(0xFF16130C),
        elevated = Color(0xFF201B11),
        accent = ChampagnePalette
    ),
    DEEP_OCEAN(
        title = "Deep Ocean",
        subtitle = "Abyssal depth with electric teal",
        bg = Color(0xFF06121A),
        surface = Color(0xFF0A1822),
        elevated = Color(0xFF0F222F),
        accent = CyanPulsePalette
    ),
    CARBON_CRIMSON(
        title = "Carbon Crimson",
        subtitle = "Smoked carbon with ruby flare",
        bg = Color(0xFF12070A),
        surface = Color(0xFF180B0F),
        elevated = Color(0xFF221116),
        accent = CrimsonPalette
    ),
    IVORY_LUXE(
        title = "Ivory Luxe",
        subtitle = "Gallery-grade pearl white & bronze",
        bg = IvoryAbyss,
        surface = IvoryObsidian,
        elevated = IvoryOnyx,
        accent = BronzePalette,
        isLight = true
    ),
    NEON_OBSIDIAN(
        title = "Neon Obsidian",
        subtitle = "High-contrast cyberpunk cyan edge",
        bg = Color(0xFF08080C),
        surface = Color(0xFF0E0E14),
        elevated = Color(0xFF161620),
        accent = NeonCyanPalette
    ),
    AURORA_GLASS(
        title = "Aurora Glass",
        subtitle = "Multi-spectrum indigo ethereal glow",
        bg = Color(0xFF0A0A14),
        surface = Color(0xFF0F0F1E),
        elevated = Color(0xFF17172B),
        accent = AuroraPalette
    )
}
