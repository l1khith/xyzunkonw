package com.splitsignal.core.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * Supported Sudoku board dimensions and sub-box geometries.
 */
@Serializable
enum class SudokuSize(
    val dim: Int,
    val boxRows: Int,
    val boxCols: Int,
    val label: String,
) {
    SPRINT_6X6(6, 2, 3, "6×6 Sprint"),
    CLASSIC_9X9(9, 3, 3, "9×9 Classic"),
}

/**
 * Difficulty tiers controlling target clue counts and par completion times.
 */
@Serializable
enum class SudokuDifficulty(
    val label: String,
    val givens6: Int,
    val givens9: Int,
    val parSeconds6: Int,
    val parSeconds9: Int,
) {
    EASY("Easy", givens6 = 20, givens9 = 42, parSeconds6 = 90, parSeconds9 = 240),
    MEDIUM("Medium", givens6 = 16, givens9 = 34, parSeconds6 = 120, parSeconds9 = 360),
    HARD("Hard", givens6 = 12, givens9 = 28, parSeconds6 = 160, parSeconds9 = 480),
    MASTER("Master", givens6 = 10, givens9 = 24, parSeconds6 = 210, parSeconds9 = 600),
}

/**
 * Plain-English logical deduction hint pointing to a single cell and digit.
 */
data class SudokuHint(
    val cellIndex: Int,
    val digit: Int,
    val technique: String,
    val explanation: String,
)

/**
 * Generated Sudoku puzzle with its unique solution and logical complexity score.
 */
data class SudokuBoard(
    val size: SudokuSize,
    val difficulty: SudokuDifficulty,
    val seed: UInt,
    val givens: List<Int>,
    val solution: List<Int>,
    val logicScore: Int,
)

/**
 * Deterministic Sudoku generator, uniqueness verifier, and logical hint finder.
 */
object SudokuGenerator {

    /**
     * Deterministically generates a [SudokuBoard] from [size], [difficulty], and [seed]
     * whose [SudokuBoard.givens] has strictly 1 unique solution.
     */
    fun generate(
        size: SudokuSize,
        difficulty: SudokuDifficulty,
        seed: UInt,
    ): SudokuBoard {
        val rng = DeterministicRng(seed)
        val dim = size.dim
        val total = dim * dim
        val fullGrid = IntArray(total)
        val rowMask = IntArray(dim)
        val colMask = IntArray(dim)
        val boxMask = IntArray(dim)

        val filled = fillFullBoard(
            size = size,
            pos = 0,
            grid = fullGrid,
            rowMask = rowMask,
            colMask = colMask,
            boxMask = boxMask,
            rng = rng,
        )
        check(filled) { "Failed to generate a valid Sudoku solution for $size and seed $seed" }

        val solution = fullGrid.toList()
        val puzzle = fullGrid.copyOf()
        val targetGivens = if (size == SudokuSize.SPRINT_6X6) difficulty.givens6 else difficulty.givens9

        val pairs = buildSymmetricPairs(total)
        shufflePairs(pairs, rng)

        var remainingGivens = total
        for ((a, b) in pairs) {
            if (remainingGivens <= targetGivens) break
            val removalCount = if (a == b) 1 else 2
            if (remainingGivens - removalCount < targetGivens && remainingGivens - removalCount != targetGivens - 1) {
                continue
            }
            if (remainingGivens == targetGivens + 1 && a != b && size == SudokuSize.CLASSIC_9X9) {
                continue
            }

            val savedA = puzzle[a]
            val savedB = puzzle[b]
            puzzle[a] = 0
            puzzle[b] = 0

            if (countSolutions(size, puzzle, limit = 2) == 1) {
                remainingGivens -= removalCount
            } else {
                puzzle[a] = savedA
                puzzle[b] = savedB
            }
        }

        val givensList = puzzle.toList()
        val logicScore = computeLogicScore(size, givensList, solution)
        return SudokuBoard(
            size = size,
            difficulty = difficulty,
            seed = seed,
            givens = givensList,
            solution = solution,
            logicScore = logicScore,
        )
    }

    /**
     * Returns `true` if [grid] has strictly 1 valid Sudoku solution under [size] rules.
     */
    fun hasUniqueSolution(size: SudokuSize, grid: List<Int>): Boolean {
        val total = size.dim * size.dim
        if (grid.size != total) return false
        return countSolutions(size, grid.toIntArray(), limit = 2) == 1
    }

