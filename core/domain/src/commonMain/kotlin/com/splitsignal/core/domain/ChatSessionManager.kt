package com.splitsignal.core.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * Lifecycle status of an offline file transfer.
 */
@Serializable
enum class TransferStatus {
    PENDING_OFFER,
    TRANSFERRING,
    COMPLETED,
    DECLINED,
    CANCELLED,
    FAILED,
}

/**
 * UI & domain model representing an active or completed file transfer.
 */
@Serializable
data class FileTransferItem(
    val transferId: String,
    val peerUserId: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val mimeType: String,
    val isOutgoing: Boolean,
    val status: TransferStatus,
    val bytesTransferred: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val savedPath: String? = null,
    val errorMessage: String? = null,
)

/**
 * Snapshot of the active 1-on-1 chat, friend request, and file transfer session.
 */
data class ChatSessionUiState(
    val activePeer: DiscoveredPeer? = null,
    val activeOfflineFriend: FriendRecord? = null,
    val connectionState: TransportConnectionState = TransportConnectionState.DISCONNECTED,
    val messages: List<ChatMessageRecord> = emptyList(),
    val peerIsTyping: Boolean = false,
    val incomingFriendRequests: List<SyncPacket.FriendRequest> = emptyList(),
    val pendingGameInvite: SyncPacket.GameInvite? = null,
    val transfers: List<FileTransferItem> = emptyList(),
    val validationError: String? = null,
)

/**
 * Coordinates nearby peer partitioning, friend requests, 1-on-1 chat messaging,
 * typing indicators, and 1 GB streaming file transfers over [SyncTransport] and [FileTransferTransport].
 */
