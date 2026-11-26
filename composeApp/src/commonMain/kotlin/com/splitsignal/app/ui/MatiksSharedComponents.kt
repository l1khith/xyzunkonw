package com.splitsignal.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitsignal.app.ui.design.SsIcon
import com.splitsignal.app.ui.design.SsIcons
import com.splitsignal.core.domain.GameType
import com.splitsignal.core.domain.OpponentProgress
import com.splitsignal.core.domain.ProgressionState
import com.splitsignal.core.domain.SolveRewardBreakdown
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Linear-inspired elevated card with `#101114` surface, `1.dp` translucent hairline,
 * and subtle top-rim highlight gradient.
 */
@Composable
fun LinearCard(
    modifier: Modifier = Modifier,
    borderColor: Color = SplitSignalColors.BorderSubtle,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SplitSignalColors.SurfaceCard)
            .background(SplitSignalColors.CardTopRimBrush)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/**
 * Tactile grid tile with `0.94f` spring scale on press and `8% -> 22%` white border brighten.
 */
@Composable
fun TactileTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = SplitSignalColors.SurfaceTile,
    restingBorderColor: Color = SplitSignalColors.BorderSubtle,
    cornerRadius: Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState().value
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
    )
    val borderColor by animateColorAsState(
        targetValue = if (isPressed && enabled) SplitSignalColors.BorderPress else restingBorderColor,
        animationSpec = tween(durationMillis = 120),
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(cornerRadius))
            .background(if (isPressed && enabled) SplitSignalColors.SurfaceTileActive else backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * Wraps a game board to provide a damped horizontal shake (`±6.dp` over `240ms`)
 * and subtle crimson border flash whenever [mistakeCount] increases.
 */
@Composable
fun MistakeShakeBox(
    mistakeCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val offsetX = remember { Animatable(0f) }
    var flashCrimson by remember { mutableStateOf(false) }

    LaunchedEffect(mistakeCount) {
        if (mistakeCount > 0) {
            flashCrimson = true
            val keyframes = listOf(6f, -6f, 4f, -4f, 2f, 0f)
            keyframes.forEach { target ->
                offsetX.animateTo(target, animationSpec = tween(durationMillis = 40))
            }
            flashCrimson = false
        }
    }

    val border by animateColorAsState(
        targetValue = if (flashCrimson) SplitSignalColors.AccentCrimson.copy(alpha = 0.7f) else Color.Transparent,
        animationSpec = tween(durationMillis = 120),
    )

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.value.dp.roundToPx(), 0) }
            .border(1.dp, border, RoundedCornerShape(14.dp)),
    ) {
        content()
    }
}

/**
 * Shared Linear top bar for all puzzle screens with tabular timer (`tnum`),
 * optional Triathlon round badge, room code badge, and Hint/Peek trigger.
 */
@Composable
fun MatiksTopBar(
    title: String,
    elapsedSeconds: Int,
    hintTokens: Int,
    hintLabel: String = "Hint",
    roomCode: String? = null,
    triathlonRoundIndex: Int? = null,
    onUseHint: (() -> Unit)? = null,
    onExit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MatiksChipButton(label = "Hub", onClick = onExit, leadingIcon = SsIcons.Back)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = SplitSignalColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (triathlonRoundIndex != null || !roomCode.isNullOrBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (triathlonRoundIndex != null) {
                            Text(
                                text = "Round ${triathlonRoundIndex + 1}/3",
                                style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                                color = SplitSignalColors.AccentAmber,
                            )
                        }
                        if (!roomCode.isNullOrBlank()) {
                            Text(
                                text = "Room $roomCode",
                                style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                                color = SplitSignalColors.AccentEmerald,
                            )
                        }
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onUseHint != null) {
                MatiksChipButton(
                    label = "$hintLabel · $hintTokens",
                    accentColor = if (hintTokens > 0) SplitSignalColors.AccentAmber else SplitSignalColors.TextMuted,
                    enabled = hintTokens > 0,
                    onClick = onUseHint,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SplitSignalColors.SurfaceCard)
                    .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = formatDuration(elapsedSeconds),
                    style = LinearTextStyles.TabularDigit.copy(fontSize = 13.sp),
                    color = SplitSignalColors.TextPrimary,
                )
            }
        }
    }
}

/**
 * Live 1v1 opponent progress bar without spoiling their board.
 */
@Composable
fun OpponentRaceBar(
    opponent: OpponentProgress,
    modifier: Modifier = Modifier,
) {
    LinearCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 12.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (opponent.finished) "Opponent finished" else "Opponent live",
                style = MaterialTheme.typography.labelMedium,
                color = if (opponent.finished) SplitSignalColors.AccentAmber else SplitSignalColors.TextSecondary,
            )
            Text(
                text = "${opponent.percent}% · ${opponent.mistakes} err",
                style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                color = SplitSignalColors.TextPrimary,
            )
        }
        LinearProgressIndicator(
            progress = { (opponent.percent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (opponent.finished) SplitSignalColors.AccentAmber else SplitSignalColors.AccentIndigo,
            trackColor = SplitSignalColors.SurfaceTile,
        )
    }
}

