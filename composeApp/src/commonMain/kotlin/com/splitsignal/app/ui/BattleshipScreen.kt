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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitsignal.core.domain.BattleshipEngine
import com.splitsignal.core.domain.BattleshipGameState

/**
 * Minimalist 6x6 Tactical Battleship screen supporting solo vs. AI and 2-device LAN co-op.
 */
@Composable
fun BattleshipScreen(
    state: BattleshipGameState,
    roomCode: String?,
    widthSizeClass: WindowWidthSizeClass,
    onFire: (Int) -> Unit,
    onSonar: (Int) -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sonarMode by remember { mutableStateOf(false) }
    val canUseSonar = state.isMyTurn && state.sonarChargesLeft > 0 && state.winnerRole == null
    val isSonarActive = sonarMode && canUseSonar
    val isWide = widthSizeClass == WindowWidthSizeClass.Expanded

    val modeBadgeLabel = if (state.vsAi || roomCode.isNullOrBlank()) {
        "vs. AI"
    } else {
        "Code: $roomCode"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BattleshipHeader(
            modeBadgeLabel = modeBadgeLabel,
            onExit = onExit,
        )

        BattleshipStatusStrip(
            statusMessage = state.statusMessage,
            isMyTurn = state.isMyTurn,
            isMatchOver = state.winnerRole != null,
            isVictory = state.winnerRole == state.localRole,
            sonarChargesLeft = state.sonarChargesLeft,
            sonarToggleOn = isSonarActive,
            sonarEnabled = canUseSonar,
            onToggleSonar = {
                if (canUseSonar) {
                    sonarMode = !sonarMode
                }
            },
        )

        if (state.winnerRole != null) {
            BattleshipResultBanner(
                isVictory = state.winnerRole == state.localRole,
                onRestart = {
                    sonarMode = false
                    onRestart()
                },
            )
        }

        val handleCellTap: (Int) -> Unit = { idx ->
            if (isSonarActive) {
                sonarMode = false
                onSonar(idx)
            } else {
                onFire(idx)
            }
        }

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
                TargetGridCard(
                    state = state,
                    isSonarActive = isSonarActive,
                    maxCellSize = 68.dp,
                    onCellTap = handleCellTap,
                    modifier = Modifier
                        .weight(1.15f)
                        .verticalScroll(rememberScrollState()),
                )

                FleetGridCard(
                    state = state,
                    maxCellSize = 48.dp,
                    modifier = Modifier
                        .weight(0.85f)
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
                TargetGridCard(
                    state = state,
                    isSonarActive = isSonarActive,
                    maxCellSize = 56.dp,
                    onCellTap = handleCellTap,
                    modifier = Modifier
                        .widthIn(max = 460.dp)
                        .fillMaxWidth(),
                )

                FleetGridCard(
                    state = state,
                    maxCellSize = 38.dp,
                    modifier = Modifier
                        .widthIn(max = 360.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun BattleshipHeader(
    modeBadgeLabel: String,
    onExit: () -> Unit,
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
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = "Tactical Battleship",
                style = MaterialTheme.typography.titleMedium,
                color = SplitSignalColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SplitSignalColors.TileSurface)
                    .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = modeBadgeLabel,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = SplitSignalColors.AccentSand,
                    maxLines = 1,
                )
            }
        }

        BattleshipPillButton(
            label = "Exit",
            accentColor = SplitSignalColors.TextSecondary,
            isActive = false,
            enabled = true,
            onClick = onExit,
        )
    }
}

@Composable
private fun BattleshipStatusStrip(
    statusMessage: String,
    isMyTurn: Boolean,
    isMatchOver: Boolean,
    isVictory: Boolean,
    sonarChargesLeft: Int,
    sonarToggleOn: Boolean,
    sonarEnabled: Boolean,
    onToggleSonar: () -> Unit,
) {
    val turnLabel = when {
        isMatchOver && isVictory -> "VICTORY"
        isMatchOver -> "DEFEAT"
        isMyTurn -> "YOUR TURN"
        else -> "OPPONENT TURN"
    }
    val turnColor = when {
        isMatchOver && isVictory -> SplitSignalColors.AccentEmerald
        isMatchOver -> SplitSignalColors.ErrorRose
        isMyTurn -> SplitSignalColors.AccentEmerald
        else -> SplitSignalColors.TextSecondary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SplitSignalColors.CardSlate)
            .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(turnColor.copy(alpha = 0.14f))
                    .border(1.dp, turnColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = turnLabel,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.6.sp,
                    color = turnColor,
                    maxLines = 1,
                )
            }

            BattleshipPillButton(
                label = "Sonar ($sonarChargesLeft)",
                accentColor = SplitSignalColors.AccentEmerald,
                isActive = sonarToggleOn,
                enabled = sonarEnabled,
                onClick = onToggleSonar,
            )
        }

        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = SplitSignalColors.TextPrimary,
        )
    }
}

