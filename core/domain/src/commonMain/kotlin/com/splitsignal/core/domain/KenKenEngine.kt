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
 * Grid size variants for KenKen / Calcudoku arithmetic cage puzzles.
 */
enum class KenKenSize(
    val dimension: Int,
    val label: String,
) {
    GRID_4X4(4, "4×4"),
    GRID_5X5(5, "5×5"),
}

/**
 * Arithmetic operation inside a KenKen cage.
 */
enum class KenKenOp(val symbol: String) {
    NONE(""),
    ADD("+"),
    SUB("−"),
    MUL("×"),
    DIV("÷"),
}

/**
 * Contiguous arithmetic cage inside a [KenKenBoard].
 */
data class KenKenCage(
    val id: Int,
    val cellIndices: List<Int>,
    val target: Int,
    val op: KenKenOp,
) {
    val label: String
        get() = if (op == KenKenOp.NONE) "$target" else "$target${op.symbol}"
}

/**
 * Deterministic KenKen / Calcudoku puzzle definition.
 */
data class KenKenBoard(
    val seed: UInt,
    val size: KenKenSize,
    val solution: List<Int>,
    val cages: List<KenKenCage>,
    val cellToCageId: List<Int>,
) {
    val dim: Int get() = size.dimension
    val cellCount: Int get() = dim * dim
}

/**
 * Live UI state for a solo, daily workout, or 1v1 KenKen / Calcudoku session.
 */
