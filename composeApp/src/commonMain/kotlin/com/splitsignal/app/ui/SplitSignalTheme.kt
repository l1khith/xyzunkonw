package com.splitsignal.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.splitsignal.app.ui.design.LocalSsSpacing
import com.splitsignal.app.ui.design.SsColors
import com.splitsignal.app.ui.design.SsShapes
import com.splitsignal.app.ui.design.SsSpacing
import org.jetbrains.compose.resources.Font
import splitsignal.composeapp.generated.resources.Res
import splitsignal.composeapp.generated.resources.inter_bold
import splitsignal.composeapp.generated.resources.inter_medium
import splitsignal.composeapp.generated.resources.inter_regular
import splitsignal.composeapp.generated.resources.inter_semibold

/**
 * Loads the official Inter font family designed for high-density UI.
 */
@Composable
fun getInterFontFamily(): FontFamily = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
    Font(Res.font.inter_bold, FontWeight.Bold),
)

/**
 * Linear-inspired minimalist dark color system with layered obsidian surfaces,
 * translucent white hairlines, and disciplined functional accents.
 */
object SplitSignalColors {
    // Canvas & layered surfaces
    val BgCanvas: Color = SsColors.BgCanvas
    val SurfaceCard: Color = SsColors.SurfaceCard
    val SurfaceTile: Color = SsColors.SurfaceTile
    val SurfaceTileActive: Color = SsColors.SurfaceTileActive

    // Translucent hairline borders
    val BorderSubtle: Color = SsColors.BorderSubtle
    val BorderFocus: Color = SsColors.BorderFocus
    val BorderPress: Color = SsColors.BorderPress

    // Typography hierarchy
    val TextPrimary: Color = SsColors.TextPrimary
    val TextSecondary: Color = SsColors.TextSecondary
    val TextMuted: Color = SsColors.TextMuted

    // Functional accents (used sparingly)
    val AccentIndigo: Color = SsColors.AccentIndigo
    val AccentEmerald: Color = SsColors.AccentEmerald
    val AccentCrimson: Color = SsColors.AccentCrimson
    val AccentAmber: Color = SsColors.AccentAmber

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
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = "tnum",
        color = SplitSignalColors.TextPrimary,
    )

    val TabularLabel: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = "tnum",
        fontSize = 12.sp,
        color = SplitSignalColors.TextPrimary,
    )
}

private val SplitSignalDarkColorScheme = darkColorScheme(
    primary = SplitSignalColors.TextPrimary,
    onPrimary = SplitSignalColors.BgCanvas,
    primaryContainer = SplitSignalColors.SurfaceTile,
    onPrimaryContainer = SplitSignalColors.TextPrimary,
    secondary = SplitSignalColors.TextSecondary,
    onSecondary = SplitSignalColors.BgCanvas,
    secondaryContainer = SplitSignalColors.SurfaceCard,
    onSecondaryContainer = SplitSignalColors.TextPrimary,
    tertiary = SplitSignalColors.TextSecondary,
    onTertiary = SplitSignalColors.BgCanvas,
    background = SplitSignalColors.BgCanvas,
    onBackground = SplitSignalColors.TextPrimary,
    surface = SplitSignalColors.SurfaceCard,
    onSurface = SplitSignalColors.TextPrimary,
    surfaceVariant = SplitSignalColors.SurfaceTile,
    onSurfaceVariant = SplitSignalColors.TextSecondary,
    outline = SplitSignalColors.BorderSubtle,
    error = SplitSignalColors.TextSecondary,
    onError = SplitSignalColors.BgCanvas,
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
 * Material 3 Linear-inspired minimalist dark theme for SplitSignal with official Inter typography.
 */
@Composable
fun SplitSignalTheme(content: @Composable () -> Unit) {
    val inter = getInterFontFamily()
    val typography = Typography(
        headlineSmall = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            letterSpacing = (-0.4).sp,
            color = SplitSignalColors.TextPrimary,
        ),
        titleLarge = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            letterSpacing = (-0.3).sp,
            color = SplitSignalColors.TextPrimary,
        ),
        titleMedium = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            letterSpacing = (-0.2).sp,
            color = SplitSignalColors.TextPrimary,
        ),
        titleSmall = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            letterSpacing = (-0.1).sp,
            color = SplitSignalColors.TextPrimary,
        ),
        bodyMedium = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = (-0.1).sp,
            color = SplitSignalColors.TextPrimary,
        ),
        bodySmall = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = SplitSignalColors.TextSecondary,
        ),
        labelLarge = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            letterSpacing = (-0.1).sp,
            color = SplitSignalColors.TextPrimary,
        ),
        labelMedium = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            letterSpacing = 0.sp,
            color = SplitSignalColors.TextPrimary,
        ),
        labelSmall = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
            color = SplitSignalColors.TextSecondary,
        ),
    )

    CompositionLocalProvider(
        LocalSsSpacing provides SsSpacing(),
    ) {
        MaterialTheme(
            colorScheme = SplitSignalDarkColorScheme,
            typography = typography,
            shapes = SsShapes,
            content = content,
        )
    }
}
