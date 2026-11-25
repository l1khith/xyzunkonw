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
 * Recall prompt mode for Memory Matrix + Math Recall.
 */
enum class MemoryRecallMode {
    SUM_TARGET,
    ASCENDING_ORDER,
}

/**
 * Deterministic Memory Matrix puzzle definition.
 */
data class MemoryMatrixBoard(
    val seed: UInt,
    val dimension: Int,
    val litTileValues: Map<Int, Int>,
    val recallMode: MemoryRecallMode,
    val targetSum: Int?,
    val requiredCount: Int,
    val expectedOrderIndices: List<Int>,
) {
    val cellCount: Int get() = dimension * dimension

    val promptText: String
        get() = when (recallMode) {
            MemoryRecallMode.SUM_TARGET ->
                "Tap the $requiredCount hidden tiles that sum to ${targetSum ?: 0}"
            MemoryRecallMode.ASCENDING_ORDER ->
                "Tap all $requiredCount lit tiles from smallest to largest"
        }
}

/**
 * Live UI state for a solo or 1v1 Memory Matrix + Math Recall session.
 */
data class MemoryMatrixGameState(
    val seed: UInt,
    val board: MemoryMatrixBoard,
    val isRevealing: Boolean,
    val selectedIndices: List<Int>,
    val wrongFlashIndex: Int?,
    val moves: Int,
    val mistakes: Int,
    val elapsedSeconds: Int,
    val percentComplete: Int,
    val isSolved: Boolean,
    val isRace: Boolean,
    val isDailyStep: Boolean,
    val opponentProgress: OpponentProgress?,
    val roundIndex: Int = 0,
) {
    val accuracyPercent: Int
        get() {
            val total = moves.coerceAtLeast(1)
            return (((total - mistakes).coerceAtLeast(0)) * 100 / total).coerceIn(0, 100)
        }
}

/**
 * Deterministic generator for [MemoryMatrixBoard].
 */
object MemoryMatrixGenerator {
    /**
     * Generates a deterministic 4×4 (or 5×5) Memory Matrix board from [seed].
     */
    fun generate(seed: UInt, dimension: Int = 4): MemoryMatrixBoard {
        val rng = DeterministicRng(seed)
        val cellCount = dimension * dimension
        val indices = (0 until cellCount).toMutableList()
        rng.shuffle(indices)

        val recallMode = if (rng.nextInt(2) == 0) {
            MemoryRecallMode.SUM_TARGET
        } else {
            MemoryRecallMode.ASCENDING_ORDER
        }

        return when (recallMode) {
            MemoryRecallMode.SUM_TARGET -> {
                // 6 lit tiles with distinct values, 3 of which uniquely sum to targetSum
                val litIndices = indices.take(6)
                val distinctPool = (1..15).toMutableList()
                rng.shuffle(distinctPool)

                // Pick a subset of 3 whose sum is unique among all 3-subsets of the 6 lit tiles
                var chosenValues = distinctPool.take(6)
                var targetIndices = litIndices.take(3)
                var targetSum = chosenValues.take(3).sum()

                for (attempt in 0 until 20) {
                    val candidate = distinctPool.drop(attempt % 5).take(6)
                    if (candidate.size < 6) break
                    val sumCounts = mutableMapOf<Int, Int>()
                    var bestSub: List<Int>? = null
                    for (a in 0 until 6) {
                        for (b in a + 1 until 6) {
                            for (c in b + 1 until 6) {
                                val s = candidate[a] + candidate[b] + candidate[c]
                                sumCounts[s] = (sumCounts[s] ?: 0) + 1
                            }
                        }
                    }
                    val uniqueSum = sumCounts.entries.firstOrNull { it.value == 1 }?.key
                    if (uniqueSum != null) {
                        outer@ for (a in 0 until 6) {
                            for (b in a + 1 until 6) {
                                for (c in b + 1 until 6) {
                                    if (candidate[a] + candidate[b] + candidate[c] == uniqueSum) {
                                        bestSub = listOf(litIndices[a], litIndices[b], litIndices[c])
                                        break@outer
                                    }
                                }
                            }
                        }
                        if (bestSub != null) {
                            chosenValues = candidate
                            targetIndices = bestSub
                            targetSum = uniqueSum
                            break
                        }
                    }
                }

                val map = litIndices.zip(chosenValues).toMap()
                MemoryMatrixBoard(
                    seed = seed,
                    dimension = dimension,
                    litTileValues = map,
                    recallMode = MemoryRecallMode.SUM_TARGET,
                    targetSum = targetSum,
                    requiredCount = 3,
                    expectedOrderIndices = targetIndices,
                )
            }

            MemoryRecallMode.ASCENDING_ORDER -> {
                val litCount = 5
                val litIndices = indices.take(litCount)
                val distinctPool = (1..18).toMutableList()
                rng.shuffle(distinctPool)
                val vals = distinctPool.take(litCount)
                val map = litIndices.zip(vals).toMap()
                val sortedIndices = map.entries.sortedBy { it.value }.map { it.key }
                MemoryMatrixBoard(
                    seed = seed,
                    dimension = dimension,
                    litTileValues = map,
                    recallMode = MemoryRecallMode.ASCENDING_ORDER,
                    targetSum = null,
                    requiredCount = litCount,
                    expectedOrderIndices = sortedIndices,
                )
            }
        }
    }
}

