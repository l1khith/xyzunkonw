package com.splitsignal.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitsignal.app.ui.design.SsIcon
import com.splitsignal.app.ui.design.SsIcons
import com.splitsignal.core.domain.UserIdentity

private data class AccentOption(
    val label: String,
    val color: Color,
)

private val OnboardingAccentSwatches: List<AccentOption> = listOf(
    AccentOption("Indigo", SplitSignalColors.AccentIndigo),
    AccentOption("Emerald", SplitSignalColors.AccentEmerald),
    AccentOption("Amber", SplitSignalColors.AccentAmber),
)

/**
 * 4-Step IKEA Effect Onboarding overlay guiding new players through accent personalization,
 * the Goal Gradient starter coin preview, an instant-win micro-puzzle, and claiming their
 * permanent offline `KMP-XXXX` ID.
 */
@Composable
fun OnboardingOverlay(
    identity: UserIdentity,
    onCompleteOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableIntStateOf(1) }
    var selectedAccentIdx by remember { mutableIntStateOf(identity.avatarAccentIndex.coerceIn(0, 2)) }
    var puzzleChoice by remember { mutableStateOf<Int?>(null) }

    val activeAccent = OnboardingAccentSwatches[selectedAccentIdx].color
    val puzzleSolved = puzzleChoice == 84

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.84f))
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        LinearCard(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth(),
            borderColor = activeAccent.copy(alpha = 0.55f),
            contentPadding = 20.dp,
        ) {
            // Top bar: step counter + subtle Skip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "SETUP $step/4",
                        style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                        color = activeAccent,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (i in 1..4) {
                            Box(
                                modifier = Modifier
                                    .size(width = if (i == step) 16.dp else 6.dp, height = 4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (i <= step) activeAccent else SplitSignalColors.SurfaceTile,
                                    ),
                            )
                        }
                    }
                }

                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.labelMedium,
                    color = SplitSignalColors.TextMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(onClick = onCompleteOnboarding)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            HorizontalDivider(color = SplitSignalColors.BorderSubtle)

            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                label = "OnboardingStepContent",
            ) { currentStep ->
                when (currentStep) {
                    1 -> OnboardingAccentStep(
                        selectedIndex = selectedAccentIdx,
                        onSelectIndex = { selectedAccentIdx = it },
                        onNext = { step = 2 },
                    )

                    2 -> OnboardingCoinsStep(
                        accentColor = activeAccent,
                        onNext = { step = 3 },
                    )

                    3 -> OnboardingMicroPuzzleStep(
                        selectedChoice = puzzleChoice,
                        onSelectChoice = { puzzleChoice = it },
                        onNext = { step = 4 },
                    )

                    else -> OnboardingClaimIdStep(
                        userId = identity.userId.ifBlank { "KMP-0000" },
                        accentColor = activeAccent,
                        onCompleteOnboarding = onCompleteOnboarding,
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingAccentStep(
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onNext: () -> Unit,
) {
    val activeAccent = OnboardingAccentSwatches[selectedIndex].color
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "Welcome to SplitSignal. Choose your Accent Color.",
            style = MaterialTheme.typography.headlineSmall,
            color = SplitSignalColors.TextPrimary,
        )
        Text(
            text = "Personalize your arena focus ring and identity badge.",
            style = MaterialTheme.typography.bodySmall,
            color = SplitSignalColors.TextSecondary,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OnboardingAccentSwatches.forEachIndexed { idx, option ->
                val selected = idx == selectedIndex
                TactileTile(
                    onClick = { onSelectIndex(idx) },
                    backgroundColor = if (selected) {
                        option.color.copy(alpha = 0.16f)
                    } else {
                        SplitSignalColors.SurfaceTile
                    },
                    restingBorderColor = if (selected) option.color else SplitSignalColors.BorderSubtle,
                    cornerRadius = 10.dp,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(option.color)
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) SplitSignalColors.TextPrimary else SplitSignalColors.BorderSubtle,
                                    shape = CircleShape,
                                ),
                        )
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) SplitSignalColors.TextPrimary else SplitSignalColors.TextSecondary,
                        )
                    }
                }
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = activeAccent,
                contentColor = SplitSignalColors.TextPrimary,
            ),
        ) {
            Text("Continue", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun OnboardingCoinsStep(
    accentColor: Color,
    onNext: () -> Unit,
) {
    val goalSweep by animateFloatAsState(
        targetValue = 0.25f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "GoalGradientPreview",
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "You've earned 100 Starter Coins.",
            style = MaterialTheme.typography.headlineSmall,
            color = SplitSignalColors.TextPrimary,
        )
        Text(
            text = "Your Daily Loop starts 25% complete — finish 4 quick modes to lock in your streak.",
            style = MaterialTheme.typography.bodySmall,
            color = SplitSignalColors.TextSecondary,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SplitSignalColors.SurfaceTile)
                .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(12.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = SplitSignalColors.BgCanvas,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = accentColor,
                        startAngle = -90f,
                        sweepAngle = 360f * goalSweep,
                        useCenter = false,
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
                Text(
                    text = "25%",
                    style = LinearTextStyles.TabularDigit.copy(fontSize = 12.sp),
                    color = accentColor,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "+100 STARTER COINS",
                    style = LinearTextStyles.TabularLabel.copy(fontSize = 12.sp),
                    color = SplitSignalColors.AccentAmber,
                )
                Text(
                    text = "Goal Gradient head start active",
                    style = MaterialTheme.typography.bodySmall,
                    color = SplitSignalColors.TextSecondary,
                )
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = SplitSignalColors.TextPrimary,
            ),
        ) {
            Text("Continue", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun OnboardingMicroPuzzleStep(
    selectedChoice: Int?,
    onSelectChoice: (Int) -> Unit,
    onNext: () -> Unit,
) {
    val choices = listOf(78, 84, 92)
    val solved = selectedChoice == 84

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "Quick Micro-Puzzle: 14 × 6 = ?",
            style = MaterialTheme.typography.headlineSmall,
            color = SplitSignalColors.TextPrimary,
        )
        Text(
            text = "Tap the exact product to verify your cognitive baseline.",
            style = MaterialTheme.typography.bodySmall,
            color = SplitSignalColors.TextSecondary,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            choices.forEach { value ->
                val isSelected = selectedChoice == value
                val isCorrect = isSelected && value == 84
                val isWrong = isSelected && value != 84

                TactileTile(
                    onClick = { onSelectChoice(value) },
                    backgroundColor = when {
                        isCorrect -> SplitSignalColors.AccentEmerald.copy(alpha = 0.20f)
                        isWrong -> SplitSignalColors.AccentCrimson.copy(alpha = 0.16f)
                        else -> SplitSignalColors.SurfaceTile
                    },
                    restingBorderColor = when {
                        isCorrect -> SplitSignalColors.AccentEmerald
                        isWrong -> SplitSignalColors.AccentCrimson
                        else -> SplitSignalColors.BorderSubtle
                    },
                    cornerRadius = 10.dp,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "$value",
                        style = LinearTextStyles.TabularDigit.copy(fontSize = 18.sp),
                        color = when {
                            isCorrect -> SplitSignalColors.AccentEmerald
                            isWrong -> SplitSignalColors.AccentCrimson
                            else -> SplitSignalColors.TextPrimary
                        },
                        modifier = Modifier.padding(vertical = 14.dp),
                    )
                }
            }
        }

        if (solved) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SsIcon(
                    icon = SsIcons.MaterialCheckCircle,
                    contentDescription = null,
                    tint = SplitSignalColors.AccentEmerald,
                    iconSize = 14.dp,
                )
                Text(
                    text = "Instant Solve! 14 × 6 = 84",
                    style = LinearTextStyles.TabularLabel.copy(fontSize = 12.sp),
                    color = SplitSignalColors.AccentEmerald,
                )
            }
        } else if (selectedChoice != null) {
            Text(
                text = "Not quite — try 14 × 6 again.",
                style = MaterialTheme.typography.bodySmall,
                color = SplitSignalColors.AccentCrimson,
            )
        }

        Button(
            onClick = onNext,
            enabled = solved,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SplitSignalColors.AccentEmerald,
                contentColor = SplitSignalColors.BgCanvas,
                disabledContainerColor = SplitSignalColors.SurfaceTile,
                disabledContentColor = SplitSignalColors.TextMuted,
            ),
        ) {
            Text("Claim Your ID", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun OnboardingClaimIdStep(
    userId: String,
    accentColor: Color,
    onCompleteOnboarding: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "Claim your permanent ID to save your streak",
            style = MaterialTheme.typography.headlineSmall,
            color = SplitSignalColors.TextPrimary,
        )
        Text(
            text = "Stored locally on your device. Zero cloud accounts required.",
            style = MaterialTheme.typography.bodySmall,
            color = SplitSignalColors.TextSecondary,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SplitSignalColors.SurfaceTile)
                .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "PERMANENT OFFLINE ID",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = SplitSignalColors.TextSecondary,
                )
                Text(
                    text = userId,
                    style = LinearTextStyles.TabularDigit.copy(fontSize = 24.sp),
                    color = SplitSignalColors.TextPrimary,
                )
            }
        }

        Button(
            onClick = onCompleteOnboarding,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = SplitSignalColors.TextPrimary,
            ),
        ) {
            Text("Enter Arena", fontWeight = FontWeight.SemiBold)
        }
    }
}
