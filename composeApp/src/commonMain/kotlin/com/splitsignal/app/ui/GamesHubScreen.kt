package com.splitsignal.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitsignal.app.presentation.LinkTab
import com.splitsignal.app.presentation.SplitSignalAppUiState
import com.splitsignal.core.domain.DiscoveredRoom
import com.splitsignal.core.domain.GameType
import com.splitsignal.core.domain.PlayerRole
import com.splitsignal.core.domain.ProgressionState
import com.splitsignal.core.domain.StreakTier
import com.splitsignal.core.domain.SudokuDifficulty
import com.splitsignal.core.domain.SudokuSize
import com.splitsignal.core.domain.TransportConnectionState
import kotlinx.coroutines.delay

private val StreakAmber = Color(0xFFF59E0B)
private val CoinGold = Color(0xFFFBBF24)

/**
 * Sudoku Duel centerpiece Games Hub with Daily Sprint, 1v1 Local Wi-Fi Race,
 * Solo Practice, Vault Shop access, and secondary Co-op Games.
 */
@Composable
fun GamesHubScreen(
    progressionState: ProgressionState,
    selectedDifficulty: SudokuDifficulty,
    widthSizeClass: WindowWidthSizeClass,
    onPlayDailySprint: () -> Unit,
    onStartDailyWorkout: () -> Unit,
    onHostSudokuRace: () -> Unit,
    onJoinSudokuRace: () -> Unit,
    onHostDuel: (GameType) -> Unit,
    onPlaySudokuSolo: (SudokuSize, SudokuDifficulty) -> Unit,
    onSelectSudokuDifficulty: (SudokuDifficulty) -> Unit,
    onPlayCrossMath: () -> Unit,
    onPlayMathMaze: () -> Unit,
    onPlayKenKen: () -> Unit,
    onPlayMemoryMatrix: () -> Unit,
    onPlayMindsnap: () -> Unit,
    onOpenVaultShop: () -> Unit,
    onOpenStatsProfile: () -> Unit,
    onPlayMathSolo: () -> Unit,
    onHostMathCoop: () -> Unit,
    onPlayBattleshipAi: () -> Unit,
    onHostBattleshipCoop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isWide = widthSizeClass == WindowWidthSizeClass.Expanded

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MatiksTopStatusPill(
            progressionState = progressionState,
            onOpenStatsProfile = onOpenStatsProfile,
            onOpenVaultShop = onOpenVaultShop,
            modifier = Modifier
                .widthIn(max = 920.dp)
                .fillMaxWidth(),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 920.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DailyWorkoutHeroCard(
                progressionState = progressionState,
                onStartWorkout = onStartDailyWorkout,
                onPlayDailySudoku = onPlayDailySprint,
                modifier = Modifier.fillMaxWidth(),
            )

            LocalArenaDuelCard(
                onHostDuel = onHostDuel,
                onJoinRoom = onJoinSudokuRace,
                modifier = Modifier.fillMaxWidth(),
            )

            SixModesGridSection(
                isWide = isWide,
                onPlayCrossMath = onPlayCrossMath,
                onPlayMathMaze = onPlayMathMaze,
                onPlayKenKen = onPlayKenKen,
                onPlayMiniSudoku = { onPlaySudokuSolo(SudokuSize.SPRINT_6X6, selectedDifficulty) },
                onPlayMemoryMatrix = onPlayMemoryMatrix,
                onPlayMindsnap = onPlayMindsnap,
                modifier = Modifier.fillMaxWidth(),
            )

            SoloPracticeAndVaultCard(
                progressionState = progressionState,
                selectedDifficulty = selectedDifficulty,
                onSelectDifficulty = onSelectSudokuDifficulty,
                onPlaySolo = onPlaySudokuSolo,
                onOpenShop = onOpenVaultShop,
                modifier = Modifier.fillMaxWidth(),
            )

            OtherCoopGamesSection(
                isWide = isWide,
                onPlayMathSolo = onPlayMathSolo,
                onHostMathCoop = onHostMathCoop,
                onPlayBattleshipAi = onPlayBattleshipAi,
                onHostBattleshipCoop = onHostBattleshipCoop,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Unified 2-Device Local Link lobby supporting room hosting and UDP Wi-Fi room discovery.
 */
@Composable
fun LocalLinkScreen(
    uiState: SplitSignalAppUiState,
    widthSizeClass: WindowWidthSizeClass,
    onBackToHub: () -> Unit,
    onSelectLinkTab: (LinkTab) -> Unit,
    onSelectGameType: (GameType) -> Unit,
    onSelectHostRole: (PlayerRole) -> Unit,
    onRegenerateHostCode: () -> Unit,
    onJoinDiscoveredRoom: (DiscoveredRoom) -> Unit,
    onClientEndpointChange: (String) -> Unit,
    onClientRoomCodeChange: (String) -> Unit,
    onSelectClientRole: (PlayerRole) -> Unit,
    onConnectManual: (String, String) -> Unit,
    onDisconnectClient: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxContentWidth = if (widthSizeClass == WindowWidthSizeClass.Expanded) 720.dp else 540.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = maxContentWidth)
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HubPillButton(
                    label = "< Back",
                    accentColor = SplitSignalColors.TextPrimary,
                    isProminent = false,
                    onClick = onBackToHub,
                )
                Text(
                    text = "2-Device Local Link",
                    style = MaterialTheme.typography.titleMedium,
                    color = SplitSignalColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        SegmentedTabBar(
            leftLabel = "HOST A ROOM",
            rightLabel = "JOIN A ROOM",
            isLeftSelected = uiState.linkTab == LinkTab.HOST,
            onSelectLeft = { onSelectLinkTab(LinkTab.HOST) },
            onSelectRight = { onSelectLinkTab(LinkTab.JOIN) },
            modifier = Modifier
                .widthIn(max = maxContentWidth)
                .fillMaxWidth(),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = maxContentWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (uiState.linkTab) {
                LinkTab.HOST -> HostRoomPanel(
                    uiState = uiState,
                    onSelectGameType = onSelectGameType,
                    onSelectHostRole = onSelectHostRole,
                    onRegenerateHostCode = onRegenerateHostCode,
                    onSwitchToJoin = { onSelectLinkTab(LinkTab.JOIN) },
                )

                LinkTab.JOIN -> JoinRoomPanel(
                    uiState = uiState,
                    onJoinDiscoveredRoom = onJoinDiscoveredRoom,
                    onSelectGameType = onSelectGameType,
                    onClientEndpointChange = onClientEndpointChange,
                    onClientRoomCodeChange = onClientRoomCodeChange,
                    onSelectClientRole = onSelectClientRole,
                    onConnectManual = onConnectManual,
                    onDisconnectClient = onDisconnectClient,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SudokuStatusTopBar(
    progressionState: ProgressionState,
    onOpenVaultShop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SplitSignalColors.CardSlate)
            .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "SUDOKU DUEL",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                letterSpacing = 1.4.sp,
                color = SplitSignalColors.TextPrimary,
                maxLines = 1,
            )
            Text(
                text = "Elo ${progressionState.elo} · ${StreakTier.forDays(progressionState.streakDays).label}",
                style = MaterialTheme.typography.bodySmall,
                color = SplitSignalColors.TextSecondary,
                maxLines = 1,
            )
        }

        FlowRow(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(SplitSignalColors.BgObsidian)
                .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            StatusMetricChip(
                icon = Icons.Rounded.LocalFireDepartment,
                iconTint = StreakAmber,
                value = "${progressionState.streakDays}d",
                onClick = null,
            )
            StatusMetricChip(
                icon = Icons.Rounded.MonetizationOn,
                iconTint = CoinGold,
                value = "${progressionState.coins}",
                onClick = onOpenVaultShop,
            )
            StatusMetricChip(
                icon = Icons.Rounded.Bolt,
                iconTint = SplitSignalColors.AccentEmerald,
                value = "${progressionState.xp}",
                onClick = null,
            )
        }
    }
}

@Composable
private fun StatusMetricChip(
    icon: ImageVector,
    iconTint: Color,
    value: String,
    onClick: (() -> Unit)?,
) {
    val baseModifier = Modifier
        .clip(RoundedCornerShape(6.dp))
        .let { mod ->
            if (onClick != null) {
                mod
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(role = Role.Button, onClick = onClick)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            } else {
                mod.padding(horizontal = 2.dp, vertical = 2.dp)
            }
        }

    Row(
        modifier = baseModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = SplitSignalColors.TextPrimary,
            maxLines = 1,
        )
    }
}

@Composable
private fun DailySprintHeroCard(
    progressionState: ProgressionState,
    onPlayDailySprint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tier = StreakTier.forDays(progressionState.streakDays)
    val filledSegments = if (progressionState.dailyCompletedToday) {
        10
    } else {
        (progressionState.streakDays % 10).let { mod ->
            if (mod == 0 && progressionState.streakDays > 0) 9 else mod
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SplitSignalColors.CardSlate)
            .border(1.dp, SplitSignalColors.AccentEmerald.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "DAILY SPRINT",
                style = MaterialTheme.typography.labelSmall,
                color = SplitSignalColors.AccentEmerald,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SplitSignalColors.BgObsidian)
                    .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "${tier.label} · ${progressionState.streakFreezes} Freeze",
                    style = MaterialTheme.typography.labelSmall,
                    color = SplitSignalColors.TextSecondary,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (progressionState.dailyCompletedToday) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = SplitSignalColors.AccentEmerald,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = if (progressionState.dailyCompletedToday) {
                    "Daily Sprint Complete ✓"
                } else {
                    "Solve 1 board to keep ${progressionState.streakDays}d streak"
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (progressionState.dailyCompletedToday) {
                    SplitSignalColors.AccentEmerald
                } else {
                    SplitSignalColors.TextPrimary
                },
            )
        }

        SegmentedProgressBar(
            filledSegments = filledSegments,
            totalSegments = 10,
            modifier = Modifier.fillMaxWidth(),
        )

        HubPrimaryButton(
            label = "PLAY DAILY SPRINT (6×6)",
            onClick = onPlayDailySprint,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SegmentedProgressBar(
    filledSegments: Int,
    totalSegments: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SplitSignalColors.BgObsidian)
            .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(8.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (segment in 0 until totalSegments) {
            val isFilled = segment < filledSegments
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (isFilled) {
                            SplitSignalColors.AccentEmerald
                        } else {
                            SplitSignalColors.TileSurface
                        },
                    ),
            )
        }
    }
}

@Composable
private fun WifiRaceCard(
    onHostRace: () -> Unit,
    onJoinRace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SplitSignalColors.CardSlate)
            .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(14.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "1v1 LOCAL WI-FI RACE",
                style = MaterialTheme.typography.labelSmall,
                color = SplitSignalColors.AccentEmerald,
            )
            Icon(
                imageVector = Icons.Rounded.Wifi,
                contentDescription = null,
                tint = SplitSignalColors.AccentEmerald,
                modifier = Modifier.size(16.dp),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Same board · Live opponent progress bar",
                style = MaterialTheme.typography.titleMedium,
                color = SplitSignalColors.TextPrimary,
            )
            Text(
                text = "Auto-discovery on local Wi-Fi · Ink sabotage every 15 cells",
                style = MaterialTheme.typography.bodySmall,
                color = SplitSignalColors.TextSecondary,
            )
        }

        HorizontalDivider(color = SplitSignalColors.BorderHairline, thickness = 1.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HubPrimaryButton(
                label = "HOST RACE",
                onClick = onHostRace,
                modifier = Modifier.weight(1f),
            )
            HubSecondaryButton(
                label = "JOIN RACE",
                onClick = onJoinRace,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SoloPracticeAndVaultCard(
    progressionState: ProgressionState,
    selectedDifficulty: SudokuDifficulty,
    onSelectDifficulty: (SudokuDifficulty) -> Unit,
    onPlaySolo: (SudokuSize, SudokuDifficulty) -> Unit,
    onOpenShop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SplitSignalColors.CardSlate)
            .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(14.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "SOLO PRACTICE & VAULT",
                style = MaterialTheme.typography.labelSmall,
                color = SplitSignalColors.AccentEmerald,
            )
            Text(
                text = "${progressionState.hintTokens} Hints · ${progressionState.solvedCount} Solved",
                style = MaterialTheme.typography.bodySmall,
                color = SplitSignalColors.TextSecondary,
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (difficulty in SudokuDifficulty.entries) {
                HubPillButton(
                    label = difficulty.label,
                    accentColor = SplitSignalColors.AccentEmerald,
                    isProminent = selectedDifficulty == difficulty,
                    onClick = { onSelectDifficulty(difficulty) },
                )
            }
        }

        HorizontalDivider(color = SplitSignalColors.BorderHairline, thickness = 1.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HubSecondaryButton(
                label = "6×6 Sprint",
                onClick = { onPlaySolo(SudokuSize.SPRINT_6X6, selectedDifficulty) },
                modifier = Modifier.weight(1f),
            )
            HubSecondaryButton(
                label = "9×9 Classic",
                onClick = { onPlaySolo(SudokuSize.CLASSIC_9X9, selectedDifficulty) },
                modifier = Modifier.weight(1f),
            )
            VaultShopButton(
                onClick = onOpenShop,
                modifier = Modifier.weight(0.85f),
            )
        }
    }
}

