package com.example.taskboard.data.datasource

import kotlinx.coroutines.delay
import kotlin.random.Random

/** Simulated network latency applied before each mock call. */
fun interface NetworkLatency {
    suspend fun await()
}

/**
 * A [NetworkLatency] that waits a random duration within [rangeMillis] using the
 * injected [random]. Tests inject a no-op latency so they neither wait nor depend
 * on wall-clock time.
 */
class RandomNetworkLatency(
    private val random: Random,
    private val rangeMillis: IntRange = DEFAULT_RANGE_MILLIS,
) : NetworkLatency {
    override suspend fun await() {
        delay(random.nextInt(rangeMillis.first, rangeMillis.last + 1).toLong())
    }

    companion object {
        /** The challenge's 300–800 ms artificial read/save delay. */
        val DEFAULT_RANGE_MILLIS: IntRange = 300..800
    }
}
