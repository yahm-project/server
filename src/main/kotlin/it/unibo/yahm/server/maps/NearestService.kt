package it.unibo.yahm.server.maps

object NearestService {

    data class Result(
            val waypoints: List<Waypoint>
    )

    data class Waypoint(
            val nodes: List<Long>,
            val distance: Double,
            val name: String,
            val location: List<Double>
    )

}
