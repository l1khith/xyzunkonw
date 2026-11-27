package com.splitsignal.app.ui

import org.jetbrains.compose.resources.painterResource
import splitsignal.composeapp.generated.resources.Res
import splitsignal.composeapp.generated.resources.ic_chevron_right
import splitsignal.composeapp.generated.resources.ic_expand_less
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GridOn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
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
import com.splitsignal.app.ui.design.SsIcon
import com.splitsignal.app.ui.design.SsIcons
import com.splitsignal.core.domain.DiscoveredRoom
import com.splitsignal.core.domain.GameType
import com.splitsignal.core.domain.PlayerRole
import com.splitsignal.core.domain.ProgressionState
import com.splitsignal.core.domain.StreakTier
import com.splitsignal.core.domain.SudokuDifficulty
import com.splitsignal.core.domain.SudokuSize
import com.splitsignal.core.domain.TransportConnectionState
import kotlinx.coroutines.delay


private data class HubGameItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: DrawableResource,
    val tag: String,
    val eloReward: String,
)

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
    onOpenStatsProfile: () -> Unit,
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
        var expandedGameId by remember { mutableStateOf<String?>(null) }

        val animBlock1 = remember { Animatable(0f) }
        val animBlock2 = remember { Animatable(0f) }
        val animBlock3 = remember { Animatable(0f) }

        LaunchedEffect(Unit) {
            launch {
                animBlock1.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
                )
            }
            delay(80)
            launch {
                animBlock2.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
                )
            }
            delay(80)
            launch {
                animBlock3.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 800.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 1. TODAY'S DAILY WORKOUT CARD
            LinearCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = animBlock1.value
                        translationY = (1f - animBlock1.value) * 36.dp.toPx()
                    },
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
                        Box(
                            modifier = Modifier.size(52.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            val targetFraction = if (progressionState.dailyCompletedToday) {
                                1f
                            } else {
                                progressionState.dailyStepsCompleted.coerceIn(0, 4) / 4f
                            }
                            val animatedFraction by animateFloatAsState(
                                targetValue = targetFraction,
                                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                                label = "DailyProgressFraction",
                            )

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(
                                    color = Color(0xFF1F2228),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                                )
                                if (animatedFraction > 0.005f) {
                                    drawArc(
                                        color = if (progressionState.dailyCompletedToday) SplitSignalColors.AccentEmerald else SplitSignalColors.AccentIndigo,
                                        startAngle = -90f,
                                        sweepAngle = 360f * animatedFraction,
                                        useCenter = false,
                                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                                    )
                                }
                            }
                            Text(
                                text = "${progressionState.dailyStepsCompleted}/4",
                                style = LinearTextStyles.TabularDigit.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                color = if (progressionState.dailyCompletedToday) SplitSignalColors.AccentEmerald else SplitSignalColors.TextPrimary,
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "DAILY WORKOUT",
                                style = MaterialTheme.typography.labelSmall,
                                color = SplitSignalColors.AccentIndigo,
                            )
                            Text(
                                text = if (progressionState.dailyCompletedToday) "Workout Completed" else "4-Step Brain Circuit",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = (-0.2).sp,
                                ),
                                color = SplitSignalColors.TextPrimary,
                            )
                            Text(
                                text = "${progressionState.streakDays} Day Streak · +50 ELO Bonus",
                                style = LinearTextStyles.TabularLabel.copy(fontSize = 12.sp, color = SplitSignalColors.TextSecondary),
                            )
                        }
                    }
                    HubPillButton(
                        label = if (progressionState.dailyCompletedToday) "Completed" else "Start Step",
                        accentColor = SplitSignalColors.AccentIndigo,
                        isProminent = !progressionState.dailyCompletedToday,
                        onClick = onStartDailyWorkout,
                    )
                }
            }

            // 2. 1V1 LOCAL ARENA DUEL CARD
            LinearCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = animBlock2.value
                        translationY = (1f - animBlock2.value) * 36.dp.toPx()
                    },
                contentPadding = 16.dp,
            ) {
                var selectedDuelMode by remember { mutableStateOf(GameType.SUDOKU_6X6) }
                val duelModes = listOf(
                    GameType.SUDOKU_6X6,
                    GameType.CROSS_MATH,
                    GameType.KENKEN,
                    GameType.MINDSNAP,
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "1V1 LOCAL ARENA",
                            style = MaterialTheme.typography.labelSmall,
                            color = SplitSignalColors.TextSecondary,
                        )
                        Text(
                            text = "Local Wi-Fi Race",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.2).sp,
                            ),
                            color = SplitSignalColors.TextPrimary,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        duelModes.forEach { mode ->
                            HubPillButton(
                                label = when (mode) {
                                    GameType.SUDOKU_6X6 -> "Sudoku"
                                    GameType.CROSS_MATH -> "Cross Math"
                                    GameType.KENKEN -> "KenKen"
                                    else -> "Mindsnap"
                                },
                                accentColor = SplitSignalColors.AccentIndigo,
                                isProminent = selectedDuelMode == mode,
                                onClick = { selectedDuelMode = mode },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        HubPillButton(
                            label = "Host Duel",
                            accentColor = SplitSignalColors.AccentIndigo,
                            isProminent = true,
                            onClick = { onHostDuel(selectedDuelMode) },
                            modifier = Modifier.weight(1f),
                        )
                        HubPillButton(
                            label = "Join Room",
                            accentColor = SplitSignalColors.TextPrimary,
                            isProminent = false,
                            onClick = onJoinSudokuRace,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // 3. SOLO PRACTICE SECTION
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = animBlock3.value
                        translationY = (1f - animBlock3.value) * 36.dp.toPx()
                    },
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "SOLO PRACTICE",
                    style = MaterialTheme.typography.labelSmall,
                    color = SplitSignalColors.TextSecondary,
                )

            listOf(
                HubGameItem("sudoku", "SUDOKU", "Classic 9x9 logic puzzle", SsIcons.SudokuGrid, "9×9", "+25 ELO"),
                HubGameItem("cross_math", "CROSS MATH", "3x3 arithmetic crossword", SsIcons.CrossMath, "3×3", "+20 ELO"),
                HubGameItem("kenken", "KENKEN", "4x4 arithmetic cages", SsIcons.KenKen, "4×4", "+25 ELO"),
                HubGameItem("mindsnap", "MINDSNAP BLITZ", "60s rapid combo drill", SsIcons.MindSnap, "60s", "+30 ELO"),
            ).forEach { (id, title, subtitle, gameIcon, tag, eloReward) ->
                val isExpanded = expandedGameId == id
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SplitSignalColors.CardSlate)
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color(0x2EFFFFFF), Color(0x0FFFFFFF))
                            ),
                            RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedGameId = if (isExpanded) null else id },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF22252A), Color(0xFF15171A))
                                    )
                                )
                                .border(
                                    1.dp,
                                    Brush.verticalGradient(
                                        listOf(Color(0x33FFFFFF), Color(0x0DFFFFFF))
                                    ),
                                    RoundedCornerShape(10.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            SsIcon(
                                icon = gameIcon,
                                contentDescription = title,
                                tint = SplitSignalColors.TextPrimary,
                                iconSize = 20.dp,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = (-0.2).sp,
                                ),
                                color = SplitSignalColors.TextPrimary,
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = Color(0xFF8A8F98),
                                ),
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF16181C))
                                    .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    text = tag,
                                    style = LinearTextStyles.TabularLabel.copy(
                                        fontSize = 11.sp,
                                        color = Color(0xFF8A8F98),
                                    ),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF16181C))
                                    .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    text = eloReward,
                                    style = LinearTextStyles.TabularLabel.copy(
                                        fontSize = 11.sp,
                                        color = Color(0xFF5E6AD2),
                                    ),
                                )
                            }
                            Icon(
                                painter = painterResource(
                                    if (isExpanded) Res.drawable.ic_expand_less else Res.drawable.ic_chevron_right
                                ),
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = Color(0xFF62666D),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    if (isExpanded) {
                        var selectedMode by remember(id) { mutableStateOf<String?>(null) }
                        HorizontalDivider(color = SplitSignalColors.BorderHairline, thickness = 1.dp)

                        if (selectedMode == null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                HubPillButton(
                                    label = "Solo",
                                    accentColor = SplitSignalColors.TextPrimary,
                                    isProminent = false,
                                    onClick = { selectedMode = "solo" },
                                    modifier = Modifier.weight(1f),
                                )
                                HubPillButton(
                                    label = "Duo",
                                    accentColor = SplitSignalColors.AccentIndigo,
                                    isProminent = true,
                                    onClick = { selectedMode = "duo" },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                val isDuo = selectedMode == "duo"
                                val easyLabel = if (id == "sudoku") "Easy 6×6" else "Easy"
                                val hardLabel = if (id == "sudoku") "Hard 9×9" else "Hard"

                                HubPillButton(
                                    label = easyLabel,
                                    accentColor = SplitSignalColors.TextPrimary,
                                    isProminent = false,
                                    onClick = {
                                        if (isDuo) {
                                            val gameType = when (id) {
                                                "sudoku" -> GameType.SUDOKU_6X6
                                                "cross_math" -> GameType.CROSS_MATH
                                                "kenken" -> GameType.KENKEN
                                                else -> GameType.MINDSNAP
                                            }
                                            onHostDuel(gameType)
                                        } else {
                                            when (id) {
                                                "sudoku" -> onPlaySudokuSolo(SudokuSize.SPRINT_6X6, SudokuDifficulty.EASY)
                                                "cross_math" -> onPlayCrossMath()
                                                "kenken" -> onPlayKenKen()
                                                else -> onPlayMindsnap()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                                HubPillButton(
                                    label = hardLabel,
                                    accentColor = SplitSignalColors.AccentIndigo,
                                    isProminent = true,
                                    onClick = {
                                        if (isDuo) {
                                            val gameType = when (id) {
                                                "sudoku" -> GameType.SUDOKU_9X9
                                                "cross_math" -> GameType.CROSS_MATH
                                                "kenken" -> GameType.KENKEN
                                                else -> GameType.MINDSNAP
                                            }
                                            onHostDuel(gameType)
                                        } else {
                                            when (id) {
                                                "sudoku" -> onPlaySudokuSolo(SudokuSize.CLASSIC_9X9, SudokuDifficulty.HARD)
                                                "cross_math" -> onPlayCrossMath()
                                                "kenken" -> onPlayKenKen()
                                                else -> onPlayMindsnap()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                )
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
                    text = "â—",
                    fontSize = 11.sp,
                    color = SplitSignalColors.AccentEmerald,
                )
                Text(
                    text = "${room.roomCode} Â· ${room.gameType.displayName}",
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
        GameType.CROSS_MATH,
        GameType.MINDSNAP,
        GameType.MEMORY_MATRIX,
        GameType.MATH_MAZE,
        GameType.KENKEN,
        GameType.SUDOKU_6X6,
        GameType.SUDOKU_9X9,
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
                text = "Same board Â· Live progress bar",
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

    val shape = RoundedCornerShape(8.dp)
    val backgroundBrush = if (isProminent) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF6875E8),
                Color(0xFF5460D1),
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                if (isHovered) Color(0xFF282C34) else Color(0xFF202328),
                if (isHovered) Color(0xFF1E2126) else Color(0xFF15171A),
            )
        )
    }

    val borderBrush = if (isProminent) {
        Brush.verticalGradient(
            listOf(
                Color(0x80FFFFFF),
                Color(0x265460D1),
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                if (isHovered) Color(0x4DFFFFFF) else Color(0x33FFFFFF),
                Color(0x0DFFFFFF),
            )
        )
    }

    val textColor = if (isProminent) Color.White else SplitSignalColors.TextPrimary

    Box(
        modifier = modifier
            .height(34.dp)
            .clip(shape)
            .background(backgroundBrush)
            .border(1.dp, borderBrush, shape)
            .hoverable(interactionSource = interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isProminent) FontWeight.SemiBold else FontWeight.Medium,
            ),
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


