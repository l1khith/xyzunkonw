package com.splitsignal.core.domain

import kotlinx.serialization.Serializable

/**
 * Role assigned to a player in SplitSignal.
 */
@Serializable
enum class PlayerRole {
    ANALYST,
    OPERATOR,
}

/**
 * Connection and session topology for a run.
 */
@Serializable
enum class GameMode {
    SOLO_SPLIT,
    LAN_HOST,
    LAN_CLIENT,
}

/**
 * Lifecycle phase of the current reactor stage.
 */
@Serializable
enum class GamePhase {
    CALIBRATION,
    ACTIVE,
    STAGE_CLEAR,
    DETONATED,
}

/**
 * Deterministic parameters defining a single reactor stage.
 */
@Serializable
data class StageConfig(
    val stageNumber: Int,
    val seed: UInt,
    val targetFrequency: Float,
    val targetPhase: Float,
    val driftRate: Float,
    val operatorCoupling: Float,
    val targetSwitchMask: Int,
    val invertedSwitchIndex: Int,
    val polarityFlipTickInterval: Int,
    val timeLimitSeconds: Int,
)

/**
 * Live control surface state manipulated by the Operator.
 */
@Serializable
data class OperatorInput(
    val frequency: Float = 2.0f,
    val phase: Float = 0.0f,
    val switches: List<Boolean> = List(6) { false },
)

/**
 * Computed telemetry snapshot presented to the Analyst and used for lock evaluation.
 */
@Serializable
data class TelemetrySnapshot(
    val effectiveTargetFreq: Float = 3.0f,
    val effectiveTargetPhase: Float = 1.57f,
    val harmonicLockRatio: Float = 0f,
    val isHarmonicLocked: Boolean = false,
    val currentVoltage: Float = 0f,
    val targetVoltage: Float = 12f,
    val isConduitLocked: Boolean = false,
    val activeRuleLabel: String = "",
    val faultHintLabel: String = "",
    val switchWeights: List<Float> = listOf(4f, 4f, 4f, 4f, 4f, 4f),
    val lockStabilityProgress: Float = 0f,
)

/**
 * Complete synchronized game state for a SplitSignal session.
 */
@Serializable
data class GameState(
    val roomCode: String = "CAL0",
    val stage: StageConfig,
    val phase: GamePhase = GamePhase.CALIBRATION,
    val operatorInput: OperatorInput = OperatorInput(),
    val telemetry: TelemetrySnapshot = TelemetrySnapshot(),
    val tickCount: Long = 0L,
    val remainingSeconds: Int = 90,
    val untimedMode: Boolean = true,
    val stagesCleared: Int = 0,
    val peerConnected: Boolean = false,
)

/**
 * Narrow UI state projection for the Harmonic Phase Lock module.
 */
data class HarmonicUiState(
    val effectiveTargetFreq: Float,
    val effectiveTargetPhase: Float,
    val operatorFreq: Float,
    val operatorPhase: Float,
    val harmonicLockRatio: Float,
    val isHarmonicLocked: Boolean,
    val driftRate: Float,
    val phase: GamePhase,
)

/**
 * Narrow UI state projection for the Boolean Conduit module.
 */
data class ConduitUiState(
    val currentVoltage: Float,
    val targetVoltage: Float,
    val isConduitLocked: Boolean,
    val isHarmonicLocked: Boolean,
    val lockStabilityProgress: Float,
    val activeRuleLabel: String,
    val faultHintLabel: String,
    val switchWeights: List<Float>,
    val switches: List<Boolean>,
    val invertedSwitchIndex: Int,
    val phase: GamePhase,
)

/**
 * Projects [GameState] into [HarmonicUiState].
 */
fun GameState.toHarmonicUiState(): HarmonicUiState = HarmonicUiState(
    effectiveTargetFreq = telemetry.effectiveTargetFreq,
    effectiveTargetPhase = telemetry.effectiveTargetPhase,
    operatorFreq = operatorInput.frequency,
    operatorPhase = operatorInput.phase,
    harmonicLockRatio = telemetry.harmonicLockRatio,
    isHarmonicLocked = telemetry.isHarmonicLocked,
    driftRate = stage.driftRate,
    phase = phase,
)

/**
 * Projects [GameState] into [ConduitUiState].
 */
fun GameState.toConduitUiState(): ConduitUiState = ConduitUiState(
    currentVoltage = telemetry.currentVoltage,
    targetVoltage = telemetry.targetVoltage,
    isConduitLocked = telemetry.isConduitLocked,
    isHarmonicLocked = telemetry.isHarmonicLocked,
    lockStabilityProgress = telemetry.lockStabilityProgress,
    activeRuleLabel = telemetry.activeRuleLabel,
    faultHintLabel = telemetry.faultHintLabel,
    switchWeights = telemetry.switchWeights,
    switches = operatorInput.switches,
    invertedSwitchIndex = stage.invertedSwitchIndex,
    phase = phase,
)
