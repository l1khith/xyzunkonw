package com.splitsignal.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitsignal.core.domain.BoardTheme
import com.splitsignal.core.domain.SolveRewardBreakdown
import com.splitsignal.core.domain.StreakTier
import com.splitsignal.core.domain.SudokuGameState
import com.splitsignal.core.domain.SudokuHint
import com.splitsignal.core.domain.SudokuSize
import kotlinx.coroutines.delay

/**
 * Color palette derived from the active [BoardTheme] for the Sudoku arena.
 */
private data class SudokuPalette(
    val screenBg: Color,
    val cardBg: Color,
    val cellBg: Color,
    val givenCellBg: Color,
    val peerHighlightBg: Color,
    val sameDigitBg: Color,
    val selectedCellBg: Color,
    val hintCellBg: Color,
    val sabotageVeil: Color,
    val thinLine: Color,
    val thickLine: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val error: Color,
    val buttonSurface: Color,
)

private fun BoardTheme.toSudokuPalette(): SudokuPalette = when (this) {
    BoardTheme.OBSIDIAN_DARK -> SudokuPalette(
        screenBg = SplitSignalColors.BgObsidian,
        cardBg = SplitSignalColors.CardSlate,
        cellBg = SplitSignalColors.TileSurface,
        givenCellBg = Color(0xFF1A1D25),
        peerHighlightBg = Color(0xFF2B303D),
        sameDigitBg = SplitSignalColors.AccentEmerald.copy(alpha = 0.14f),
        selectedCellBg = SplitSignalColors.AccentEmerald.copy(alpha = 0.28f),
        hintCellBg = Color(0xFFF59E0B).copy(alpha = 0.20f),
        sabotageVeil = Color(0xFF0B0D11),
        thinLine = Color(0xFF313644),
        thickLine = Color(0xFF8A94A6),
        cardBorder = SplitSignalColors.BorderHairline,
        textPrimary = SplitSignalColors.TextPrimary,
        textSecondary = SplitSignalColors.TextSecondary,
        accent = SplitSignalColors.AccentEmerald,
        onAccent = SplitSignalColors.BgObsidian,
        error = SplitSignalColors.ErrorRose,
        buttonSurface = SplitSignalColors.TileSurface,
    )

    BoardTheme.PAPER_IVORY -> SudokuPalette(
        screenBg = Color(0xFFF6F1E7),
        cardBg = Color(0xFFEDE5D6),
        cellBg = Color(0xFFFAF7F0),
        givenCellBg = Color(0xFFE5DCCB),
        peerHighlightBg = Color(0xFFEBE3D3),
        sameDigitBg = Color(0xFF047857).copy(alpha = 0.12f),
        selectedCellBg = Color(0xFF047857).copy(alpha = 0.24f),
        hintCellBg = Color(0xFFD97706).copy(alpha = 0.20f),
        sabotageVeil = Color(0xFF292524),
        thinLine = Color(0xFFCFC4B2),
        thickLine = Color(0xFF44403C),
        cardBorder = Color(0xFFC7BAA6),
        textPrimary = Color(0xFF1C1917),
        textSecondary = Color(0xFF57534E),
        accent = Color(0xFF047857),
        onAccent = Color(0xFFFAF7F0),
        error = Color(0xFFDC2626),
        buttonSurface = Color(0xFFE3DAC7),
    )

    BoardTheme.SOLARIZED_SLATE -> SudokuPalette(
        screenBg = Color(0xFF002B36),
        cardBg = Color(0xFF073642),
        cellBg = Color(0xFF0B414F),
        givenCellBg = Color(0xFF042D38),
        peerHighlightBg = Color(0xFF124D5C),
        sameDigitBg = Color(0xFF2AA198).copy(alpha = 0.16f),
        selectedCellBg = Color(0xFF2AA198).copy(alpha = 0.30f),
        hintCellBg = Color(0xFFB58900).copy(alpha = 0.24f),
        sabotageVeil = Color(0xFF001B22),
        thinLine = Color(0xFF1E5664),
        thickLine = Color(0xFF93A1A1),
        cardBorder = Color(0xFF1D525F),
        textPrimary = Color(0xFFFDF6E3),
        textSecondary = Color(0xFF93A1A1),
        accent = Color(0xFF2AA198),
        onAccent = Color(0xFF002B36),
        error = Color(0xFFDC322F),
        buttonSurface = Color(0xFF0E4958),
    )
}

