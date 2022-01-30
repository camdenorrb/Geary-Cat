package com.mineinabyss.geary.ecs.api.systems

import com.mineinabyss.geary.ecs.accessors.TargetScope
import com.mineinabyss.geary.ecs.accessors.building.get
import com.mineinabyss.geary.ecs.accessors.building.relation
import com.mineinabyss.geary.ecs.api.GearyType
import com.mineinabyss.geary.ecs.api.annotations.Handler
import com.mineinabyss.geary.ecs.api.engine.componentId
import com.mineinabyss.geary.ecs.api.engine.entity
import com.mineinabyss.geary.ecs.api.relations.RelationValueId
import com.mineinabyss.geary.ecs.engine.HOLDS_DATA
import com.mineinabyss.geary.ecs.engine.getArchetype
import com.mineinabyss.geary.ecs.query.contains
import com.mineinabyss.geary.helpers.GearyTest
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class QueryManagerTest : GearyTest() {
    @Nested
    inner class FamilyMatchingTest {
        val stringId = engine.async { componentId<String>() or HOLDS_DATA }
        val intId = engine.async { componentId<Int>() }

        val system = object : TickingSystem() {
            val TargetScope.string by get<String>()

            override suspend fun onStart() {
                has<Int>()
            }

            override suspend fun TargetScope.tick() {
                string shouldBe entity.get<String>()
                entity.has<Int>() shouldBe true
            }
        }

        val correctArchetype = engine.async { engine.rootArchetype + stringId.await() + intId.await() }

        init {
            runBlocking {
                queryManager.trackQuery(system)
            }
        }

        @Test
        fun `family type is correct`() = runTest {
            GearyType(system.family.components).getArchetype() shouldBe engine.rootArchetype + stringId.await()
        }

        @Test
        fun `archetypes have been matched correctly`() = runTest {
            system.matchedArchetypes shouldContain correctArchetype.await()
        }

        @Test
        fun `get entities matching family`() = runTest {
            val entity = entity {
                set("Test")
                add<Int>()
            }
            val entity2 = entity {
                set("Test")
                set(1)
            }
            queryManager.getEntitiesMatching(system.family).apply {
                shouldContainAll(entity, entity2)
            }
        }

        @Test
        fun `accessors in system correctly read data`() = runTest {
            system.doTick()
        }
    }

    @Nested
    inner class ConcurrentModificationTests {
        var ran = 0

        val removingSystem = object : TickingSystem() {
            val TargetScope.string by get<String>()

            override suspend fun TargetScope.tick() {
                entity.remove<String>()
                ran++
            }
        }

        init {
            runBlocking {
                queryManager.trackQuery(removingSystem)
            }
        }

        @Test
        fun `concurrent modification`() = runTest {
            val entities = (0 until 10).map { entity { set("Test") } }
            val total =
                queryManager.getEntitiesMatching(family {
                    hasSet<String>()
                }).count()
            removingSystem.doTick()
            ran shouldBe total
            entities.map { it.getComponents() } shouldContainExactly entities.map { setOf() }
        }
    }

    private class RelationTestComponent

    @Test
    fun relations() = runTest {
        var ran = 0
        val system = object : TickingSystem() {
            val TargetScope.test by relation<Any?, RelationTestComponent>()
            override suspend fun TargetScope.tick() {
                ran++
                family.relationValueTypes.map { it.id } shouldContain test.valueId
                test.value.shouldBeInstanceOf<RelationTestComponent>()
            }
        }
        queryManager.trackQuery(system)
        system.family.relationValueTypes.shouldContainExactly(RelationValueId(componentId<RelationTestComponent>()))
        val entity = entity {
            setRelation(String::class, RelationTestComponent())
            add<String>()
        }
        val entity2 = entity {
            setRelation(Int::class, RelationTestComponent())
            add<Int>()
        }
        val entity3 = entity {
            setRelation(RelationTestComponent::class, "")
            add<RelationTestComponent>()
        }
        system.matchedArchetypes.shouldContainAll(entity.type.getArchetype(), entity2.type.getArchetype())
        system.matchedArchetypes.shouldNotContain(entity3.type.getArchetype())

        system.doTick()
        ran shouldBe 2

    }

    private class RelationTestComponent1
    private class RelationTestComponent2

    @Test
    fun relationPermutations() = runTest {
        var ran = 0
        val system = object : TickingSystem() {
            val TargetScope.test1 by relation<Any?, RelationTestComponent1>()
            val TargetScope.test2 by relation<Any?, RelationTestComponent2>()
            override suspend fun TargetScope.tick() {
                ran++
                test1.value.shouldBeInstanceOf<RelationTestComponent1>()
                test2.value.shouldBeInstanceOf<RelationTestComponent2>()
            }
        }
        queryManager.trackQuery(system)

        entity {
            setRelation(String::class, RelationTestComponent1())
            setRelation(Int::class, RelationTestComponent1())
            setRelation(String::class, RelationTestComponent2())
            setRelation(Int::class, RelationTestComponent2())
            add<String>()
        }

        system.doTick()

        ran shouldBe 4
    }

    class RelationTestWithData

    @Test
    fun relationsWithData() = runTest {
        val system = object : TickingSystem() {
            val TargetScope.withData by relation<Any, RelationTestWithData>()

            override suspend fun TargetScope.tick() {
                withData.value.shouldBeInstanceOf<RelationTestWithData>()
                withData.key shouldBe "Test"
            }
        }

        val entity = entity {
            setRelation(String::class, RelationTestWithData())
            add<String>()
        }

        val entityWithData = entity {
            setRelation(String::class, RelationTestWithData())
            set("Test")
        }

        queryManager.trackQuery(system)

        system.matchedArchetypes.shouldNotContain(entity.type.getArchetype())
        system.matchedArchetypes.shouldContain(entityWithData.type.getArchetype())

        system.doTick()
    }

    private class TestComponent
    private object EventListener : GearyListener() {
        var ran = 0
        private val TargetScope.testComponent by get<TestComponent>()

        @Handler
        fun handle() {
            this@EventListener.ran++
        }
    }

    @Test
    fun `empty event handler`() = runTest {
        engine.addSystem(EventListener)
        (engine.rootArchetype.type in EventListener.event.family) shouldBe true
        engine.rootArchetype.eventHandlers.map { it.parentListener } shouldContain EventListener
        entity {
            set(TestComponent())
        }.callEvent()
        // 1 from setting, 1 from calling empty event
        EventListener.ran shouldBe 2
    }
}
