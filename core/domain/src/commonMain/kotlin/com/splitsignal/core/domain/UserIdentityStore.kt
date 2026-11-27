package com.splitsignal.core.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persisted friend profile and head-to-head duel record.
 */
@Serializable
data class FriendRecord(
    val userId: String,
    val displayName: String,
    val addedAtEpochMs: Long,
    val lastSeenEpochMs: Long,
    val duelWins: Int = 0,
    val duelLosses: Int = 0,
    val elo: Int = 0,
    val streakDays: Int = 0,
    val topTrophy: String = "",
)

/**
 * Controls whether the local player's presence is broadcast on the local network.
 */
@Serializable
enum class PrivacyMode(val label: String) {
    PUBLIC("Public"),
    FRIENDS_ONLY("Friends"),
    PRIVATE("Private"),
}

/**
 * Permanent offline user identity generated on first launch.
 */
@Serializable
data class UserIdentity(
    val userId: String,
    val displayName: String,
    val avatarAccentIndex: Int = 0,
    val onboardingCompleted: Boolean = false,
    val privacyMode: PrivacyMode = PrivacyMode.PUBLIC,
    val defaultSaveDir: String = "",
    val autoSaveFiles: Boolean = false,
)

/**
 * Persisted 1-on-1 chat message entry scoped to a peer's [peerUserId].
 */
@Serializable
data class ChatMessageRecord(
    val messageId: String,
    val peerUserId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestampMs: Long,
    val isFromSelf: Boolean,
    val isSystemEvent: Boolean = false,
    val stickerCode: String? = null,
)

/**
 * Manages persistent offline user identity (`KMP-XXXX`), friend list, and per-peer chat history
 * backed by [LocalStorageProvider].
 */
