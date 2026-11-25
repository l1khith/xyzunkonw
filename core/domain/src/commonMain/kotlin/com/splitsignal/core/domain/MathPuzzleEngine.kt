package com.splitsignal.core.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Single mathematical constraint displayed to the Analyst alongside live board status.
 */
data class MathClue(
    val id: Int,
    val statement: String,
    val liveStatus: String,
    val isSatisfied: Boolean,
)

/**
 * Immutable definition for a 3x3 mathematical deduction level.
 */
data class MathPuzzleSpec(
    val levelIndex: Int,
    val title: String,
    val initialGrid: List<Int>,
    val targetRow0Sum: Int,
    val targetCol1Sum: Int,
    val targetDiagSum: Int,
    val requireCenterPrime: Boolean,
)

/**
 * Synchronized UI/engine state for the 3x3 mathematical deduction puzzle.
 */
data class MathGameState(
    val role: PlayerRole,
    val levelIndex: Int = 0,
    val title: String = "",
    val grid: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
    val clues: List<MathClue> = emptyList(),
    val isSolved: Boolean = false,
)

/**
 * Deterministic generator and clue evaluator for 3x3 deduction puzzles.
 */
object MathLevelGenerator {
    private const val GRID_SIZE = 9
    private const val CENTER_INDEX = 4
    private const val LEVEL_SEED_MIX: UInt = 0x9E3779B9u
    private val PRIMES: List<Int> = listOf(2, 3, 5, 7)
    private val PRIME_SET: Set<Int> = PRIMES.toSet()
    private val NON_PRIMES: List<Int> = listOf(1, 4, 6, 8, 9)

    /**
     * Deterministically generates a solvable [MathPuzzleSpec] for [seed] and [levelIndex].
     */
    fun generate(seed: UInt, levelIndex: Int): MathPuzzleSpec {
        if (levelIndex <= 0) {
            return MathPuzzleSpec(
                levelIndex = 0,
                title = "Level 01 · Warm-Up",
                initialGrid = listOf(2, 4, 1, 3, 4, 6, 7, 2, 5),
                targetRow0Sum = 10,
                targetCol1Sum = 12,
                targetDiagSum = 11,
                requireCenterPrime = true,
            )
        }

        val rng = DeterministicRng(seed xor (levelIndex.toUInt() * LEVEL_SEED_MIX))
        val solution = List(GRID_SIZE) { index ->
            if (index == CENTER_INDEX) {
                PRIMES[rng.nextInt(PRIMES.size)]
            } else {
                1 + rng.nextInt(9)
            }
        }

        val initial = MutableList(GRID_SIZE) { 1 + rng.nextInt(9) }
        val numberLabel = (levelIndex + 1).toString().padStart(2, '0')
        val spec = MathPuzzleSpec(
            levelIndex = levelIndex,
            title = "Level $numberLabel · Matrix",
            initialGrid = initial.toList(),
            targetRow0Sum = solution[0] + solution[1] + solution[2],
            targetCol1Sum = solution[1] + solution[4] + solution[7],
            targetDiagSum = solution[0] + solution[4] + solution[8],
            requireCenterPrime = true,
        )

        if (evaluateClues(spec, initial).all { it.isSatisfied }) {
            initial[CENTER_INDEX] = NON_PRIMES[rng.nextInt(NON_PRIMES.size)]
            return spec.copy(initialGrid = initial.toList())
        }
        return spec
    }

    /**
     * Evaluates all four mathematical clues of [spec] against the current 3x3 [grid].
     */
    fun evaluateClues(spec: MathPuzzleSpec, grid: List<Int>): List<MathClue> {
        val c0 = grid.getOrElse(0) { 0 }
        val c1 = grid.getOrElse(1) { 0 }
        val c2 = grid.getOrElse(2) { 0 }
        val c4 = grid.getOrElse(4) { 0 }
        val c7 = grid.getOrElse(7) { 0 }
        val c8 = grid.getOrElse(8) { 0 }

        val row0 = c0 + c1 + c2
        val col1 = c1 + c4 + c7
        val diag = c0 + c4 + c8
        val center = c4

        return listOf(
            MathClue(
                id = 0,
                statement = "Top row sum = ${spec.targetRow0Sum}",
                liveStatus = "Current: $row0",
                isSatisfied = row0 == spec.targetRow0Sum,
            ),
            MathClue(
                id = 1,
                statement = "Middle column sum = ${spec.targetCol1Sum}",
                liveStatus = "Current: $col1",
                isSatisfied = col1 == spec.targetCol1Sum,
            ),
            MathClue(
                id = 2,
                statement = "Main diagonal (↘) sum = ${spec.targetDiagSum}",
                liveStatus = "Current: $diag",
                isSatisfied = diag == spec.targetDiagSum,
            ),
            MathClue(
                id = 3,
                statement = "Center tile must be prime (2, 3, 5, 7)",
                liveStatus = "Current: $center",
                isSatisfied = !spec.requireCenterPrime || center in PRIME_SET,
            ),
        )
    }
}

