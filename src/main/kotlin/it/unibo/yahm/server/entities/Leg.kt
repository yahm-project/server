package it.unibo.yahm.server.entities


data class Leg(
        val from: Node,
        val to: Node,
        val quality: Int,
        val obstacles: Map<ObstacleType, List<Double>> = emptyMap()
)
