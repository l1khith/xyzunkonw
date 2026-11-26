package com.splitsignal.core.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Duolingo-style streak tiers unlocked by consecutive daily solves.
 */
enum class StreakTier(
    val label: String,
    val minDays: Int,
) {
    NONE("No Streak", 0),
    BRONZE("Bronze", 1),
    SILVER("Silver", 7),
    EMERALD("Emerald", 14),
    DIAMOND("Diamond", 30),
    ;

    companion object {
        /**
         * Returns the highest [StreakTier] earned for [days].
         */
        fun forDays(days: Int): StreakTier {
            return entries.lastOrNull { days >= it.minDays } ?: NONE
        }
    }
}

/**
 * Competitive Elo skill leagues from Bronze Tactician (1200) to Grandmaster (2100+).
 */
enum class EloLeague(
    val title: String,
    val minElo: Int,
) {
    BRONZE("Bronze Tactician", 0),
    SILVER("Silver Strategist", 1350),
    GOLD("Gold Analyst", 1550),
    PLATINUM("Platinum Master", 1800),
    GRANDMASTER("Grandmaster", 2100),
    ;

    companion object {
        /**
         * Resolves the [EloLeague] for the given [elo] rating.
         */
        fun forElo(elo: Int): EloLeague {
            return entries.lastOrNull { elo >= it.minElo } ?: BRONZE
        }
    }
}

/**
 * Competence trophies earned through flawless, rapid, or competitive mastery.
 */
enum class CompetenceTrophy(
    val title: String,
    val description: String,
) {
    FLAWLESS_SOLVE("Flawless Clear", "Solved a puzzle with 0 mistakes"),
    SPEED_DEMON("Speed Demon", "Cleared a puzzle in under 30s"),
    DUEL_VICTOR("Duel Victor", "Defeated a peer in a 1v1 LAN duel"),
    DIAMOND_TIER_1("Diamond Tier 1", "Reached a 7-day daily streak"),
}

/**
 * Unlockable visual board themes purchasable with in-game coins.
 */
enum class BoardTheme(
    val id: String,
    val label: String,
    val priceCoins: Int,
) {
    OBSIDIAN_DARK("obsidian", "Obsidian Dark", 0),
    PAPER_IVORY("ivory", "Paper Ivory", 120),
    SOLARIZED_SLATE("slate", "Monochrome Slate", 180),
}

/**
 * Detailed coin, XP, Elo, streak, and cognitive analytics summary produced after completing a puzzle or duel.
 */
data class SolveRewardBreakdown(
    val baseCoins: Int,
    val speedBonusCoins: Int,
    val flawlessBonusCoins: Int,
    val totalCoins: Int,
    val xpEarned: Int,
    val eloDelta: Int,
    val streakExtended: Boolean,
    val newStreakDays: Int,
    val scorecardText: String,
    val modeLabel: String = "Puzzle",
    val elapsedSeconds: Int = 0,
    val accuracyPercent: Int = 100,
    val avgSecondsPerMove: Float = 0f,
    val bestCombo: Int = 0,
    val mistakes: Int = 0,
    val totalMoves: Int = 0,
    val isVictory: Boolean = true,
)

/**
 * Persistent player progression state covering streaks, currencies, rank XP, Elo leagues,
 * Daily Brain Workout progress, and lifetime cognitive stats.
 */
data class ProgressionState(
    val streakDays: Int = 0,
    val dailyCompletedToday: Boolean = false,
    val dailyStepsCompleted: Int = 0,
    val streakFreezes: Int = 1,
    val coins: Int = 0,
    val xp: Int = 0,
    val elo: Int = 0,
    val solvedCount: Int = 0,
    val duelWins: Int = 0,
    val totalMatches: Int = 0,
    val fastestSolveSeconds: Int? = null,
    val bestAccuracyPercent: Int = 100,
    val modeClears: Map<GameType, Int> = emptyMap(),
    val unlockedThemes: Set<BoardTheme> = setOf(BoardTheme.OBSIDIAN_DARK),
    val activeTheme: BoardTheme = BoardTheme.OBSIDIAN_DARK,
    val hintTokens: Int = 2,
    val unlockedTrophies: Set<CompetenceTrophy> = emptySet(),
) {
    /** Competitive league derived from [elo]. */
    val league: EloLeague
        get() = EloLeague.forElo(elo)

    /** Most recently unlocked competence trophy title, or `"Contender"` if none unlocked. */
    val topTrophyTitle: String
        get() = unlockedTrophies.lastOrNull()?.title ?: "Contender"

    /** Human-readable rank title derived from total accumulated [xp]. */
    val rankTitle: String
        get() = when {
            xp < 300 -> "Signal Initiate"
            xp < 800 -> "Grid Scout"
            xp < 1500 -> "Cipher Analyst"
            xp < 3000 -> "Logic Specialist"
            xp < 6000 -> "Grand Tactician"
            else -> "Apex Signalmaster"
        }
}

