package com.splitsignal.app.ui

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitsignal.app.presentation.AppScreen
import com.splitsignal.app.presentation.SplitSignalAppUiState
import com.splitsignal.app.ui.design.SsIcon
import com.splitsignal.app.ui.design.SsIcons
import com.splitsignal.core.domain.GameMode
import com.splitsignal.core.domain.MathClue
import com.splitsignal.core.domain.MathGameState
import com.splitsignal.core.domain.PlayerRole
import com.splitsignal.core.domain.TransportConnectionState

/**
 * Top-level screen router rendering the minimalist mathematical deduction interface.
 */
@Composable
fun MathPuzzleScreen(
    uiState: SplitSignalAppUiState,
    widthSizeClass: WindowWidthSizeClass,
    onOpenCoopLobby: () -> Unit,
    onExitToHub: () -> Unit,
    onCellTapped: (Int) -> Unit,
    onNextLevel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState.screen) {
        AppScreen.MATH_SOLO -> SoloSplitArenaScreen(
            solverState = uiState.mathSolverState,
            clueState = uiState.mathClueState,
            widthSizeClass = widthSizeClass,
            onOpenCoopLobby = onOpenCoopLobby,
            onExitToLanding = onExitToHub,
            onCellTapped = onCellTapped,
            onNextLevel = onNextLevel,
            modifier = modifier,
        )

        AppScreen.COOP_ACTIVE -> CoopActiveScreen(
            uiState = uiState,
            onExitToLanding = onExitToHub,
            onCellTapped = onCellTapped,
            onNextLevel = onNextLevel,
            modifier = modifier,
        )

        else -> Unit
    }
}

@Composable
private fun SoloSplitArenaScreen(
    solverState: MathGameState,
    clueState: MathGameState,
    widthSizeClass: WindowWidthSizeClass,
    onOpenCoopLobby: () -> Unit,
    onExitToLanding: () -> Unit,
    onCellTapped: (Int) -> Unit,
    onNextLevel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isWide = widthSizeClass == WindowWidthSizeClass.Expanded

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SoloTopHeader(
            levelNumber = clueState.levelIndex + 1,
            onOpenCoopLobby = onOpenCoopLobby,
            onExitToLanding = onExitToLanding,
        )

        if (isWide) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 1040.dp)
                    .align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                ClueSheetCard(
                    headerLabel = "SCREEN B · CLUE SHEET",
                    clues = clueState.clues,
                    isSolved = clueState.isSolved,
                    showFooterHint = false,
                    onNextLevel = onNextLevel,
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                )

                NumberGridCard(
                    headerLabel = "SCREEN A · NUMBER GRID",
                    subtitleLabel = "Tap tile (+1)",
                    grid = solverState.grid,
                    isSolved = solverState.isSolved,
                    maxTileSize = 88.dp,
                    onCellTapped = onCellTapped,
                    onNextLevel = onNextLevel,
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ClueSheetCard(
                    headerLabel = "SCREEN B · CLUE SHEET",
                    clues = clueState.clues,
                    isSolved = clueState.isSolved,
                    showFooterHint = false,
                    onNextLevel = onNextLevel,
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxWidth(),
                )

                NumberGridCard(
                    headerLabel = "SCREEN A · NUMBER GRID",
                    subtitleLabel = "Tap tile (+1)",
                    grid = solverState.grid,
                    isSolved = solverState.isSolved,
                    maxTileSize = 62.dp,
                    onCellTapped = onCellTapped,
                    onNextLevel = onNextLevel,
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SoloTopHeader(
    levelNumber: Int,
    onOpenCoopLobby: () -> Unit,
    onExitToLanding: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SplitSignalColors.CardSlate)
            .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "SplitSignal · Level $levelNumber",
            style = MaterialTheme.typography.titleMedium,
            color = SplitSignalColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderPillButton(
                label = "2-Device",
                accentColor = SplitSignalColors.AccentEmerald,
                isProminent = true,
                onClick = onOpenCoopLobby,
            )
            HeaderPillButton(
                label = "Exit",
                accentColor = SplitSignalColors.TextSecondary,
                isProminent = false,
                onClick = onExitToLanding,
            )
        }
    }
}

