package com.splitsignal.core.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MathPuzzleEngineTest {

    @Test
    fun level0GenerationMatchesExpectedWarmUpSpec() {
        val spec = MathLevelGenerator.generate(seed = 0x12345678u, levelIndex = 0)

        assertEquals(0, spec.levelIndex)
        assertEquals("Level 01 · Warm-Up", spec.title)
        assertEquals(listOf(2, 4, 1, 3, 4, 6, 7, 2, 5), spec.initialGrid)
        assertEquals(10, spec.targetRow0Sum)
        assertEquals(12, spec.targetCol1Sum)
        assertEquals(11, spec.targetDiagSum)
        assertTrue(spec.requireCenterPrime)
    }

    @Test
    fun level1PlusGenerationIsDeterministicAndUnsolvedInitially() {
        val specA = MathLevelGenerator.generate(seed = 42u, levelIndex = 1)
        val specB = MathLevelGenerator.generate(seed = 42u, levelIndex = 1)
        val specLevel2 = MathLevelGenerator.generate(seed = 42u, levelIndex = 2)

        assertEquals(specA, specB)
        assertNotEquals(specA.initialGrid, specLevel2.initialGrid)
        assertEquals(9, specA.initialGrid.size)
        assertTrue(specA.initialGrid.all { it in 1..9 })
        assertTrue(specA.requireCenterPrime)
        assertFalse(MathLevelGenerator.evaluateClues(specA, specA.initialGrid).all { it.isSatisfied })
    }

    @Test
    fun evaluateCluesReportsStatementsLiveStatusAndSatisfactionAccurately() {
        val spec = MathLevelGenerator.generate(seed = 0u, levelIndex = 0)

        val initialClues = MathLevelGenerator.evaluateClues(spec, spec.initialGrid)
        assertEquals(4, initialClues.size)

        assertEquals(0, initialClues[0].id)
        assertEquals("Top row sum = 10", initialClues[0].statement)
        assertEquals("Current: 7", initialClues[0].liveStatus)
        assertFalse(initialClues[0].isSatisfied)

        assertEquals(1, initialClues[1].id)
        assertEquals("Middle column sum = 12", initialClues[1].statement)
        assertEquals("Current: 10", initialClues[1].liveStatus)
        assertFalse(initialClues[1].isSatisfied)

        assertEquals(2, initialClues[2].id)
        assertEquals("Main diagonal (↘) sum = 11", initialClues[2].statement)
        assertEquals("Current: 11", initialClues[2].liveStatus)
        assertTrue(initialClues[2].isSatisfied)

        assertEquals(3, initialClues[3].id)
        assertEquals("Center tile must be prime (2, 3, 5, 7)", initialClues[3].statement)
        assertEquals("Current: 4", initialClues[3].liveStatus)
        assertFalse(initialClues[3].isSatisfied)

        val solvedGrid = listOf(4, 5, 1, 3, 5, 6, 7, 2, 2)
        val solvedClues = MathLevelGenerator.evaluateClues(spec, solvedGrid)
        assertTrue(solvedClues.all { it.isSatisfied })
        assertEquals("Current: 10", solvedClues[0].liveStatus)
        assertEquals("Current: 12", solvedClues[1].liveStatus)
        assertEquals("Current: 11", solvedClues[2].liveStatus)
        assertEquals("Current: 5", solvedClues[3].liveStatus)
    }

    @Test
    fun solveLevel0ViaOnCellTappedCyclesWrapAndLocksWhenSolved() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val engineScope = CoroutineScope(SupervisorJob() + dispatcher)
        val pipe = InMemorySyncPipe()
        val operator = MathPuzzleEngine(
            role = PlayerRole.OPERATOR,
            seed = 99u,
            transport = pipe.localLoopback,
            scope = engineScope,
        )

        assertFalse(operator.state.value.isSolved)

        // Initial Level 0: [2, 4, 1,  3, 4, 6,  7, 2, 5]
        // Target solution: [4, 5, 1,  3, 5, 6,  7, 2, 2]
        // Cell 0: 2 -> 4 (2 taps)
        repeat(2) { operator.onCellTapped(0) }
        // Cell 1: 4 -> 5 (1 tap)
        operator.onCellTapped(1)
        // Cell 4: 4 -> 5 (1 tap)
        operator.onCellTapped(4)
        // Cell 8: 5 -> 6 -> 7 -> 8 -> 9 -> 1 -> 2 (6 taps, tests 9 -> 1 wrap)
        repeat(6) { operator.onCellTapped(8) }

        val solvedState = operator.state.value
        assertEquals(listOf(4, 5, 1, 3, 5, 6, 7, 2, 2), solvedState.grid)
        assertTrue(solvedState.isSolved)
        assertTrue(solvedState.clues.all { it.isSatisfied })

        // Tapping after solved must be ignored
        operator.onCellTapped(0)
        assertEquals(4, operator.state.value.grid[0])

        operator.dispose()
        pipe.localLoopback.close()
    }

    @Test
    fun operatorAndAnalystSyncOverInMemorySyncPipeAcrossTapsAdvanceAndRestart() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val engineScope = CoroutineScope(SupervisorJob() + dispatcher)
        val (hostTransport, clientTransport) = InMemorySyncPipe.createPair()
        val initialSeed = Fnv1a.hash32("MATH")

        val operator = MathPuzzleEngine(
            role = PlayerRole.OPERATOR,
            seed = initialSeed,
            transport = hostTransport,
            scope = engineScope,
        )
        val analyst = MathPuzzleEngine(
            role = PlayerRole.ANALYST,
            seed = initialSeed,
            transport = clientTransport,
            scope = engineScope,
        )

        // Analyst taps are ignored and do not mutate state or send packets
        analyst.onCellTapped(0)
        assertEquals(operator.state.value.grid, analyst.state.value.grid)

        // Operator solves Level 0 and Analyst receives every TileIncremented update
        repeat(2) { operator.onCellTapped(0) }
        operator.onCellTapped(1)
        operator.onCellTapped(4)
        repeat(6) { operator.onCellTapped(8) }

        assertEquals(operator.state.value.grid, analyst.state.value.grid)
        assertEquals(operator.state.value.clues, analyst.state.value.clues)
        assertTrue(analyst.state.value.isSolved)

        // Advance to Level 1 syncs both engines to identical level state
        operator.onNextLevel()
        assertEquals(1, operator.state.value.levelIndex)
        assertEquals(1, analyst.state.value.levelIndex)
        assertEquals(operator.state.value.title, analyst.state.value.title)
        assertEquals(operator.state.value.grid, analyst.state.value.grid)
        assertEquals(operator.state.value.clues, analyst.state.value.clues)
        assertFalse(analyst.state.value.isSolved)

        // RestartRun packet resets to Level 0 using Fnv1a.hash32(roomCode)
        val restartPacket = SyncPacket.RestartRun(roomCode = "NEXT", untimedMode = true)
        hostTransport.send(restartPacket)
        clientTransport.send(restartPacket)

        assertEquals(0, operator.state.value.levelIndex)
        assertEquals(0, analyst.state.value.levelIndex)
        assertEquals("Level 01 · Warm-Up", analyst.state.value.title)

        // Advancing after restart uses the new roomCode seed
        operator.onNextLevel()
        val expectedSpec = MathLevelGenerator.generate(Fnv1a.hash32("NEXT"), 1)
        assertEquals(expectedSpec.initialGrid, operator.state.value.grid)
        assertEquals(expectedSpec.initialGrid, analyst.state.value.grid)

        operator.dispose()
        analyst.dispose()
        hostTransport.close()
    }

    @Test
    fun newSyncPacketsRoundTripThroughKotlinxSerialization() {
        val json = Json { encodeDefaults = true }
        val packets: List<SyncPacket> = listOf(
            SyncPacket.TileIncremented(cellIndex = 4, newValue = 7),
            SyncPacket.AdvanceLevel(nextLevelIndex = 3),
        )

        for (packet in packets) {
            val encoded = json.encodeToString<SyncPacket>(packet)
            val decoded = json.decodeFromString<SyncPacket>(encoded)
            assertEquals(packet, decoded)
        }
    }
}
