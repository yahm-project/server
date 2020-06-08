package it.unibo.yahm.server.controllers

import it.unibo.yahm.server.controllers.RoadsController.PositionAndObstacleType
import it.unibo.yahm.server.entities.Coordinate
import it.unibo.yahm.server.entities.Evaluations
import it.unibo.yahm.server.entities.Node
import it.unibo.yahm.server.entities.ObstacleType
import it.unibo.yahm.server.maps.MapServices
import it.unibo.yahm.server.maps.NearestService
import it.unibo.yahm.server.utils.DBQueries
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers


/**
 * A controller based on streams that manages clients data insertions.
 *
 * @property streamToObserve the stream to observe. The data of the stream are the clients' insertions requests.
 * @property mapServices manage requests to OpenStreetMap Api.
 * @property queriesManager bunch of queries.
 */
class InputStreamLegController(private val streamToObserve: EmitterProcessor<Evaluations>,
                               private val mapServices: MapServices,
                               private val queriesManager: DBQueries) {

    /**
     * Observe the stream in order to insert incoming data on the DB.
     */
    fun observe() {
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
        fun getObstaclesAdjacentPoints(obstacles: List<PositionAndObstacleType>)
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
        fun getObstacleDistanceFromStartAndType(obstaclesAdjacentPoints: Map<Pair<Long, Long>, MutableList<OnRoadObstacle>>,
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
        fun getObstacleTypeToRelativeDistances(distanceFromPoints: Double,
                                               obstaclesAdjacentPoints:  Map<Pair<Long, Long>, MutableList<OnRoadObstacle>>,
                                               fromNodeToNode: Pair<Node, Node>): MutableMap<String, MutableList<Double>>{
            val startEndDistancesAndObstacles = getObstacleDistanceFromStartAndType(obstaclesAdjacentPoints, fromNodeToNode)
            val obstacleTypeToRelativeDistances: MutableMap<String, MutableList<Double>> = mutableMapOf()
            startEndDistancesAndObstacles.forEach { obstacleTypeAndDistance ->
                val relativeDistance = obstacleTypeAndDistance.second / distanceFromPoints
                val actualStoredDistances = obstacleTypeToRelativeDistances
                        .putIfAbsent(obstacleTypeAndDistance.first.toString(), mutableListOf(relativeDistance))
                if (actualStoredDistances != null) {
                    actualStoredDistances.add(relativeDistance)
                    obstacleTypeToRelativeDistances[obstacleTypeAndDistance.first.toString()] = actualStoredDistances
                }
            }
            return obstacleTypeToRelativeDistances
        }

        streamToObserve
            .subscribeOn(Schedulers.single())
            .flatMap { it ->
                val snappedNodesForOriginalNodes = Flux.fromIterable(mapServices
                    .snapToRoadNodes(
                            coordinates = it.coordinates,
                            // timestamps = it.timestamps,
                            radiuses = it.radiuses
                    )!!)

                val obstaclesAdjacentPoints = getObstaclesAdjacentPoints(it.obstacles)
                snappedNodesForOriginalNodes.index().flatMap { snappedNodesWithIndex ->
                    val quality = it.qualities[snappedNodesWithIndex.t1.toInt()]
                    Flux.fromIterable(snappedNodesWithIndex.t2.zipWithNext()).flatMap { fromNodeToNode ->
                        val distanceFromPoints = fromNodeToNode.first.coordinates.distanceTo(fromNodeToNode.second.coordinates)
                        val obstacleTypeToRelativeDistances = getObstacleTypeToRelativeDistances(distanceFromPoints, obstaclesAdjacentPoints, fromNodeToNode)
                        queriesManager.getLegObstacleTypeToDistance(fromNodeToNode.first.id!!, fromNodeToNode.second.id!!)
                                .switchIfEmpty(Mono.just(mapOf()))
                                .map { onDBDistances -> aggregateDistances(onDBDistances, obstacleTypeToRelativeDistances, distanceFromPoints) }
                                .flatMap { queriesManager.createOrUpdateQuality(fromNodeToNode.first, fromNodeToNode.second, quality, it) }
                    }
                }
            }.subscribe {
                // pass
            }
    }

    /**
     * Aggregates the obstacles relative distances on the DB with the distances to be inserted.
     *
     * @property onDBDistances a map from each obstacle type to relative distances. This data are stored on DB.
     * @property toBeInsertedDistances a map from each obstacle type to relative distances. This data are going to be inserted.
     * @property segmentLength the leg, that contains obstacles, length.
     */
    private fun aggregateDistances(onDBDistances: Map<String, List<Double>>,
                                   toBeInsertedDistances: Map<String, List<Double>>,
                                   segmentLength: Double): Map<String, List<Double>> {

        fun mergeListElement(list: List<Double>): List<Double> {
            fun aggregateElementIfListIsNonEmpty(toAggregateNumberList: MutableList<Double>,
                                                 destinationList: MutableList<Double>): Boolean {
                return if(toAggregateNumberList.isNotEmpty()){
                    destinationList.add(toAggregateNumberList.reduce { sum, element -> sum + element } / toAggregateNumberList.size)
                    toAggregateNumberList.clear()
                    true
                } else {
                    false
                }
            }
            return if (list.isNotEmpty()) {
                val toReturnList = mutableListOf<Double>()
                var firstElement = list[0]
                val toPutTogetherElements = mutableListOf(firstElement)
                for (index in 1 until list.size) {
                    val actualElement = list[index]
                    if ((actualElement - firstElement) * segmentLength < MINIMUM_DISTANCE_BETWEEN_OBSTACLES_IN_METERS) {
                        toPutTogetherElements.add(actualElement)
                    } else {
                        if(!aggregateElementIfListIsNonEmpty(toPutTogetherElements, toReturnList)) {
                            firstElement = actualElement
                        }
                        toReturnList.add(actualElement)
                    }
                }
                aggregateElementIfListIsNonEmpty(toPutTogetherElements, toReturnList)
                toReturnList
            } else {
                list
            }
        }
        return if (toBeInsertedDistances.isNotEmpty()) {
            val toReturnMap = onDBDistances.toMutableMap()
            toBeInsertedDistances.entries.forEach {
                toReturnMap.merge(it.key, mergeListElement(it.value)) { dbDistances, newDistances ->
                    val toReturnList = dbDistances.toMutableList()
                    toReturnList.addAll(newDistances)
                    mergeListElement(toReturnList.distinct().sorted())
                }
            }
            toReturnMap
        } else {
            onDBDistances
        }
    }

    companion object {
        const val NEW_QUALITY_WEIGHT = 0.55
        const val MINIMUM_DISTANCE_BETWEEN_OBSTACLES_IN_METERS = 5
    }
}
