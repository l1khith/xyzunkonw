package com.splitsignal.core.domain

import kotlinx.serialization.Serializable

/**
 * Wire protocol messages exchanged between host and client transports.
 */
@Serializable
sealed interface SyncPacket {
    @Serializable
    data class Handshake(
        val roomCode: String,
        val role: PlayerRole,
    ) : SyncPacket

    @Serializable
    data class DialUpdate(
        val frequency: Float,
        val phase: Float,
    ) : SyncPacket

    @Serializable
    data class SwitchToggle(
        val index: Int,
        val enabled: Boolean,
    ) : SyncPacket

    @Serializable
    data class StateSync(
        val state: GameState,
    ) : SyncPacket

    @Serializable
    data class AdvanceStage(
        val nextStageNumber: Int,
    ) : SyncPacket

    @Serializable
    data class RestartRun(
        val roomCode: String,
        val untimedMode: Boolean,
    ) : SyncPacket

    @Serializable
    data class TileIncremented(
        val cellIndex: Int,
        val newValue: Int,
    ) : SyncPacket

    @Serializable
    data class AdvanceLevel(
        val nextLevelIndex: Int,
    ) : SyncPacket

    @Serializable
    data class BattleshipShot(
        val cellIndex: Int,
        val shooterRole: PlayerRole,
    ) : SyncPacket

    @Serializable
    data class BattleshipSonar(
        val centerIndex: Int,
        val scannerRole: PlayerRole,
    ) : SyncPacket

    @Serializable
    data class BattleshipRestart(
        val seed: UInt,
    ) : SyncPacket

    @Serializable
    data class SudokuStart(
        val seed: UInt,
        val size: SudokuSize,
        val difficulty: SudokuDifficulty,
    ) : SyncPacket

    @Serializable
    data class SudokuProgress(
        val percent: Int,
        val mistakes: Int,
        val elapsedSeconds: Int,
        val finished: Boolean,
    ) : SyncPacket

    @Serializable
    data class SudokuSabotage(
        val targetBoxIndex: Int,
    ) : SyncPacket

    @Serializable
    data class DuelStart(
        val seed: UInt,
        val gameType: GameType,
        val isTriathlon: Boolean = false,
        val roundIndex: Int = 0,
    ) : SyncPacket

    @Serializable
    data class DuelProgress(
        val roundIndex: Int,
        val percent: Int,
        val mistakes: Int,
        val elapsedSeconds: Int,
        val finished: Boolean,
        val score: Int = 0,
    ) : SyncPacket

    @Serializable
    data class TriathlonRoundAdvance(
        val nextRoundIndex: Int,
        val nextGameType: GameType,
        val nextSeed: UInt,
    ) : SyncPacket

    @Serializable
    data class FriendRequest(
        val fromUserId: String,
        val fromName: String,
    ) : SyncPacket

    @Serializable
    data class FriendAccept(
        val fromUserId: String,
        val fromName: String,
    ) : SyncPacket

    @Serializable
    data class ChatTextMessage(
        val messageId: String,
        val senderId: String,
        val senderName: String,
        val text: String,
        val timestampMs: Long,
        val stickerCode: String? = null,
    ) : SyncPacket

    @Serializable
    data class ChatTypingIndicator(
        val senderId: String,
        val isTyping: Boolean,
    ) : SyncPacket

    @Serializable
    data class FileTransferOffer(
        val transferId: String,
        val senderId: String,
        val fileName: String,
        val fileSizeBytes: Long,
        val mimeType: String,
    ) : SyncPacket

    @Serializable
    data class FileTransferDecision(
        val transferId: String,
        val accepted: Boolean,
        val reason: String? = null,
    ) : SyncPacket

    @Serializable
    data class FileTransferProgress(
        val transferId: String,
        val bytesTransferred: Long,
        val totalBytes: Long,
        val speedBytesPerSec: Long,
    ) : SyncPacket

    @Serializable
    data class GameInvite(
        val inviteId: String,
        val fromUserId: String,
        val fromName: String,
        val gameType: GameType,
        val seed: UInt,
    ) : SyncPacket

    @Serializable
    data class GameInviteResponse(
        val inviteId: String,
        val accepted: Boolean,
        val fromUserId: String,
    ) : SyncPacket
}