    /**
     * Returns `true` if [grid] is completely filled and satisfies all row, column, and box constraints.
     */
    fun isValidSolvedGrid(size: SudokuSize, grid: List<Int>): Boolean {
        val dim = size.dim
        if (grid.size != dim * dim) return false
        val fullMask = ((1 shl dim) - 1) shl 1
        val rowMask = IntArray(dim)
        val colMask = IntArray(dim)
        val boxMask = IntArray(dim)
        for (idx in grid.indices) {
            val digit = grid[idx]
            if (digit !in 1..dim) return false
            val r = idx / dim
            val c = idx % dim
            val b = boxIndex(size, r, c)
            val bit = 1 shl digit
            if ((rowMask[r] and bit) != 0 || (colMask[c] and bit) != 0 || (boxMask[b] and bit) != 0) {
                return false
            }
            rowMask[r] = rowMask[r] or bit
            colMask[c] = colMask[c] or bit
            boxMask[b] = boxMask[b] or bit
        }
        return rowMask.all { it == fullMask } &&
            colMask.all { it == fullMask } &&
            boxMask.all { it == fullMask }
    }

    /**
     * Finds a Naked Single or Hidden Single deduction on [grid] and explains it in plain English,
     * falling back to a guided deduction against [solution] if no single exists.
     */
    fun findLogicalHint(
        size: SudokuSize,
        grid: List<Int>,
        solution: List<Int>,
    ): SudokuHint? {
        val dim = size.dim
        val total = dim * dim
        if (grid.size != total || solution.size != total) return null

        val masks = buildConstraintMasks(size, grid) ?: return null
        val allDigitsMask = ((1 shl dim) - 1) shl 1

        // 1. Naked Single: an empty cell with exactly 1 valid candidate.
        for (idx in 0 until total) {
            if (grid[idx] != 0) continue
            val r = idx / dim
            val c = idx % dim
            val b = boxIndex(size, r, c)
            val used = masks.rowMask[r] or masks.colMask[c] or masks.boxMask[b]
            val candidates = allDigitsMask and used.inv()
            if (candidates.countOneBits() == 1) {
                val digit = candidates.countTrailingZeroBits()
                if (digit == solution[idx]) {
                    val excludedDigits = (1..dim)
                        .filter { it != digit }
                        .joinToString(", ")
                    return SudokuHint(
                        cellIndex = idx,
                        digit = digit,
                        technique = "Naked Single",
                        explanation = "Row ${r + 1}, Col ${c + 1} can only be $digit because $excludedDigits already appear in its row, column, or box.",
                    )
                }
            }
        }

        // 2. Hidden Single in Rows.
        for (r in 0 until dim) {
            for (digit in 1..dim) {
                val bit = 1 shl digit
                if ((masks.rowMask[r] and bit) != 0) continue
                var matchCell = -1
                var matchCount = 0
                for (c in 0 until dim) {
                    val idx = r * dim + c
                    if (grid[idx] != 0) continue
                    val b = boxIndex(size, r, c)
                    val used = masks.rowMask[r] or masks.colMask[c] or masks.boxMask[b]
                    if ((used and bit) == 0) {
                        matchCell = idx
                        matchCount++
                        if (matchCount > 1) break
                    }
                }
                if (matchCount == 1 && matchCell >= 0 && solution[matchCell] == digit) {
                    val c = matchCell % dim
                    return SudokuHint(
                        cellIndex = matchCell,
                        digit = digit,
                        technique = "Hidden Single (Row)",
                        explanation = "In Row ${r + 1}, digit $digit can only go in Row ${r + 1}, Col ${c + 1} because all other empty cells in this row are blocked by columns or boxes.",
                    )
                }
            }
        }

        // 3. Hidden Single in Columns.
        for (c in 0 until dim) {
            for (digit in 1..dim) {
                val bit = 1 shl digit
                if ((masks.colMask[c] and bit) != 0) continue
                var matchCell = -1
                var matchCount = 0
                for (r in 0 until dim) {
                    val idx = r * dim + c
                    if (grid[idx] != 0) continue
                    val b = boxIndex(size, r, c)
                    val used = masks.rowMask[r] or masks.colMask[c] or masks.boxMask[b]
                    if ((used and bit) == 0) {
                        matchCell = idx
                        matchCount++
                        if (matchCount > 1) break
                    }
                }
                if (matchCount == 1 && matchCell >= 0 && solution[matchCell] == digit) {
                    val r = matchCell / dim
                    return SudokuHint(
                        cellIndex = matchCell,
                        digit = digit,
                        technique = "Hidden Single (Column)",
                        explanation = "In Column ${c + 1}, digit $digit can only go in Row ${r + 1}, Col ${c + 1} because all other empty cells in this column are blocked by rows or boxes.",
                    )
                }
            }
        }

        // 4. Hidden Single in Boxes.
        val boxesPerRow = dim / size.boxCols
        for (b in 0 until dim) {
            val startRow = (b / boxesPerRow) * size.boxRows
            val startCol = (b % boxesPerRow) * size.boxCols
            for (digit in 1..dim) {
                val bit = 1 shl digit
                if ((masks.boxMask[b] and bit) != 0) continue
                var matchCell = -1
                var matchCount = 0
                for (dr in 0 until size.boxRows) {
                    for (dc in 0 until size.boxCols) {
                        val r = startRow + dr
                        val c = startCol + dc
                        val idx = r * dim + c
                        if (grid[idx] != 0) continue
                        val used = masks.rowMask[r] or masks.colMask[c] or masks.boxMask[b]
                        if ((used and bit) == 0) {
                            matchCell = idx
                            matchCount++
                            if (matchCount > 1) break
                        }
                    }
                    if (matchCount > 1) break
                }
                if (matchCount == 1 && matchCell >= 0 && solution[matchCell] == digit) {
                    val r = matchCell / dim
                    val c = matchCell % dim
                    return SudokuHint(
                        cellIndex = matchCell,
                        digit = digit,
                        technique = "Hidden Single (Box)",
                        explanation = "In Box ${b + 1}, digit $digit can only go in Row ${r + 1}, Col ${c + 1} because every other open cell in that box conflicts with $digit.",
                    )
                }
            }
        }

        // 5. Fallback for advanced boards requiring multi-cell techniques.
        var bestCell = -1
        var fewestCandidates = dim + 1
        for (idx in 0 until total) {
            if (grid[idx] != 0) continue
            val r = idx / dim
            val c = idx % dim
            val b = boxIndex(size, r, c)
            val used = masks.rowMask[r] or masks.colMask[c] or masks.boxMask[b]
            val candidateCount = (allDigitsMask and used.inv()).countOneBits()
            if (candidateCount in 1 until fewestCandidates) {
                fewestCandidates = candidateCount
                bestCell = idx
            }
        }
        if (bestCell < 0) return null
        val r = bestCell / dim
        val c = bestCell % dim
        val digit = solution[bestCell]
        return SudokuHint(
            cellIndex = bestCell,
            digit = digit,
            technique = "Strategic Deduction",
            explanation = "Row ${r + 1}, Col ${c + 1} is constrained to $fewestCandidates candidates; only $digit leads to the unique global solution.",
        )
    }

