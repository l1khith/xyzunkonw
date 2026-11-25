package com.splitsignal.core.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransportParityTest {

    private fun runSync(block: suspend () -> Unit) {
        var failure: Throwable? = null
        var completed = false
        block.startCoroutine(
            object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) {
                    completed = true
                    failure = result.exceptionOrNull()
                }
            },
        )
        check(completed) { "Coroutine suspended unexpectedly in synchronous test" }
        failure?.let { throw it }
    }

    @Test
    fun inMemorySyncPipeRoutesPacketsBidirectionallyAndClosesCleanly() {
        val pipe = InMemorySyncPipe()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val clientReceived = mutableListOf<SyncPacket>()
        val hostReceived = mutableListOf<SyncPacket>()

        val job1 = scope.launch {
            pipe.client.incomingPackets.collect { clientReceived += it }
        }
        val job2 = scope.launch {
            pipe.host.incomingPackets.collect { hostReceived += it }
        }

        val handshake = SyncPacket.Handshake(roomCode = "KMP7", role = PlayerRole.OPERATOR)
        val dial = SyncPacket.DialUpdate(frequency = 3.25f, phase = 1.1f)

        runSync {
            pipe.client.send(handshake)
            pipe.host.send(dial)
        }

        assertEquals(listOf<SyncPacket>(handshake), hostReceived)
        assertEquals(listOf<SyncPacket>(dial), clientReceived)
        assertEquals(TransportConnectionState.CONNECTED, pipe.host.connectionState.value)
        assertEquals(TransportConnectionState.CONNECTED, pipe.client.connectionState.value)

        pipe.client.close()
        assertEquals(TransportConnectionState.DISCONNECTED, pipe.host.connectionState.value)
        assertEquals(TransportConnectionState.DISCONNECTED, pipe.client.connectionState.value)

        job1.cancel()
        job2.cancel()
        scope.cancel()
    }

    @Test
    fun reducePacketProducesIdenticalStateForHostAndClientOverPipe() {
        val (hostTransport, clientTransport) = InMemorySyncPipe.createPair()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

        var hostState = ReactorEngine.reducePacket(
            GameState(stage = ProceduralStageFactory.createStage("CAL0", 0)),
            SyncPacket.RestartRun(roomCode = "ROOM", untimedMode = false),
        )
        var clientState = ReactorEngine.reducePacket(
            GameState(stage = ProceduralStageFactory.createStage("CAL0", 0)),
            SyncPacket.RestartRun(roomCode = "ROOM", untimedMode = false),
        )

        val hostCollector = scope.launch {
            hostTransport.incomingPackets.collect { packet ->
                hostState = ReactorEngine.reducePacket(hostState, packet)
            }
        }
        val clientCollector = scope.launch {
            clientTransport.incomingPackets.collect { packet ->
                clientState = ReactorEngine.reducePacket(clientState, packet)
            }
        }

        val packets = listOf(
            SyncPacket.Handshake(roomCode = "ROOM", role = PlayerRole.OPERATOR),
            SyncPacket.AdvanceStage(nextStageNumber = 1),
            SyncPacket.DialUpdate(frequency = 2.85f, phase = 2.2f),
            SyncPacket.SwitchToggle(index = 0, enabled = true),
            SyncPacket.SwitchToggle(index = 3, enabled = true),
            SyncPacket.AdvanceStage(nextStageNumber = 2),
            SyncPacket.RestartRun(roomCode = "NEW9", untimedMode = true),
        )

        runSync {
            for (packet in packets) {
                hostState = ReactorEngine.reducePacket(hostState, packet)
                hostTransport.send(packet)
            }
        }

        assertEquals(hostState, clientState)
        assertEquals("NEW9", clientState.roomCode)
        assertEquals(0, clientState.stage.stageNumber)
        assertEquals(GamePhase.CALIBRATION, clientState.phase)
        assertTrue(clientState.untimedMode)
        assertTrue(clientState.peerConnected)

        hostCollector.cancel()
        clientCollector.cancel()
        hostTransport.close()
        scope.cancel()
    }

    @Test
    fun stateSyncPacketRoundTripsThroughJsonAndMatchesHostClientEngines() {
        val pipe = InMemorySyncPipe()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var clientState = GameState(stage = ProceduralStageFactory.createStage("CAL0", 0))

        val collector = scope.launch {
            pipe.client.incomingPackets.collect { packet ->
                clientState = ReactorEngine.reducePacket(clientState, packet)
            }
        }

        var hostState = ReactorEngine.reducePacket(
            clientState,
            SyncPacket.RestartRun(roomCode = "Z9X2", untimedMode = false),
        )
        hostState = ReactorEngine.reducePacket(hostState, SyncPacket.AdvanceStage(1))
        hostState = ReactorEngine.reducePacket(hostState, SyncPacket.DialUpdate(3.4f, 2.5f))
        hostState = ReactorEngine.reducePacket(hostState, SyncPacket.SwitchToggle(2, true))

        val json = Json { encodeDefaults = true }
        val encoded = json.encodeToString<SyncPacket>(SyncPacket.StateSync(hostState))
        val decoded = json.decodeFromString<SyncPacket>(encoded)

        runSync {
            pipe.host.send(decoded)
        }

        assertEquals(hostState, clientState)
        assertEquals(1, clientState.stage.stageNumber)
        assertEquals(3.4f, clientState.operatorInput.frequency)
        assertTrue(clientState.operatorInput.switches[2])

        collector.cancel()
        pipe.host.close()
        scope.cancel()
    }

    @Test
    fun telemetryLocksWhenDialAndSwitchesMatchIncludingInvertedFault() {
        val stage0 = ProceduralStageFactory.createStage("CAL0", 0)
        val matchingSwitches0 = List(6) { idx -> ((stage0.targetSwitchMask shr idx) and 1) == 1 }
        val input0 = OperatorInput(
            frequency = stage0.targetFrequency,
            phase = stage0.targetPhase,
            switches = matchingSwitches0,
        )
        val snap0 = ReactorEngine.computeTelemetry(
            stage = stage0,
            input = input0,
            tickCount = 0L,
            previousStability = 0.8f,
            dtSeconds = 0.5f,
        )
        assertTrue(snap0.isHarmonicLocked)
        assertTrue(snap0.isConduitLocked)
        assertEquals(12f, snap0.currentVoltage)
        assertEquals(12f, snap0.targetVoltage)
        assertEquals(1.0f, snap0.lockStabilityProgress)

        val stage1 = ProceduralStageFactory.createStage("TEST", 1)
        val matchingSwitches1 = List(6) { idx ->
            val targetBit = ((stage1.targetSwitchMask shr idx) and 1) == 1
            if (idx == stage1.invertedSwitchIndex) !targetBit else targetBit
        }
        val wrongSwitches1 = List(6) { idx -> ((stage1.targetSwitchMask shr idx) and 1) == 1 }

        val lockedSnap1 = ReactorEngine.computeTelemetry(
            stage = stage1,
            input = OperatorInput(
                frequency = stage1.targetFrequency,
                phase = stage1.targetPhase,
                switches = matchingSwitches1,
            ),
            tickCount = 0L,
            previousStability = 0f,
            dtSeconds = 0f,
        )
        assertTrue(lockedSnap1.isConduitLocked)
        assertEquals(lockedSnap1.targetVoltage, lockedSnap1.currentVoltage)
        assertTrue(lockedSnap1.faultHintLabel.contains("SW-${stage1.invertedSwitchIndex + 1} INVERTED"))

        val unlockedSnap1 = ReactorEngine.computeTelemetry(
            stage = stage1,
            input = OperatorInput(
                frequency = stage1.targetFrequency,
                phase = stage1.targetPhase,
                switches = wrongSwitches1,
            ),
            tickCount = 0L,
            previousStability = 0.5f,
            dtSeconds = 0.25f,
        )
        assertFalse(unlockedSnap1.isConduitLocked)
        assertEquals(0.3f, unlockedSnap1.lockStabilityProgress, 0.0001f)
    }

    @Test
    fun uiStateProjectionsMapGameStateFieldsAccurately() {
        val stage = ProceduralStageFactory.createStage("PROJ", 1)
        val switches = listOf(true, false, true, false, false, true)
        val input = OperatorInput(frequency = 3.5f, phase = 1.25f, switches = switches)
        val telemetry = ReactorEngine.computeTelemetry(
            stage = stage,
            input = input,
            tickCount = 10L,
            previousStability = 0.4f,
            dtSeconds = 0f,
        )
        val state = GameState(
            roomCode = "PROJ",
            stage = stage,
            phase = GamePhase.ACTIVE,
            operatorInput = input,
            telemetry = telemetry,
            tickCount = 10L,
        )

        val harmonic = state.toHarmonicUiState()
        assertEquals(telemetry.effectiveTargetFreq, harmonic.effectiveTargetFreq)
        assertEquals(telemetry.effectiveTargetPhase, harmonic.effectiveTargetPhase)
        assertEquals(input.frequency, harmonic.operatorFreq)
        assertEquals(input.phase, harmonic.operatorPhase)
        assertEquals(telemetry.harmonicLockRatio, harmonic.harmonicLockRatio)
        assertEquals(telemetry.isHarmonicLocked, harmonic.isHarmonicLocked)
        assertEquals(stage.driftRate, harmonic.driftRate)
        assertEquals(GamePhase.ACTIVE, harmonic.phase)

        val conduit = state.toConduitUiState()
        assertEquals(telemetry.currentVoltage, conduit.currentVoltage)
        assertEquals(telemetry.targetVoltage, conduit.targetVoltage)
        assertEquals(telemetry.isConduitLocked, conduit.isConduitLocked)
        assertEquals(telemetry.isHarmonicLocked, conduit.isHarmonicLocked)
        assertEquals(telemetry.lockStabilityProgress, conduit.lockStabilityProgress)
        assertEquals(telemetry.activeRuleLabel, conduit.activeRuleLabel)
        assertEquals(telemetry.faultHintLabel, conduit.faultHintLabel)
        assertEquals(telemetry.switchWeights, conduit.switchWeights)
        assertEquals(switches, conduit.switches)
        assertEquals(stage.invertedSwitchIndex, conduit.invertedSwitchIndex)
        assertEquals(GamePhase.ACTIVE, conduit.phase)
    }
}