/**
 * Interactive 6x6 Sprint and 9x9 Classic Sudoku screen supporting solo, daily sprint,
 * and 2-device LAN races with live opponent progress, logical hints, and 3-stage rewards.
 */
@Composable
fun SudokuScreen(
    state: SudokuGameState,
    theme: BoardTheme,
    hintTokens: Int,
    reward: SolveRewardBreakdown?,
    roomCode: String?,
    widthSizeClass: WindowWidthSizeClass,
    onSelectCell: (Int) -> Unit,
    onEnterDigit: (Int) -> Unit,
    onErase: () -> Unit,
    onUseHint: () -> Unit,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = remember(theme) { theme.toSudokuPalette() }
    val isWide = widthSizeClass == WindowWidthSizeClass.Expanded

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.screenBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SudokuTopBar(
                state = state,
                roomCode = roomCode,
                palette = palette,
                onExit = onExit,
                modifier = Modifier
                    .widthIn(max = 1040.dp)
                    .fillMaxWidth(),
            )

            if (state.isRace) {
                OpponentProgressBar(
                    percent = state.opponent.percent,
                    mistakes = state.opponent.mistakes,
                    connected = state.opponent.connected,
                    finished = state.opponent.finished,
                    palette = palette,
                    modifier = Modifier
                        .widthIn(max = 1040.dp)
                        .fillMaxWidth(),
                )
            }

            if (state.lockoutSecondsRemaining > 0) {
                LockoutBanner(
                    secondsRemaining = state.lockoutSecondsRemaining,
                    palette = palette,
                    modifier = Modifier
                        .widthIn(max = 1040.dp)
                        .fillMaxWidth(),
                )
            }

            if (state.sabotageSecondsRemaining > 0 && state.sabotageBoxIndex != null) {
                SabotageBanner(
                    boxIndex = state.sabotageBoxIndex!!,
                    secondsRemaining = state.sabotageSecondsRemaining,
                    palette = palette,
                    modifier = Modifier
                        .widthIn(max = 1040.dp)
                        .fillMaxWidth(),
                )
            }

            if (state.activeHint != null) {
                ActiveHintBanner(
                    hint = state.activeHint!!,
                    palette = palette,
                    modifier = Modifier
                        .widthIn(max = 1040.dp)
                        .fillMaxWidth(),
                )
            }

            if (isWide) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 1040.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1.25f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        SudokuBoardGrid(
                            state = state,
                            palette = palette,
                            maxBoardSize = 480.dp,
                            onSelectCell = onSelectCell,
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(0.75f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SudokuStatusCard(
                            state = state,
                            palette = palette,
                        )
                        SudokuNumberPad(
                            state = state,
                            hintTokens = hintTokens,
                            palette = palette,
                            onEnterDigit = onEnterDigit,
                            onErase = onErase,
                            onUseHint = onUseHint,
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 540.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SudokuBoardGrid(
                        state = state,
                        palette = palette,
                        maxBoardSize = 420.dp,
                        onSelectCell = onSelectCell,
                    )

                    SudokuStatusCard(
                        state = state,
                        palette = palette,
                    )

                    SudokuNumberPad(
                        state = state,
                        hintTokens = hintTokens,
                        palette = palette,
                        onEnterDigit = onEnterDigit,
                        onErase = onErase,
                        onUseHint = onUseHint,
                    )
                }
            }
        }

        if (reward != null) {
            VictoryCeremonyOverlay(
                reward = reward,
                size = state.size,
                elapsedSeconds = state.elapsedSeconds,
                mistakes = state.mistakes,
                isRace = state.isRace,
                opponentFinished = state.opponent.finished,
                palette = palette,
                onPlayAgain = onPlayAgain,
                onDone = onExit,
            )
        }
    }
}

