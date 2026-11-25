package com.splitsignal.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitsignal.core.domain.CrossMathGameState
import com.splitsignal.core.domain.KenKenGameState
import com.splitsignal.core.domain.MathMazeGameState
import com.splitsignal.core.domain.MemoryMatrixGameState
import com.splitsignal.core.domain.MindsnapGameState
import com.splitsignal.core.domain.ProgressionState
import com.splitsignal.core.domain.SolveRewardBreakdown

/**
 * 1. Cross Math (3x3 & 4x4 Arithmetic Crossword) with BODMAS validation,
 * animated emerald lock-in sweep, tactile Number Bank, and mistake shake.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CrossMathScreen(
    state: CrossMathGameState,
    progression: ProgressionState,
    reward: SolveRewardBreakdown?,
    roomCode: String?,
    isTriathlon: Boolean,
    widthSizeClass: WindowWidthSizeClass,
    onSelectCell: (Int) -> Unit,
    onPlaceBankTile: (Int) -> Unit,
    onUseHint: () -> Unit,
    onPlayAgain: () -> Unit,
    onContinueWorkout: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxWidth = if (widthSizeClass == WindowWidthSizeClass.Expanded) 600.dp else 460.dp
    val dim = state.board.dim

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MatiksTopBar(
                title = "Cross Math ${state.size.label}",
                elapsedSeconds = state.elapsedSeconds,
                hintTokens = progression.hintTokens,
                roomCode = roomCode,
                triathlonRoundIndex = if (isTriathlon) state.roundIndex else null,
                onUseHint = onUseHint,
                onExit = onExit,
            )

            state.opponentProgress?.let { opp ->
                OpponentRaceBar(opponent = opp, modifier = Modifier.widthIn(max = maxWidth))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = maxWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Tap a cell, then pick a number from the bank.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SplitSignalColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )

                MistakeShakeBox(mistakeCount = state.mistakes) {
                    LinearCard(modifier = Modifier.fillMaxWidth()) {
                        for (r in 0 until dim) {
                            val rowLocked = state.rowSatisfied[r]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                for (c in 0 until dim) {
                                    val idx = r * dim + c
                                    val isGiven = state.board.givenMask[idx]
                                    val value = state.values[idx]
                                    val isSelected = state.selectedCellIndex == idx
                                    val cellLocked = rowLocked || state.colSatisfied[c]

                                    val borderColor by animateColorAsState(
                                        targetValue = when {
                                            isSelected -> SplitSignalColors.AccentIndigo
                                            cellLocked -> SplitSignalColors.AccentEmerald.copy(alpha = 0.65f)
                                            else -> SplitSignalColors.BorderSubtle
                                        },
                                        animationSpec = tween(durationMillis = 250),
                                    )

                                    TactileTile(
                                        onClick = { onSelectCell(idx) },
                                        enabled = !isGiven,
                                        backgroundColor = when {
                                            isSelected -> SplitSignalColors.AccentIndigo.copy(alpha = 0.18f)
                                            isGiven -> SplitSignalColors.BgCanvas
                                            else -> SplitSignalColors.SurfaceTile
                                        },
                                        restingBorderColor = borderColor,
                                        cornerRadius = 8.dp,
                                        modifier = Modifier.weight(1f).aspectRatio(1f),
                                    ) {
                                        Text(
                                            text = value?.toString() ?: "—",
                                            style = LinearTextStyles.TabularDigit.copy(fontSize = 18.sp),
                                            color = when {
                                                value == null -> SplitSignalColors.TextMuted
                                                isGiven -> SplitSignalColors.TextPrimary
                                                cellLocked -> SplitSignalColors.AccentEmerald
                                                else -> SplitSignalColors.TextPrimary
                                            },
                                        )
                                    }

                                    if (c < dim - 1) {
                                        Text(
                                            text = state.board.rowOps[r][c].symbol,
                                            style = LinearTextStyles.TabularDigit.copy(fontSize = 14.sp),
                                            color = SplitSignalColors.TextSecondary,
                                        )
                                    }
                                }

                                val targetBorder by animateColorAsState(
                                    targetValue = if (rowLocked) SplitSignalColors.AccentEmerald else SplitSignalColors.BorderSubtle,
                                    animationSpec = tween(durationMillis = 250),
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (rowLocked) SplitSignalColors.AccentEmerald.copy(alpha = 0.16f)
                                            else SplitSignalColors.BgCanvas,
                                        )
                                        .border(1.dp, targetBorder, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = if (rowLocked) "✓ ${state.board.rowTargets[r]}" else "= ${state.board.rowTargets[r]}",
                                        style = LinearTextStyles.TabularLabel.copy(fontSize = 12.sp),
                                        color = if (rowLocked) SplitSignalColors.AccentEmerald else SplitSignalColors.TextPrimary,
                                    )
                                }
                            }

                            if (r < dim - 1) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    for (c in 0 until dim) {
                                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = state.board.colOps[c][r].symbol,
                                                style = LinearTextStyles.TabularDigit.copy(fontSize = 13.sp),
                                                color = SplitSignalColors.TextSecondary,
                                            )
                                        }
                                        if (c < dim - 1) Spacer(modifier = Modifier.size(10.dp))
                                    }
                                    Spacer(modifier = Modifier.size(48.dp))
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            for (c in 0 until dim) {
                                val sat = state.colSatisfied[c]
                                val colBorder by animateColorAsState(
                                    targetValue = if (sat) SplitSignalColors.AccentEmerald else SplitSignalColors.BorderSubtle,
                                    animationSpec = tween(durationMillis = 250),
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (sat) SplitSignalColors.AccentEmerald.copy(alpha = 0.16f)
                                            else SplitSignalColors.BgCanvas,
                                        )
                                        .border(1.dp, colBorder, RoundedCornerShape(8.dp))
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = if (sat) "✓ ${state.board.colTargets[c]}" else "= ${state.board.colTargets[c]}",
                                        style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                                        color = if (sat) SplitSignalColors.AccentEmerald else SplitSignalColors.TextPrimary,
                                    )
                                }
                                if (c < dim - 1) Spacer(modifier = Modifier.size(10.dp))
                            }
                            Spacer(modifier = Modifier.size(48.dp))
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    state.bank.forEach { tile ->
                        val available = tile.usedInCellIndex == null
                        TactileTile(
                            onClick = { onPlaceBankTile(tile.id) },
                            enabled = available,
                            backgroundColor = if (available) SplitSignalColors.SurfaceTile else SplitSignalColors.BgCanvas,
                            restingBorderColor = if (available) SplitSignalColors.BorderFocus else SplitSignalColors.BorderSubtle,
                            modifier = Modifier.size(52.dp),
                        ) {
                            Text(
                                text = if (available) "${tile.value}" else "·",
                                style = LinearTextStyles.TabularDigit.copy(fontSize = 18.sp),
                                color = if (available) SplitSignalColors.TextPrimary else SplitSignalColors.TextMuted,
                            )
                        }
                    }
                }
            }
        }

        if (reward != null) {
            CognitiveRewardOverlay(
                reward = reward,
                progression = progression,
                isDailyWorkoutStep = state.isDailyStep,
                onPlayAgain = onPlayAgain,
                onContinueWorkout = onContinueWorkout,
                onExitToHub = onExit,
            )
        }
    }
}

/**
 * 2. Math Maze (Pathfinding & Running Total).
 */
