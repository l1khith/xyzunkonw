package com.splitsignal.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Linear-inspired minimalist dark color system with layered obsidian surfaces,
 * translucent white hairlines, and disciplined functional accents.
 */
object SplitSignalColors {
    // Canvas & layered surfaces
    val BgCanvas: Color = Color(0xFF08090A)
    val SurfaceCard: Color = Color(0xFF101114)
    val SurfaceTile: Color = Color(0xFF17191E)
    val SurfaceTileActive: Color = Color(0xFF1F2229)

    // Translucent hairline borders
    val BorderSubtle: Color = Color(0x14FFFFFF) // 8% white
    val BorderFocus: Color = Color(0x2EFFFFFF)  // 18% white
    val BorderPress: Color = Color(0x38FFFFFF)  // 22% white

    // Typography hierarchy
    val TextPrimary: Color = Color(0xFFF7F8F8)
    val TextSecondary: Color = Color(0xFF8A8F98)
    val TextMuted: Color = Color(0xFF555A64)

    // Functional accents (used sparingly)
    val AccentIndigo: Color = Color(0xFF5E6AD2)
    val AccentEmerald: Color = Color(0xFF26B57B)
    val AccentCrimson: Color = Color(0xFFE5484D)
    val AccentAmber: Color = Color(0xFFF5A623)

    // Subtle top-rim highlight brush for primary cards
    val CardTopRimBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0x0FFFFFFF),
            Color(0x00FFFFFF),
        ),
    )

    // Semantic aliases preserved for compatibility across all screens
    val BgObsidian: Color = BgCanvas
    val CardSlate: Color = SurfaceCard
    val TileSurface: Color = SurfaceTile
    val BorderHairline: Color = BorderSubtle
    val AccentSand: Color = Color(0xFFE5E7EB)
    val ErrorRose: Color = AccentCrimson
}

/**
 * Reusable tabular-numeral (`tnum`) text styles so timers, grid digits,
 * coins, and Elo ratings never jitter horizontally.
 */
object LinearTextStyles {
    val TabularDigit: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = "tnum",
        color = SplitSignalColors.TextPrimary,
    )

    val TabularLabel: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = "tnum",
        fontSize = 12.sp,
        color = SplitSignalColors.TextPrimary,
    )
}

private val SplitSignalDarkColorScheme = darkColorScheme(
    primary = SplitSignalColors.AccentIndigo,
    onPrimary = SplitSignalColors.TextPrimary,
    primaryContainer = SplitSignalColors.SurfaceTile,
    onPrimaryContainer = SplitSignalColors.TextPrimary,
    secondary = SplitSignalColors.AccentEmerald,
    onSecondary = SplitSignalColors.BgCanvas,
    secondaryContainer = SplitSignalColors.SurfaceCard,
    onSecondaryContainer = SplitSignalColors.TextPrimary,
    tertiary = SplitSignalColors.AccentAmber,
    onTertiary = SplitSignalColors.BgCanvas,
    background = SplitSignalColors.BgCanvas,
    onBackground = SplitSignalColors.TextPrimary,
    surface = SplitSignalColors.SurfaceCard,
    onSurface = SplitSignalColors.TextPrimary,
    surfaceVariant = SplitSignalColors.SurfaceTile,
    onSurfaceVariant = SplitSignalColors.TextSecondary,
    outline = SplitSignalColors.BorderSubtle,
    error = SplitSignalColors.AccentCrimson,
    onError = SplitSignalColors.TextPrimary,
)

private val SplitSignalTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = (-0.3).sp,
        color = SplitSignalColors.TextPrimary,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        letterSpacing = (-0.1).sp,
        color = SplitSignalColors.TextPrimary,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 0.sp,
        color = SplitSignalColors.TextPrimary,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = SplitSignalColors.TextPrimary,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        color = SplitSignalColors.TextSecondary,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.2.sp,
        color = SplitSignalColors.TextPrimary,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.7.sp,
        color = SplitSignalColors.TextSecondary,
    ),
)

/**
 * Material 3 Linear-inspired minimalist dark theme for SplitSignal.
 */
@Composable
fun SplitSignalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SplitSignalDarkColorScheme,
        typography = SplitSignalTypography,
        content = content,
    )
}
