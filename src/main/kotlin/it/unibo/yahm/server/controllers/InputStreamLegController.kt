package it.unibo.yahm.server.controllers

import it.unibo.yahm.server.maps.MapServices
import org.neo4j.springframework.data.core.ReactiveNeo4jClient
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import it.unibo.yahm.server.controllers.RoadsController.PositionAndObstacleType
import it.unibo.yahm.server.entities.*
import org.neo4j.driver.Record
import org.neo4j.driver.summary.ResultSummary
import org.neo4j.springframework.data.core.fetchAs

class InputStreamLegController(private val streamToObserve: EmitterProcessor<RoadsController.ClientIdAndEvaluations>,
                               private val mapServices: MapServices,
                               private val client: ReactiveNeo4jClient) {

    fun observe() {
        data class OnRoadObstacle(
                val onRoadLocation: Coordinate,
                val obstacle: ObstacleType
        )

        fun getFromNodeToNodePair(nodes: List<Node>): Flux<Pair<Node, Node>> {
            val fromToNodesPair: MutableList<Pair<Node, Node>> = mutableListOf()
            nodes.forEachIndexed { index, node ->
                if (index < nodes.size - 1) {
                    fromToNodesPair.add(Pair(node, nodes[index + 1]))
                }
            }
            return Flux.fromIterable(fromToNodesPair)
        }

        fun getObstaclesAdjacentPoints(obstacles: List<PositionAndObstacleType>)
                : Map<Pair<Long, Long>, MutableList<OnRoadObstacle>> {
            val resultMap: MutableMap<Pair<Long, Long>, MutableList<OnRoadObstacle>> = mutableMapOf()
            obstacles.mapNotNull {
                val result = mapServices.findNearestNodes(it.coordinates, number = 1)
                if (result != null) Pair(it.obstacleType, result.waypoints[0]) else null
            }.forEach {
                val nearestNodeId = it.second.nodes
                val keyPair = Pair(nearestNodeId[0], nearestNodeId[1])
                val obstacleRoadPoint = Coordinate(it.second.location[1], it.second.location[0]) //lat, long
                val onRoadObstacle = OnRoadObstacle(obstacleRoadPoint, it.first)
                val actualStoredValue = resultMap.putIfAbsent(keyPair, mutableListOf(onRoadObstacle))
                if (actualStoredValue != null) {
                    resultMap[keyPair]!!.add(onRoadObstacle)
                }
            }
            return resultMap
        }

        fun getStartEndDistancesAndObstacles(obstaclesAdjacentPoints: Map<Pair<Long, Long>, MutableList<OnRoadObstacle>>,
                                             element: Pair<Node, Node>)
                : List<Pair<ObstacleType, Double>> {
            val standardKey = Pair(element.first.id!!, element.second.id!!)
            val reverseKey = Pair(element.second.id!!, element.first.id!!)
            return obstaclesAdjacentPoints.getOrDefault(
                    standardKey,
                    obstaclesAdjacentPoints.getOrDefault(reverseKey, mutableListOf())
            ).map { Pair(it.obstacle, element.first.coordinates.distanceTo(it.onRoadLocation)) }
        }

        streamToObserve
                .subscribeOn(Schedulers.single())
                .flatMap { it ->
                    val snappedNodesForOriginalNodes = Flux.fromIterable(mapServices
                            .snapToRoadNodes(
                                    coordinates = it.coordinates,
                                    /* { it.timestamp },*/
                                    radiuses = it.radiuses
                            )!!)

                    val obstaclesAdjacentPoints = getObstaclesAdjacentPoints(it.obstacles)
                    snappedNodesForOriginalNodes.index().flatMap { snappedNodesWithIndex ->
                        val quality = it.qualities[snappedNodesWithIndex.t1.toInt()]
                        val fromNodeToNodePair = getFromNodeToNodePair(snappedNodesWithIndex.t2)
                        fromNodeToNodePair.flatMap { fromNodeToNode ->
                            val startEndDistancesAndObstacles = getStartEndDistancesAndObstacles(obstaclesAdjacentPoints, fromNodeToNode)
                            val obstacleTypeToRelativeDistances: MutableMap<String, MutableList<Double>> = mutableMapOf()
                            val distanceFromPoints = fromNodeToNode.first.coordinates.distanceTo(fromNodeToNode.second.coordinates)
                            startEndDistancesAndObstacles.forEach { obstacleTypeAndDistance ->
                                val relativeDistance = obstacleTypeAndDistance.second / distanceFromPoints
                                val actualStoredDistances = obstacleTypeToRelativeDistances
                                        .putIfAbsent(obstacleTypeAndDistance.first.toString(), mutableListOf(relativeDistance))
                                if (actualStoredDistances != null) {
                                    actualStoredDistances.add(relativeDistance)
                                    obstacleTypeToRelativeDistances.put(obstacleTypeAndDistance.first.toString(), actualStoredDistances)
                                }
                            }
                            getLegInformation(fromNodeToNode.first.id!!, fromNodeToNode.second.id!!)
                                    .switchIfEmpty(Mono.just(mapOf()))
                                    .map { onDBDistances -> filterDistances(onDBDistances, obstacleTypeToRelativeDistances, distanceFromPoints) }
                                    .flatMap { createOrUpdateQuality(fromNodeToNode.first, fromNodeToNode.second, quality, it) }
                        }
                    }
                }.subscribe {
                    println(it)
                }
    }

    private fun createOrUpdateQuality(firstNode: Node, secondNode: Node, quality: Quality, obstacles: Map<String, List<Double>>): Mono<ResultSummary> {
        fun getQueryString(): String{
            val qualityValue = quality.value
            val queryFirstPart = "MERGE (a:Node{id:${firstNode.id}, coordinates: point({ longitude: ${firstNode.coordinates.longitude}, latitude:${firstNode.coordinates.latitude}})}) \n" +
                    "MERGE (b:Node{id:${secondNode.id}, coordinates: point({ longitude: ${secondNode.coordinates.longitude}, latitude:${secondNode.coordinates.latitude}})}) \n" +
                    "MERGE (a)-[s:LEG]->(b)\n"
            return if(obstacles.isNotEmpty()){
                val mapToString = obstacles
                        .entries
                        .joinToString(separator = ",")
                        { it.key + ": " + it.value.joinToString(",", "[", "]") }
                queryFirstPart +
                        "ON CREATE SET s = {quality: $qualityValue, $mapToString \n}" +
                        "ON MATCH SET s = {quality: s.quality * (1 - $NEW_QUALITY_WEIGHT) + $qualityValue * $NEW_QUALITY_WEIGHT, $mapToString}"
            } else {
                queryFirstPart+
                        "ON CREATE SET s = {quality: $qualityValue\n}" +
                        "ON MATCH SET s = {quality: s.quality * (1 - $NEW_QUALITY_WEIGHT) + $qualityValue * $NEW_QUALITY_WEIGHT}"
            }
        }

        return client.query(getQueryString())
                .run()
    }

    private fun getLegInformation(firstNodeId: Long, secondNodeId: Long): Mono<Map<String, List<Double>>> {

        fun mapObstacleTypeToDistance(record: Record): Map<String, List<Double>> {
            val toReturnMap = mutableMapOf<String, List<Double>>()
            val leg = record["leg"].asRelationship()
            ObstacleType.values().forEach {
                val optionalRelativeDistances = leg[it.toString()]
                if (!optionalRelativeDistances.isNull) {
                    toReturnMap.put(it.toString(), optionalRelativeDistances.asList { it.asDouble() })
                }

            }
            return toReturnMap
        }

        return client.query("MATCH (a:Node{id:$firstNodeId})-[leg:LEG]->(b:Node{id:$secondNodeId}) RETURN leg")
                .fetchAs<Map<String, List<Double>>>()
                .mappedBy { _, record ->
                    mapObstacleTypeToDistance(record)
                }
                .first()
    }

    private fun filterDistances(onDBDistances: Map<String, List<Double>>,
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