    private fun fillFullBoard(
        size: SudokuSize,
        pos: Int,
        grid: IntArray,
        rowMask: IntArray,
        colMask: IntArray,
        boxMask: IntArray,
        rng: DeterministicRng,
    ): Boolean {
        val dim = size.dim
        val total = dim * dim
        if (pos == total) return true

        val r = pos / dim
        val c = pos % dim
        val b = boxIndex(size, r, c)
        val allDigitsMask = ((1 shl dim) - 1) shl 1
        val used = rowMask[r] or colMask[c] or boxMask[b]
        val availableMask = allDigitsMask and used.inv()
        if (availableMask == 0) return false

        val candidates = IntArray(availableMask.countOneBits())
        var count = 0
        for (d in 1..dim) {
            if ((availableMask and (1 shl d)) != 0) {
                candidates[count++] = d
            }
        }
        for (i in count - 1 downTo 1) {
            val j = rng.nextInt(i + 1)
            val tmp = candidates[i]
            candidates[i] = candidates[j]
            candidates[j] = tmp
        }

        for (i in 0 until count) {
            val digit = candidates[i]
            val bit = 1 shl digit
            grid[pos] = digit
            rowMask[r] = rowMask[r] or bit
            colMask[c] = colMask[c] or bit
            boxMask[b] = boxMask[b] or bit

            if (fillFullBoard(size, pos + 1, grid, rowMask, colMask, boxMask, rng)) {
                return true
            }

            grid[pos] = 0
            rowMask[r] = rowMask[r] xor bit
            colMask[c] = colMask[c] xor bit
            boxMask[b] = boxMask[b] xor bit
        }
        return false
    }

