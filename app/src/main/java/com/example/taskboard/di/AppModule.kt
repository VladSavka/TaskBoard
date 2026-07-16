package com.example.taskboard.di

import com.example.taskboard.data.SampleTasks
import com.example.taskboard.data.datasource.InMemoryTaskDataSource
import com.example.taskboard.data.datasource.LocalTaskDataSource
import com.example.taskboard.data.datasource.MockNetworkTaskDataSource
import com.example.taskboard.data.datasource.RandomNetworkFailurePolicy
import com.example.taskboard.data.datasource.RandomNetworkLatency
import com.example.taskboard.data.datasource.RemoteTaskDataSource
import com.example.taskboard.data.repository.DefaultTaskRepository
import com.example.taskboard.domain.repository.TaskRepository
import com.example.taskboard.domain.usecase.IdGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.util.UUID
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Hilt composition root. Provides the remote (mock network, seeded with the
 * [SampleTasks] default seed) and local (in-memory cache) data sources, the
 * repository, and use cases. ViewModels receive these via constructor injection
 * (Hilt also supplies each ViewModel's SavedStateHandle).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** The remote backend: a mock network seeded with the default sample tasks. */
    @Provides
    @Singleton
    fun provideRemoteTaskDataSource(): RemoteTaskDataSource = MockNetworkTaskDataSource(
        initial = SampleTasks.DEFAULT,
        failurePolicy = RandomNetworkFailurePolicy(Random.Default),
        latency = RandomNetworkLatency(Random.Default),
    )

    /** The local cache: in-memory for the session. */
    @Provides
    @Singleton
    fun provideLocalTaskDataSource(): LocalTaskDataSource = InMemoryTaskDataSource()

    @Provides
    @Singleton
    fun provideTaskRepository(
        remote: RemoteTaskDataSource,
        local: LocalTaskDataSource,
    ): TaskRepository = DefaultTaskRepository(remote, local)

    /**
     * New-task id source for [com.example.taskboard.domain.usecase.AddTaskUseCase].
     * The use cases (and their grouping [com.example.taskboard.presentation.tasklist.TaskUseCases])
     * are otherwise assembled by Hilt via constructor injection — no factory needed here.
     */
    @Provides
    fun provideIdGenerator(): IdGenerator = IdGenerator { UUID.randomUUID().toString() }

    /** The reference clock for due-date labels; the system zone in production. */
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
