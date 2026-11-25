package com.splitsignal.feature.harmonic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitsignal.core.domain.HarmonicUiState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val PI_F = PI.toFloat()
private const val TWO_PI_F = (2.0 * PI).toFloat()
private const val MAX_PHASE_RAD = 6.283185f

/**
 * Analyst CRT oscilloscope rendering live target reference and operator signal waveforms.
 */
@Composable
fun AnalystOscilloscope(
    state: HarmonicUiState,
    modifier: Modifier = Modifier,
) {
    var elapsedSeconds by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var startNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (startNanos == 0L) {
                    startNanos = frameNanos
                }
                elapsedSeconds = ((frameNanos - startNanos) / 1_000_000_000.0).toFloat()
            }
        }
    }

    val isLocked = state.isHarmonicLocked
    val lockPercent = (state.harmonicLockRatio.coerceIn(0f, 1f) * 100f).roundToInt()
    val freqDelta = abs(state.operatorFreq - state.effectiveTargetFreq)
    val phaseDelta = shortestAngularDelta(state.operatorPhase, state.effectiveTargetPhase)

    val statusColor = if (isLocked) HarmonicPalette.PhosphorGreen else HarmonicPalette.PhosphorAmber
    val statusText = if (isLocked) "LOCKED" else "TUNING"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HarmonicPalette.PanelBg)
            .border(1.5.dp, HarmonicPalette.Bezel, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "HARMONIC CRT OSCILLOSCOPE",
                    style = MaterialTheme.typography.titleSmall,
                    color = HarmonicPalette.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "DRIFT ${formatHarmonicDecimal(state.driftRate, 2)} Hz/s // CH-A REF vs CH-B OP",
                    style = MaterialTheme.typography.labelSmall,
                    color = HarmonicPalette.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(statusColor.copy(alpha = 0.16f))
                    .border(1.dp, statusColor.copy(alpha = 0.8f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusColor),
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        maxLines = 1,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(HarmonicPalette.CrtScreenBg)
                .border(
                    width = 2.dp,
                    color = if (isLocked) {
                        HarmonicPalette.PhosphorGreen.copy(alpha = 0.45f)
                    } else {
                        HarmonicPalette.Bezel
                    },
                    shape = RoundedCornerShape(10.dp),
                ),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                if (w <= 0f || h <= 0f) return@Canvas

                val centerY = h * 0.5f
                val centerX = w * 0.5f
                val waveAmplitude = h * 0.34f

                val cols = 10
                val rows = 6

                for (c in 1 until cols) {
                    val x = w * (c / cols.toFloat())
                    drawLine(
                        color = if (c == cols / 2) HarmonicPalette.MajorAxis else HarmonicPalette.MinorGrid,
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = if (c == cols / 2) 1.5f else 1f,
                    )
                }
                for (r in 1 until rows) {
                    val y = h * (r / rows.toFloat())
                    drawLine(
                        color = if (r == rows / 2) HarmonicPalette.MajorAxis else HarmonicPalette.MinorGrid,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = if (r == rows / 2) 1.5f else 1f,
                    )
                }

                val tickHalf = 4.dp.toPx()
                for (sub in 1 until 20) {
                    val tx = w * (sub / 20f)
                    drawLine(
                        color = HarmonicPalette.MajorAxis,
                        start = Offset(tx, centerY - tickHalf),
                        end = Offset(tx, centerY + tickHalf),
                        strokeWidth = 1f,
                    )
                }
                for (sub in 1 until 12) {
                    val ty = h * (sub / 12f)
                    drawLine(
                        color = HarmonicPalette.MajorAxis,
                        start = Offset(centerX - tickHalf, ty),
                        end = Offset(centerX + tickHalf, ty),
                        strokeWidth = 1f,
                    )
                }

                val scrollOffset = elapsedSeconds * 2.4f
                val sampleCount = 160
                val targetPath = Path()
                val operatorPath = Path()

                for (i in 0..sampleCount) {
                    val normX = i / sampleCount.toFloat()
                    val x = normX * w

                    val targetAngle =
                        TWO_PI_F * state.effectiveTargetFreq * normX +
                            state.effectiveTargetPhase -
                            scrollOffset
                    val targetY = centerY - waveAmplitude * sin(targetAngle)

                    val opAngle =
                        TWO_PI_F * state.operatorFreq * normX +
                            state.operatorPhase -
                            scrollOffset
                    val opY = centerY - waveAmplitude * sin(opAngle)

                    if (i == 0) {
                        targetPath.moveTo(x, targetY)
                        operatorPath.moveTo(x, opY)
                    } else {
                        targetPath.lineTo(x, targetY)
                        operatorPath.lineTo(x, opY)
                    }
                }

                drawPath(
                    path = targetPath,
                    color = HarmonicPalette.PhosphorAmber.copy(alpha = 0.22f),
                    style = Stroke(
                        width = 6.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
                drawPath(
                    path = targetPath,
                    color = HarmonicPalette.PhosphorAmber.copy(alpha = 0.92f),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )

                drawPath(
                    path = operatorPath,
                    color = HarmonicPalette.PhosphorGreen.copy(alpha = 0.14f),
                    style = Stroke(
                        width = 10.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
                drawPath(
                    path = operatorPath,
                    color = HarmonicPalette.PhosphorGreen.copy(alpha = 0.34f),
                    style = Stroke(
                        width = 4.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
                drawPath(
                    path = operatorPath,
                    color = HarmonicPalette.PhosphorGreen,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )

                val scanStep = 3.dp.toPx().coerceAtLeast(2f)
                var scanY = 0f
                while (scanY < h) {
                    drawLine(
                        color = Color.Black.copy(alpha = 0.24f),
                        start = Offset(0f, scanY),
                        end = Offset(w, scanY),
                        strokeWidth = 1f,
                    )
                    scanY += scanStep
                }

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.42f),
                        ),
                        center = Offset(centerX, centerY),
                        radius = maxOf(w, h) * 0.72f,
                    ),
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WaveLegendTag(
                    label = "TGT ${formatHarmonicDecimal(state.effectiveTargetFreq, 2)}Hz",
                    color = HarmonicPalette.PhosphorAmber,
                )
                WaveLegendTag(
                    label = "OP ${formatHarmonicDecimal(state.operatorFreq, 2)}Hz",
                    color = HarmonicPalette.PhosphorGreen,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OscilloscopeMetricBox(
                label = "LOCK RATIO",
                value = "$lockPercent%",
                valueColor = statusColor,
                modifier = Modifier.weight(1.15f),
            )
            OscilloscopeMetricBox(
                label = "FREQ Δ",
                value = "${formatHarmonicDecimal(freqDelta, 2)} Hz",
                valueColor = if (freqDelta <= 0.15f) {
                    HarmonicPalette.PhosphorGreen
                } else {
                    HarmonicPalette.TelemetryCyan
                },
                modifier = Modifier.weight(1f),
            )
            OscilloscopeMetricBox(
                label = "PHASE Δ",
                value = "${formatHarmonicDecimal(phaseDelta, 2)} rad",
                valueColor = if (phaseDelta <= 0.25f) {
                    HarmonicPalette.PhosphorGreen
                } else {
                    HarmonicPalette.TelemetryCyan
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WaveLegendTag(
    label: String,
    color: Color,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun OscilloscopeMetricBox(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(HarmonicPalette.PanelInset)
            .border(1.dp, HarmonicPalette.Bezel, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = HarmonicPalette.TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Operator harmonic synthesizer containing rotary frequency and phase dials with pointer, wheel, and keyboard control.
 */
@Composable
fun OperatorRotaryControls(
    frequency: Float,
    phase: Float,
    isLocked: Boolean,
    onUpdate: (Float, Float) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val latestFrequency by rememberUpdatedState(frequency.coerceIn(1f, 5f))
    val latestPhase by rememberUpdatedState(wrapPhaseRad(phase))
    val latestOnUpdate by rememberUpdatedState(onUpdate)

    val panelBorderColor = if (isLocked) {
        HarmonicPalette.PhosphorGreen.copy(alpha = 0.55f)
    } else {
        HarmonicPalette.Bezel
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HarmonicPalette.PanelBg)
            .border(1.5.dp, panelBorderColor, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "HARMONIC SYNTHESIZER",
                style = MaterialTheme.typography.titleSmall,
                color = HarmonicPalette.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = when {
                    !enabled -> "LOCKED"
                    isLocked -> "PHASE LOCK"
                    else -> "MANUAL INPUT"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    !enabled -> HarmonicPalette.TextMuted
                    isLocked -> HarmonicPalette.PhosphorGreen
                    else -> HarmonicPalette.TelemetryCyan
                },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RotaryDial(
                label = "FREQUENCY (Hz)",
                value = latestFrequency,
                minValue = 1.0f,
                maxValue = 5.0f,
                step = 0.05f,
                unitSuffix = "Hz",
                accentColor = HarmonicPalette.TelemetryCyan,
                wrapAround = false,
                enabled = enabled,
                onValueChange = { nextFreq ->
                    latestOnUpdate(nextFreq.coerceIn(1.0f, 5.0f), latestPhase)
                },
                modifier = Modifier.weight(1f),
            )

            RotaryDial(
                label = "PHASE (rad)",
                value = latestPhase,
                minValue = 0.0f,
                maxValue = MAX_PHASE_RAD,
                step = 0.05f,
                unitSuffix = "rad",
                accentColor = HarmonicPalette.PhosphorGreen,
                wrapAround = true,
                enabled = enabled,
                onValueChange = { nextPhase ->
                    latestOnUpdate(latestFrequency, wrapPhaseRad(nextPhase))
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RotaryDial(
    label: String,
    value: Float,
    minValue: Float,
    maxValue: Float,
    step: Float,
    unitSuffix: String,
    accentColor: Color,
    wrapAround: Boolean,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestValue by rememberUpdatedState(value)
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val latestEnabled by rememberUpdatedState(enabled)

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val range = maxValue - minValue
    val fraction = if (range > 0f) ((value - minValue) / range).coerceIn(0f, 1f) else 0f
    val startAngleDeg = if (wrapAround) -90f else 135f
    val sweepSpanDeg = if (wrapAround) 360f else 270f
    val sweepSpanRad = sweepSpanDeg * PI_F / 180f

    fun adjustByDelta(delta: Float) {
        val next = if (wrapAround) {
            wrapInRange(latestValue + delta, minValue, maxValue)
        } else {
            (latestValue + delta).coerceIn(minValue, maxValue)
        }
        latestOnValueChange(next)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(HarmonicPalette.PanelInset)
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) accentColor.copy(alpha = 0.85f) else HarmonicPalette.Bezel,
                shape = RoundedCornerShape(10.dp),
            )
            .padding(10.dp)
            .alpha(if (enabled) 1f else 0.6f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = HarmonicPalette.TextMuted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(maxWidth = 164.dp, maxHeight = 164.dp)
                .aspectRatio(1f)
                .hoverable(interactionSource = interactionSource, enabled = enabled)
                .focusable(enabled = enabled, interactionSource = interactionSource)
                .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
                .onPreviewKeyEvent { keyEvent ->
                    if (!latestEnabled || keyEvent.type != KeyEventType.KeyDown) {
                        return@onPreviewKeyEvent false
                    }
                    val delta = when (keyEvent.key) {
                        Key.DirectionLeft, Key.DirectionDown -> -step
                        Key.DirectionRight, Key.DirectionUp -> step
                        Key.PageDown -> -step * 5f
                        Key.PageUp -> step * 5f
                        else -> null
                    }
                    if (delta != null) {
                        adjustByDelta(delta)
                        true
                    } else {
                        false
                    }
                }
                .semantics {
                    contentDescription = "$label: ${formatHarmonicDecimal(value, 2)} $unitSuffix"
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = value,
                        range = minValue..maxValue,
                    )
                }
                .pointerInput(enabled, wrapAround, minValue, maxValue) {
                    if (!enabled) return@pointerInput
                    var previousAngleRad = 0f
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val center = Offset(size.width * 0.5f, size.height * 0.5f)
                            previousAngleRad = atan2(
                                startOffset.y - center.y,
                                startOffset.x - center.x,
                            )
                        },
                        onDrag = { change, _ ->
                            if (!latestEnabled) return@detectDragGestures
                            change.consume()
                            val center = Offset(size.width * 0.5f, size.height * 0.5f)
                            val dx = change.position.x - center.x
                            val dy = change.position.y - center.y
                            if (dx * dx + dy * dy < 16f) return@detectDragGestures

                            val currentAngleRad = atan2(dy, dx)
                            var deltaRad = currentAngleRad - previousAngleRad
                            while (deltaRad > PI_F) deltaRad -= TWO_PI_F
                            while (deltaRad < -PI_F) deltaRad += TWO_PI_F
                            previousAngleRad = currentAngleRad

                            val deltaValue = (deltaRad / sweepSpanRad) * range
                            adjustByDelta(deltaValue)
                        },
                    )
                }
                .pointerInput(enabled, wrapAround, minValue, maxValue, step) {
                    if (!enabled) return@pointerInput
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Scroll && latestEnabled) {
                                val scroll = event.changes.firstOrNull()?.scrollDelta ?: Offset.Zero
                                val primary = if (scroll.y != 0f) -scroll.y else scroll.x
                                if (primary != 0f) {
                                    event.changes.forEach { it.consume() }
                                    adjustByDelta(primary * step)
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val diameter = min(size.width, size.height)
                if (diameter <= 0f) return@Canvas

                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                val outerRadius = diameter * 0.47f
                val tickOuterRadius = outerRadius
                val tickInnerMinor = outerRadius * 0.87f
                val tickInnerMajor = outerRadius * 0.81f
                val arcRadius = outerRadius * 0.74f
                val ringOuterRadius = outerRadius * 0.65f
                val knobRadius = outerRadius * 0.56f

                if (isFocused) {
                    drawCircle(
                        color = accentColor.copy(alpha = 0.25f),
                        radius = outerRadius + 2.dp.toPx(),
                        center = center,
                        style = Stroke(width = 4.dp.toPx()),
                    )
                    drawCircle(
                        color = accentColor.copy(alpha = 0.92f),
                        radius = outerRadius + 2.dp.toPx(),
                        center = center,
                        style = Stroke(width = 1.8.dp.toPx()),
                    )
                } else if (isHovered && enabled) {
                    drawCircle(
                        color = accentColor.copy(alpha = 0.35f),
                        radius = outerRadius + 1.5.dp.toPx(),
                        center = center,
                        style = Stroke(width = 1.2.dp.toPx()),
                    )
                }

                val tickCount = 24
                val tickDivisor = if (wrapAround) tickCount else (tickCount - 1)
                for (i in 0 until tickCount) {
                    val tickFrac = i / tickDivisor.toFloat()
                    val tickDeg = startAngleDeg + tickFrac * sweepSpanDeg
                    val tickRad = tickDeg * PI_F / 180f
                    val cosA = cos(tickRad)
                    val sinA = sin(tickRad)

                    val isMajor = i % 4 == 0 || (!wrapAround && i == tickCount - 1)
                    val isActive = tickFrac <= fraction + 0.005f
                    val innerR = if (isMajor) tickInnerMajor else tickInnerMinor

                    drawLine(
                        color = if (isActive) {
                            accentColor.copy(alpha = if (enabled) 0.95f else 0.45f)
                        } else {
                            HarmonicPalette.TextMuted.copy(alpha = 0.35f)
                        },
                        start = Offset(center.x + innerR * cosA, center.y + innerR * sinA),
                        end = Offset(center.x + tickOuterRadius * cosA, center.y + tickOuterRadius * sinA),
                        strokeWidth = if (isMajor) 2.dp.toPx() else 1.2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }

                val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)
                val arcSize = Size(arcRadius * 2f, arcRadius * 2f)
                val arcStrokeWidth = 3.5.dp.toPx()

                drawArc(
                    color = HarmonicPalette.Bezel,
                    startAngle = startAngleDeg,
                    sweepAngle = sweepSpanDeg,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = arcStrokeWidth, cap = StrokeCap.Round),
                )

                val activeSweep = fraction * sweepSpanDeg
                if (activeSweep > 0.5f) {
                    drawArc(
                        color = accentColor.copy(alpha = 0.25f),
                        startAngle = startAngleDeg,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = arcStrokeWidth * 2.1f, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = accentColor,
                        startAngle = startAngleDeg,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = arcStrokeWidth, cap = StrokeCap.Round),
                    )
                }

                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            HarmonicPalette.MetalHighlight,
                            HarmonicPalette.KnobOuterMid,
                            HarmonicPalette.MetalShadow,
                        ),
                        start = Offset(center.x - ringOuterRadius, center.y - ringOuterRadius),
                        end = Offset(center.x + ringOuterRadius, center.y + ringOuterRadius),
                    ),
                    radius = ringOuterRadius,
                    center = center,
                )
                drawCircle(
                    color = if (isHovered && enabled) {
                        accentColor.copy(alpha = 0.65f)
                    } else {
                        HarmonicPalette.Bezel
                    },
                    radius = ringOuterRadius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            HarmonicPalette.KnobFaceLight,
                            HarmonicPalette.KnobFaceDark,
                        ),
                        center = Offset(
                            center.x - knobRadius * 0.2f,
                            center.y - knobRadius * 0.2f,
                        ),
                        radius = knobRadius * 1.3f,
                    ),
                    radius = knobRadius,
                    center = center,
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.6f),
                    radius = knobRadius,
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx()),
                )

                val needleDeg = startAngleDeg + fraction * sweepSpanDeg
                val needleRad = needleDeg * PI_F / 180f
                val needleCos = cos(needleRad)
                val needleSin = sin(needleRad)
                val needleStart = Offset(
                    center.x + knobRadius * 0.18f * needleCos,
                    center.y + knobRadius * 0.18f * needleSin,
                )
                val needleEnd = Offset(
                    center.x + knobRadius * 0.88f * needleCos,
                    center.y + knobRadius * 0.88f * needleSin,
                )

                drawLine(
                    color = accentColor.copy(alpha = 0.35f),
                    start = needleStart,
                    end = needleEnd,
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = accentColor,
                    start = needleStart,
                    end = needleEnd,
                    strokeWidth = 2.4.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                drawCircle(
                    color = HarmonicPalette.KnobHub,
                    radius = knobRadius * 0.22f,
                    center = center,
                )
                drawCircle(
                    color = accentColor.copy(alpha = 0.65f),
                    radius = knobRadius * 0.08f,
                    center = center,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(HarmonicPalette.ChassisBg)
                .border(1.dp, HarmonicPalette.Bezel, RoundedCornerShape(6.dp))
                .padding(vertical = 4.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${formatHarmonicDecimal(value, 2)} $unitSuffix",
                style = MaterialTheme.typography.titleSmall,
                color = accentColor,
                maxLines = 1,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FineTuneButton(
                symbol = "-",
                description = "Decrease $label",
                enabled = enabled,
                onClick = { adjustByDelta(-step) },
                modifier = Modifier.weight(1f),
            )
            FineTuneButton(
                symbol = "+",
                description = "Increase $label",
                enabled = enabled,
                onClick = { adjustByDelta(step) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FineTuneButton(
    symbol: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val bgAlpha = when {
        !enabled -> 0.35f
        isHovered -> 0.92f
        else -> 0.7f
    }
    val borderColor = when {
        isFocused -> HarmonicPalette.TelemetryCyan
        isHovered && enabled -> HarmonicPalette.TelemetryCyan.copy(alpha = 0.7f)
        else -> HarmonicPalette.MetalHighlight.copy(alpha = 0.6f)
    }

    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(HarmonicPalette.Bezel.copy(alpha = bgAlpha))
            .border(if (isFocused) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(6.dp))
            .hoverable(interactionSource = interactionSource, enabled = enabled)
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isFocused) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val inset = 2.dp.toPx()
                drawRoundRect(
                    color = HarmonicPalette.TelemetryCyan.copy(alpha = 0.85f),
                    topLeft = Offset(inset, inset),
                    size = Size(
                        width = (size.width - inset * 2f).coerceAtLeast(0f),
                        height = (size.height - inset * 2f).coerceAtLeast(0f),
                    ),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
        }
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) HarmonicPalette.TextPrimary else HarmonicPalette.TextMuted,
        )
    }
}

private fun shortestAngularDelta(a: Float, b: Float): Float {
    var diff = (a - b) % TWO_PI_F
    if (diff > PI_F) diff -= TWO_PI_F
    if (diff < -PI_F) diff += TWO_PI_F
    return abs(diff)
}

private fun wrapPhaseRad(phase: Float): Float =
    wrapInRange(phase, 0f, MAX_PHASE_RAD)

private fun wrapInRange(value: Float, minValue: Float, maxValue: Float): Float {
    val span = maxValue - minValue
    if (span <= 0f) return minValue
    var shifted = (value - minValue) % span
    if (shifted < 0f) shifted += span
    return minValue + shifted
}
