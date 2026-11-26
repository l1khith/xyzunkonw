package com.splitsignal.app.platform

actual object AppLoggerBridge : AppLogger {
    override fun logEvent(name: String, params: Map<String, Any>) {
        println("[SplitSignal:Event] $name -> $params")
    }

    override fun logScreen(screenName: String) {
        println("[SplitSignal:Screen] $screenName")
    }

    override fun logGameCompleted(gameType: String, score: Int, timeSeconds: Int, eloChange: Int) {
        println("[SplitSignal:Game] $gameType finished: score=$score, time=${timeSeconds}s, eloChange=$eloChange")
    }

    override fun logDuelEvent(action: String, opponentId: String, role: String) {
        println("[SplitSignal:Duel] $action with $opponentId as $role")
    }

    override fun logError(tag: String, message: String, throwable: Throwable?) {
        System.err.println("[$tag:ERROR] $message")
        throwable?.printStackTrace(System.err)
    }

    override fun setUserId(userId: String) {
        println("[SplitSignal:User] ID set to $userId")
    }

    override fun setUserProperty(name: String, value: String) {
        println("[SplitSignal:UserProp] $name = $value")
    }
}
