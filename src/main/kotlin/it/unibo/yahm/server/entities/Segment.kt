package it.unibo.yahm.server.entities


data class Segment(
        val from: Waypoint,
        val to: Waypoint,
        val quality: Int,
        val obstacles: Map<ObstacleType, List<Double>> = emptyMap()
)
