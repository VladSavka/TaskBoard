package com.example.taskboard.data.datasource

import kotlin.random.Random

/** Decides whether a simulated network call fails. */
fun interface NetworkFailurePolicy {
    fun shouldFail(): Boolean
}

/**
 * A [NetworkFailurePolicy] that fails a [failureRate] fraction of calls using the
 * injected [random]. Seeding [random] makes the sequence deterministic under test.
 */
class RandomNetworkFailurePolicy(
    private val random: Random,
    private val failureRate: Double = DEFAULT_FAILURE_RATE,
) : NetworkFailurePolicy {
    override fun shouldFail(): Boolean = random.nextDouble() < failureRate

    companion object {
        /** The challenge's ~15% failure rate. */
        const val DEFAULT_FAILURE_RATE: Double = 0.15
    }
}
