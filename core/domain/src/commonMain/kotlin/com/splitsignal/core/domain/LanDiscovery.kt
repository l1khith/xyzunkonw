package com.splitsignal.core.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Supported SplitSignal co-op and tactical game modes advertised over LAN discovery.
 */
enum class GameType(
    val code: String,
    val displayName: String,
) {
    CROSS_MATH("XMATH", "Cross Math"),
    MATH_MAZE("MAZE", "Math Maze"),
    KENKEN("KENK", "KenKen"),
    SUDOKU_6X6("SDK6", "Mini Sudoku 6×6"),
    SUDOKU_9X9("SDK9", "Classic Sudoku 9×9"),
    MEMORY_MATRIX("MEMX", "Memory Matrix"),
    MINDSNAP("SNAP", "Mindsnap Blitz"),
    TRIATHLON("TRIA", "3-Round Triathlon"),
    MATH("MATH", "Math Deduction"),
    BATTLESHIP("SHIP", "Tactical Battleship"),
    ;

    companion object {
        /**
         * Resolves a [GameType] from its wire [code], or `null` if unrecognized.
         */
        fun fromCode(code: String): GameType? {
            val normalized = code.trim()
            return entries.firstOrNull { it.code.equals(normalized, ignoreCase = true) }
        }
    }
}

/**
 * LAN room announcement discovered via UDP broadcast.
 */
data class DiscoveredRoom(
    val roomCode: String,
    val hostIp: String,
    val wsPort: Int,
    val gameType: GameType,
)

/**
 * Parsed representation of a compact SplitSignal invite token or host address.
 */
data class InviteToken(
    val roomCode: String?,
    val hostIp: String,
    val wsPort: Int,
)

/**
 * Presence status advertised by a SplitSignal peer on the local network.
 */
enum class PeerStatus(val code: String) {
    ONLINE("ONLINE"),
    IN_GAME("INGAME"),
    ;

    companion object {
        /**
         * Resolves a [PeerStatus] from its wire [code], or `null` if unrecognized.
         */
        fun fromCode(code: String): PeerStatus? {
            val normalized = code.trim()
            return entries.firstOrNull { it.code.equals(normalized, ignoreCase = true) }
        }
    }
}

/**
 * Nearby SplitSignal peer discovered via LAN presence broadcast.
 */
data class DiscoveredPeer(
    val userId: String,
    val displayName: String,
    val hostIp: String,
    val ktorPort: Int,
    val status: PeerStatus,
    val elo: Int = 1200,
    val streakDays: Int = 0,
    val topTrophy: String = "",
)

/**
 * Partitioned view of nearby LAN peers and saved offline friends.
 */
data class NearbyDevicesState(
    val friendsInRange: List<DiscoveredPeer> = emptyList(),
    val unknownNearby: List<DiscoveredPeer> = emptyList(),
    val savedFriendsOffline: List<FriendRecord> = emptyList(),
)

/**
 * Platform abstraction for broadcasting and discovering SplitSignal rooms and peers on the local network.
 */
interface LanDiscovery {
    fun startBroadcasting(roomCode: String, wsPort: Int, gameType: GameType)
    fun stopBroadcasting()
    fun observeNearbyRooms(): Flow<List<DiscoveredRoom>>

    fun startPeerPresence(
        userId: String,
        displayName: String,
        ktorPort: Int,
        status: PeerStatus,
        elo: Int = 1200,
        streakDays: Int = 0,
        topTrophy: String = "",
    ) {}
    fun stopPeerPresence() {}
    fun observeNearbyPeers(): Flow<List<DiscoveredPeer>> = emptyFlow()
}

/**
 * Formats and parses compact invite tokens (`"CODE@IP:PORT"` and `"IP:PORT"`).
 */
object InviteTokenCodec {
    /**
     * Formats a full invite string containing [roomCode], [hostIp], and [wsPort].
     */
    fun format(roomCode: String, hostIp: String, wsPort: Int): String {
        return "${roomCode.trim().uppercase()}@${hostIp.trim()}:$wsPort"
    }

    /**
     * Formats a bare host endpoint string (`"IP:PORT"`).
     */
    fun formatEndpoint(hostIp: String, wsPort: Int): String {
        return "${hostIp.trim()}:$wsPort"
    }

    /**
     * Parses `"$roomCode@$hostIp:$wsPort"` or `"$hostIp:$wsPort"` into an [InviteToken],
     * returning `null` if the string is malformed.
     */
    fun parse(raw: String): InviteToken? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        val atIndex = trimmed.indexOf('@')
        val roomCode: String?
        val endpointPart: String
        if (atIndex >= 0) {
            if (trimmed.indexOf('@', startIndex = atIndex + 1) >= 0) return null
            val parsedCode = trimmed.substring(0, atIndex).trim().uppercase()
            if (parsedCode.isEmpty() || parsedCode.any { it.isWhitespace() }) return null
            roomCode = parsedCode
            endpointPart = trimmed.substring(atIndex + 1).trim()
        } else {
            roomCode = null
            endpointPart = trimmed
        }

        val colonIndex = endpointPart.lastIndexOf(':')
        if (colonIndex <= 0 || colonIndex >= endpointPart.lastIndex) return null

        val hostIp = endpointPart.substring(0, colonIndex).trim()
        val portText = endpointPart.substring(colonIndex + 1).trim()
        if (hostIp.isEmpty() || hostIp.any { it.isWhitespace() || it == ':' }) return null

        val port = portText.toIntOrNull() ?: return null
        if (port !in 1..65535) return null

        return InviteToken(
            roomCode = roomCode,
            hostIp = hostIp,
            wsPort = port,
        )
    }
}