@Composable
private fun VaultShopButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CoinGold.copy(alpha = 0.55f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = CoinGold.copy(alpha = 0.1f),
            contentColor = CoinGold,
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
    ) {
        Icon(
            imageVector = Icons.Rounded.Storefront,
            contentDescription = null,
            tint = CoinGold,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.size(5.dp))
        Text(
            text = "Shop",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = CoinGold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun OtherCoopGamesSection(
    isWide: Boolean,
    onPlayMathSolo: () -> Unit,
    onHostMathCoop: () -> Unit,
    onPlayBattleshipAi: () -> Unit,
    onHostBattleshipCoop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SplitSignalColors.CardSlate)
            .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(role = Role.Button) { expanded = !expanded }
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Other Co-op Games",
                    style = MaterialTheme.typography.titleSmall,
                    color = SplitSignalColors.TextPrimary,
                )
                Text(
                    text = "Math Deduction · Tactical Battleship",
                    style = MaterialTheme.typography.bodySmall,
                    color = SplitSignalColors.TextSecondary,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Collapse other games" else "Expand other games",
                tint = SplitSignalColors.TextSecondary,
            )
        }

        AnimatedVisibility(visible = expanded) {
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SecondaryCoopGameCard(
                        codeLabel = "CO-OP MATH DEDUCTION",
                        description = "3×3 Grid · Asymmetric Clues",
                        primaryActionLabel = "2-Device",
                        secondaryActionLabel = "Play Solo",
                        onPrimaryAction = onHostMathCoop,
                        onSecondaryAction = onPlayMathSolo,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryCoopGameCard(
                        codeLabel = "TACTICAL BATTLESHIP",
                        description = "6×6 Fleet Duel · Sonar Scans",
                        primaryActionLabel = "2-Device",
                        secondaryActionLabel = "vs. Smart AI",
                        onPrimaryAction = onHostBattleshipCoop,
                        onSecondaryAction = onPlayBattleshipAi,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryCoopGameCard(
                        codeLabel = "CO-OP MATH DEDUCTION",
                        description = "3×3 Grid · Asymmetric Clues",
                        primaryActionLabel = "2-Device",
                        secondaryActionLabel = "Play Solo",
                        onPrimaryAction = onHostMathCoop,
                        onSecondaryAction = onPlayMathSolo,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SecondaryCoopGameCard(
                        codeLabel = "TACTICAL BATTLESHIP",
                        description = "6×6 Fleet Duel · Sonar Scans",
                        primaryActionLabel = "2-Device",
                        secondaryActionLabel = "vs. Smart AI",
                        onPrimaryAction = onHostBattleshipCoop,
                        onSecondaryAction = onPlayBattleshipAi,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SecondaryCoopGameCard(
    codeLabel: String,
    description: String,
    primaryActionLabel: String,
    secondaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SplitSignalColors.BgObsidian)
            .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = codeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = SplitSignalColors.AccentSand,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = SplitSignalColors.TextSecondary,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HubSecondaryButton(
                label = secondaryActionLabel,
                onClick = onSecondaryAction,
                modifier = Modifier.weight(1f),
            )
            HubPrimaryButton(
                label = primaryActionLabel,
                onClick = onPrimaryAction,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HostRoomPanel(
    uiState: SplitSignalAppUiState,
    onSelectGameType: (GameType) -> Unit,
    onSelectHostRole: (PlayerRole) -> Unit,
    onRegenerateHostCode: () -> Unit,
    onSwitchToJoin: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    var copiedInvite by remember(uiState.hostRoomCode, uiState.hostAddress) { mutableStateOf(false) }

    LaunchedEffect(copiedInvite) {
        if (copiedInvite) {
            delay(1800)
            copiedInvite = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SplitSignalColors.CardSlate)
            .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(14.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SplitSignalColors.AccentEmerald.copy(alpha = 0.14f))
                    .border(
                        1.dp,
                        SplitSignalColors.AccentEmerald.copy(alpha = 0.55f),
                        RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = uiState.selectedGameType.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = SplitSignalColors.AccentEmerald,
                )
            }

            GameTypeToggleRow(
                selectedGameType = uiState.selectedGameType,
                onSelectGameType = onSelectGameType,
            )
        }

        if (!uiState.canHostLan) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SplitSignalColors.TileSurface)
                    .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(10.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Browser sandbox: client only",
                    style = MaterialTheme.typography.titleSmall,
                    color = SplitSignalColors.TextPrimary,
                )
                Text(
                    text = "Browsers cannot open local socket servers. Host on Desktop or Android, then join from here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SplitSignalColors.TextSecondary,
                )
                HubPrimaryButton(
                    label = "Switch to Join a Room",
                    onClick = onSwitchToJoin,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SplitSignalColors.AccentEmerald),
                )
                Text(
                    text = "Broadcasting on Local Wi-Fi...",
                    style = MaterialTheme.typography.bodySmall,
                    color = SplitSignalColors.AccentEmerald,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SplitSignalColors.BgObsidian)
                    .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "INVITE CODE: ${uiState.hostRoomCode}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            letterSpacing = 1.4.sp,
                            color = SplitSignalColors.AccentEmerald,
                        )
                        Text(
                            text = "Direct IP: ${uiState.hostAddress ?: "Starting..."}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = SplitSignalColors.TextSecondary,
                        )
                    }

                    HubPillButton(
                        label = "New Code",
                        accentColor = SplitSignalColors.TextSecondary,
                        isProminent = false,
                        onClick = onRegenerateHostCode,
                    )
                }

                HorizontalDivider(color = SplitSignalColors.BorderHairline, thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val invitePayload = uiState.hostInviteToken.ifBlank {
                        val addr = uiState.hostAddress
                        if (addr != null) "${uiState.hostRoomCode}@$addr" else ""
                    }
                    HubSecondaryButton(
                        label = if (copiedInvite) "Copied!" else "Copy Invite",
                        enabled = invitePayload.isNotEmpty(),
                        onClick = {
                            if (invitePayload.isNotEmpty()) {
                                clipboardManager.setText(AnnotatedString(invitePayload))
                                copiedInvite = true
                            }
                        },
                    )
                    Text(
                        text = "(Auto-discoverable)",
                        style = MaterialTheme.typography.bodySmall,
                        color = SplitSignalColors.TextSecondary,
                    )
                }
            }

            LinkRoleToggleRow(
                label = "Host Role",
                gameType = uiState.selectedGameType,
                selectedRole = uiState.hostRole,
                onSelectRole = onSelectHostRole,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SplitSignalColors.AccentSand.copy(alpha = 0.7f)),
                )
                Text(
                    text = "Waiting for Player 2 to tap join...",
                    style = MaterialTheme.typography.bodySmall,
                    color = SplitSignalColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun JoinRoomPanel(
    uiState: SplitSignalAppUiState,
    onJoinDiscoveredRoom: (DiscoveredRoom) -> Unit,
    onSelectGameType: (GameType) -> Unit,
    onClientEndpointChange: (String) -> Unit,
    onClientRoomCodeChange: (String) -> Unit,
    onSelectClientRole: (PlayerRole) -> Unit,
    onConnectManual: (String, String) -> Unit,
    onDisconnectClient: () -> Unit,
) {
    val isConnecting = uiState.clientConnectionState == TransportConnectionState.CONNECTING

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SplitSignalColors.CardSlate)
            .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(14.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "NEARBY ROOMS ON WI-FI (Auto-Detected)",
            style = MaterialTheme.typography.labelSmall,
            color = SplitSignalColors.AccentEmerald,
        )

        if (uiState.nearbyRooms.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SplitSignalColors.BgObsidian)
                    .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(10.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SplitSignalColors.AccentEmerald.copy(alpha = 0.7f)),
                )
                Text(
                    text = "Scanning local Wi-Fi for hosts...",
                    style = MaterialTheme.typography.bodySmall,
                    color = SplitSignalColors.TextSecondary,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (room in uiState.nearbyRooms) {
                    DiscoveredRoomCard(
                        room = room,
                        enabled = !isConnecting,
                        onJoinClick = { onJoinDiscoveredRoom(room) },
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SplitSignalColors.CardSlate)
            .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(14.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Or enter IP:Port & Room Code manually:",
            style = MaterialTheme.typography.titleSmall,
            color = SplitSignalColors.TextPrimary,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Game",
                style = MaterialTheme.typography.bodySmall,
                color = SplitSignalColors.TextSecondary,
            )
            GameTypeToggleRow(
                selectedGameType = uiState.selectedGameType,
                onSelectGameType = onSelectGameType,
            )
        }

        OutlinedTextField(
            value = uiState.clientEndpointInput,
            onValueChange = onClientEndpointChange,
            label = { Text("IP:Port or Invite Token (CODE@IP:Port)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SplitSignalColors.AccentEmerald,
                unfocusedBorderColor = SplitSignalColors.BorderHairline,
                focusedLabelColor = SplitSignalColors.AccentEmerald,
                unfocusedLabelColor = SplitSignalColors.TextSecondary,
                focusedTextColor = SplitSignalColors.TextPrimary,
                unfocusedTextColor = SplitSignalColors.TextPrimary,
                cursorColor = SplitSignalColors.AccentEmerald,
            ),
        )

        OutlinedTextField(
            value = uiState.clientRoomCodeInput,
            onValueChange = onClientRoomCodeChange,
            label = { Text("Room Code (4 characters, optional with Invite Token)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SplitSignalColors.AccentEmerald,
                unfocusedBorderColor = SplitSignalColors.BorderHairline,
                focusedLabelColor = SplitSignalColors.AccentEmerald,
                unfocusedLabelColor = SplitSignalColors.TextSecondary,
                focusedTextColor = SplitSignalColors.TextPrimary,
                unfocusedTextColor = SplitSignalColors.TextPrimary,
                cursorColor = SplitSignalColors.AccentEmerald,
            ),
        )

        LinkRoleToggleRow(
            label = "Join Role",
            gameType = uiState.selectedGameType,
            selectedRole = uiState.clientRole,
            onSelectRole = onSelectClientRole,
        )

        if (uiState.clientValidationError != null) {
            Text(
                text = uiState.clientValidationError,
                style = MaterialTheme.typography.bodySmall,
                color = SplitSignalColors.ErrorRose,
            )
        } else if (uiState.clientConnectionState == TransportConnectionState.ERROR) {
            Text(
                text = "Could not connect. Check the Host IP:Port and Room Code.",
                style = MaterialTheme.typography.bodySmall,
                color = SplitSignalColors.ErrorRose,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HubPrimaryButton(
                label = if (isConnecting) "Connecting..." else "Connect",
                enabled = !isConnecting,
                onClick = { onConnectManual(uiState.clientEndpointInput, uiState.clientRoomCodeInput) },
                modifier = Modifier.weight(1f),
            )
            if (isConnecting) {
                OutlinedButton(
                    onClick = onDisconnectClient,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SplitSignalColors.ErrorRose),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelMedium,
                        color = SplitSignalColors.ErrorRose,
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun DiscoveredRoomCard(
    room: DiscoveredRoom,
    enabled: Boolean,
    onJoinClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SplitSignalColors.BgObsidian)
            .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "●",
                    fontSize = 11.sp,
                    color = SplitSignalColors.AccentEmerald,
                )
                Text(
                    text = "${room.roomCode} · ${room.gameType.displayName}",
                    style = MaterialTheme.typography.titleSmall,
                    color = SplitSignalColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "${room.hostIp}:${room.wsPort}",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = SplitSignalColors.TextSecondary,
            )
        }

        Button(
            onClick = onJoinClick,
            enabled = enabled,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SplitSignalColors.AccentEmerald,
                contentColor = SplitSignalColors.BgObsidian,
                disabledContainerColor = SplitSignalColors.AccentEmerald.copy(alpha = 0.4f),
                disabledContentColor = SplitSignalColors.BgObsidian,
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier = Modifier.pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default),
        ) {
            Text(
                text = "JOIN",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SegmentedTabBar(
    leftLabel: String,
    rightLabel: String,
    isLeftSelected: Boolean,
    onSelectLeft: () -> Unit,
    onSelectRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SplitSignalColors.CardSlate)
            .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(10.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SegmentedTabButton(
            label = leftLabel,
            selected = isLeftSelected,
            onClick = onSelectLeft,
            modifier = Modifier.weight(1f),
        )
        SegmentedTabButton(
            label = rightLabel,
            selected = !isLeftSelected,
            onClick = onSelectRight,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SegmentedTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered = interactionSource.collectIsHoveredAsState().value

    val bgColor = when {
        selected -> SplitSignalColors.AccentEmerald.copy(alpha = 0.16f)
        isHovered -> SplitSignalColors.TileSurface
        else -> Color.Transparent
    }
    val borderColor = if (selected) {
        SplitSignalColors.AccentEmerald.copy(alpha = 0.7f)
    } else {
        Color.Transparent
    }
    val textColor = when {
        selected -> SplitSignalColors.AccentEmerald
        isHovered -> SplitSignalColors.TextPrimary
        else -> SplitSignalColors.TextSecondary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .hoverable(interactionSource = interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GameTypeToggleRow(
    selectedGameType: GameType,
    onSelectGameType: (GameType) -> Unit,
) {
    val duelModes = listOf(
        GameType.TRIATHLON,
        GameType.CROSS_MATH,
        GameType.MINDSNAP,
        GameType.MEMORY_MATRIX,
        GameType.MATH_MAZE,
        GameType.KENKEN,
        GameType.SUDOKU_6X6,
        GameType.SUDOKU_9X9,
        GameType.MATH,
        GameType.BATTLESHIP,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        duelModes.forEach { mode ->
            HubPillButton(
                label = mode.displayName,
                accentColor = SplitSignalColors.AccentEmerald,
                isProminent = selectedGameType == mode,
                onClick = { onSelectGameType(mode) },
            )
        }
    }
}

@Composable
private fun LinkRoleToggleRow(
    label: String,
    gameType: GameType,
    selectedRole: PlayerRole,
    onSelectRole: (PlayerRole) -> Unit,
) {
    if (gameType != GameType.MATH && gameType != GameType.BATTLESHIP) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SplitSignalColors.BgObsidian)
                .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "1v1 Duel Format",
                style = MaterialTheme.typography.bodySmall,
                color = SplitSignalColors.TextSecondary,
            )
            Text(
                text = "Same board · Live progress bar",
                style = MaterialTheme.typography.labelMedium,
                color = SplitSignalColors.AccentEmerald,
            )
        }
        return
    }

    val operatorLabel = if (gameType == GameType.MATH) "Number Grid" else "Player 1 (First)"
    val analystLabel = if (gameType == GameType.MATH) "Clue Sheet" else "Player 2"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = SplitSignalColors.TextSecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HubPillButton(
                label = operatorLabel,
                accentColor = SplitSignalColors.AccentEmerald,
                isProminent = selectedRole == PlayerRole.OPERATOR,
                onClick = { onSelectRole(PlayerRole.OPERATOR) },
            )
            HubPillButton(
                label = analystLabel,
                accentColor = SplitSignalColors.AccentEmerald,
                isProminent = selectedRole == PlayerRole.ANALYST,
                onClick = { onSelectRole(PlayerRole.ANALYST) },
            )
        }
    }
}

@Composable
private fun HubPillButton(
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
private fun HubPrimaryButton(
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
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        modifier = modifier.pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HubSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, SplitSignalColors.BorderHairline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SplitSignalColors.TileSurface,
            contentColor = SplitSignalColors.TextPrimary,
            disabledContentColor = SplitSignalColors.TextSecondary,
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        modifier = modifier.pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MatiksTopStatusPill(
    progressionState: ProgressionState,
    onOpenStatsProfile: () -> Unit,
    onOpenVaultShop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LinearCard(
        modifier = modifier,
        contentPadding = 14.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "SplitSignal",
                    style = MaterialTheme.typography.titleMedium,
                    color = SplitSignalColors.TextPrimary,
                )
                Text(
                    text = "${progressionState.streakDays}d Streak · ${progressionState.coins} Coins · ${progressionState.elo} Elo",
                    style = LinearTextStyles.TabularLabel.copy(fontSize = 12.sp),
                    color = SplitSignalColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MatiksChipButton(
                    label = progressionState.league.title.substringBefore(" "),
                    accentColor = SplitSignalColors.AccentIndigo,
                    onClick = onOpenStatsProfile,
                )
                MatiksChipButton(
                    label = "Vault",
                    accentColor = SplitSignalColors.AccentAmber,
                    onClick = onOpenVaultShop,
                )
            }
        }
    }
}

@Composable
private fun DailyWorkoutHeroCard(
    progressionState: ProgressionState,
    onStartWorkout: () -> Unit,
    onPlayDailySudoku: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val steps = listOf("Cross Math", "Math Maze", "KenKen", "Mindsnap")
    val completed = progressionState.dailyStepsCompleted.coerceIn(0, 4)
    val closureTarget = (0.25f + (completed / 4f) * 0.75f).coerceIn(0.25f, 1f)
    val closureSweep by animateFloatAsState(
        targetValue = closureTarget,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "GestaltClosureSweep",
    )
    val closurePercent = (closureTarget * 100f).toInt()
    val streakDays = progressionState.streakDays
    val tier1Unlocked = streakDays >= 7
    val tier2Unlocked = streakDays >= 30

    LinearCard(
        modifier = modifier,
        borderColor = SplitSignalColors.BorderFocus,
        contentPadding = 16.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Gestalt Closure Ring (25% Goal Gradient baseline -> 100%)
                Box(
                    modifier = Modifier.size(58.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = SplitSignalColors.SurfaceTile,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
                        )
                        drawArc(
                            color = if (completed >= 4) {
                                SplitSignalColors.AccentEmerald
                            } else {
                                SplitSignalColors.AccentIndigo
                            },
                            startAngle = -90f,
                            sweepAngle = 360f * closureSweep,
                            useCenter = false,
                            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                    Text(
                        text = "$closurePercent%",
                        style = LinearTextStyles.TabularDigit.copy(fontSize = 12.sp),
                        color = if (completed >= 4) SplitSignalColors.AccentEmerald else SplitSignalColors.TextPrimary,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "DAILY WORKOUT",
                        style = MaterialTheme.typography.labelSmall,
                        color = SplitSignalColors.AccentIndigo,
                    )
                    Text(
                        text = if (progressionState.dailyCompletedToday) "Daily Loop Complete ✓" else "4-Step Brain Workout ($completed/4)",
                        style = MaterialTheme.typography.headlineSmall,
                        color = SplitSignalColors.TextPrimary,
                    )
                }
            }
            MatiksChipButton(
                label = "6×6 Daily",
                accentColor = SplitSignalColors.AccentAmber,
                onClick = onPlayDailySudoku,
            )
        }

        // Diamond Tier 1 (7d) & Tier 2 (30d) streak milestone indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (tier1Unlocked) {
                            SplitSignalColors.AccentEmerald.copy(alpha = 0.14f)
                        } else {
                            SplitSignalColors.SurfaceTile
                        },
                    )
                    .border(
                        1.dp,
                        if (tier1Unlocked) SplitSignalColors.AccentEmerald.copy(alpha = 0.5f) else SplitSignalColors.BorderSubtle,
                        RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = if (tier1Unlocked) {
                        "Diamond Tier 1 (7d) ✓"
                    } else {
                        "Diamond Tier 1 (7d) · ${streakDays.coerceAtMost(7)}/7d"
                    },
                    style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                    color = if (tier1Unlocked) SplitSignalColors.AccentEmerald else SplitSignalColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (tier2Unlocked) {
                            SplitSignalColors.AccentIndigo.copy(alpha = 0.18f)
                        } else {
                            SplitSignalColors.SurfaceTile
                        },
                    )
                    .border(
                        1.dp,
                        if (tier2Unlocked) SplitSignalColors.AccentIndigo.copy(alpha = 0.6f) else SplitSignalColors.BorderSubtle,
                        RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = if (tier2Unlocked) {
                        "Tier 2 (30d) ✓"
                    } else {
                        "Tier 2 (30d) · ${streakDays.coerceAtMost(30)}/30d"
                    },
                    style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                    color = if (tier2Unlocked) SplitSignalColors.AccentIndigo else SplitSignalColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            steps.forEachIndexed { idx, label ->
                val done = idx < completed
                val current = idx == completed && !progressionState.dailyCompletedToday
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                done -> SplitSignalColors.AccentEmerald.copy(alpha = 0.16f)
                                current -> SplitSignalColors.AccentIndigo.copy(alpha = 0.16f)
                                else -> SplitSignalColors.BgCanvas
                            },
                        )
                        .border(
                            1.dp,
                            when {
                                done -> SplitSignalColors.AccentEmerald
                                current -> SplitSignalColors.AccentIndigo
                                else -> SplitSignalColors.BorderSubtle
                            },
                            RoundedCornerShape(8.dp),
                        )
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (done) "✓ $label" else label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (done) SplitSignalColors.AccentEmerald else SplitSignalColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Button(
            onClick = onStartWorkout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SplitSignalColors.AccentIndigo,
                contentColor = SplitSignalColors.TextPrimary,
            ),
        ) {
            Text(
                text = when {
                    progressionState.dailyCompletedToday -> "Replay Workout"
                    completed > 0 -> "Continue Workout (${completed + 1}/4)"
                    else -> "Start Workout"
                },
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocalArenaDuelCard(
    onHostDuel: (GameType) -> Unit,
    onJoinRoom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedDuelMode by remember { mutableStateOf(GameType.TRIATHLON) }
    val arenaModes = listOf(
        GameType.TRIATHLON,
        GameType.CROSS_MATH,
        GameType.MINDSNAP,
        GameType.MEMORY_MATRIX,
        GameType.SUDOKU_6X6,
        GameType.MATH_MAZE,
        GameType.KENKEN,
    )

    LinearCard(
        modifier = modifier,
        contentPadding = 16.dp,
    ) {
        Text(
            text = "1V1 LOCAL DUEL",
            style = MaterialTheme.typography.labelSmall,
            color = SplitSignalColors.TextSecondary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            arenaModes.forEach { mode ->
                HubPillButton(
                    label = mode.displayName,
                    accentColor = SplitSignalColors.AccentIndigo,
                    isProminent = selectedDuelMode == mode,
                    onClick = { selectedDuelMode = mode },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = { onHostDuel(selectedDuelMode) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SplitSignalColors.AccentIndigo,
                    contentColor = SplitSignalColors.TextPrimary,
                ),
            ) {
                Text("Host Duel", fontWeight = FontWeight.SemiBold)
            }
            HubSecondaryButton(
                label = "Join Nearby",
                onClick = onJoinRoom,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SixModesGridSection(
    isWide: Boolean,
    onPlayCrossMath: () -> Unit,
    onPlayMathMaze: () -> Unit,
    onPlayKenKen: () -> Unit,
    onPlayMiniSudoku: () -> Unit,
    onPlayMemoryMatrix: () -> Unit,
    onPlayMindsnap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = listOf(
        Triple("Cross Math", "3×3 arithmetic crossword", onPlayCrossMath),
        Triple("Math Maze", "Pathfind to target sum", onPlayMathMaze),
        Triple("KenKen", "4×4 arithmetic cages", onPlayKenKen),
        Triple("Mini Sudoku", "6×6 speed grid", onPlayMiniSudoku),
        Triple("Memory Matrix", "2.0s flash & recall", onPlayMemoryMatrix),
        Triple("Mindsnap Blitz", "60s rapid combo drill", onPlayMindsnap),
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "PUZZLE MODES",
            style = MaterialTheme.typography.labelSmall,
            color = SplitSignalColors.TextSecondary,
        )
        val cols = if (isWide) 3 else 2
        entries.chunked(cols).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowItems.forEach { (title, subtitle, onClick) ->
                    TactileTile(
                        onClick = onClick,
                        backgroundColor = SplitSignalColors.SurfaceCard,
                        cornerRadius = 14.dp,
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                color = SplitSignalColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = SplitSignalColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (rowItems.size < cols) {
                    repeat(cols - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
