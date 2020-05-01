package it.unibo.yahm.server.controllers

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import it.unibo.yahm.server.entities.Coordinate
import it.unibo.yahm.server.entities.Leg
import it.unibo.yahm.server.entities.ObstacleType
import it.unibo.yahm.server.entities.Quality
import it.unibo.yahm.server.maps.MapServices
import it.unibo.yahm.server.utils.ClientIdToStream
import it.unibo.yahm.server.utils.GeographicPointDeserializer
import org.neo4j.springframework.data.core.ReactiveNeo4jClient
import org.neo4j.springframework.data.types.GeographicPoint2d
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/roads")
class RoadsController(val service: MapServices, val client: ReactiveNeo4jClient) {

    data class PositionSpeedAndRadius(
            val coordinates: Coordinate,
            val speed: Int,
            val radius: Double
    )

    data class PositionAndObstacleType(
            val coordinates: Coordinate,
            val obstacleType: ObstacleType
    )

    data class ClientIdAndEvaluations(
            val id: String,
            val coordinates: List<Coordinate>,
            //val timestamps: List<Long>,
            val radiuses: List<Double>,
            //val obstacles: List<PositionAndObstacleType>,
            val qualities: List<Quality>
    )

    data class ClientLegInfo(
            val coordinate: Coordinate,
            //val timestamp: Long,
            val radius: Double,
            //val obstacle: PositionAndObstacleType,
            val quality: Quality?
    )

    @PostMapping("/evaluations")
    fun addEvaluations(@RequestBody clientIdAndEvaluations: ClientIdAndEvaluations) {
        val clientStream = ClientIdToStream.getStreamForClient(clientIdAndEvaluations.id, service, client)
        clientIdAndEvaluations.coordinates.forEachIndexed { index, coordinate ->
            //if(index < clientIdAndEvaluations.coordinates.size - 1){
                clientStream.onNext(
                        ClientLegInfo(coordinate,
                                //clientIdAndEvaluations.timestamps[index],
                                clientIdAndEvaluations.radiuses[index],
                                //clientIdAndEvaluations.obstacles[index],
                                if(index < clientIdAndEvaluations.coordinates.size - 1)  clientIdAndEvaluations.qualities[index] else null
                        )
                )
           //}
        }
    }

    @GetMapping("/evaluations")
    fun getEvaluations(@RequestBody positionAndSpeed: PositionSpeedAndRadius): List<Leg> {
        return emptyList()
    }

}
