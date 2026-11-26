package com.splitsignal.app.platform

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

actual object AppLoggerBridge : AppLogger {
    private const val TAG = "SplitSignal"
    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
            Log.i(TAG, "Firebase Analytics initialized successfully")
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to initialize Firebase Analytics: ${e.message}")
        }
    }

    override fun logEvent(name: String, params: Map<String, Any>) {
        Log.d(TAG, "Event: $name $params")
        firebaseAnalytics?.let { fa ->
            val bundle = Bundle().apply {
                params.forEach { (k, v) ->
                    when (v) {
                        is String -> putString(k, v)
                        is Int -> putInt(k, v)
                        is Long -> putLong(k, v)
                        is Double -> putDouble(k, v)
                        is Boolean -> putBoolean(k, v)
                        else -> putString(k, v.toString())
                    }
                }
            }
            fa.logEvent(name, bundle)
        }
    }

    override fun logScreen(screenName: String) {
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, mapOf(
            FirebaseAnalytics.Param.SCREEN_NAME to screenName,
            FirebaseAnalytics.Param.SCREEN_CLASS to screenName,
        ))
    }

    override fun logGameCompleted(gameType: String, score: Int, timeSeconds: Int, eloChange: Int) {
        logEvent("game_completed", mapOf(
            "game_type" to gameType,
            "score" to score,
            "time_seconds" to timeSeconds,
            "elo_change" to eloChange,
        ))
    }

    override fun logDuelEvent(action: String, opponentId: String, role: String) {
        logEvent("duel_action", mapOf(
            "action" to action,
            "opponent_id" to opponentId,
            "player_role" to role,
        ))
    }

    override fun logError(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    override fun setUserId(userId: String) {
        firebaseAnalytics?.setUserId(userId)
    }

    override fun setUserProperty(name: String, value: String) {
        firebaseAnalytics?.setUserProperty(name, value)
    }
}
