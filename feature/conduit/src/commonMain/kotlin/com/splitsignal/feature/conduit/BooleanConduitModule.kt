package com.splitsignal.feature.conduit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitsignal.core.domain.ConduitUiState
import kotlin.math.abs
import kotlin.math.roundToInt

private const val MAX_VOLTAGE = 24f
private const val SWITCH_COUNT = 6

/**
 * Analyst telemetry panel showing bus voltage, diagnostic matrix, and dual-lock stability.
 */
@Composable
fun AnalystTelemetryPanel(
    state: ConduitUiState,
    widthSizeClass: WindowWidthSizeClass,
    modifier: Modifier = Modifier,
) {
    val switches = state.switches
    val currentV = state.currentVoltage.coerceIn(0f, MAX_VOLTAGE)
    val targetV = state.targetVoltage.coerceIn(0f, MAX_VOLTAGE)
    val deltaV = currentV - targetV
    val isConduitLocked = state.isConduitLocked
    val bothLocked = state.isHarmonicLocked && state.isConduitLocked

    val rawStability = state.lockStabilityProgress
    val stabilityFraction = if (rawStability > 1f) {
        (rawStability / 100f).coerceIn(0f, 1f)
    } else {
        rawStability.coerceIn(0f, 1f)
    }
    val stabilityPercent = (stabilityFraction * 100f).roundToInt()

    val conduitStatusColor = when {
        isConduitLocked -> ConduitPalette.PhosphorGreen
        currentV > targetV + 0.5f -> ConduitPalette.AlertRed
        else -> ConduitPalette.PhosphorAmber
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ConduitPalette.PanelBg)
            .border(1.5.dp, ConduitPalette.Bezel, RoundedCornerShape(12.dp))
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
                    text = "BOOLEAN CONDUIT TELEMETRY",
                    style = MaterialTheme.typography.titleSmall,
                    color = ConduitPalette.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "BUS RAIL 0..24V // DIAGNOSTIC MATRIX",
                    style = MaterialTheme.typography.labelSmall,
                    color = ConduitPalette.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(conduitStatusColor.copy(alpha = 0.16f))
                    .border(1.dp, conduitStatusColor.copy(alpha = 0.8f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = if (isConduitLocked) "CONDUIT LOCK" else "CALIBRATING",
                    style = MaterialTheme.typography.labelMedium,
                    color = conduitStatusColor,
                    maxLines = 1,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ConduitPalette.PanelInset)
                .border(1.dp, ConduitPalette.Bezel, RoundedCornerShape(10.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VoltageReadoutPill(
                    label = "BUS",
                    value = "${formatConduitDecimal(currentV, 1)}V",
                    color = if (isConduitLocked) {
                        ConduitPalette.PhosphorGreen
                    } else {
                        ConduitPalette.TelemetryCyan
                    },
                )
                VoltageReadoutPill(
                    label = "TARGET",
                    value = "${formatConduitDecimal(targetV, 1)}V",
                    color = ConduitPalette.PhosphorAmber,
                )
                val deltaPrefix = if (deltaV > 0.05f) "+" else ""
                VoltageReadoutPill(
                    label = "DELTA",
                    value = "$deltaPrefix${formatConduitDecimal(deltaV, 1)}V",
                    color = conduitStatusColor,
                )
            }

            VoltageMeterCanvas(
                currentVoltage = currentV,
                targetVoltage = targetV,
                isLocked = isConduitLocked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf("0V", "6V", "12V", "18V", "24V").forEach { mark ->
                    Text(
                        text = mark,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = ConduitPalette.TextMuted,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ConduitPalette.PanelInset)
                .border(1.dp, ConduitPalette.Bezel, RoundedCornerShape(10.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(ConduitPalette.TelemetryCyan.copy(alpha = 0.1f))
                    .border(
                        1.dp,
                        ConduitPalette.TelemetryCyan.copy(alpha = 0.45f),
                        RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "RULE:",
                    style = MaterialTheme.typography.labelSmall,
                    color = ConduitPalette.TelemetryCyan,
                )
                Text(
                    text = state.activeRuleLabel.ifBlank { "STANDARD LINEAR SUM" },
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = ConduitPalette.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            val hasFault = state.faultHintLabel.isNotBlank()
            val faultColor = if (hasFault) ConduitPalette.PhosphorAmber else ConduitPalette.TextMuted
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (hasFault) {
                            ConduitPalette.PhosphorAmber.copy(alpha = 0.1f)
                        } else {
                            ConduitPalette.ChassisBg
                        },
                    )
                    .border(
                        1.dp,
                        if (hasFault) {
                            ConduitPalette.PhosphorAmber.copy(alpha = 0.45f)
                        } else {
                            ConduitPalette.Bezel
                        },
                        RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "DIAG:",
                    style = MaterialTheme.typography.labelSmall,
                    color = faultColor,
                )
                Text(
                    text = state.faultHintLabel.ifBlank { "NO FAULT ANOMALY DETECTED" },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasFault) ConduitPalette.PhosphorAmber else ConduitPalette.TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            val columns = when (widthSizeClass) {
                WindowWidthSizeClass.Expanded, WindowWidthSizeClass.Medium -> 3
                else -> 2
            }
            val rows = (SWITCH_COUNT + columns - 1) / columns
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        for (c in 0 until columns) {
                            val index = r * columns + c
                            if (index < SWITCH_COUNT) {
                                val isInverted = index == state.invertedSwitchIndex
                                val weight = state.switchWeights.getOrNull(index)
                                val isEnergized = switches.getOrElse(index) { false }
                                BusLineDiagnosticCell(
                                    channelLabel = "SW-${index + 1}",
                                    weight = weight,
                                    isInverted = isInverted,
                                    isEnergized = isEnergized,
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ConduitPalette.PanelInset)
                .border(
                    1.dp,
                    if (bothLocked) {
                        ConduitPalette.PhosphorGreen.copy(alpha = 0.65f)
                    } else {
                        ConduitPalette.Bezel
                    },
                    RoundedCornerShape(10.dp),
                )
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (bothLocked) {
                        "LOCK STABILITY // HOLDING DUAL SYNC"
                    } else {
                        "LOCK STABILITY // AWAITING DUAL LOCK"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (bothLocked) {
                        ConduitPalette.PhosphorGreen
                    } else {
                        ConduitPalette.TextMuted
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "$stabilityPercent%",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (bothLocked) {
                        ConduitPalette.PhosphorGreen
                    } else {
                        ConduitPalette.TelemetryCyan
                    },
                )
            }

            LockStabilityBarCanvas(
                progress = stabilityFraction,
                bothLocked = bothLocked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp),
            )
        }
    }
}

@Composable
private fun VoltageReadoutPill(
    label: String,
    value: String,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = ConduitPalette.TextMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
        )
    }
}

@Composable
private fun VoltageMeterCanvas(
    currentVoltage: Float,
    targetVoltage: Float,
    isLocked: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val padX = 4.dp.toPx()
        val usableW = (w - padX * 2f).coerceAtLeast(1f)

        val currentNorm = (currentVoltage / MAX_VOLTAGE).coerceIn(0f, 1f)
        val targetNorm = (targetVoltage / MAX_VOLTAGE).coerceIn(0f, 1f)
        val targetX = padX + targetNorm * usableW
        val currentX = padX + currentNorm * usableW

        val markerHeight = 14.dp.toPx()
        val barTop = markerHeight + 6.dp.toPx()
        val barHeight = (h - barTop - 8.dp.toPx()).coerceAtLeast(18.dp.toPx())

        drawRoundRect(
            color = ConduitPalette.ChassisBg,
            topLeft = Offset(padX, barTop),
            size = Size(usableW, barHeight),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
        )

        val segmentCount = 24
        val gap = 2.dp.toPx()
        val segWidth = ((usableW - gap * (segmentCount + 1)) / segmentCount).coerceAtLeast(2f)
        val segPadY = 4.dp.toPx()
        val segHeight = (barHeight - segPadY * 2f).coerceAtLeast(4f)
        val activeSegments = currentVoltage.roundToInt().coerceIn(0, segmentCount)

        for (i in 0 until segmentCount) {
            val sx = padX + gap + i * (segWidth + gap)
            val segVolt = i + 1f
            val isLit = (i + 1) <= activeSegments || (currentVoltage > i + 0.15f && currentVoltage > 0f)

            val segColor = when {
                !isLit -> ConduitPalette.Bezel.copy(alpha = 0.55f)
                isLocked -> ConduitPalette.PhosphorGreen
                segVolt > targetVoltage + 0.5f -> ConduitPalette.AlertRed
                abs(segVolt - targetVoltage) <= 1.0f -> ConduitPalette.PhosphorAmber
                else -> ConduitPalette.TelemetryCyan
            }

            if (isLit) {
                drawRoundRect(
                    color = segColor.copy(alpha = 0.25f),
                    topLeft = Offset(sx - 1.dp.toPx(), barTop + segPadY - 1.dp.toPx()),
                    size = Size(segWidth + 2.dp.toPx(), segHeight + 2.dp.toPx()),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                )
            }

            drawRoundRect(
                color = segColor,
                topLeft = Offset(sx, barTop + segPadY),
                size = Size(segWidth, segHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            )
        }

        val tolHalfW = (0.5f / MAX_VOLTAGE) * usableW
        drawRect(
            color = ConduitPalette.PhosphorAmber.copy(alpha = 0.14f),
            topLeft = Offset((targetX - tolHalfW).coerceAtLeast(padX), barTop),
            size = Size((tolHalfW * 2f).coerceAtMost(usableW), barHeight),
        )

        val triSize = 6.dp.toPx()
        val targetPath = Path().apply {
            moveTo(targetX, markerHeight)
            lineTo(targetX - triSize, markerHeight - triSize * 1.4f)
            lineTo(targetX + triSize, markerHeight - triSize * 1.4f)
            close()
        }
        drawPath(path = targetPath, color = ConduitPalette.PhosphorAmber)
        drawLine(
            color = ConduitPalette.PhosphorAmber.copy(alpha = 0.9f),
            start = Offset(targetX, barTop - 2.dp.toPx()),
            end = Offset(targetX, barTop + barHeight + 2.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )

        val needleColor = when {
            isLocked -> ConduitPalette.PhosphorGreen
            currentVoltage > targetVoltage + 0.5f -> ConduitPalette.AlertRed
            else -> ConduitPalette.TelemetryCyan
        }
        drawLine(
            color = needleColor.copy(alpha = 0.35f),
            start = Offset(currentX, barTop),
            end = Offset(currentX, h),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = needleColor,
            start = Offset(currentX, barTop),
            end = Offset(currentX, h),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun BusLineDiagnosticCell(
    channelLabel: String,
    weight: Float?,
    isInverted: Boolean,
    isEnergized: Boolean,
    modifier: Modifier = Modifier,
) {
    val statusColor = if (isInverted) ConduitPalette.AlertRed else ConduitPalette.PhosphorGreen
    val statusTag = if (isInverted) "INVERTED" else "NOMINAL"
    val borderColor = when {
        isInverted -> ConduitPalette.AlertRed.copy(alpha = 0.5f)
        isEnergized -> ConduitPalette.TelemetryCyan.copy(alpha = 0.55f)
        else -> ConduitPalette.Bezel
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(ConduitPalette.ChassisBg)
            .border(1.dp, borderColor, RoundedCornerShape(7.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(
                    if (isEnergized) {
                        ConduitPalette.TelemetryCyan
                    } else {
                        ConduitPalette.Bezel
                    },
                ),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = channelLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = ConduitPalette.TextPrimary,
                    maxLines = 1,
                )
                if (weight != null) {
                    val sign = if (weight > 0f) "+" else ""
                    Text(
                        text = "$sign${formatConduitDecimal(weight, 1)}V",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = ConduitPalette.TelemetryCyan,
                        maxLines = 1,
                    )
                }
            }
            Text(
                text = statusTag,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = statusColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LockStabilityBarCanvas(
    progress: Float,
    bothLocked: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val corner = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        drawRoundRect(
            color = ConduitPalette.ChassisBg,
            size = size,
            cornerRadius = corner,
        )

        val segmentCount = 20
        val gap = 2.dp.toPx()
        val segW = ((w - gap * (segmentCount + 1)) / segmentCount).coerceAtLeast(1f)
        val segH = (h - 4.dp.toPx()).coerceAtLeast(2f)
        val litCount = (progress.coerceIn(0f, 1f) * segmentCount).roundToInt()

        val activeColor = if (bothLocked) {
            ConduitPalette.PhosphorGreen
        } else {
            ConduitPalette.PhosphorAmber
        }

        for (i in 0 until segmentCount) {
            val sx = gap + i * (segW + gap)
            val isLit = i < litCount
            drawRoundRect(
                color = if (isLit) activeColor else ConduitPalette.Bezel.copy(alpha = 0.55f),
                topLeft = Offset(sx, 2.dp.toPx()),
                size = Size(segW, segH),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            )
        }

        drawRoundRect(
            color = ConduitPalette.Bezel,
            size = size,
            cornerRadius = corner,
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

/**
 * Operator circuit breaker switch bank adapting between 6-column and 3-column layouts by [widthSizeClass].
 */
@Composable
fun OperatorSwitchArray(
    switches: List<Boolean>,
    onToggle: (Int) -> Unit,
    enabled: Boolean,
    widthSizeClass: WindowWidthSizeClass,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ConduitPalette.PanelBg)
            .border(1.5.dp, ConduitPalette.Bezel, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .alpha(if (enabled) 1f else 0.6f),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "CIRCUIT BREAKER BAY",
                style = MaterialTheme.typography.titleSmall,
                color = ConduitPalette.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            val activeCount = (0 until SWITCH_COUNT).count { switches.getOrElse(it) { false } }
            Text(
                text = "$activeCount/6 ACTIVE",
                style = MaterialTheme.typography.labelSmall,
                color = if (activeCount > 0) ConduitPalette.TelemetryCyan else ConduitPalette.TextMuted,
            )
        }

        val columns = if (widthSizeClass == WindowWidthSizeClass.Expanded) 6 else 3
        val rows = (SWITCH_COUNT + columns - 1) / columns

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for (r in 0 until rows) {
                val startIdx = r * columns
                val endIdx = (startIdx + columns).coerceAtMost(SWITCH_COUNT)
                val rowActiveFlags = (startIdx until endIdx).map { idx ->
                    switches.getOrElse(idx) { false }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    BusbarTraceBackdrop(
                        activeFlags = rowActiveFlags,
                        modifier = Modifier.matchParentSize(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (idx in startIdx until endIdx) {
                            val isOn = switches.getOrElse(idx) { false }
                            CircuitBreakerSwitch(
                                label = "SW-${idx + 1}",
                                isOn = isOn,
                                enabled = enabled,
                                onToggle = { onToggle(idx) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BusbarTraceBackdrop(
    activeFlags: List<Boolean>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f || activeFlags.isEmpty()) return@Canvas

        val railYTop = h * 0.22f
        val railYBottom = h * 0.80f
        val count = activeFlags.size
        val slotW = w / count.toFloat()

        drawLine(
            color = ConduitPalette.CopperTrace.copy(alpha = 0.4f),
            start = Offset(slotW * 0.5f, railYTop),
            end = Offset(w - slotW * 0.5f, railYTop),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = ConduitPalette.CopperTrace.copy(alpha = 0.45f),
            start = Offset(slotW * 0.5f, railYBottom),
            end = Offset(w - slotW * 0.5f, railYBottom),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )

        activeFlags.forEachIndexed { index, isActive ->
            val cx = slotW * (index + 0.5f)
            val traceColor = if (isActive) {
                ConduitPalette.TelemetryCyan
            } else {
                ConduitPalette.CopperTrace.copy(alpha = 0.45f)
            }
            if (isActive) {
                drawLine(
                    color = ConduitPalette.TelemetryCyan.copy(alpha = 0.25f),
                    start = Offset(cx, railYTop),
                    end = Offset(cx, railYBottom),
                    strokeWidth = 7.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            drawLine(
                color = traceColor,
                start = Offset(cx, railYTop),
                end = Offset(cx, railYBottom),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun CircuitBreakerSwitch(
    label: String,
    isOn: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val accent = if (isOn) ConduitPalette.PhosphorGreen else ConduitPalette.TextMuted
    val borderColor = when {
        isFocused -> ConduitPalette.PhosphorAmber
        isHovered && enabled -> ConduitPalette.TelemetryCyan
        isOn -> ConduitPalette.TelemetryCyan.copy(alpha = 0.8f)
        else -> ConduitPalette.Bezel
    }
    val borderWidth = if (isFocused) 2.dp else 1.5.dp

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isHovered && enabled) {
                    ConduitPalette.PanelBg
                } else {
                    ConduitPalette.PanelInset
                },
            )
            .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
            .hoverable(interactionSource = interactionSource, enabled = enabled)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .toggleable(
                value = isOn,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onValueChange = { onToggle() },
            )
            .semantics {
                contentDescription = "$label circuit breaker ${if (isOn) "engaged" else "open"}"
            }
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(modifier = Modifier.size(12.dp)) {
            val r = size.minDimension * 0.45f
            if (isOn) {
                drawCircle(
                    color = ConduitPalette.PhosphorGreen.copy(alpha = 0.35f),
                    radius = r * 1.35f,
                )
            }
            drawCircle(
                color = if (isOn) ConduitPalette.PhosphorGreen else ConduitPalette.Bezel,
                radius = r,
            )
            drawCircle(
                color = Color.White.copy(alpha = if (isOn) 0.55f else 0.1f),
                radius = r * 0.35f,
                center = Offset(center.x - r * 0.22f, center.y - r * 0.22f),
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 4.dp),
        ) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            drawRoundRect(
                color = ConduitPalette.ChassisBg,
                size = size,
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
            )
            drawRoundRect(
                color = ConduitPalette.MetalShadow,
                size = size,
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx()),
            )

            val leverMargin = 4.dp.toPx()
            val leverW = (w - leverMargin * 2f).coerceAtLeast(8f)
            val leverH = (h * 0.52f).coerceAtLeast(12f)
            val leverTop = if (isOn) leverMargin else (h - leverMargin - leverH)

            drawRoundRect(
                color = Color.Black.copy(alpha = 0.55f),
                topLeft = Offset(leverMargin, leverTop + 2.dp.toPx()),
                size = Size(leverW, leverH),
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
            )

            val topColor = if (isOn) ConduitPalette.RockerOnTop else ConduitPalette.RockerOffTop
            val bottomColor = if (isOn) ConduitPalette.RockerOnBottom else ConduitPalette.RockerOffBottom
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(topColor, bottomColor),
                    startY = leverTop,
                    endY = leverTop + leverH,
                ),
                topLeft = Offset(leverMargin, leverTop),
                size = Size(leverW, leverH),
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
            )

            val stripColor = if (isOn) {
                ConduitPalette.TelemetryCyan
            } else {
                ConduitPalette.TextMuted.copy(alpha = 0.35f)
            }
            val stripY = if (isOn) leverTop + 3.dp.toPx() else leverTop + leverH - 5.dp.toPx()
            drawRoundRect(
                color = stripColor,
                topLeft = Offset(leverMargin + 4.dp.toPx(), stripY),
                size = Size((leverW - 8.dp.toPx()).coerceAtLeast(4f), 2.5.dp.toPx()),
                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
            )

            val ridgeStartX = leverMargin + leverW * 0.22f
            val ridgeEndX = leverMargin + leverW * 0.78f
            val ridgeCenterY = leverTop + leverH * 0.54f
            for (offsetIdx in -1..1) {
                val ry = ridgeCenterY + offsetIdx * 3.5.dp.toPx()
                drawLine(
                    color = ConduitPalette.MetalHighlight.copy(alpha = 0.55f),
                    start = Offset(ridgeStartX, ry),
                    end = Offset(ridgeEndX, ry),
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = ConduitPalette.TextPrimary,
            maxLines = 1,
        )
        Text(
            text = if (isOn) "ON" else "OFF",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = accent,
            maxLines = 1,
        )
    }
}
