package com.splitsignal.app.ui.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design tokens adapted from the CashBuddy / PaisaPal Compose design system,
 * remapped to SplitSignal's Linear-minimalist palette.
 */
object SsColors {
    val BgCanvas: Color = Color(0xFF08090A)
    val SurfaceCard: Color = Color(0xFF101114)
    val SurfaceTile: Color = Color(0xFF17191E)
    val SurfaceTileActive: Color = Color(0xFF1F2229)

    val BorderSubtle: Color = Color(0x14FFFFFF)
    val BorderFocus: Color = Color(0x2EFFFFFF)
    val BorderPress: Color = Color(0x38FFFFFF)

    val TextPrimary: Color = Color(0xFFF7F8F8)
    val TextSecondary: Color = Color(0xFF8A8F98)
    val TextMuted: Color = Color(0xFF555A64)

    val AccentIndigo: Color = Color(0xFF5E6AD2)
    val AccentEmerald: Color = Color(0xFF26B57B)
    val AccentCrimson: Color = Color(0xFFE5484D)
    val AccentAmber: Color = Color(0xFFFF9F1C)
}

/**
 * 4/8 dp grid spacing tokens imported from CashBuddy.
 */
data class SsSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
)

val LocalSsSpacing = staticCompositionLocalOf { SsSpacing() }

val MaterialSsSpacing: SsSpacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSsSpacing.current

object SsRadii {
    val Small = RoundedCornerShape(4.dp)
    val Medium = RoundedCornerShape(8.dp)
    val Card = RoundedCornerShape(14.dp)
    val Large = RoundedCornerShape(16.dp)
    val ExtraLarge = RoundedCornerShape(24.dp)
    val Pill = RoundedCornerShape(999.dp)
}

val SsShapes = Shapes(
    extraSmall = SsRadii.Small,
    small = SsRadii.Medium,
    medium = SsRadii.Medium,
    large = SsRadii.Large,
    extraLarge = SsRadii.ExtraLarge,
)

object SsEasing {
    val EaseOutQuart = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
    val EaseInOutQuart = CubicBezierEasing(0.76f, 0f, 0.24f, 1f)
}
