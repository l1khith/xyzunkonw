package com.splitsignal.core.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SudokuEngineTest {

    @Test
    fun generatorProducesValidUniqueSolutionBoardsFor6x6And9x9AcrossDifficulties() {
        val seeds = listOf(101u, 2026u)
        for (size in SudokuSize.entries) {
            val total = size.dim * size.dim
            for (difficulty in SudokuDifficulty.entries) {
                for (seed in seeds) {
                    val board = SudokuGenerator.generate(size, difficulty, seed)
                    assertEquals(total, board.givens.size)
                    assertEquals(total, board.solution.size)
                    assertTrue(
                        SudokuGenerator.isValidSolvedGrid(size, board.solution),
                        "Solution invalid for $size $difficulty $seed",
                    )
                    assertTrue(
                        SudokuGenerator.hasUniqueSolution(size, board.givens),
                        "Board does not have strictly 1 unique solution for $size $difficulty $seed",
                    )
                    for (i in 0 until total) {
                        if (board.givens[i] != 0) {
                            assertEquals(board.solution[i], board.givens[i])
                        }
                        // 180-degree rotational symmetry preserved
                        assertEquals(board.givens[i] == 0, board.givens[total - 1 - i] == 0)
                    }
                    assertTrue(board.logicScore > 0)
                }
            }
        }
    }

    @Test
    fun generatorIsDeterministicForSameSeedAndVariesAcrossSeeds() {
        val boardA = SudokuGenerator.generate(SudokuSize.SPRINT_6X6, SudokuDifficulty.MEDIUM, 0xCAFEBABEu)
        val boardB = SudokuGenerator.generate(SudokuSize.SPRINT_6X6, SudokuDifficulty.MEDIUM, 0xCAFEBABEu)
        val boardOther = SudokuGenerator.generate(SudokuSize.SPRINT_6X6, SudokuDifficulty.MEDIUM, 0xDEADBEEFu)

        assertEquals(boardA, boardB)
        assertNotEquals(boardA.solution, boardOther.solution)

        val board9A = SudokuGenerator.generate(SudokuSize.CLASSIC_9X9, SudokuDifficulty.HARD, 777u)
        val board9B = SudokuGenerator.generate(SudokuSize.CLASSIC_9X9, SudokuDifficulty.HARD, 777u)
        assertEquals(board9A, board9B)
    }

    @Test
    fun findLogicalHintDetectsNakedSingleAndHiddenSingleAndReturnsNullWhenSolved() {
        val fullBoard = SudokuGenerator.generate(SudokuSize.SPRINT_6X6, SudokuDifficulty.EASY, 42u)
        val solution = fullBoard.solution

        // Leave exactly 1 empty cell -> guaranteed Naked Single.
        val nakedGrid = solution.toMutableList().apply { this[0] = 0 }
        val nakedHint = SudokuGenerator.findLogicalHint(SudokuSize.SPRINT_6X6, nakedGrid, solution)
        assertNotNull(nakedHint)
        assertEquals(0, nakedHint.cellIndex)
        assertEquals(solution[0], nakedHint.digit)
        assertEquals("Naked Single", nakedHint.technique)
        assertTrue(nakedHint.explanation.contains("Row 1, Col 1"))

        // Leave an entire 2x3 box empty? Better: construct a 6x6 grid with no Naked Single, only Hidden Single.
        // Start from a solved board and clear all cells except 1 row/col constraint that forces a Hidden Single
        // while ensuring no cell has a single candidate.
        val hiddenSolution = listOf(
            1, 2, 3, 4, 5, 6,
            4, 5, 6, 1, 2, 3,
            2, 1, 4, 3, 6, 5,
            3, 6, 5, 2, 1, 4,
            5, 3, 1, 6, 4, 2,
            6, 4, 2, 5, 3, 1,
        )
        // Clear two cells in row 0 (cols 0 and 1 -> digits 1 and 2) AND two cells in row 2 (cols 0 and 1 -> digits 2 and 1),
        // plus one cell in col 0 of row 1 (digit 4) and col 2 of row 0 (digit 3) so row 0 has 1,2,3 open,
        // actually let's test a direct Hidden Single board where every empty cell has >= 2 candidates:
        // Clear (0,0)=1, (0,1)=2, (2,0)=2, (2,1)=1, and also (3,3)=2, (3,4)=1, (1,3)=1, (1,4)=2 -> each has {1,2},
        // Wait: if we also block 1 in (0,1) via (4,1) already having 3? No, put 1 in (5,1)? Wait: (5,1)=4.
        // Let's verify: in hiddenSolution, cell (0,0)=1, (0,1)=2, (2,0)=2, (2,1)=1.
        // If we clear (0,0), (0,1), (2,0), (2,1), AND clear (4,1)=3, (4,2)=1, (0,2)=3 ->
        // Now in Row 0: empty cells are (0,0), (0,1), (0,2) missing {1, 2, 3}.
        // Col 0 has: 4, 3, 5, 6 (missing 1, 2).
        // Col 1 has: 5, 6, 4 (missing 1, 2, 3).
        // Col 2 has: 6, 4, 5, 1, 2 (missing 3)! Wait -> (0,2) would be a Naked Single = 3.
        // Instead clear (2,2)=4 as well -> Col 2 missing {3, 4}, Box 0 missing {1,2,3,4}, Row 0 missing {1,2,3}.
        // Then:
        // (0,0) candidates: Row0{1,2,3} & Col0{1,2} & Box0{1,2,3,4} -> {1,2} (2 candidates)
        // (0,1) candidates: Row0{1,2,3} & Col1{1,2,3} & Box0{1,2,3,4} -> {1,2,3} (3 candidates)
        // (0,2) candidates: Row0{1,2,3} & Col2{3,4} & Box0{1,2,3,4} -> {3} -> still 1!
        // Make (4,2)=1 stays? No, if we clear (4,2)=1 and (4,4)=4, Row 4 missing {1,4}, (4,2) has {1,4}, (0,2) has {1,3}?
        // Let's check: if (4,2) is 0, Col 2 is missing {1,3,4}. Row 0 missing {1,2,3}.
        // Then (0,2) candidates: {1,3} (2 candidates)!
        // (2,0) candidates: Row2 missing {1,2,4}, Col0 missing {1,2}, Box1? (2,0) is Box2! Box2 missing {2,1,4}. -> {1,2} (2 candidates)!
        // (2,1) candidates: Row2 missing {1,2,4}, Col1 missing {1,2,3} -> {1,2} (2 candidates)!
        // (2,2) candidates: Row2 missing {1,2,4}, Col2 missing {1,3,4} -> {1,4} (2 candidates)!
        // (4,1) candidates: Row4 missing {3,1,4}, Col1 missing {1,2,3} -> {1,3} (2 candidates)!
        // (4,2) candidates: Row4 missing {3,1,4}, Col2 missing {1,3,4} -> {1,3,4} (3 candidates)!
        // (4,4) candidates: Row4 missing {3,1,4}, Col4 missing {4}? Col4 only missing 4 -> clear (5,4)=3 and (5,1)=4?
        // Actually in Row 0, notice digit 3 can ONLY go in (0,1) if (0,2) can't be 3! Wait: (0,2) CAN be 3, so Row 0 has 3 in (0,1) and (0,2).
        // Meanwhile in Row 0, digit 2 can go in (0,0) and (0,1). In Row 0, digit 1 can go in (0,0), (0,1), (0,2).
        // Where is the Hidden Single? Look at Row 2: digit 4 can ONLY go in (2,2)!! Because (2,0) and (2,1) are blocked from 4 by Box2? No, Box2 has (3,0)=3, (3,1)=6, (3,2)=5, so 4 is NOT in Box2. Wait: Col 0 has 4 at (1,0), Col 1 has 4 at (5,1)! So neither (2,0) nor (2,1) can be 4! Therefore in Row 2, digit 4 can ONLY go in (2,2), even though EVERY empty cell has >= 2 candidates!
        val hiddenGrid = hiddenSolution.toMutableList().apply {
            this[0 * 6 + 0] = 0 // 1, cand {1,2}
            this[0 * 6 + 1] = 0 // 2, cand {1,2,3}
            this[0 * 6 + 2] = 0 // 3, cand {1,3}
            this[2 * 6 + 0] = 0 // 2, cand {1,2}
            this[2 * 6 + 1] = 0 // 1, cand {1,2}
            this[2 * 6 + 2] = 0 // 4, cand {1,4} -> Hidden Single 4 in Row 2!
            this[4 * 6 + 1] = 0 // 3, cand {1,3}
            this[4 * 6 + 2] = 0 // 1, cand {1,3}
        }
        val hiddenHint = SudokuGenerator.findLogicalHint(SudokuSize.SPRINT_6X6, hiddenGrid, hiddenSolution)
        assertNotNull(hiddenHint)
        assertTrue(hiddenHint.technique.startsWith("Hidden Single"), "Expected Hidden Single, got ${hiddenHint.technique}")
        assertEquals(hiddenSolution[hiddenHint.cellIndex], hiddenHint.digit)

        // Already solved grid returns null.
        assertNull(SudokuGenerator.findLogicalHint(SudokuSize.SPRINT_6X6, solution, solution))
    }

    @Test
    fun raceEngineValidatesMistakesLockoutHintsEraseSabotageAndRestart() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val engineScope = CoroutineScope(SupervisorJob() + dispatcher)
        val (hostTransport, clientTransport) = InMemorySyncPipe.createPair()

        val collectedFromHost = mutableListOf<SyncPacket>()
        val collectJob = engineScope.launch {
            clientTransport.incomingPackets.collect { packet ->
                collectedFromHost.add(packet)
            }
        }

        val engine = SudokuRaceEngine(
            seed = 1234u,
            size = SudokuSize.SPRINT_6X6,
            difficulty = SudokuDifficulty.MEDIUM,
            isRace = true,
            isDailySprint = false,
            transport = hostTransport,
            scope = engineScope,
        )
        try {
            runCurrent()

            val initial = engine.state.value
            val emptyIndices = initial.givens.indices.filter { initial.givens[it] == 0 }
            assertTrue(emptyIndices.size >= 16)

            // 1. Timer ticks at 1Hz
            advanceTimeBy(2000L)
            runCurrent()
            assertEquals(2, engine.state.value.elapsedSeconds)

            // 2. Wrong digit increments mistakes and triggers 3s lockout
            val firstEmpty = emptyIndices[0]
            val rightDigit = initial.solution[firstEmpty]
            val wrongDigit = if (rightDigit == 1) 2 else 1
            engine.selectCell(firstEmpty)
            engine.enterDigit(wrongDigit)
            runCurrent()

            assertEquals(1, engine.state.value.mistakes)
            assertEquals(3, engine.state.value.lockoutSecondsRemaining)
            assertEquals(0, engine.state.value.grid[firstEmpty])

            // Entering digit while locked out is rejected
            engine.enterDigit(rightDigit)
            assertEquals(0, engine.state.value.grid[firstEmpty])

            // Wait 3s for lockout to expire
            advanceTimeBy(3000L)
            runCurrent()
            assertEquals(0, engine.state.value.lockoutSecondsRemaining)

            // 3. Apply hint highlights a cell and enterDigit clears activeHint on that cell
            val hint = engine.applyHint()
            assertNotNull(hint)
            assertEquals(hint, engine.state.value.activeHint)
            assertEquals(hint.cellIndex, engine.state.value.selectedCell)

            engine.enterDigit(hint.digit)
            runCurrent()
            assertNull(engine.state.value.activeHint)
            assertEquals(hint.digit, engine.state.value.grid[hint.cellIndex])

            // 4. Erase clears non-given cell, ignores given cell
            engine.eraseSelected()
            runCurrent()
            assertEquals(0, engine.state.value.grid[hint.cellIndex])

            val givenIndex = initial.givens.indices.first { initial.givens[it] != 0 }
            val givenValue = initial.givens[givenIndex]
            engine.selectCell(givenIndex)
            engine.eraseSelected()
            assertEquals(givenValue, engine.state.value.grid[givenIndex])

            // 5. Fill 15 unique valid cells -> emits SudokuSabotage packet
            var placed = 0
            for (idx in emptyIndices) {
                if (placed == 15) break
                engine.selectCell(idx)
                engine.enterDigit(initial.solution[idx])
                placed++
            }
            runCurrent()
            assertTrue(collectedFromHost.any { it is SyncPacket.SudokuSabotage })
            assertTrue(collectedFromHost.any { it is SyncPacket.SudokuProgress && it.percent > 0 })

            // 6. Incoming SudokuSabotage blurs target box for 4s and decays via tick
            clientTransport.send(SyncPacket.SudokuSabotage(targetBoxIndex = 2))
            runCurrent()
            assertEquals(2, engine.state.value.sabotageBoxIndex)
            assertEquals(4, engine.state.value.sabotageSecondsRemaining)

            advanceTimeBy(4000L)
            runCurrent()
            assertNull(engine.state.value.sabotageBoxIndex)
            assertEquals(0, engine.state.value.sabotageSecondsRemaining)

            // 7. Solve remaining cells -> isSolved = true and finished = true packet emitted
            for (idx in emptyIndices) {
                if (engine.state.value.grid[idx] == 0) {
                    engine.selectCell(idx)
                    engine.enterDigit(initial.solution[idx])
                }
            }
            runCurrent()
            assertTrue(engine.state.value.isSolved)
            assertTrue(collectedFromHost.any { it is SyncPacket.SudokuProgress && it.finished && it.percent == 100 })

            // 8. Restart generates fresh board
            engine.restart(9999u)
            runCurrent()
            assertEquals(9999u, engine.state.value.seed)
            assertFalse(engine.state.value.isSolved)
            assertEquals(0, engine.state.value.mistakes)
        } finally {
            collectJob.cancel()
            engine.dispose()
            hostTransport.close()
        }
    }

    @Test
    fun progressionStoreCalculatesRewardsExtendsStreakAndHandlesShopPurchases() {
        val store = ProgressionStore(
            initialState = ProgressionState(
                streakDays = 6,
                coins = 250,
                xp = 840,
                elo = 1200,
                solvedCount = 12,
                duelWins = 3,
                totalMatches = 12,
            )
        )
        val initial = store.state.value
        assertEquals(6, initial.streakDays)
        assertEquals(250, initial.coins)
        assertEquals("Cipher Analyst", store.rankTitle(initial.xp))

        // First solve of the day: Daily Sprint 6x6 Medium, fast & flawless -> extends streak 6 -> 7 (Silver)
        val reward = store.recordSolve(
            size = SudokuSize.SPRINT_6X6,
            difficulty = SudokuDifficulty.MEDIUM,
            elapsedSeconds = 45,
            mistakes = 0,
            isDailySprint = true,
            isRaceWin = null,
        )
        assertTrue(reward.streakExtended)
        assertEquals(7, reward.newStreakDays)
        assertEquals(StreakTier.SILVER, StreakTier.forDays(reward.newStreakDays))
        assertEquals(55, reward.baseCoins) // 35 medium + 20 daily
        assertEquals(25, reward.speedBonusCoins) // <= 120/2
        assertEquals(20, reward.flawlessBonusCoins)
        assertEquals(100, reward.totalCoins)
        assertTrue(reward.xpEarned > 0)
        assertTrue(reward.scorecardText.contains("7d (Silver)"))

        // Second solve same day does not double-extend streak
        val raceReward = store.recordSolve(
            size = SudokuSize.CLASSIC_9X9,
            difficulty = SudokuDifficulty.HARD,
            elapsedSeconds = 500,
            mistakes = 2,
            isDailySprint = false,
            isRaceWin = true,
        )
        assertFalse(raceReward.streakExtended)
        assertEquals(7, raceReward.newStreakDays)
        assertEquals(24, raceReward.eloDelta)

        // Shop purchases
        val coinsBeforeShop = store.state.value.coins
        assertTrue(store.buyStreakFreeze())
        assertEquals(coinsBeforeShop - 100, store.state.value.coins)
        assertEquals(2, store.state.value.streakFreezes)

        assertTrue(store.buyHintToken())
        assertEquals(3, store.state.value.hintTokens)
        assertTrue(store.consumeHintToken())
        assertEquals(2, store.state.value.hintTokens)

        assertTrue(store.buyOrSelectTheme(BoardTheme.PAPER_IVORY))
        assertEquals(BoardTheme.PAPER_IVORY, store.state.value.activeTheme)
        assertTrue(BoardTheme.PAPER_IVORY in store.state.value.unlockedThemes)

        // Selecting already-unlocked Obsidian Dark costs 0 coins
        val coinsBeforeSelect = store.state.value.coins
        assertTrue(store.buyOrSelectTheme(BoardTheme.OBSIDIAN_DARK))
        assertEquals(coinsBeforeSelect, store.state.value.coins)
        assertEquals(BoardTheme.OBSIDIAN_DARK, store.state.value.activeTheme)

        // Cannot afford Solarized Slate (180) when coins < 180
        val lowCoinStore = ProgressionStore(ProgressionState(coins = 50, hintTokens = 0))
        assertFalse(lowCoinStore.buyStreakFreeze())
        assertFalse(lowCoinStore.buyOrSelectTheme(BoardTheme.SOLARIZED_SLATE))
        assertFalse(lowCoinStore.consumeHintToken())
        assertTrue(lowCoinStore.buyHintToken())
        assertEquals(10, lowCoinStore.state.value.coins)
    }

    @Test
    fun sudokuPacketsAndGameTypesSerializeAndResolveCleanly() {
        assertEquals(GameType.SUDOKU_6X6, GameType.fromCode("SDK6"))
        assertEquals(GameType.SUDOKU_9X9, GameType.fromCode("sdk9"))

        val json = Json { encodeDefaults = true }
        val packets: List<SyncPacket> = listOf(
            SyncPacket.SudokuStart(
                seed = 42u,
                size = SudokuSize.CLASSIC_9X9,
                difficulty = SudokuDifficulty.MASTER,
            ),
            SyncPacket.SudokuProgress(
                percent = 64,
                mistakes = 1,
                elapsedSeconds = 88,
                finished = false,
            ),
            SyncPacket.SudokuSabotage(targetBoxIndex = 4),
        )
        for (packet in packets) {
            val encoded = json.encodeToString<SyncPacket>(packet)
            val decoded = json.decodeFromString<SyncPacket>(encoded)
            assertEquals(packet, decoded)
        }
    }
}
