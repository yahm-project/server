package it.unibo.yahm.server.controllers

import it.unibo.yahm.server.entities.Coordinate
import it.unibo.yahm.server.entities.Leg
import it.unibo.yahm.server.entities.ObstacleType
import it.unibo.yahm.server.entities.Quality
import it.unibo.yahm.server.maps.MapServices
import it.unibo.yahm.server.utils.DBQueries
import org.neo4j.springframework.data.core.ReactiveNeo4jClient
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux
import java.util.*

@RestController
@RequestMapping("/roads")
class RoadsController(private val service: MapServices, private val client: ReactiveNeo4jClient) {

    private val inputRequestStream: EmitterProcessor<ClientIdAndEvaluations> = EmitterProcessor.create()
    private val queriesManager: DBQueries = DBQueries(client)

    init {
        InputStreamLegController(inputRequestStream, service, queriesManager).observe()
    }

    data class PositionAndObstacleType(
            val coordinates: Coordinate,
            val obstacleType: ObstacleType
    )

    data class ClientIdAndEvaluations(
            val id: String,
            val coordinates: List<Coordinate>,
            //val timestamps: List<Long>,
            val radiuses: List<Double>,
            val obstacles: List<PositionAndObstacleType>,
            val qualities: List<Quality>
    )

    @DeleteMapping("/obstacles")
    fun removeObstacle(@RequestParam latitude: Double,
                       @RequestParam longitude: Double,
                       @RequestParam obstacleType: ObstacleType) {
        data class RelativeDistances(
                val distanceFromFirstNode: Double,
                val distanceFromSecondNode: Double
        )

        fun getRelativeDistanceFromNodes(firstNodeId: Long, secondNodeId: Long, obstacleCoordinate: Coordinate):
                Optional<RelativeDistances> {
            val firstNode = queriesManager.getNodeById(firstNodeId).block()
            val secondNode = queriesManager.getNodeById(secondNodeId).block()
            return if (firstNode != null && secondNode != null) {
                val relativeDistanceFromFirstNode = firstNode
                        .coordinates
                        .distanceTo(obstacleCoordinate) / firstNode.coordinates.distanceTo(secondNode.coordinates)
                val relativeDistanceFromSecondNode = secondNode
                        .coordinates
                        .distanceTo(obstacleCoordinate) / secondNode.coordinates.distanceTo(firstNode.coordinates)
                Optional.of(RelativeDistances(relativeDistanceFromFirstNode, relativeDistanceFromSecondNode))
            } else {
                Optional.empty()
            }
        }

        fun getRelationObstacles(firstNodeId: Long, secondNodeId: Long) {

        }

        val obstacleCoordinate = Coordinate(latitude, longitude)
        val nearestWaypoints = service.findNearestNodes(obstacleCoordinate, number = 1)
        if (nearestWaypoints != null) {
            val relativeDistances = getRelativeDistanceFromNodes(nearestWaypoints.waypoints[0].nodes[0],
                    nearestWaypoints.waypoints[0].nodes[1],
                    obstacleCoordinate)
            if (relativeDistances.isPresent) {

            }
        }
    }

    @PostMapping("/evaluations")
    fun addEvaluations(@RequestBody clientIdAndEvaluations: ClientIdAndEvaluations) {
        inputRequestStream.onNext(clientIdAndEvaluations)
    }

    @GetMapping("/evaluations")
    fun getEvaluationWithinRadius(@RequestParam latitude: Double,
                                  @RequestParam longitude: Double,
                                  @RequestParam radius: Double): Flux<Leg> {
        fun mergeObstaclesMaps(first: Map<ObstacleType, List<Coordinate>>,
                               second: Map<ObstacleType, List<Coordinate>>): Map<ObstacleType, List<Coordinate>> {
            return (first.asSequence() + second.asSequence())
                    .distinct()
                    .groupBy({ it.key }, { it.value })
                    .mapValues { (_, values) -> values.flatten() }
        }
        return queriesManager.getEvaluationWithinRadius(latitude, longitude, radius).buffer().flatMap { legs ->
            val modifyList = legs.groupBy { if (it.from.id!! < it.to.id!!) Pair(it.from.id, it.to.id) else Pair(it.to.id, it.from.id) }.values.map {
                val firstLeg = it[0]
                if (it.size == 2) {
                    val secondLeg = it[1]
                    firstLeg.quality = (firstLeg.quality + secondLeg.quality) / 2
                    firstLeg.obstacles = mergeObstaclesMaps(firstLeg.obstacles, secondLeg.obstacles)
                }
                firstLeg
            }
            Flux.fromIterable(modifyList)
        }
    }

    @GetMapping("/evaluations/relative")
    fun getEvaluationWithinBoundariesAlongUserDirection(@RequestParam latitude: Double,
                                                        @RequestParam longitude: Double,
                                                        @RequestParam radius: Double): Flux<Leg> {
        val userPosition = Coordinate(latitude, longitude)
        val userNearestNodeId = service.findNearestNode(userPosition)
        return if (userNearestNodeId != null) {
            queriesManager.getEvaluationWithinBoundariesAlongUserDirection(latitude, longitude, radius, userNearestNodeId)
        } else return Flux.empty()
    }


}
