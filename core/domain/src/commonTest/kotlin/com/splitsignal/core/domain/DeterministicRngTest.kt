package com.splitsignal.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DeterministicRngTest {

    @Test
    fun splitMix32MatchesGoldenValuesForSeedZero() {
        val rng = DeterministicRng(0u)
        assertEquals(0x92CA2F0Eu, rng.nextUInt())
        assertEquals(0x3CD6E3F3u, rng.nextUInt())
        assertEquals(0x1B147DCCu, rng.nextUInt())
    }

    @Test
    fun splitMix32BoundsAndRangesAreRespected() {
        val rng = DeterministicRng(123456u)
        assertFailsWith<IllegalArgumentException> {
            rng.nextInt(0)
        }
        assertFailsWith<IllegalArgumentException> {
            rng.nextInt(-5)
        }

        repeat(200) {
            val i = rng.nextInt(6)
            assertTrue(i in 0 until 6, "nextInt(6) out of range: $i")

            val f = rng.nextFloat()
            assertTrue(f >= 0f && f < 1f, "nextFloat() out of range: $f")

            val r = rng.nextFloatRange(1.4f, 4.6f)
            assertTrue(r >= 1.4f && r < 4.6f, "nextFloatRange out of range: $r")
        }
    }

    @Test
    fun fnv1aHash32MatchesGoldenVectors() {
        assertEquals(0x811C9DC5u, Fnv1a.hash32(""))
        assertEquals(0xE40C292Cu, Fnv1a.hash32("a"))
        assertEquals(0xBF9CF968u, Fnv1a.hash32("foobar"))
        assertEquals(Fnv1a.hash32("SIGNAL"), Fnv1a.hash32("SIGNAL"))
        assertNotEquals(Fnv1a.hash32("SIGNAL"), Fnv1a.hash32("SIGNAL2"))
    }

    @Test
    fun roomCodeGeneratorRoundTripAndDeterminism() {
        val codeA = RoomCodeGenerator.fromSeed(0x12345678u)
        val codeB = RoomCodeGenerator.fromSeed(0x12345678u)
        assertEquals(codeA, codeB)
        assertEquals(4, codeA.length)
        assertTrue(codeA.all { it in RoomCodeGenerator.ALPHABET })

        assertEquals(RoomCodeGenerator.toSeed("abcd"), RoomCodeGenerator.toSeed(" ABCD "))
        assertEquals(Fnv1a.hash32("ABCD"), RoomCodeGenerator.toSeed("abcd"))
    }

    @Test
    fun proceduralStageFactoryIsDeterministicAndInBounds() {
        val stage0 = ProceduralStageFactory.createStage("ABCD", 0)
        assertEquals(0, stage0.stageNumber)
        assertEquals(3.0f, stage0.targetFrequency)
        assertEquals(1.57f, stage0.targetPhase)
        assertEquals(0f, stage0.driftRate)
        assertEquals(0f, stage0.operatorCoupling)
        assertEquals(0b101010, stage0.targetSwitchMask)
        assertEquals(-1, stage0.invertedSwitchIndex)
        assertEquals(0, stage0.polarityFlipTickInterval)
        assertEquals(90, stage0.timeLimitSeconds)

        val stage1A = ProceduralStageFactory.createStage("ABCD", 1)
        val stage1B = ProceduralStageFactory.createStage("ABCD", 1)
        assertEquals(stage1A, stage1B)

        val stage2 = ProceduralStageFactory.createStage("ABCD", 2)
        assertNotEquals(stage1A.seed, stage2.seed)

        for (n in 1..15) {
            val s = ProceduralStageFactory.createStage("WXYZ", n)
            assertEquals(n, s.stageNumber)
            assertTrue(s.targetFrequency in 1.4f..4.6f)
            assertTrue(s.targetPhase in 0.4f..5.8f)
            assertTrue(s.driftRate in 0.15f..0.45f)
            assertTrue(s.operatorCoupling in 0.10f..0.35f)
            assertTrue(s.targetSwitchMask in 1..62)
            assertTrue(s.invertedSwitchIndex in 0..5)
            assertTrue(s.polarityFlipTickInterval in 80..140)
            assertTrue(s.timeLimitSeconds in 45..90)
        }
    }
}
