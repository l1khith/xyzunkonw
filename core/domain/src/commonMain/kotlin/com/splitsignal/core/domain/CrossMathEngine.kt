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
 * Grid size variants for Cross Math arithmetic crosswords.
 */
enum class CrossMathSize(
    val dimension: Int,
    val label: String,
    val blankCount: Int,
) {
    GRID_3X3(3, "3×3", 5),
    GRID_4X4(4, "4×4", 8),
}

/**
 * Arithmetic operator used in Cross Math equations, evaluated with BODMAS/PEMDAS precedence.
 */
enum class MathOp(
    val symbol: String,
    val precedence: Int,
) {
    ADD("+", 1),
    SUB("−", 1),
    MUL("×", 2),
    DIV("÷", 2),
}

/**
 * One entry in the Cross Math bottom Number Bank.
 */
data class BankTile(
    val id: Int,
    val value: Int,
    val usedInCellIndex: Int? = null,
)

/**
 * Immutable puzzle definition for a deterministic Cross Math board.
 */
data class CrossMathBoard(
    val seed: UInt,
    val size: CrossMathSize,
    val solution: List<Int>,
    val givenMask: List<Boolean>,
    val rowOps: List<List<MathOp>>,
    val colOps: List<List<MathOp>>,
    val rowTargets: List<Int>,
    val colTargets: List<Int>,
    val initialBankValues: List<Int>,
) {
    val dim: Int get() = size.dimension
    val cellCount: Int get() = dim * dim
}

/**
 * Live UI state for a solo or 1v1 Cross Math session.
 */