@Composable
fun MathMazeScreen(
    state: MathMazeGameState,
    progression: ProgressionState,
    reward: SolveRewardBreakdown?,
    roomCode: String?,
    widthSizeClass: WindowWidthSizeClass,
    onTapCell: (Int) -> Unit,
    onStepBack: () -> Unit,
    onUseHint: () -> Unit,
    onPlayAgain: () -> Unit,
    onContinueWorkout: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxWidth = if (widthSizeClass == WindowWidthSizeClass.Expanded) 540.dp else 440.dp
    val dim = state.board.dimension

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MatiksTopBar(
                title = "Math Maze",
                elapsedSeconds = state.elapsedSeconds,
                hintTokens = progression.hintTokens,
                roomCode = roomCode,
                onUseHint = onUseHint,
                onExit = onExit,
            )

            state.opponentProgress?.let { opp ->
                OpponentRaceBar(opponent = opp, modifier = Modifier.widthIn(max = maxWidth))
            }

            Row(
                modifier = Modifier.widthIn(max = maxWidth).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetricTile("CURRENT", "${state.runningTotal}", valueColor = SplitSignalColors.AccentIndigo, modifier = Modifier.weight(1f))
                MetricTile("TARGET", "${state.board.targetGoal}", valueColor = SplitSignalColors.AccentEmerald, modifier = Modifier.weight(1f))
            }

            MistakeShakeBox(
                mistakeCount = state.mistakes,
                modifier = Modifier.weight(1f).widthIn(max = maxWidth).fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (r in 0 until dim) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            for (c in 0 until dim) {
                                val idx = r * dim + c
                                val cell = state.board.cells[idx]
                                val inPath = idx in state.path
                                val isHead = state.path.lastOrNull() == idx
                                val isHinted = state.hintedNextIndex == idx

                                TactileTile(
                                    onClick = { onTapCell(idx) },
                                    backgroundColor = when {
                                        isHead -> SplitSignalColors.AccentIndigo.copy(alpha = 0.25f)
                                        inPath -> SplitSignalColors.AccentEmerald.copy(alpha = 0.14f)
                                        isHinted -> SplitSignalColors.AccentAmber.copy(alpha = 0.20f)
                                        else -> SplitSignalColors.SurfaceTile
                                    },
                                    restingBorderColor = when {
                                        isHead -> SplitSignalColors.AccentIndigo
                                        isHinted -> SplitSignalColors.AccentAmber
                                        cell.isExit -> SplitSignalColors.AccentEmerald.copy(alpha = 0.7f)
                                        inPath -> SplitSignalColors.AccentEmerald.copy(alpha = 0.5f)
                                        else -> SplitSignalColors.BorderSubtle
                                    },
                                    modifier = Modifier.weight(1f).aspectRatio(1f),
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        if (cell.isStart) {
                                            Text("START", fontSize = 9.sp, color = SplitSignalColors.AccentIndigo, fontWeight = FontWeight.Bold)
                                        } else if (cell.isExit) {
                                            Text("EXIT", fontSize = 9.sp, color = SplitSignalColors.AccentEmerald, fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            text = cell.displayLabel,
                                            style = LinearTextStyles.TabularDigit.copy(fontSize = 17.sp),
                                            color = SplitSignalColors.TextPrimary,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onStepBack,
                        enabled = state.path.size > 1,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SplitSignalColors.BorderSubtle),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SplitSignalColors.TextPrimary),
                    ) {
                        Text("Step Back")
                    }
                }
            }
        }

        if (reward != null) {
            CognitiveRewardOverlay(
                reward = reward,
                progression = progression,
                isDailyWorkoutStep = state.isDailyStep,
                onPlayAgain = onPlayAgain,
                onContinueWorkout = onContinueWorkout,
                onExitToHub = onExit,
            )
        }
    }
}

