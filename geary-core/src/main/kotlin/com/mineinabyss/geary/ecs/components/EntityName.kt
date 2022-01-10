package com.mineinabyss.geary.ecs.components

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@JvmInline
@Serializable
@SerialName("geary:name")
public value class EntityName(public val name: String)
