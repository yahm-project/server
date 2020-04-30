package it.unibo.yahm.server.entities

import org.neo4j.springframework.data.types.GeographicPoint2d


data class Leg(
        val from: Node,
        val to: Node,
        val quality: Int,
        val obstacles: Map<ObstacleType, List<Double>> = emptyMap()
)
