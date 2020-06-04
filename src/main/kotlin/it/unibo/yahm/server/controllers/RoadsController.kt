package it.unibo.yahm.server.controllers

import com.fasterxml.jackson.annotation.JsonAlias
import it.unibo.yahm.server.entities.*
import it.unibo.yahm.server.maps.MapServices
import it.unibo.yahm.server.utils.DBQueries
import org.neo4j.springframework.data.core.ReactiveNeo4jClient
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt


@RestController
@RequestMapping("/roads")
class RoadsController(private val service: MapServices, client: ReactiveNeo4jClient) {

    private val inputRequestStream: EmitterProcessor<Evaluations> = EmitterProcessor.create()
    private val queriesManager: DBQueries = DBQueries(client)

    init {
        InputStreamLegController(inputRequestStream, service, queriesManager).observe()
    }

    data class PositionAndObstacleType(
            val coordinates: Coordinate,
            @JsonAlias("obstacle_type")
            val obstacleType: ObstacleType
    )

    private fun Double.round(decimalNumber: Double): Double {
        val pow = (10.0.pow(decimalNumber))
        return (this * pow.toInt()).roundToInt() / pow
    }


    private fun removeElementFromListIfPresent(initialList: List<Double>, element: Double): List<Double> {
        val toReturnList = mutableListOf<Double>()
        if (initialList.isNotEmpty()) {
            var lastIndex = 0
            for (index in initialList.indices) {
                val listValue = initialList[index]
                lastIndex = index
                if (listValue.round(6.0) != element.round(6.0)) {
                    toReturnList.add(listValue)
                } else {
                    break
                }
            }
            toReturnList.addAll(initialList.subList(lastIndex + 1, initialList.size))
        }
        return toReturnList
    }

    @DeleteMapping("/obstacles")
    fun removeObstacle(@RequestParam latitude: Double,
                       @RequestParam longitude: Double,
                       @RequestParam obstacleType: ObstacleType,
                       @RequestParam legFromId: Long,
                       @RequestParam legToId: Long): Mono<Boolean> {
        data class RelativeDistances(
                val distanceFromFirstNode: Double,
                val distanceFromSecondNode: Double
        )

        fun getRelativeDistanceFromNodes(firstNodeId: Long, secondNodeId: Long, obstacleCoordinate: Coordinate):
                Optional<RelativeDistances> {
            val firstNode = queriesManager.getNodeByNeo4jId(firstNodeId).block()
            val secondNode = queriesManager.getNodeByNeo4jId(secondNodeId).block()
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

        fun removeObstacleFromLegAndUpdateDB(it: Map<String, List<Double>>,
                                             relativeDistanceFromNode: Double,
                                             firstNodeId: Long,
                                             secondNodeId: Long): Mono<Boolean> {
            if (it.containsKey(obstacleType.toString())) {
                val obstaclesDistances = it[obstacleType.toString()]!!
                val newObstacles = removeElementFromListIfPresent(obstaclesDistances, relativeDistanceFromNode)
                return if (obstaclesDistances.size != newObstacles.size) {
                    queriesManager.updateLegObstacles(firstNodeId, secondNodeId, Pair(obstacleType.toString(), newObstacles)).map { true }
                } else {
                    Mono.just(false)
                }
            }
            return Mono.just(false)
        }

        val obstacleCoordinate = Coordinate(latitude, longitude)
        val relativeDistances = getRelativeDistanceFromNodes(legFromId,
                legToId,
                obstacleCoordinate)
        if (relativeDistances.isPresent) {
            return queriesManager.getLegObstacleTypeToDistance(legFromId, legToId).flatMap { obstacleToDistance ->
                removeObstacleFromLegAndUpdateDB(obstacleToDistance,
                        relativeDistances.get().distanceFromFirstNode,
                        legFromId,
                        legToId).flatMap {
                    //the obstacle wasn't in the leg from firstnode to second node
                    if (!it) {
                        queriesManager.getLegObstacleTypeToDistance(legToId, legFromId).flatMap { obstacleToDistance ->
                            removeObstacleFromLegAndUpdateDB(obstacleToDistance,
                                    relativeDistances.get().distanceFromSecondNode,
                                    legToId,
                                    legFromId)
                        }
                    } else {
                        Mono.just(true)
                    }
                }
            }
        }
        return Mono.just(false)
    }

    @PostMapping("/evaluations")
    fun addEvaluations(@RequestBody evaluations: Evaluations) {
        inputRequestStream.onNext(evaluations)
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
    fun getEvaluationsWithinBoundariesAlongUserDirection(@RequestParam latitude: Double,
                                                        @RequestParam longitude: Double,
                                                        @RequestParam radius: Double): Flux<Leg> {
        val userNearestNodeId = service.findNearestNode(Coordinate(latitude, longitude))
        return if (userNearestNodeId != null) {
            queriesManager.getEvaluationsWithinBoundariesAlongUserDirection(radius, userNearestNodeId)
        } else return Flux.empty()
    }


}