@Composable
private fun CoopActiveScreen(
    uiState: SplitSignalAppUiState,
    onExitToLanding: () -> Unit,
    onCellTapped: (Int) -> Unit,
    onNextLevel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeState = uiState.activeMathState
    val isSolver = uiState.activeRole == PlayerRole.OPERATOR
    val roleSuffix = if (isSolver) "Solver" else "Clues"
    val peerConnected = when (uiState.gameMode) {
        GameMode.LAN_HOST -> uiState.connectedClients > 0
        GameMode.LAN_CLIENT -> uiState.clientConnectionState == TransportConnectionState.CONNECTED
        GameMode.SOLO_SPLIT -> true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SplitSignalColors.CardSlate)
                .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (peerConnected) SplitSignalColors.AccentEmerald else SplitSignalColors.ErrorRose,
                        ),
                )
                Text(
                    text = "Level ${activeState.levelIndex + 1} · $roleSuffix",
                    style = MaterialTheme.typography.titleMedium,
                    color = SplitSignalColors.TextPrimary,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SplitSignalColors.TileSurface)
                        .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "Code:${uiState.activeRoomCode}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = SplitSignalColors.TextSecondary,
                    )
                }
            }

            HeaderPillButton(
                label = "Exit",
                accentColor = SplitSignalColors.TextSecondary,
                isProminent = false,
                onClick = onExitToLanding,
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (isSolver) {
                NumberGridCard(
                    headerLabel = "NUMBER GRID",
                    subtitleLabel = "Tap any tile to +1",
                    grid = activeState.grid,
                    isSolved = activeState.isSolved,
                    maxTileSize = 96.dp,
                    onCellTapped = onCellTapped,
                    onNextLevel = onNextLevel,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .widthIn(max = 460.dp)
                        .fillMaxWidth(),
                )
            } else {
                ClueSheetCard(
                    headerLabel = "CLUE SHEET",
                    clues = activeState.clues,
                    isSolved = activeState.isSolved,
                    showFooterHint = true,
                    onNextLevel = onNextLevel,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .widthIn(max = 520.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ClueSheetCard(
    headerLabel: String,
    clues: List<MathClue>,
    isSolved: Boolean,
    showFooterHint: Boolean,
    onNextLevel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val satisfiedCount = clues.count { it.isSatisfied }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SplitSignalColors.CardSlate)
            .border(
                1.dp,
                if (isSolved) SplitSignalColors.AccentEmerald.copy(alpha = 0.7f) else SplitSignalColors.BorderHairline,
                RoundedCornerShape(14.dp),
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = headerLabel,
                style = MaterialTheme.typography.labelSmall,
                color = SplitSignalColors.TextSecondary,
            )
            Text(
                text = "$satisfiedCount / ${clues.size} satisfied",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = if (isSolved) SplitSignalColors.AccentEmerald else SplitSignalColors.TextSecondary,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (clue in clues) {
                ClueRow(clue = clue)
            }
        }

        if (showFooterHint) {
            HorizontalDivider(color = SplitSignalColors.BorderHairline, thickness = 1.dp)
            if (isSolved) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Solved: $satisfiedCount / ${clues.size} rules satisfied",
                        style = MaterialTheme.typography.titleSmall,
                        color = SplitSignalColors.AccentEmerald,
                    )
                    PrimaryActionButton(
                        label = "Solved · Next Puzzle",
                        onClick = onNextLevel,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Solved: $satisfiedCount / ${clues.size} rules satisfied",
                        style = MaterialTheme.typography.bodySmall,
                        color = SplitSignalColors.TextSecondary,
                    )
                    Text(
                        text = "Tell Solver what to adjust!",
                        style = MaterialTheme.typography.labelMedium,
                        color = SplitSignalColors.AccentSand,
                    )
                }
            }
        }
    }
}

@Composable
private fun ClueRow(clue: MathClue) {
    val bgColor = if (clue.isSatisfied) {
        SplitSignalColors.AccentEmerald.copy(alpha = 0.12f)
    } else {
        SplitSignalColors.BgObsidian
    }
    val borderColor = if (clue.isSatisfied) {
        SplitSignalColors.AccentEmerald.copy(alpha = 0.55f)
    } else {
        SplitSignalColors.BorderHairline
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SsIcon(
            icon = if (clue.isSatisfied) SsIcons.MaterialCheckCircle else SsIcons.MaterialRadioUnchecked,
            contentDescription = if (clue.isSatisfied) "Satisfied" else "Unsatisfied",
            tint = if (clue.isSatisfied) SplitSignalColors.AccentEmerald else SplitSignalColors.TextSecondary,
            iconSize = 15.dp,
        )

        Text(
            text = clue.statement,
            style = MaterialTheme.typography.bodyMedium,
            color = SplitSignalColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = clue.liveStatus,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = if (clue.isSatisfied) SplitSignalColors.AccentEmerald else SplitSignalColors.TextSecondary,
        )
    }
}

