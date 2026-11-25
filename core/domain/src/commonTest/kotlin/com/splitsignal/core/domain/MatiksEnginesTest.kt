package com.splitsignal.core.domain

import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MatiksEnginesTest {

    @Test
    fun crossMathBodmasAndHintSolveBoardDeterministically() = runTest {
        val b1 = CrossMathGenerator.generate(1234u, CrossMathSize.GRID_3X3)
        val b2 = CrossMathGenerator.generate(1234u, CrossMathSize.GRID_3X3)
        assertEquals(b1.solution, b2.solution)
        assertEquals(b1.rowTargets, b2.rowTargets)
        assertEquals(18, CrossMathGenerator.evaluateBodmas(listOf(2, 4, 4), listOf(MathOp.ADD, MathOp.MUL)))

        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val pipe = InMemorySyncPipe().localLoopback
        val engine = CrossMathEngine(
            seed = 1234u,
            size = CrossMathSize.GRID_3X3,
            isRace = false,
            transport = pipe,
            scope = scope,
        )
        repeat(CrossMathSize.GRID_3X3.blankCount) {
            engine.applyHint()
        }
        assertTrue(engine.state.value.isSolved)
        assertTrue(engine.state.value.rowSatisfied.all { it })
        assertTrue(engine.state.value.colSatisfied.all { it })
        engine.dispose()
    }

    @Test
    fun mathMazeGeneratesValidPathAndSolvesAlongSolution() = runTest {
        val board = MathMazeGenerator.generate(4242u, dimension = 4)
        assertEquals(0, board.solutionPath.first())
        assertEquals(15, board.solutionPath.last())

        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val engine = MathMazeEngine(
            seed = 4242u,
            dimension = 4,
            isRace = false,
            transport = InMemorySyncPipe().localLoopback,
            scope = scope,
        )
        for (idx in board.solutionPath.drop(1)) {
            engine.tapCell(idx)
        }
        assertTrue(engine.state.value.isSolved)
        assertEquals(board.targetGoal, engine.state.value.runningTotal)
        engine.dispose()
    }

    @Test
    fun kenKenGeneratesLatinSquareAndSolvesViaHints() = runTest {
        val board = KenKenGenerator.generate(999u, KenKenSize.GRID_4X4)
        for (r in 0 until 4) {
            val rowSet = (0 until 4).map { c -> board.solution[r * 4 + c] }.toSet()
            assertEquals(setOf(1, 2, 3, 4), rowSet)
        }
        for (c in 0 until 4) {
            val colSet = (0 until 4).map { r -> board.solution[r * 4 + c] }.toSet()
            assertEquals(setOf(1, 2, 3, 4), colSet)
        }

        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val engine = KenKenEngine(
            seed = 999u,
            size = KenKenSize.GRID_4X4,
            isRace = false,
            transport = InMemorySyncPipe().localLoopback,
            scope = scope,
        )
        repeat(16) {
            engine.applyHint()
        }
        assertTrue(engine.state.value.isSolved)
        assertTrue(engine.state.value.conflictCells.isEmpty())
        engine.dispose()
    }

    @Test
    fun memoryMatrixSolvesExpectedRecallIndices() = runTest {
        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val engine = MemoryMatrixEngine(
            seed = 777u,
            dimension = 4,
            isRace = false,
            initialRevealMs = 0L,
            transport = InMemorySyncPipe().localLoopback,
            scope = scope,
        )
        assertFalse(engine.state.value.isRevealing)
        for (idx in engine.state.value.board.expectedOrderIndices) {
            engine.tapCell(idx)
        }
        assertTrue(engine.state.value.isSolved)
        assertEquals(100, engine.state.value.percentComplete)
        engine.dispose()
    }

    @Test
    fun mindsnapBuildsComboMultiplierAndFinishesDeck() = runTest {
        val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val engine = MindsnapEngine(
            seed = 555u,
            isRace = false,
            transport = InMemorySyncPipe().localLoopback,
            scope = scope,
        )
        val totalQ = engine.state.value.questions.size
        repeat(totalQ) {
            val q = engine.state.value.currentQuestion
            assertNotNull(q)
            engine.submitAnswer(q.correctIndex)
        }
        assertTrue(engine.state.value.isFinished)
        assertEquals(5, engine.state.value.multiplier)
        assertTrue(engine.state.value.score > 0)
        engine.dispose()
    }

    @Test
    fun progressionStoreTracksEloLeaguesAndDailyWorkout() {
        val store = ProgressionStore()
        assertEquals(1200, store.state.value.elo)
        assertEquals(EloLeague.BRONZE, store.state.value.league)

        val reward = store.recordModeSolve(
            gameType = GameType.CROSS_MATH,
            elapsedSeconds = 25,
            moves = 8,
            mistakes = 0,
            parSeconds = 60,
            isDailyStep = true,
            isRaceWin = true,
        )
        assertTrue(reward.totalCoins > 0)
        assertEquals(1, store.state.value.dailyStepsCompleted)
        assertEquals(1, store.state.value.modeClears[GameType.CROSS_MATH])
    }
}
