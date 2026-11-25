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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BattleshipEngineTest {

    @Test
    fun deterministicPlacementCreatesSevenNonOverlappingCellsPerFleet() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val (hostTransport, clientTransport) = InMemorySyncPipe.createPair()
        val seed = Fnv1a.hash32("SHIP")

        val op1 = BattleshipEngine(PlayerRole.OPERATOR, seed, vsAi = false, hostTransport, scope)
        val op2 = BattleshipEngine(PlayerRole.OPERATOR, seed, vsAi = false, InMemorySyncPipe().localLoopback, scope)
        val analyst = BattleshipEngine(PlayerRole.ANALYST, seed, vsAi = false, clientTransport, scope)

        assertEquals(op1.state.value.myShips, op2.state.value.myShips)
        assertEquals(op1.state.value.enemyShips, op2.state.value.enemyShips)
        assertEquals(7, op1.state.value.myShips.size)
        assertEquals(7, op1.state.value.enemyShips.size)
        assertTrue(op1.state.value.myShips.all { it in 0 until BattleshipEngine.GRID_SIZE })
        assertTrue(op1.state.value.enemyShips.all { it in 0 until BattleshipEngine.GRID_SIZE })

        // Host (Operator) and Joiner (Analyst) hold mirrored fleet views for the same seed.
        assertEquals(op1.state.value.myShips, analyst.state.value.enemyShips)
        assertEquals(op1.state.value.enemyShips, analyst.state.value.myShips)

        op1.dispose()
        op2.dispose()
        analyst.dispose()
        hostTransport.close()
    }

    @Test
    fun coOpFiringSyncsHitsMissesTurnsAndVictory() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val (hostTransport, clientTransport) = InMemorySyncPipe.createPair()
        val seed = 0x13572468u

        val operator = BattleshipEngine(PlayerRole.OPERATOR, seed, vsAi = false, hostTransport, scope)
        val analyst = BattleshipEngine(PlayerRole.ANALYST, seed, vsAi = false, clientTransport, scope)

        assertTrue(operator.state.value.isMyTurn)
        assertFalse(analyst.state.value.isMyTurn)

        val opHitTarget = operator.state.value.enemyShips.first()
        val opMissTarget = (0 until BattleshipEngine.GRID_SIZE).first { it !in operator.state.value.enemyShips }

        // Analyst cannot fire out of turn.
        analyst.fireAt(0)
        assertTrue(analyst.state.value.enemyHits.isEmpty())
        assertTrue(analyst.state.value.enemyMisses.isEmpty())

        // Operator fires a hit -> turn passes to Analyst.
        operator.fireAt(opHitTarget)
        assertTrue(opHitTarget in operator.state.value.enemyHits)
        assertTrue(opHitTarget in analyst.state.value.myHitsReceived)
        assertFalse(operator.state.value.isMyTurn)
        assertTrue(analyst.state.value.isMyTurn)

        // Analyst fires a miss -> turn passes back to Operator.
        val analystMissTarget = (0 until BattleshipEngine.GRID_SIZE).first { it !in analyst.state.value.enemyShips }
        analyst.fireAt(analystMissTarget)
        assertTrue(analystMissTarget in analyst.state.value.enemyMisses)
        assertTrue(analystMissTarget in operator.state.value.myMissesReceived)
        assertTrue(operator.state.value.isMyTurn)
        assertFalse(analyst.state.value.isMyTurn)

        // Operator fires a miss, Analyst fires back, then Operator sinks remaining ships.
        operator.fireAt(opMissTarget)
        assertTrue(opMissTarget in operator.state.value.enemyMisses)
        assertTrue(opMissTarget in analyst.state.value.myMissesReceived)

        val remainingOpTargets = operator.state.value.enemyShips.filter { it != opHitTarget }
        val analystShots = (0 until BattleshipEngine.GRID_SIZE)
            .filter { it !in analyst.state.value.enemyShips && it != analystMissTarget }
            .iterator()

        for (target in remainingOpTargets) {
            if (!operator.state.value.isMyTurn) {
                analyst.fireAt(analystShots.next())
            }
            operator.fireAt(target)
        }

        assertEquals(PlayerRole.OPERATOR, operator.state.value.winnerRole)
        assertEquals(PlayerRole.OPERATOR, analyst.state.value.winnerRole)
        assertFalse(operator.state.value.isMyTurn)
        assertFalse(analyst.state.value.isMyTurn)

        operator.dispose()
        analyst.dispose()
        hostTransport.close()
    }

    @Test
    fun sonarScanReveals2x2AreaWithoutConsumingTurnAndCapsAtTwoCharges() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val transport = InMemorySyncPipe().localLoopback
        val engine = BattleshipEngine(PlayerRole.OPERATOR, 42u, vsAi = true, transport, scope)

        assertEquals(2, engine.state.value.sonarChargesLeft)

        val firstShipCell = engine.state.value.enemyShips.first()
        engine.sonarScan(firstShipCell)

        val afterFirst = engine.state.value
        assertEquals(1, afterFirst.sonarChargesLeft)
        assertTrue(afterFirst.isMyTurn)
        assertTrue(firstShipCell in afterFirst.sonarRevealedShips)
        assertEquals(4, afterFirst.sonarRevealedShips.size + afterFirst.sonarScannedEmpty.size)

        // Edge-clamped scan at bottom-right cell (35) still scans a 2x2 block {28, 29, 34, 35}.
        engine.sonarScan(35)
        val afterSecond = engine.state.value
        assertEquals(0, afterSecond.sonarChargesLeft)
        assertTrue(afterSecond.isMyTurn)
        val covered = afterSecond.sonarRevealedShips + afterSecond.sonarScannedEmpty
        assertTrue(setOf(28, 29, 34, 35).all { it in covered })

        // Third scan has no charges left and does not modify board.
        val beforeThird = afterSecond
        engine.sonarScan(0)
        assertEquals(beforeThird, engine.state.value)

        engine.dispose()
        transport.close()
    }

    @Test
    fun smartAiFiresBackAndTargetsOrthogonalNeighborsAfterHit() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val transport = InMemorySyncPipe().localLoopback
        val engine = BattleshipEngine(PlayerRole.OPERATOR, 98765u, vsAi = true, transport, scope)

        val safeMissCells = (0 until BattleshipEngine.GRID_SIZE)
            .filter { it !in engine.state.value.enemyShips }

        var observedTargetFollowUp = false
        for (cell in safeMissCells) {
            if (engine.state.value.winnerRole != null) break

            val hitsBefore = engine.state.value.myHitsReceived
            val shotsBefore = hitsBefore + engine.state.value.myMissesReceived
            val hadUnsunkHitBefore = hitsBefore.any { hit ->
                // Check if any orthogonal neighbor of an existing hit is still unshot.
                orthogonalNeighbors(hit).any { it !in shotsBefore }
            }

            engine.fireAt(cell)
            val stateAfter = engine.state.value
            val shotsAfter = stateAfter.myHitsReceived + stateAfter.myMissesReceived
            val newAiShots = shotsAfter - shotsBefore
            assertEquals(1, newAiShots.size)

            if (hadUnsunkHitBefore) {
                val aiShot = newAiShots.first()
                val isNeighborOfPriorHit = hitsBefore.any { hit -> aiShot in orthogonalNeighbors(hit) }
                if (isNeighborOfPriorHit) {
                    observedTargetFollowUp = true
                    break
                }
            }
        }

        assertTrue(observedTargetFollowUp, "Smart AI should target orthogonal neighbors of unsunk hits")

        engine.dispose()
        transport.close()
    }

    @Test
    fun restartResetsBoardWithFreshSeedAndPacketsSerialize() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val (hostTransport, clientTransport) = InMemorySyncPipe.createPair()

        val operator = BattleshipEngine(PlayerRole.OPERATOR, 100u, vsAi = false, hostTransport, scope)
        val analyst = BattleshipEngine(PlayerRole.ANALYST, 100u, vsAi = false, clientTransport, scope)

        val initialShips = operator.state.value.myShips
        operator.sonarScan(0)
        operator.fireAt(0)
        assertEquals(1, operator.state.value.sonarChargesLeft)
        assertEquals(1, operator.state.value.enemyHits.size + operator.state.value.enemyMisses.size)

        operator.restart()

        assertEquals(2, operator.state.value.sonarChargesLeft)
        assertTrue(operator.state.value.enemyHits.isEmpty())
        assertTrue(operator.state.value.enemyMisses.isEmpty())
        assertTrue(analyst.state.value.myHitsReceived.isEmpty())
        assertTrue(analyst.state.value.myMissesReceived.isEmpty())
        assertNull(operator.state.value.winnerRole)
        assertTrue(operator.state.value.isMyTurn)
        assertFalse(analyst.state.value.isMyTurn)
        assertNotEquals(initialShips, operator.state.value.myShips)
        assertEquals(operator.state.value.myShips, analyst.state.value.enemyShips)

        val json = Json { encodeDefaults = true }
        val packets: List<SyncPacket> = listOf(
            SyncPacket.BattleshipShot(cellIndex = 14, shooterRole = PlayerRole.OPERATOR),
            SyncPacket.BattleshipSonar(centerIndex = 7, scannerRole = PlayerRole.ANALYST),
            SyncPacket.BattleshipRestart(seed = 555u),
        )
        for (packet in packets) {
            val decoded = json.decodeFromString<SyncPacket>(json.encodeToString<SyncPacket>(packet))
            assertEquals(packet, decoded)
        }

        operator.dispose()
        analyst.dispose()
        hostTransport.close()
    }

    @Test
    fun inviteTokenCodecParsesAndFormatsTokens() {
        val full = InviteTokenCodec.format(" ab23 ", "192.168.1.44", 51234)
        assertEquals("AB23@192.168.1.44:51234", full)

        val parsedFull = InviteTokenCodec.parse("ab23@192.168.1.44:51234")
        assertEquals(InviteToken(roomCode = "AB23", hostIp = "192.168.1.44", wsPort = 51234), parsedFull)

        val parsedEndpoint = InviteTokenCodec.parse("10.0.2.2:8080")
        assertEquals(InviteToken(roomCode = null, hostIp = "10.0.2.2", wsPort = 8080), parsedEndpoint)

        assertNull(InviteTokenCodec.parse(""))
        assertNull(InviteTokenCodec.parse("@192.168.1.1:8080"))
        assertNull(InviteTokenCodec.parse("192.168.1.1:99999"))
        assertNull(InviteTokenCodec.parse("192.168.1.1"))
    }

    private fun orthogonalNeighbors(cellIndex: Int): Set<Int> {
        val side = BattleshipEngine.GRID_SIDE
        val row = cellIndex / side
        val col = cellIndex % side
        return buildSet {
            if (row > 0) add((row - 1) * side + col)
            if (row < side - 1) add((row + 1) * side + col)
            if (col > 0) add(row * side + (col - 1))
            if (col < side - 1) add(row * side + (col + 1))
        }
    }
}