@Composable
private fun SudokuTopBar(
    state: SudokuGameState,
    roomCode: String?,
    palette: SudokuPalette,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(palette.cardBg)
            .border(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = state.size.label,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(palette.buttonSurface)
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            ) {
                Text(
                    text = state.difficulty.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textSecondary,
                    maxLines = 1,
                )
            }

            if (state.isDailySprint) {
                BadgeChip(
                    label = "Daily",
                    accentColor = palette.accent,
                )
            } else if (state.isRace && !roomCode.isNullOrBlank()) {
                BadgeChip(
                    label = "Code: $roomCode",
                    accentColor = palette.accent,
                    monospace = true,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Mistakes: ${state.mistakes}",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = if (state.mistakes > 0) palette.error else palette.textSecondary,
                maxLines = 1,
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(palette.screenBg)
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = formatMmSs(state.elapsedSeconds),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = palette.textPrimary,
                )
            }

            ThemedActionChip(
                label = "Exit",
                textColor = palette.textSecondary,
                bgColor = palette.buttonSurface,
                borderColor = palette.cardBorder,
                onClick = onExit,
            )
        }
    }
}

@Composable
private fun BadgeChip(
    label: String,
    accentColor: Color,
    monospace: Boolean = false,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(accentColor.copy(alpha = 0.15f))
            .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = accentColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun OpponentProgressBar(
    percent: Int,
    mistakes: Int,
    connected: Boolean,
    finished: Boolean,
    palette: SudokuPalette,
    modifier: Modifier = Modifier,
) {
    val clampedPercent = percent.coerceIn(0, 100)
    val animatedRatio by animateFloatAsState(
        targetValue = clampedPercent / 100f,
        animationSpec = tween(durationMillis = 300),
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(palette.cardBg)
            .border(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (connected) palette.accent else palette.textSecondary),
                )
                Text(
                    text = "Opponent: $clampedPercent% · $mistakes Mistakes",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = palette.textPrimary,
                )
            }

            Text(
                text = when {
                    finished -> "FINISHED"
                    connected -> "LIVE"
                    else -> "WAITING"
                },
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (finished) palette.error else palette.accent,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(palette.screenBg),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = animatedRatio.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (finished) palette.error else palette.accent),
            )
        }
    }
}

@Composable
private fun LockoutBanner(
    secondsRemaining: Int,
    palette: SudokuPalette,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(palette.error.copy(alpha = 0.16f))
            .border(1.dp, palette.error.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Input frozen: ${secondsRemaining}s",
            style = MaterialTheme.typography.titleSmall,
            color = palette.error,
        )
        Text(
            text = "Mistake penalty active",
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
        )
    }
}

@Composable
private fun SabotageBanner(
    boxIndex: Int,
    secondsRemaining: Int,
    palette: SudokuPalette,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(palette.cardBg)
            .border(1.dp, palette.accent.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Ink Sabotage on Box ${boxIndex + 1}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
        )
        Text(
            text = "${secondsRemaining}s",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = palette.accent,
        )
    }
}

@Composable
private fun ActiveHintBanner(
    hint: SudokuHint,
    palette: SudokuPalette,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(palette.cardBg)
            .border(1.dp, palette.accent.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BadgeChip(
                label = "HINT · ${hint.technique.uppercase()}",
                accentColor = palette.accent,
            )
            Text(
                text = "Try ${hint.digit}",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = palette.accent,
            )
        }
        Text(
            text = hint.explanation,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textPrimary,
        )
    }
}

