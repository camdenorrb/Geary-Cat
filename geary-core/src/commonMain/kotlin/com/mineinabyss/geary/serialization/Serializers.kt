package com.mineinabyss.geary.serialization

import com.mineinabyss.geary.datatypes.Component
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.overwriteWith
import kotlin.reflect.KClass

public class Serializers {
    private val addonToModuleMap = mutableMapOf<String, SerializersModule>()
    private val serialName2Component: MutableMap<String, KClass<out Component>> = mutableMapOf()
    private val component2serialName: MutableMap<KClass<out Component>, String> = mutableMapOf()
    public val module: SerializersModule by lazy {
        addonToModuleMap.values.fold(EmptySerializersModule()) { acc, module ->
            acc.overwriteWith(module)
        }
    }

    //TODO allow this to work for all registered classes, not just components
    public fun getClassFor(serialName: String): KClass<out Component> =
        serialName2Component[serialName]
            ?: error("$serialName is not a valid component name in the registered components")

    public fun isRegistered(serialName: String): Boolean =
        serialName in serialName2Component

    /**
     * Adds a class associated with a serial name. Currently haven't found an easy way to get this using serializer
     * modules, but if possible this will be removed.
     */
    public fun registerSerialName(name: String, kClass: KClass<out Component>) {
        serialName2Component[name] = kClass
        component2serialName[kClass] = name
    }

    public fun <T: Component> getSerializerFor(
        key: String,
        baseClass: KClass<in T>
    ): DeserializationStrategy<out T>? =
        module.getPolymorphic(baseClass = baseClass, serializedClassName = key)

    public fun <T: Component> getSerializerFor(kClass: KClass<in T>): DeserializationStrategy<out T>? {
        val serialName = getSerialNameFor(kClass) ?: return null

        @Suppress("UNCHECKED_CAST") // higher level logic ensures this never fails based on how we register serial names
        return getSerializerFor(serialName, Component::class)
    }

    public fun getSerialNameFor(kClass: KClass<out Component>): String? =
        component2serialName[kClass]

    public fun addSerializersModule(namespace: String, module: SerializersModule) {
        addonToModuleMap[namespace] =
            addonToModuleMap.getOrElse(namespace) { EmptySerializersModule() }.overwriteWith(module)
    }

    public fun clearSerializerModule(addonName: String) {
        addonToModuleMap -= addonName
    }
}
