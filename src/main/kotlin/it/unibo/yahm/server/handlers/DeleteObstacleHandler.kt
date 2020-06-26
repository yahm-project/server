package it.unibo.yahm.server.handlers

import it.unibo.yahm.server.entities.Coordinate
import it.unibo.yahm.server.entities.ObstacleType
import it.unibo.yahm.server.utils.DBQueries
import reactor.core.publisher.Mono
import java.util.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt


class DeleteObstacleHandler(private val queriesManager: DBQueries) {

    data class RelativeDistances(
            val distanceFromFirstNode: Double,
            val distanceFromSecondNode: Double
    )

    fun deleteObstacle(latitude: Double,
                       longitude: Double,
                       obstacleType: ObstacleType,
                       legFromId: Long,
                       legToId: Long): Mono<Boolean> {
        val obstacleCoordinate = Coordinate(latitude, longitude)
        val relativeDistances = getRelativeDistanceFromNodes(legFromId,
                legToId,
                obstacleCoordinate)
        if (relativeDistances.isPresent) {
            return queriesManager.getLegObstacleTypeToDistance(legFromId, legToId).flatMap { obstacleToDistance ->
                removeObstacleFromLegAndUpdateDB(
                        obstacleToDistance,
                        relativeDistances.get().distanceFromFirstNode,
                        legFromId,
                        legToId,
                        obstacleType.toString()
                ).flatMap {
                    //the obstacle wasn't in the leg from firstnode to second node
                    if (!it) {
                        queriesManager.getLegObstacleTypeToDistance(legToId, legFromId).flatMap { obstacleToDistance ->
                            removeObstacleFromLegAndUpdateDB(
                                    obstacleToDistance,
                                    relativeDistances.get().distanceFromSecondNode,
                                    legToId,
                                    legFromId,
                                    obstacleType.toString()
                            )
                        }
                    } else {
                        Mono.just(true)
                    }
                }
            }
        }
        return Mono.just(false)
    }

    private fun getRelativeDistanceFromNodes(firstNodeId: Long, secondNodeId: Long, obstacleCoordinate: Coordinate):
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

    private fun removeObstacleFromLegAndUpdateDB(obstaclesMap: Map<String, List<Double>>,
                                                 relativeDistanceFromNode: Double,
                                                 firstNodeId: Long,
                                                 secondNodeId: Long,
                                                 obstacleType: String): Mono<Boolean> {
        if (obstaclesMap.containsKey(obstacleType)) {
            val obstaclesDistances = obstaclesMap[obstacleType]!!
            val newObstacles = removeElementFromListIfPresent(obstaclesDistances, relativeDistanceFromNode)
            return if (obstaclesDistances.size != newObstacles.size) {
                queriesManager.updateLegObstacles(firstNodeId, secondNodeId, Pair(obstacleType, newObstacles)).map { true }
            } else {
                Mono.just(false)
            }
        }
        return Mono.just(false)
    }

    private fun removeElementFromListIfPresent(initialList: List<Double>, element: Double): List<Double> {
        val index = initialList.indexOfFirst { abs(it - element) < 0.000001 }
        if (index >= 0) {
            return initialList.toMutableList().apply {
                removeAt(index)
            }
        }
        return initialList
    }

}