/**
 * 3-Stage Victory Ceremony & Post-Duel Cognitive Breakdown overlay:
 * - **Stage 1 (`0–1100ms` Anticipation Window):** Pulsing kinetic ring + `"Calculating Elo…"` (`tnum`).
 * - **Stage 2 (`1100ms` Ceremony Layer):** Radial particle burst (`Canvas`) + haptic feedback +
 *   smooth Elo counter transition (`prevElo -> newElo`) + Coins & XP roll-up (`animateIntAsState`).
 * - **Stage 3 (Afterglow):** Streak ring arc sweep (`animateFloatAsState`) +
 *   `"Copy Shareable Scorecard"` + `"Next Workout"` / `"Play Again"` / `"Hub"`.
 */
@Composable
fun CognitiveRewardOverlay(
    reward: SolveRewardBreakdown,
    progression: ProgressionState,
    isDailyWorkoutStep: Boolean,
    onPlayAgain: () -> Unit,
    onContinueWorkout: (() -> Unit)? = null,
    onExitToHub: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var copied by remember { mutableStateOf(false) }

    var stageCeremony by remember { mutableStateOf(false) }
    var stageAfterglow by remember { mutableStateOf(false) }
    val particleBurst = remember { Animatable(0f) }

    LaunchedEffect(reward) {
        stageCeremony = false
        stageAfterglow = false
        particleBurst.snapTo(0f)

        // Stage 1: 0-1100ms Anticipation Window
        delay(1100L)

        // Stage 2: Ceremony Layer + Haptic + Particle Burst
        stageCeremony = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        particleBurst.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        )

        // Stage 3: Afterglow
        stageAfterglow = true
    }

    val prevElo = (progression.elo - reward.eloDelta).coerceAtLeast(0)
    val animatedElo by animateIntAsState(
        targetValue = if (stageCeremony) progression.elo else prevElo,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
    )
    val animatedCoins by animateIntAsState(
        targetValue = if (stageCeremony) reward.totalCoins else 0,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
    )
    val animatedXp by animateIntAsState(
        targetValue = if (stageCeremony) reward.xpEarned else 0,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
    )
    val streakSweep by animateFloatAsState(
        targetValue = if (stageAfterglow) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
    )

    val pulseTransition = rememberInfiniteTransition(label = "AnticipationPulse")
    val pulseProgress by pulseTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "RingPulse",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Stage 2 Radial Particle Burst Canvas behind the ceremony card
        if (stageCeremony && particleBurst.value < 1f) {
            val burstProgress = particleBurst.value
            val particleColors = listOf(
                SplitSignalColors.AccentEmerald,
                SplitSignalColors.AccentIndigo,
                SplitSignalColors.AccentAmber,
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.minDimension * 0.48f
                val particleCount = 18
                for (i in 0 until particleCount) {
                    val angleRad = (2.0 * PI * i) / particleCount
                    val speedFactor = 0.72f + (i % 3) * 0.14f
                    val dist = maxRadius * burstProgress * speedFactor
                    val px = center.x + (cos(angleRad) * dist).toFloat()
                    val py = center.y + (sin(angleRad) * dist).toFloat()
                    val radius = (5.dp.toPx() * (1f - burstProgress * 0.65f)).coerceAtLeast(1.5f)
                    val color = particleColors[i % particleColors.size]
                        .copy(alpha = (1f - burstProgress).coerceIn(0f, 1f))
                    drawCircle(color = color, radius = radius, center = Offset(px, py))
                }
            }
        }

        LinearCard(
            modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(),
            borderColor = if (stageCeremony) {
                SplitSignalColors.AccentEmerald.copy(alpha = 0.55f)
            } else {
                SplitSignalColors.AccentIndigo.copy(alpha = 0.55f)
            },
            contentPadding = 20.dp,
        ) {
            if (!stageCeremony) {
                // Stage 1: Anticipation Window (0-1100ms)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier.size(72.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = SplitSignalColors.AccentIndigo.copy(alpha = 0.22f * pulseProgress),
                                radius = (size.minDimension / 2f) * pulseProgress,
                                style = Stroke(width = 3.dp.toPx()),
                            )
                            drawArc(
                                color = SplitSignalColors.AccentIndigo,
                                startAngle = -90f,
                                sweepAngle = 270f * pulseProgress,
                                useCenter = false,
                                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                            )
                        }
                        Text(
                            text = "$prevElo",
                            style = LinearTextStyles.TabularDigit.copy(fontSize = 14.sp),
                            color = SplitSignalColors.TextPrimary,
                        )
                    }

                    Text(
                        text = "Calculating Elo…",
                        style = LinearTextStyles.TabularDigit.copy(fontSize = 16.sp),
                        color = SplitSignalColors.TextPrimary,
                    )
                    Text(
                        text = "${reward.modeLabel} · ${reward.accuracyPercent}% Acc · ${reward.elapsedSeconds}s",
                        style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                        color = SplitSignalColors.TextSecondary,
                    )
                }
            } else {
                // Stage 2 & Stage 3: Ceremony + Afterglow
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (reward.isVictory) "SOLVE VERIFIED" else "DUEL COMPLETE",
                            style = MaterialTheme.typography.labelSmall,
                            color = SplitSignalColors.AccentEmerald,
                        )
                        Text(
                            text = reward.modeLabel,
                            style = MaterialTheme.typography.headlineSmall,
                            color = SplitSignalColors.TextPrimary,
                        )
                    }

                    // Stage 3 Streak Ring
                    Box(
                        modifier = Modifier.size(52.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = SplitSignalColors.SurfaceTile,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                            )
                            drawArc(
                                color = SplitSignalColors.AccentEmerald,
                                startAngle = -90f,
                                sweepAngle = 360f * streakSweep,
                                useCenter = false,
                                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                            )
                        }
                        Text(
                            text = "${reward.newStreakDays}d",
                            style = LinearTextStyles.TabularDigit.copy(fontSize = 12.sp),
                            color = SplitSignalColors.TextPrimary,
                        )
                    }
                }

                // Smooth Elo counter transition (prevElo -> newElo)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SplitSignalColors.SurfaceTile)
                        .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "LIVE ELO RATING",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = SplitSignalColors.TextSecondary,
                        )
                        Text(
                            text = progression.league.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = SplitSignalColors.TextPrimary,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "$animatedElo",
                            style = LinearTextStyles.TabularDigit.copy(fontSize = 22.sp),
                            color = SplitSignalColors.TextPrimary,
                        )
                        val sign = if (reward.eloDelta >= 0) "+" else ""
                        Text(
                            text = "$sign${reward.eloDelta}",
                            style = LinearTextStyles.TabularLabel.copy(fontSize = 12.sp),
                            color = if (reward.eloDelta >= 0) {
                                SplitSignalColors.AccentEmerald
                            } else {
                                SplitSignalColors.AccentCrimson
                            },
                        )
                    }
                }

                HorizontalDivider(color = SplitSignalColors.BorderSubtle)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MetricTile("ACCURACY", "${reward.accuracyPercent}%", modifier = Modifier.weight(1f))
                    MetricTile("TIME", "${reward.elapsedSeconds}s", modifier = Modifier.weight(1f))
                    MetricTile("SEC/MOVE", formatTenths(reward.avgSecondsPerMove), modifier = Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MetricTile(
                        label = "COINS",
                        value = "+$animatedCoins",
                        valueColor = SplitSignalColors.AccentAmber,
                        modifier = Modifier.weight(1f),
                    )
                    MetricTile(
                        label = "XP",
                        value = "+$animatedXp",
                        valueColor = SplitSignalColors.AccentEmerald,
                        modifier = Modifier.weight(1f),
                    )
                    val sign = if (reward.eloDelta >= 0) "+" else ""
                    MetricTile(
                        label = "ELO",
                        value = "$sign${reward.eloDelta}",
                        valueColor = if (reward.eloDelta >= 0) SplitSignalColors.AccentIndigo else SplitSignalColors.AccentCrimson,
                        modifier = Modifier.weight(1f),
                    )
                }

                Text(
                    text = "${progression.league.title} · $animatedElo Elo · Daily ${progression.dailyStepsCompleted}/4",
                    style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                    color = SplitSignalColors.TextSecondary,
                )

                // Stage 3 Afterglow Actions
                AnimatedVisibility(
                    visible = stageAfterglow,
                    enter = fadeIn(tween(240)) + expandVertically(tween(240)),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(reward.scorecardText))
                                copied = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, SplitSignalColors.BorderSubtle),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SplitSignalColors.TextPrimary),
                        ) {
                            if (copied) {
                                SsIcon(
                                    icon = SsIcons.MaterialCheck,
                                    contentDescription = null,
                                    tint = SplitSignalColors.AccentEmerald,
                                    iconSize = 14.dp,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copied Scorecard")
                            } else {
                                Text("Copy Shareable Scorecard")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = onExitToHub,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, SplitSignalColors.BorderSubtle),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SplitSignalColors.TextSecondary),
                            ) {
                                Text("Hub")
                            }
                            if (isDailyWorkoutStep && !progression.dailyCompletedToday && onContinueWorkout != null) {
                                Button(
                                    onClick = onContinueWorkout,
                                    modifier = Modifier.weight(1.5f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SplitSignalColors.AccentIndigo,
                                        contentColor = SplitSignalColors.TextPrimary,
                                    ),
                                ) {
                                    Text("Next Workout (${progression.dailyStepsCompleted}/4)", fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                Button(
                                    onClick = onPlayAgain,
                                    modifier = Modifier.weight(1.5f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SplitSignalColors.AccentIndigo,
                                        contentColor = SplitSignalColors.TextPrimary,
                                    ),
                                ) {
                                    Text("Play Again", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Lifetime Cognitive Stats & Elo League Profile screen.
 */
@Composable
fun StatsProfileScreen(
    progression: ProgressionState,
    widthSizeClass: WindowWidthSizeClass,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxContentWidth = if (widthSizeClass == WindowWidthSizeClass.Expanded) 780.dp else 540.dp
    val winRate = if (progression.totalMatches > 0) {
        (progression.duelWins * 100) / progression.totalMatches
    } else {
        0
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.widthIn(max = maxContentWidth).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MatiksChipButton(label = "Hub", onClick = onBack, leadingIcon = SsIcons.Back)
            Text(
                text = "Profile",
                style = MaterialTheme.typography.titleMedium,
                color = SplitSignalColors.TextPrimary,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = maxContentWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LinearCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = progression.league.title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = SplitSignalColors.AccentIndigo,
                )
                Text(
                    text = "${progression.elo} Elo · ${progression.rankTitle}",
                    style = LinearTextStyles.TabularDigit.copy(fontSize = 20.sp),
                    color = SplitSignalColors.TextPrimary,
                )
                Text(
                    text = "${progression.streakDays}d Streak (${progression.streakFreezes} Shield) · ${progression.coins} Coins",
                    style = LinearTextStyles.TabularLabel,
                    color = SplitSignalColors.TextSecondary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetricTile("SOLVES", "${progression.solvedCount}", modifier = Modifier.weight(1f))
                MetricTile("WINS", "${progression.duelWins} ($winRate%)", modifier = Modifier.weight(1f))
                MetricTile("FASTEST", progression.fastestSolveSeconds?.let { "${it}s" } ?: "—", modifier = Modifier.weight(1f))
                MetricTile("BEST ACC", "${progression.bestAccuracyPercent}%", modifier = Modifier.weight(1f))
            }

            LinearCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "CLEARS BY MODE",
                    style = MaterialTheme.typography.labelSmall,
                    color = SplitSignalColors.TextSecondary,
                )
                val trackedModes = listOf(
                    GameType.CROSS_MATH,
                    GameType.MATH_MAZE,
                    GameType.KENKEN,
                    GameType.SUDOKU_6X6,
                    GameType.MEMORY_MATRIX,
                    GameType.MINDSNAP,
                    GameType.TRIATHLON,
                )
                trackedModes.forEach { mode ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = mode.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SplitSignalColors.TextPrimary,
                        )
                        Text(
                            text = "${progression.modeClears[mode] ?: 0}",
                            style = LinearTextStyles.TabularDigit.copy(fontSize = 14.sp),
                            color = SplitSignalColors.AccentEmerald,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = SplitSignalColors.TextPrimary,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SplitSignalColors.SurfaceTile)
            .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = SplitSignalColors.TextSecondary,
            maxLines = 1,
        )
        Text(
            text = value,
            style = LinearTextStyles.TabularDigit.copy(fontSize = 15.sp),
            color = valueColor,
            maxLines = 1,
        )
    }
}

@Composable
fun MatiksChipButton(
    label: String,
    onClick: () -> Unit,
    accentColor: Color = SplitSignalColors.TextPrimary,
    enabled: Boolean = true,
    leadingIcon: org.jetbrains.compose.resources.DrawableResource? = null,
) {
    TactileTile(
        onClick = onClick,
        enabled = enabled,
        backgroundColor = SplitSignalColors.SurfaceCard,
        cornerRadius = 8.dp,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            if (leadingIcon != null) {
                SsIcon(
                    icon = leadingIcon,
                    contentDescription = null,
                    tint = if (enabled) accentColor else SplitSignalColors.TextMuted,
                    iconSize = 13.dp,
                )
            }
            Text(
                text = label,
                style = LinearTextStyles.TabularLabel.copy(fontSize = 12.sp),
                color = if (enabled) accentColor else SplitSignalColors.TextMuted,
                maxLines = 1,
            )
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}

private fun formatTenths(value: Float): String {
    val tenths = (value * 10f).roundToInt()
    return "${tenths / 10}.${tenths % 10}s"
}
