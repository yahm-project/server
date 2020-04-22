package it.unibo.yahm.server.controllers

import it.unibo.yahm.server.entities.Segment
import it.unibo.yahm.server.repositories.WaypointRepository
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/roads")
class EvaluationsController(val repository: WaypointRepository) {

    data class PositionAndSpeed(
            val longitude: Double,
            val latitude: Double,
            val speed: Int
    )

    @PostMapping("/evaluations")
    fun addEvaluations(@RequestBody evaluations: List<Segment>) {
    }

    @GetMapping("/evaluations")
    fun getEvaluations(@RequestBody positionAndSpeed: PositionAndSpeed): List<Segment> {
        return emptyList()
    }

}