class UserIdentityStore(
    private val storage: LocalStorageProvider,
    private val initialSeed: UInt = 0x4B4D5031u,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _identity: MutableStateFlow<UserIdentity>
    val identity: StateFlow<UserIdentity>

    private val _friends: MutableStateFlow<List<FriendRecord>>
    val friends: StateFlow<List<FriendRecord>>

    private val chatHistory: MutableMap<String, MutableList<ChatMessageRecord>>

    init {
        val loadedIdentity = loadIdentity()
        _identity = MutableStateFlow(loadedIdentity)
        identity = _identity.asStateFlow()

        val loadedFriends = loadFriends()
        _friends = MutableStateFlow(loadedFriends)
        friends = _friends.asStateFlow()

        chatHistory = loadChatHistory()
    }

    /**
     * Updates the local user's display name if [name] is non-blank and persists the identity.
     */
    fun updateDisplayName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val updated = _identity.value.copy(displayName = trimmed)
        _identity.value = updated
        persistIdentity(updated)
    }

    /**
     * Cycles the avatar accent color index and persists the identity.
     */
    fun cycleAvatarAccent() {
        val current = _identity.value
        val updated = current.copy(avatarAccentIndex = (current.avatarAccentIndex + 1) % AVATAR_ACCENT_COUNT)
        _identity.value = updated
        persistIdentity(updated)
    }

    /**
     * Marks onboarding as completed and persists the updated identity.
     */
    fun completeOnboarding() {
        val current = _identity.value
        if (current.onboardingCompleted) return
        val updated = current.copy(onboardingCompleted = true)
        _identity.value = updated
        persistIdentity(updated)
    }

    /**
     * Updates the local user's profile visibility [PrivacyMode] and persists the identity.
     */
    fun updatePrivacyMode(mode: PrivacyMode) {
        val current = _identity.value
        if (current.privacyMode == mode) return
        val updated = current.copy(privacyMode = mode)
        _identity.value = updated
        persistIdentity(updated)
    }

    /**
     * Updates the default file save directory and auto-save preference and persists the identity.
     */
    fun updateStorageSettings(defaultSaveDir: String, autoSaveFiles: Boolean) {
        val normalizedDir = defaultSaveDir.trim()
        val current = _identity.value
        if (current.defaultSaveDir == normalizedDir && current.autoSaveFiles == autoSaveFiles) return
        val updated = current.copy(
            defaultSaveDir = normalizedDir,
            autoSaveFiles = autoSaveFiles,
        )
        _identity.value = updated
        persistIdentity(updated)
    }

    /**
     * Adds a new friend or updates an existing friend's display name and last-seen timestamp.
     */
    fun addOrUpdateFriend(userId: String, displayName: String, nowMs: Long) {
        val normalizedId = userId.trim().uppercase()
        if (normalizedId.isEmpty()) return
        val normalizedName = displayName.trim().ifEmpty { normalizedId }

        val current = _friends.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.userId == normalizedId }
        if (existingIndex >= 0) {
            val existing = current[existingIndex]
            current[existingIndex] = existing.copy(
                displayName = normalizedName,
                lastSeenEpochMs = maxOf(existing.lastSeenEpochMs, nowMs),
            )
        } else {
            current.add(
                FriendRecord(
                    userId = normalizedId,
                    displayName = normalizedName,
                    addedAtEpochMs = nowMs,
                    lastSeenEpochMs = nowMs,
                ),
            )
        }
        _friends.value = current
        persistFriends(current)
    }

    /**
     * Removes the friend matching [userId] from the saved friends list and persists the change.
     */
    fun removeFriend(userId: String) {
        val normalizedId = userId.trim().uppercase()
        if (normalizedId.isEmpty()) return
        val updated = _friends.value.filterNot { it.userId == normalizedId || it.userId == userId }
        if (updated.size == _friends.value.size) return
        _friends.value = updated
        persistFriends(updated)
    }

    /**
     * Increments [duelWins] or [duelLosses] for [peerUserId] if they are a saved friend.
     */
    fun recordDuelResult(peerUserId: String, won: Boolean) {
        val current = _friends.value.toMutableList()
        val index = current.indexOfFirst { it.userId == peerUserId }
        if (index < 0) return

        val existing = current[index]
        current[index] = if (won) {
            existing.copy(duelWins = existing.duelWins + 1)
        } else {
            existing.copy(duelLosses = existing.duelLosses + 1)
        }
        _friends.value = current
        persistFriends(current)
    }

    /**
     * Returns `true` if [userId] is in the saved friends list.
     */
    fun isFriend(userId: String): Boolean {
        return _friends.value.any { it.userId == userId }
    }

    /**
     * Returns the persisted chat history with [peerUserId] in chronological order.
     */
    fun getChatHistory(peerUserId: String): List<ChatMessageRecord> {
        return chatHistory[peerUserId]?.toList().orEmpty()
    }

    /**
     * Appends [record] to the chat history for [ChatMessageRecord.peerUserId] and persists it.
     */
    fun appendChatMessage(record: ChatMessageRecord) {
        val list = chatHistory.getOrPut(record.peerUserId) { mutableListOf() }
        if (list.any { it.messageId == record.messageId }) return
        list.add(record)
        persistChatHistory()
    }

    /**
     * Resets cached friends, head-to-head records, and local chat messages.
     */
    fun clearLocalData() {
        _friends.value = emptyList()
        persistFriends(emptyList())
        chatHistory.clear()
        persistChatHistory()
    }

    private fun loadIdentity(): UserIdentity {
        val raw = storage.readText(KEY_IDENTITY)
        if (raw != null) {
            val decoded = runCatching { json.decodeFromString<UserIdentity>(raw) }.getOrNull()
            if (decoded != null && decoded.userId.isNotBlank()) {
                return decoded
            }
        }
        val generatedId = generateUserId(initialSeed)
        val created = UserIdentity(
            userId = generatedId,
            displayName = "Player ${generatedId.takeLast(4)}",
            avatarAccentIndex = 0,
        )
        persistIdentity(created)
        return created
    }

    private fun loadFriends(): List<FriendRecord> {
        val raw = storage.readText(KEY_FRIENDS) ?: return emptyList()
        return runCatching { json.decodeFromString<List<FriendRecord>>(raw) }.getOrDefault(emptyList())
    }

    private fun loadChatHistory(): MutableMap<String, MutableList<ChatMessageRecord>> {
        val raw = storage.readText(KEY_CHAT_HISTORY) ?: return mutableMapOf()
        val decoded = runCatching {
            json.decodeFromString<Map<String, List<ChatMessageRecord>>>(raw)
        }.getOrDefault(emptyMap())
        return decoded.mapValuesTo(mutableMapOf()) { (_, list) -> list.toMutableList() }
    }

    private fun persistIdentity(value: UserIdentity) {
        storage.writeText(KEY_IDENTITY, json.encodeToString(value))
    }

    private fun persistFriends(value: List<FriendRecord>) {
        storage.writeText(KEY_FRIENDS, json.encodeToString(value))
    }

    private fun persistChatHistory() {
        val snapshot: Map<String, List<ChatMessageRecord>> = chatHistory.mapValues { (_, list) -> list.toList() }
        storage.writeText(KEY_CHAT_HISTORY, json.encodeToString(snapshot))
    }

    private fun generateUserId(seed: UInt): String {
        val rng = DeterministicRng(seed)
        val suffix = buildString(4) {
            repeat(4) {
                append(RoomCodeGenerator.ALPHABET[rng.nextInt(RoomCodeGenerator.ALPHABET.length)])
            }
        }
        return "KMP-$suffix"
    }

    private companion object {
        const val KEY_IDENTITY = "user_identity_v2"
        const val KEY_FRIENDS = "user_friends_v1"
        const val KEY_CHAT_HISTORY = "user_chat_history_v1"
        const val AVATAR_ACCENT_COUNT = 6
    }
}
