package com.splitsignal.app.presentation

import com.splitsignal.core.domain.BattleshipEngine
import com.splitsignal.core.domain.BattleshipGameState
import com.splitsignal.core.domain.BoardTheme
import com.splitsignal.core.domain.ChatSessionManager
import com.splitsignal.core.domain.ChatSessionUiState
import com.splitsignal.core.domain.ClientSyncTransport
import com.splitsignal.core.domain.CrossMathEngine
import com.splitsignal.core.domain.CrossMathGameState
import com.splitsignal.core.domain.CrossMathGenerator
import com.splitsignal.core.domain.CrossMathSize
import com.splitsignal.core.domain.DeterministicRng
import com.splitsignal.core.domain.DiscoveredPeer
import com.splitsignal.core.domain.DiscoveredRoom
import com.splitsignal.core.domain.FileTransferTransport
import com.splitsignal.core.domain.Fnv1a
import com.splitsignal.core.domain.FriendRecord
import com.splitsignal.core.domain.GameMode
import com.splitsignal.core.domain.GameType
import com.splitsignal.core.domain.HostSyncTransport
import com.splitsignal.core.domain.InMemorySyncPipe
import com.splitsignal.core.domain.InviteTokenCodec
import com.splitsignal.core.domain.KenKenEngine
import com.splitsignal.core.domain.KenKenGameState
import com.splitsignal.core.domain.KenKenGenerator
import com.splitsignal.core.domain.KenKenSize
import com.splitsignal.core.domain.LanDiscovery
import com.splitsignal.core.domain.MathGameState
import com.splitsignal.core.domain.MathLevelGenerator
import com.splitsignal.core.domain.MathMazeEngine
import com.splitsignal.core.domain.MathMazeGameState
import com.splitsignal.core.domain.MathMazeGenerator
import com.splitsignal.core.domain.MathPuzzleEngine
import com.splitsignal.core.domain.MemoryMatrixEngine
import com.splitsignal.core.domain.MemoryMatrixGameState
import com.splitsignal.core.domain.MemoryMatrixGenerator
import com.splitsignal.core.domain.MindsnapEngine
import com.splitsignal.core.domain.MindsnapGameState
import com.splitsignal.core.domain.MindsnapGenerator
import com.splitsignal.core.domain.NearbyDevicesState
import com.splitsignal.core.domain.PeerStatus
import com.splitsignal.core.domain.PlatformFileHandle
import com.splitsignal.core.domain.PlayerRole
import com.splitsignal.core.domain.PrivacyMode
import com.splitsignal.core.domain.ProgressionState
import com.splitsignal.core.domain.ProgressionStore
import com.splitsignal.core.domain.RoomCodeGenerator
import com.splitsignal.core.domain.SolveRewardBreakdown
import com.splitsignal.core.domain.SudokuDifficulty
import com.splitsignal.core.domain.SudokuGameState
import com.splitsignal.core.domain.SudokuGenerator
import com.splitsignal.core.domain.SudokuRaceEngine
import com.splitsignal.core.domain.SudokuSize
import com.splitsignal.core.domain.SyncPacket
import com.splitsignal.core.domain.SyncTransport
import com.splitsignal.core.domain.TransportConnectionState
import com.splitsignal.core.domain.TransportFactory
import com.splitsignal.core.domain.UserIdentity
import com.splitsignal.core.domain.UserIdentityStore
import com.splitsignal.app.platform.AppLoggerBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Validated host:port endpoint pair parsed from user input.
 */
data class ParsedEndpoint(
    val host: String,
    val port: Int,
)

/**
 * Top-level navigation destinations in SplitSignal.
 */
enum class AppScreen {
    HUB,
    LOCAL_LINK,
    SUDOKU_SOLO,
    SUDOKU_RACE,
    CROSS_MATH,
    MATH_MAZE,
    KENKEN,
    MEMORY_MATRIX,
    MINDSNAP,
    STATS_PROFILE,
    MATH_SOLO,
    COOP_ACTIVE,
}

/**
 * Top-level bottom-bar navigation tabs in SplitSignal.
 */
enum class MainTab {
    PLAY,
    NETWORK,
    PROFILE,
}

/**
 * Active sub-tab inside the unified 2-device local link screen.
 */
enum class LinkTab {
    HOST,
    JOIN,
}

/**
 * Immutable UI state exposed by [SplitSignalStateHolder] to the Compose UI layer.
 */
data class SplitSignalAppUiState(
    val screen: AppScreen,
    val gameMode: GameMode,
    val selectedGameType: GameType,
    val linkTab: LinkTab,
    val canHostLan: Boolean,
    val nearbyRooms: List<DiscoveredRoom>,
    val hostInviteToken: String,
    val progressionState: ProgressionState,
    val sudokuState: SudokuGameState,
    val crossMathState: CrossMathGameState,
    val mathMazeState: MathMazeGameState,
    val kenKenState: KenKenGameState,
    val memoryMatrixState: MemoryMatrixGameState,
    val mindsnapState: MindsnapGameState,
    val isTriathlonActive: Boolean,
    val triathlonRoundIndex: Int,
    val isDailyWorkoutRun: Boolean,
    val lastSolveReward: SolveRewardBreakdown?,
    val selectedSudokuSize: SudokuSize,
    val selectedSudokuDifficulty: SudokuDifficulty,
    val mathSolverState: MathGameState,
    val mathClueState: MathGameState,
    val battleshipState: BattleshipGameState,
    val activeRole: PlayerRole,
    val hostRole: PlayerRole,
    val hostRoomCode: String,
    val hostAddress: String?,
    val connectedClients: Int,
    val clientConnectionState: TransportConnectionState,
    val clientEndpointInput: String,
    val clientRoomCodeInput: String,
    val clientRole: PlayerRole,
    val clientValidationError: String?,
    val activeTab: MainTab = MainTab.PLAY,
    val userIdentity: UserIdentity = UserIdentity(userId = "", displayName = ""),
    val friends: List<FriendRecord> = emptyList(),
    val nearbyDevices: NearbyDevicesState = NearbyDevicesState(),
    val chatUiState: ChatSessionUiState = ChatSessionUiState(),
    val isChatSheetOpen: Boolean = false,
    val isNearbyScanning: Boolean = false,
) {
    /**
     * Math state corresponding to the player's active role in two-device mode.
     */
    val activeMathState: MathGameState
        get() = if (activeRole == PlayerRole.OPERATOR) mathSolverState else mathClueState

    /**
     * Room code label associated with the active session.
     */
    val activeRoomCode: String
        get() = when (gameMode) {
            GameMode.SOLO_SPLIT -> SOLO_ROOM_CODE
            GameMode.LAN_HOST -> hostRoomCode
            GameMode.LAN_CLIENT -> clientRoomCodeInput.trim().uppercase()
        }

    private companion object {
        const val SOLO_ROOM_CODE = "SOLO"
    }
}

/**
 * Presentation state holder coordinating Sudoku Duel, player progression, LAN discovery,
 * sync transports, Math Deduction, and Tactical Battleship.
 */
