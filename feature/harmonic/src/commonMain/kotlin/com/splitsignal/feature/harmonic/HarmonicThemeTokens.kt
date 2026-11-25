package com.splitsignal.feature.harmonic

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Self-contained color and formatting tokens for the Harmonic Phase Lock feature module.
 */
internal object HarmonicPalette {
    val ChassisBg: Color = Color(0xFF0B0E11)
    val PanelBg: Color = Color(0xFF141920)
    val Bezel: Color = Color(0xFF232D38)
    val PhosphorAmber: Color = Color(0xFFFFB000)
    val PhosphorGreen: Color = Color(0xFF00FF88)
    val TelemetryCyan: Color = Color(0xFF00E5FF)
    val TextPrimary: Color = Color(0xFFE6EDF3)
    val TextMuted: Color = Color(0xFF8B949E)

    val CrtScreenBg: Color = Color(0xFF07110C)
    val PanelInset: Color = Color(0xFF0D1217)
    val MetalHighlight: Color = Color(0xFF364454)
    val MetalShadow: Color = Color(0xFF080A0D)

    val MinorGrid: Color = Color(0xFF123020)
    val MajorAxis: Color = Color(0xFF1F5338)
    val KnobOuterMid: Color = Color(0xFF1A222B)
    val KnobFaceLight: Color = Color(0xFF232C37)
    val KnobFaceDark: Color = Color(0xFF13181F)
    val KnobHub: Color = Color(0xFF2B3644)
}

/**
 * Multiplatform-safe fixed-decimal formatter for oscilloscope and dial readouts.
 */
internal fun formatHarmonicDecimal(value: Float, decimals: Int = 2): String {
    if (value.isNaN() || value.isInfinite()) return "0.00"
    val clampedDecimals = decimals.coerceIn(0, 4)
    val factor = 10.0.pow(clampedDecimals)
    val scaled = (abs(value.toDouble()) * factor).roundToLong()
    val sign = if (value < 0f && scaled > 0L) "-" else ""
    if (clampedDecimals == 0) {
        return "$sign$scaled"
    }
    val divisor = factor.toLong()
    val whole = scaled / divisor
    val frac = (scaled % divisor).toString().padStart(clampedDecimals, '0')
    return "$sign$whole.$frac"
}