/**
 * State engine for Memory Matrix + Math Recall with 2.0s initial reveal and 0.8s hint re-flash.
 */
class MemoryMatrixEngine(
    seed: UInt,
    private val dimension: Int = 4,
    private val isRace: Boolean,
    private val isDailyStep: Boolean = false,
    private val roundIndex: Int = 0,
    private val initialRevealMs: Long = DEFAULT_REVEAL_MS,
    private val transport: SyncTransport,
    private val scope: CoroutineScope,
) {
    private val _state: MutableStateFlow<MemoryMatrixGameState>
    val state: StateFlow<MemoryMatrixGameState>

    private var timerJob: Job? = null
    private var revealJob: Job? = null
    private var receiveJob: Job? = null

    init {
        val board = MemoryMatrixGenerator.generate(seed, dimension)
        _state = MutableStateFlow(createInitialState(board))
        state = _state.asStateFlow()
        scheduleHide(initialRevealMs)
        startTimer()
        if (isRace) observeTransport()
    }

    /**
     * Taps cell [index] during recall phase.
     */
    suspend fun tapCell(index: Int) {
        val cur = _state.value
        if (cur.isSolved || cur.isRevealing || index !in 0 until cur.board.cellCount) return
        if (index in cur.selectedIndices) return

        val board = cur.board
        val isValidTap = when (board.recallMode) {
            MemoryRecallMode.SUM_TARGET -> index in board.expectedOrderIndices
            MemoryRecallMode.ASCENDING_ORDER -> {
                val expectedNext = board.expectedOrderIndices.getOrNull(cur.selectedIndices.size)
                index == expectedNext
            }
        }

        if (!isValidTap) {
            _state.update {
                it.copy(
                    wrongFlashIndex = index,
                    moves = it.moves + 1,
                    mistakes = it.mistakes + 1,
                )
            }
            if (isRace) emitProgress()
            return
        }

        val nextSelected = cur.selectedIndices + index
        val solved = nextSelected.size == board.requiredCount
        val pct = ((nextSelected.size * 100) / board.requiredCount).coerceIn(0, 100)

        _state.update {
            it.copy(
                selectedIndices = nextSelected,
                wrongFlashIndex = null,
                moves = it.moves + 1,
                percentComplete = pct,
                isSolved = solved,
            )
        }
        if (solved) timerJob?.cancel()
        if (isRace) emitProgress()
    }

    /**
     * Re-flashes the lit tiles for `0.8s` (`800ms`) using a Hint / Peek token.
     */
    fun applyPeekHint(peekDurationMs: Long = PEEK_REVEAL_MS) {
        val cur = _state.value
        if (cur.isSolved) return
        scheduleHide(peekDurationMs)
    }

    /**
     * Restarts with a fresh deterministic [newSeed].
     */
    fun restart(newSeed: UInt) {
        val board = MemoryMatrixGenerator.generate(newSeed, dimension)
        _state.value = createInitialState(board)
        scheduleHide(initialRevealMs)
        startTimer()
    }

    /**
     * Releases timer, reveal, and network coroutines.
     */
    fun dispose() {
        timerJob?.cancel()
        revealJob?.cancel()
        receiveJob?.cancel()
    }

    private fun scheduleHide(delayMs: Long) {
        revealJob?.cancel()
        _state.update { it.copy(isRevealing = true, wrongFlashIndex = null) }
        if (delayMs <= 0L) {
            _state.update { it.copy(isRevealing = false) }
            return
        }
        revealJob = scope.launch {
            delay(delayMs)
            _state.update { it.copy(isRevealing = false) }
        }
    }

    private fun createInitialState(board: MemoryMatrixBoard): MemoryMatrixGameState = MemoryMatrixGameState(
        seed = board.seed,
        board = board,
        isRevealing = true,
        selectedIndices = emptyList(),
        wrongFlashIndex = null,
        moves = 0,
        mistakes = 0,
        elapsedSeconds = 0,
        percentComplete = 0,
        isSolved = false,
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
                if (_state.value.isSolved) break
                _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
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
                finished = cur.isSolved,
            ),
        )
    }

    companion object {
        const val DEFAULT_REVEAL_MS = 2_000L
        const val PEEK_REVEAL_MS = 800L
    }
}
