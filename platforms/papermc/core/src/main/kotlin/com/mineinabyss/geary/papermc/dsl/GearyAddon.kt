package com.mineinabyss.geary.papermc.dsl

import com.mineinabyss.geary.api.addon.GearyAddon
import com.mineinabyss.geary.papermc.GearyMCContextKoin
import org.bukkit.plugin.Plugin

/** Entry point to register a new [Plugin] with the Geary ECS. */
//TODO support plugins being re-registered after a reload
public inline fun Plugin.gearyAddon(crossinline init: GearyAddon.() -> Unit) {
    with(GearyMCContextKoin()) {
        serializers.clearSerializerModule(name)
        GearyAddon(
            namespace = this@gearyAddon.name.lowercase()
        ).init()
    }
}
