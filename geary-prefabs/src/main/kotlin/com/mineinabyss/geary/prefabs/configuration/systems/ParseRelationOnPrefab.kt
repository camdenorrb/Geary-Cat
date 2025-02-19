package com.mineinabyss.geary.prefabs.configuration.systems

import com.mineinabyss.geary.annotations.AutoScan
import com.mineinabyss.geary.annotations.Handler
import com.mineinabyss.geary.prefabs.configuration.components.RelationOnPrefab
import com.mineinabyss.geary.systems.Listener
import com.mineinabyss.geary.systems.accessors.TargetScope


@AutoScan
class ParseRelationOnPrefab : Listener() {
    private val TargetScope.relation by onSet<RelationOnPrefab>()

    @Handler
    private fun TargetScope.convertToRelation() {
        try {
            val rel: RelationOnPrefab = relation
//            entity.setRelation(relation.value, entity.parseEntity(relation.key).id)
        } finally {
            entity.remove<RelationOnPrefab>()
        }
    }
}

