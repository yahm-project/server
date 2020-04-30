package it.unibo.yahm.server.controllers

import it.unibo.yahm.server.entities.Leg
import it.unibo.yahm.server.entities.ObstacleType
import it.unibo.yahm.server.entities.Quality
import it.unibo.yahm.server.maps.MapServices
import it.unibo.yahm.server.utils.ClientIdToStream
import org.neo4j.springframework.data.core.ReactiveNeo4jClient
import org.neo4j.springframework.data.types.GeographicPoint2d
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/roads")
class RoadsController(val service: MapServices, val client: ReactiveNeo4jClient) {

    data class PositionSpeedAndRadius(
            val coordinates: GeographicPoint2d,
            val speed: Int,
            val radius: Double
    )

    data class PositionAndObstacleType(
            val coordinates: GeographicPoint2d,
            val obstacleType: ObstacleType
    )

    data class ClientIdAndEvaluations(
            val id: String,
            val coordinates: List<GeographicPoint2d>,
            val timestamps: List<Long>,
            val radiuses: List<Double>,
            val obstacles: List<PositionAndObstacleType>,
            val quality: List<Quality>
    )

    //data class

    @PostMapping("/evaluations")
    fun addEvaluations(@RequestBody clientIdAndEvaluations: ClientIdAndEvaluations) {
        val clientStream = ClientIdToStream.getStreamForClient(clientIdAndEvaluations.id, service, client)
        //clientIdAndEvaluations.evaluations.forEach { clientStream.onNext(it) }
    }

    @GetMapping("/evaluations")
    fun getEvaluations(@RequestBody positionAndSpeed: PositionSpeedAndRadius): List<Leg> {
        return emptyList()
    }

}