class ChatSessionManager(
    private val identityStore: UserIdentityStore,
    lanDiscovery: LanDiscovery,
    private val fileTransferTransport: FileTransferTransport? = null,
    private val scope: CoroutineScope,
    private val nowMsProvider: () -> Long = { 0L },
) {
    private val _uiState = MutableStateFlow(ChatSessionUiState())
    val uiState: StateFlow<ChatSessionUiState> = _uiState.asStateFlow()

    val nearbyDevices: StateFlow<NearbyDevicesState> = combine(
        lanDiscovery.observeNearbyPeers(),
        identityStore.friends,
    ) { peers, friends ->
        partitionNearbyDevices(peers, friends)
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = partitionNearbyDevices(emptyList(), identityStore.friends.value),
    )

    private var activeTransport: SyncTransport? = null
    private var transportPacketsJob: Job? = null
    private var transportStateJob: Job? = null
    private val outgoingFiles = mutableMapOf<String, PlatformFileHandle>()
    private var idSequence: Long = 0L

    /**
     * Binds [transport] for sending and receiving chat, friend, and file-transfer control packets.
     */
    fun bindTransport(transport: SyncTransport) {
        transportPacketsJob?.cancel()
        transportStateJob?.cancel()
        activeTransport = transport

        _uiState.update { it.copy(connectionState = transport.connectionState.value) }

        transportStateJob = scope.launch {
            transport.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
            }
        }
        transportPacketsJob = scope.launch {
            transport.incomingPackets.collect { packet ->
                onIncomingPacket(packet)
            }
        }
    }

    /**
     * Sets [peer] as the active chat partner and loads their persisted message history.
     */
    fun setActivePeer(peer: DiscoveredPeer?) {
        val history = if (peer != null) identityStore.getChatHistory(peer.userId) else emptyList()
        _uiState.update {
            it.copy(
                activePeer = peer,
                activeOfflineFriend = null,
                messages = history,
                peerIsTyping = false,
                validationError = null,
            )
        }
    }

    /**
     * Opens an offline read-only chat history view for [friend] without an active peer transport.
     */
    fun openOfflineSession(friend: FriendRecord) {
        val history = identityStore.getChatHistory(friend.userId)
        _uiState.update {
            it.copy(
                activeOfflineFriend = friend,
                activePeer = null,
                connectionState = TransportConnectionState.DISCONNECTED,
                messages = history,
                peerIsTyping = false,
                validationError = null,
            )
        }
    }

    /**
     * Opens a chat session with [peer] over [transport].
     */
    fun openSession(peer: DiscoveredPeer, transport: SyncTransport) {
        setActivePeer(peer)
        bindTransport(transport)
        val lastMessage = identityStore.getChatHistory(peer.userId).lastOrNull()
        val connectedText = "Connected via Local Wi-Fi"
        if (lastMessage == null || !lastMessage.isSystemEvent || lastMessage.text != connectedText) {
            appendSystemMessage(peer.userId, connectedText)
        }
    }

    /**
     * Appends a local `[ SYSTEM ]` event message to [peerUserId]'s chat history and updates
     * [ChatSessionUiState.messages] when [peerUserId] is the active peer.
     */
    fun appendSystemMessage(peerUserId: String, text: String) {
        val nowMs = nowMsProvider()
        val messageId = nextId("sys", peerUserId, nowMs)
        val record = ChatMessageRecord(
            messageId = messageId,
            peerUserId = peerUserId,
            senderId = "SYSTEM",
            senderName = "SYSTEM",
            text = text,
            timestampMs = nowMs,
            isFromSelf = false,
            isSystemEvent = true,
        )
        identityStore.appendChatMessage(record)
        _uiState.update { state ->
            if (state.activePeer?.userId == peerUserId) {
                state.copy(messages = identityStore.getChatHistory(peerUserId))
            } else {
                state
            }
        }
    }

    /**
     * Closes the active transport binding and resets the active peer.
     */
    fun closeSession() {
        transportPacketsJob?.cancel()
        transportStateJob?.cancel()
        transportPacketsJob = null
        transportStateJob = null
        activeTransport = null
        _uiState.update {
            it.copy(
                activePeer = null,
                activeOfflineFriend = null,
                connectionState = TransportConnectionState.DISCONNECTED,
                peerIsTyping = false,
            )
        }
    }

    /**
     * Sends a chat text message (or optional sticker/GIF) to the active peer and persists it locally.
     */
    suspend fun sendChatMessage(text: String, stickerCode: String? = null) {
        val trimmed = text.trim()
        val normalizedSticker = stickerCode?.trim()?.takeIf { it.isNotEmpty() }
        val peer = _uiState.value.activePeer ?: return
        if (trimmed.isEmpty() && normalizedSticker == null) return

        val self = identityStore.identity.value
        val nowMs = nowMsProvider()
        val messageId = nextId("msg", self.userId, nowMs)

        val packet = SyncPacket.ChatTextMessage(
            messageId = messageId,
            senderId = self.userId,
            senderName = self.displayName,
            text = trimmed,
            timestampMs = nowMs,
            stickerCode = normalizedSticker,
        )
        val record = ChatMessageRecord(
            messageId = messageId,
            peerUserId = peer.userId,
            senderId = self.userId,
            senderName = self.displayName,
            text = trimmed,
            timestampMs = nowMs,
            isFromSelf = true,
            stickerCode = normalizedSticker,
        )
        identityStore.appendChatMessage(record)
        _uiState.update { it.copy(messages = identityStore.getChatHistory(peer.userId)) }
        activeTransport?.send(packet)
    }

    /**
     * Sends a typing indicator update to the connected peer.
     */
    suspend fun sendTypingIndicator(isTyping: Boolean) {
        val self = identityStore.identity.value
        activeTransport?.send(
            SyncPacket.ChatTypingIndicator(
                senderId = self.userId,
                isTyping = isTyping,
            ),
        )
    }

    /**
     * Sends a friend request to the connected peer.
     */
    suspend fun sendFriendRequest(targetPeer: DiscoveredPeer? = _uiState.value.activePeer) {
        if (targetPeer == null && activeTransport == null) return
        val self = identityStore.identity.value
        activeTransport?.send(
            SyncPacket.FriendRequest(
                fromUserId = self.userId,
                fromName = self.displayName,
            ),
        )
    }

    /**
     * Accepts a pending friend request from [fromUserId], persists them as a friend,
     * and replies with [SyncPacket.FriendAccept].
     */
    suspend fun acceptFriendRequest(fromUserId: String, fromName: String) {
        identityStore.addOrUpdateFriend(
            userId = fromUserId,
            displayName = fromName,
            nowMs = nowMsProvider(),
        )
        _uiState.update { state ->
            state.copy(
                incomingFriendRequests = state.incomingFriendRequests.filterNot { it.fromUserId == fromUserId },
            )
        }
        val self = identityStore.identity.value
        activeTransport?.send(
            SyncPacket.FriendAccept(
                fromUserId = self.userId,
                fromName = self.displayName,
            ),
        )
    }

    /**
     * Dismisses an incoming friend request from [fromUserId] without adding them.
     */
    fun declineFriendRequest(fromUserId: String) {
        _uiState.update { state ->
            state.copy(
                incomingFriendRequests = state.incomingFriendRequests.filterNot { it.fromUserId == fromUserId },
            )
        }
    }

    /**
     * Validates [file] against [MAX_FILE_TRANSFER_BYTES] and sends a [SyncPacket.FileTransferOffer].
     */
    suspend fun offerFile(file: PlatformFileHandle) {
        if (file.sizeBytes > MAX_FILE_TRANSFER_BYTES) {
            _uiState.update { it.copy(validationError = LIMIT_EXCEEDED_MESSAGE) }
            return
        }

        val self = identityStore.identity.value
        val peerId = _uiState.value.activePeer?.userId ?: "unknown"
        val transferId = nextId("xfer", self.userId, nowMsProvider())

        outgoingFiles[transferId] = file
        fileTransferTransport?.registerOutgoingFile(transferId, file)

        val item = FileTransferItem(
            transferId = transferId,
            peerUserId = peerId,
            fileName = file.name,
            fileSizeBytes = file.sizeBytes,
            mimeType = file.mimeType,
            isOutgoing = true,
            status = TransferStatus.PENDING_OFFER,
        )
        _uiState.update { state ->
            state.copy(
                validationError = null,
                transfers = state.transfers + item,
            )
        }

        activeTransport?.send(
            SyncPacket.FileTransferOffer(
                transferId = transferId,
                senderId = self.userId,
                fileName = file.name,
                fileSizeBytes = file.sizeBytes,
                mimeType = file.mimeType,
            ),
        )
    }

    /**
     * Responds to an incoming file transfer offer and starts downloading via [FileTransferTransport] if accepted.
     */
    suspend fun respondToFileOffer(
        transferId: String,
        accept: Boolean,
        reason: String? = null,
    ) {
        val item = _uiState.value.transfers.firstOrNull { it.transferId == transferId } ?: return
        activeTransport?.send(
            SyncPacket.FileTransferDecision(
                transferId = transferId,
                accepted = accept,
                reason = reason,
            ),
        )

        if (!accept) {
            updateTransfer(transferId) {
                it.copy(status = TransferStatus.DECLINED, errorMessage = reason)
            }
            return
        }

        updateTransfer(transferId) { it.copy(status = TransferStatus.TRANSFERRING) }
        val transport = fileTransferTransport ?: return
        val peer = _uiState.value.activePeer ?: return

        scope.launch {
            transport.downloadOfferedFile(
                peerIp = peer.hostIp,
                peerPort = peer.ktorPort,
                transferId = transferId,
                fileName = item.fileName,
                totalBytes = item.fileSizeBytes,
                onProgress = { transferred, speed ->
                    updateTransfer(transferId) {
                        it.copy(bytesTransferred = transferred, speedBytesPerSec = speed)
                    }
                },
                onComplete = { savedPath ->
                    updateTransfer(transferId) {
                        it.copy(
                            status = TransferStatus.COMPLETED,
                            bytesTransferred = item.fileSizeBytes,
                            savedPath = savedPath,
                        )
                    }
                    appendSystemMessage(
                        peerUserId = peer.userId,
                        text = "File transfer completed: ${item.fileName}",
                    )
                },
                onError = { error ->
                    updateTransfer(transferId) {
                        it.copy(status = TransferStatus.FAILED, errorMessage = error)
                    }
                },
            )
        }
    }

    /**
     * Cancels an active or pending file transfer.
     */
    fun cancelTransfer(transferId: String) {
        fileTransferTransport?.cancelTransfer(transferId)
        fileTransferTransport?.unregisterOutgoingFile(transferId)
        outgoingFiles.remove(transferId)
        updateTransfer(transferId) { it.copy(status = TransferStatus.CANCELLED) }
    }

    /**
     * Sends a [SyncPacket.GameInvite] over the currently bound transport.
     */
    suspend fun sendGameInvite(inviteId: String, gameType: GameType, seed: UInt) {
        val self = identityStore.identity.value
        activeTransport?.send(
            SyncPacket.GameInvite(
                inviteId = inviteId,
                fromUserId = self.userId,
                fromName = self.displayName,
                gameType = gameType,
                seed = seed,
            ),
        )
    }

    /**
     * Sends a [SyncPacket.GameInviteResponse] over the currently bound transport.
     */
    suspend fun sendGameInviteResponse(inviteId: String, accepted: Boolean) {
        val self = identityStore.identity.value
        activeTransport?.send(
            SyncPacket.GameInviteResponse(
                inviteId = inviteId,
                accepted = accepted,
                fromUserId = self.userId,
            ),
        )
    }

    /**
     * Clears any pending incoming [ChatSessionUiState.pendingGameInvite].
     */
    fun clearPendingGameInvite() {
        _uiState.update { it.copy(pendingGameInvite = null) }
    }

    /**
     * Clears any displayed [ChatSessionUiState.validationError].
     */
    fun clearValidationError() {
        _uiState.update { it.copy(validationError = null) }
    }

    /**
     * Processes a single incoming [SyncPacket] relevant to chat, friends, or file transfers.
     */
    suspend fun onIncomingPacket(packet: SyncPacket) {
        when (packet) {
            is SyncPacket.ChatTextMessage -> handleIncomingChatMessage(packet)
            is SyncPacket.ChatTypingIndicator -> handleIncomingTypingIndicator(packet)
            is SyncPacket.FriendRequest -> handleIncomingFriendRequest(packet)
            is SyncPacket.FriendAccept -> handleIncomingFriendAccept(packet)
            is SyncPacket.GameInvite -> handleIncomingGameInvite(packet)
            is SyncPacket.FileTransferOffer -> handleIncomingFileOffer(packet)
            is SyncPacket.FileTransferDecision -> handleIncomingFileDecision(packet)
            is SyncPacket.FileTransferProgress -> handleIncomingFileProgress(packet)
            else -> Unit
        }
    }

    private fun handleIncomingChatMessage(packet: SyncPacket.ChatTextMessage) {
        val activePeerId = _uiState.value.activePeer?.userId
        val peerUserId = activePeerId ?: packet.senderId
        val record = ChatMessageRecord(
            messageId = packet.messageId,
            peerUserId = peerUserId,
            senderId = packet.senderId,
            senderName = packet.senderName,
            text = packet.text,
            timestampMs = packet.timestampMs,
            isFromSelf = false,
            stickerCode = packet.stickerCode,
        )
        identityStore.appendChatMessage(record)
        _uiState.update { state ->
            val updatedMessages = if (state.activePeer == null || state.activePeer.userId == peerUserId) {
                identityStore.getChatHistory(peerUserId)
            } else {
                state.messages
            }
            state.copy(
                messages = updatedMessages,
                peerIsTyping = false,
            )
        }
    }

    private fun handleIncomingTypingIndicator(packet: SyncPacket.ChatTypingIndicator) {
        if (packet.senderId == identityStore.identity.value.userId) return
        _uiState.update { it.copy(peerIsTyping = packet.isTyping) }
    }

    private fun handleIncomingFriendRequest(packet: SyncPacket.FriendRequest) {
        if (packet.fromUserId == identityStore.identity.value.userId) return
        if (identityStore.isFriend(packet.fromUserId)) {
            identityStore.addOrUpdateFriend(packet.fromUserId, packet.fromName, nowMsProvider())
            return
        }
        _uiState.update { state ->
            if (state.incomingFriendRequests.any { it.fromUserId == packet.fromUserId }) {
                state
            } else {
                state.copy(incomingFriendRequests = state.incomingFriendRequests + packet)
            }
        }
    }

    private fun handleIncomingFriendAccept(packet: SyncPacket.FriendAccept) {
        if (packet.fromUserId == identityStore.identity.value.userId) return
        identityStore.addOrUpdateFriend(
            userId = packet.fromUserId,
            displayName = packet.fromName,
            nowMs = nowMsProvider(),
        )
    }

    private fun handleIncomingGameInvite(packet: SyncPacket.GameInvite) {
        if (packet.fromUserId == identityStore.identity.value.userId) return
        _uiState.update { it.copy(pendingGameInvite = packet) }
    }

    private suspend fun handleIncomingFileOffer(packet: SyncPacket.FileTransferOffer) {
        if (packet.fileSizeBytes > MAX_FILE_TRANSFER_BYTES) {
            _uiState.update { it.copy(validationError = LIMIT_EXCEEDED_MESSAGE) }
            activeTransport?.send(
                SyncPacket.FileTransferDecision(
                    transferId = packet.transferId,
                    accepted = false,
                    reason = LIMIT_EXCEEDED_MESSAGE,
                ),
            )
            return
        }

        val item = FileTransferItem(
            transferId = packet.transferId,
            peerUserId = packet.senderId,
            fileName = packet.fileName,
            fileSizeBytes = packet.fileSizeBytes,
            mimeType = packet.mimeType,
            isOutgoing = false,
            status = TransferStatus.PENDING_OFFER,
        )
        _uiState.update { state ->
            if (state.transfers.any { it.transferId == packet.transferId }) {
                state
            } else {
                state.copy(transfers = state.transfers + item)
            }
        }
    }

    private fun handleIncomingFileDecision(packet: SyncPacket.FileTransferDecision) {
        if (!packet.accepted) {
            fileTransferTransport?.unregisterOutgoingFile(packet.transferId)
            outgoingFiles.remove(packet.transferId)
            updateTransfer(packet.transferId) {
                it.copy(status = TransferStatus.DECLINED, errorMessage = packet.reason)
            }
            return
        }

        updateTransfer(packet.transferId) { it.copy(status = TransferStatus.TRANSFERRING) }
        val file = outgoingFiles[packet.transferId] ?: return
        val transport = fileTransferTransport ?: return
        val peer = _uiState.value.activePeer ?: return

        scope.launch {
            transport.sendOfferAndStream(
                peerIp = peer.hostIp,
                peerPort = peer.ktorPort,
                transferId = packet.transferId,
                file = file,
                onProgress = { transferred, speed ->
                    updateTransfer(packet.transferId) {
                        it.copy(bytesTransferred = transferred, speedBytesPerSec = speed)
                    }
                },
                onComplete = {
                    outgoingFiles.remove(packet.transferId)
                    transport.unregisterOutgoingFile(packet.transferId)
                    updateTransfer(packet.transferId) {
                        it.copy(
                            status = TransferStatus.COMPLETED,
                            bytesTransferred = file.sizeBytes,
                        )
                    }
                    appendSystemMessage(
                        peerUserId = peer.userId,
                        text = "File transfer completed: ${file.name}",
                    )
                },
                onError = { error ->
                    outgoingFiles.remove(packet.transferId)
                    transport.unregisterOutgoingFile(packet.transferId)
                    updateTransfer(packet.transferId) {
                        it.copy(status = TransferStatus.FAILED, errorMessage = error)
                    }
                },
            )
        }
    }

    private fun handleIncomingFileProgress(packet: SyncPacket.FileTransferProgress) {
        updateTransfer(packet.transferId) {
            it.copy(
                bytesTransferred = packet.bytesTransferred,
                speedBytesPerSec = packet.speedBytesPerSec,
            )
        }
    }

    private fun updateTransfer(transferId: String, transform: (FileTransferItem) -> FileTransferItem) {
        _uiState.update { state ->
            state.copy(
                transfers = state.transfers.map { item ->
                    if (item.transferId == transferId) transform(item) else item
                },
            )
        }
    }

    private fun partitionNearbyDevices(
        peers: List<DiscoveredPeer>,
        friends: List<FriendRecord>,
    ): NearbyDevicesState {
        val localUserId = identityStore.identity.value.userId
        val remotePeers = peers.filter { it.userId != localUserId }
        val friendIds = friends.mapTo(mutableSetOf()) { it.userId }
        val friendsInRange = remotePeers.filter { it.userId in friendIds }
        val unknownNearby = remotePeers.filter { it.userId !in friendIds }
        val onlineFriendIds = friendsInRange.mapTo(mutableSetOf()) { it.userId }
        val savedFriendsOffline = friends.filter { it.userId !in onlineFriendIds }
        return NearbyDevicesState(
            friendsInRange = friendsInRange,
            unknownNearby = unknownNearby,
            savedFriendsOffline = savedFriendsOffline,
        )
    }

    private fun nextId(prefix: String, userId: String, nowMs: Long): String {
        idSequence += 1L
        return "$prefix-$userId-$nowMs-$idSequence"
    }

    companion object {
        const val LIMIT_EXCEEDED_MESSAGE: String = "File exceeds the 1 GB offline transfer limit"
    }
}
