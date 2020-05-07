package it.unibo.yahm.server.controllers

import it.unibo.yahm.server.entities.Node
import it.unibo.yahm.server.entities.Quality
import it.unibo.yahm.server.maps.MapServices
import org.neo4j.springframework.data.core.ReactiveNeo4jClient
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import it.unibo.yahm.server.controllers.RoadsController.PositionAndObstacleType
import it.unibo.yahm.server.entities.Coordinate
import it.unibo.yahm.server.entities.ObstacleType

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
                        fromNodeToNodePair.flatMap {
                            val startEndDistancesAndObstacles = getStartEndDistancesAndObstacles(obstaclesAdjacentPoints, it)
                            val obstacleTypeToRelativeDistances: MutableMap<String, MutableList<Double>> = mutableMapOf()
                            startEndDistancesAndObstacles.forEach { obstacleTypeAndDistance ->
                                val distanceFromPoints = it.first.coordinates.distanceTo(it.second.coordinates)
                                val relativeDistance = obstacleTypeAndDistance.second / distanceFromPoints
                                val actualStoredDistances = obstacleTypeToRelativeDistances
                                        .putIfAbsent(obstacleTypeAndDistance.first.toString(), mutableListOf(relativeDistance))
                                if (actualStoredDistances != null) {
                                    actualStoredDistances.add(relativeDistance)
                                    obstacleTypeToRelativeDistances.put(obstacleTypeAndDistance.first.toString(), actualStoredDistances)
                                }

                            }
                            createOrUpdateQuality(it.first, it.second, quality, obstacleTypeToRelativeDistances)
                        }
                    }
                }.subscribe {
                    println(it)
                }
    }

    private fun createOrUpdateQuality(firstNode: Node, secondNode: Node, quality: Quality, obstacles: Map<String, List<Double>>): Mono<Int> {
        val qualityValue = quality.value
        val mapToString = obstacles
                .entries
                .joinToString(separator = ",")
                { it.key + ": "+it.value.joinToString(",", "[", "]") }
        return client.query("MERGE (a:Node{id:${firstNode.id}, coordinates: point({ longitude: ${firstNode.coordinates.longitude}, latitude:${firstNode.coordinates.latitude}})}) \n" +
                "MERGE (b:Node{id:${secondNode.id}, coordinates: point({ longitude: ${secondNode.coordinates.longitude}, latitude:${secondNode.coordinates.latitude}})}) \n" +
                "MERGE (a)-[s:Leg]->(b)\n" +
                "ON CREATE SET s = {quality: $qualityValue, $mapToString\n}" +
                "ON MATCH SET s = {quality: s.quality * (1 - $NEW_QUALITY_WEIGHT) + $qualityValue * $NEW_QUALITY_WEIGHT, $mapToString}")
                .run()
                .map { qualityValue }
    }

    companion object {
        const val NEW_QUALITY_WEIGHT = 0.55
    }
}