package com.splitsignal.core.domain

/**
 * Generates deterministic [StageConfig] instances from room codes and stage numbers.
 */
object ProceduralStageFactory {
    /**
     * Creates a [StageConfig] for [roomCode] and [stageNumber].
     * Stage 0 is a fixed calibration stage; stage 1+ scales deterministically.
     */
    fun createStage(roomCode: String, stageNumber: Int): StageConfig {
        require(stageNumber >= 0) { "stageNumber must be non-negative: $stageNumber" }
        val seed = Fnv1a.hash32("$roomCode:$stageNumber")

        if (stageNumber == 0) {
            return StageConfig(
                stageNumber = 0,
                seed = seed,
                targetFrequency = 3.0f,
                targetPhase = 1.57f,
                driftRate = 0f,
                operatorCoupling = 0f,
                targetSwitchMask = 0b101010,
                invertedSwitchIndex = -1,
                polarityFlipTickInterval = 0,
                timeLimitSeconds = 90,
            )
        }

        val rng = DeterministicRng(seed)
        val targetFrequency = rng.nextFloatRange(1.4f, 4.6f)
        val targetPhase = rng.nextFloatRange(0.4f, 5.8f)
        val driftRate = rng.nextFloatRange(0.15f, 0.45f)
        val operatorCoupling = rng.nextFloatRange(0.10f, 0.35f)
        val targetSwitchMask = 1 + rng.nextInt(62)
        val invertedSwitchIndex = rng.nextInt(6)
        val polarityFlipTickInterval = 80 + rng.nextInt(61)
        val timeLimitSeconds = (90 - (stageNumber - 1) * 5).coerceAtLeast(45)

        return StageConfig(
            stageNumber = stageNumber,
            seed = seed,
            targetFrequency = targetFrequency,
            targetPhase = targetPhase,
            driftRate = driftRate,
            operatorCoupling = operatorCoupling,
            targetSwitchMask = targetSwitchMask,
            invertedSwitchIndex = invertedSwitchIndex,
            polarityFlipTickInterval = polarityFlipTickInterval,
            timeLimitSeconds = timeLimitSeconds,
        )
    }
}