/**
 * 3. KenKen / Calcudoku (4x4 & 5x5 Arithmetic Cages).
 */
@Composable
fun KenKenScreen(
    state: KenKenGameState,
    progression: ProgressionState,
    reward: SolveRewardBreakdown?,
    roomCode: String?,
    widthSizeClass: WindowWidthSizeClass,
    onSelectCell: (Int) -> Unit,
    onEnterDigit: (Int) -> Unit,
    onErase: () -> Unit,
    onUseHint: () -> Unit,
    onPlayAgain: () -> Unit,
    onContinueWorkout: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxWidth = if (widthSizeClass == WindowWidthSizeClass.Expanded) 540.dp else 440.dp
    val dim = state.board.dim

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MatiksTopBar(
                title = "KenKen ${state.size.label}",
                elapsedSeconds = state.elapsedSeconds,
                hintTokens = progression.hintTokens,
                roomCode = roomCode,
                onUseHint = onUseHint,
                onExit = onExit,
            )

            state.opponentProgress?.let { opp ->
                OpponentRaceBar(opponent = opp, modifier = Modifier.widthIn(max = maxWidth))
            }

            MistakeShakeBox(
                mistakeCount = state.mistakes,
                modifier = Modifier.weight(1f).widthIn(max = maxWidth).fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (r in 0 until dim) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            for (c in 0 until dim) {
                                val idx = r * dim + c
                                val cageId = state.board.cellToCageId[idx]
                                val cage = state.board.cages[cageId]
                                val isAnchor = cage.cellIndices.firstOrNull() == idx
                                val isSelected = state.selectedCellIndex == idx
                                val isConflict = idx in state.conflictCells
                                val valNum = state.values[idx]

                                val cageTint = when (cageId % 4) {
                                    0 -> Color(0xFF14161B)
                                    1 -> Color(0xFF121817)
                                    2 -> Color(0xFF191713)
                                    else -> Color(0xFF17141C)
                                }

                                TactileTile(
                                    onClick = { onSelectCell(idx) },
                                    backgroundColor = if (isSelected) SplitSignalColors.AccentIndigo.copy(alpha = 0.20f) else cageTint,
                                    restingBorderColor = when {
                                        isConflict -> SplitSignalColors.AccentCrimson
                                        isSelected -> SplitSignalColors.AccentIndigo
                                        else -> SplitSignalColors.BorderSubtle
                                    },
                                    cornerRadius = 8.dp,
                                    modifier = Modifier.weight(1f).aspectRatio(1f),
                                ) {
                                    Box(modifier = Modifier.fillMaxSize().padding(5.dp)) {
                                        if (isAnchor) {
                                            Text(
                                                text = cage.label,
                                                style = LinearTextStyles.TabularLabel.copy(fontSize = 10.sp),
                                                color = SplitSignalColors.AccentAmber,
                                                modifier = Modifier.align(Alignment.TopStart),
                                            )
                                        }
                                        Text(
                                            text = valNum?.toString() ?: "",
                                            style = LinearTextStyles.TabularDigit.copy(fontSize = 20.sp),
                                            color = if (isConflict) SplitSignalColors.AccentCrimson else SplitSignalColors.TextPrimary,
                                            modifier = Modifier.align(Alignment.Center),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (d in 1..dim) {
                            TactileTile(
                                onClick = { onEnterDigit(d) },
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) {
                                Text("$d", style = LinearTextStyles.TabularDigit.copy(fontSize = 16.sp))
                            }
                        }
                        OutlinedButton(
                            onClick = onErase,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                            border = BorderStroke(1.dp, SplitSignalColors.BorderSubtle),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SplitSignalColors.TextSecondary),
                        ) {
                            Text("CLR")
                        }
                    }
                }
            }
        }

        if (reward != null) {
            CognitiveRewardOverlay(
                reward = reward,
                progression = progression,
                isDailyWorkoutStep = state.isDailyStep,
                onPlayAgain = onPlayAgain,
                onContinueWorkout = onContinueWorkout,
                onExitToHub = onExit,
            )
        }
    }
}