    private fun countSolutions(size: SudokuSize, grid: IntArray, limit: Int): Int {
        val dim = size.dim
        val total = dim * dim
        val rowMask = IntArray(dim)
        val colMask = IntArray(dim)
        val boxMask = IntArray(dim)

        for (idx in 0 until total) {
            val digit = grid[idx]
            if (digit == 0) continue
            if (digit !in 1..dim) return 0
            val r = idx / dim
            val c = idx % dim
            val b = boxIndex(size, r, c)
            val bit = 1 shl digit
            if ((rowMask[r] and bit) != 0 || (colMask[c] and bit) != 0 || (boxMask[b] and bit) != 0) {
                return 0
            }
            rowMask[r] = rowMask[r] or bit
            colMask[c] = colMask[c] or bit
            boxMask[b] = boxMask[b] or bit
        }

        return solveCountMrv(
            size = size,
            grid = grid,
            rowMask = rowMask,
            colMask = colMask,
            boxMask = boxMask,
            limit = limit,
        )
    }

    private fun solveCountMrv(
        size: SudokuSize,
        grid: IntArray,
        rowMask: IntArray,
        colMask: IntArray,
        boxMask: IntArray,
        limit: Int,
    ): Int {
        val dim = size.dim
        val total = dim * dim
        val allDigitsMask = ((1 shl dim) - 1) shl 1

        var bestCell = -1
        var bestMask = 0
        var bestCount = dim + 1

        for (idx in 0 until total) {
            if (grid[idx] != 0) continue
            val r = idx / dim
            val c = idx % dim
            val b = boxIndex(size, r, c)
            val candidates = allDigitsMask and (rowMask[r] or colMask[c] or boxMask[b]).inv()
            val count = candidates.countOneBits()
            if (count == 0) return 0
            if (count < bestCount) {
                bestCount = count
                bestCell = idx
                bestMask = candidates
                if (count == 1) break
            }
        }

        if (bestCell == -1) return 1

        val r = bestCell / dim
        val c = bestCell % dim
        val b = boxIndex(size, r, c)

        var solutions = 0
        var mask = bestMask
        while (mask != 0) {
            val bit = mask and -mask
            mask = mask xor bit
            val digit = bit.countTrailingZeroBits()

            grid[bestCell] = digit
            rowMask[r] = rowMask[r] or bit
            colMask[c] = colMask[c] or bit
            boxMask[b] = boxMask[b] or bit

            solutions += solveCountMrv(size, grid, rowMask, colMask, boxMask, limit - solutions)

            grid[bestCell] = 0
            rowMask[r] = rowMask[r] xor bit
            colMask[c] = colMask[c] xor bit
            boxMask[b] = boxMask[b] xor bit

            if (solutions >= limit) return solutions
        }
        return solutions
    }

    private fun computeLogicScore(
        size: SudokuSize,
        givens: List<Int>,
        solution: List<Int>,
    ): Int {
        val simulated = givens.toMutableList()
        var score = givens.count { it == 0 } * 5
        while (true) {
            val hint = findLogicalHint(size, simulated, solution) ?: break
            score += when (hint.technique) {
                "Naked Single" -> 4
                "Strategic Deduction" -> 35
                else -> 12
            }
            simulated[hint.cellIndex] = hint.digit
        }
        return score
    }

    private fun buildConstraintMasks(size: SudokuSize, grid: List<Int>): ConstraintMasks? {
        val dim = size.dim
        val rowMask = IntArray(dim)
        val colMask = IntArray(dim)
        val boxMask = IntArray(dim)
        for (idx in grid.indices) {
            val digit = grid[idx]
            if (digit == 0) continue
            if (digit !in 1..dim) return null
            val r = idx / dim
            val c = idx % dim
            val b = boxIndex(size, r, c)
            val bit = 1 shl digit
            if ((rowMask[r] and bit) != 0 || (colMask[c] and bit) != 0 || (boxMask[b] and bit) != 0) {
                return null
            }
            rowMask[r] = rowMask[r] or bit
            colMask[c] = colMask[c] or bit
            boxMask[b] = boxMask[b] or bit
        }
        return ConstraintMasks(rowMask, colMask, boxMask)
    }

