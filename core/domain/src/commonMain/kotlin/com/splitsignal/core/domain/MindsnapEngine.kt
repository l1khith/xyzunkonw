package com.splitsignal.core.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Single rapid-fire Mindsnap arithmetic prompt with 4 choices.
 */
data class MindsnapQuestion(
    val prompt: String,
    val choices: List<String>,
    val correctIndex: Int,
)

/**
 * Live UI state for a 60-second Mindsnap Blitz Drill.
 */
data class MindsnapGameState(
    val seed: UInt,
    val questions: List<MindsnapQuestion>,
    val currentIndex: Int,
    val score: Int,
    val combo: Int,
    val bestCombo: Int,
    val multiplier: Int,
    val remainingSeconds: Int,
    val elapsedSeconds: Int,
    val moves: Int,
    val mistakes: Int,
    val percentComplete: Int,
    val isFinished: Boolean,
    val isRace: Boolean,
    val isDailyStep: Boolean,
    val opponentProgress: OpponentProgress?,
    val roundIndex: Int = 0,
) {
    val currentQuestion: MindsnapQuestion?
        get() = questions.getOrNull(currentIndex)

    val accuracyPercent: Int
        get() {
            val total = moves.coerceAtLeast(1)
            return (((total - mistakes).coerceAtLeast(0)) * 100 / total).coerceIn(0, 100)
        }
}

/**
 * Deterministic generator for Mindsnap rapid-fire arithmetic and missing-operator questions.
 */
object MindsnapGenerator {
    const val DEFAULT_QUESTION_COUNT = 12
    const val BLITZ_DURATION_SECONDS = 60

    /**
     * Generates [count] deterministic [MindsnapQuestion] items from [seed].
     */
    fun generateDeck(seed: UInt, count: Int = DEFAULT_QUESTION_COUNT): List<MindsnapQuestion> {
        val rng = DeterministicRng(seed)
        return List(count) { idx ->
            when (idx % 3) {
                0 -> generateMultiplyOrDivide(rng)
                1 -> generateMissingOperator(rng)
                else -> generateAddSubtract(rng)
            }
        }
    }

    private fun generateMultiplyOrDivide(rng: DeterministicRng): MindsnapQuestion {
        val isDiv = rng.nextInt(2) == 0
        if (isDiv) {
            val b = rng.nextInt(3, 14)
            val ans = rng.nextInt(4, 18)
            val a = b * ans
            return numericQuestion("$a ÷ $b = ?", ans, rng)
        }
        val a = rng.nextInt(6, 21)
        val b = rng.nextInt(3, 13)
        return numericQuestion("$a × $b = ?", a * b, rng)
    }

    private fun generateAddSubtract(rng: DeterministicRng): MindsnapQuestion {
        val isSub = rng.nextInt(2) == 0
        val a = rng.nextInt(18, 95)
        val b = rng.nextInt(12, 65)
        return if (isSub) {
            val hi = maxOf(a, b) + 10
            val lo = minOf(a, b)
            numericQuestion("$hi − $lo = ?", hi - lo, rng)
        } else {
            numericQuestion("$a + $b = ?", a + b, rng)
        }
    }

    private fun generateMissingOperator(rng: DeterministicRng): MindsnapQuestion {
        val ops = listOf(MathOp.ADD, MathOp.SUB, MathOp.MUL, MathOp.DIV)
        val chosen = ops[rng.nextInt(ops.size)]
        val (a, b, result) = when (chosen) {
            MathOp.ADD -> {
                val x = rng.nextInt(8, 30)
                val y = rng.nextInt(7, 25)
                Triple(x, y, x + y)
            }
            MathOp.SUB -> {
                val y = rng.nextInt(5, 22)
                val res = rng.nextInt(6, 28)
                Triple(y + res, y, res)
            }
            MathOp.MUL -> {
                val x = rng.nextInt(4, 13)
                val y = rng.nextInt(3, 12)
                Triple(x, y, x * y)
            }
            MathOp.DIV -> {
                val y = rng.nextInt(3, 12)
                val res = rng.nextInt(3, 15)
                Triple(y * res, y, res)
            }
        }
        val choices = ops.map { it.symbol }
        val correctIndex = ops.indexOf(chosen)
        return MindsnapQuestion(
            prompt = "$a  [ ? ]  $b  =  $result",
            choices = choices,
            correctIndex = correctIndex,
        )
    }

    private fun numericQuestion(prompt: String, answer: Int, rng: DeterministicRng): MindsnapQuestion {
        val distractors = mutableSetOf<Int>()
        val offsets = listOf(-10, -4, -2, -1, 1, 2, 4, 8, 10, 12)
        var attempts = 0
        while (distractors.size < 3 && attempts < 30) {
            attempts++
            val candidate = answer + offsets[rng.nextInt(offsets.size)]
            if (candidate != answer && candidate > 0) {
                distractors.add(candidate)
            }
        }
        while (distractors.size < 3) {
            distractors.add(answer + distractors.size + 1)
        }
        val all = (distractors.take(3) + answer).toMutableList()
        rng.shuffle(all)
        val correctIdx = all.indexOf(answer)
        return MindsnapQuestion(
            prompt = prompt,
            choices = all.map { it.toString() },
            correctIndex = correctIdx,
        )
    }
}