data class CrossMathGameState(
    val seed: UInt,
    val size: CrossMathSize,
    val board: CrossMathBoard,
    val values: List<Int?>,
    val bank: List<BankTile>,
    val selectedCellIndex: Int?,
    val rowSatisfied: List<Boolean>,
    val colSatisfied: List<Boolean>,
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
 * Deterministic generator and BODMAS/PEMDAS evaluator for [CrossMathBoard].
 */
object CrossMathGenerator {
    /**
     * Evaluates a sequence of integer [operands] and [ops] using BODMAS/PEMDAS (`×`/`÷` before `+`/`−`).
     * Returns `null` if any division is non-exact or divides by zero.
     */
    fun evaluateBodmas(operands: List<Int>, ops: List<MathOp>): Int? {
        if (operands.isEmpty() || ops.size != operands.size - 1) return null
        val nums = operands.toMutableList()
        val remOps = ops.toMutableList()

        // First pass: multiplication & division left-to-right
        var i = 0
        while (i < remOps.size) {
            when (remOps[i]) {
                MathOp.MUL -> {
                    nums[i] = nums[i] * nums[i + 1]
                    nums.removeAt(i + 1)
                    remOps.removeAt(i)
                }
                MathOp.DIV -> {
                    val divisor = nums[i + 1]
                    if (divisor == 0 || nums[i] % divisor != 0) return null
                    nums[i] = nums[i] / divisor
                    nums.removeAt(i + 1)
                    remOps.removeAt(i)
                }
                else -> i++
            }
        }

        // Second pass: addition & subtraction left-to-right
        var result = nums[0]
        for (j in remOps.indices) {
            result = when (remOps[j]) {
                MathOp.ADD -> result + nums[j + 1]
                MathOp.SUB -> result - nums[j + 1]
                else -> return null
            }
        }
        return result
    }

    /**
     * Generates a deterministic, guaranteed-solvable [CrossMathBoard] from [seed] and [size].
     */
    fun generate(seed: UInt, size: CrossMathSize): CrossMathBoard {
        val rng = DeterministicRng(seed)
        val n = size.dimension
        val cellCount = n * n

        // Populate grid with small clean integers (1..9 for 3x3, 1..12 for 4x4)
        val maxVal = if (size == CrossMathSize.GRID_3X3) 9 else 12
        val solution = List(cellCount) { rng.nextInt(1, maxVal + 1) }

        val rowOps = List(n) { r ->
            val rowVals = (0 until n).map { c -> solution[r * n + c] }
            pickValidOps(rowVals, rng)
        }
        val colOps = List(n) { c ->
            val colVals = (0 until n).map { r -> solution[r * n + c] }
            pickValidOps(colVals, rng)
        }

        val rowTargets = List(n) { r ->
            val rowVals = (0 until n).map { c -> solution[r * n + c] }
            evaluateBodmas(rowVals, rowOps[r]) ?: rowVals.sum()
        }
        val colTargets = List(n) { c ->
            val colVals = (0 until n).map { r -> solution[r * n + c] }
            evaluateBodmas(colVals, colOps[c]) ?: colVals.sum()
        }

        val indices = (0 until cellCount).toMutableList()
        rng.shuffle(indices)
        val blankSet = indices.take(size.blankCount).toSet()
        val givenMask = List(cellCount) { idx -> idx !in blankSet }

        val bankValues = blankSet.map { solution[it] }.toMutableList()
        rng.shuffle(bankValues)

        return CrossMathBoard(
            seed = seed,
            size = size,
            solution = solution,
            givenMask = givenMask,
            rowOps = rowOps,
            colOps = colOps,
            rowTargets = rowTargets,
            colTargets = colTargets,
            initialBankValues = bankValues,
        )
    }

    private fun pickValidOps(line: List<Int>, rng: DeterministicRng): List<MathOp> {
        val candidates = listOf(MathOp.ADD, MathOp.SUB, MathOp.MUL, MathOp.DIV)
        repeat(24) {
            val ops = List(line.size - 1) { candidates[rng.nextInt(candidates.size)] }
            val evaluated = evaluateBodmas(line, ops)
            if (evaluated != null && evaluated in -20..200) {
                return ops
            }
        }
        return List(line.size - 1) { index ->
            if (index % 2 == 0) MathOp.ADD else MathOp.MUL
        }
    }
}

/**
 * State engine for solo, daily workout, and 1v1 LAN Cross Math races.
 */
class CrossMathEngine(
    seed: UInt,
    size: CrossMathSize,
    private val isRace: Boolean,
    private val isDailyStep: Boolean = false,
    private val roundIndex: Int = 0,
    private val transport: SyncTransport,
    private val scope: CoroutineScope,
) {
    private val _state: MutableStateFlow<CrossMathGameState>
    val state: StateFlow<CrossMathGameState>

    private var timerJob: Job? = null
    private var receiveJob: Job? = null

    init {
        val initialBoard = CrossMathGenerator.generate(seed, size)
        val initialState = createInitialState(initialBoard)
        _state = MutableStateFlow(initialState)
        state = _state.asStateFlow()

        startTimer()
        if (isRace) {
            observeTransport()
        }
    }

    /**
     * Selects a blank cell [index] (`0 until cellCount`) for placement, or returns its placed bank tile if tapped again.
     */
    fun selectCell(index: Int) {
        val cur = _state.value
        if (cur.isSolved || index !in 0 until cur.board.cellCount) return
        if (cur.board.givenMask[index]) return

        // If cell already has a user-placed tile and is tapped while selected, return it to bank
        if (cur.selectedCellIndex == index && cur.values[index] != null) {
            scope.launch { clearCell(index) }
            return
        }
        _state.update { it.copy(selectedCellIndex = index) }
    }

    /**
     * Places the bank tile with [bankTileId] into the currently selected blank cell (or first empty blank cell).
     */
    suspend fun placeBankTile(bankTileId: Int) {
        val cur = _state.value
        if (cur.isSolved) return
        val tile = cur.bank.firstOrNull { it.id == bankTileId && it.usedInCellIndex == null } ?: return

        val targetCell = cur.selectedCellIndex?.takeIf { !cur.board.givenMask[it] }
            ?: (0 until cur.board.cellCount).firstOrNull { !cur.board.givenMask[it] && cur.values[it] == null }
            ?: return

        val nextValues = cur.values.toMutableList()
        val nextBank = cur.bank.map { existing ->
            when {
                existing.usedInCellIndex == targetCell -> existing.copy(usedInCellIndex = null)
                existing.id == bankTileId -> existing.copy(usedInCellIndex = targetCell)
                else -> existing
            }
        }
        nextValues[targetCell] = tile.value

        val isPlacementMistake = tile.value != cur.board.solution[targetCell]
        val nextMoves = cur.moves + 1
        val nextMistakes = cur.mistakes + if (isPlacementMistake) 1 else 0

        val rowSat = evaluateRows(cur.board, nextValues)
        val colSat = evaluateCols(cur.board, nextValues)
        val allFilled = nextValues.none { it == null }
        val solved = allFilled && rowSat.all { it } && colSat.all { it }
        val pct = computePercent(cur.board, nextValues)

        val nextSelected = if (solved) {
            null
        } else {
            (0 until cur.board.cellCount).firstOrNull { !cur.board.givenMask[it] && nextValues[it] == null }
        }

        _state.update {
            it.copy(
                values = nextValues,
                bank = nextBank,
                selectedCellIndex = nextSelected,
                rowSatisfied = rowSat,
                colSatisfied = colSat,
                moves = nextMoves,
                mistakes = nextMistakes,
                percentComplete = pct,
                isSolved = solved,
            )
        }
        if (solved) timerJob?.cancel()
        if (isRace) emitProgress()
    }

    /**
     * Returns any bank tile currently placed at [cellIndex] back to the Number Bank.
     */
    suspend fun clearCell(cellIndex: Int) {
        val cur = _state.value
        if (cur.isSolved || cellIndex !in 0 until cur.board.cellCount) return
        if (cur.board.givenMask[cellIndex] || cur.values[cellIndex] == null) return

        val nextValues = cur.values.toMutableList()
        nextValues[cellIndex] = null
        val nextBank = cur.bank.map { tile ->
            if (tile.usedInCellIndex == cellIndex) tile.copy(usedInCellIndex = null) else tile
        }

        val rowSat = evaluateRows(cur.board, nextValues)
        val colSat = evaluateCols(cur.board, nextValues)

        _state.update {
            it.copy(
                values = nextValues,
                bank = nextBank,
                selectedCellIndex = cellIndex,
                rowSatisfied = rowSat,
                colSatisfied = colSat,
                percentComplete = computePercent(cur.board, nextValues),
            )
        }
        if (isRace) emitProgress()
    }

    /**
     * Applies a Hint / Peek token by placing the exact solution value into an empty or incorrect blank cell.
     */
    suspend fun applyHint() {
        val cur = _state.value
        if (cur.isSolved) return
        val targetCell = cur.selectedCellIndex?.takeIf {
            !cur.board.givenMask[it] && cur.values[it] != cur.board.solution[it]
        } ?: (0 until cur.board.cellCount).firstOrNull {
            !cur.board.givenMask[it] && cur.values[it] != cur.board.solution[it]
        } ?: return

        val neededValue = cur.board.solution[targetCell]
        val matchingBank = cur.bank.firstOrNull { it.value == neededValue && it.usedInCellIndex == null }
            ?: cur.bank.firstOrNull { it.value == neededValue }
            ?: return

        val nextValues = cur.values.toMutableList()
        // If that bank tile was placed somewhere wrong, pull it out
        val oldCell = matchingBank.usedInCellIndex
        if (oldCell != null) nextValues[oldCell] = null

        val nextBank = cur.bank.map { tile ->
            when {
                tile.id == matchingBank.id -> tile.copy(usedInCellIndex = targetCell)
                tile.usedInCellIndex == targetCell -> tile.copy(usedInCellIndex = null)
                else -> tile
            }
        }
        nextValues[targetCell] = neededValue

        val rowSat = evaluateRows(cur.board, nextValues)
        val colSat = evaluateCols(cur.board, nextValues)
        val solved = nextValues.none { it == null } && rowSat.all { it } && colSat.all { it }

        _state.update {
            it.copy(
                values = nextValues,
                bank = nextBank,
                selectedCellIndex = if (solved) null else targetCell,
                rowSatisfied = rowSat,
                colSatisfied = colSat,
                moves = it.moves + 1,
                percentComplete = computePercent(cur.board, nextValues),
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
        val freshBoard = CrossMathGenerator.generate(newSeed, _state.value.size)
        _state.value = createInitialState(freshBoard)
        startTimer()
    }

    /**
     * Releases timer and network collector coroutines.
     */
    fun dispose() {
        timerJob?.cancel()
        receiveJob?.cancel()
    }

    private fun createInitialState(board: CrossMathBoard): CrossMathGameState {
        val values = List(board.cellCount) { idx ->
            if (board.givenMask[idx]) board.solution[idx] else null
        }
        val bank = board.initialBankValues.mapIndexed { idx, value ->
            BankTile(id = idx, value = value)
        }
        val firstBlank = (0 until board.cellCount).firstOrNull { !board.givenMask[it] }
        return CrossMathGameState(
            seed = board.seed,
            size = board.size,
            board = board,
            values = values,
            bank = bank,
            selectedCellIndex = firstBlank,
            rowSatisfied = evaluateRows(board, values),
            colSatisfied = evaluateCols(board, values),
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
    }

    private fun evaluateRows(board: CrossMathBoard, values: List<Int?>): List<Boolean> {
        val n = board.dim
        return List(n) { r ->
            val rowVals = (0 until n).map { c -> values[r * n + c] ?: return@List false }
            CrossMathGenerator.evaluateBodmas(rowVals, board.rowOps[r]) == board.rowTargets[r]
        }
    }

    private fun evaluateCols(board: CrossMathBoard, values: List<Int?>): List<Boolean> {
        val n = board.dim
        return List(n) { c ->
            val colVals = (0 until n).map { r -> values[r * n + c] ?: return@List false }
            CrossMathGenerator.evaluateBodmas(colVals, board.colOps[c]) == board.colTargets[c]
        }
    }

    private fun computePercent(board: CrossMathBoard, values: List<Int?>): Int {
        val totalBlanks = board.givenMask.count { !it }.coerceAtLeast(1)
        val filledBlanks = (0 until board.cellCount).count { !board.givenMask[it] && values[it] != null }
        return ((filledBlanks * 100) / totalBlanks).coerceIn(0, 100)
    }

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
