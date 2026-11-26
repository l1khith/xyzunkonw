package com.splitsignal.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitsignal.app.platform.rememberPlatformFileBridge
import com.splitsignal.app.platform.rememberPlatformMediaBridge
import com.splitsignal.app.presentation.AppScreen
import com.splitsignal.app.presentation.LinkTab
import com.splitsignal.app.presentation.MainTab
import com.splitsignal.app.presentation.SplitSignalAppUiState
import com.splitsignal.app.presentation.SplitSignalStateHolder
import com.splitsignal.app.ui.CrossMathScreen
import com.splitsignal.app.ui.DirectChatSheet
import com.splitsignal.app.ui.GamesHubScreen
import com.splitsignal.app.ui.KenKenScreen
import com.splitsignal.app.ui.LinearCard
import com.splitsignal.app.ui.LinearTextStyles
import com.splitsignal.app.ui.LocalLinkScreen
import com.splitsignal.app.ui.MathMazeScreen
import com.splitsignal.app.ui.MathPuzzleScreen
import com.splitsignal.app.ui.MemoryMatrixScreen
import com.splitsignal.app.ui.MindsnapScreen
import com.splitsignal.app.ui.NetworkScreen
import com.splitsignal.app.ui.OnboardingOverlay
import com.splitsignal.app.ui.ProfileScreen
import com.splitsignal.app.ui.SettingsModal
import com.splitsignal.app.ui.SplitSignalColors
import com.splitsignal.app.ui.SplitSignalTheme
import com.splitsignal.app.ui.StatsProfileScreen
import com.splitsignal.app.ui.SudokuScreen
import com.splitsignal.app.ui.TactileTile
import com.splitsignal.app.ui.design.SsBottomNav
import com.splitsignal.app.ui.design.SsIcon
import com.splitsignal.app.ui.design.SsIcons
import com.splitsignal.app.ui.design.SsNavItem
import com.splitsignal.app.ui.design.SsNavRail
import org.jetbrains.compose.resources.DrawableResource
import com.splitsignal.core.domain.GameType
import com.splitsignal.core.domain.SyncPacket
import com.splitsignal.core.domain.UserIdentityStore
import org.koin.compose.getKoin
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf

private val ShellNavSurface = Color(0xFF101114)
private val ShellTopBarBg = Color(0xFF08090A)
private val ShellHairline = Color(0x14FFFFFF)
private val ShellAccentIndigo = Color(0xFF5E6AD2)
private val ShellActiveText = Color(0xFFF7F8F8)
private val ShellInactiveText = Color(0xFF8A8F98)
private val ShellStreakAmber = Color(0xFFFF9F1C)
private val ShellBadgeRed = Color(0xFFE5484D)

private sealed interface GlobalBannerItem {
    data class Invite(val invite: SyncPacket.GameInvite) : GlobalBannerItem
    data class Request(val request: SyncPacket.FriendRequest) : GlobalBannerItem
}