@Composable
private fun SudokuBoardGrid(
    state: SudokuGameState,
    palette: SudokuPalette,
    maxBoardSize: Dp,
    onSelectCell: (Int) -> Unit,
) {
    val dim = state.size.dim
    val boxRows = state.size.boxRows
    val boxCols = state.size.boxCols
    val boxesPerRow = dim / boxCols
    val selectedIdx = state.selectedCell
    val selectedRow = selectedIdx?.let { it / dim }
    val selectedCol = selectedIdx?.let { it % dim }
    val selectedBox = if (selectedRow != null && selectedCol != null) {
        (selectedRow / boxRows) * boxesPerRow + (selectedCol / boxCols)
    } else {
        null
    }
    val selectedDigit = selectedIdx?.let { idx ->
        state.grid.getOrNull(idx)?.takeIf { it != 0 }
    }

    BoxWithConstraints(
        modifier = Modifier
            .widthIn(max = maxBoardSize)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val boardSide = minOf(maxWidth, maxBoardSize)
        val digitFontSize = if (dim == 6) 22.sp else 18.sp

        Box(
            modifier = Modifier
                .size(boardSide)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.cellBg)
                .drawWithContent {
                    drawContent()

                    val width = size.width
                    val height = size.height
                    val cellW = width / dim
                    val cellH = height / dim
                    val thinPx = 1.dp.toPx()
                    val thickPx = 2.5.dp.toPx()

                    // Vertical grid lines
                    for (c in 1 until dim) {
                        val x = c * cellW
                        val isBoxBoundary = c % boxCols == 0
                        drawLine(
                            color = if (isBoxBoundary) palette.thickLine else palette.thinLine,
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = if (isBoxBoundary) thickPx else thinPx,
                        )
                    }

                    // Horizontal grid lines
                    for (r in 1 until dim) {
                        val y = r * cellH
                        val isBoxBoundary = r % boxRows == 0
                        drawLine(
                            color = if (isBoxBoundary) palette.thickLine else palette.thinLine,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = if (isBoxBoundary) thickPx else thinPx,
                        )
                    }
                }
                .border(2.dp, palette.thickLine, RoundedCornerShape(10.dp)),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 0 until dim) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        for (c in 0 until dim) {
                            val idx = r * dim + c
                            val boxIdx = (r / boxRows) * boxesPerRow + (c / boxCols)
                            val isGiven = state.givens.getOrElse(idx) { 0 } != 0
                            val value = state.grid.getOrElse(idx) { 0 }
                            val isSelected = selectedIdx == idx
                            val isPeer = !isSelected && (
                                r == selectedRow ||
                                    c == selectedCol ||
                                    boxIdx == selectedBox
                                )
                            val isSameDigit = !isSelected && selectedDigit != null && value == selectedDigit
                            val isHintTarget = state.activeHint?.cellIndex == idx
                            val isSabotaged = state.sabotageSecondsRemaining > 0 && state.sabotageBoxIndex == boxIdx

                            SudokuCell(
                                value = value,
                                isGiven = isGiven,
                                isSelected = isSelected,
                                isPeer = isPeer,
                                isSameDigit = isSameDigit,
                                isHintTarget = isHintTarget,
                                isSabotaged = isSabotaged,
                                enabled = !state.isSolved,
                                digitFontSize = digitFontSize,
                                palette = palette,
                                onClick = { onSelectCell(idx) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SudokuCell(
    value: Int,
    isGiven: Boolean,
    isSelected: Boolean,
    isPeer: Boolean,
    isSameDigit: Boolean,
    isHintTarget: Boolean,
    isSabotaged: Boolean,
    enabled: Boolean,
    digitFontSize: androidx.compose.ui.unit.TextUnit,
    palette: SudokuPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered = interactionSource.collectIsHoveredAsState().value
    val isFocused = interactionSource.collectIsFocusedAsState().value

    val cellBg = when {
        isSelected -> palette.selectedCellBg
        isHintTarget -> palette.hintCellBg
        isSameDigit -> palette.sameDigitBg
        isPeer -> palette.peerHighlightBg
        isGiven -> palette.givenCellBg
        isHovered && enabled -> palette.peerHighlightBg
        else -> palette.cellBg
    }

    val textColor = when {
        isGiven -> palette.textPrimary
        else -> palette.accent
    }

    Box(
        modifier = modifier
            .background(cellBg)
            .then(
                if (isSelected || isFocused) {
                    Modifier.border(1.5.dp, palette.accent)
                } else if (isHintTarget) {
                    Modifier.border(1.5.dp, palette.accent.copy(alpha = 0.7f))
                } else {
                    Modifier
                },
            )
            .hoverable(interactionSource = interactionSource)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (value != 0) {
            Text(
                text = value.toString(),
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isGiven) FontWeight.ExtraBold else FontWeight.Bold,
                fontSize = digitFontSize,
                color = textColor,
                modifier = if (isSabotaged) {
                    Modifier.blur(6.dp).alpha(0.28f)
                } else {
                    Modifier
                },
            )
        }

        if (isSabotaged) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.sabotageVeil.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "?",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = palette.textSecondary.copy(alpha = 0.65f),
                )
            }
        }
    }
}

