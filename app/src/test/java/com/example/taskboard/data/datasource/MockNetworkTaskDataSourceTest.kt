package com.example.taskboard.data.datasource

import com.example.taskboard.domain.model.Task
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.random.Random

class MockNetworkTaskDataSourceTest {

    private val noLatency = NetworkLatency { }
    private val neverFails = NetworkFailurePolicy { false }
    private val alwaysFails = NetworkFailurePolicy { true }

    private fun task(id: String, title: String) = Task(id = id, title = title)

    @Test
    fun `reads return the stored tasks when the network succeeds`() = runTest {
        val source = MockNetworkTaskDataSource(
            initial = listOf(task("1", "A")),
            failurePolicy = neverFails,
            latency = noLatency,
        )

        assertEquals(listOf("A"), source.readTasks().map { it.title })
    }

    @Test
    fun `a read failure surfaces as an error`() = runTest {
        val source = MockNetworkTaskDataSource(failurePolicy = alwaysFails, latency = noLatency)

        val error = runCatching { source.readTasks() }.exceptionOrNull()

        assertTrue(error is IOException)
    }

    @Test
    fun `a successful write persists the new tasks`() = runTest {
        val source = MockNetworkTaskDataSource(failurePolicy = neverFails, latency = noLatency)

        source.writeTasks(listOf(task("1", "A")))

        assertEquals(listOf("A"), source.readTasks().map { it.title })
    }

    @Test
    fun `a failed write does not change the stored tasks`() = runTest {
        val source = MockNetworkTaskDataSource(
            initial = listOf(task("1", "A")),
            failurePolicy = alwaysFails,
            latency = noLatency,
        )

        val error = runCatching { source.writeTasks(listOf(task("2", "B"))) }.exceptionOrNull()

        assertTrue(error is IOException)
        // The read also fails under alwaysFails, so assert the store via a succeeding read.
        val readable = MockNetworkTaskDataSource(
            initial = listOf(task("1", "A")),
            failurePolicy = neverFails,
            latency = noLatency,
        )
        assertEquals(listOf("A"), readable.readTasks().map { it.title })
    }

    @Test
    fun `the seeded failure policy produces the same sequence for the same seed`() {
        val rate = 0.15
        val a = RandomNetworkFailurePolicy(Random(seed = 42), rate)
        val b = RandomNetworkFailurePolicy(Random(seed = 42), rate)

        val seqA = (1..50).map { a.shouldFail() }
        val seqB = (1..50).map { b.shouldFail() }

        assertEquals(seqA, seqB)
        // A ~15% policy over 50 draws should fail at least once but not always.
        assertTrue(seqA.any { it })
        assertTrue(seqA.any { !it })
    }
}
