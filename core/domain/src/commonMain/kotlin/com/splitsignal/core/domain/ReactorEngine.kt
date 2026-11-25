package com.splitsignal.core.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

/**
 * Authoritative reactor simulation and transport synchronization coordinator.
 */
class ReactorEngine(
    private val transport: SyncTransport,
    scope: CoroutineScope,
    private val isHost: Boolean = true,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val engineScope: CoroutineScope = scope + dispatcher

    private val _state: MutableStateFlow<GameState> = MutableStateFlow(createInitialState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val collectorJob: Job = engineScope.launch {
        transport.incomingPackets.collect { packet ->
            handleIncomingPacket(packet)
        }
    }

    private val tickJob: Job? = if (isHost) {
        engineScope.launch {
            var lastBroadcastState: GameState? = null
            while (currentCoroutineContext().isActive) {
                delay(TICK_INTERVAL_MS)
                val updated = stepHostTick()
                if (updated != lastBroadcastState || updated.tickCount % STATE_SYNC_TICK_CADENCE == 0L) {
                    lastBroadcastState = updated
                    if (transport.connectionState.value == TransportConnectionState.CONNECTED) {
                        transport.send(SyncPacket.StateSync(updated))
                    }
                }
            }
        }
    } else {
        null
    }

    /**
     * Starts a fresh run from stage 0 (calibration) with [roomCode] and [untimedMode].
     */
    fun startNewRun(roomCode: String, untimedMode: Boolean) {
        val packet = SyncPacket.RestartRun(roomCode = roomCode, untimedMode = untimedMode)
        if (isHost) {
            val updated = _state.updateAndGet { reducePacket(it, packet) }
            broadcastStateIfConnected(updated)
        } else {
            engineScope.launch { transport.send(packet) }
        }
    }

    /**
     * Updates the Operator's frequency and phase dials.
     */
    fun updateDials(frequency: Float, phase: Float) {
        val packet = SyncPacket.DialUpdate(frequency = frequency, phase = phase)
        if (isHost) {
            _state.update { reducePacket(it, packet) }
        } else {
            engineScope.launch { transport.send(packet) }
        }
    }

    /**
     * Toggles conduit switch at [index] (`0..5`).
     */
    fun toggleSwitch(index: Int) {
        if (index !in 0 until SWITCH_COUNT) return
        val nextEnabled = !_state.value.operatorInput.switches.getOrElse(index) { false }
        val packet = SyncPacket.SwitchToggle(index = index, enabled = nextEnabled)
        if (isHost) {
            _state.update { reducePacket(it, packet) }
        } else {
            engineScope.launch { transport.send(packet) }
        }
    }

    /**
     * Advances the session to the next procedural stage.
     */
    fun nextStage() {
        val nextStageNumber = _state.value.stage.stageNumber + 1
        val packet = SyncPacket.AdvanceStage(nextStageNumber = nextStageNumber)
        if (isHost) {
            val updated = _state.updateAndGet { reducePacket(it, packet) }
            broadcastStateIfConnected(updated)
        } else {
            engineScope.launch { transport.send(packet) }
        }
    }

    /**
     * Cancels active engine coroutines and closes the underlying [SyncTransport].
     */
    fun dispose() {
        tickJob?.cancel()
        collectorJob.cancel()
        transport.close()
    }

    private fun handleIncomingPacket(packet: SyncPacket) {
        if (isHost && packet is SyncPacket.StateSync) return
        _state.update { current -> reducePacket(current, packet) }
    }

    private fun stepHostTick(): GameState {
        return _state.updateAndGet { current ->
            if (current.phase != GamePhase.CALIBRATION && current.phase != GamePhase.ACTIVE) {
                return@updateAndGet current
            }

            val nextTick = current.tickCount + 1L
            val newTelemetry = computeTelemetry(
                stage = current.stage,
                input = current.operatorInput,
                tickCount = nextTick,
                previousStability = current.telemetry.lockStabilityProgress,
                dtSeconds = TICK_INTERVAL_SECONDS,
            )

            val nextRemainingSeconds = if (!current.untimedMode && nextTick % TICKS_PER_SECOND == 0L) {
                (current.remainingSeconds - 1).coerceAtLeast(0)
            } else {
                current.remainingSeconds
            }

            val (nextPhase, nextStagesCleared) = when {
                newTelemetry.lockStabilityProgress >= 1.0f -> {
                    GamePhase.STAGE_CLEAR to (current.stagesCleared + 1)
                }
                !current.untimedMode && nextRemainingSeconds <= 0 -> {
                    GamePhase.DETONATED to current.stagesCleared
                }
                else -> current.phase to current.stagesCleared
            }

            current.copy(
                phase = nextPhase,
                telemetry = newTelemetry,
                tickCount = nextTick,
                remainingSeconds = nextRemainingSeconds,
                stagesCleared = nextStagesCleared,
            )
        }
    }

    private fun broadcastStateIfConnected(state: GameState) {
        engineScope.launch {
            if (transport.connectionState.value == TransportConnectionState.CONNECTED) {
                transport.send(SyncPacket.StateSync(state))
            }
        }
    }

    companion object {
        private const val SWITCH_COUNT = 6
        private const val SWITCH_VOLTAGE_WEIGHT = 4.0f
        private const val TICK_INTERVAL_MS = 50L
        private const val TICK_INTERVAL_SECONDS = 0.05f
        private const val TICKS_PER_SECOND = 20L
        private const val STATE_SYNC_TICK_CADENCE = 4L
        private const val PI_F = PI.toFloat()
        private const val TWO_PI = (2.0 * PI).toFloat()

        private fun createInitialState(): GameState {
            val stage = ProceduralStageFactory.createStage("CAL0", 0)
            val input = OperatorInput()
            val telemetry = computeTelemetry(
                stage = stage,
                input = input,
                tickCount = 0L,
                previousStability = 0f,
                dtSeconds = 0f,
            )
            return GameState(
                roomCode = "CAL0",
                stage = stage,
                phase = GamePhase.CALIBRATION,
                operatorInput = input,
                telemetry = telemetry,
                tickCount = 0L,
                remainingSeconds = stage.timeLimitSeconds,
                untimedMode = true,
                stagesCleared = 0,
                peerConnected = false,
            )
        }

        /**
         * Pure state reducer applying a [SyncPacket] to [current] without side effects.
         */
        fun reducePacket(current: GameState, packet: SyncPacket): GameState {
            return when (packet) {
                is SyncPacket.Handshake -> {
                    current.copy(peerConnected = true)
                }

                is SyncPacket.DialUpdate -> {
                    if (current.phase != GamePhase.CALIBRATION && current.phase != GamePhase.ACTIVE) {
                        return current
                    }
                    val updatedInput = current.operatorInput.copy(
                        frequency = packet.frequency.coerceIn(1.0f, 5.0f),
                        phase = wrapAngle(packet.phase),
                    )
                    val updatedTelemetry = computeTelemetry(
                        stage = current.stage,
                        input = updatedInput,
                        tickCount = current.tickCount,
                        previousStability = current.telemetry.lockStabilityProgress,
                        dtSeconds = 0f,
                    )
                    current.copy(
                        operatorInput = updatedInput,
                        telemetry = updatedTelemetry,
                    )
                }

                is SyncPacket.SwitchToggle -> {
                    if (current.phase != GamePhase.CALIBRATION && current.phase != GamePhase.ACTIVE) {
                        return current
                    }
                    if (packet.index !in 0 until SWITCH_COUNT) {
                        return current
                    }
                    val updatedSwitches = List(SWITCH_COUNT) { idx ->
                        if (idx == packet.index) {
                            packet.enabled
                        } else {
                            current.operatorInput.switches.getOrElse(idx) { false }
                        }
                    }
                    val updatedInput = current.operatorInput.copy(switches = updatedSwitches)
                    val updatedTelemetry = computeTelemetry(
                        stage = current.stage,
                        input = updatedInput,
                        tickCount = current.tickCount,
                        previousStability = current.telemetry.lockStabilityProgress,
                        dtSeconds = 0f,
                    )
                    current.copy(
                        operatorInput = updatedInput,
                        telemetry = updatedTelemetry,
                    )
                }

                is SyncPacket.StateSync -> {
                    packet.state
                }

                is SyncPacket.AdvanceStage -> {
                    val targetNumber = packet.nextStageNumber.coerceAtLeast(0)
                    val nextStage = ProceduralStageFactory.createStage(current.roomCode, targetNumber)
                    val resetInput = OperatorInput()
                    val resetTelemetry = computeTelemetry(
                        stage = nextStage,
                        input = resetInput,
                        tickCount = 0L,
                        previousStability = 0f,
                        dtSeconds = 0f,
                    )
                    current.copy(
                        stage = nextStage,
                        phase = if (targetNumber == 0) GamePhase.CALIBRATION else GamePhase.ACTIVE,
                        operatorInput = resetInput,
                        telemetry = resetTelemetry,
                        tickCount = 0L,
                        remainingSeconds = nextStage.timeLimitSeconds,
                    )
                }

                is SyncPacket.RestartRun -> {
                    val stage0 = ProceduralStageFactory.createStage(packet.roomCode, 0)
                    val resetInput = OperatorInput()
                    val resetTelemetry = computeTelemetry(
                        stage = stage0,
                        input = resetInput,
                        tickCount = 0L,
                        previousStability = 0f,
                        dtSeconds = 0f,
                    )
                    current.copy(
                        roomCode = packet.roomCode,
                        stage = stage0,
                        phase = GamePhase.CALIBRATION,
                        operatorInput = resetInput,
                        telemetry = resetTelemetry,
                        tickCount = 0L,
                        remainingSeconds = stage0.timeLimitSeconds,
                        untimedMode = packet.untimedMode,
                        stagesCleared = 0,
                    )
                }

                is SyncPacket.TileIncremented,
                is SyncPacket.AdvanceLevel,
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
                -> current
            }
        }

        /**
         * Pure telemetry evaluator for a given [stage], [input], [tickCount], and time step [dtSeconds].
         */
        fun computeTelemetry(
            stage: StageConfig,
            input: OperatorInput,
            tickCount: Long,
            previousStability: Float,
            dtSeconds: Float,
        ): TelemetrySnapshot {
            val couplingOffset = if (stage.operatorCoupling > 0f) {
                (input.phase - 3.14f) * 0.08f * stage.operatorCoupling
            } else {
                0f
            }
            val effectiveTargetFreq = (
                stage.targetFrequency +
                    sin(tickCount * 0.05f * stage.driftRate) * 0.45f +
                    couplingOffset
                ).coerceIn(1.0f, 5.0f)

            val phaseDrift = if (stage.stageNumber > 0) {
                sin(tickCount * 0.03f * stage.driftRate) * 0.35f
            } else {
                0f
            }
            val effectiveTargetPhase = wrapAngle(stage.targetPhase + phaseDrift)

            val freqError = abs(input.frequency - effectiveTargetFreq) / 2.0f
            val phaseError = circularPhaseDiff(input.phase, effectiveTargetPhase) / PI_F
            val harmonicLockRatio = (1f - (freqError + phaseError) * 0.5f).coerceIn(0f, 1f)
            val isHarmonicLocked = harmonicLockRatio >= 0.90f

            var currentMask = 0
            var currentVoltage = 0f
            var targetVoltage = 0f

            for (i in 0 until SWITCH_COUNT) {
                val rawSwitch = input.switches.getOrElse(i) { false }
                val effectiveBit = if (stage.stageNumber > 0 && i == stage.invertedSwitchIndex) {
                    !rawSwitch
                } else {
                    rawSwitch
                }
                if (effectiveBit) {
                    currentMask = currentMask or (1 shl i)
                    currentVoltage += SWITCH_VOLTAGE_WEIGHT
                }
                if (((stage.targetSwitchMask shr i) and 1) == 1) {
                    targetVoltage += SWITCH_VOLTAGE_WEIGHT
                }
            }

            val isConduitLocked = currentMask == stage.targetSwitchMask

            val isPhaseB = stage.polarityFlipTickInterval > 0 &&
                ((tickCount / stage.polarityFlipTickInterval) % 2L == 1L)
            val activeRuleLabel = when {
                stage.stageNumber == 0 || stage.polarityFlipTickInterval <= 0 -> "PHASE A: STANDARD BUS"
                isPhaseB -> "PHASE B: ODD BUS MONITOR"
                else -> "PHASE A: STANDARD BUS"
            }
            val faultHintLabel = if (stage.stageNumber > 0 && stage.invertedSwitchIndex in 0 until SWITCH_COUNT) {
                "FAULT: SW-${stage.invertedSwitchIndex + 1} INVERTED"
            } else {
                "BUS NOMINAL"
            }

            val nextStability = if (dtSeconds > 0f) {
                if (isHarmonicLocked && isConduitLocked) {
                    previousStability + dtSeconds / 1.5f
                } else {
                    previousStability - dtSeconds * 0.8f
                }
            } else {
                previousStability
            }.coerceIn(0f, 1f)

            return TelemetrySnapshot(
                effectiveTargetFreq = effectiveTargetFreq,
                effectiveTargetPhase = effectiveTargetPhase,
                harmonicLockRatio = harmonicLockRatio,
                isHarmonicLocked = isHarmonicLocked,
                currentVoltage = currentVoltage,
                targetVoltage = targetVoltage,
                isConduitLocked = isConduitLocked,
                activeRuleLabel = activeRuleLabel,
                faultHintLabel = faultHintLabel,
                switchWeights = List(SWITCH_COUNT) { SWITCH_VOLTAGE_WEIGHT },
                lockStabilityProgress = nextStability,
            )
        }

        private fun wrapAngle(angle: Float): Float {
            var wrapped = angle % TWO_PI
            if (wrapped < 0f) wrapped += TWO_PI
            if (wrapped >= TWO_PI) wrapped = 0f
            return wrapped
        }

        private fun circularPhaseDiff(a: Float, b: Float): Float {
            val diff = abs(wrapAngle(a) - wrapAngle(b))
            return min(diff, TWO_PI - diff)
        }
    }
}