@Composable
private fun SudokuStatusCard(
    state: SudokuGameState,
    palette: SudokuPalette,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(palette.cardBg)
            .border(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = state.statusMessage.ifBlank { "${state.size.label} · ${state.difficulty.label}" },
            style = MaterialTheme.typography.bodySmall,
            color = palette.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SudokuNumberPad(
    state: SudokuGameState,
    hintTokens: Int,
    palette: SudokuPalette,
    onEnterDigit: (Int) -> Unit,
    onErase: () -> Unit,
    onUseHint: () -> Unit,
) {
    val dim = state.size.dim
    val isFrozen = state.lockoutSecondsRemaining > 0
    val selectedIdx = state.selectedCell
    val canEditSelected = !state.isSolved &&
        !isFrozen &&
        selectedIdx != null &&
        selectedIdx in state.givens.indices &&
        state.givens[selectedIdx] == 0
    val canErase = canEditSelected && state.grid[selectedIdx!!] != 0
    val canHint = !state.isSolved && hintTokens > 0

    val digitCounts = remember(state.grid, dim) {
        IntArray(dim + 1).also { counts ->
            for (v in state.grid) {
                if (v in 1..dim) counts[v]++
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.cardBg)
            .border(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (dim == 6) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (digit in 1..6) {
                    DigitKeyButton(
                        digit = digit,
                        isCompleted = digitCounts[digit] >= dim,
                        enabled = canEditSelected,
                        palette = palette,
                        onClick = { onEnterDigit(digit) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            val rows = listOf(1..5, 6..9)
            for (range in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (digit in range) {
                        DigitKeyButton(
                            digit = digit,
                            isCompleted = digitCounts[digit] >= dim,
                            enabled = canEditSelected,
                            palette = palette,
                            onClick = { onEnterDigit(digit) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onErase,
                enabled = canErase,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, palette.cardBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = palette.buttonSurface,
                    contentColor = palette.textPrimary,
                    disabledContainerColor = palette.buttonSurface.copy(alpha = 0.45f),
                    disabledContentColor = palette.textSecondary.copy(alpha = 0.5f),
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp),
                modifier = Modifier
                    .weight(1f)
                    .pointerHoverIcon(if (canErase) PointerIcon.Hand else PointerIcon.Default),
            ) {
                Text(
                    text = "Erase",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Button(
                onClick = onUseHint,
                enabled = canHint,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = palette.onAccent,
                    disabledContainerColor = palette.buttonSurface.copy(alpha = 0.45f),
                    disabledContentColor = palette.textSecondary.copy(alpha = 0.5f),
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp),
                modifier = Modifier
                    .weight(1f)
                    .pointerHoverIcon(if (canHint) PointerIcon.Hand else PointerIcon.Default),
            ) {
                Text(
                    text = "Hint ($hintTokens)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DigitKeyButton(
    digit: Int,
    isCompleted: Boolean,
    enabled: Boolean,
    palette: SudokuPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered = interactionSource.collectIsHoveredAsState().value

    val bg = when {
        !enabled -> palette.buttonSurface.copy(alpha = 0.45f)
        isHovered -> palette.accent.copy(alpha = 0.18f)
        else -> palette.buttonSurface
    }
    val borderColor = if (isHovered && enabled) palette.accent else palette.cardBorder
    val textColor = when {
        !enabled -> palette.textSecondary.copy(alpha = 0.55f)
        isCompleted -> palette.textSecondary
        else -> palette.textPrimary
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .hoverable(interactionSource = interactionSource)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = digit.toString(),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = textColor,
        )
    }
}

@Composable
private fun VictoryCeremonyOverlay(
    reward: SolveRewardBreakdown,
    size: SudokuSize,
    elapsedSeconds: Int,
    mistakes: Int,
    isRace: Boolean,
    opponentFinished: Boolean,
    palette: SudokuPalette,
    onPlayAgain: () -> Unit,
    onDone: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    var revealStage by remember(reward) { mutableStateOf(0) }
    var copiedScorecard by remember(reward) { mutableStateOf(false) }

    LaunchedEffect(reward) {
        revealStage = 1
        delay(500L)
        revealStage = 2
        delay(500L)
        revealStage = 3
    }

    LaunchedEffect(copiedScorecard) {
        if (copiedScorecard) {
            delay(1800L)
            copiedScorecard = false
        }
    }

    val tier = StreakTier.forDays(reward.newStreakDays)
    val headline = when {
        isRace && !opponentFinished -> "RACE VICTORY"
        isRace -> "RACE COMPLETE"
        else -> "PUZZLE SOLVED"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.cardBg)
                .border(1.5.dp, palette.accent.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BadgeChip(
                label = "${size.label} · ${formatMmSs(elapsedSeconds)} · $mistakes MISTAKES",
                accentColor = palette.accent,
                monospace = true,
            )

            Text(
                text = headline,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                textAlign = TextAlign.Center,
            )

            // Stage 1 (0ms): Base Solve Coins
            AnimatedVisibility(
                visible = revealStage >= 1,
                enter = fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.94f),
            ) {
                CeremonyRewardRow(
                    label = "Stage 1 · Base Solve Reward",
                    valueText = "+${reward.baseCoins} Coins",
                    palette = palette,
                )
            }

            // Stage 2 (500ms): Speed / Flawless Multiplier
            AnimatedVisibility(
                visible = revealStage >= 2,
                enter = fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.94f),
            ) {
                val bonusCoins = reward.speedBonusCoins + reward.flawlessBonusCoins
                val subDetails = buildList {
                    if (reward.speedBonusCoins > 0) add("Speed +${reward.speedBonusCoins}")
                    if (reward.flawlessBonusCoins > 0) add("Flawless +${reward.flawlessBonusCoins}")
                }.joinToString(" · ").ifBlank { "No bonus multipliers" }

                CeremonyRewardRow(
                    label = "Stage 2 · Speed & Flawless ($subDetails)",
                    valueText = "+$bonusCoins Coins",
                    palette = palette,
                )
            }

            // Stage 3 (1000ms): Streak Ring / Tier + Shareable Scorecard
            AnimatedVisibility(
                visible = revealStage >= 3,
                enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.94f),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.screenBg)
                            .border(1.dp, palette.accent.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(palette.accent.copy(alpha = 0.16f))
                                    .border(2.5.dp, palette.accent, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "${reward.newStreakDays}d",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = palette.accent,
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "${tier.label} Streak Tier",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.textPrimary,
                                )
                                val eloSign = if (reward.eloDelta >= 0) "+" else ""
                                Text(
                                    text = "+${reward.xpEarned} XP · $eloSign${reward.eloDelta} Elo · Total +${reward.totalCoins} Coins",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary,
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.screenBg)
                            .border(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "SHAREABLE SCORECARD",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accent,
                        )
                        Text(
                            text = reward.scorecardText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = palette.textPrimary,
                        )
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(reward.scorecardText))
                                copiedScorecard = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.7f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerHoverIcon(PointerIcon.Hand),
                        ) {
                            Text(
                                text = if (copiedScorecard) "Copied Scorecard!" else "Copy Scorecard",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.accent,
                            )
                        }
                    }

                    HorizontalDivider(color = palette.cardBorder, thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = onDone,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, palette.cardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = palette.buttonSurface,
                                contentColor = palette.textPrimary,
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .pointerHoverIcon(PointerIcon.Hand),
                        ) {
                            Text(
                                text = "Done",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        Button(
                            onClick = onPlayAgain,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.accent,
                                contentColor = palette.onAccent,
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .pointerHoverIcon(PointerIcon.Hand),
                        ) {
                            Text(
                                text = "Play Again",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            if (revealStage < 3) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Calculating mastery rewards...",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun CeremonyRewardRow(
    label: String,
    valueText: String,
    palette: SudokuPalette,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(palette.screenBg)
            .border(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = valueText,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = palette.accent,
        )
    }
}

@Composable
private fun ThemedActionChip(
    label: String,
    textColor: Color,
    bgColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered = interactionSource.collectIsHoveredAsState().value

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHovered) borderColor else bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .hoverable(interactionSource = interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            maxLines = 1,
        )
    }
}

private fun formatMmSs(totalSeconds: Int): String {
    val clamped = totalSeconds.coerceAtLeast(0)
    val mins = clamped / 60
    val secs = clamped % 60
    val mm = mins.toString().padStart(2, '0')
    val ss = secs.toString().padStart(2, '0')
    return "$mm:$ss"
}
