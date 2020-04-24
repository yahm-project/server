package it.unibo.yahm.server.controllers

import it.unibo.yahm.server.entities.Coordinate
import it.unibo.yahm.server.entities.Segment
import it.unibo.yahm.server.maps.MapServices
import it.unibo.yahm.server.repositories.WaypointRepository
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/roads")
class EvaluationsController(val repository: WaypointRepository, val service: MapServices) {

    data class PositionAndSpeed(
            val longitude: Double,
            val latitude: Double,
            val speed: Int
    )

    @PostMapping("/evaluations")
    fun addEvaluations(): List<Coordinate>? {
        return service.snapToRoadCoordinates(listOf(Coordinate(12.604016,43.970994),
                Coordinate(12.602042,43.969059)))
    }

    @GetMapping("/evaluations")
    fun getEvaluations(@RequestBody positionAndSpeed: PositionAndSpeed): List<Segment> {
        return emptyList()
    }

}