@Composable
private fun NumberGridCard(
    headerLabel: String,
    subtitleLabel: String,
    grid: List<Int>,
    isSolved: Boolean,
    maxTileSize: Dp,
    onCellTapped: (Int) -> Unit,
    onNextLevel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SplitSignalColors.CardSlate)
            .border(
                1.dp,
                if (isSolved) SplitSignalColors.AccentEmerald.copy(alpha = 0.7f) else SplitSignalColors.BorderHairline,
                RoundedCornerShape(14.dp),
            )
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = headerLabel,
                style = MaterialTheme.typography.labelSmall,
                color = SplitSignalColors.TextSecondary,
            )
            Text(
                text = subtitleLabel,
                style = MaterialTheme.typography.bodySmall,
                color = SplitSignalColors.TextSecondary,
            )
        }

        Column(
            modifier = Modifier.widthIn(max = maxTileSize * 3 + 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            for (row in 0 until 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    for (col in 0 until 3) {
                        val cellIndex = row * 3 + col
                        val value = grid.getOrElse(cellIndex) { 1 }
                        NumberTile(
                            value = value,
                            enabled = !isSolved,
                            isSolved = isSolved,
                            onClick = { onCellTapped(cellIndex) },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                        )
                    }
                }
            }
        }

        if (isSolved) {
            PrimaryActionButton(
                label = "Solved · Next Puzzle",
                onClick = onNextLevel,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun NumberTile(
    value: Int,
    enabled: Boolean,
    isSolved: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered = interactionSource.collectIsHoveredAsState().value
    val isFocused = interactionSource.collectIsFocusedAsState().value

    val bgColor = when {
        isSolved -> SplitSignalColors.AccentEmerald.copy(alpha = 0.14f)
        isHovered && enabled -> SplitSignalColors.TileSurface.copy(alpha = 0.9f)
        else -> SplitSignalColors.TileSurface
    }
    val borderColor = when {
        isFocused -> SplitSignalColors.AccentEmerald
        isSolved -> SplitSignalColors.AccentEmerald.copy(alpha = 0.7f)
        isHovered && enabled -> SplitSignalColors.AccentSand.copy(alpha = 0.7f)
        else -> SplitSignalColors.BorderHairline
    }
    val borderWidth = if (isFocused) 2.dp else 1.dp

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
            .hoverable(interactionSource = interactionSource, enabled = enabled)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = value.toString(),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = if (isSolved) SplitSignalColors.AccentEmerald else SplitSignalColors.TextPrimary,
        )
    }
}

@Composable
private fun HeaderPillButton(
    label: String,
    accentColor: Color,
    isProminent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered = interactionSource.collectIsHoveredAsState().value
    val isFocused = interactionSource.collectIsFocusedAsState().value

    val bgColor = when {
        isProminent -> accentColor.copy(alpha = if (isHovered) 0.24f else 0.16f)
        isHovered -> SplitSignalColors.TileSurface
        else -> SplitSignalColors.BgObsidian
    }
    val borderColor = when {
        isFocused -> SplitSignalColors.AccentEmerald
        isProminent -> accentColor.copy(alpha = 0.7f)
        isHovered -> SplitSignalColors.AccentSand.copy(alpha = 0.5f)
        else -> SplitSignalColors.BorderHairline
    }
    val textColor = when {
        isProminent -> accentColor
        isHovered -> SplitSignalColors.TextPrimary
        else -> SplitSignalColors.TextSecondary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(if (isFocused) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(8.dp))
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

@Composable
private fun PrimaryActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SplitSignalColors.AccentEmerald,
            contentColor = SplitSignalColors.BgObsidian,
            disabledContainerColor = SplitSignalColors.AccentEmerald.copy(alpha = 0.4f),
            disabledContentColor = SplitSignalColors.BgObsidian,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
        modifier = modifier.pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
        )
    }
}
