package it.unibo.yahm.server.handlers

import it.unibo.yahm.server.controllers.RoadsController.PositionAndObstacleType
import it.unibo.yahm.server.entities.Coordinate
import it.unibo.yahm.server.entities.Evaluations
import it.unibo.yahm.server.entities.Node
import it.unibo.yahm.server.entities.ObstacleType
import it.unibo.yahm.server.maps.MapServices
import it.unibo.yahm.server.maps.NearestService
import it.unibo.yahm.server.utils.DBQueries
import it.unibo.yahm.server.utils.GeographiUtils.aggregateDistances
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers


/**
 * A controller based on streams that manages clients data insertions.
 *
 * @property streamToObserve the stream to observe. The data of the stream are the clients' insertions requests.
 * @property mapServices manage requests to OpenStreetMap Api.
 * @property queriesManager bunch of queries.
 */
class AddEvaluationsObservableHandler(private val streamToObserve: EmitterProcessor<Evaluations>,
                                      private val mapServices: MapServices,
                                      private val queriesManager: DBQueries) {
    /**
     * A representation of an obstacle on road.
     *
     * @property onRoadLocation the coordinate (on road) of the obstacle.
     * @property obstacleType the obstacle type.
     */
    data class OnRoadObstacle(
            val onRoadLocation: Coordinate,
            val obstacleType: ObstacleType
    )


    /**
     * Turns a list of obstacles type and position (out of road) into a map from pair of node's id to the list of obstacles on road.
     * @property obstacles the list of obstacles out of road.
     * @return a map from node's id to list of obstacles on road.
     */
    private fun getObstaclesAdjacentPoints(obstacles: List<PositionAndObstacleType>)
            : Map<Pair<Long, Long>, MutableList<OnRoadObstacle>> {
        val resultMap: MutableMap<Pair<Long, Long>, MutableList<OnRoadObstacle>> = mutableMapOf()
        fun addNodePairAndRelativeObstaclesToResult(obstacleTypeBetweenNodes: Pair<ObstacleType, NearestService.Waypoint>){
            val nearestNodeId = obstacleTypeBetweenNodes.second.nodes
            val keyPair = Pair(nearestNodeId[0], nearestNodeId[1])
            val obstacleRoadPoint = Coordinate(obstacleTypeBetweenNodes.second.location[1], obstacleTypeBetweenNodes.second.location[0]) //lat, long
            val onRoadObstacle = OnRoadObstacle(obstacleRoadPoint, obstacleTypeBetweenNodes.first)
            val actualStoredValue = resultMap.putIfAbsent(keyPair, mutableListOf(onRoadObstacle))
            if (actualStoredValue != null) {
                resultMap[keyPair]!!.add(onRoadObstacle)
            }
        }
        obstacles.mapNotNull {
            val result = mapServices.findNearestNodes(it.coordinates, number = 1)
            if (result != null) Pair(it.obstacleType, result.waypoints[0]) else null
        }.forEach {
            addNodePairAndRelativeObstaclesToResult(it)
        }
        return resultMap
    }

    /**
     * Returns a list of obstacle type and the distance of the obstacle from the first (start) adjacent nodes.
     *
     * @property obstaclesAdjacentPoints a map from node's id to list of obstacles on road.
     * @property element a from node to node element.
     * @return a list of pair in which every obstacle type is associated with his distance from the first adjacent node.
     */
    private fun getObstacleDistanceFromStartAndType(obstaclesAdjacentPoints: Map<Pair<Long, Long>, MutableList<OnRoadObstacle>>,
                                            element: Pair<Node, Node>)
            : List<Pair<ObstacleType, Double>> {
        val standardKey = Pair(element.first.id!!, element.second.id!!)
        val reverseKey = Pair(element.second.id!!, element.first.id!!)
        return obstaclesAdjacentPoints.getOrDefault(
                standardKey,
                obstaclesAdjacentPoints.getOrDefault(reverseKey, mutableListOf())
        ).map { Pair(it.obstacleType, element.first.coordinates.distanceTo(it.onRoadLocation)) }
    }

    /**
     *  Returns a map from obstacle type to relative distance of each instance in a specified road segment.
     *
     *  @property distanceFromPoints the road segment length.
     *  @property obstaclesAdjacentPoints a map from a road segment (two adjacent points) to the list of obstacle inside it.
     *  @property fromNodeToNode the ends node of the segment.
     *  @return a map from obstacle type to relative distance of each instance in a specified road segment.
     */
    private fun getObstacleTypeToRelativeDistances(distanceFromPoints: Double,
                                           obstaclesAdjacentPoints:  Map<Pair<Long, Long>, MutableList<OnRoadObstacle>>,
                                           fromNodeToNode: Pair<Node, Node>): MutableMap<String, MutableList<Double>>{

        val startNodeDistancesAndObstacles = getObstacleDistanceFromStartAndType(obstaclesAdjacentPoints, fromNodeToNode)
        val obstacleTypeToRelativeDistances: MutableMap<String, MutableList<Double>> = mutableMapOf()
        startNodeDistancesAndObstacles.forEach { obstacleTypeAndDistance ->
            val relativeDistance = obstacleTypeAndDistance.second / distanceFromPoints
            obstacleTypeToRelativeDistances
                    .putIfAbsent(obstacleTypeAndDistance.first.toString(), mutableListOf(relativeDistance))?.add(relativeDistance)
        }
        return obstacleTypeToRelativeDistances
    }

    /**
     * Aggregates new obstacles from the request with obstacles on DB
     *
     * @property fromNodeToNode the road segment
     * @property obstacles the list of obstacle (coordinate and obstacle type) on the segment
     * @return a mono of a map from obstacle type to relative distances
     */
    private fun aggregateObstacles(fromNodeToNode: Pair<Node, Node>, obstacles: List<PositionAndObstacleType>):
            Mono<Map<String, List<Double>>> {
        val obstaclesAdjacentPoints = getObstaclesAdjacentPoints(obstacles)
        val distanceFromPoints = fromNodeToNode.first.coordinates.distanceTo(fromNodeToNode.second.coordinates)
        val obstacleTypeToRelativeDistances =
                getObstacleTypeToRelativeDistances(distanceFromPoints, obstaclesAdjacentPoints, fromNodeToNode)
        return queriesManager.getLegObstacles(fromNodeToNode.first.id!!, fromNodeToNode.second.id!!)
                .subscribeOn(Schedulers.newSingle("obstacle-updater"))
                .switchIfEmpty(Mono.just(mapOf()))
                .map { onDBDistances ->
                    aggregateDistances(onDBDistances, obstacleTypeToRelativeDistances, distanceFromPoints, MINIMUM_DISTANCE_BETWEEN_OBSTACLES_IN_METERS)
                }
                .retry(RETRY_TIMES_CREATE_UPDATE_QUALITIES)
    }

    /**
     * Observe the stream in order to insert incoming data on the DB.
     */
    fun observe() {
        streamToObserve
            .subscribeOn(Schedulers.newSingle("stretch-processor"))
            .retry()
            .subscribe({ it ->
                val snappedNodesForOriginalNodes = mapServices.snapToRoadNodes(
                    coordinates = it.coordinates,
                    radiuses = it.radiuses
                ) ?: emptyList()

                snappedNodesForOriginalNodes.forEachIndexed { index, nodes ->
                    val quality = it.qualities[index]
                    nodes.zipWithNext().forEach { fromNodeToNode ->
                            aggregateObstacles(fromNodeToNode, it.obstacles)
                            .subscribe({
                                queriesManager.createOrUpdateQuality(fromNodeToNode.first, fromNodeToNode.second, quality, it).subscribe {
                                }
                            }, {
                                println("Failed to create or update stretch qualities")
                                it.printStackTrace()
                            })
                    }
                }
            }, {
                println("Failure on input stream leg controller")
                it.printStackTrace()
            })
    }

    companion object {
        const val NEW_QUALITY_WEIGHT = 0.55
        const val MINIMUM_DISTANCE_BETWEEN_OBSTACLES_IN_METERS = 5
        const val RETRY_TIMES_CREATE_UPDATE_QUALITIES = 3L
    }
}
