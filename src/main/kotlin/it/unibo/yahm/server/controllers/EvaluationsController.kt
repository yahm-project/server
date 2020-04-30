package it.unibo.yahm.server.controllers

import it.unibo.yahm.server.entities.Leg
import it.unibo.yahm.server.entities.Node
import it.unibo.yahm.server.maps.MapServices
import it.unibo.yahm.server.repositories.WaypointRepository
import it.unibo.yahm.server.utils.ClientIdToStream
import org.neo4j.springframework.data.types.GeographicPoint2d
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/roads")
class EvaluationsController(val repository: WaypointRepository, val service: MapServices) {

    data class PositionAndSpeed(
            val longitude: Double,
            val latitude: Double,
            val speed: Int
    )

    data class ClientIdAndEvaluations(
        val id: String,
        val evaluations: List<Leg>
    )

    @PostMapping("/evaluations")
    fun addEvaluations(@RequestBody clientIdAndEvaluations: ClientIdAndEvaluations) {
        val clientStream = ClientIdToStream.getStreamForClient(clientIdAndEvaluations.id)
        clientIdAndEvaluations.evaluations.forEach{clientStream.onNext(it)}
    }

    @GetMapping("/evaluations")
    fun getEvaluations(@RequestBody positionAndSpeed: PositionAndSpeed): List<Leg> {
        return emptyList()
    }

}
