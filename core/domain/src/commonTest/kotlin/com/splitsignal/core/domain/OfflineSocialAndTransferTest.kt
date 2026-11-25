package com.splitsignal.core.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineSocialAndTransferTest {
    private class FakeLanDiscovery : LanDiscovery {
        val peersFlow = MutableStateFlow<List<DiscoveredPeer>>(emptyList())

        override fun startBroadcasting(roomCode: String, wsPort: Int, gameType: GameType) = Unit
        override fun stopBroadcasting() = Unit
        override fun observeNearbyRooms(): Flow<List<DiscoveredRoom>> = emptyFlow()
        override fun observeNearbyPeers(): Flow<List<DiscoveredPeer>> = peersFlow
    }

    private data class FakeFileHandle(
        override val id: String,
        override val name: String,
        override val sizeBytes: Long,
        override val mimeType: String,
    ) : PlatformFileHandle

    @Test
    fun userIdentityStoreGeneratesIdAndPersistsAcrossInstances() {
        val storage = InMemoryLocalStorageProvider()
        val store1 = UserIdentityStore(storage = storage, initialSeed = 0x12345678u)

        val generatedId = store1.identity.value.userId
        assertTrue(generatedId.startsWith("KMP-"), "Expected KMP- prefix, got $generatedId")
        assertEquals(8, generatedId.length)
        assertTrue(generatedId.substring(4).all { it in RoomCodeGenerator.ALPHABET })

        store1.updateDisplayName("Nova")
        store1.cycleAvatarAccent()
        store1.addOrUpdateFriend(userId = "KMP-ABCD", displayName = "Aria", nowMs = 1_000L)
        store1.recordDuelResult(peerUserId = "KMP-ABCD", won = true)
        store1.appendChatMessage(
            ChatMessageRecord(
                messageId = "m1",
                peerUserId = "KMP-ABCD",
                senderId = generatedId,
                senderName = "Nova",
                text = "Ready for a duel?",
                timestampMs = 1_500L,
                isFromSelf = true,
            ),
        )

        val store2 = UserIdentityStore(storage = storage, initialSeed = 0x99999999u)
        assertEquals(generatedId, store2.identity.value.userId)
        assertEquals("Nova", store2.identity.value.displayName)
        assertEquals(1, store2.identity.value.avatarAccentIndex)

        assertTrue(store2.isFriend("KMP-ABCD"))
        val friend = store2.friends.value.single()
        assertEquals("Aria", friend.displayName)
        assertEquals(1, friend.duelWins)
        assertEquals(0, friend.duelLosses)

        val history = store2.getChatHistory("KMP-ABCD")
        assertEquals(1, history.size)
        assertEquals("Ready for a duel?", history.single().text)
    }

    @Test
    fun nearbyDevicesStatePartitionsFriendsUnknownAndOffline() = runTest(UnconfinedTestDispatcher()) {
        val storage = InMemoryLocalStorageProvider()
        val store = UserIdentityStore(storage = storage, initialSeed = 0x11112222u)
        store.addOrUpdateFriend(userId = "KMP-FRND", displayName = "OnlineFriend", nowMs = 100L)
        store.addOrUpdateFriend(userId = "KMP-OFFL", displayName = "OfflineFriend", nowMs = 100L)

        val discovery = FakeLanDiscovery()
        val manager = ChatSessionManager(
            identityStore = store,
            lanDiscovery = discovery,
            scope = backgroundScope,
            nowMsProvider = { 500L },
        )

        val onlineFriendPeer = DiscoveredPeer(
            userId = "KMP-FRND",
            displayName = "OnlineFriend",
            hostIp = "192.168.1.10",
            ktorPort = 9091,
            status = PeerStatus.ONLINE,
        )
        val strangerPeer = DiscoveredPeer(
            userId = "KMP-STRG",
            displayName = "Stranger",
            hostIp = "192.168.1.25",
            ktorPort = 9091,
            status = PeerStatus.IN_GAME,
        )
        discovery.peersFlow.value = listOf(onlineFriendPeer, strangerPeer)

        val state = manager.nearbyDevices.value
        assertEquals(listOf(onlineFriendPeer), state.friendsInRange)
        assertEquals(listOf(strangerPeer), state.unknownNearby)
        assertEquals(listOf("KMP-OFFL"), state.savedFriendsOffline.map { it.userId })
    }

    @Test
    fun chatSessionManagerRejectsOversizedFileAndHandlesMutualFriendAccept() = runTest(UnconfinedTestDispatcher()) {
        val (hostTransport, clientTransport) = InMemorySyncPipe.createPair()

        val aliceStore = UserIdentityStore(InMemoryLocalStorageProvider(), initialSeed = 0xAAAAAAA1u)
        aliceStore.updateDisplayName("Alice")
        val bobStore = UserIdentityStore(InMemoryLocalStorageProvider(), initialSeed = 0xBBBBBBB2u)
        bobStore.updateDisplayName("Bob")

        val aliceDiscovery = FakeLanDiscovery()
        val bobDiscovery = FakeLanDiscovery()

        val aliceManager = ChatSessionManager(
            identityStore = aliceStore,
            lanDiscovery = aliceDiscovery,
            scope = backgroundScope,
            nowMsProvider = { 10_000L },
        )
        val bobManager = ChatSessionManager(
            identityStore = bobStore,
            lanDiscovery = bobDiscovery,
            scope = backgroundScope,
            nowMsProvider = { 10_500L },
        )

        val bobPeer = DiscoveredPeer(
            userId = bobStore.identity.value.userId,
            displayName = "Bob",
            hostIp = "192.168.1.20",
            ktorPort = 9091,
            status = PeerStatus.ONLINE,
        )
        val alicePeer = DiscoveredPeer(
            userId = aliceStore.identity.value.userId,
            displayName = "Alice",
            hostIp = "192.168.1.10",
            ktorPort = 9091,
            status = PeerStatus.ONLINE,
        )

        aliceManager.openSession(bobPeer, hostTransport)
        bobManager.openSession(alicePeer, clientTransport)

        // 1. Oversized file (> 1 GB) must be rejected with exact validation error
        val oversizedFile = FakeFileHandle(
            id = "file-1",
            name = "huge-archive.zip",
            sizeBytes = MAX_FILE_TRANSFER_BYTES + 1L,
            mimeType = "application/zip",
        )
        aliceManager.offerFile(oversizedFile)
        assertEquals("File exceeds the 1 GB offline transfer limit", aliceManager.uiState.value.validationError)
        assertTrue(aliceManager.uiState.value.transfers.isEmpty())

        aliceManager.clearValidationError()
        assertNull(aliceManager.uiState.value.validationError)

        // 2. FriendRequest -> FriendAccept mutual persistence
        assertFalse(aliceStore.isFriend(bobStore.identity.value.userId))
        assertFalse(bobStore.isFriend(aliceStore.identity.value.userId))

        aliceManager.sendFriendRequest(bobPeer)
        val pendingRequests = bobManager.uiState.value.incomingFriendRequests
        assertEquals(1, pendingRequests.size)
        assertEquals(aliceStore.identity.value.userId, pendingRequests.single().fromUserId)

        bobManager.acceptFriendRequest(
            fromUserId = pendingRequests.single().fromUserId,
            fromName = pendingRequests.single().fromName,
        )

        assertTrue(bobManager.uiState.value.incomingFriendRequests.isEmpty())
        assertTrue(bobStore.isFriend(aliceStore.identity.value.userId))
        assertTrue(aliceStore.isFriend(bobStore.identity.value.userId))

        // 3. Chat message persistence & offline friend history view
        aliceManager.sendChatMessage("Ready for Battleship?")
        assertEquals(1, bobManager.uiState.value.messages.count { !it.isSystemEvent })

        bobManager.closeSession()
        val aliceFriend = bobStore.friends.value.single { it.userId == aliceStore.identity.value.userId }
        bobManager.openOfflineSession(aliceFriend)
        val offlineState = bobManager.uiState.value
        assertEquals(aliceFriend, offlineState.activeOfflineFriend)
        assertNull(offlineState.activePeer)
        assertEquals(TransportConnectionState.DISCONNECTED, offlineState.connectionState)
        val userMessages = offlineState.messages.filterNot { it.isSystemEvent }
        assertEquals(1, userMessages.size)
        assertEquals("Ready for Battleship?", userMessages.single().text)

        // 4. P2P GameInvite -> pendingGameInvite -> clearPendingGameInvite
        bobManager.openSession(alicePeer, clientTransport)
        aliceManager.sendGameInvite(inviteId = "inv-1", gameType = GameType.BATTLESHIP, seed = 0x12345678u)
        val pendingInvite = bobManager.uiState.value.pendingGameInvite
        assertEquals("inv-1", pendingInvite?.inviteId)
        assertEquals(GameType.BATTLESHIP, pendingInvite?.gameType)
        assertEquals(0x12345678u, pendingInvite?.seed)

        bobManager.clearPendingGameInvite()
        assertNull(bobManager.uiState.value.pendingGameInvite)
    }

    @Test
    fun formatByteSizeFormatsHumanReadableUnits() {
        assertEquals("0 B", formatByteSize(0L))
        assertEquals("512 B", formatByteSize(512L))
        assertEquals("12.4 KB", formatByteSize(12_698L))
        assertEquals("1.0 GB", formatByteSize(MAX_FILE_TRANSFER_BYTES))
    }
}
