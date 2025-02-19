package com.mineinabyss.geary.helpers.tests

import com.mineinabyss.geary.context.EngineContext
import com.mineinabyss.geary.context.GearyContextKoin
import com.mineinabyss.geary.context.QueryContext
import com.mineinabyss.geary.context.globalContext
import com.mineinabyss.geary.datatypes.maps.HashTypeMap
import com.mineinabyss.geary.datatypes.maps.TypeMap
import com.mineinabyss.geary.engine.Components
import com.mineinabyss.geary.engine.Engine
import com.mineinabyss.geary.engine.EntityProvider
import com.mineinabyss.geary.engine.EventRunner
import com.mineinabyss.geary.engine.archetypes.*
import com.mineinabyss.geary.systems.QueryManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterAll
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.logger.Logger
import org.koin.core.logger.PrintLogger
import org.koin.dsl.module
import kotlin.time.Duration.Companion.milliseconds

abstract class GearyTest : KoinComponent, EngineContext {
    override val engine get() = globalContext.engine as ArchetypeEngine
    val queryManager get() = globalContext.queryManager

    init {
        clearEngine()
    }

    private fun startKoinWithGeary() {
        with(object : QueryContext {
            override val queryManager = QueryManager()
        }) {
            @Suppress("RemoveExplicitTypeArguments")
            startKoin {
                modules(module {
                    single<Logger> { PrintLogger() }
                    single { Components() }
                    single<QueryManager> { queryManager }
                    single<TypeMap> { HashTypeMap() }
                    single<EventRunner> { ArchetypeEventRunner() }
                    single<EntityProvider> { EntityByArchetypeProvider() }
                    single<ArchetypeProvider> { SimpleArchetypeProvider() }
                    single<Engine> { ArchetypeEngine(10.milliseconds) }
                })
            }
            globalContext = GearyContextKoin()
            engine.init()
            queryManager.init(engine)
        }
    }

    @AfterAll
    private fun stop() {
        stopKoin()
    }

    /** Recreates the engine. */
    fun clearEngine() {
        stopKoin()
        startKoinWithGeary()
    }

    suspend inline fun concurrentOperation(
        times: Int = 10000,
        crossinline run: suspend (id: Int) -> Unit
    ): List<Deferred<*>> {
        return withContext(Dispatchers.Default) {
            (0 until times).map { id ->
                async { run(id) }
            }
        }
    }
}