/**
 * Coordinates the 3x3 mathematical deduction puzzle state and synchronizes updates over [SyncTransport].
 */
class MathPuzzleEngine(
    private val role: PlayerRole,
    seed: UInt,
    private val transport: SyncTransport,
    scope: CoroutineScope,
) {
    private var currentSeed: UInt = seed
    private var currentSpec: MathPuzzleSpec = MathLevelGenerator.generate(currentSeed, 0)

    private val _state: MutableStateFlow<MathGameState> = MutableStateFlow(
        buildStateForSpec(currentSpec),
    )

    /**
     * Observable state stream for the active mathematical puzzle.
     */
    val state: StateFlow<MathGameState> = _state.asStateFlow()

    private val collectorJob: Job = scope.launch {
        transport.incomingPackets.collect { packet ->
            when (packet) {
                is SyncPacket.TileIncremented -> applyTileChange(packet.cellIndex, packet.newValue)
                is SyncPacket.AdvanceLevel -> loadLevel(packet.nextLevelIndex)
                is SyncPacket.RestartRun -> {
                    currentSeed = Fnv1a.hash32(packet.roomCode)
                    loadLevel(0)
                }
                is SyncPacket.Handshake,
                is SyncPacket.DialUpdate,
                is SyncPacket.SwitchToggle,
                is SyncPacket.StateSync,
                is SyncPacket.AdvanceStage,
                is SyncPacket.BattleshipShot,
                is SyncPacket.BattleshipSonar,
                is SyncPacket.BattleshipRestart,
                is SyncPacket.SudokuStart,
                is SyncPacket.SudokuProgress,
                is SyncPacket.SudokuSabotage,
                is SyncPacket.DuelStart,
                is SyncPacket.DuelProgress,
                is SyncPacket.TriathlonRoundAdvance,
                is SyncPacket.FriendRequest,
                is SyncPacket.FriendAccept,
                is SyncPacket.ChatTextMessage,
                is SyncPacket.ChatTypingIndicator,
                is SyncPacket.FileTransferOffer,
                is SyncPacket.FileTransferDecision,
                is SyncPacket.FileTransferProgress,
                is SyncPacket.GameInvite,
                is SyncPacket.GameInviteResponse,
                -> Unit
            }
        }
    }

    /**
     * Cycles cell [index] through `1..9` and broadcasts [SyncPacket.TileIncremented] when called
     * by [PlayerRole.OPERATOR] while the level is unsolved.
     */
    suspend fun onCellTapped(index: Int) {
        val current = _state.value
        if (role != PlayerRole.OPERATOR || current.isSolved) return
        if (index !in 0 until GRID_CELL_COUNT) return

        val currentValue = current.grid[index]
        val nextValue = if (currentValue >= 9) 1 else currentValue + 1
        applyTileChange(index, nextValue)
        transport.send(SyncPacket.TileIncremented(cellIndex = index, newValue = nextValue))
    }

    /**
     * Advances to the next level and broadcasts [SyncPacket.AdvanceLevel].
     */
    suspend fun onNextLevel() {
        val nextLevelIndex = _state.value.levelIndex + 1
        loadLevel(nextLevelIndex)
        transport.send(SyncPacket.AdvanceLevel(nextLevelIndex = nextLevelIndex))
    }

    /**
     * Cancels the incoming packet collector job.
     */
    fun dispose() {
        collectorJob.cancel()
    }

    private fun applyTileChange(cellIndex: Int, newValue: Int) {
        if (cellIndex !in 0 until GRID_CELL_COUNT) return
        val boundedValue = newValue.coerceIn(1, 9)
        _state.update { current ->
            val updatedGrid = current.grid.toMutableList().apply {
                this[cellIndex] = boundedValue
            }.toList()
            val evaluatedClues = MathLevelGenerator.evaluateClues(currentSpec, updatedGrid)
            current.copy(
                grid = updatedGrid,
                clues = evaluatedClues,
                isSolved = evaluatedClues.all { it.isSatisfied },
            )
        }
    }

    private fun loadLevel(levelIndex: Int) {
        val targetIndex = levelIndex.coerceAtLeast(0)
        val spec = MathLevelGenerator.generate(currentSeed, targetIndex)
        currentSpec = spec
        _state.value = buildStateForSpec(spec)
    }

    private fun buildStateForSpec(spec: MathPuzzleSpec): MathGameState {
        val initialGrid = spec.initialGrid.toList()
        val clues = MathLevelGenerator.evaluateClues(spec, initialGrid)
        return MathGameState(
            role = role,
            levelIndex = spec.levelIndex,
            title = spec.title,
            grid = initialGrid,
            clues = clues,
            isSolved = clues.all { it.isSatisfied },
        )
    }

    private companion object {
        const val GRID_CELL_COUNT = 9
    }
}