/**
 * Root Compose UI shell for SplitSignal, driven by [SplitSignalStateHolder].
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun SplitSignalApp(
    stateHolder: SplitSignalStateHolder? = null,
    onLaunchDualDesktopDemo: ((GameType) -> Unit)? = null,
) {
    SplitSignalTheme {
        val appScope = rememberCoroutineScope()
        val resolvedHolder = if (stateHolder != null) {
            stateHolder
        } else {
            val koin = getKoin()
            val ownedHolder = remember(koin, appScope) {
                koin.get<SplitSignalStateHolder> { parametersOf(appScope) }
            }
            DisposableEffect(ownedHolder) {
                onDispose {
                    ownedHolder.dispose()
                }
            }
            ownedHolder
        }

        val uiState by resolvedHolder.uiState.collectAsState()
        val fileBridge = rememberPlatformFileBridge()
        val mediaBridge = rememberPlatformMediaBridge()
        var localOnboardingDismissed by remember { mutableStateOf(false) }
        var networkSearchQuery by remember { mutableStateOf("") }
        var showAddFriendDialog by remember { mutableStateOf(false) }
        var showSettingsModal by remember { mutableStateOf(false) }

        val pendingInvitesCount = if (uiState.chatUiState.pendingGameInvite != null) 1 else 0
        val pendingRequestsCount = uiState.chatUiState.incomingFriendRequests.size
        val hasPendingNotifications = pendingInvitesCount + pendingRequestsCount > 0

        val activeBanner: GlobalBannerItem? = when {
            uiState.chatUiState.pendingGameInvite != null ->
                GlobalBannerItem.Invite(uiState.chatUiState.pendingGameInvite!!)
            !uiState.isChatSheetOpen && uiState.chatUiState.incomingFriendRequests.isNotEmpty() ->
                GlobalBannerItem.Request(uiState.chatUiState.incomingFriendRequests.first())
            else -> null
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = SplitSignalColors.BgObsidian,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SplitSignalColors.BgObsidian)
                    .safeDrawingPadding(),
            ) {
                val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(maxWidth, maxHeight))
                val widthSizeClass = windowSizeClass.widthSizeClass
                val isDesktop = widthSizeClass == WindowWidthSizeClass.Expanded

                AnimatedContent(
                    targetState = uiState.screen,
                    transitionSpec = {
                        (slideInVertically(animationSpec = tween(220)) { it / 16 } +
                            fadeIn(animationSpec = tween(220))) togetherWith
                            fadeOut(animationSpec = tween(150))
                    },
                    label = "ScreenTransition",
                ) { targetScreen ->
                    when (targetScreen) {
                        AppScreen.HUB -> AppShell(
                            uiState = uiState,
                            widthSizeClass = widthSizeClass,
                            isDesktop = isDesktop,
                            hasPendingNotifications = hasPendingNotifications,
                            networkSearchQuery = networkSearchQuery,
                            onNetworkSearchQueryChange = { networkSearchQuery = it },
                            onOpenAddFriend = { showAddFriendDialog = true },
                            onOpenSettings = { showSettingsModal = true },
                            onNotificationClick = { resolvedHolder.selectMainTab(MainTab.NETWORK) },
                            onSelectTab = resolvedHolder::selectMainTab,
                            holder = resolvedHolder,
                            onPickFileForPeer = { peer ->
                                resolvedHolder.openPeerChat(peer)
                                fileBridge.pickFile { picked ->
                                    if (picked != null) {
                                        resolvedHolder.offerFileToActivePeer(picked)
                                    }
                                }
                            },
                            onLaunchDualDesktopDemo = onLaunchDualDesktopDemo,
                        )

                        AppScreen.LOCAL_LINK -> LocalLinkScreen(
                            uiState = uiState,
                            widthSizeClass = widthSizeClass,
                            onBackToHub = resolvedHolder::openHub,
                            onSelectLinkTab = resolvedHolder::selectLinkTab,
                            onSelectGameType = resolvedHolder::selectGameType,
                            onSelectHostRole = resolvedHolder::selectHostRole,
                            onRegenerateHostCode = resolvedHolder::regenerateHostCode,
                            onJoinDiscoveredRoom = resolvedHolder::joinDiscoveredRoom,
                            onClientEndpointChange = resolvedHolder::updateClientEndpoint,
                            onClientRoomCodeChange = resolvedHolder::updateClientRoomCode,
                            onSelectClientRole = resolvedHolder::selectClientRole,
                            onConnectManual = resolvedHolder::connectManual,
                            onDisconnectClient = resolvedHolder::disconnectClient,
                        )

                        AppScreen.MATH_SOLO -> MathPuzzleRoute(
                            uiState = uiState,
                            widthSizeClass = widthSizeClass,
                            holder = resolvedHolder,
                        )

                        AppScreen.SUDOKU_SOLO -> SudokuScreen(
                            state = uiState.sudokuState,
                            theme = uiState.progressionState.activeTheme,
                            hintTokens = uiState.progressionState.hintTokens,
                            reward = uiState.lastSolveReward,
                            roomCode = null,
                            widthSizeClass = widthSizeClass,
                            onSelectCell = resolvedHolder::selectSudokuCell,
                            onEnterDigit = resolvedHolder::enterSudokuDigit,
                            onErase = resolvedHolder::eraseSudokuCell,
                            onUseHint = resolvedHolder::useSudokuHint,
                            onPlayAgain = resolvedHolder::restartSudoku,
                            onExit = resolvedHolder::openHub,
                        )

                        AppScreen.SUDOKU_RACE -> SudokuScreen(
                            state = uiState.sudokuState,
                            theme = uiState.progressionState.activeTheme,
                            hintTokens = uiState.progressionState.hintTokens,
                            reward = uiState.lastSolveReward,
                            roomCode = uiState.activeRoomCode,
                            widthSizeClass = widthSizeClass,
                            onSelectCell = resolvedHolder::selectSudokuCell,
                            onEnterDigit = resolvedHolder::enterSudokuDigit,
                            onErase = resolvedHolder::eraseSudokuCell,
                            onUseHint = resolvedHolder::useSudokuHint,
                            onPlayAgain = resolvedHolder::restartSudoku,
                            onExit = resolvedHolder::openHub,
                        )

                        AppScreen.CROSS_MATH -> CrossMathScreen(
                            state = uiState.crossMathState,
                            progression = uiState.progressionState,
                            reward = uiState.lastSolveReward,
                            roomCode = if (uiState.crossMathState.isRace) uiState.activeRoomCode else null,
                            widthSizeClass = widthSizeClass,
                            onSelectCell = resolvedHolder::selectCrossMathCell,
                            onPlaceBankTile = resolvedHolder::placeCrossMathBankTile,
                            onUseHint = resolvedHolder::useCrossMathHint,
                            onPlayAgain = resolvedHolder::restartCrossMath,
                            onContinueWorkout = resolvedHolder::startDailyWorkoutStep,
                            onExit = resolvedHolder::openHub,
                        )

                        AppScreen.MATH_MAZE -> MathMazeScreen(
                            state = uiState.mathMazeState,
                            progression = uiState.progressionState,
                            reward = uiState.lastSolveReward,
                            roomCode = if (uiState.mathMazeState.isRace) uiState.activeRoomCode else null,
                            widthSizeClass = widthSizeClass,
                            onTapCell = resolvedHolder::tapMathMazeCell,
                            onStepBack = resolvedHolder::stepBackMathMaze,
                            onUseHint = resolvedHolder::useMathMazeHint,
                            onPlayAgain = resolvedHolder::restartMathMaze,
                            onContinueWorkout = resolvedHolder::startDailyWorkoutStep,
                            onExit = resolvedHolder::openHub,
                        )

                        AppScreen.KENKEN -> KenKenScreen(
                            state = uiState.kenKenState,
                            progression = uiState.progressionState,
                            reward = uiState.lastSolveReward,
                            roomCode = if (uiState.kenKenState.isRace) uiState.activeRoomCode else null,
                            widthSizeClass = widthSizeClass,
                            onSelectCell = resolvedHolder::selectKenKenCell,
                            onEnterDigit = resolvedHolder::enterKenKenDigit,
                            onErase = resolvedHolder::eraseKenKenCell,
                            onUseHint = resolvedHolder::useKenKenHint,
                            onPlayAgain = resolvedHolder::restartKenKen,
                            onContinueWorkout = resolvedHolder::startDailyWorkoutStep,
                            onExit = resolvedHolder::openHub,
                        )

                        AppScreen.MEMORY_MATRIX -> MemoryMatrixScreen(
                            state = uiState.memoryMatrixState,
                            progression = uiState.progressionState,
                            reward = uiState.lastSolveReward,
                            roomCode = if (uiState.memoryMatrixState.isRace) uiState.activeRoomCode else null,
                            widthSizeClass = widthSizeClass,
                            onTapCell = resolvedHolder::tapMemoryMatrixCell,
                            onPeekHint = resolvedHolder::useMemoryMatrixPeek,
                            onPlayAgain = resolvedHolder::restartMemoryMatrix,
                            onExit = resolvedHolder::openHub,
                        )

                        AppScreen.MINDSNAP -> MindsnapScreen(
                            state = uiState.mindsnapState,
                            progression = uiState.progressionState,
                            reward = uiState.lastSolveReward,
                            roomCode = if (uiState.mindsnapState.isRace) uiState.activeRoomCode else null,
                            widthSizeClass = widthSizeClass,
                            onSubmitAnswer = resolvedHolder::submitMindsnapAnswer,
                            onUseHint = resolvedHolder::useMindsnapHint,
                            onPlayAgain = resolvedHolder::restartMindsnap,
                            onContinueWorkout = resolvedHolder::startDailyWorkoutStep,
                            onExit = resolvedHolder::openHub,
                        )

                        AppScreen.STATS_PROFILE -> StatsProfileScreen(
                            progression = uiState.progressionState,
                            widthSizeClass = widthSizeClass,
                            onBack = resolvedHolder::openHub,
                        )

                        AppScreen.COOP_ACTIVE -> when (uiState.selectedGameType) {
                            GameType.MATH -> MathPuzzleRoute(
                                uiState = uiState,
                                widthSizeClass = widthSizeClass,
                                holder = resolvedHolder,
                            )

                            GameType.SUDOKU_6X6, GameType.SUDOKU_9X9 -> SudokuScreen(
                                state = uiState.sudokuState,
                                theme = uiState.progressionState.activeTheme,
                                hintTokens = uiState.progressionState.hintTokens,
                                reward = uiState.lastSolveReward,
                                roomCode = uiState.activeRoomCode,
                                widthSizeClass = widthSizeClass,
                                onSelectCell = resolvedHolder::selectSudokuCell,
                                onEnterDigit = resolvedHolder::enterSudokuDigit,
                                onErase = resolvedHolder::eraseSudokuCell,
                                onUseHint = resolvedHolder::useSudokuHint,
                                onPlayAgain = resolvedHolder::restartSudoku,
                                onExit = resolvedHolder::openHub,
                            )

                            GameType.CROSS_MATH,
                            GameType.MATH_MAZE,
                            GameType.KENKEN,
                            GameType.MEMORY_MATRIX,
                            GameType.MINDSNAP,
                            -> Unit
                            else -> Unit
                        }

                        else -> Unit
                    }
                }

                if (uiState.isChatSheetOpen) {
                    DirectChatSheet(
                        sessionState = uiState.chatUiState,
                        mediaBridge = mediaBridge,
                        onSendMessage = resolvedHolder::sendChatMessage,
                        onTypingChanged = resolvedHolder::sendTypingIndicator,
                        onAttachFile = {
                            fileBridge.pickFile { picked ->
                                if (picked != null) {
                                    resolvedHolder.offerFileToActivePeer(picked)
                                }
                            }
                        },
                        onSendGameInvite = {
                            uiState.chatUiState.activePeer?.let { resolvedHolder.sendQuickGameInvite(it) }
                        },
                        onAcceptFriendRequest = { fromUserId, fromName ->
                            resolvedHolder.acceptFriendRequest(SyncPacket.FriendRequest(fromUserId, fromName))
                        },
                        onDeclineFriendRequest = { fromUserId ->
                            resolvedHolder.declineFriendRequest(SyncPacket.FriendRequest(fromUserId, ""))
                        },
                        onAcceptTransfer = { id -> resolvedHolder.respondToFileOffer(id, accept = true) },
                        onDeclineTransfer = { id -> resolvedHolder.respondToFileOffer(id, accept = false) },
                        onCancelTransfer = resolvedHolder::cancelFileTransfer,
                        onShareFile = { filePath, mimeType ->
                            mediaBridge.shareFile(filePath, mimeType) { _ -> }
                        },
                        onDismissError = {},
                        onClose = resolvedHolder::closePeerChat,
                    )
                }

                if (showAddFriendDialog) {
                    AddFriendByIdDialog(
                        onAdd = { userId ->
                            resolvedHolder.addManualFriendById(userId)
                            showAddFriendDialog = false
                        },
                        onDismiss = { showAddFriendDialog = false },
                    )
                }

                if (showSettingsModal) {
                    SettingsModal(
                        defaultSaveDir = uiState.userIdentity.defaultSaveDir,
                        autoSaveFiles = uiState.userIdentity.autoSaveFiles,
                        canPickDirectory = mediaBridge.canPickDirectory,
                        onChangeSaveDir = {
                            mediaBridge.pickDirectory { picked ->
                                if (picked != null) {
                                    resolvedHolder.updateStorageSettings(
                                        defaultSaveDir = picked,
                                        autoSaveFiles = uiState.userIdentity.autoSaveFiles,
                                    )
                                }
                            }
                        },
                        onToggleAutoSave = { autoSave ->
                            resolvedHolder.updateStorageSettings(
                                defaultSaveDir = uiState.userIdentity.defaultSaveDir,
                                autoSaveFiles = autoSave,
                            )
                        },
                        onCycleAccent = resolvedHolder::cycleAvatarAccent,
                        onClose = { showSettingsModal = false },
                    )
                }

                if (!uiState.userIdentity.onboardingCompleted && !localOnboardingDismissed) {
                    OnboardingOverlay(
                        identity = uiState.userIdentity,
                        onCompleteOnboarding = {
                            localOnboardingDismissed = true
                            GlobalContext.getOrNull()?.getOrNull<UserIdentityStore>()?.completeOnboarding()
                        },
                    )
                }

                // Global slide-down notification banner above all tabs and screens
                GlobalNotificationBanner(
                    banner = activeBanner,
                    onAcceptInvite = { invite -> resolvedHolder.acceptGameInvite(invite) },
                    onDeclineInvite = { invite -> resolvedHolder.declineGameInvite(invite) },
                    onAcceptRequest = { req -> resolvedHolder.acceptFriendRequest(req) },
                    onDeclineRequest = { req -> resolvedHolder.declineFriendRequest(req) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun AppShell(
    uiState: SplitSignalAppUiState,
    widthSizeClass: WindowWidthSizeClass,
    isDesktop: Boolean,
    hasPendingNotifications: Boolean,
    networkSearchQuery: String,
    onNetworkSearchQueryChange: (String) -> Unit,
    onOpenAddFriend: () -> Unit,
    onOpenSettings: () -> Unit,
    onNotificationClick: () -> Unit,
    onSelectTab: (MainTab) -> Unit,
    holder: SplitSignalStateHolder,
    onPickFileForPeer: (com.splitsignal.core.domain.DiscoveredPeer) -> Unit,
    onLaunchDualDesktopDemo: ((GameType) -> Unit)?,
) {
    if (isDesktop) {
        Row(modifier = Modifier.fillMaxSize()) {
            DesktopNavigationRail(
                activeTab = uiState.activeTab,
                onSelectTab = onSelectTab,
            )
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                ContextualTopBar(
                    activeTab = uiState.activeTab,
                    streakDays = uiState.progressionState.streakDays,
                    coins = uiState.progressionState.coins,
                    elo = uiState.progressionState.elo,
                    hasPendingNotifications = hasPendingNotifications,
                    onNotificationClick = onNotificationClick,
                    onOpenAddFriend = onOpenAddFriend,
                    onOpenSettings = onOpenSettings,
                )
                HubTabContent(
                    uiState = uiState,
                    widthSizeClass = widthSizeClass,
                    networkSearchQuery = networkSearchQuery,
                    onNetworkSearchQueryChange = onNetworkSearchQueryChange,
                    holder = holder,
                    onPickFileForPeer = onPickFileForPeer,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                if (onLaunchDualDesktopDemo != null) {
                    DesktopJudgeDemoBar(
                        onLaunchMath = { onLaunchDualDesktopDemo(GameType.MATH) },
                    )
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            ContextualTopBar(
                activeTab = uiState.activeTab,
                streakDays = uiState.progressionState.streakDays,
                coins = uiState.progressionState.coins,
                elo = uiState.progressionState.elo,
                hasPendingNotifications = hasPendingNotifications,
                onNotificationClick = onNotificationClick,
                onOpenAddFriend = onOpenAddFriend,
                onOpenSettings = onOpenSettings,
            )
            HubTabContent(
                uiState = uiState,
                widthSizeClass = widthSizeClass,
                networkSearchQuery = networkSearchQuery,
                onNetworkSearchQueryChange = onNetworkSearchQueryChange,
                holder = holder,
                onPickFileForPeer = onPickFileForPeer,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            if (onLaunchDualDesktopDemo != null) {
                DesktopJudgeDemoBar(
                    onLaunchMath = { onLaunchDualDesktopDemo(GameType.MATH) },
                )
            }
            MobileBottomNavBar(
                activeTab = uiState.activeTab,
                onSelectTab = onSelectTab,
                onAddFriendClick = onOpenAddFriend,
            )
        }
    }
}

@Composable
private fun HubTabContent(
    uiState: SplitSignalAppUiState,
    widthSizeClass: WindowWidthSizeClass,
    networkSearchQuery: String,
    onNetworkSearchQueryChange: (String) -> Unit,
    holder: SplitSignalStateHolder,
    onPickFileForPeer: (com.splitsignal.core.domain.DiscoveredPeer) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        when (uiState.activeTab) {
            MainTab.PLAY -> GamesHubScreen(
                progressionState = uiState.progressionState,
                selectedDifficulty = uiState.selectedSudokuDifficulty,
                widthSizeClass = widthSizeClass,
                onPlayDailySprint = holder::startDailySprint,
                onStartDailyWorkout = holder::startDailyWorkoutStep,
                onHostSudokuRace = { holder.openSudokuRaceLink(LinkTab.HOST) },
                onJoinSudokuRace = { holder.openSudokuRaceLink(LinkTab.JOIN) },
                onHostDuel = { type -> holder.openLocalLink(type, LinkTab.HOST) },
                onPlaySudokuSolo = holder::startSudokuSolo,
                onSelectSudokuDifficulty = holder::selectSudokuDifficulty,
                onPlayCrossMath = { holder.startCrossMath() },
                onPlayMathMaze = { holder.startMathMaze() },
                onPlayKenKen = { holder.startKenKen() },
                onPlayMemoryMatrix = { holder.startMemoryMatrix() },
                onPlayMindsnap = { holder.startMindsnap() },
                onOpenStatsProfile = holder::openStatsProfile,
            )

            MainTab.NETWORK -> NetworkScreen(
                nearbyState = uiState.nearbyDevices,
                pendingRequests = uiState.chatUiState.incomingFriendRequests,
                isScanning = uiState.isNearbyScanning,
                searchQuery = networkSearchQuery,
                windowSizeClass = widthSizeClass,
                onToggleScan = holder::toggleNetworkScanning,
                onSearchQueryChange = onNetworkSearchQueryChange,
                onAcceptRequest = holder::acceptFriendRequest,
                onDeclineRequest = holder::declineFriendRequest,
                onDuelPeer = holder::duelPeer,
                onChatPeer = holder::openPeerChat,
                onSendFileToPeer = onPickFileForPeer,
                onAddFriend = holder::sendFriendRequest,
                onViewOfflineFriend = holder::openOfflineFriendChat,
                onRemoveFriend = holder::removeFriend,
            )

            MainTab.PROFILE -> ProfileScreen(
                identity = uiState.userIdentity,
                friends = uiState.friends,
                progression = uiState.progressionState,
                windowSizeClass = widthSizeClass,
                onUpdateDisplayName = holder::updateDisplayName,
                onCycleAvatarAccent = holder::cycleAvatarAccent,
                onOpenCognitiveStats = holder::openStatsProfile,
                onUpdatePrivacyMode = holder::updatePrivacyMode,
                transfers = uiState.chatUiState.transfers,
            )
        }
    }
}

@Composable
private fun ContextualTopBar(
    activeTab: MainTab,
    streakDays: Int,
    coins: Int,
    elo: Int,
    hasPendingNotifications: Boolean,
    onNotificationClick: () -> Unit,
    onOpenAddFriend: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val title = when (activeTab) {
        MainTab.PLAY -> "Play"
        MainTab.NETWORK -> "Network"
        MainTab.PROFILE -> "Profile"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ShellTopBarBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = ShellActiveText,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Global Stats: Streak, Coins, Elo on every screen (Linear style with icons & height 32dp)
                val pillShape = RoundedCornerShape(8.dp)
                val pillBg = Brush.verticalGradient(
                    listOf(Color(0xFF1B1E22), Color(0xFF111316))
                )
                val pillBorderDefault = Brush.verticalGradient(
                    listOf(Color(0x33FFFFFF), Color(0x0FFFFFFF))
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Streak Box: Flame + streak days
                    val isStreakActive = streakDays > 0
                    val streakBorder = if (isStreakActive) {
                        Brush.verticalGradient(listOf(Color(0x80FF9F1C), Color(0x26FF9F1C)))
                    } else {
                        pillBorderDefault
                    }
                    Row(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(pillShape)
                            .background(pillBg)
                            .border(1.dp, streakBorder, pillShape)
                            .padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        SsIcon(
                            icon = SsIcons.StreakFire,
                            contentDescription = "Daily streak",
                            tint = if (isStreakActive) Color(0xFFFF9F1C) else ShellInactiveText,
                            iconSize = 14.dp,
                        )
                        Text(
                            text = "$streakDays",
                            style = LinearTextStyles.TabularLabel.copy(
                                color = if (isStreakActive) ShellActiveText else ShellInactiveText,
                                fontSize = 12.sp,
                            ),
                        )
                    }

                    // Coins Box: Coin icon + coin amount
                    val hasCoins = coins > 0
                    Row(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(pillShape)
                            .background(pillBg)
                            .border(1.dp, pillBorderDefault, pillShape)
                            .padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        SsIcon(
                            icon = SsIcons.Coin,
                            contentDescription = "Coins",
                            tint = if (hasCoins) Color(0xFFE2E4E9) else ShellInactiveText,
                            iconSize = 14.dp,
                        )
                        Text(
                            text = "$coins",
                            style = LinearTextStyles.TabularLabel.copy(
                                color = if (hasCoins) ShellActiveText else ShellInactiveText,
                                fontSize = 12.sp,
                            ),
                        )
                    }

                    // Elo Box: Shield rank icon + Elo rating
                    Row(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(pillShape)
                            .background(pillBg)
                            .border(1.dp, pillBorderDefault, pillShape)
                            .padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        SsIcon(
                            icon = SsIcons.Shield,
                            contentDescription = "Elo rating",
                            tint = Color(0xFF5E6AD2),
                            iconSize = 14.dp,
                        )
                        Text(
                            text = "$elo",
                            style = LinearTextStyles.TabularLabel.copy(
                                color = ShellActiveText,
                                fontSize = 12.sp,
                            ),
                        )
                    }
                }

                // Screen-specific actions if on Network tab
                if (activeTab == MainTab.NETWORK) {
                    NotificationBellButton(
                        hasBadge = hasPendingNotifications,
                        onClick = onNotificationClick,
                    )
                    TopBarIconButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = "Add friend by ID",
                        tint = ShellActiveText,
                        onClick = onOpenAddFriend,
                    )
                }

                // Global Settings button on every screen
                TopBarIconButton(
                    icon = SsIcons.SettingsOutline,
                    contentDescription = "Settings",
                    tint = ShellActiveText,
                    onClick = onOpenSettings,
                )
            }
        }
        HorizontalDivider(color = ShellHairline, thickness = 1.dp)
    }
}

@Composable
private fun NotificationBellButton(
    hasBadge: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ShellNavSurface)
            .border(1.dp, ShellHairline, RoundedCornerShape(8.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Notifications,
            contentDescription = "Notifications",
            tint = ShellActiveText,
            modifier = Modifier.size(17.dp),
        )
        if (hasBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(ShellBadgeRed),
            )
        }
    }
}

@Composable
private fun TopBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1B1E22), Color(0xFF111316))
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(Color(0x33FFFFFF), Color(0x0FFFFFFF))
                ),
                shape,
            )
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun TopBarIconButton(
    icon: DrawableResource,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1B1E22), Color(0xFF111316))
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(Color(0x33FFFFFF), Color(0x0FFFFFFF))
                ),
                shape,
            )
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        SsIcon(
            icon = icon,
            contentDescription = contentDescription,
            tint = tint,
            iconSize = 16.dp,
        )
    }
}

private data class NavTabSpec(
    val tab: MainTab,
    val label: String,
    val icon: DrawableResource,
)

private val NavigationTabs = listOf(
    NavTabSpec(MainTab.PLAY, "Play", SsIcons.Games),
    NavTabSpec(MainTab.NETWORK, "Network", SsIcons.Network),
    NavTabSpec(MainTab.PROFILE, "Profile", SsIcons.Profile),
)

private fun navItems(
    activeTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
): List<SsNavItem> = NavigationTabs.map { spec ->
    SsNavItem(
        label = spec.label,
        icon = spec.icon,
        selected = activeTab == spec.tab,
        onClick = { onSelectTab(spec.tab) },
    )
}

@Composable
private fun DesktopNavigationRail(
    activeTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
) {
    SsNavRail(
        items = navItems(activeTab, onSelectTab),
    )
}

@Composable
private fun MobileBottomNavBar(
    activeTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    onAddFriendClick: () -> Unit,
) {
    SsBottomNav(
        items = navItems(activeTab, onSelectTab),
        onFabClick = if (activeTab == MainTab.NETWORK) onAddFriendClick else null,
        fabIcon = SsIcons.AddFriend,
        fabContentDescription = "Add friend",
    )
}

@Composable
private fun GlobalNotificationBanner(
    banner: GlobalBannerItem?,
    onAcceptInvite: (SyncPacket.GameInvite) -> Unit,
    onDeclineInvite: (SyncPacket.GameInvite) -> Unit,
    onAcceptRequest: (SyncPacket.FriendRequest) -> Unit,
    onDeclineRequest: (SyncPacket.FriendRequest) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = banner != null,
        enter = slideInVertically(
            animationSpec = spring(dampingRatio = 0.8f),
            initialOffsetY = { -it },
        ) + fadeIn(animationSpec = tween(180)),
        exit = slideOutVertically(
            animationSpec = tween(180),
            targetOffsetY = { -it },
        ) + fadeOut(animationSpec = tween(150)),
        modifier = modifier,
    ) {
        if (banner != null) {
            val shape = RoundedCornerShape(12.dp)
            Row(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = shape,
                        ambientColor = ShellAccentIndigo,
                        spotColor = ShellAccentIndigo,
                    )
                    .clip(shape)
                    .background(ShellNavSurface)
                    .border(1.dp, ShellAccentIndigo, shape)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val eyebrow = when (banner) {
                        is GlobalBannerItem.Invite -> "GAME INVITE"
                        is GlobalBannerItem.Request -> "FRIEND REQUEST"
                    }
                    val message = when (banner) {
                        is GlobalBannerItem.Invite ->
                            "${banner.invite.fromName} invited you to ${banner.invite.gameType.displayName}"
                        is GlobalBannerItem.Request ->
                            "${banner.request.fromName} (${banner.request.fromUserId}) wants to connect"
                    }
                    Text(
                        text = eyebrow,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = ShellAccentIndigo,
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = ShellActiveText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TactileTile(
                        onClick = {
                            when (banner) {
                                is GlobalBannerItem.Invite -> onDeclineInvite(banner.invite)
                                is GlobalBannerItem.Request -> onDeclineRequest(banner.request)
                            }
                        },
                        backgroundColor = SplitSignalColors.SurfaceTile,
                        restingBorderColor = ShellHairline,
                        cornerRadius = 8.dp,
                    ) {
                        Text(
                            text = "Decline",
                            style = MaterialTheme.typography.labelSmall,
                            color = ShellInactiveText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        )
                    }
                    TactileTile(
                        onClick = {
                            when (banner) {
                                is GlobalBannerItem.Invite -> onAcceptInvite(banner.invite)
                                is GlobalBannerItem.Request -> onAcceptRequest(banner.request)
                            }
                        },
                        backgroundColor = ShellAccentIndigo,
                        restingBorderColor = ShellAccentIndigo,
                        cornerRadius = 8.dp,
                    ) {
                        Text(
                            text = "Accept",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF08090A),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddFriendByIdDialog(
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val normalized = input.trim().uppercase()
    val isValid = Regex("^KMP-[A-Z0-9]{4}$").matches(normalized)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ShellTopBarBg.copy(alpha = 0.88f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        LinearCard(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            borderColor = ShellAccentIndigo,
            contentPadding = 20.dp,
        ) {
            Text(
                text = "ADD FRIEND BY ID",
                style = MaterialTheme.typography.labelSmall,
                color = ShellAccentIndigo,
            )
            Text(
                text = "Enter a friend's permanent KMP-XXXX identifier to add them to your list.",
                style = MaterialTheme.typography.bodySmall,
                color = ShellInactiveText,
            )
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.uppercase() },
                singleLine = true,
                placeholder = {
                    Text(
                        text = "KMP-XXXX",
                        style = LinearTextStyles.TabularLabel,
                        color = SplitSignalColors.TextMuted,
                    )
                },
                textStyle = LinearTextStyles.TabularLabel.copy(color = ShellActiveText),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ShellAccentIndigo,
                    unfocusedBorderColor = ShellHairline,
                    focusedContainerColor = SplitSignalColors.SurfaceTile,
                    unfocusedContainerColor = SplitSignalColors.SurfaceTile,
                    cursorColor = ShellAccentIndigo,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TactileTile(
                    onClick = onDismiss,
                    backgroundColor = SplitSignalColors.SurfaceTile,
                    restingBorderColor = ShellHairline,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelMedium,
                        color = ShellInactiveText,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                }
                TactileTile(
                    onClick = { if (isValid) onAdd(normalized) },
                    backgroundColor = if (isValid) ShellAccentIndigo else SplitSignalColors.SurfaceTile,
                    restingBorderColor = if (isValid) ShellAccentIndigo else ShellHairline,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "Add Friend",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isValid) Color(0xFF08090A) else SplitSignalColors.TextMuted,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopJudgeDemoBar(
    onLaunchMath: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SplitSignalColors.SurfaceCard)
            .border(1.dp, SplitSignalColors.BorderSubtle)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "JUDGE DUAL-WINDOW DEMO",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = SplitSignalColors.TextSecondary,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TactileTile(
                onClick = onLaunchMath,
                backgroundColor = SplitSignalColors.SurfaceTile,
                restingBorderColor = SplitSignalColors.BorderSubtle,
                cornerRadius = 8.dp,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)),
            ) {
                Text(
                    text = "Dual Math",
                    style = MaterialTheme.typography.labelMedium,
                    color = SplitSignalColors.AccentIndigo,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun MathPuzzleRoute(
    uiState: SplitSignalAppUiState,
    widthSizeClass: WindowWidthSizeClass,
    holder: SplitSignalStateHolder,
) {
    MathPuzzleScreen(
        uiState = uiState,
        widthSizeClass = widthSizeClass,
        onOpenCoopLobby = { holder.openLocalLink(GameType.MATH, LinkTab.HOST) },
        onExitToHub = holder::openHub,
        onCellTapped = holder::onCellTapped,
        onNextLevel = holder::onNextLevel,
    )
}

