package it.unibo.yahm.server.controllers

import com.fasterxml.jackson.annotation.JsonAlias
import it.unibo.yahm.server.entities.*
import it.unibo.yahm.server.handlers.DeleteObstacleHandler
import it.unibo.yahm.server.handlers.AddEvaluationsObservableHandler
import it.unibo.yahm.server.handlers.GetEvaluationsHandler
import it.unibo.yahm.server.maps.MapServices
import it.unibo.yahm.server.utils.DBQueries
import org.neo4j.springframework.data.core.ReactiveNeo4jClient
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


@RestController
@RequestMapping("/roads")
class RoadsController(client: ReactiveNeo4jClient, service: MapServices) {

    private val inputRequestStream: EmitterProcessor<Evaluations> = EmitterProcessor.create()
    private val queriesManager: DBQueries = DBQueries(client)
    private val getEvaluationsHandler = GetEvaluationsHandler(queriesManager, service)
    private val deleteObstacleHandler = DeleteObstacleHandler(queriesManager)

    init {
        AddEvaluationsObservableHandler(inputRequestStream, service, queriesManager).observe()
    }

    data class PositionAndObstacleType(
            val coordinates: Coordinate,
            @JsonAlias("obstacle_type")
            val obstacleType: ObstacleType
    )

    @DeleteMapping("/obstacles")
    fun removeObstacle(@RequestParam latitude: Double,
                       @RequestParam longitude: Double,
                       @RequestParam obstacleType: ObstacleType,
                       @RequestParam legFromId: Long,
                       @RequestParam legToId: Long): Mono<Boolean> {
        return deleteObstacleHandler.deleteObstacle(latitude, longitude, obstacleType, legFromId, legToId)
    }

    @PostMapping("/evaluations")
    fun addEvaluations(@RequestBody evaluations: Evaluations) {
        inputRequestStream.onNext(evaluations)
    }

    @GetMapping("/evaluations")
    fun getEvaluationWithinRadius(@RequestParam latitude: Double,
                                  @RequestParam longitude: Double,
                                  @RequestParam radius: Double): Flux<Leg> {
        return getEvaluationsHandler.getEvaluationWithinRadius(latitude, longitude, radius)
    }

    @GetMapping("/evaluations/relative")
    fun getEvaluationsWithinBoundariesAlongUserDirection(@RequestParam latitude: Double,
                                                         @RequestParam longitude: Double,
                                                         @RequestParam radius: Double): Flux<Leg> {
        return getEvaluationsHandler.getEvaluationsWithinBoundariesAlongUserDirection(latitude, longitude, radius)
    }

}