    private fun buildSymmetricPairs(total: Int): MutableList<Pair<Int, Int>> {
        val half = (total + 1) / 2
        return MutableList(half) { i -> i to (total - 1 - i) }
    }

    private fun shufflePairs(pairs: MutableList<Pair<Int, Int>>, rng: DeterministicRng) {
        for (i in pairs.size - 1 downTo 1) {
            val j = rng.nextInt(i + 1)
            val tmp = pairs[i]
            pairs[i] = pairs[j]
            pairs[j] = tmp
        }
    }

    internal fun boxIndex(size: SudokuSize, row: Int, col: Int): Int {
        val boxesPerRow = size.dim / size.boxCols
        return (row / size.boxRows) * boxesPerRow + (col / size.boxCols)
    }

    private class ConstraintMasks(
        val rowMask: IntArray,
        val colMask: IntArray,
        val boxMask: IntArray,
    )
}

/**
 * Live telemetry of the opponent during a 2-device LAN Sudoku Sprint Race.
 */
data class OpponentProgress(
    val percent: Int = 0,
    val mistakes: Int = 0,
    val elapsedSeconds: Int = 0,
    val finished: Boolean = false,
    val connected: Boolean = false,
)

/**
 * Immutable UI/engine state snapshot for solo and race Sudoku sessions.
 */
data class SudokuGameState(
    val size: SudokuSize,
    val difficulty: SudokuDifficulty,
    val seed: UInt,
    val givens: List<Int>,
    val grid: List<Int>,
    val solution: List<Int>,
    val selectedCell: Int? = null,
    val mistakes: Int = 0,
    val elapsedSeconds: Int = 0,
    val lockoutSecondsRemaining: Int = 0,
    val sabotageBoxIndex: Int? = null,
    val sabotageSecondsRemaining: Int = 0,
    val activeHint: SudokuHint? = null,
    val isSolved: Boolean = false,
    val isRace: Boolean = false,
    val isDailySprint: Boolean = false,
    val opponent: OpponentProgress = OpponentProgress(),
    val statusMessage: String = "",
)

/**
 * Coordinates solo, daily sprint, and 2-device LAN race Sudoku gameplay.
 */
