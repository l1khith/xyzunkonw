package com.splitsignal.core.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Synchronized state for the 6x6 Tactical Battleship game mode.
 */
data class BattleshipGameState(
    val localRole: PlayerRole,
    val vsAi: Boolean,
    val myShips: Set<Int>,
    val enemyShips: Set<Int>,
    val myHitsReceived: Set<Int>,
    val myMissesReceived: Set<Int>,
    val enemyHits: Set<Int>,
    val enemyMisses: Set<Int>,
    val sonarRevealedShips: Set<Int>,
    val sonarScannedEmpty: Set<Int>,
    val sonarChargesLeft: Int,
    val isMyTurn: Boolean,
    val winnerRole: PlayerRole?,
    val statusMessage: String,
)

private data class FleetLayout(
    val ships: List<Set<Int>>,
    val allCells: Set<Int>,
)

/**
 * Coordinates the 6x6 Tactical Battleship match, supporting solo vs Smart AI and 2-device LAN co-op.
 */
class BattleshipEngine(
    private val localRole: PlayerRole,
    seed: UInt,
    private val vsAi: Boolean,
    private val transport: SyncTransport,
    scope: CoroutineScope,
) {
    private var currentSeed: UInt = seed
    private var myFleet: List<Set<Int>> = emptyList()
    private var enemyFleet: List<Set<Int>> = emptyList()
    private var aiRng: DeterministicRng = DeterministicRng(seed xor AI_SEED_MASK)

    private val _state: MutableStateFlow<BattleshipGameState> = MutableStateFlow(
        buildInitialState(currentSeed),
    )

    /**
     * Observable state stream for the active Tactical Battleship match.
     */
    val state: StateFlow<BattleshipGameState> = _state.asStateFlow()

    private val collectorJob: Job = scope.launch {
        transport.incomingPackets.collect { packet ->
            handleIncomingPacket(packet)
        }
    }

    /**
     * Fires a torpedo at [cellIndex] (`0..35`) on the opponent's 6x6 grid.
     */
    suspend fun fireAt(cellIndex: Int) {
        val snapshot = _state.value
        if (snapshot.winnerRole != null || !snapshot.isMyTurn) return
        if (cellIndex !in 0 until GRID_SIZE) return
        if (cellIndex in snapshot.enemyHits || cellIndex in snapshot.enemyMisses) return

        val isHit = cellIndex in snapshot.enemyShips
        val updatedEnemyHits = if (isHit) snapshot.enemyHits + cellIndex else snapshot.enemyHits
        val updatedEnemyMisses = if (isHit) snapshot.enemyMisses else snapshot.enemyMisses + cellIndex
        val playerWon = snapshot.enemyShips.all { it in updatedEnemyHits }

        if (playerWon) {
            _state.update { current ->
                current.copy(
                    enemyHits = updatedEnemyHits,
                    enemyMisses = updatedEnemyMisses,
                    isMyTurn = false,
                    winnerRole = localRole,
                    statusMessage = "Victory! All enemy vessels neutralized.",
                )
            }
            if (!vsAi) {
                transport.send(SyncPacket.BattleshipShot(cellIndex = cellIndex, shooterRole = localRole))
            }
            return
        }

        if (vsAi) {
            val postPlayerState = snapshot.copy(
                enemyHits = updatedEnemyHits,
                enemyMisses = updatedEnemyMisses,
                isMyTurn = false,
                statusMessage = if (isHit) {
                    "Direct hit at ${coordLabel(cellIndex)}!"
                } else {
                    "Shot missed at ${coordLabel(cellIndex)}."
                },
            )
            _state.value = executeAiTurn(postPlayerState, playerHit = isHit)
        } else {
            _state.update { current ->
                current.copy(
                    enemyHits = updatedEnemyHits,
                    enemyMisses = updatedEnemyMisses,
                    isMyTurn = false,
                    statusMessage = if (isHit) {
                        "Direct hit at ${coordLabel(cellIndex)}! Awaiting opponent."
                    } else {
                        "Missed at ${coordLabel(cellIndex)}. Opponent's turn."
                    },
                )
            }
            transport.send(SyncPacket.BattleshipShot(cellIndex = cellIndex, shooterRole = localRole))
        }
    }

    /**
     * Deploys a 2x2 Sonar Scan anchored near [cellIndex] without consuming a firing turn.
     */
    suspend fun sonarScan(cellIndex: Int) {
        val snapshot = _state.value
        if (snapshot.winnerRole != null || !snapshot.isMyTurn) return
        if (snapshot.sonarChargesLeft <= 0) return
        if (cellIndex !in 0 until GRID_SIZE) return

        val sectorCells = computeSonarSector(cellIndex)
        val detectedShips = sectorCells.filter { it in snapshot.enemyShips }.toSet()
        val detectedEmpty = sectorCells - detectedShips

        _state.update { current ->
            current.copy(
                sonarRevealedShips = current.sonarRevealedShips + detectedShips,
                sonarScannedEmpty = current.sonarScannedEmpty + detectedEmpty,
                sonarChargesLeft = (current.sonarChargesLeft - 1).coerceAtLeast(0),
                statusMessage = if (detectedShips.isNotEmpty()) {
                    "Sonar ping: ${detectedShips.size} target signature(s) in 2x2 sector!"
                } else {
                    "Sonar ping: 2x2 sector clear."
                },
            )
        }

        if (!vsAi) {
            transport.send(SyncPacket.BattleshipSonar(centerIndex = cellIndex, scannerRole = localRole))
        }
    }

    /**
     * Starts a fresh Tactical Battleship match with a deterministically rotated seed.
     */
    suspend fun restart() {
        val nextSeed = currentSeed + RESTART_SEED_STEP
        resetWithSeed(nextSeed)
        if (!vsAi) {
            transport.send(SyncPacket.BattleshipRestart(seed = nextSeed))
        }
    }

    /**
     * Cancels the incoming packet collector job.
     */
    fun dispose() {
        collectorJob.cancel()
    }

    private fun handleIncomingPacket(packet: SyncPacket) {
        when (packet) {
            is SyncPacket.BattleshipShot -> {
                if (vsAi || packet.shooterRole == localRole) return
                applyOpponentShot(packet.cellIndex, packet.shooterRole)
            }

            is SyncPacket.BattleshipSonar -> {
                if (vsAi || packet.scannerRole == localRole) return
                _state.update { current ->
                    if (current.winnerRole != null) return@update current
                    current.copy(
                        statusMessage = "Enemy swept sector ${coordLabel(packet.centerIndex)} with Sonar.",
                    )
                }
            }

            is SyncPacket.BattleshipRestart -> {
                if (!vsAi && packet.seed != currentSeed) {
                    resetWithSeed(packet.seed)
                }
            }

            is SyncPacket.Handshake,
            is SyncPacket.DialUpdate,
            is SyncPacket.SwitchToggle,
            is SyncPacket.StateSync,
            is SyncPacket.AdvanceStage,
            is SyncPacket.RestartRun,
            is SyncPacket.TileIncremented,
            is SyncPacket.AdvanceLevel,
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

    private fun applyOpponentShot(cellIndex: Int, shooterRole: PlayerRole) {
        _state.update { current ->
            if (current.winnerRole != null) return@update current
            if (cellIndex !in 0 until GRID_SIZE) return@update current
            if (cellIndex in current.myHitsReceived || cellIndex in current.myMissesReceived) {
                return@update current
            }

            val isHit = cellIndex in current.myShips
            val updatedMyHits = if (isHit) current.myHitsReceived + cellIndex else current.myHitsReceived
            val updatedMyMisses = if (isHit) current.myMissesReceived else current.myMissesReceived + cellIndex
            val opponentWon = current.myShips.all { it in updatedMyHits }

            current.copy(
                myHitsReceived = updatedMyHits,
                myMissesReceived = updatedMyMisses,
                isMyTurn = !opponentWon,
                winnerRole = if (opponentWon) shooterRole else null,
                statusMessage = when {
                    opponentWon -> "Defeat! Enemy sank your final vessel."
                    isHit -> "Enemy hit at ${coordLabel(cellIndex)}! Your turn."
                    else -> "Enemy missed at ${coordLabel(cellIndex)}. Your turn!"
                },
            )
        }
    }

    private fun executeAiTurn(stateAfterPlayerShot: BattleshipGameState, playerHit: Boolean): BattleshipGameState {
        val aiShot = selectAiShot(
            myHitsReceived = stateAfterPlayerShot.myHitsReceived,
            myMissesReceived = stateAfterPlayerShot.myMissesReceived,
        ) ?: return stateAfterPlayerShot.copy(isMyTurn = true)

        val aiHit = aiShot in stateAfterPlayerShot.myShips
        val updatedMyHits = if (aiHit) {
            stateAfterPlayerShot.myHitsReceived + aiShot
        } else {
            stateAfterPlayerShot.myHitsReceived
        }
        val updatedMyMisses = if (aiHit) {
            stateAfterPlayerShot.myMissesReceived
        } else {
            stateAfterPlayerShot.myMissesReceived + aiShot
        }
        val aiWon = stateAfterPlayerShot.myShips.all { it in updatedMyHits }
        val aiRole = if (localRole == PlayerRole.OPERATOR) PlayerRole.ANALYST else PlayerRole.OPERATOR

        val status = when {
            aiWon -> "Defeat! AI sank your fleet at ${coordLabel(aiShot)}."
            playerHit && aiHit -> "Trade of fire! You hit, AI hit at ${coordLabel(aiShot)}."
            playerHit && !aiHit -> "Direct hit! AI missed at ${coordLabel(aiShot)}. Your turn."
            !playerHit && aiHit -> "Missed! AI scored a hit at ${coordLabel(aiShot)}."
            else -> "Both shots missed (AI fired ${coordLabel(aiShot)}). Your turn."
        }

        return stateAfterPlayerShot.copy(
            myHitsReceived = updatedMyHits,
            myMissesReceived = updatedMyMisses,
            isMyTurn = !aiWon,
            winnerRole = if (aiWon) aiRole else null,
            statusMessage = status,
        )
    }

    private fun selectAiShot(
        myHitsReceived: Set<Int>,
        myMissesReceived: Set<Int>,
    ): Int? {
        val alreadyShot = myHitsReceived + myMissesReceived
        val unshotCells = (0 until GRID_SIZE).filter { it !in alreadyShot }
        if (unshotCells.isEmpty()) return null

        // Target mode: prioritize orthogonal neighbors of hits belonging to unsunk ships.
        val unsunkShips = myFleet.filter { ship -> !ship.all { it in myHitsReceived } }
        val unsunkHits = unsunkShips.flatMap { ship -> ship.filter { it in myHitsReceived } }

        if (unsunkHits.isNotEmpty()) {
            val alignedTargets = findAlignedExtensionTargets(unsunkShips, myHitsReceived, alreadyShot)
            if (alignedTargets.isNotEmpty()) {
                return alignedTargets[aiRng.nextInt(alignedTargets.size)]
            }

            val orthogonalTargets = unsunkHits
                .flatMap { orthogonalNeighbors(it) }
                .distinct()
                .filter { it !in alreadyShot }
            if (orthogonalTargets.isNotEmpty()) {
                return orthogonalTargets[aiRng.nextInt(orthogonalTargets.size)]
            }
        }

        // Hunt mode: favor checkerboard parity cells because every ship has length >= 2.
        val parityCells = unshotCells.filter { cell ->
            val r = cell / GRID_SIDE
            val c = cell % GRID_SIDE
            (r + c) % 2 == 0
        }
        val pool = parityCells.ifEmpty { unshotCells }
        return pool[aiRng.nextInt(pool.size)]
    }

    private fun findAlignedExtensionTargets(
        unsunkShips: List<Set<Int>>,
        hits: Set<Int>,
        alreadyShot: Set<Int>,
    ): List<Int> {
        val candidates = mutableListOf<Int>()
        for (ship in unsunkShips) {
            val shipHits = ship.filter { it in hits }
            if (shipHits.size < 2) continue

            val rows = shipHits.map { it / GRID_SIDE }.distinct()
            val cols = shipHits.map { it % GRID_SIDE }.distinct()

            if (rows.size == 1) {
                val r = rows.first()
                val minCol = cols.min()
                val maxCol = cols.max()
                if (minCol - 1 >= 0) {
                    val left = r * GRID_SIDE + (minCol - 1)
                    if (left !in alreadyShot) candidates.add(left)
                }
                if (maxCol + 1 < GRID_SIDE) {
                    val right = r * GRID_SIDE + (maxCol + 1)
                    if (right !in alreadyShot) candidates.add(right)
                }
            } else if (cols.size == 1) {
                val c = cols.first()
                val minRow = rows.min()
                val maxRow = rows.max()
                if (minRow - 1 >= 0) {
                    val up = (minRow - 1) * GRID_SIDE + c
                    if (up !in alreadyShot) candidates.add(up)
                }
                if (maxRow + 1 < GRID_SIDE) {
                    val down = (maxRow + 1) * GRID_SIDE + c
                    if (down !in alreadyShot) candidates.add(down)
                }
            }
        }
        return candidates.distinct()
    }

    private fun resetWithSeed(newSeed: UInt) {
        currentSeed = newSeed
        aiRng = DeterministicRng(newSeed xor AI_SEED_MASK)
        _state.value = buildInitialState(newSeed)
    }

    private fun buildInitialState(seedToUse: UInt): BattleshipGameState {
        val operatorFleet = placeFleet(seedToUse xor OPERATOR_SEED_MASK)
        val analystFleet = placeFleet(seedToUse xor ANALYST_SEED_MASK)

        val (ownFleet, opponentFleet) = if (localRole == PlayerRole.OPERATOR) {
            operatorFleet to analystFleet
        } else {
            analystFleet to operatorFleet
        }

        myFleet = ownFleet.ships
        enemyFleet = opponentFleet.ships

        val initialTurn = if (vsAi) true else localRole == PlayerRole.OPERATOR
        val initialMessage = when {
            vsAi -> "Solo Tactical Command: fire on the enemy 6x6 grid."
            initialTurn -> "Your turn (Operator). Fire or deploy Sonar."
            else -> "Waiting for Operator's opening strike."
        }

        return BattleshipGameState(
            localRole = localRole,
            vsAi = vsAi,
            myShips = ownFleet.allCells,
            enemyShips = opponentFleet.allCells,
            myHitsReceived = emptySet(),
            myMissesReceived = emptySet(),
            enemyHits = emptySet(),
            enemyMisses = emptySet(),
            sonarRevealedShips = emptySet(),
            sonarScannedEmpty = emptySet(),
            sonarChargesLeft = MAX_SONAR_CHARGES,
            isMyTurn = initialTurn,
            winnerRole = null,
            statusMessage = initialMessage,
        )
    }

    companion object {
        const val GRID_SIDE = 6
        const val GRID_SIZE = GRID_SIDE * GRID_SIDE
        const val MAX_SONAR_CHARGES = 2
        val SHIP_LENGTHS: List<Int> = listOf(3, 2, 2)

        private const val OPERATOR_SEED_MASK: UInt = 0xA5A55A5Au
        private const val ANALYST_SEED_MASK: UInt = 0x3C3CC3C3u
        private const val AI_SEED_MASK: UInt = 0x7F4A7C15u
        private const val RESTART_SEED_STEP: UInt = 0x9E3779B9u

        private fun placeFleet(fleetSeed: UInt): FleetLayout {
            val rng = DeterministicRng(fleetSeed)
            val occupied = mutableSetOf<Int>()
            val placedShips = mutableListOf<Set<Int>>()

            for (length in SHIP_LENGTHS) {
                val candidates = mutableListOf<Set<Int>>()

                // Horizontal placements
                for (row in 0 until GRID_SIDE) {
                    for (col in 0..(GRID_SIDE - length)) {
                        val cells = (0 until length).map { offset -> row * GRID_SIDE + (col + offset) }.toSet()
                        if (cells.none { it in occupied }) {
                            candidates.add(cells)
                        }
                    }
                }

                // Vertical placements
                for (row in 0..(GRID_SIDE - length)) {
                    for (col in 0 until GRID_SIDE) {
                        val cells = (0 until length).map { offset -> (row + offset) * GRID_SIDE + col }.toSet()
                        if (cells.none { it in occupied }) {
                            candidates.add(cells)
                        }
                    }
                }

                check(candidates.isNotEmpty()) { "No valid placement left for ship length $length" }
                val chosen = candidates[rng.nextInt(candidates.size)]
                placedShips.add(chosen)
                occupied.addAll(chosen)
            }

            return FleetLayout(
                ships = placedShips.toList(),
                allCells = occupied.toSet(),
            )
        }

        private fun computeSonarSector(cellIndex: Int): Set<Int> {
            val topRow = (cellIndex / GRID_SIDE).coerceAtMost(GRID_SIDE - 2)
            val leftCol = (cellIndex % GRID_SIDE).coerceAtMost(GRID_SIDE - 2)
            return setOf(
                topRow * GRID_SIDE + leftCol,
                topRow * GRID_SIDE + (leftCol + 1),
                (topRow + 1) * GRID_SIDE + leftCol,
                (topRow + 1) * GRID_SIDE + (leftCol + 1),
            )
        }

        private fun orthogonalNeighbors(cellIndex: Int): List<Int> {
            val row = cellIndex / GRID_SIDE
            val col = cellIndex % GRID_SIDE
            return buildList(4) {
                if (row > 0) add((row - 1) * GRID_SIDE + col)
                if (row < GRID_SIDE - 1) add((row + 1) * GRID_SIDE + col)
                if (col > 0) add(row * GRID_SIDE + (col - 1))
                if (col < GRID_SIDE - 1) add(row * GRID_SIDE + (col + 1))
            }
        }

        private fun coordLabel(cellIndex: Int): String {
            val row = cellIndex / GRID_SIDE
            val col = cellIndex % GRID_SIDE
            val rowChar = ('A'.code + row).toChar()
            return "$rowChar${col + 1}"
        }
    }
}
