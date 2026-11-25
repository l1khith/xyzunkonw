package com.splitsignal.core.domain

/**
 * Deterministic 32-bit SplitMix PRNG using unsigned 32-bit arithmetic.
 * Multiplatform-safe with identical output across all Kotlin targets.
 */
class DeterministicRng(seed: UInt) {
    private var state: UInt = seed

    /**
     * Advances the generator and returns the next pseudo-random [UInt].
     */
    fun nextUInt(): UInt {
        state += 0x9E3779B9u
        var z = state
        z = (z xor (z shr 16)) * 0x85EBCA6Bu
        z = (z xor (z shr 13)) * 0xC2B2AE35u
        return z xor (z shr 16)
    }

    /**
     * Returns a uniformly distributed [Int] in `0 until bound`.
     */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive: $bound" }
        val uBound = bound.toUInt()
        val threshold = (0u - uBound) % uBound
        while (true) {
            val r = nextUInt()
            if (r >= threshold) {
                return (r % uBound).toInt()
            }
        }
    }

    /**
     * Returns a uniformly distributed [Float] in `[0f, 1f)`.
     */
    fun nextFloat(): Float {
        return (nextUInt() shr 8).toFloat() / 16777216f
    }

    /**
     * Returns a uniformly distributed [Float] in `[min, max)`.
     */
    fun nextFloatRange(min: Float, max: Float): Float {
        require(max >= min) { "max ($max) must be >= min ($min)" }
        return min + nextFloat() * (max - min)
    }

    /**
     * Returns a uniformly distributed [Int] in `from until until`.
     */
    fun nextInt(from: Int, until: Int): Int {
        require(until > from) { "until ($until) must be > from ($from)" }
        return from + nextInt(until - from)
    }

    /**
     * Deterministically shuffles [list] in-place using Fisher-Yates.
     */
    fun <T> shuffle(list: MutableList<T>) {
        for (i in list.lastIndex downTo 1) {
            val j = nextInt(i + 1)
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
    }

    /**
     * Returns a pseudo-random [Boolean].
     */
    fun nextBoolean(): Boolean {
        return (nextUInt() shr 31) != 0u
    }
}

/**
 * 32-bit FNV-1a non-cryptographic hash over UTF-8 bytes.
 */
object Fnv1a {
    private const val FNV_OFFSET_BASIS: UInt = 0x811C9DC5u
    private const val FNV_PRIME: UInt = 0x01000193u

    /**
     * Computes the deterministic 32-bit FNV-1a hash of [input].
     */
    fun hash32(input: String): UInt {
        var hash = FNV_OFFSET_BASIS
        for (byte in input.encodeToByteArray()) {
            hash = hash xor byte.toUByte().toUInt()
            hash *= FNV_PRIME
        }
        return hash
    }
}

/**
 * Generates and parses 4-character unambiguous room codes.
 */
object RoomCodeGenerator {
    const val ALPHABET: String = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    /**
     * Deterministically generates a 4-character uppercase room code from [seed].
     */
    fun fromSeed(seed: UInt): String {
        val rng = DeterministicRng(seed)
        return buildString(4) {
            repeat(4) {
                append(ALPHABET[rng.nextInt(ALPHABET.length)])
            }
        }
    }

    /**
     * Maps a room code to a deterministic 32-bit seed via [Fnv1a.hash32].
     */
    fun toSeed(roomCode: String): UInt {
        return Fnv1a.hash32(roomCode.trim().uppercase())
    }
}
