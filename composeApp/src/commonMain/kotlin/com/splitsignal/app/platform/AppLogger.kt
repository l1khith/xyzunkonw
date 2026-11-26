package com.splitsignal.app.platform

/**
 * Universal logging & analytics contract for SplitSignal.
 *
 * Implements a sidecar facade pattern across Kotlin Multiplatform:
 * - On Android: delegates to Firebase Analytics and Android Logcat.
 * - On Desktop: delegates to standard output / diagnostics stream.
 * - On Web (wasmJs): delegates to browser console.
 */
interface AppLogger {
    fun logEvent(name: String, params: Map<String, Any> = emptyMap())
    fun logScreen(screenName: String)
    fun logGameCompleted(gameType: String, score: Int, timeSeconds: Int, eloChange: Int)
    fun logDuelEvent(action: String, opponentId: String, role: String)
    fun logError(tag: String, message: String, throwable: Throwable? = null)
    fun setUserId(userId: String)
    fun setUserProperty(name: String, value: String)
}

expect object AppLoggerBridge : AppLogger
