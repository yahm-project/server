package it.unibo.yahm.server.controllers

import it.unibo.yahm.server.entities.Waypoint
import it.unibo.yahm.server.repositories.WaypointRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/roads")
class WaypointController(val repository: WaypointRepository) {

    data class WaypointBody(val longitude: Double, val latitude: Double)

    @GetMapping("/waypoints/closest")
    fun findClosestWaypoint(@RequestBody waypoint: WaypointBody): Mono<Waypoint> {
        return repository.findClosestPoints(waypoint.longitude, waypoint.latitude).next()
    }
}