@Composable
private fun BattleshipResultBanner(
    isVictory: Boolean,
    onRestart: () -> Unit,
) {
    val accentColor = if (isVictory) SplitSignalColors.AccentEmerald else SplitSignalColors.ErrorRose
    val bannerText = if (isVictory) "Victory — Fleet Neutralized" else "Defeat — Fleet Lost"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .border(1.dp, accentColor.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = bannerText,
            style = MaterialTheme.typography.titleMedium,
            color = accentColor,
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Button(
            onClick = onRestart,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SplitSignalColors.AccentEmerald,
                contentColor = SplitSignalColors.BgObsidian,
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
        ) {
            Text(
                text = "Play Again",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TargetGridCard(
    state: BattleshipGameState,
    isSonarActive: Boolean,
    maxCellSize: Dp,
    onCellTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalTargets = state.enemyShips.size
    val subtitle = if (isSonarActive) {
        "2×2 Sonar armed · Tap sector"
    } else {
        "${state.enemyHits.size}/$totalTargets hits"
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SplitSignalColors.CardSlate)
            .border(
                1.dp,
                if (isSonarActive) SplitSignalColors.AccentEmerald.copy(alpha = 0.65f) else SplitSignalColors.BorderHairline,
                RoundedCornerShape(14.dp),
            )
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "TARGET GRID",
                style = MaterialTheme.typography.labelSmall,
                color = SplitSignalColors.TextSecondary,
            )
            Text(
                text = subtitle,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = if (isSonarActive) SplitSignalColors.AccentEmerald else SplitSignalColors.TextSecondary,
            )
        }

        TacticalGridFrame(
            maxCellSize = maxCellSize,
            cellSpacing = 6.dp,
        ) { cellIndex, cellModifier ->
            val isHit = cellIndex in state.enemyHits
            val isMiss = cellIndex in state.enemyMisses
            val isSonarShip = !isHit && !isMiss && cellIndex in state.sonarRevealedShips
            val isSonarEmpty = !isHit && !isMiss && !isSonarShip && cellIndex in state.sonarScannedEmpty

            val canTap = state.winnerRole == null && state.isMyTurn && (isSonarActive || (!isHit && !isMiss))

            val symbol = when {
                isHit -> "✕"
                isMiss -> "·"
                isSonarShip -> "◆"
                isSonarEmpty -> "○"
                else -> ""
            }
            val symbolColor = when {
                isHit -> SplitSignalColors.ErrorRose
                isMiss -> SplitSignalColors.TextSecondary.copy(alpha = 0.7f)
                isSonarShip -> SplitSignalColors.AccentEmerald
                isSonarEmpty -> SplitSignalColors.TextSecondary.copy(alpha = 0.55f)
                else -> SplitSignalColors.TextPrimary
            }
            val baseBg = when {
                isHit -> SplitSignalColors.ErrorRose.copy(alpha = 0.14f)
                isMiss -> SplitSignalColors.BgObsidian
                isSonarShip -> SplitSignalColors.AccentEmerald.copy(alpha = 0.14f)
                isSonarEmpty -> SplitSignalColors.CardSlate
                else -> SplitSignalColors.TileSurface
            }
            val baseBorder = when {
                isHit -> SplitSignalColors.ErrorRose.copy(alpha = 0.6f)
                isSonarShip -> SplitSignalColors.AccentEmerald.copy(alpha = 0.65f)
                isSonarEmpty -> SplitSignalColors.BorderHairline.copy(alpha = 0.8f)
                isMiss -> SplitSignalColors.BorderHairline.copy(alpha = 0.6f)
                else -> SplitSignalColors.BorderHairline
            }

            TargetGridCell(
                symbol = symbol,
                symbolColor = symbolColor,
                baseBg = baseBg,
                baseBorder = baseBorder,
                isSonarActive = isSonarActive,
                enabled = canTap,
                fontSize = 18.sp,
                onClick = { onCellTap(cellIndex) },
                modifier = cellModifier,
            )
        }
    }
}

@Composable
private fun FleetGridCard(
    state: BattleshipGameState,
    maxCellSize: Dp,
    modifier: Modifier = Modifier,
) {
    val intactCount = (state.myShips - state.myHitsReceived).size
    val totalCount = state.myShips.size

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SplitSignalColors.CardSlate)
            .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "YOUR FLEET",
                style = MaterialTheme.typography.labelSmall,
                color = SplitSignalColors.TextSecondary,
            )
            Text(
                text = "$intactCount/$totalCount hull",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = if (intactCount == 0) SplitSignalColors.ErrorRose else SplitSignalColors.TextSecondary,
            )
        }

        TacticalGridFrame(
            maxCellSize = maxCellSize,
            cellSpacing = 4.dp,
        ) { cellIndex, cellModifier ->
            val hasShip = cellIndex in state.myShips
            val isHit = cellIndex in state.myHitsReceived
            val isMiss = cellIndex in state.myMissesReceived

            val bgColor = when {
                isHit -> SplitSignalColors.ErrorRose.copy(alpha = 0.18f)
                hasShip -> SplitSignalColors.AccentEmerald.copy(alpha = 0.18f)
                isMiss -> SplitSignalColors.BgObsidian
                else -> SplitSignalColors.TileSurface.copy(alpha = 0.65f)
            }
            val borderColor = when {
                isHit -> SplitSignalColors.ErrorRose.copy(alpha = 0.7f)
                hasShip -> SplitSignalColors.AccentEmerald.copy(alpha = 0.65f)
                else -> SplitSignalColors.BorderHairline
            }
            val symbol = when {
                isHit -> "✕"
                hasShip -> "■"
                isMiss -> "·"
                else -> ""
            }
            val symbolColor = when {
                isHit -> SplitSignalColors.ErrorRose
                hasShip -> SplitSignalColors.AccentEmerald
                else -> SplitSignalColors.TextSecondary.copy(alpha = 0.6f)
            }

            Box(
                modifier = cellModifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (symbol.isNotEmpty()) {
                    Text(
                        text = symbol,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = symbolColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun TacticalGridFrame(
    maxCellSize: Dp,
    cellSpacing: Dp,
    cellContent: @Composable (cellIndex: Int, modifier: Modifier) -> Unit,
) {
    val gridSide = BattleshipEngine.GRID_SIDE
    val axisWidth = 18.dp
    val maxGridWidth = maxCellSize * gridSide + cellSpacing * (gridSide - 1) + axisWidth + cellSpacing

    Column(
        modifier = Modifier.widthIn(max = maxGridWidth),
        verticalArrangement = Arrangement.spacedBy(cellSpacing),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(cellSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width(axisWidth))
            for (col in 0 until gridSide) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (col + 1).toString(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = SplitSignalColors.TextSecondary,
                    )
                }
            }
        }

        for (row in 0 until gridSide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(cellSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.width(axisWidth),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = ('A'.code + row).toChar().toString(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = SplitSignalColors.TextSecondary,
                    )
                }

                for (col in 0 until gridSide) {
                    val cellIndex = row * gridSide + col
                    cellContent(
                        cellIndex,
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TargetGridCell(
    symbol: String,
    symbolColor: Color,
    baseBg: Color,
    baseBorder: Color,
    isSonarActive: Boolean,
    enabled: Boolean,
    fontSize: TextUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered = interactionSource.collectIsHoveredAsState().value
    val isFocused = interactionSource.collectIsFocusedAsState().value

    val hoverAccent = if (isSonarActive) SplitSignalColors.AccentEmerald else SplitSignalColors.AccentSand
    val bgColor = when {
        isHovered && enabled -> hoverAccent.copy(alpha = 0.12f)
        else -> baseBg
    }
    val borderColor = when {
        isFocused && enabled -> SplitSignalColors.AccentEmerald
        isHovered && enabled -> hoverAccent.copy(alpha = 0.75f)
        else -> baseBorder
    }
    val borderWidth = if ((isFocused || isHovered) && enabled) 1.5.dp else 1.dp

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(7.dp))
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
        if (symbol.isNotEmpty()) {
            Text(
                text = symbol,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                color = symbolColor,
            )
        }
    }
}

@Composable
private fun BattleshipPillButton(
    label: String,
    accentColor: Color,
    isActive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered = interactionSource.collectIsHoveredAsState().value
    val isFocused = interactionSource.collectIsFocusedAsState().value

    val bgColor = when {
        !enabled -> SplitSignalColors.BgObsidian
        isActive -> accentColor.copy(alpha = if (isHovered) 0.28f else 0.18f)
        isHovered -> SplitSignalColors.TileSurface
        else -> SplitSignalColors.BgObsidian
    }
    val borderColor = when {
        !enabled -> SplitSignalColors.BorderHairline.copy(alpha = 0.5f)
        isFocused -> SplitSignalColors.AccentEmerald
        isActive -> accentColor.copy(alpha = 0.8f)
        isHovered -> SplitSignalColors.AccentSand.copy(alpha = 0.5f)
        else -> SplitSignalColors.BorderHairline
    }
    val textColor = when {
        !enabled -> SplitSignalColors.TextSecondary.copy(alpha = 0.45f)
        isActive -> accentColor
        isHovered -> SplitSignalColors.TextPrimary
        else -> SplitSignalColors.TextSecondary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(if (isFocused && enabled) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(8.dp))
            .hoverable(interactionSource = interactionSource, enabled = enabled)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
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