class SudokuRaceEngine(
    seed: UInt,
    size: SudokuSize,
    difficulty: SudokuDifficulty,
    private val isRace: Boolean,
    private val isDailySprint: Boolean,
    private val transport: SyncTransport,
    private val scope: CoroutineScope,
) {
    private var sabotageRng: DeterministicRng = DeterministicRng(seed xor SABOTAGE_SEED_MASK)
    private val creditedCells: MutableSet<Int> = mutableSetOf()

    private val _state: MutableStateFlow<SudokuGameState> = MutableStateFlow(
        buildInitialState(seed = seed, size = size, difficulty = difficulty, opponentConnected = false),
    )

    /**
     * Observable state stream for the active Sudoku session.
     */
    val state: StateFlow<SudokuGameState> = _state.asStateFlow()

    private val collectorJob: Job = scope.launch {
        transport.incomingPackets.collect { packet ->
            handleIncomingPacket(packet)
        }
    }

    private val tickJob: Job = scope.launch {
        while (currentCoroutineContext().isActive) {
            delay(TICK_INTERVAL_MS)
            _state.update { current ->
                if (current.isSolved) {
                    current
                } else {
                    val nextLockout = (current.lockoutSecondsRemaining - 1).coerceAtLeast(0)
                    val nextSabotage = (current.sabotageSecondsRemaining - 1).coerceAtLeast(0)
                    current.copy(
                        elapsedSeconds = current.elapsedSeconds + 1,
                        lockoutSecondsRemaining = nextLockout,
                        sabotageSecondsRemaining = nextSabotage,
                        sabotageBoxIndex = if (nextSabotage == 0) null else current.sabotageBoxIndex,
                    )
                }
            }
        }
    }

    /**
     * Selects cell [index] on the active Sudoku grid.
     */
    fun selectCell(index: Int) {
        val current = _state.value
        if (index !in current.grid.indices) return
        _state.update { it.copy(selectedCell = index) }
    }

    /**
     * Enters [digit] into the currently selected editable cell, validating against the unique solution.
     */
    suspend fun enterDigit(digit: Int) {
        val current = _state.value
        if (current.isSolved) return
        if (current.lockoutSecondsRemaining > 0) {
            _state.update {
                it.copy(statusMessage = "Locked out for ${it.lockoutSecondsRemaining}s after mistake.")
            }
            return
        }
        val idx = current.selectedCell ?: return
        if (idx !in current.grid.indices) return
        if (current.givens[idx] != 0) return
        if (digit !in 1..current.size.dim) return
        if (current.grid[idx] == current.solution[idx]) return

        if (digit != current.solution[idx]) {
            val nextMistakes = current.mistakes + 1
            val percent = computeCompletionPercent(current.givens, current.grid, current.solution)
            _state.update {
                it.copy(
                    mistakes = nextMistakes,
                    lockoutSecondsRemaining = MISTAKE_LOCKOUT_SECONDS,
                    statusMessage = "Wrong digit! Locked out for ${MISTAKE_LOCKOUT_SECONDS}s.",
                )
            }
            transport.send(
                SyncPacket.SudokuProgress(
                    percent = percent,
                    mistakes = nextMistakes,
                    elapsedSeconds = current.elapsedSeconds,
                    finished = false,
                ),
            )
            return
        }

        val updatedGrid = current.grid.toMutableList().apply {
            this[idx] = digit
        }.toList()
        val solved = updatedGrid == current.solution
        val percent = computeCompletionPercent(current.givens, updatedGrid, current.solution)
        val clearedHint = if (current.activeHint?.cellIndex == idx) null else current.activeHint
        val newlyCredited = creditedCells.add(idx)

        val row = idx / current.size.dim + 1
        val col = idx % current.size.dim + 1
        _state.update {
            it.copy(
                grid = updatedGrid,
                activeHint = clearedHint,
                isSolved = solved,
                statusMessage = if (solved) {
                    "Puzzle solved in ${it.elapsedSeconds}s!"
                } else {
                    "Placed $digit at R${row}C${col}."
                },
            )
        }

        transport.send(
            SyncPacket.SudokuProgress(
                percent = percent,
                mistakes = current.mistakes,
                elapsedSeconds = current.elapsedSeconds,
                finished = solved,
            ),
        )

        if (isRace && !solved && newlyCredited && creditedCells.size % SABOTAGE_CELL_CADENCE == 0) {
            val targetBox = sabotageRng.nextInt(current.size.dim)
            transport.send(SyncPacket.SudokuSabotage(targetBoxIndex = targetBox))
        }
    }

    /**
     * Clears the currently selected cell if it is not a given and the puzzle is not yet solved.
     */
    suspend fun eraseSelected() {
        val current = _state.value
        if (current.isSolved) return
        val idx = current.selectedCell ?: return
        if (idx !in current.grid.indices) return
        if (current.givens[idx] != 0 || current.grid[idx] == 0) return

        val updatedGrid = current.grid.toMutableList().apply {
            this[idx] = 0
        }.toList()
        val percent = computeCompletionPercent(current.givens, updatedGrid, current.solution)

        _state.update {
            it.copy(
                grid = updatedGrid,
                statusMessage = "Cell cleared.",
            )
        }

        if (isRace) {
            transport.send(
                SyncPacket.SudokuProgress(
                    percent = percent,
                    mistakes = current.mistakes,
                    elapsedSeconds = current.elapsedSeconds,
                    finished = false,
                ),
            )
        }
    }

    /**
     * Computes a plain-English logical hint for the current board and highlights its target cell.
     */
    fun applyHint(): SudokuHint? {
        val current = _state.value
        if (current.isSolved) return null
        val hint = SudokuGenerator.findLogicalHint(current.size, current.grid, current.solution)
            ?: return null
        _state.update {
            it.copy(
                selectedCell = hint.cellIndex,
                activeHint = hint,
                statusMessage = "${hint.technique}: ${hint.explanation}",
            )
        }
        return hint
    }

    /**
     * Generates a fresh puzzle with [newSeed] and resets timers, mistakes, and sabotage state.
     */
    fun restart(newSeed: UInt) {
        val current = _state.value
        resetBoard(seed = newSeed, size = current.size, difficulty = current.difficulty)
        if (isRace) {
            scope.launch {
                transport.send(
                    SyncPacket.SudokuStart(
                        seed = newSeed,
                        size = current.size,
                        difficulty = current.difficulty,
                    ),
                )
            }
        }
    }

    /**
     * Cancels active timer and packet-collector coroutines.
     */
    fun dispose() {
        tickJob.cancel()
        collectorJob.cancel()
    }

    private fun handleIncomingPacket(packet: SyncPacket) {
        when (packet) {
            is SyncPacket.Handshake -> {
                _state.update { current ->
                    current.copy(
                        opponent = current.opponent.copy(connected = true),
                        statusMessage = if (isRace) "Opponent connected! Sprint ready." else current.statusMessage,
                    )
                }
            }

            is SyncPacket.SudokuStart -> {
                val current = _state.value
                if (packet.seed != current.seed || packet.size != current.size || packet.difficulty != current.difficulty) {
                    resetBoard(seed = packet.seed, size = packet.size, difficulty = packet.difficulty)
                }
            }

            is SyncPacket.SudokuProgress -> {
                _state.update { current ->
                    val updatedOpponent = OpponentProgress(
                        percent = packet.percent.coerceIn(0, 100),
                        mistakes = packet.mistakes,
                        elapsedSeconds = packet.elapsedSeconds,
                        finished = packet.finished,
                        connected = true,
                    )
                    current.copy(
                        opponent = updatedOpponent,
                        statusMessage = if (packet.finished && !current.isSolved) {
                            "Opponent finished in ${packet.elapsedSeconds}s! Keep going!"
                        } else {
                            current.statusMessage
                        },
                    )
                }
            }

            is SyncPacket.SudokuSabotage -> {
                _state.update { current ->
                    if (current.isSolved) return@update current
                    val clampedBox = packet.targetBoxIndex.coerceIn(0, current.size.dim - 1)
                    current.copy(
                        sabotageBoxIndex = clampedBox,
                        sabotageSecondsRemaining = SABOTAGE_DURATION_SECONDS,
                        statusMessage = "Ink sabotage! Box ${clampedBox + 1} blurred for ${SABOTAGE_DURATION_SECONDS}s!",
                    )
                }
            }

            is SyncPacket.DialUpdate,
            is SyncPacket.SwitchToggle,
            is SyncPacket.StateSync,
            is SyncPacket.AdvanceStage,
            is SyncPacket.RestartRun,
            is SyncPacket.TileIncremented,
            is SyncPacket.AdvanceLevel,
            is SyncPacket.BattleshipShot,
            is SyncPacket.BattleshipSonar,
            is SyncPacket.BattleshipRestart,
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

    private fun resetBoard(seed: UInt, size: SudokuSize, difficulty: SudokuDifficulty) {
        sabotageRng = DeterministicRng(seed xor SABOTAGE_SEED_MASK)
        creditedCells.clear()
        val opponentConnected = _state.value.opponent.connected
        _state.value = buildInitialState(
            seed = seed,
            size = size,
            difficulty = difficulty,
            opponentConnected = opponentConnected,
        )
    }

    private fun buildInitialState(
        seed: UInt,
        size: SudokuSize,
        difficulty: SudokuDifficulty,
        opponentConnected: Boolean,
    ): SudokuGameState {
        val board = SudokuGenerator.generate(size = size, difficulty = difficulty, seed = seed)
        val initialMessage = when {
            isRace -> "Race Sprint: first to solve wins! Every 15 cells sabotages opponent."
            isDailySprint -> "Daily Sprint (${size.label}): beat par for bonus streak XP!"
            else -> "${size.label} • ${difficulty.label} ready."
        }
        return SudokuGameState(
            size = size,
            difficulty = difficulty,
            seed = seed,
            givens = board.givens,
            grid = board.givens.toList(),
            solution = board.solution,
            isRace = isRace,
            isDailySprint = isDailySprint,
            opponent = OpponentProgress(connected = opponentConnected),
            statusMessage = initialMessage,
        )
    }

    private fun computeCompletionPercent(
        givens: List<Int>,
        grid: List<Int>,
        solution: List<Int>,
    ): Int {
        var totalToFill = 0
        var filledCorrect = 0
        for (i in givens.indices) {
            if (givens[i] == 0) {
                totalToFill++
                if (grid[i] == solution[i]) {
                    filledCorrect++
                }
            }
        }
        return if (totalToFill == 0) 100 else (filledCorrect * 100) / totalToFill
    }

    companion object {
        const val MISTAKE_LOCKOUT_SECONDS = 3
        const val SABOTAGE_DURATION_SECONDS = 4
        const val SABOTAGE_CELL_CADENCE = 15
        private const val TICK_INTERVAL_MS = 1000L
        private const val SABOTAGE_SEED_MASK: UInt = 0x5A807A9Eu
    }
}