/**
 * State engine for 60-second Mindsnap Blitz Drill with rising `1x → 5x` Combo Fire Multiplier.
 */
class MindsnapEngine(
    seed: UInt,
    private val isRace: Boolean,
    private val isDailyStep: Boolean = false,
    private val roundIndex: Int = 0,
    private val transport: SyncTransport,
    private val scope: CoroutineScope,
) {
    private val _state: MutableStateFlow<MindsnapGameState>
    val state: StateFlow<MindsnapGameState>

    private var timerJob: Job? = null
    private var receiveJob: Job? = null

    init {
        val deck = MindsnapGenerator.generateDeck(seed)
        _state = MutableStateFlow(createInitialState(seed, deck))
        state = _state.asStateFlow()
        startTimer()
        if (isRace) observeTransport()
    }

    /**
     * Submits choice [choiceIndex] (`0..3`) for the current question.
     */
    suspend fun submitAnswer(choiceIndex: Int) {
        val cur = _state.value
        if (cur.isFinished) return
        val q = cur.currentQuestion ?: return
        val correct = choiceIndex == q.correctIndex

        val nextCombo = if (correct) cur.combo + 1 else 0
        val nextBestCombo = maxOf(cur.bestCombo, nextCombo)
        val nextMult = (1 + nextCombo / 2).coerceIn(1, 5)
        val points = if (correct) 100 * nextMult else 0
        val nextScore = cur.score + points
        val nextMoves = cur.moves + 1
        val nextMistakes = cur.mistakes + if (correct) 0 else 1

        val nextIndex = if (correct) cur.currentIndex + 1 else cur.currentIndex
        val finished = nextIndex >= cur.questions.size
        val pct = ((nextIndex * 100) / cur.questions.size.coerceAtLeast(1)).coerceIn(0, 100)

        _state.update {
            it.copy(
                currentIndex = nextIndex,
                score = nextScore,
                combo = nextCombo,
                bestCombo = nextBestCombo,
                multiplier = nextMult,
                moves = nextMoves,
                mistakes = nextMistakes,
                percentComplete = pct,
                isFinished = finished,
            )
        }
        if (finished) timerJob?.cancel()
        if (isRace) emitProgress()
    }

    /**
     * Eliminates/auto-answers the current question via a Hint token.
     */
    suspend fun applyHint() {
        val cur = _state.value
        if (cur.isFinished) return
        val q = cur.currentQuestion ?: return
        submitAnswer(q.correctIndex)
    }

    /**
     * Restarts with a fresh deterministic [newSeed].
     */
    fun restart(newSeed: UInt) {
        val deck = MindsnapGenerator.generateDeck(newSeed)
        _state.value = createInitialState(newSeed, deck)
        startTimer()
    }

    /**
     * Releases timer and network coroutines.
     */
    fun dispose() {
        timerJob?.cancel()
        receiveJob?.cancel()
    }

    private fun createInitialState(seed: UInt, deck: List<MindsnapQuestion>): MindsnapGameState = MindsnapGameState(
        seed = seed,
        questions = deck,
        currentIndex = 0,
        score = 0,
        combo = 0,
        bestCombo = 0,
        multiplier = 1,
        remainingSeconds = MindsnapGenerator.BLITZ_DURATION_SECONDS,
        elapsedSeconds = 0,
        moves = 0,
        mistakes = 0,
        percentComplete = 0,
        isFinished = false,
        isRace = isRace,
        isDailyStep = isDailyStep,
        opponentProgress = if (isRace) OpponentProgress() else null,
        roundIndex = roundIndex,
    )

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                delay(1_000L)
                val cur = _state.value
                if (cur.isFinished) break
                val nextRem = (cur.remainingSeconds - 1).coerceAtLeast(0)
                val nextElapsed = cur.elapsedSeconds + 1
                val timedOut = nextRem == 0
                _state.update {
                    it.copy(
                        remainingSeconds = nextRem,
                        elapsedSeconds = nextElapsed,
                        isFinished = it.isFinished || timedOut,
                    )
                }
                if (timedOut) {
                    if (isRace) emitProgress()
                    break
                }
            }
        }
    }

    private fun observeTransport() {
        receiveJob?.cancel()
        receiveJob = scope.launch {
            transport.incomingPackets.collect { packet ->
                if (packet is SyncPacket.DuelProgress && packet.roundIndex == roundIndex) {
                    _state.update {
                        it.copy(
                            opponentProgress = OpponentProgress(
                                percent = packet.percent,
                                mistakes = packet.mistakes,
                                elapsedSeconds = packet.elapsedSeconds,
                                finished = packet.finished,
                            ),
                        )
                    }
                }
            }
        }
    }

    private suspend fun emitProgress() {
        val cur = _state.value
        transport.send(
            SyncPacket.DuelProgress(
                roundIndex = roundIndex,
                percent = cur.percentComplete,
                mistakes = cur.mistakes,
                elapsedSeconds = cur.elapsedSeconds,
                finished = cur.isFinished,
                score = cur.score,
            ),
        )
    }
}
