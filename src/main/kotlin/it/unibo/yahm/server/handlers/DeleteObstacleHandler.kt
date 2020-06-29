package it.unibo.yahm.server.handlers

import it.unibo.yahm.server.entities.Coordinate
import it.unibo.yahm.server.entities.ObstacleType
import it.unibo.yahm.server.utils.DBQueries
import reactor.core.publisher.Mono
import kotlin.math.abs


class DeleteObstacleHandler(private val queriesManager: DBQueries) {

    data class RelativeDistances(val distanceFromFirstNode: Double, val distanceFromSecondNode: Double)

    fun deleteObstacle(coordinate: Coordinate, obstacleType: ObstacleType, legFromId: Long, legToId: Long): Mono<Boolean> {

        fun tryRemoveObstacle(first: Long, second: Long, distance: Double): Mono<Boolean> {
            return queriesManager.getLegObstacles(first, second).flatMap {
                removeObstacleFromLegAndUpdateDB(it, distance, first, second, obstacleType.toString())
            }.switchIfEmpty(Mono.just(false))
        }

        val relativeDistances = getRelativeDistanceFromNodes(legFromId, legToId, coordinate)
        return if (relativeDistances != null) {
            Mono.zip(
                tryRemoveObstacle(legFromId, legToId, relativeDistances.distanceFromFirstNode),
                tryRemoveObstacle(legToId, legFromId, relativeDistances.distanceFromSecondNode)
            ).map { it.t1 || it.t2 }
        } else {
            Mono.just(false)
        }
    }

    private fun getRelativeDistanceFromNodes(firstNodeId: Long, secondNodeId: Long, obstacleCoordinate: Coordinate):
            RelativeDistances? {
        val firstNode = queriesManager.getNodeByNeo4jId(firstNodeId).block()
        val secondNode = queriesManager.getNodeByNeo4jId(secondNodeId).block()
        if (firstNode != null && secondNode != null) {
            val legLength = firstNode.coordinates.distanceTo(secondNode.coordinates)
            val relativeDistanceFromFirstNode = firstNode.coordinates.distanceTo(obstacleCoordinate) / legLength
            val relativeDistanceFromSecondNode = secondNode.coordinates.distanceTo(obstacleCoordinate) / legLength
            return RelativeDistances(relativeDistanceFromFirstNode, relativeDistanceFromSecondNode)
        }
        return null
    }

    private fun removeObstacleFromLegAndUpdateDB(obstaclesMap: Map<String, List<Double>>,
                                                 relativeDistanceFromNode: Double,
                                                 firstNodeId: Long,
                                                 secondNodeId: Long,
                                                 obstacleType: String): Mono<Boolean> {
        val obstaclesDistances = obstaclesMap[obstacleType]
        if (obstaclesDistances != null) {
            val newObstacles = removeFirstDoubleEquals(obstaclesDistances, relativeDistanceFromNode)
            if (obstaclesDistances.size != newObstacles.size) {
                return queriesManager.updateLegObstacles(firstNodeId, secondNodeId, Pair(obstacleType, newObstacles)).map { true }
            }
        }
        return Mono.just(false)
    }

    private fun removeFirstDoubleEquals(initialList: List<Double>, element: Double): List<Double> {
        val index = initialList.indexOfFirst { abs(it - element) < 0.000001 }
        if (index >= 0) {
            return initialList.toMutableList().apply {
                removeAt(index)
            }
        }
        return initialList
    }

}