data class KenKenGameState(
    val seed: UInt,
    val size: KenKenSize,
    val board: KenKenBoard,
    val values: List<Int?>,
    val conflictCells: Set<Int>,
    val selectedCellIndex: Int?,
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
 * Deterministic generator and validator for [KenKenBoard] (4×4 & 5×5 Latin squares with irregular cages).
 */
object KenKenGenerator {
    /**
     * Generates a deterministic [KenKenBoard] from [seed] and [size].
     */
    fun generate(seed: UInt, size: KenKenSize): KenKenBoard {
        val rng = DeterministicRng(seed)
        val n = size.dimension
        val solution = buildLatinSquare(n, rng)

        // Partition grid into contiguous 1-, 2-, or 3-cell cages
        val assigned = BooleanArray(n * n)
        val cages = mutableListOf<KenKenCage>()
        val cellToCageId = IntArray(n * n)

        val order = (0 until n * n).toMutableList()
        rng.shuffle(order)

        for (start in order) {
            if (assigned[start]) continue
            val cageId = cages.size
            val desiredSize = when (rng.nextInt(10)) {
                0 -> 1
                in 1..6 -> 2
                else -> 3
            }
            val group = mutableListOf(start)
            assigned[start] = true
            cellToCageId[start] = cageId

            while (group.size < desiredSize) {
                val frontier = group.flatMap { idx ->
                    val r = idx / n
                    val c = idx % n
                    listOf(r - 1 to c, r + 1 to c, r to c - 1, r to c + 1)
                        .filter { (nr, nc) -> nr in 0 until n && nc in 0 until n }
                        .map { (nr, nc) -> nr * n + nc }
                        .filter { !assigned[it] }
                }.distinct()

                if (frontier.isEmpty()) break
                val next = frontier[rng.nextInt(frontier.size)]
                assigned[next] = true
                cellToCageId[next] = cageId
                group.add(next)
            }

            val sortedGroup = group.sorted()
            val cageVals = sortedGroup.map { solution[it] }
            val (op, target) = assignCageOp(cageVals, rng)
            cages.add(KenKenCage(id = cageId, cellIndices = sortedGroup, target = target, op = op))
        }

        return KenKenBoard(
            seed = seed,
            size = size,
            solution = solution,
            cages = cages,
            cellToCageId = cellToCageId.toList(),
        )
    }

    /**
     * Evaluates whether [cage] is satisfied by the candidate [values] (returns `false` if any cell is empty).
     */
    fun isCageSatisfied(cage: KenKenCage, values: List<Int?>): Boolean {
        val nums = cage.cellIndices.map { values[it] ?: return false }
        return when (cage.op) {
            KenKenOp.NONE -> nums.size == 1 && nums[0] == cage.target
            KenKenOp.ADD -> nums.sum() == cage.target
            KenKenOp.MUL -> nums.fold(1) { acc, v -> acc * v } == cage.target
            KenKenOp.SUB -> nums.size == 2 && abs(nums[0] - nums[1]) == cage.target
            KenKenOp.DIV -> {
                if (nums.size != 2) return false
                val hi = maxOf(nums[0], nums[1])
                val lo = minOf(nums[0], nums[1])
                lo != 0 && hi % lo == 0 && hi / lo == cage.target
            }
        }
    }

    /**
     * Finds row/column duplicate cells and fully-filled unsatisfied cage cells.
     */
    fun findConflicts(board: KenKenBoard, values: List<Int?>): Set<Int> {
        val n = board.dim
        val conflicts = mutableSetOf<Int>()

        for (r in 0 until n) {
            val seen = mutableMapOf<Int, MutableList<Int>>()
            for (c in 0 until n) {
                val idx = r * n + c
                val v = values[idx] ?: continue
                seen.getOrPut(v) { mutableListOf() }.add(idx)
            }
            seen.values.filter { it.size > 1 }.forEach { conflicts.addAll(it) }
        }

        for (c in 0 until n) {
            val seen = mutableMapOf<Int, MutableList<Int>>()
            for (r in 0 until n) {
                val idx = r * n + c
                val v = values[idx] ?: continue
                seen.getOrPut(v) { mutableListOf() }.add(idx)
            }
            seen.values.filter { it.size > 1 }.forEach { conflicts.addAll(it) }
        }

        for (cage in board.cages) {
            if (cage.cellIndices.all { values[it] != null } && !isCageSatisfied(cage, values)) {
                conflicts.addAll(cage.cellIndices)
            }
        }
        return conflicts
    }

    private fun buildLatinSquare(n: Int, rng: DeterministicRng): List<Int> {
        val digits = (1..n).toMutableList()
        rng.shuffle(digits)
        val rowPerm = (0 until n).toMutableList()
        val colPerm = (0 until n).toMutableList()
        rng.shuffle(rowPerm)
        rng.shuffle(colPerm)

        val grid = IntArray(n * n)
        for (r in 0 until n) {
            for (c in 0 until n) {
                val valIdx = (rowPerm[r] + colPerm[c]) % n
                grid[r * n + c] = digits[valIdx]
            }
        }
        return grid.toList()
    }

    private fun assignCageOp(vals: List<Int>, rng: DeterministicRng): Pair<KenKenOp, Int> {
        if (vals.size == 1) return KenKenOp.NONE to vals[0]
        if (vals.size == 2) {
            val a = vals[0]
            val b = vals[1]
            val hi = maxOf(a, b)
            val lo = minOf(a, b)
            val choices = mutableListOf(
                KenKenOp.ADD to (a + b),
                KenKenOp.MUL to (a * b),
                KenKenOp.SUB to (hi - lo),
            )
            if (lo != 0 && hi % lo == 0) {
                choices.add(KenKenOp.DIV to (hi / lo))
            }
            return choices[rng.nextInt(choices.size)]
        }
        return if (rng.nextInt(2) == 0) {
            KenKenOp.ADD to vals.sum()
        } else {
            KenKenOp.MUL to vals.fold(1) { acc, v -> acc * v }
        }
    }
}

/**
 * State engine for solo, daily workout, and 1v1 KenKen / Calcudoku races.
 */
class KenKenEngine(
    seed: UInt,
    size: KenKenSize,
    private val isRace: Boolean,
    private val isDailyStep: Boolean = false,
    private val roundIndex: Int = 0,
    private val transport: SyncTransport,
    private val scope: CoroutineScope,
) {
    private val _state: MutableStateFlow<KenKenGameState>
    val state: StateFlow<KenKenGameState>

    private var timerJob: Job? = null
    private var receiveJob: Job? = null

    init {
        val board = KenKenGenerator.generate(seed, size)
        _state = MutableStateFlow(createInitialState(board))
        state = _state.asStateFlow()
        startTimer()
        if (isRace) observeTransport()
    }

    /**
     * Selects cell [index] on the KenKen grid.
     */
    fun selectCell(index: Int) {
        val cur = _state.value
        if (cur.isSolved || index !in 0 until cur.board.cellCount) return
        _state.update { it.copy(selectedCellIndex = index) }
    }

    /**
     * Enters [digit] (`1..dim`) into the selected cell.
     */
    suspend fun enterDigit(digit: Int) {
        val cur = _state.value
        val idx = cur.selectedCellIndex ?: return
        if (cur.isSolved || digit !in 1..cur.board.dim) return

        val nextValues = cur.values.toMutableList()
        nextValues[idx] = digit
        val conflicts = KenKenGenerator.findConflicts(cur.board, nextValues)
        val isMistake = idx in conflicts || digit != cur.board.solution[idx]

        val filled = nextValues.count { it != null }
        val pct = ((filled * 100) / cur.board.cellCount).coerceIn(0, 100)
        val solved = filled == cur.board.cellCount &&
            conflicts.isEmpty() &&
            cur.board.cages.all { KenKenGenerator.isCageSatisfied(it, nextValues) }

        _state.update {
            it.copy(
                values = nextValues,
                conflictCells = conflicts,
                moves = it.moves + 1,
                mistakes = it.mistakes + if (isMistake) 1 else 0,
                percentComplete = pct,
                isSolved = solved,
            )
        }
        if (solved) timerJob?.cancel()
        if (isRace) emitProgress()
    }

    /**
     * Clears the currently selected cell.
     */
    suspend fun eraseSelected() {
        val cur = _state.value
        val idx = cur.selectedCellIndex ?: return
        if (cur.isSolved || cur.values[idx] == null) return

        val nextValues = cur.values.toMutableList()
        nextValues[idx] = null
        val conflicts = KenKenGenerator.findConflicts(cur.board, nextValues)
        val filled = nextValues.count { it != null }
        val pct = ((filled * 100) / cur.board.cellCount).coerceIn(0, 100)

        _state.update {
            it.copy(
                values = nextValues,
                conflictCells = conflicts,
                percentComplete = pct,
            )
        }
        if (isRace) emitProgress()
    }

    /**
     * Reveals one empty or incorrect cell using a Hint / Peek token.
     */
    suspend fun applyHint() {
        val cur = _state.value
        if (cur.isSolved) return
        val target = cur.selectedCellIndex?.takeIf { cur.values[it] != cur.board.solution[it] }
            ?: (0 until cur.board.cellCount).firstOrNull { cur.values[it] != cur.board.solution[it] }
            ?: return

        val nextValues = cur.values.toMutableList()
        nextValues[target] = cur.board.solution[target]
        val conflicts = KenKenGenerator.findConflicts(cur.board, nextValues)
        val filled = nextValues.count { it != null }
        val solved = filled == cur.board.cellCount &&
            conflicts.isEmpty() &&
            cur.board.cages.all { KenKenGenerator.isCageSatisfied(it, nextValues) }

        _state.update {
            it.copy(
                values = nextValues,
                conflictCells = conflicts,
                selectedCellIndex = target,
                moves = it.moves + 1,
                percentComplete = ((filled * 100) / cur.board.cellCount).coerceIn(0, 100),
                isSolved = solved,
            )
        }
        if (solved) timerJob?.cancel()
        if (isRace) emitProgress()
    }

    /**
     * Restarts with a fresh deterministic [newSeed].
     */
    fun restart(newSeed: UInt) {
        val board = KenKenGenerator.generate(newSeed, _state.value.size)
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

    private fun createInitialState(board: KenKenBoard): KenKenGameState = KenKenGameState(
        seed = board.seed,
        size = board.size,
        board = board,
        values = List(board.cellCount) { null },
        conflictCells = emptySet(),
        selectedCellIndex = 0,
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
