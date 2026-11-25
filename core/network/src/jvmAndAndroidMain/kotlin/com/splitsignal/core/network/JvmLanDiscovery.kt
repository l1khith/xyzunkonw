package com.splitsignal.core.network

import com.splitsignal.core.domain.DiscoveredPeer
import com.splitsignal.core.domain.DiscoveredRoom
import com.splitsignal.core.domain.GameType
import com.splitsignal.core.domain.LanDiscovery
import com.splitsignal.core.domain.LocalIpProvider
import com.splitsignal.core.domain.PeerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import kotlin.concurrent.Volatile

/**
 * JVM and Android UDP LAN room discovery over broadcast port 45678.
 */
class JvmLanDiscovery(
    private val localIpProvider: LocalIpProvider,
) : LanDiscovery {

    private val broadcastScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var broadcastJob: Job? = null

    @Volatile
    private var peerPresenceJob: Job? = null

    @Volatile
    private var localUserId: String? = null

    override fun startBroadcasting(roomCode: String, wsPort: Int, gameType: GameType) {
        stopBroadcasting()
        val normalizedRoom = roomCode.trim().uppercase()
        if (normalizedRoom.isEmpty() || wsPort !in 1..65535) return

        broadcastJob = broadcastScope.launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket().apply {
                    broadcast = true
                    reuseAddress = true
                }
                while (currentCoroutineContext().isActive) {
                    val ip = localIpProvider.getLocalIpv4Address()
                    val payload = "$MAGIC_HEADER|$normalizedRoom|$ip|$wsPort|${gameType.code}"
                    val bytes = payload.encodeToByteArray()

                    for (target in resolveBroadcastAddresses()) {
                        runCatching {
                            val packet = DatagramPacket(bytes, bytes.size, target, DISCOVERY_PORT)
                            socket.send(packet)
                        }
                    }
                    delay(BROADCAST_INTERVAL_MS)
                }
            } catch (_: Throwable) {
                // Swallow socket errors so LAN hosting degrades gracefully.
            } finally {
                runCatching { socket?.close() }
            }
        }
    }

    override fun stopBroadcasting() {
        broadcastJob?.cancel()
        broadcastJob = null
    }

    override fun startPeerPresence(
        userId: String,
        displayName: String,
        ktorPort: Int,
        status: PeerStatus,
        elo: Int,
        streakDays: Int,
        topTrophy: String,
    ) {
        stopPeerPresence()
        val normalizedUserId = userId.trim()
        if (normalizedUserId.isEmpty() || ktorPort !in 1..65535) return
        val sanitizedDisplayName = displayName.replace('|', ' ').trim().ifEmpty { "Player" }
        val sanitizedTrophy = topTrophy.replace('|', ' ').trim()
        localUserId = normalizedUserId

        peerPresenceJob = broadcastScope.launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket().apply {
                    broadcast = true
                    reuseAddress = true
                }
                while (currentCoroutineContext().isActive) {
                    val ip = localIpProvider.getLocalIpv4Address()
                    val payload = "$PEER_MAGIC_HEADER|$normalizedUserId|$sanitizedDisplayName|$ip|$ktorPort|${status.code}|$elo|$streakDays|$sanitizedTrophy"
                    val bytes = payload.encodeToByteArray()

                    for (target in resolveBroadcastAddresses()) {
                        runCatching {
                            val packet = DatagramPacket(bytes, bytes.size, target, DISCOVERY_PORT)
                            socket.send(packet)
                        }
                    }
                    delay(PEER_BROADCAST_INTERVAL_MS)
                }
            } catch (_: Throwable) {
                // Swallow socket errors so LAN presence degrades gracefully.
            } finally {
                runCatching { socket?.close() }
            }
        }
    }

    override fun stopPeerPresence() {
        peerPresenceJob?.cancel()
        peerPresenceJob = null
        localUserId = null
    }

    override fun observeNearbyPeers(): Flow<List<DiscoveredPeer>> = callbackFlow {
        val seenPeers = mutableMapOf<String, PeerRecord>()
        var socket: DatagramSocket? = null

        val receiverJob = launch(Dispatchers.IO) {
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                    soTimeout = PEER_SOCKET_TIMEOUT_MS
                    bind(InetSocketAddress(DISCOVERY_PORT))
                }

                val buffer = ByteArray(MAX_PACKET_BYTES)
                val packet = DatagramPacket(buffer, buffer.size)

                while (currentCoroutineContext().isActive) {
                    packet.length = buffer.size
                    try {
                        socket?.receive(packet)
                        val text = packet.data.decodeToString(
                            startIndex = packet.offset,
                            endIndex = packet.offset + packet.length,
                        )
                        val peer = parsePeerPayload(text, packet.address?.hostAddress)
                        if (peer != null && peer.userId != localUserId) {
                            seenPeers[peer.userId] = PeerRecord(
                                peer = peer,
                                lastSeenMs = System.currentTimeMillis(),
                            )
                        }
                    } catch (_: SocketTimeoutException) {
                        // Periodic wakeup to prune expired peers.
                    }

                    val now = System.currentTimeMillis()
                    seenPeers.entries.removeAll { (_, record) ->
                        now - record.lastSeenMs > PEER_EXPIRY_MS
                    }
                    val snapshot = seenPeers.values
                        .map { it.peer }
                        .sortedWith(compareBy<DiscoveredPeer> { it.displayName.lowercase() }.thenBy { it.userId })
                    trySend(snapshot)
                }
            } catch (_: Throwable) {
                trySend(emptyList())
            } finally {
                runCatching { socket?.close() }
            }
        }

        awaitClose {
            receiverJob.cancel()
            runCatching { socket?.close() }
        }
    }.distinctUntilChanged()

    override fun observeNearbyRooms(): Flow<List<DiscoveredRoom>> = callbackFlow {
        val seenRooms = mutableMapOf<String, RoomRecord>()
        var socket: DatagramSocket? = null

        val receiverJob = launch(Dispatchers.IO) {
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                    soTimeout = SOCKET_TIMEOUT_MS
                    bind(InetSocketAddress(DISCOVERY_PORT))
                }

                val buffer = ByteArray(MAX_PACKET_BYTES)
                val packet = DatagramPacket(buffer, buffer.size)

                while (currentCoroutineContext().isActive) {
                    packet.length = buffer.size
                    try {
                        socket?.receive(packet)
                        val text = packet.data.decodeToString(
                            startIndex = packet.offset,
                            endIndex = packet.offset + packet.length,
                        )
                        val room = parsePayload(text, packet.address?.hostAddress)
                        if (room != null) {
                            val key = "${room.roomCode}|${room.hostIp}|${room.wsPort}"
                            seenRooms[key] = RoomRecord(
                                room = room,
                                lastSeenMs = System.currentTimeMillis(),
                            )
                        }
                    } catch (_: SocketTimeoutException) {
                        // Periodic wakeup to prune expired rooms.
                    }

                    val now = System.currentTimeMillis()
                    seenRooms.entries.removeAll { (_, record) ->
                        now - record.lastSeenMs > ROOM_EXPIRY_MS
                    }
                    val snapshot = seenRooms.values
                        .map { it.room }
                        .sortedWith(compareBy<DiscoveredRoom> { it.roomCode }.thenBy { it.hostIp })
                    trySend(snapshot)
                }
            } catch (_: Throwable) {
                trySend(emptyList())
            } finally {
                runCatching { socket?.close() }
            }
        }

        awaitClose {
            receiverJob.cancel()
            runCatching { socket?.close() }
        }
    }.distinctUntilChanged()

    private fun resolveBroadcastAddresses(): List<InetAddress> {
        val addresses = linkedSetOf<InetAddress>()
        runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return@runCatching
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val isUp = runCatching { iface.isUp && !iface.isLoopback }.getOrDefault(false)
                if (!isUp) continue
                for (ifaceAddr in iface.interfaceAddresses) {
                    val broadcast = ifaceAddr.broadcast
                    if (broadcast != null) {
                        addresses.add(broadcast)
                    }
                }
            }
        }
        runCatching {
            addresses.add(InetAddress.getByName(LIMITED_BROADCAST_IP))
        }
        return addresses.toList()
    }

    private fun parsePayload(payload: String, senderIp: String?): DiscoveredRoom? {
        val parts = payload.trim().split('|')
        if (parts.size != 5) return null
        if (parts[0] != MAGIC_HEADER) return null

        val roomCode = parts[1].trim().uppercase()
        if (roomCode.isEmpty()) return null

        val advertisedIp = parts[2].trim().ifEmpty { senderIp.orEmpty() }
        if (advertisedIp.isEmpty()) return null

        val wsPort = parts[3].trim().toIntOrNull() ?: return null
        if (wsPort !in 1..65535) return null

        val gameType = GameType.fromCode(parts[4]) ?: return null

        return DiscoveredRoom(
            roomCode = roomCode,
            hostIp = advertisedIp,
            wsPort = wsPort,
            gameType = gameType,
        )
    }

    private fun parsePeerPayload(payload: String, senderIp: String?): DiscoveredPeer? {
        val parts = payload.trim().split('|')
        if (parts.size != 6 && parts.size != 9) return null
        if (parts[0] != PEER_MAGIC_HEADER) return null

        val userId = parts[1].trim()
        if (userId.isEmpty()) return null

        val displayName = parts[2].trim().ifEmpty { "Player" }
        val hostIp = parts[3].trim().ifEmpty { senderIp.orEmpty() }
        if (hostIp.isEmpty()) return null

        val ktorPort = parts[4].trim().toIntOrNull() ?: return null
        if (ktorPort !in 1..65535) return null

        val status = PeerStatus.fromCode(parts[5]) ?: return null

        val elo = if (parts.size == 9) parts[6].trim().toIntOrNull() ?: 1200 else 1200
        val streakDays = if (parts.size == 9) parts[7].trim().toIntOrNull() ?: 0 else 0
        val topTrophy = if (parts.size == 9) parts[8].trim() else ""

        return DiscoveredPeer(
            userId = userId,
            displayName = displayName,
            hostIp = hostIp,
            ktorPort = ktorPort,
            status = status,
            elo = elo,
            streakDays = streakDays,
            topTrophy = topTrophy,
        )
    }

    private data class RoomRecord(
        val room: DiscoveredRoom,
        val lastSeenMs: Long,
    )

    private data class PeerRecord(
        val peer: DiscoveredPeer,
        val lastSeenMs: Long,
    )

    companion object {
        const val DISCOVERY_PORT = 45678
        const val SOCKET_TIMEOUT_MS = 1200
        const val PEER_SOCKET_TIMEOUT_MS = 750
        const val ROOM_EXPIRY_MS = 3500L
        const val PEER_EXPIRY_MS = 3500L
        private const val BROADCAST_INTERVAL_MS = 800L
        private const val PEER_BROADCAST_INTERVAL_MS = 1000L
        private const val MAX_PACKET_BYTES = 1024
        private const val MAGIC_HEADER = "SPLITSIGNAL"
        private const val PEER_MAGIC_HEADER = "SPLITSIGNAL_PEER"
        private const val LIMITED_BROADCAST_IP = "255.255.255.255"
    }
}