/**
 * Manages player progression, solve rewards, rank titles, and coin shop purchases.
 */
class ProgressionStore(
    initialState: ProgressionState = ProgressionState(),
) {
    private val _state: MutableStateFlow<ProgressionState> = MutableStateFlow(initialState)

    /**
     * Observable progression state stream.
     */
    val state: StateFlow<ProgressionState> = _state.asStateFlow()

    /**
     * Records a completed puzzle solve, updates currencies/streak/Elo, and returns a [SolveRewardBreakdown].
     */
    fun recordSolve(
        size: SudokuSize,
        difficulty: SudokuDifficulty,
        elapsedSeconds: Int,
        mistakes: Int,
        isDailySprint: Boolean,
        isRaceWin: Boolean?,
    ): SolveRewardBreakdown {
        val current = _state.value
        val difficultyCoins = when (difficulty) {
            SudokuDifficulty.EASY -> 20
            SudokuDifficulty.MEDIUM -> 35
            SudokuDifficulty.HARD -> 55
            SudokuDifficulty.MASTER -> 80
        }
        val sizeBonus = if (size == SudokuSize.CLASSIC_9X9) 15 else 0
        val dailyBonus = if (isDailySprint) 20 else 0
        val raceBonus = if (isRaceWin == true) 25 else 0
        val baseCoins = difficultyCoins + sizeBonus + dailyBonus + raceBonus

        val parSeconds = if (size == SudokuSize.SPRINT_6X6) {
            difficulty.parSeconds6
        } else {
            difficulty.parSeconds9
        }
        val speedBonusCoins = when {
            elapsedSeconds <= parSeconds / 2 -> 25
            elapsedSeconds <= parSeconds -> 15
            else -> 0
        }
        val flawlessBonusCoins = if (mistakes == 0) 20 else 0
        val totalCoins = baseCoins + speedBonusCoins + flawlessBonusCoins

        val xpEarned = totalCoins * 2 + if (isDailySprint) 30 else 0
        val eloDelta = when (isRaceWin) {
            true -> 24
            false -> -12
            null -> if (mistakes == 0 && elapsedSeconds <= parSeconds) 5 else 2
        }

        val streakExtended = !current.dailyCompletedToday
        val newStreakDays = if (streakExtended) current.streakDays + 1 else current.streakDays
        val tier = StreakTier.forDays(newStreakDays)
        val resultTag = when (isRaceWin) {
            true -> "RACE WIN"
            false -> "RACE COMPLETE"
            null -> if (isDailySprint) "DAILY SPRINT" else "SOLO SOLVE"
        }

        val scorecardText = buildString {
            append("SplitSignal Sudoku • $resultTag\n")
            append("${size.label} (${difficulty.label}) • ${elapsedSeconds}s • $mistakes mistake(s)\n")
            append("Streak: ${newStreakDays}d (${tier.label}) • +$totalCoins Coins • +$xpEarned XP")
        }

        val gameType = if (size == SudokuSize.SPRINT_6X6) GameType.SUDOKU_6X6 else GameType.SUDOKU_9X9
        _state.update { state ->
            val updatedClears = state.modeClears.toMutableMap().apply {
                this[gameType] = (this[gameType] ?: 0) + 1
            }
            val nextFastest = state.fastestSolveSeconds?.let { minOf(it, elapsedSeconds) } ?: elapsedSeconds
            val updatedTrophies = collectNewlyUnlockedTrophies(
                existing = state.unlockedTrophies,
                mistakes = mistakes,
                elapsedSeconds = elapsedSeconds,
                isRaceWin = isRaceWin,
                newStreakDays = newStreakDays,
            )
            state.copy(
                streakDays = newStreakDays,
                dailyCompletedToday = state.dailyCompletedToday || isDailySprint,
                coins = state.coins + totalCoins,
                xp = state.xp + xpEarned,
                elo = (state.elo + eloDelta).coerceAtLeast(MIN_ELO),
                solvedCount = state.solvedCount + 1,
                totalMatches = state.totalMatches + 1,
                duelWins = state.duelWins + if (isRaceWin == true) 1 else 0,
                fastestSolveSeconds = nextFastest,
                modeClears = updatedClears,
                unlockedTrophies = updatedTrophies,
            )
        }

        return SolveRewardBreakdown(
            baseCoins = baseCoins,
            speedBonusCoins = speedBonusCoins,
            flawlessBonusCoins = flawlessBonusCoins,
            totalCoins = totalCoins,
            xpEarned = xpEarned,
            eloDelta = eloDelta,
            streakExtended = streakExtended,
            newStreakDays = newStreakDays,
            scorecardText = scorecardText,
            modeLabel = gameType.displayName,
            elapsedSeconds = elapsedSeconds,
            accuracyPercent = if (mistakes == 0) 100 else (100 - mistakes * 8).coerceIn(40, 99),
            avgSecondsPerMove = if (elapsedSeconds > 0) elapsedSeconds / 18f else 0f,
            mistakes = mistakes,
            isVictory = isRaceWin != false,
        )
    }

    /**
     * Records a solve or 1v1 duel outcome for any of the 6 Matiks puzzle modes or Triathlon,
     * updating Elo, Daily Brain Workout progress, coins, XP, streak, and lifetime stats.
     */
    fun recordModeSolve(
        gameType: GameType,
        elapsedSeconds: Int,
        moves: Int,
        mistakes: Int,
        parSeconds: Int,
        bestCombo: Int = 0,
        isDailyStep: Boolean = false,
        isRaceWin: Boolean? = null,
    ): SolveRewardBreakdown {
        val current = _state.value
        val baseCoins = when (gameType) {
            GameType.TRIATHLON -> 75
            GameType.SUDOKU_9X9, GameType.KENKEN -> 45
            else -> 35
        } + (if (isDailyStep) 20 else 0) + (if (isRaceWin == true) 30 else 0)

        val speedBonusCoins = when {
            elapsedSeconds <= parSeconds / 2 -> 25
            elapsedSeconds <= parSeconds -> 15
            else -> 0
        }
        val flawlessBonusCoins = if (mistakes == 0) 20 else 0
        val totalCoins = baseCoins + speedBonusCoins + flawlessBonusCoins

        val xpEarned = totalCoins * 2 + bestCombo * 5 + if (isDailyStep) 25 else 0
        val eloDelta = when (isRaceWin) {
            true -> if (gameType == GameType.TRIATHLON) 36 else 24
            false -> -14
            null -> if (mistakes == 0 && elapsedSeconds <= parSeconds) 6 else 2
        }

        val nextDailySteps = if (isDailyStep) {
            (current.dailyStepsCompleted + 1).coerceAtMost(DAILY_WORKOUT_ORDER.size)
        } else {
            current.dailyStepsCompleted
        }
        val workoutJustCompleted = isDailyStep && nextDailySteps >= DAILY_WORKOUT_ORDER.size && !current.dailyCompletedToday
        val streakExtended = workoutJustCompleted || (!isDailyStep && !current.dailyCompletedToday && current.dailyStepsCompleted == 0)
        val newStreakDays = if (streakExtended) current.streakDays + 1 else current.streakDays

        val safeMoves = moves.coerceAtLeast(1)
        val accuracy = (((safeMoves - mistakes).coerceAtLeast(0)) * 100 / safeMoves).coerceIn(0, 100)
        val avgSecPerMove = elapsedSeconds.toFloat() / safeMoves.toFloat()

        val newElo = (current.elo + eloDelta).coerceAtLeast(MIN_ELO)
        val leagueTitle = EloLeague.forElo(newElo).title
        val resultTag = when (isRaceWin) {
            true -> "DUEL WIN"
            false -> "DUEL COMPLETE"
            null -> if (isDailyStep) "DAILY WORKOUT" else "SOLO CLEAR"
        }

        val scorecardText = buildString {
            append("SplitSignal • ${gameType.displayName} ($resultTag)\n")
            append("${elapsedSeconds}s • $accuracy% Acc • $mistakes mistake(s) • $leagueTitle ($newElo Elo)\n")
            append("${newStreakDays}d Streak • +$totalCoins Coins • +$xpEarned XP")
        }

        _state.update { state ->
            val updatedClears = state.modeClears.toMutableMap().apply {
                this[gameType] = (this[gameType] ?: 0) + 1
            }
            val nextFastest = state.fastestSolveSeconds?.let { minOf(it, elapsedSeconds) } ?: elapsedSeconds
            val updatedTrophies = collectNewlyUnlockedTrophies(
                existing = state.unlockedTrophies,
                mistakes = mistakes,
                elapsedSeconds = elapsedSeconds,
                isRaceWin = isRaceWin,
                newStreakDays = newStreakDays,
            )
            state.copy(
                streakDays = newStreakDays,
                dailyStepsCompleted = nextDailySteps,
                dailyCompletedToday = state.dailyCompletedToday || nextDailySteps >= DAILY_WORKOUT_ORDER.size,
                coins = state.coins + totalCoins,
                xp = state.xp + xpEarned,
                elo = newElo,
                solvedCount = state.solvedCount + 1,
                totalMatches = state.totalMatches + 1,
                duelWins = state.duelWins + if (isRaceWin == true) 1 else 0,
                fastestSolveSeconds = nextFastest,
                bestAccuracyPercent = maxOf(state.bestAccuracyPercent, accuracy),
                modeClears = updatedClears,
                unlockedTrophies = updatedTrophies,
            )
        }

        return SolveRewardBreakdown(
            baseCoins = baseCoins,
            speedBonusCoins = speedBonusCoins,
            flawlessBonusCoins = flawlessBonusCoins,
            totalCoins = totalCoins,
            xpEarned = xpEarned,
            eloDelta = eloDelta,
            streakExtended = streakExtended,
            newStreakDays = newStreakDays,
            scorecardText = scorecardText,
            modeLabel = gameType.displayName,
            elapsedSeconds = elapsedSeconds,
            accuracyPercent = accuracy,
            avgSecondsPerMove = avgSecPerMove,
            bestCombo = bestCombo,
            mistakes = mistakes,
            totalMoves = moves,
            isVictory = isRaceWin != false,
        )
    }

    /**
     * Purchases a Streak Freeze for 100 coins if affordable, returning `true` on success.
     */
    fun buyStreakFreeze(): Boolean {
        val current = _state.value
        if (current.coins < STREAK_FREEZE_COST) return false
        _state.update {
            it.copy(
                coins = it.coins - STREAK_FREEZE_COST,
                streakFreezes = it.streakFreezes + 1,
            )
        }
        return true
    }

    /**
     * Purchases a Hint Token for 40 coins if affordable, returning `true` on success.
     */
    fun buyHintToken(): Boolean {
        val current = _state.value
        if (current.coins < HINT_TOKEN_COST) return false
        _state.update {
            it.copy(
                coins = it.coins - HINT_TOKEN_COST,
                hintTokens = it.hintTokens + 1,
            )
        }
        return true
    }

    /**
     * Equips [theme] if already unlocked, or purchases and equips it if the player has enough coins.
     */
    fun buyOrSelectTheme(theme: BoardTheme): Boolean {
        val current = _state.value
        if (theme in current.unlockedThemes) {
            _state.update { it.copy(activeTheme = theme) }
            return true
        }
        if (current.coins < theme.priceCoins) return false
        _state.update {
            it.copy(
                coins = it.coins - theme.priceCoins,
                unlockedThemes = it.unlockedThemes + theme,
                activeTheme = theme,
            )
        }
        return true
    }

    /**
     * Consumes one Hint Token if available, returning `true` on success.
     */
    fun consumeHintToken(): Boolean {
        val current = _state.value
        if (current.hintTokens <= 0) return false
        _state.update {
            it.copy(hintTokens = it.hintTokens - 1)
        }
        return true
    }

    /**
     * Maps total accumulated [xp] to a player rank title.
     */
    fun rankTitle(xp: Int): String {
        return when {
            xp < 300 -> "Signal Initiate"
            xp < 800 -> "Grid Scout"
            xp < 1500 -> "Cipher Analyst"
            xp < 3000 -> "Logic Specialist"
            xp < 6000 -> "Grand Tactician"
            else -> "Apex Signalmaster"
        }
    }

    private fun collectNewlyUnlockedTrophies(
        existing: Set<CompetenceTrophy>,
        mistakes: Int,
        elapsedSeconds: Int,
        isRaceWin: Boolean?,
        newStreakDays: Int,
    ): Set<CompetenceTrophy> {
        val updated = existing.toMutableSet()
        if (mistakes == 0) updated.add(CompetenceTrophy.FLAWLESS_SOLVE)
        if (elapsedSeconds < 30) updated.add(CompetenceTrophy.SPEED_DEMON)
        if (isRaceWin == true) updated.add(CompetenceTrophy.DUEL_VICTOR)
        if (newStreakDays >= 7) updated.add(CompetenceTrophy.DIAMOND_TIER_1)
        return updated
    }

    companion object {
        const val STREAK_FREEZE_COST = 100
        const val HINT_TOKEN_COST = 40
        private const val MIN_ELO = 0

        /** 4-step Daily Brain Workout sequence. */
        val DAILY_WORKOUT_ORDER: List<GameType> = listOf(
            GameType.CROSS_MATH,
            GameType.MATH_MAZE,
            GameType.KENKEN,
            GameType.MINDSNAP,
        )

        /** 3-round Triathlon Duel sequence. */
        val TRIATHLON_ROUNDS: List<GameType> = listOf(
            GameType.MINDSNAP,
            GameType.MEMORY_MATRIX,
            GameType.CROSS_MATH,
        )
    }
}