class SplitSignalStateHolder(
    private val transportFactory: TransportFactory,
    private val lanDiscovery: LanDiscovery,
    private val progressionStore: ProgressionStore,
    private val identityStore: UserIdentityStore,
    private val fileTransferTransport: FileTransferTransport? = null,
    private val scope: CoroutineScope,
) {
    val canHostLan: Boolean = transportFactory.canHostLan

    @OptIn(kotlin.time.ExperimentalTime::class)
    private val chatSessionManager = ChatSessionManager(
        identityStore = identityStore,
        lanDiscovery = lanDiscovery,
        fileTransferTransport = fileTransferTransport,
        scope = scope,
        nowMsProvider = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
    )

    private val roomCodeRng = DeterministicRng(INITIAL_ROOM_RNG_SEED)
    private var hostNonce: Int = 0
    private var battleshipNonce: Int = 0
    private var sudokuNonce: Int = 0
    private var hostRoomCode: String = generateRoomCode(hostNonce)

    private var soloPipeHost: SyncTransport? = null
    private var soloPipeClient: SyncTransport? = null
    private var soloSudokuEngine: SudokuRaceEngine? = null
    private var hostSudokuEngine: SudokuRaceEngine? = null
    private var clientSudokuEngine: SudokuRaceEngine? = null

    private var soloMathSolverEngine: MathPuzzleEngine? = null
    private var soloMathClueEngine: MathPuzzleEngine? = null
    private var hostMathEngine: MathPuzzleEngine? = null
    private var clientMathEngine: MathPuzzleEngine? = null

    private var soloBattleshipEngine: BattleshipEngine? = null
    private var hostBattleshipEngine: BattleshipEngine? = null
    private var clientBattleshipEngine: BattleshipEngine? = null

    private var activeCrossMathEngine: CrossMathEngine? = null
    private var activeMathMazeEngine: MathMazeEngine? = null
    private var activeKenKenEngine: KenKenEngine? = null
    private var activeMemoryMatrixEngine: MemoryMatrixEngine? = null
    private var activeMindsnapEngine: MindsnapEngine? = null
    private var matiksNonce: Int = 0

    private data class PendingOutgoingInvite(
        val inviteId: String,
        val gameType: GameType,
        val seed: UInt,
        val transport: SyncTransport,
    )

    private var hostTransport: HostSyncTransport? = null
    private var clientTransport: ClientSyncTransport? = null
    private var socialHostTransport: HostSyncTransport? = null
    private var socialClientTransport: ClientSyncTransport? = null
    private var p2pChallengeTransport: SyncTransport? = null
    private var pendingOutgoingInvite: PendingOutgoingInvite? = null
    private var socialHostPort: Int = 0
    private var sessionJob: Job? = null
    private var nearbyRoomsJob: Job? = null
    private var progressionJob: Job? = null
    private var socialHostJob: Job? = null
    private var identityJob: Job? = null
    private var friendsJob: Job? = null
    private var nearbyDevicesJob: Job? = null
    private var chatStateJob: Job? = null
    private var friendRequestJob: Job? = null
    private var duelInviteJob: Job? = null
    private var activeBroadcastSignature: BroadcastSignature? = null

    private val _uiState: MutableStateFlow<SplitSignalAppUiState>
    val uiState: StateFlow<SplitSignalAppUiState>

    init {
        val initialMathSolverState = buildIdleMathState(PlayerRole.OPERATOR)
        val initialMathClueState = buildIdleMathState(PlayerRole.ANALYST)
        val initialBattleshipState = buildIdleBattleshipState()
        val initialSudokuState = buildIdleSudokuState(
            size = SudokuSize.SPRINT_6X6,
            difficulty = SudokuDifficulty.MEDIUM,
        )

        _uiState = MutableStateFlow(
            SplitSignalAppUiState(
                screen = AppScreen.HUB,
                gameMode = GameMode.SOLO_SPLIT,
                selectedGameType = GameType.SUDOKU_6X6,
                linkTab = if (canHostLan) LinkTab.HOST else LinkTab.JOIN,
                canHostLan = canHostLan,
                nearbyRooms = emptyList(),
                hostInviteToken = "",
                progressionState = progressionStore.state.value,
                sudokuState = initialSudokuState,
                crossMathState = buildIdleCrossMathState(),
                mathMazeState = buildIdleMathMazeState(),
                kenKenState = buildIdleKenKenState(),
                memoryMatrixState = buildIdleMemoryMatrixState(),
                mindsnapState = buildIdleMindsnapState(),
                isTriathlonActive = false,
                triathlonRoundIndex = 0,
                isDailyWorkoutRun = false,
                lastSolveReward = null,
                selectedSudokuSize = SudokuSize.SPRINT_6X6,
                selectedSudokuDifficulty = SudokuDifficulty.MEDIUM,
                mathSolverState = initialMathSolverState,
                mathClueState = initialMathClueState,
                battleshipState = initialBattleshipState,
                activeRole = PlayerRole.OPERATOR,
                hostRole = PlayerRole.OPERATOR,
                hostRoomCode = hostRoomCode,
                hostAddress = null,
                connectedClients = 0,
                clientConnectionState = TransportConnectionState.DISCONNECTED,
                clientEndpointInput = "",
                clientRoomCodeInput = "",
                clientRole = PlayerRole.ANALYST,
                clientValidationError = null,
                activeTab = MainTab.PLAY,
                userIdentity = identityStore.identity.value,
                friends = identityStore.friends.value,
                nearbyDevices = chatSessionManager.nearbyDevices.value,
                chatUiState = chatSessionManager.uiState.value,
                isChatSheetOpen = false,
            ),
        )
        uiState = _uiState.asStateFlow()

        progressionJob = scope.launch {
            progressionStore.state.collect { progression ->
                _uiState.update { it.copy(progressionState = progression) }
                AppLoggerBridge.setUserProperty("elo_rating", progression.elo.toString())
                AppLoggerBridge.setUserProperty("streak_days", progression.streakDays.toString())
            }
        }
        identityJob = scope.launch {
            identityStore.identity.collect { identity ->
                _uiState.update { it.copy(userIdentity = identity) }
                AppLoggerBridge.setUserId(identity.userId)
                AppLoggerBridge.setUserProperty("avatar_id", identity.avatarAccentIndex.toString())
                refreshPeerPresence()
            }
        }
        friendsJob = scope.launch {
            identityStore.friends.collect { friends ->
                _uiState.update { it.copy(friends = friends) }
            }
        }
        nearbyDevicesJob = scope.launch {
            chatSessionManager.nearbyDevices.collect { nearby ->
                _uiState.update { it.copy(nearbyDevices = nearby) }
            }
        }
        chatStateJob = scope.launch {
            chatSessionManager.uiState.collect { chatState ->
                _uiState.update { it.copy(chatUiState = chatState) }
            }
        }
        if (canHostLan) {
            startSocialHostAndPresence()
        }
    }

    /**
     * Switches the active bottom-bar [MainTab] without altering manual network scanning state.
     */
    fun selectMainTab(tab: MainTab) {
        AppLoggerBridge.logScreen("TAB_${tab.name}")
        _uiState.update { it.copy(activeTab = tab) }
    }

    /**
     * Toggles manual LAN peer scanning on or off.
     */
    fun toggleNetworkScanning() {
        if (_uiState.value.isNearbyScanning) {
            stopNetworkScanning()
        } else {
            startNetworkScanning()
        }
    }

    /**
     * Starts manual LAN peer scanning unless [PrivacyMode.PRIVATE] is active.
     */
    fun startNetworkScanning() {
        if (identityStore.identity.value.privacyMode == PrivacyMode.PRIVATE) {
            _uiState.update { it.copy(isNearbyScanning = false) }
            lanDiscovery.stopPeerPresence()
            return
        }
        _uiState.update { it.copy(isNearbyScanning = true) }
        refreshPeerPresence()
    }

    /**
     * Stops manual LAN peer scanning and clears local UDP presence broadcasts.
     */
    fun stopNetworkScanning() {
        _uiState.update { it.copy(isNearbyScanning = false) }
        lanDiscovery.stopPeerPresence()
    }

    /**
     * Updates the local offline profile visibility [PrivacyMode] and stops scanning if set to [PrivacyMode.PRIVATE].
     */
    fun updatePrivacyMode(mode: PrivacyMode) {
        identityStore.updatePrivacyMode(mode)
        if (mode == PrivacyMode.PRIVATE) {
            stopNetworkScanning()
        }
    }

    /**
     * Updates the default file save directory and auto-save preference.
     */
    fun updateStorageSettings(defaultSaveDir: String, autoSaveFiles: Boolean) {
        identityStore.updateStorageSettings(defaultSaveDir, autoSaveFiles)
    }

    /**
     * Resets cached peers, saved friends list, chat history, and active invites.
     */
    fun clearLocalData() {
        identityStore.clearLocalData()
        chatSessionManager.clearPendingGameInvite()
        lanDiscovery.stopBroadcasting()
    }

    /**
     * Removes the friend matching [userId] and closes the chat sheet if currently open for them.
     */
    fun removeFriend(userId: String) {
        identityStore.removeFriend(userId)
        val normalizedId = userId.trim().uppercase()
        val chatState = chatSessionManager.uiState.value
        val activePeerId = chatState.activePeer?.userId
        val activeOfflineId = chatState.activeOfflineFriend?.userId
        if (activePeerId == userId || activePeerId == normalizedId ||
            activeOfflineId == userId || activeOfflineId == normalizedId
        ) {
            closePeerChat()
        }
    }

    /**
     * Sends a quick P2P game invite to [peer] for [gameType].
     */
    fun sendQuickGameInvite(peer: DiscoveredPeer, gameType: GameType = GameType.SUDOKU_6X6) {
        duelPeer(peer, gameType)
    }

    /**
     * Updates the local offline profile display name.
     */
    fun updateDisplayName(name: String) {
        identityStore.updateDisplayName(name)
    }

    /**
     * Cycles the local offline profile avatar accent color index.
     */
    fun cycleAvatarAccent() {
        identityStore.cycleAvatarAccent()
    }

    /**
     * Marks first-launch onboarding as completed and persists the user identity.
     */
    fun completeOnboarding() {
        identityStore.completeOnboarding()
    }

    /**
     * Opens the 1-on-1 live chat & file transfer sheet for [peer] and connects over WebSocket.
     */
    fun openPeerChat(peer: DiscoveredPeer) {
        socialClientTransport?.close()
        val client = transportFactory.createClient(scope)
        socialClientTransport = client
        chatSessionManager.openSession(peer, client)
        _uiState.update { it.copy(isChatSheetOpen = true) }
        client.connect(
            host = peer.hostIp,
            port = peer.ktorPort,
            roomCode = SOCIAL_ROOM_CODE,
            role = PlayerRole.ANALYST,
        )
    }

    /**
     * Opens the chat sheet in offline read-only mode showing saved history for [friend].
     */
    fun openOfflineFriendChat(friend: FriendRecord) {
        socialClientTransport?.close()
        socialClientTransport = null
        socialHostTransport?.let { chatSessionManager.bindTransport(it) }
        chatSessionManager.openOfflineSession(friend)
        _uiState.update { it.copy(isChatSheetOpen = true) }
    }

    /**
     * Closes the 1-on-1 live chat sheet and restores background listening on the social host.
     */
    fun closePeerChat() {
        socialClientTransport?.close()
        socialClientTransport = null
        chatSessionManager.closeSession()
        socialHostTransport?.let { chatSessionManager.bindTransport(it) }
        _uiState.update { it.copy(isChatSheetOpen = false) }
    }

    /**
     * Sends a chat text message to the active peer.
     */
    fun sendChatMessage(text: String) {
        scope.launch {
            chatSessionManager.sendChatMessage(text)
        }
    }

    /**
     * Sends an offline sticker/GIF message to the active peer.
     */
    fun sendStickerMessage(stickerCode: String, label: String) {
        scope.launch {
            chatSessionManager.sendChatMessage(text = label, stickerCode = stickerCode)
        }
    }

    /**
     * Sends a typing indicator update to the active peer.
     */
    fun sendTypingIndicator(isTyping: Boolean) {
        scope.launch {
            chatSessionManager.sendTypingIndicator(isTyping)
        }
    }

    /**
     * Sends a mutual friend request to [peer], connecting over WebSocket if not already active.
     */
    fun sendFriendRequest(peer: DiscoveredPeer) {
        val activePeer = chatSessionManager.uiState.value.activePeer
        if (activePeer?.userId == peer.userId && socialClientTransport != null) {
            scope.launch {
                chatSessionManager.sendFriendRequest(peer)
            }
            return
        }

        friendRequestJob?.cancel()
        val client = transportFactory.createClient(scope)
        friendRequestJob = scope.launch {
            val packetForwardJob = launch {
                client.incomingPackets.collect { packet ->
                    chatSessionManager.onIncomingPacket(packet)
                }
            }
            try {
                client.connect(
                    host = peer.hostIp,
                    port = peer.ktorPort,
                    roomCode = SOCIAL_ROOM_CODE,
                    role = PlayerRole.ANALYST,
                )
                val state = client.connectionState.first {
                    it == TransportConnectionState.CONNECTED ||
                        it == TransportConnectionState.ERROR ||
                        it == TransportConnectionState.DISCONNECTED
                }
                if (state == TransportConnectionState.CONNECTED) {
                    val self = identityStore.identity.value
                    client.send(
                        SyncPacket.FriendRequest(
                            fromUserId = self.userId,
                            fromName = self.displayName,
                        ),
                    )
                    // Keep listening on this WebSocket so FriendAccept from the peer arrives cleanly
                    kotlinx.coroutines.delay(60_000L)
                }
            } finally {
                packetForwardJob.cancel()
                client.close()
            }
        }
    }

    /**
     * Accepts an incoming [SyncPacket.FriendRequest] and persists the peer as a friend.
     */
    fun acceptFriendRequest(request: SyncPacket.FriendRequest) {
        scope.launch {
            chatSessionManager.acceptFriendRequest(request.fromUserId, request.fromName)
            val self = identityStore.identity.value
            val acceptPacket = SyncPacket.FriendAccept(
                fromUserId = self.userId,
                fromName = self.displayName,
            )
            // Also broadcast on socialHostTransport in case activeTransport was switched to a client socket
            if (socialClientTransport != null) {
                socialHostTransport?.send(acceptPacket)
            }
        }
    }

    /**
     * Declines an incoming [SyncPacket.FriendRequest].
     */
    fun declineFriendRequest(request: SyncPacket.FriendRequest) {
        chatSessionManager.declineFriendRequest(request.fromUserId)
    }

    /**
     * Normalizes [userId] to uppercase `KMP-XXXX` and persists it as a manual friend if valid.
     */
    @OptIn(kotlin.time.ExperimentalTime::class)
    fun addManualFriendById(userId: String) {
        val normalized = userId.trim().uppercase()
        if (!Regex("^KMP-[A-Z0-9]{4}$").matches(normalized)) return
        if (normalized == identityStore.identity.value.userId) return
        val nowMs = kotlin.time.Clock.System.now().toEpochMilliseconds()
        identityStore.addOrUpdateFriend(
            userId = normalized,
            displayName = normalized,
            nowMs = nowMs,
        )
    }

    /**
     * Offers [file] (up to 1 GB) to the currently active chat peer.
     */
    fun offerFileToActivePeer(file: PlatformFileHandle) {
        scope.launch {
            chatSessionManager.offerFile(file)
        }
    }

    /**
     * Accepts or declines an incoming file transfer offer identified by [transferId].
     */
    fun respondToFileOffer(transferId: String, accept: Boolean) {
        scope.launch {
            chatSessionManager.respondToFileOffer(transferId = transferId, accept = accept)
        }
    }

    /**
     * Cancels an active or pending file transfer identified by [transferId].
     */
    fun cancelFileTransfer(transferId: String) {
        chatSessionManager.cancelTransfer(transferId)
    }

    /**
     * Challenges [peer] to a P2P [gameType] match by sending a [SyncPacket.GameInvite]
     * with a deterministic seed and listening for [SyncPacket.GameInviteResponse].
     */
    fun duelPeer(peer: DiscoveredPeer, gameType: GameType = GameType.SUDOKU_6X6) {
        duelInviteJob?.cancel()
        val seed = Fnv1a.hash32("CHALLENGE:${peer.userId}:${matiksNonce++}")
        val self = identityStore.identity.value
        val inviteId = "invite-${self.userId}-$seed"

        val activePeer = chatSessionManager.uiState.value.activePeer
        val existingClient = socialClientTransport
        val isAlreadyConnected = activePeer?.userId == peer.userId &&
            existingClient != null &&
            existingClient.connectionState.value == TransportConnectionState.CONNECTED

        val client = if (isAlreadyConnected) {
            existingClient!!
        } else {
            socialClientTransport?.close()
            val newClient = transportFactory.createClient(scope)
            socialClientTransport = newClient
            chatSessionManager.openSession(peer, newClient)
            newClient
        }

        pendingOutgoingInvite = PendingOutgoingInvite(
            inviteId = inviteId,
            gameType = gameType,
            seed = seed,
            transport = client,
        )

        duelInviteJob = scope.launch {
            val responseJob = launch {
                val response = client.incomingPackets.first {
                    it is SyncPacket.GameInviteResponse && it.inviteId == inviteId
                } as SyncPacket.GameInviteResponse
                val pending = pendingOutgoingInvite
                if (pending != null && pending.inviteId == response.inviteId) {
                    pendingOutgoingInvite = null
                    if (response.accepted) {
                        launchP2pChallengeGame(
                            gameType = pending.gameType,
                            seed = pending.seed,
                            transport = pending.transport,
                            isHostSide = false,
                        )
                    }
                }
            }

            if (!isAlreadyConnected) {
                client.connect(
                    host = peer.hostIp,
                    port = peer.ktorPort,
                    roomCode = SOCIAL_ROOM_CODE,
                    role = PlayerRole.ANALYST,
                )
                val state = client.connectionState.first {
                    it == TransportConnectionState.CONNECTED ||
                        it == TransportConnectionState.ERROR ||
                        it == TransportConnectionState.DISCONNECTED
                }
                if (state != TransportConnectionState.CONNECTED) {
                    pendingOutgoingInvite = null
                    responseJob.cancel()
                    return@launch
                }
            }

            chatSessionManager.sendGameInvite(
                inviteId = inviteId,
                gameType = gameType,
                seed = seed,
            )
        }
    }

    /**
     * Accepts an incoming [SyncPacket.GameInvite], replies to the challenger, and launches the game.
     */
    fun acceptGameInvite(invite: SyncPacket.GameInvite) {
        val transport: SyncTransport? = socialClientTransport ?: socialHostTransport
        val isHostSide = socialClientTransport == null
        chatSessionManager.clearPendingGameInvite()
        scope.launch {
            chatSessionManager.sendGameInviteResponse(inviteId = invite.inviteId, accepted = true)
            if (transport != null) {
                launchP2pChallengeGame(
                    gameType = invite.gameType,
                    seed = invite.seed,
                    transport = transport,
                    isHostSide = isHostSide,
                )
            }
        }
    }

    /**
     * Declines an incoming [SyncPacket.GameInvite] and clears it from state.
     */
    fun declineGameInvite(invite: SyncPacket.GameInvite) {
        chatSessionManager.clearPendingGameInvite()
        scope.launch {
            chatSessionManager.sendGameInviteResponse(inviteId = invite.inviteId, accepted = false)
        }
    }

    /**
     * Returns to the Games Hub and cleanly stops any active gameplay, hosting, or discovery jobs.
     */
    fun openHub() {
        AppLoggerBridge.logScreen("SCREEN_HUB")
        stopLanActivity()
        disposeCurrentSession()
        _uiState.update { current ->
            current.copy(
                screen = AppScreen.HUB,
                gameMode = GameMode.SOLO_SPLIT,
                nearbyRooms = emptyList(),
                lastSolveReward = null,
                hostAddress = null,
                hostInviteToken = "",
                connectedClients = 0,
                clientConnectionState = TransportConnectionState.DISCONNECTED,
                clientValidationError = null,
            )
        }
    }

    /**
     * Launches the 6×6 Medium Daily Sprint Sudoku challenge.
     */
    fun startDailySprint() {
        AppLoggerBridge.logEvent("game_start", mapOf("game_type" to "SUDOKU_DAILY_SPRINT"))
        val progression = progressionStore.state.value
        val seed = Fnv1a.hash32("DAILY_SPRINT:${progression.streakDays}:${progression.solvedCount}")
        launchSoloSudoku(
            size = SudokuSize.SPRINT_6X6,
            difficulty = SudokuDifficulty.MEDIUM,
            seed = seed,
            isDailySprint = true,
        )
    }

    /**
     * Launches solo Sudoku practice for [size] and [difficulty].
     */
    fun startSudokuSolo(
        size: SudokuSize,
        difficulty: SudokuDifficulty = SudokuDifficulty.MEDIUM,
    ) {
        AppLoggerBridge.logEvent("game_start", mapOf("game_type" to "SUDOKU", "size" to size.name, "difficulty" to difficulty.name))
        sudokuNonce++
        val seed = Fnv1a.hash32("SUDOKU_SOLO:${size.name}:${difficulty.name}:$sudokuNonce")
        launchSoloSudoku(
            size = size,
            difficulty = difficulty,
            seed = seed,
            isDailySprint = false,
        )
    }

    /**
     * Updates the selected Sudoku practice/race [difficulty].
     */
    fun selectSudokuDifficulty(difficulty: SudokuDifficulty) {
        val current = _uiState.value
        if (current.selectedSudokuDifficulty == difficulty) return
        _uiState.update { it.copy(selectedSudokuDifficulty = difficulty) }
        if (current.screen == AppScreen.LOCAL_LINK &&
            current.linkTab == LinkTab.HOST &&
            (current.selectedGameType == GameType.SUDOKU_6X6 || current.selectedGameType == GameType.SUDOKU_9X9)
        ) {
            enterLocalLinkTab(tab = LinkTab.HOST, gameType = current.selectedGameType)
        }
    }

    /**
     * Opens the 2-Device Local Link lobby preselected for 1v1 6×6 Sudoku Race on [initialTab].
     */
    fun openSudokuRaceLink(initialTab: LinkTab = LinkTab.HOST) {
        openLocalLink(gameType = GameType.SUDOKU_6X6, initialTab = initialTab)
    }

    /**
     * Opens the Stats & Cognitive Profile screen.
     */
    fun openStatsProfile() {
        AppLoggerBridge.logScreen("SCREEN_PROFILE")
        stopLanActivity()
        disposeCurrentSession()
        _uiState.update { current ->
            current.copy(
                screen = AppScreen.STATS_PROFILE,
                nearbyRooms = emptyList(),
                lastSolveReward = null,
                isTriathlonActive = false,
                isDailyWorkoutRun = false,
                hostAddress = null,
                hostInviteToken = "",
                connectedClients = 0,
                clientConnectionState = TransportConnectionState.DISCONNECTED,
                clientValidationError = null,
            )
        }
    }

    /**
     * Launches the next uncompleted step of Today's 4-step Daily Brain Workout
     * (`Cross Math` -> `Math Maze` -> `KenKen` -> `Mindsnap`).
     */
    fun startDailyWorkoutStep() {
        val stepIndex = progressionStore.state.value.dailyStepsCompleted
            .coerceIn(0, ProgressionStore.DAILY_WORKOUT_ORDER.lastIndex)
        val gameType = ProgressionStore.DAILY_WORKOUT_ORDER[stepIndex]
        val seed = Fnv1a.hash32("DAILY_WORKOUT:${progressionStore.state.value.streakDays}:$stepIndex:${gameType.code}")
        when (gameType) {
            GameType.CROSS_MATH -> startCrossMath(CrossMathSize.GRID_3X3, isDailyStep = true, seedOverride = seed)
            GameType.MATH_MAZE -> startMathMaze(dimension = 4, isDailyStep = true, seedOverride = seed)
            GameType.KENKEN -> startKenKen(KenKenSize.GRID_4X4, isDailyStep = true, seedOverride = seed)
            GameType.MINDSNAP -> startMindsnap(isDailyStep = true, seedOverride = seed)
            else -> startCrossMath(CrossMathSize.GRID_3X3, isDailyStep = true, seedOverride = seed)
        }
    }

    /** Launches Cross Math solo or daily workout step. */
    fun startCrossMath(
        size: CrossMathSize = CrossMathSize.GRID_3X3,
        isDailyStep: Boolean = false,
        seedOverride: UInt? = null,
    ) {
        AppLoggerBridge.logEvent("game_start", mapOf("game_type" to "CROSS_MATH", "size" to size.name, "is_daily" to isDailyStep))
        stopLanActivity()
        disposeCurrentSession()
        matiksNonce++
        val seed = seedOverride ?: Fnv1a.hash32("CROSS_MATH:${size.name}:$matiksNonce")
        val loopback = InMemorySyncPipe().localLoopback
        soloPipeHost = loopback
        val engine = CrossMathEngine(
            seed = seed,
            size = size,
            isRace = false,
            isDailyStep = isDailyStep,
            transport = loopback,
            scope = scope,
        )
        activeCrossMathEngine = engine
        _uiState.update {
            it.copy(
                screen = AppScreen.CROSS_MATH,
                gameMode = GameMode.SOLO_SPLIT,
                selectedGameType = GameType.CROSS_MATH,
                crossMathState = engine.state.value,
                isTriathlonActive = false,
                triathlonRoundIndex = 0,
                isDailyWorkoutRun = isDailyStep,
                lastSolveReward = null,
            )
        }
        observeActiveSession()
    }

    /** Launches Math Maze solo or daily workout step. */
    fun startMathMaze(
        dimension: Int = 4,
        isDailyStep: Boolean = false,
        seedOverride: UInt? = null,
    ) {
        AppLoggerBridge.logEvent("game_start", mapOf("game_type" to "MATH_MAZE", "dimension" to dimension, "is_daily" to isDailyStep))
        stopLanActivity()
        disposeCurrentSession()
        matiksNonce++
        val seed = seedOverride ?: Fnv1a.hash32("MATH_MAZE:$dimension:$matiksNonce")
        val loopback = InMemorySyncPipe().localLoopback
        soloPipeHost = loopback
        val engine = MathMazeEngine(
            seed = seed,
            dimension = dimension,
            isRace = false,
            isDailyStep = isDailyStep,
            transport = loopback,
            scope = scope,
        )
        activeMathMazeEngine = engine
        _uiState.update {
            it.copy(
                screen = AppScreen.MATH_MAZE,
                gameMode = GameMode.SOLO_SPLIT,
                selectedGameType = GameType.MATH_MAZE,
                mathMazeState = engine.state.value,
                isTriathlonActive = false,
                triathlonRoundIndex = 0,
                isDailyWorkoutRun = isDailyStep,
                lastSolveReward = null,
            )
        }
        observeActiveSession()
    }

    /** Launches KenKen / Calcudoku solo or daily workout step. */
    fun startKenKen(
        size: KenKenSize = KenKenSize.GRID_4X4,
        isDailyStep: Boolean = false,
        seedOverride: UInt? = null,
    ) {
        AppLoggerBridge.logEvent("game_start", mapOf("game_type" to "KENKEN", "size" to size.name, "is_daily" to isDailyStep))
        stopLanActivity()
        disposeCurrentSession()
        matiksNonce++
        val seed = seedOverride ?: Fnv1a.hash32("KENKEN:${size.name}:$matiksNonce")
        val loopback = InMemorySyncPipe().localLoopback
        soloPipeHost = loopback
        val engine = KenKenEngine(
            seed = seed,
            size = size,
            isRace = false,
            isDailyStep = isDailyStep,
            transport = loopback,
            scope = scope,
        )
        activeKenKenEngine = engine
        _uiState.update {
            it.copy(
                screen = AppScreen.KENKEN,
                gameMode = GameMode.SOLO_SPLIT,
                selectedGameType = GameType.KENKEN,
                kenKenState = engine.state.value,
                isTriathlonActive = false,
                triathlonRoundIndex = 0,
                isDailyWorkoutRun = isDailyStep,
                lastSolveReward = null,
            )
        }
        observeActiveSession()
    }

    /** Launches Memory Matrix + Math Recall solo. */
    fun startMemoryMatrix(
        dimension: Int = 4,
        isDailyStep: Boolean = false,
        seedOverride: UInt? = null,
    ) {
        AppLoggerBridge.logEvent("game_start", mapOf("game_type" to "MEMORY_MATRIX", "dimension" to dimension, "is_daily" to isDailyStep))
        stopLanActivity()
        disposeCurrentSession()
        matiksNonce++
        val seed = seedOverride ?: Fnv1a.hash32("MEMORY_MATRIX:$dimension:$matiksNonce")
        val loopback = InMemorySyncPipe().localLoopback
        soloPipeHost = loopback
        val engine = MemoryMatrixEngine(
            seed = seed,
            dimension = dimension,
            isRace = false,
            isDailyStep = isDailyStep,
            transport = loopback,
            scope = scope,
        )
        activeMemoryMatrixEngine = engine
        _uiState.update {
            it.copy(
                screen = AppScreen.MEMORY_MATRIX,
                gameMode = GameMode.SOLO_SPLIT,
                selectedGameType = GameType.MEMORY_MATRIX,
                memoryMatrixState = engine.state.value,
                isTriathlonActive = false,
                triathlonRoundIndex = 0,
                isDailyWorkoutRun = isDailyStep,
                lastSolveReward = null,
            )
        }
        observeActiveSession()
    }

    /** Launches 60-Second Mindsnap Blitz solo or daily workout step. */
    fun startMindsnap(
        isDailyStep: Boolean = false,
        seedOverride: UInt? = null,
    ) {
        AppLoggerBridge.logEvent("game_start", mapOf("game_type" to "MINDSNAP", "is_daily" to isDailyStep))
        stopLanActivity()
        disposeCurrentSession()
        matiksNonce++
        val seed = seedOverride ?: Fnv1a.hash32("MINDSNAP:$matiksNonce")
        val loopback = InMemorySyncPipe().localLoopback
        soloPipeHost = loopback
        val engine = MindsnapEngine(
            seed = seed,
            isRace = false,
            isDailyStep = isDailyStep,
            transport = loopback,
            scope = scope,
        )
        activeMindsnapEngine = engine
        _uiState.update {
            it.copy(
                screen = AppScreen.MINDSNAP,
                gameMode = GameMode.SOLO_SPLIT,
                selectedGameType = GameType.MINDSNAP,
                mindsnapState = engine.state.value,
                isTriathlonActive = false,
                triathlonRoundIndex = 0,
                isDailyWorkoutRun = isDailyStep,
                lastSolveReward = null,
            )
        }
        observeActiveSession()
    }

    // Cross Math actions
    fun selectCrossMathCell(index: Int) { activeCrossMathEngine?.selectCell(index) }
    fun placeCrossMathBankTile(bankTileId: Int) {
        val e = activeCrossMathEngine ?: return
        scope.launch { e.placeBankTile(bankTileId) }
    }
    fun clearCrossMathCell(index: Int) {
        val e = activeCrossMathEngine ?: return
        scope.launch { e.clearCell(index) }
    }
    fun useCrossMathHint() {
        val e = activeCrossMathEngine ?: return
        if (e.state.value.isSolved || !progressionStore.consumeHintToken()) return
        scope.launch { e.applyHint() }
    }
    fun restartCrossMath() {
        val e = activeCrossMathEngine ?: return
        matiksNonce++
        _uiState.update { it.copy(lastSolveReward = null) }
        e.restart(Fnv1a.hash32("CROSS_MATH_R:$matiksNonce"))
    }

    // Math Maze actions
    fun tapMathMazeCell(index: Int) {
        val e = activeMathMazeEngine ?: return
        scope.launch { e.tapCell(index) }
    }
    fun stepBackMathMaze() {
        val e = activeMathMazeEngine ?: return
        scope.launch { e.stepBack() }
    }
    fun useMathMazeHint() {
        val e = activeMathMazeEngine ?: return
        if (e.state.value.isSolved || !progressionStore.consumeHintToken()) return
        scope.launch { e.applyHint() }
    }
    fun restartMathMaze() {
        val e = activeMathMazeEngine ?: return
        matiksNonce++
        _uiState.update { it.copy(lastSolveReward = null) }
        e.restart(Fnv1a.hash32("MATH_MAZE_R:$matiksNonce"))
    }

    // KenKen actions
    fun selectKenKenCell(index: Int) { activeKenKenEngine?.selectCell(index) }
    fun enterKenKenDigit(digit: Int) {
        val e = activeKenKenEngine ?: return
        scope.launch { e.enterDigit(digit) }
    }
    fun eraseKenKenCell() {
        val e = activeKenKenEngine ?: return
        scope.launch { e.eraseSelected() }
    }
    fun useKenKenHint() {
        val e = activeKenKenEngine ?: return
        if (e.state.value.isSolved || !progressionStore.consumeHintToken()) return
        scope.launch { e.applyHint() }
    }
    fun restartKenKen() {
        val e = activeKenKenEngine ?: return
        matiksNonce++
        _uiState.update { it.copy(lastSolveReward = null) }
        e.restart(Fnv1a.hash32("KENKEN_R:$matiksNonce"))
    }

    // Memory Matrix actions
    fun tapMemoryMatrixCell(index: Int) {
        val e = activeMemoryMatrixEngine ?: return
        scope.launch { e.tapCell(index) }
    }
    fun useMemoryMatrixPeek() {
        val e = activeMemoryMatrixEngine ?: return
        if (e.state.value.isSolved || !progressionStore.consumeHintToken()) return
        e.applyPeekHint()
    }
    fun restartMemoryMatrix() {
        val e = activeMemoryMatrixEngine ?: return
        matiksNonce++
        _uiState.update { it.copy(lastSolveReward = null) }
        e.restart(Fnv1a.hash32("MEMORY_MATRIX_R:$matiksNonce"))
    }

    // Mindsnap actions
    fun submitMindsnapAnswer(choiceIndex: Int) {
        val e = activeMindsnapEngine ?: return
        scope.launch { e.submitAnswer(choiceIndex) }
    }
    fun useMindsnapHint() {
        val e = activeMindsnapEngine ?: return
        if (e.state.value.isFinished || !progressionStore.consumeHintToken()) return
        scope.launch { e.applyHint() }
    }
    fun restartMindsnap() {
        val e = activeMindsnapEngine ?: return
        matiksNonce++
        _uiState.update { it.copy(lastSolveReward = null) }
        e.restart(Fnv1a.hash32("MINDSNAP_R:$matiksNonce"))
    }

    /**
     * Selects cell [index] on the active Sudoku grid.
     */
    fun selectSudokuCell(index: Int) {
        activeSudokuEngine()?.selectCell(index)
    }

    /**
     * Enters [digit] into the selected cell of the active Sudoku session.
     */
    fun enterSudokuDigit(digit: Int) {
        val engine = activeSudokuEngine() ?: return
        scope.launch {
            engine.enterDigit(digit)
        }
    }

    /**
     * Erases the selected user-entered cell in the active Sudoku session.
     */
    fun eraseSudokuCell() {
        val engine = activeSudokuEngine() ?: return
        scope.launch {
            engine.eraseSelected()
        }
    }

    /**
     * Consumes one Hint Token from [ProgressionStore] and applies a logical hint on the active Sudoku board.
     */
    fun useSudokuHint() {
        val engine = activeSudokuEngine() ?: return
        if (engine.state.value.isSolved) return
        if (!progressionStore.consumeHintToken()) return
        engine.applyHint()
    }

    /**
     * Dismisses the solve reward scorecard overlay.
     */
    fun dismissSolveReward() {
        _uiState.update { it.copy(lastSolveReward = null) }
    }

    /**
     * Restarts the active Sudoku board with a fresh deterministic seed.
     */
    fun restartSudoku() {
        val engine = activeSudokuEngine() ?: return
        val current = engine.state.value
        sudokuNonce++
        val newSeed = Fnv1a.hash32("SUDOKU_RESTART:${current.size.name}:${current.difficulty.name}:$sudokuNonce")
        _uiState.update { it.copy(lastSolveReward = null) }
        engine.restart(newSeed)
    }

    /**
     * Purchases a Streak Freeze in the Vault Shop if affordable.
     */
    fun buyStreakFreeze() {
        progressionStore.buyStreakFreeze()
    }

    /**
     * Purchases a Hint Token in the Vault Shop if affordable.
     */
    fun buyHintToken() {
        progressionStore.buyHintToken()
    }

    /**
     * Purchases (if locked) or equips [theme] in the Vault Shop.
     */
    fun buyOrSelectTheme(theme: BoardTheme) {
        progressionStore.buyOrSelectTheme(theme)
    }

    /**
     * Launches Co-op Math Deduction in single-device split-screen mode.
     */
    fun startMathSolo() {
        stopLanActivity()
        disposeCurrentSession()

        val (pipeHost, pipeClient) = InMemorySyncPipe.createPair()
        soloPipeHost = pipeHost
        soloPipeClient = pipeClient

        val solver = MathPuzzleEngine(
            role = PlayerRole.OPERATOR,
            seed = SOLO_PUZZLE_SEED,
            transport = pipeHost,
            scope = scope,
        )
        val clue = MathPuzzleEngine(
            role = PlayerRole.ANALYST,
            seed = SOLO_PUZZLE_SEED,
            transport = pipeClient,
            scope = scope,
        )
        soloMathSolverEngine = solver
        soloMathClueEngine = clue

        _uiState.update { current ->
            current.copy(
                screen = AppScreen.MATH_SOLO,
                gameMode = GameMode.SOLO_SPLIT,
                selectedGameType = GameType.MATH,
                nearbyRooms = emptyList(),
                mathSolverState = solver.state.value,
                mathClueState = clue.state.value,
                activeRole = PlayerRole.OPERATOR,
                hostAddress = null,
                hostInviteToken = "",
                connectedClients = 0,
                clientConnectionState = TransportConnectionState.DISCONNECTED,
                clientValidationError = null,
            )
        }
        observeActiveSession()
    }

    /**
     * Opens the unified 2-Device Local Link lobby for [gameType] on [initialTab].
     */
    fun openLocalLink(gameType: GameType, initialTab: LinkTab = LinkTab.HOST) {
        _uiState.update { current ->
            current.copy(
                selectedGameType = gameType,
                linkTab = initialTab,
                clientValidationError = null,
            )
        }
        enterLocalLinkTab(tab = initialTab, gameType = gameType)
    }

    /**
     * Switches between [LinkTab.HOST] and [LinkTab.JOIN] within the 2-Device Local Link screen.
     */
    fun selectLinkTab(tab: LinkTab) {
        val current = _uiState.value
        if (current.screen == AppScreen.LOCAL_LINK && current.linkTab == tab) return
        _uiState.update {
            it.copy(
                linkTab = tab,
                clientValidationError = null,
            )
        }
        enterLocalLinkTab(tab = tab, gameType = current.selectedGameType)
    }

    /**
     * Selects the [GameType] on the Local Link lobby, restarting the host session if currently hosting.
     */
    fun selectGameType(gameType: GameType) {
        val current = _uiState.value
        if (current.selectedGameType == gameType) return
        _uiState.update {
            it.copy(
                selectedGameType = gameType,
                clientValidationError = null,
            )
        }
        if (current.screen == AppScreen.LOCAL_LINK && current.linkTab == LinkTab.HOST) {
            enterLocalLinkTab(tab = LinkTab.HOST, gameType = gameType)
        }
    }

    /**
     * One-tap join for an auto-discovered [DiscoveredRoom] from the local Wi-Fi radar.
     */
    fun joinDiscoveredRoom(room: DiscoveredRoom) {
        val endpoint = InviteTokenCodec.formatEndpoint(room.hostIp, room.wsPort)
        val normalizedCode = room.roomCode.trim().uppercase()
        val role = _uiState.value.clientRole

        _uiState.update { current ->
            current.copy(
                selectedGameType = room.gameType,
                clientEndpointInput = endpoint,
                clientRoomCodeInput = normalizedCode,
                clientValidationError = null,
            )
        }

        connectToHost(
            host = room.hostIp,
            port = room.wsPort,
            roomCode = normalizedCode,
            gameType = room.gameType,
            role = role,
        )
    }

    /**
     * Validates manual `"IP:PORT"` + room code or `"CODE@IP:PORT"` invite token and initiates a client connection.
     */
    fun connectManual(input: String, roomCode: String) {
        val parsed = InviteTokenCodec.parse(input)
        if (parsed == null) {
            _uiState.update {
                it.copy(clientValidationError = "Enter IP:Port (e.g. 192.168.1.42:54321) or CODE@IP:Port")
            }
            return
        }

        val resolvedCode = (parsed.roomCode ?: roomCode).trim().uppercase()
        if (resolvedCode.length != ROOM_CODE_LENGTH) {
            _uiState.update {
                it.copy(clientValidationError = "Enter 4-character room code")
            }
            return
        }

        val snapshot = _uiState.value
        val formattedEndpoint = InviteTokenCodec.formatEndpoint(parsed.hostIp, parsed.wsPort)
        _uiState.update { current ->
            current.copy(
                clientEndpointInput = formattedEndpoint,
                clientRoomCodeInput = resolvedCode,
                clientValidationError = null,
            )
        }

        connectToHost(
            host = parsed.hostIp,
            port = parsed.wsPort,
            roomCode = resolvedCode,
            gameType = snapshot.selectedGameType,
            role = snapshot.clientRole,
        )
    }

    /**
     * Fires a torpedo at [cellIndex] (`0..35`) in the active Tactical Battleship match.
     */
    fun fireBattleship(cellIndex: Int) {
        val engine = activeBattleshipEngine() ?: return
        scope.launch {
            engine.fireAt(cellIndex)
        }
    }

    /**
     * Deploys a 2x2 Sonar Scan centered near [cellIndex] (`0..35`) in the active Tactical Battleship match.
     */
    fun sonarBattleship(cellIndex: Int) {
        val engine = activeBattleshipEngine() ?: return
        scope.launch {
            engine.sonarScan(cellIndex)
        }
    }

    /**
     * Increments the tile value at [index] (`0..8`) on the active Math Deduction Solver engine.
     */
    fun onCellTapped(index: Int) {
        val targetEngine = activeMathEngine() ?: return
        scope.launch {
            targetEngine.onCellTapped(index)
        }
    }

    /**
     * Advances the active Math Deduction puzzle to the next deterministic level.
     */
    fun onNextLevel() {
        val targetEngine = activeMathEngine() ?: return
        scope.launch {
            targetEngine.onNextLevel()
        }
    }

    /**
     * Selects which role the LAN host device plays (`OPERATOR` or `ANALYST`).
     */
    fun selectHostRole(role: PlayerRole) {
        val current = _uiState.value
        if (current.hostRole == role) return

        val activeTransport = hostTransport
        if (current.gameMode == GameMode.LAN_HOST && activeTransport != null) {
            val seed = RoomCodeGenerator.toSeed(hostRoomCode)
            when (current.selectedGameType) {
                GameType.MATH -> {
                    sessionJob?.cancel()
                    sessionJob = null
                    hostMathEngine?.dispose()
                    hostBattleshipEngine?.dispose()
                    hostSudokuEngine?.dispose()
                    hostMathEngine = null
                    hostBattleshipEngine = null
                    hostSudokuEngine = null

                    val updatedEngine = MathPuzzleEngine(
                        role = role,
                        seed = seed,
                        transport = activeTransport,
                        scope = scope,
                    )
                    hostMathEngine = updatedEngine
                    val freshState = updatedEngine.state.value
                    _uiState.update {
                        it.copy(
                            hostRole = role,
                            activeRole = role,
                            mathSolverState = freshState,
                            mathClueState = freshState,
                        )
                    }
                    observeActiveSession()
                }

                GameType.BATTLESHIP -> {
                    sessionJob?.cancel()
                    sessionJob = null
                    hostMathEngine?.dispose()
                    hostBattleshipEngine?.dispose()
                    hostSudokuEngine?.dispose()
                    hostMathEngine = null
                    hostBattleshipEngine = null
                    hostSudokuEngine = null

                    val updatedEngine = BattleshipEngine(
                        localRole = role,
                        seed = seed,
                        vsAi = false,
                        transport = activeTransport,
                        scope = scope,
                    )
                    hostBattleshipEngine = updatedEngine
                    _uiState.update {
                        it.copy(
                            hostRole = role,
                            activeRole = role,
                            battleshipState = updatedEngine.state.value,
                        )
                    }
                    observeActiveSession()
                }

                GameType.SUDOKU_6X6,
                GameType.SUDOKU_9X9,
                GameType.CROSS_MATH,
                GameType.MATH_MAZE,
                GameType.KENKEN,
                GameType.MEMORY_MATRIX,
                GameType.MINDSNAP,
                GameType.TRIATHLON,
                -> {
                    _uiState.update { it.copy(hostRole = role) }
                }
            }
        } else {
            _uiState.update { it.copy(hostRole = role) }
        }
    }

    /**
     * Generates a fresh deterministic room code and restarts the LAN host session.
     */
    fun regenerateHostCode() {
        if (!canHostLan) return
        val current = _uiState.value
        if (current.screen == AppScreen.LOCAL_LINK && current.linkTab == LinkTab.HOST) {
            switchToLanHost(targetScreen = AppScreen.LOCAL_LINK, gameType = current.selectedGameType)
        } else {
            hostNonce++
            hostRoomCode = generateRoomCode(hostNonce)
            _uiState.update { it.copy(hostRoomCode = hostRoomCode, hostInviteToken = "") }
        }
    }

    /**
     * Updates the client IP:PORT or invite token text input and clears any prior validation error.
     */
    fun updateClientEndpoint(endpoint: String) {
        _uiState.update {
            it.copy(
                clientEndpointInput = endpoint,
                clientValidationError = null,
            )
        }
    }

    /**
     * Updates the 4-character client room code input and clears any prior validation error.
     */
    fun updateClientRoomCode(code: String) {
        _uiState.update {
            it.copy(
                clientRoomCodeInput = code.uppercase().take(ROOM_CODE_LENGTH),
                clientValidationError = null,
            )
        }
    }

    /**
     * Selects which role the joining client player controls locally.
     */
    fun selectClientRole(role: PlayerRole) {
        _uiState.update { it.copy(clientRole = role) }
    }

    /**
     * Cancels an in-progress client connection and returns to idle join mode on [AppScreen.LOCAL_LINK].
     */
    fun disconnectClient() {
        val gameType = _uiState.value.selectedGameType
        _uiState.update { it.copy(clientValidationError = null, linkTab = LinkTab.JOIN) }
        enterLocalLinkTab(tab = LinkTab.JOIN, gameType = gameType)
    }

    /**
     * Disposes active discovery jobs, coroutines, game engines, and transports.
     */
    fun dispose() {
        progressionJob?.cancel()
        progressionJob = null
        socialHostJob?.cancel()
        socialHostJob = null
        identityJob?.cancel()
        identityJob = null
        friendsJob?.cancel()
        friendsJob = null
        nearbyDevicesJob?.cancel()
        nearbyDevicesJob = null
        chatStateJob?.cancel()
        chatStateJob = null
        friendRequestJob?.cancel()
        friendRequestJob = null
        duelInviteJob?.cancel()
        duelInviteJob = null
        pendingOutgoingInvite = null
        lanDiscovery.stopPeerPresence()
        socialClientTransport?.close()
        socialClientTransport = null
        socialHostTransport?.close()
        socialHostTransport = null
        chatSessionManager.closeSession()
        stopLanActivity()
        disposeCurrentSession()
    }

    private fun startSocialHostAndPresence() {
        val host = transportFactory.createHost(roomCode = SOCIAL_ROOM_CODE, scope = scope)
        socialHostTransport = host
        chatSessionManager.bindTransport(host)
        socialHostJob?.cancel()
        socialHostJob = scope.launch {
            launch {
                host.boundAddress.collect { address ->
                    val port = address?.let { InviteTokenCodec.parse(it)?.wsPort } ?: return@collect
                    socialHostPort = port
                    refreshPeerPresence()
                }
            }
            // Always forward incoming host packets to ChatSessionManager even when a client chat socket is active
            launch {
                host.incomingPackets.collect { packet ->
                    if (socialClientTransport != null) {
                        chatSessionManager.onIncomingPacket(packet)
                    }
                }
            }
        }
    }

    private fun refreshPeerPresence() {
        val state = _uiState.value
        val identity = identityStore.identity.value
        if (identity.privacyMode == PrivacyMode.PRIVATE || !state.isNearbyScanning) {
            lanDiscovery.stopPeerPresence()
            return
        }
        if (!canHostLan || socialHostPort <= 0) return
        val progression = progressionStore.state.value
        val screen = state.screen
        val status = if (screen == AppScreen.HUB || screen == AppScreen.STATS_PROFILE) {
            PeerStatus.ONLINE
        } else {
            PeerStatus.IN_GAME
        }
        lanDiscovery.startPeerPresence(
            userId = identity.userId,
            displayName = identity.displayName,
            ktorPort = socialHostPort,
            status = status,
            elo = progression.elo,
            streakDays = progression.streakDays,
            topTrophy = progression.topTrophyTitle,
        )
    }

    private fun launchP2pChallengeGame(
        gameType: GameType,
        seed: UInt,
        transport: SyncTransport,
        isHostSide: Boolean,
    ) {
        stopLanActivity()
        disposeCurrentSession()
        p2pChallengeTransport = transport

        val gameMode = if (isHostSide) GameMode.LAN_HOST else GameMode.LAN_CLIENT
        val role = if (isHostSide) PlayerRole.OPERATOR else PlayerRole.ANALYST

        when (gameType) {
            GameType.MATH -> {
                val engine = MathPuzzleEngine(
                    role = role,
                    seed = seed,
                    transport = transport,
                    scope = scope,
                )
                if (isHostSide) hostMathEngine = engine else clientMathEngine = engine
                val initialState = engine.state.value
                _uiState.update { current ->
                    current.copy(
                        screen = AppScreen.COOP_ACTIVE,
                        activeTab = MainTab.PLAY,
                        isChatSheetOpen = false,
                        gameMode = gameMode,
                        selectedGameType = GameType.MATH,
                        mathSolverState = initialState,
                        mathClueState = initialState,
                        activeRole = role,
                        isTriathlonActive = false,
                        triathlonRoundIndex = 0,
                        isDailyWorkoutRun = false,
                        lastSolveReward = null,
                    )
                }
            }

            GameType.BATTLESHIP -> {
                val engine = BattleshipEngine(
                    localRole = role,
                    seed = seed,
                    vsAi = false,
                    transport = transport,
                    scope = scope,
                )
                if (isHostSide) hostBattleshipEngine = engine else clientBattleshipEngine = engine
                _uiState.update { current ->
                    current.copy(
                        screen = AppScreen.COOP_ACTIVE,
                        activeTab = MainTab.PLAY,
                        isChatSheetOpen = false,
                        gameMode = gameMode,
                        selectedGameType = GameType.BATTLESHIP,
                        battleshipState = engine.state.value,
                        activeRole = role,
                        isTriathlonActive = false,
                        triathlonRoundIndex = 0,
                        isDailyWorkoutRun = false,
                        lastSolveReward = null,
                    )
                }
            }

            GameType.SUDOKU_6X6, GameType.SUDOKU_9X9 -> {
                val size = sudokuSizeForGameType(gameType)
                val difficulty = _uiState.value.selectedSudokuDifficulty
                val engine = SudokuRaceEngine(
                    seed = seed,
                    size = size,
                    difficulty = difficulty,
                    isRace = true,
                    isDailySprint = false,
                    transport = transport,
                    scope = scope,
                )
                if (isHostSide) hostSudokuEngine = engine else clientSudokuEngine = engine
                _uiState.update { current ->
                    current.copy(
                        screen = AppScreen.SUDOKU_RACE,
                        activeTab = MainTab.PLAY,
                        isChatSheetOpen = false,
                        gameMode = gameMode,
                        selectedGameType = gameType,
                        selectedSudokuSize = size,
                        sudokuState = engine.state.value,
                        activeRole = role,
                        isTriathlonActive = false,
                        triathlonRoundIndex = 0,
                        isDailyWorkoutRun = false,
                        lastSolveReward = null,
                    )
                }
            }

            GameType.CROSS_MATH,
            GameType.MATH_MAZE,
            GameType.KENKEN,
            GameType.MEMORY_MATRIX,
            GameType.MINDSNAP,
            GameType.TRIATHLON,
            -> {
                val isTriathlon = gameType == GameType.TRIATHLON
                val initialMode = if (isTriathlon) ProgressionStore.TRIATHLON_ROUNDS.first() else gameType
                setupMatiksRaceEngine(
                    gameType = initialMode,
                    seed = seed,
                    transport = transport,
                    roundIndex = 0,
                )
                _uiState.update { current ->
                    current.copy(
                        screen = activeLanScreenFor(gameType),
                        activeTab = MainTab.PLAY,
                        isChatSheetOpen = false,
                        gameMode = gameMode,
                        selectedGameType = gameType,
                        isTriathlonActive = isTriathlon,
                        triathlonRoundIndex = 0,
                        isDailyWorkoutRun = false,
                        activeRole = role,
                        lastSolveReward = null,
                    )
                }
            }
        }
        observeActiveSession()
    }

    private fun launchSoloSudoku(
        size: SudokuSize,
        difficulty: SudokuDifficulty,
        seed: UInt,
        isDailySprint: Boolean,
    ) {
        stopLanActivity()
        disposeCurrentSession()

        val loopback = InMemorySyncPipe().localLoopback
        soloPipeHost = loopback

        val engine = SudokuRaceEngine(
            seed = seed,
            size = size,
            difficulty = difficulty,
            isRace = false,
            isDailySprint = isDailySprint,
            transport = loopback,
            scope = scope,
        )
        soloSudokuEngine = engine

        _uiState.update { current ->
            current.copy(
                screen = AppScreen.SUDOKU_SOLO,
                gameMode = GameMode.SOLO_SPLIT,
                selectedGameType = gameTypeForSudokuSize(size),
                selectedSudokuSize = size,
                selectedSudokuDifficulty = difficulty,
                nearbyRooms = emptyList(),
                sudokuState = engine.state.value,
                lastSolveReward = null,
                activeRole = PlayerRole.OPERATOR,
                hostAddress = null,
                hostInviteToken = "",
                connectedClients = 0,
                clientConnectionState = TransportConnectionState.DISCONNECTED,
                clientValidationError = null,
            )
        }
        observeActiveSession()
    }

    private fun enterLocalLinkTab(tab: LinkTab, gameType: GameType) {
        when (tab) {
            LinkTab.HOST -> {
                stopObservingNearbyRooms()
                if (canHostLan) {
                    switchToLanHost(targetScreen = AppScreen.LOCAL_LINK, gameType = gameType)
                } else {
                    stopBroadcasting()
                    disposeCurrentSession()
                    _uiState.update { current ->
                        current.copy(
                            screen = AppScreen.LOCAL_LINK,
                            linkTab = LinkTab.HOST,
                            selectedGameType = gameType,
                            nearbyRooms = emptyList(),
                            hostAddress = null,
                            hostInviteToken = "",
                            connectedClients = 0,
                            clientConnectionState = TransportConnectionState.DISCONNECTED,
                        )
                    }
                }
            }

            LinkTab.JOIN -> {
                stopBroadcasting()
                switchToLanClientIdle(targetScreen = AppScreen.LOCAL_LINK, gameType = gameType)
                startObservingNearbyRooms()
            }
        }
    }

    private fun switchToLanHost(targetScreen: AppScreen, gameType: GameType) {
        stopBroadcasting()
        disposeCurrentSession()
        hostNonce++
        hostRoomCode = generateRoomCode(hostNonce)

        val transport = transportFactory.createHost(roomCode = hostRoomCode, scope = scope)
        hostTransport = transport

        val role = _uiState.value.hostRole
        val seed = RoomCodeGenerator.toSeed(hostRoomCode)

        when (gameType) {
            GameType.MATH -> {
                val engine = MathPuzzleEngine(
                    role = role,
                    seed = seed,
                    transport = transport,
                    scope = scope,
                )
                hostMathEngine = engine
                val initialState = engine.state.value
                _uiState.update { current ->
                    current.copy(
                        screen = targetScreen,
                        linkTab = LinkTab.HOST,
                        gameMode = GameMode.LAN_HOST,
                        selectedGameType = GameType.MATH,
                        nearbyRooms = emptyList(),
                        mathSolverState = initialState,
                        mathClueState = initialState,
                        activeRole = role,
                        hostRoomCode = hostRoomCode,
                        hostAddress = transport.boundAddress.value,
                        hostInviteToken = "",
                        connectedClients = transport.connectedClients.value,
                        clientConnectionState = TransportConnectionState.DISCONNECTED,
                    )
                }
            }

            GameType.BATTLESHIP -> {
                val engine = BattleshipEngine(
                    localRole = role,
                    seed = seed,
                    vsAi = false,
                    transport = transport,
                    scope = scope,
                )
                hostBattleshipEngine = engine
                _uiState.update { current ->
                    current.copy(
                        screen = targetScreen,
                        linkTab = LinkTab.HOST,
                        gameMode = GameMode.LAN_HOST,
                        selectedGameType = GameType.BATTLESHIP,
                        nearbyRooms = emptyList(),
                        battleshipState = engine.state.value,
                        activeRole = role,
                        hostRoomCode = hostRoomCode,
                        hostAddress = transport.boundAddress.value,
                        hostInviteToken = "",
                        connectedClients = transport.connectedClients.value,
                        clientConnectionState = TransportConnectionState.DISCONNECTED,
                    )
                }
            }

            GameType.SUDOKU_6X6, GameType.SUDOKU_9X9 -> {
                val size = sudokuSizeForGameType(gameType)
                val difficulty = _uiState.value.selectedSudokuDifficulty
                val engine = SudokuRaceEngine(
                    seed = seed,
                    size = size,
                    difficulty = difficulty,
                    isRace = true,
                    isDailySprint = false,
                    transport = transport,
                    scope = scope,
                )
                hostSudokuEngine = engine
                _uiState.update { current ->
                    current.copy(
                        screen = targetScreen,
                        linkTab = LinkTab.HOST,
                        gameMode = GameMode.LAN_HOST,
                        selectedGameType = gameType,
                        selectedSudokuSize = size,
                        nearbyRooms = emptyList(),
                        sudokuState = engine.state.value,
                        lastSolveReward = null,
                        activeRole = PlayerRole.OPERATOR,
                        hostRoomCode = hostRoomCode,
                        hostAddress = transport.boundAddress.value,
                        hostInviteToken = "",
                        connectedClients = transport.connectedClients.value,
                        clientConnectionState = TransportConnectionState.DISCONNECTED,
                    )
                }
            }

            GameType.CROSS_MATH,
            GameType.MATH_MAZE,
            GameType.KENKEN,
            GameType.MEMORY_MATRIX,
            GameType.MINDSNAP,
            GameType.TRIATHLON,
            -> {
                val isTriathlon = gameType == GameType.TRIATHLON
                val initialMode = if (isTriathlon) ProgressionStore.TRIATHLON_ROUNDS.first() else gameType
                setupMatiksRaceEngine(
                    gameType = initialMode,
                    seed = seed,
                    transport = transport,
                    roundIndex = 0,
                )
                _uiState.update { current ->
                    current.copy(
                        screen = targetScreen,
                        linkTab = LinkTab.HOST,
                        gameMode = GameMode.LAN_HOST,
                        selectedGameType = gameType,
                        isTriathlonActive = isTriathlon,
                        triathlonRoundIndex = 0,
                        isDailyWorkoutRun = false,
                        nearbyRooms = emptyList(),
                        lastSolveReward = null,
                        activeRole = PlayerRole.OPERATOR,
                        hostRoomCode = hostRoomCode,
                        hostAddress = transport.boundAddress.value,
                        hostInviteToken = "",
                        connectedClients = transport.connectedClients.value,
                        clientConnectionState = TransportConnectionState.DISCONNECTED,
                    )
                }
            }
        }
        observeActiveSession()
    }

    private fun switchToLanClientIdle(targetScreen: AppScreen, gameType: GameType) {
        disposeCurrentSession()

        val role = _uiState.value.clientRole
        _uiState.update { current ->
            current.copy(
                screen = targetScreen,
                linkTab = LinkTab.JOIN,
                gameMode = GameMode.LAN_CLIENT,
                selectedGameType = gameType,
                activeRole = role,
                hostAddress = null,
                hostInviteToken = "",
                connectedClients = 0,
                clientConnectionState = TransportConnectionState.DISCONNECTED,
            )
        }
    }

    private fun connectToHost(
        host: String,
        port: Int,
        roomCode: String,
        gameType: GameType,
        role: PlayerRole,
    ) {
        stopBroadcasting()
        disposeCurrentSession()

        val transport = transportFactory.createClient(scope = scope)
        clientTransport = transport
        val seed = RoomCodeGenerator.toSeed(roomCode)

        when (gameType) {
            GameType.MATH -> {
                val engine = MathPuzzleEngine(
                    role = role,
                    seed = seed,
                    transport = transport,
                    scope = scope,
                )
                clientMathEngine = engine
                val initialState = engine.state.value
                _uiState.update { current ->
                    current.copy(
                        screen = AppScreen.LOCAL_LINK,
                        linkTab = LinkTab.JOIN,
                        gameMode = GameMode.LAN_CLIENT,
                        selectedGameType = GameType.MATH,
                        mathSolverState = initialState,
                        mathClueState = initialState,
                        activeRole = role,
                        hostAddress = null,
                        hostInviteToken = "",
                        connectedClients = 0,
                        clientConnectionState = transport.connectionState.value,
                        clientValidationError = null,
                    )
                }
            }

            GameType.BATTLESHIP -> {
                val engine = BattleshipEngine(
                    localRole = role,
                    seed = seed,
                    vsAi = false,
                    transport = transport,
                    scope = scope,
                )
                clientBattleshipEngine = engine
                _uiState.update { current ->
                    current.copy(
                        screen = AppScreen.LOCAL_LINK,
                        linkTab = LinkTab.JOIN,
                        gameMode = GameMode.LAN_CLIENT,
                        selectedGameType = GameType.BATTLESHIP,
                        battleshipState = engine.state.value,
                        activeRole = role,
                        hostAddress = null,
                        hostInviteToken = "",
                        connectedClients = 0,
                        clientConnectionState = transport.connectionState.value,
                        clientValidationError = null,
                    )
                }
            }

            GameType.SUDOKU_6X6, GameType.SUDOKU_9X9 -> {
                val size = sudokuSizeForGameType(gameType)
                val difficulty = _uiState.value.selectedSudokuDifficulty
                val engine = SudokuRaceEngine(
                    seed = seed,
                    size = size,
                    difficulty = difficulty,
                    isRace = true,
                    isDailySprint = false,
                    transport = transport,
                    scope = scope,
                )
                clientSudokuEngine = engine
                _uiState.update { current ->
                    current.copy(
                        screen = AppScreen.LOCAL_LINK,
                        linkTab = LinkTab.JOIN,
                        gameMode = GameMode.LAN_CLIENT,
                        selectedGameType = gameType,
                        selectedSudokuSize = size,
                        sudokuState = engine.state.value,
                        lastSolveReward = null,
                        activeRole = role,
                        hostAddress = null,
                        hostInviteToken = "",
                        connectedClients = 0,
                        clientConnectionState = transport.connectionState.value,
                        clientValidationError = null,
                    )
                }
            }

            GameType.CROSS_MATH,
            GameType.MATH_MAZE,
            GameType.KENKEN,
            GameType.MEMORY_MATRIX,
            GameType.MINDSNAP,
            GameType.TRIATHLON,
            -> {
                val isTriathlon = gameType == GameType.TRIATHLON
                val initialMode = if (isTriathlon) ProgressionStore.TRIATHLON_ROUNDS.first() else gameType
                setupMatiksRaceEngine(
                    gameType = initialMode,
                    seed = seed,
                    transport = transport,
                    roundIndex = 0,
                )
                _uiState.update { current ->
                    current.copy(
                        screen = AppScreen.LOCAL_LINK,
                        linkTab = LinkTab.JOIN,
                        gameMode = GameMode.LAN_CLIENT,
                        selectedGameType = gameType,
                        isTriathlonActive = isTriathlon,
                        triathlonRoundIndex = 0,
                        isDailyWorkoutRun = false,
                        lastSolveReward = null,
                        activeRole = role,
                        hostAddress = null,
                        hostInviteToken = "",
                        connectedClients = 0,
                        clientConnectionState = transport.connectionState.value,
                        clientValidationError = null,
                    )
                }
            }
        }

        observeActiveSession()
        transport.connect(
            host = host,
            port = port,
            roomCode = roomCode,
            role = role,
        )
    }

    private fun observeActiveSession() {
        val activeMathSolver = soloMathSolverEngine
        val activeMathClue = soloMathClueEngine
        val activeHostMath = hostMathEngine
        val activeClientMath = clientMathEngine
        val activeSoloBattleship = soloBattleshipEngine
        val activeHostBattleship = hostBattleshipEngine
        val activeClientBattleship = clientBattleshipEngine
        val activeSudoku = soloSudokuEngine ?: hostSudokuEngine ?: clientSudokuEngine
        val activeHostSudoku = hostSudokuEngine
        val activeCrossMath = activeCrossMathEngine
        val activeMathMaze = activeMathMazeEngine
        val activeKenKen = activeKenKenEngine
        val activeMemoryMatrix = activeMemoryMatrixEngine
        val activeMindsnap = activeMindsnapEngine
        val activeHost = hostTransport
        val activeClient = clientTransport

        sessionJob = scope.launch {
            if (activeMathSolver != null) {
                launch {
                    activeMathSolver.state.collect { state ->
                        _uiState.update { it.copy(mathSolverState = state) }
                    }
                }
            }
            if (activeMathClue != null) {
                launch {
                    activeMathClue.state.collect { state ->
                        _uiState.update { it.copy(mathClueState = state) }
                    }
                }
            }
            if (activeHostMath != null) {
                launch {
                    activeHostMath.state.collect { state ->
                        _uiState.update { it.copy(mathSolverState = state, mathClueState = state) }
                    }
                }
            }
            if (activeClientMath != null) {
                launch {
                    activeClientMath.state.collect { state ->
                        _uiState.update { it.copy(mathSolverState = state, mathClueState = state) }
                    }
                }
            }
            if (activeSoloBattleship != null) {
                launch {
                    activeSoloBattleship.state.collect { state ->
                        _uiState.update { it.copy(battleshipState = state) }
                    }
                }
            }
            if (activeHostBattleship != null) {
                launch {
                    activeHostBattleship.state.collect { state ->
                        _uiState.update { it.copy(battleshipState = state) }
                    }
                }
            }
            if (activeClientBattleship != null) {
                launch {
                    activeClientBattleship.state.collect { state ->
                        _uiState.update { it.copy(battleshipState = state) }
                    }
                }
            }
            if (activeSudoku != null) {
                launch {
                    observeSudokuState(activeSudoku)
                }
            }
            if (activeCrossMath != null) {
                launch { observeCrossMathState(activeCrossMath) }
            }
            if (activeMathMaze != null) {
                launch { observeMathMazeState(activeMathMaze) }
            }
            if (activeKenKen != null) {
                launch { observeKenKenState(activeKenKen) }
            }
            if (activeMemoryMatrix != null) {
                launch { observeMemoryMatrixState(activeMemoryMatrix) }
            }
            if (activeMindsnap != null) {
                launch { observeMindsnapState(activeMindsnap) }
            }

            if (activeHost != null) {
                launch {
                    activeHost.boundAddress.collect { addr ->
                        val parsed = addr?.let(::parseHostPort)
                        val gameType = _uiState.value.selectedGameType
                        val inviteToken = if (parsed != null) {
                            InviteTokenCodec.format(hostRoomCode, parsed.host, parsed.port)
                        } else {
                            ""
                        }
                        _uiState.update {
                            it.copy(
                                hostAddress = addr,
                                hostInviteToken = inviteToken,
                            )
                        }
                        if (parsed != null &&
                            _uiState.value.screen == AppScreen.LOCAL_LINK &&
                            _uiState.value.linkTab == LinkTab.HOST
                        ) {
                            val signature = BroadcastSignature(hostRoomCode, parsed.port, gameType)
                            if (activeBroadcastSignature != signature) {
                                activeBroadcastSignature = signature
                                lanDiscovery.startBroadcasting(
                                    roomCode = hostRoomCode,
                                    wsPort = parsed.port,
                                    gameType = gameType,
                                )
                            }
                        }
                    }
                }

                launch {
                    activeHost.connectedClients.collect { count ->
                        var transitionedToActive = false
                        _uiState.update { current ->
                            val shouldAdvance = count > 0 &&
                                current.gameMode == GameMode.LAN_HOST &&
                                current.screen == AppScreen.LOCAL_LINK
                            if (shouldAdvance) {
                                transitionedToActive = true
                            }
                            current.copy(
                                connectedClients = count,
                                screen = if (shouldAdvance) {
                                    activeLanScreenFor(current.selectedGameType)
                                } else {
                                    current.screen
                                },
                            )
                        }
                        if (transitionedToActive) {
                            stopBroadcasting()
                            if (activeHostSudoku != null) {
                                sudokuNonce++
                                val raceSeed = Fnv1a.hash32("SUDOKU_RACE:$hostRoomCode:$sudokuNonce")
                                activeHostSudoku.restart(raceSeed)
                            }
                        }
                    }
                }
            }

            if (activeClient != null) {
                launch {
                    activeClient.connectionState.collect { connState ->
                        var transitionedToActive = false
                        _uiState.update { current ->
                            val shouldAdvance = connState == TransportConnectionState.CONNECTED &&
                                current.gameMode == GameMode.LAN_CLIENT &&
                                current.screen == AppScreen.LOCAL_LINK
                            if (shouldAdvance) {
                                transitionedToActive = true
                            }
                            current.copy(
                                clientConnectionState = connState,
                                screen = if (shouldAdvance) {
                                    activeLanScreenFor(current.selectedGameType)
                                } else {
                                    current.screen
                                },
                            )
                        }
                        if (transitionedToActive) {
                            stopObservingNearbyRooms()
                        }
                    }
                }
            }
        }
    }

    private suspend fun observeSudokuState(engine: SudokuRaceEngine) {
        var previousSolved = engine.state.value.isSolved
        engine.state.collect { state ->
            val justSolved = !previousSolved && state.isSolved
            val wasReset = previousSolved && !state.isSolved
            previousSolved = state.isSolved

            val reward = if (justSolved) {
                val isRaceWin: Boolean? = if (state.isRace) {
                    !state.opponent.finished
                } else {
                    null
                }
                val r = progressionStore.recordSolve(
                    size = state.size,
                    difficulty = state.difficulty,
                    elapsedSeconds = state.elapsedSeconds,
                    mistakes = state.mistakes,
                    isDailySprint = state.isDailySprint,
                    isRaceWin = isRaceWin,
                )
                AppLoggerBridge.logGameCompleted(
                    gameType = if (state.isDailySprint) "SUDOKU_DAILY_SPRINT" else "SUDOKU_${state.size.name}",
                    score = r.xpEarned,
                    timeSeconds = state.elapsedSeconds,
                    eloChange = r.eloDelta,
                )
                r
            } else {
                null
            }

            _uiState.update { current ->
                current.copy(
                    sudokuState = state,
                    lastSolveReward = when {
                        reward != null -> reward
                        wasReset -> null
                        else -> current.lastSolveReward
                    },
                )
            }
        }
    }

    private fun setupMatiksRaceEngine(
        gameType: GameType,
        seed: UInt,
        transport: SyncTransport,
        roundIndex: Int,
    ) {
        when (gameType) {
            GameType.CROSS_MATH -> {
                val e = CrossMathEngine(seed, CrossMathSize.GRID_3X3, isRace = true, roundIndex = roundIndex, transport = transport, scope = scope)
                activeCrossMathEngine = e
                _uiState.update { it.copy(crossMathState = e.state.value) }
            }
            GameType.MATH_MAZE -> {
                val e = MathMazeEngine(seed, dimension = 4, isRace = true, roundIndex = roundIndex, transport = transport, scope = scope)
                activeMathMazeEngine = e
                _uiState.update { it.copy(mathMazeState = e.state.value) }
            }
            GameType.KENKEN -> {
                val e = KenKenEngine(seed, KenKenSize.GRID_4X4, isRace = true, roundIndex = roundIndex, transport = transport, scope = scope)
                activeKenKenEngine = e
                _uiState.update { it.copy(kenKenState = e.state.value) }
            }
            GameType.MEMORY_MATRIX -> {
                val e = MemoryMatrixEngine(seed, dimension = 4, isRace = true, roundIndex = roundIndex, transport = transport, scope = scope)
                activeMemoryMatrixEngine = e
                _uiState.update { it.copy(memoryMatrixState = e.state.value) }
            }
            GameType.MINDSNAP -> {
                val e = MindsnapEngine(seed, isRace = true, roundIndex = roundIndex, transport = transport, scope = scope)
                activeMindsnapEngine = e
                _uiState.update { it.copy(mindsnapState = e.state.value) }
            }
            else -> Unit
        }
    }

    private suspend fun observeCrossMathState(engine: CrossMathEngine) {
        var prevSolved = engine.state.value.isSolved
        engine.state.collect { state ->
            val justSolved = !prevSolved && state.isSolved
            prevSolved = state.isSolved
            val reward = if (justSolved) {
                val isWin = if (state.isRace) state.opponentProgress?.finished != true else null
                val effectiveType = if (_uiState.value.isTriathlonActive) GameType.TRIATHLON else GameType.CROSS_MATH
                val r = progressionStore.recordModeSolve(
                    gameType = effectiveType,
                    elapsedSeconds = state.elapsedSeconds,
                    moves = state.moves,
                    mistakes = state.mistakes,
                    parSeconds = 75,
                    isDailyStep = state.isDailyStep,
                    isRaceWin = isWin,
                )
                AppLoggerBridge.logGameCompleted(
                    gameType = effectiveType.name,
                    score = r.xpEarned,
                    timeSeconds = state.elapsedSeconds,
                    eloChange = r.eloDelta,
                )
                r
            } else null
            _uiState.update {
                it.copy(
                    crossMathState = state,
                    lastSolveReward = reward ?: it.lastSolveReward,
                )
            }
        }
    }

    private suspend fun observeMathMazeState(engine: MathMazeEngine) {
        var prevSolved = engine.state.value.isSolved
        engine.state.collect { state ->
            val justSolved = !prevSolved && state.isSolved
            prevSolved = state.isSolved
            val reward = if (justSolved) {
                val isWin = if (state.isRace) state.opponentProgress?.finished != true else null
                val r = progressionStore.recordModeSolve(
                    gameType = GameType.MATH_MAZE,
                    elapsedSeconds = state.elapsedSeconds,
                    moves = state.moves,
                    mistakes = state.mistakes,
                    parSeconds = 45,
                    isDailyStep = state.isDailyStep,
                    isRaceWin = isWin,
                )
                AppLoggerBridge.logGameCompleted(
                    gameType = "MATH_MAZE",
                    score = r.xpEarned,
                    timeSeconds = state.elapsedSeconds,
                    eloChange = r.eloDelta,
                )
                r
            } else null
            _uiState.update {
                it.copy(
                    mathMazeState = state,
                    lastSolveReward = reward ?: it.lastSolveReward,
                )
            }
        }
    }

    private suspend fun observeKenKenState(engine: KenKenEngine) {
        var prevSolved = engine.state.value.isSolved
        engine.state.collect { state ->
            val justSolved = !prevSolved && state.isSolved
            prevSolved = state.isSolved
            val reward = if (justSolved) {
                val isWin = if (state.isRace) state.opponentProgress?.finished != true else null
                val r = progressionStore.recordModeSolve(
                    gameType = GameType.KENKEN,
                    elapsedSeconds = state.elapsedSeconds,
                    moves = state.moves,
                    mistakes = state.mistakes,
                    parSeconds = 90,
                    isDailyStep = state.isDailyStep,
                    isRaceWin = isWin,
                )
                AppLoggerBridge.logGameCompleted(
                    gameType = "KENKEN",
                    score = r.xpEarned,
                    timeSeconds = state.elapsedSeconds,
                    eloChange = r.eloDelta,
                )
                r
            } else null
            _uiState.update {
                it.copy(
                    kenKenState = state,
                    lastSolveReward = reward ?: it.lastSolveReward,
                )
            }
        }
    }

    private suspend fun observeMemoryMatrixState(engine: MemoryMatrixEngine) {
        var prevSolved = engine.state.value.isSolved
        engine.state.collect { state ->
            val justSolved = !prevSolved && state.isSolved
            prevSolved = state.isSolved
            if (justSolved && _uiState.value.isTriathlonActive && _uiState.value.triathlonRoundIndex == 1) {
                _uiState.update { it.copy(memoryMatrixState = state) }
                advanceTriathlonRound(nextRoundIndex = 2)
                return@collect
            }
            val reward = if (justSolved) {
                val isWin = if (state.isRace) state.opponentProgress?.finished != true else null
                val r = progressionStore.recordModeSolve(
                    gameType = GameType.MEMORY_MATRIX,
                    elapsedSeconds = state.elapsedSeconds,
                    moves = state.moves,
                    mistakes = state.mistakes,
                    parSeconds = 30,
                    isDailyStep = state.isDailyStep,
                    isRaceWin = isWin,
                )
                AppLoggerBridge.logGameCompleted(
                    gameType = "MEMORY_MATRIX",
                    score = r.xpEarned,
                    timeSeconds = state.elapsedSeconds,
                    eloChange = r.eloDelta,
                )
                r
            } else null
            _uiState.update {
                it.copy(
                    memoryMatrixState = state,
                    lastSolveReward = reward ?: it.lastSolveReward,
                )
            }
        }
    }

    private suspend fun observeMindsnapState(engine: MindsnapEngine) {
        var prevFinished = engine.state.value.isFinished
        engine.state.collect { state ->
            val justFinished = !prevFinished && state.isFinished
            prevFinished = state.isFinished
            if (justFinished && _uiState.value.isTriathlonActive && _uiState.value.triathlonRoundIndex == 0) {
                _uiState.update { it.copy(mindsnapState = state) }
                advanceTriathlonRound(nextRoundIndex = 1)
                return@collect
            }
            val reward = if (justFinished) {
                val isWin = if (state.isRace) state.opponentProgress?.finished != true else null
                val r = progressionStore.recordModeSolve(
                    gameType = GameType.MINDSNAP,
                    elapsedSeconds = state.elapsedSeconds,
                    moves = state.moves,
                    mistakes = state.mistakes,
                    parSeconds = 40,
                    bestCombo = state.bestCombo,
                    isDailyStep = state.isDailyStep,
                    isRaceWin = isWin,
                )
                AppLoggerBridge.logGameCompleted(
                    gameType = "MINDSNAP",
                    score = r.xpEarned,
                    timeSeconds = state.elapsedSeconds,
                    eloChange = r.eloDelta,
                )
                r
            } else null
            _uiState.update {
                it.copy(
                    mindsnapState = state,
                    lastSolveReward = reward ?: it.lastSolveReward,
                )
            }
        }
    }

    private fun advanceTriathlonRound(nextRoundIndex: Int) {
        val nextType = ProgressionStore.TRIATHLON_ROUNDS.getOrNull(nextRoundIndex) ?: return
        val transport: SyncTransport = hostTransport ?: clientTransport ?: p2pChallengeTransport ?: soloPipeHost ?: return
        val room = _uiState.value.activeRoomCode
        val nextSeed = Fnv1a.hash32("TRIATHLON:$room:$nextRoundIndex")

        activeMindsnapEngine?.dispose()
        activeMemoryMatrixEngine?.dispose()
        activeCrossMathEngine?.dispose()
        activeMindsnapEngine = null
        activeMemoryMatrixEngine = null
        activeCrossMathEngine = null

        setupMatiksRaceEngine(nextType, nextSeed, transport, nextRoundIndex)
        val nextScreen = activeLanScreenFor(nextType)
        _uiState.update {
            it.copy(
                screen = nextScreen,
                triathlonRoundIndex = nextRoundIndex,
            )
        }
        // Observe the newly created round engine
        scope.launch {
            when (nextType) {
                GameType.MEMORY_MATRIX -> activeMemoryMatrixEngine?.let { observeMemoryMatrixState(it) }
                GameType.CROSS_MATH -> activeCrossMathEngine?.let { observeCrossMathState(it) }
                else -> Unit
            }
        }
    }

    private fun startObservingNearbyRooms() {
        stopObservingNearbyRooms()
        nearbyRoomsJob = scope.launch {
            lanDiscovery.observeNearbyRooms().collect { rooms ->
                _uiState.update { current ->
                    if (current.screen == AppScreen.LOCAL_LINK && current.linkTab == LinkTab.JOIN) {
                        current.copy(nearbyRooms = rooms)
                    } else {
                        current
                    }
                }
            }
        }
    }

    private fun stopObservingNearbyRooms() {
        nearbyRoomsJob?.cancel()
        nearbyRoomsJob = null
    }

    private fun stopBroadcasting() {
        activeBroadcastSignature = null
        lanDiscovery.stopBroadcasting()
    }

    private fun stopLanActivity() {
        stopObservingNearbyRooms()
        stopBroadcasting()
    }

    private fun activeSudokuEngine(): SudokuRaceEngine? {
        return when (_uiState.value.gameMode) {
            GameMode.SOLO_SPLIT -> soloSudokuEngine
            GameMode.LAN_HOST -> hostSudokuEngine
            GameMode.LAN_CLIENT -> clientSudokuEngine
        }
    }

    private fun activeMathEngine(): MathPuzzleEngine? {
        return when (_uiState.value.gameMode) {
            GameMode.SOLO_SPLIT -> soloMathSolverEngine
            GameMode.LAN_HOST -> hostMathEngine
            GameMode.LAN_CLIENT -> clientMathEngine
        }
    }

    private fun activeBattleshipEngine(): BattleshipEngine? {
        return when (_uiState.value.gameMode) {
            GameMode.SOLO_SPLIT -> soloBattleshipEngine
            GameMode.LAN_HOST -> hostBattleshipEngine
            GameMode.LAN_CLIENT -> clientBattleshipEngine
        }
    }

    private fun disposeCurrentSession() {
        sessionJob?.cancel()
        sessionJob = null

        soloSudokuEngine?.dispose()
        hostSudokuEngine?.dispose()
        clientSudokuEngine?.dispose()
        soloSudokuEngine = null
        hostSudokuEngine = null
        clientSudokuEngine = null

        soloMathSolverEngine?.dispose()
        soloMathClueEngine?.dispose()
        hostMathEngine?.dispose()
        clientMathEngine?.dispose()
        soloMathSolverEngine = null
        soloMathClueEngine = null
        hostMathEngine = null
        clientMathEngine = null

        soloBattleshipEngine?.dispose()
        hostBattleshipEngine?.dispose()
        clientBattleshipEngine?.dispose()
        soloBattleshipEngine = null
        hostBattleshipEngine = null
        clientBattleshipEngine = null

        activeCrossMathEngine?.dispose()
        activeMathMazeEngine?.dispose()
        activeKenKenEngine?.dispose()
        activeMemoryMatrixEngine?.dispose()
        activeMindsnapEngine?.dispose()
        activeCrossMathEngine = null
        activeMathMazeEngine = null
        activeKenKenEngine = null
        activeMemoryMatrixEngine = null
        activeMindsnapEngine = null

        soloPipeHost?.close()
        soloPipeClient?.close()
        soloPipeHost = null
        soloPipeClient = null

        hostTransport?.close()
        clientTransport?.close()
        hostTransport = null
        clientTransport = null
        p2pChallengeTransport = null
    }

    private fun buildIdleSudokuState(
        size: SudokuSize,
        difficulty: SudokuDifficulty,
    ): SudokuGameState {
        val board = SudokuGenerator.generate(
            size = size,
            difficulty = difficulty,
            seed = SOLO_SUDOKU_SEED,
        )
        return SudokuGameState(
            size = size,
            difficulty = difficulty,
            seed = board.seed,
            givens = board.givens,
            grid = board.givens.toList(),
            solution = board.solution,
        )
    }

    private fun buildIdleMathState(role: PlayerRole): MathGameState {
        val spec = MathLevelGenerator.generate(SOLO_PUZZLE_SEED, 0)
        val initialGrid = spec.initialGrid.toList()
        val clues = MathLevelGenerator.evaluateClues(spec, initialGrid)
        return MathGameState(
            role = role,
            levelIndex = spec.levelIndex,
            title = spec.title,
            grid = initialGrid,
            clues = clues,
            isSolved = clues.all { it.isSatisfied },
        )
    }

    private fun buildIdleBattleshipState(): BattleshipGameState {
        val pipe = InMemorySyncPipe().localLoopback
        val engine = BattleshipEngine(
            localRole = PlayerRole.OPERATOR,
            seed = SOLO_BATTLESHIP_SEED,
            vsAi = true,
            transport = pipe,
            scope = scope,
        )
        val initial = engine.state.value
        engine.dispose()
        pipe.close()
        return initial
    }

    private fun buildIdleCrossMathState(): CrossMathGameState {
        val board = CrossMathGenerator.generate(SOLO_PUZZLE_SEED, CrossMathSize.GRID_3X3)
        return CrossMathGameState(
            seed = board.seed,
            size = board.size,
            board = board,
            values = List(board.cellCount) { idx -> if (board.givenMask[idx]) board.solution[idx] else null },
            bank = board.initialBankValues.mapIndexed { idx, v -> com.splitsignal.core.domain.BankTile(idx, v) },
            selectedCellIndex = 0,
            rowSatisfied = List(board.dim) { false },
            colSatisfied = List(board.dim) { false },
            moves = 0,
            mistakes = 0,
            elapsedSeconds = 0,
            percentComplete = 0,
            isSolved = false,
            isRace = false,
            isDailyStep = false,
            opponentProgress = null,
        )
    }

    private fun buildIdleMathMazeState(): MathMazeGameState {
        val board = MathMazeGenerator.generate(SOLO_PUZZLE_SEED, 4)
        return MathMazeGameState(
            seed = board.seed,
            board = board,
            path = listOf(board.startIndex),
            runningTotal = board.startValue,
            hintedNextIndex = null,
            moves = 0,
            mistakes = 0,
            elapsedSeconds = 0,
            percentComplete = 0,
            isSolved = false,
            isRace = false,
            isDailyStep = false,
            opponentProgress = null,
        )
    }

    private fun buildIdleKenKenState(): KenKenGameState {
        val board = KenKenGenerator.generate(SOLO_PUZZLE_SEED, KenKenSize.GRID_4X4)
        return KenKenGameState(
            seed = board.seed,
            size = board.size,
            board = board,
            values = List(board.cellCount) { null },
            conflictCells = emptySet(),
            selectedCellIndex = 0,
            moves = 0,
            mistakes = 0,
            elapsedSeconds = 0,
            percentComplete = 0,
            isSolved = false,
            isRace = false,
            isDailyStep = false,
            opponentProgress = null,
        )
    }

    private fun buildIdleMemoryMatrixState(): MemoryMatrixGameState {
        val board = MemoryMatrixGenerator.generate(SOLO_PUZZLE_SEED, 4)
        return MemoryMatrixGameState(
            seed = board.seed,
            board = board,
            isRevealing = false,
            selectedIndices = emptyList(),
            wrongFlashIndex = null,
            moves = 0,
            mistakes = 0,
            elapsedSeconds = 0,
            percentComplete = 0,
            isSolved = false,
            isRace = false,
            isDailyStep = false,
            opponentProgress = null,
        )
    }

    private fun buildIdleMindsnapState(): MindsnapGameState {
        val deck = MindsnapGenerator.generateDeck(SOLO_PUZZLE_SEED)
        return MindsnapGameState(
            seed = SOLO_PUZZLE_SEED,
            questions = deck,
            currentIndex = 0,
            score = 0,
            combo = 0,
            bestCombo = 0,
            multiplier = 1,
            remainingSeconds = MindsnapGenerator.BLITZ_DURATION_SECONDS,
            elapsedSeconds = 0,
            moves = 0,
            mistakes = 0,
            percentComplete = 0,
            isFinished = false,
            isRace = false,
            isDailyStep = false,
            opponentProgress = null,
        )
    }

    private fun activeLanScreenFor(gameType: GameType): AppScreen {
        return when (gameType) {
            GameType.SUDOKU_6X6, GameType.SUDOKU_9X9 -> AppScreen.SUDOKU_RACE
            GameType.CROSS_MATH -> AppScreen.CROSS_MATH
            GameType.MATH_MAZE -> AppScreen.MATH_MAZE
            GameType.KENKEN -> AppScreen.KENKEN
            GameType.MEMORY_MATRIX -> AppScreen.MEMORY_MATRIX
            GameType.MINDSNAP -> AppScreen.MINDSNAP
            GameType.TRIATHLON -> AppScreen.MINDSNAP
            GameType.MATH, GameType.BATTLESHIP -> AppScreen.COOP_ACTIVE
        }
    }

    private fun sudokuSizeForGameType(gameType: GameType): SudokuSize {
        return when (gameType) {
            GameType.SUDOKU_9X9 -> SudokuSize.CLASSIC_9X9
            else -> SudokuSize.SPRINT_6X6
        }
    }

    private fun gameTypeForSudokuSize(size: SudokuSize): GameType {
        return when (size) {
            SudokuSize.SPRINT_6X6 -> GameType.SUDOKU_6X6
            SudokuSize.CLASSIC_9X9 -> GameType.SUDOKU_9X9
        }
    }

    private fun generateRoomCode(nonce: Int): String {
        val entropy = Fnv1a.hash32("SPLITSIGNAL:$nonce:${roomCodeRng.nextUInt()}")
        return RoomCodeGenerator.fromSeed(entropy)
    }

    private data class BroadcastSignature(
        val roomCode: String,
        val port: Int,
        val gameType: GameType,
    )

    companion object {
        private const val INITIAL_ROOM_RNG_SEED: UInt = 0x53504C54u
        private const val SOLO_PUZZLE_SEED: UInt = 0x534F4C4Fu
        private const val SOLO_BATTLESHIP_SEED: UInt = 0x42415454u
        private const val SOLO_SUDOKU_SEED: UInt = 0x5355444Bu
        private const val ROOM_CODE_LENGTH = 4
        private const val SOCIAL_ROOM_CODE = "SOCIAL"

        /**
         * Parses and validates a `host:port` endpoint string, returning `null` if malformed.
         */
        fun parseHostPort(input: String): ParsedEndpoint? {
            val trimmed = input.trim()
            val colonIndex = trimmed.lastIndexOf(':')
            if (colonIndex <= 0 || colonIndex >= trimmed.lastIndex) return null
            val host = trimmed.substring(0, colonIndex).trim()
            val port = trimmed.substring(colonIndex + 1).trim().toIntOrNull() ?: return null
            if (host.isEmpty() || port !in 1..65535) return null
            return ParsedEndpoint(host = host, port = port)
        }
    }
}
