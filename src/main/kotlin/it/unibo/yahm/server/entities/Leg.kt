package it.unibo.yahm.server.entities

import org.neo4j.springframework.data.types.GeographicPoint2d


data class Leg(
        val from: Node,
        val to: Node,
        var quality: Int,
        var obstacles: Map<ObstacleType, List<Coordinate>> = emptyMap()
)
