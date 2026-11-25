package com.splitsignal.core.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Single tile in a Math Maze grid.
 */
data class MazeCell(
    val index: Int,
    val row: Int,
    val col: Int,
    val op: MathOp?,
    val operand: Int,
    val isStart: Boolean,
    val isExit: Boolean,
) {
    /** Compact display label such as `"START 4"`, `"+ 3"`, `"× 2"`, `"EXIT − 5"`. */
    val displayLabel: String
        get() = when {
            isStart -> "$operand"
            op == null -> "$operand"
            else -> "${op.symbol}$operand"
        }
}

/**
 * Deterministic Math Maze puzzle definition with guaranteed valid path to [targetGoal].
 */
data class MathMazeBoard(
    val seed: UInt,
    val dimension: Int,
    val startValue: Int,
    val targetGoal: Int,
    val cells: List<MazeCell>,
    val solutionPath: List<Int>,
) {
    val cellCount: Int get() = dimension * dimension
    val startIndex: Int get() = 0
    val exitIndex: Int get() = cellCount - 1
}

/**
 * Live UI state for a solo, daily workout, or 1v1 Math Maze session.
 */
data class MathMazeGameState(
    val seed: UInt,
    val board: MathMazeBoard,
    val path: List<Int>,
    val runningTotal: Int,
    val hintedNextIndex: Int?,
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
 * Deterministic generator for [MathMazeBoard] with orthogonal pathfinding and clean integer arithmetic.
 */
object MathMazeGenerator {
    /**
     * Generates a deterministic [dimension]×[dimension] Math Maze from [seed].
     */
    fun generate(seed: UInt, dimension: Int = 4): MathMazeBoard {
        val rng = DeterministicRng(seed)
        val path = buildRandomPath(dimension, rng)
        val startValue = rng.nextInt(2, 10)

        val cellOps = mutableMapOf<Int, Pair<MathOp, Int>>()
        var running = startValue
        for (i in 1 until path.size) {
            val idx = path[i]
            val (op, operand, nextTotal) = pickSafeOperation(running, rng)
            cellOps[idx] = op to operand
            running = nextTotal
        }
        val targetGoal = running

        val cells = List(dimension * dimension) { idx ->
            val r = idx / dimension
            val c = idx % dimension
            val isStart = idx == 0
            val isExit = idx == dimension * dimension - 1
            if (isStart) {
                MazeCell(idx, r, c, op = null, operand = startValue, isStart = true, isExit = false)
            } else {
                val pair = cellOps[idx] ?: pickDecoyOp(rng)
                MazeCell(idx, r, c, op = pair.first, operand = pair.second, isStart = false, isExit = isExit)
            }
        }

        return MathMazeBoard(
            seed = seed,
            dimension = dimension,
            startValue = startValue,
            targetGoal = targetGoal,
            cells = cells,
            solutionPath = path,
        )
    }

    /**
     * Applies [cell]'s operation to [currentTotal], returning `null` if division is non-exact.
     */
    fun applyCell(currentTotal: Int, cell: MazeCell): Int? {
        if (cell.isStart) return cell.operand
        return when (cell.op) {
            MathOp.ADD -> currentTotal + cell.operand
            MathOp.SUB -> currentTotal - cell.operand
            MathOp.MUL -> currentTotal * cell.operand
            MathOp.DIV -> if (cell.operand != 0 && currentTotal % cell.operand == 0) {
                currentTotal / cell.operand
            } else {
                null
            }
            null -> currentTotal
        }
    }

    private fun buildRandomPath(dim: Int, rng: DeterministicRng): List<Int> {
        // Random walk biased toward exit (dim-1, dim-1) with backtracking if cornered
        val target = dim * dim - 1
        repeat(40) {
            val visited = mutableSetOf(0)
            val path = mutableListOf(0)
            var cur = 0
            var steps = 0
            while (cur != target && steps < dim * dim * 3) {
                steps++
                val r = cur / dim
                val c = cur % dim
                val neighbors = listOf(r - 1 to c, r + 1 to c, r to c - 1, r to c + 1)
                    .filter { (nr, nc) -> nr in 0 until dim && nc in 0 until dim }
                    .map { (nr, nc) -> nr * dim + nc }
                    .filter { it !in visited }

                if (neighbors.isEmpty()) break
                // Bias toward moving down/right while keeping turns interesting
                val weighted = neighbors.sortedByDescending { nIdx ->
                    val nr = nIdx / dim
                    val nc = nIdx % dim
                    (nr - r) + (nc - c) + rng.nextInt(0, 3)
                }
                val next = weighted.first()
                visited.add(next)
                path.add(next)
                cur = next
            }
            if (cur == target && path.size >= dim + 1) {
                return path
            }
        }
        // Deterministic staircase fallback
        val fallback = mutableListOf<Int>()
        var r = 0
        var c = 0
        fallback.add(0)
        while (r < dim - 1 || c < dim - 1) {
            if (r < dim - 1 && (c == dim - 1 || rng.nextInt(2) == 0)) r++ else c++
            fallback.add(r * dim + c)
        }
        return fallback
    }

    private fun pickSafeOperation(running: Int, rng: DeterministicRng): Triple<MathOp, Int, Int> {
        val roll = rng.nextInt(100)
        return when {
            roll < 35 -> {
                val v = rng.nextInt(2, 10)
                Triple(MathOp.ADD, v, running + v)
            }
            roll < 65 -> {
                val v = rng.nextInt(1, 9)
                Triple(MathOp.SUB, v, running - v)
            }
            roll < 85 && abs(running) <= 30 -> {
                val v = rng.nextInt(2, 5)
                Triple(MathOp.MUL, v, running * v)
            }
            else -> {
                val divisors = (2..5).filter { running != 0 && running % it == 0 }
                if (divisors.isNotEmpty()) {
                    val d = divisors[rng.nextInt(divisors.size)]
                    Triple(MathOp.DIV, d, running / d)
                } else {
                    val v = rng.nextInt(2, 8)
                    Triple(MathOp.ADD, v, running + v)
                }
            }
        }
    }

    private fun pickDecoyOp(rng: DeterministicRng): Pair<MathOp, Int> {
        return when (rng.nextInt(4)) {
            0 -> MathOp.ADD to rng.nextInt(2, 10)
            1 -> MathOp.SUB to rng.nextInt(1, 9)
            2 -> MathOp.MUL to rng.nextInt(2, 5)
            else -> MathOp.DIV to rng.nextInt(2, 4)
        }
    }
}

/**
 * State engine for solo, daily workout, and 1v1 Math Maze races.
 */
class MathMazeEngine(
    seed: UInt,
    private val dimension: Int = 4,
    private val isRace: Boolean,
    private val isDailyStep: Boolean = false,
    private val roundIndex: Int = 0,
    private val transport: SyncTransport,
    private val scope: CoroutineScope,
) {
    private val _state: MutableStateFlow<MathMazeGameState>
    val state: StateFlow<MathMazeGameState>

    private var timerJob: Job? = null
    private var receiveJob: Job? = null

    init {
        val board = MathMazeGenerator.generate(seed, dimension)
        _state = MutableStateFlow(createInitialState(board))
        state = _state.asStateFlow()
        startTimer()
        if (isRace) observeTransport()
    }

    /**
     * Taps [cellIndex]: extends the path if orthogonally adjacent to current head,
     * or backtracks if tapping the previous tile or current head.
     */
    suspend fun tapCell(cellIndex: Int) {
        val cur = _state.value
        if (cur.isSolved || cellIndex !in 0 until cur.board.cellCount) return

        // Tapping current head (if length > 1) or immediate previous tile steps back
        if (cur.path.size > 1 && (cellIndex == cur.path.last() || cellIndex == cur.path[cur.path.size - 2])) {
            stepBack()
            return
        }

        // Ignore already-visited earlier cells
        if (cellIndex in cur.path) return

        val head = cur.path.last()
        val hr = head / cur.board.dimension
        val hc = head % cur.board.dimension
        val tr = cellIndex / cur.board.dimension
        val tc = cellIndex % cur.board.dimension
        if (abs(hr - tr) + abs(hc - tc) != 1) return

        val cell = cur.board.cells[cellIndex]
        val nextTotal = MathMazeGenerator.applyCell(cur.runningTotal, cell)
        if (nextTotal == null) {
            _state.update { it.copy(moves = it.moves + 1, mistakes = it.mistakes + 1) }
            if (isRace) emitProgress()
            return
        }

        val nextPath = cur.path + cellIndex
        val hitExit = cell.isExit
        val solved = hitExit && nextTotal == cur.board.targetGoal
        val wrongExit = hitExit && nextTotal != cur.board.targetGoal

        val pct = if (solved) 100 else ((nextPath.size * 100) / cur.board.solutionPath.size).coerceIn(0, 95)

        _state.update {
            it.copy(
                path = nextPath,
                runningTotal = nextTotal,
                hintedNextIndex = null,
                moves = it.moves + 1,
                mistakes = it.mistakes + if (wrongExit) 1 else 0,
                percentComplete = pct,
                isSolved = solved,
            )
        }
        if (solved) timerJob?.cancel()
        if (isRace) emitProgress()
    }

    /**
     * Steps back one cell along the player's current path.
     */
    suspend fun stepBack() {
        val cur = _state.value
        if (cur.isSolved || cur.path.size <= 1) return
        val trimmed = cur.path.dropLast(1)
        val recomputed = recomputeTotal(cur.board, trimmed)
        val pct = ((trimmed.size * 100) / cur.board.solutionPath.size).coerceIn(0, 95)
        _state.update {
            it.copy(
                path = trimmed,
                runningTotal = recomputed,
                hintedNextIndex = null,
                percentComplete = pct,
            )
        }
        if (isRace) emitProgress()
    }

    /**
     * Highlights the next cell on the deterministic solution path (or rewinds to the common prefix first).
     */
    suspend fun applyHint() {
        val cur = _state.value
        if (cur.isSolved) return
        val sol = cur.board.solutionPath
        // Find longest common prefix with solutionPath
        var prefixLen = 0
        while (prefixLen < cur.path.size && prefixLen < sol.size && cur.path[prefixLen] == sol[prefixLen]) {
            prefixLen++
        }
        val alignedPath = sol.take(prefixLen.coerceAtLeast(1))
        val nextHintIdx = sol.getOrNull(alignedPath.size) ?: sol.last()
        val recomputed = recomputeTotal(cur.board, alignedPath)
        _state.update {
            it.copy(
                path = alignedPath,
                runningTotal = recomputed,
                hintedNextIndex = nextHintIdx,
            )
        }
        if (isRace) emitProgress()
    }

    /**
     * Restarts the maze with a new deterministic [newSeed].
     */
    fun restart(newSeed: UInt) {
        val board = MathMazeGenerator.generate(newSeed, dimension)
        _state.value = createInitialState(board)
        startTimer()
    }

    /**
     * Releases timer and network coroutines.
     */
    fun dispose() {
        timerJob?.cancel()
        receiveJob?.cancel()
    }

    private fun recomputeTotal(board: MathMazeBoard, path: List<Int>): Int {
        var total = board.startValue
        for (i in 1 until path.size) {
            total = MathMazeGenerator.applyCell(total, board.cells[path[i]]) ?: total
        }
        return total
    }

    private fun createInitialState(board: MathMazeBoard): MathMazeGameState = MathMazeGameState(
        seed = board.seed,
        board = board,
        path = listOf(board.startIndex),
        runningTotal = board.startValue,
        hintedNextIndex = null,
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
}
