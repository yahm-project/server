package it.unibo.yahm.server.entities


data class Leg(
        val from: Node,
        val to: Node,
        var quality: Int,
        var obstacles: Map<ObstacleType, List<Coordinate>> = emptyMap()
)