/**
 * 5. Memory Matrix + Math Recall with staggered 3D card flip (`graphicsLayer { rotationY }`)
 * and damped mistake shake.
 */
@Composable
fun MemoryMatrixScreen(
    state: MemoryMatrixGameState,
    progression: ProgressionState,
    reward: SolveRewardBreakdown?,
    roomCode: String?,
    isTriathlon: Boolean,
    widthSizeClass: WindowWidthSizeClass,
    onTapCell: (Int) -> Unit,
    onPeekHint: () -> Unit,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxWidth = if (widthSizeClass == WindowWidthSizeClass.Expanded) 520.dp else 440.dp
    val dim = state.board.dimension
    val density = LocalDensity.current.density

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MatiksTopBar(
                title = "Memory Matrix",
                elapsedSeconds = state.elapsedSeconds,
                hintTokens = progression.hintTokens,
                hintLabel = "Peek",
                roomCode = roomCode,
                triathlonRoundIndex = if (isTriathlon) state.roundIndex else null,
                onUseHint = onPeekHint,
                onExit = onExit,
            )

            state.opponentProgress?.let { opp ->
                OpponentRaceBar(opponent = opp, modifier = Modifier.widthIn(max = maxWidth))
            }

            LinearCard(
                modifier = Modifier.widthIn(max = maxWidth).fillMaxWidth(),
                borderColor = if (state.isRevealing) SplitSignalColors.AccentAmber.copy(alpha = 0.6f) else SplitSignalColors.BorderFocus,
            ) {
                Text(
                    text = if (state.isRevealing) "Memorize the numbered tiles…" else state.board.promptText,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (state.isRevealing) SplitSignalColors.AccentAmber else SplitSignalColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            MistakeShakeBox(
                mistakeCount = state.mistakes,
                modifier = Modifier.widthIn(max = maxWidth).fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (r in 0 until dim) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            for (c in 0 until dim) {
                                val idx = r * dim + c
                                val litValue = state.board.litTileValues[idx]
                                val isFound = idx in state.selectedIndices
                                val isWrong = state.wrongFlashIndex == idx
                                val faceUp = (state.isRevealing || isFound || state.isSolved) && litValue != null

                                val rotationY by animateFloatAsState(
                                    targetValue = if (faceUp) 0f else 180f,
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        delayMillis = (r + c) * 30,
                                        easing = FastOutSlowInEasing,
                                    ),
                                )
                                val showFront = rotationY <= 90f

                                TactileTile(
                                    onClick = { onTapCell(idx) },
                                    enabled = !state.isRevealing && !isFound,
                                    backgroundColor = when {
                                        isFound -> SplitSignalColors.AccentEmerald.copy(alpha = 0.22f)
                                        showFront && litValue != null -> SplitSignalColors.AccentAmber.copy(alpha = 0.20f)
                                        isWrong -> SplitSignalColors.AccentCrimson.copy(alpha = 0.24f)
                                        else -> SplitSignalColors.SurfaceTile
                                    },
                                    restingBorderColor = when {
                                        isFound -> SplitSignalColors.AccentEmerald
                                        showFront && litValue != null -> SplitSignalColors.AccentAmber
                                        isWrong -> SplitSignalColors.AccentCrimson
                                        else -> SplitSignalColors.BorderSubtle
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .graphicsLayer {
                                            this.rotationY = rotationY
                                            cameraDistance = 12f * density
                                        },
                                ) {
                                    Text(
                                        text = if (showFront && litValue != null) "$litValue" else "",
                                        style = LinearTextStyles.TabularDigit.copy(fontSize = 20.sp),
                                        color = SplitSignalColors.TextPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (reward != null) {
            CognitiveRewardOverlay(
                reward = reward,
                progression = progression,
                isDailyWorkoutStep = state.isDailyStep,
                onPlayAgain = onPlayAgain,
                onExitToHub = onExit,
            )
        }
    }
}

/**
 * 6. Mindsnap / 60-Second Blitz Drill with tabular figures (`tnum`),
 * rising `1x -> 5x` combo badge, tactile choice buttons, and mistake shake.
 */
@Composable
fun MindsnapScreen(
    state: MindsnapGameState,
    progression: ProgressionState,
    reward: SolveRewardBreakdown?,
    roomCode: String?,
    isTriathlon: Boolean,
    widthSizeClass: WindowWidthSizeClass,
    onSubmitAnswer: (Int) -> Unit,
    onUseHint: () -> Unit,
    onPlayAgain: () -> Unit,
    onContinueWorkout: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxWidth = if (widthSizeClass == WindowWidthSizeClass.Expanded) 540.dp else 440.dp
    val question = state.currentQuestion

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MatiksTopBar(
                title = "Mindsnap Blitz",
                elapsedSeconds = state.remainingSeconds,
                hintTokens = progression.hintTokens,
                roomCode = roomCode,
                triathlonRoundIndex = if (isTriathlon) state.roundIndex else null,
                onUseHint = onUseHint,
                onExit = onExit,
            )

            state.opponentProgress?.let { opp ->
                OpponentRaceBar(opponent = opp, modifier = Modifier.widthIn(max = maxWidth))
            }

            MistakeShakeBox(
                mistakeCount = state.mistakes,
                modifier = Modifier.widthIn(max = maxWidth).fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    LinearProgressIndicator(
                        progress = { state.remainingSeconds / 60f },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                        color = if (state.remainingSeconds <= 10) SplitSignalColors.AccentCrimson else SplitSignalColors.AccentIndigo,
                        trackColor = SplitSignalColors.SurfaceTile,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MetricTile("SCORE", "${state.score}", valueColor = SplitSignalColors.AccentEmerald, modifier = Modifier.weight(1f))
                        MetricTile("COMBO", "${state.multiplier}x", valueColor = SplitSignalColors.AccentAmber, modifier = Modifier.weight(1f))
                        MetricTile("DRILL", "${(state.currentIndex + 1).coerceAtMost(state.questions.size)}/${state.questions.size}", modifier = Modifier.weight(1f))
                    }

                    if (question != null) {
                        LinearCard(modifier = Modifier.fillMaxWidth(), contentPadding = 28.dp) {
                            Text(
                                text = question.prompt,
                                style = LinearTextStyles.TabularDigit.copy(fontSize = 28.sp),
                                color = SplitSignalColors.TextPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        for (row in 0..1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                for (col in 0..1) {
                                    val choiceIdx = row * 2 + col
                                    val choiceText = question.choices.getOrNull(choiceIdx) ?: ""
                                    TactileTile(
                                        onClick = { onSubmitAnswer(choiceIdx) },
                                        restingBorderColor = SplitSignalColors.BorderFocus,
                                        modifier = Modifier.weight(1f).height(64.dp),
                                    ) {
                                        Text(
                                            text = choiceText,
                                            style = LinearTextStyles.TabularDigit.copy(fontSize = 22.sp),
                                            color = SplitSignalColors.TextPrimary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (reward != null) {
            CognitiveRewardOverlay(
                reward = reward,
                progression = progression,
                isDailyWorkoutStep = state.isDailyStep,
                onPlayAgain = onPlayAgain,
                onContinueWorkout = onContinueWorkout,
                onExitToHub = onExit,
            )
        }
    }
}
