package it.unibo.yahm.server.utils

import it.unibo.yahm.server.controllers.InputStreamLegController
import it.unibo.yahm.server.entities.*
import org.neo4j.driver.Record
import org.neo4j.driver.summary.ResultSummary
import org.neo4j.springframework.data.core.ReactiveNeo4jClient
import org.neo4j.springframework.data.core.fetchAs
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*
import kotlin.math.round


class DBQueries(private val client: ReactiveNeo4jClient) {
    private fun obstaclesMapToString(obstacles: Map<String, List<Double>>): Optional<String> {
        return if (obstacles.isNotEmpty()) {
            Optional.of(obstacles
                    .entries
                    .joinToString(separator = ",")
                    { it.key + ": " + it.value.joinToString(",", "[", "]") })
        } else {
            Optional.empty()
        }
    }

    fun createOrUpdateQuality(firstNode: Node, secondNode: Node, quality: Quality, obstacles: Map<String, List<Double>>): Mono<ResultSummary> {
        fun getQueryString(): String {
            fun getQuerySecondPartString(obstaclesMapToOptionalString: Optional<String>): String {
                val qualityValue = quality.value
                return if (obstaclesMapToOptionalString.isPresent) {
                    val obstaclesMapToString = obstaclesMapToOptionalString.get()
                    "ON CREATE SET s = {quality: $qualityValue, $obstaclesMapToString \n}" +
                            "ON MATCH SET s = {quality: s.quality * (1 - ${InputStreamLegController.NEW_QUALITY_WEIGHT}) + $qualityValue * ${InputStreamLegController.NEW_QUALITY_WEIGHT}, $obstaclesMapToString}"
                } else {
                    "ON CREATE SET s = {quality: $qualityValue\n}" +
                            "ON MATCH SET s = {quality: s.quality * (1 - ${InputStreamLegController.NEW_QUALITY_WEIGHT}) + $qualityValue * ${InputStreamLegController.NEW_QUALITY_WEIGHT}}"
                }
            }

            val queryFirstPart = "MERGE (a:Node{id:${firstNode.id}, coordinates: point({ longitude: ${firstNode.coordinates.longitude}, latitude:${firstNode.coordinates.latitude}})}) \n" +
                    "MERGE (b:Node{id:${secondNode.id}, coordinates: point({ longitude: ${secondNode.coordinates.longitude}, latitude:${secondNode.coordinates.latitude}})}) \n" +
                    "MERGE (a)-[s:LEG]->(b)\n"
            val obstaclesMapToOptionalString = obstaclesMapToString(obstacles)
            return queryFirstPart + getQuerySecondPartString(obstaclesMapToOptionalString)
        }

        return client.query(getQueryString())
                .run()
    }

    fun updateLegObstacles(firstNodeId: Long, secondNodeId: Long, obstacles: Pair<String, List<Double>>): Mono<ResultSummary> {
       return client.query("MATCH (a:Node)-[leg:LEG]->(b:Node)\n" +
               "WHERE ID(a) = $firstNodeId AND ID(b) = $secondNodeId\n"+
                "SET leg.${obstacles.first} = ${obstacles.second}")
               .run()
    }

    fun getLegObstacleTypeToDistance(firstNodeId: Long, secondNodeId: Long): Mono<Map<String, List<Double>>> {

        fun mapObstacleTypeToDistance(record: Record): Map<String, List<Double>> {
            val toReturnMap = mutableMapOf<String, List<Double>>()
            val leg = record["leg"].asRelationship()
            ObstacleType.values().forEach { obstacleType ->
                val optionalRelativeDistances = leg[obstacleType.toString()]
                if (!optionalRelativeDistances.isNull) {
                    toReturnMap[obstacleType.toString()] = optionalRelativeDistances.asList { it.asDouble() }
                }

            }
            return toReturnMap
        }

        return client.query("MATCH (a:Node)-[leg:LEG]->(b:Node) WHERE ID(a) = $firstNodeId AND ID(b) = $secondNodeId RETURN leg")
                .fetchAs<Map<String, List<Double>>>()
                .mappedBy { _, record ->
                    mapObstacleTypeToDistance(record)
                }
                .first()
    }

    fun getNodeByNeo4jId(id: Long): Mono<Node> {
        return client.query("MATCH (a) WHERE ID(a) = $id RETURN a").fetchAs<Node>().mappedBy { _, record ->
            val node = record["a"].asNode()
            val nodeCoordinates = node["coordinates"].asPoint()
            Node(node.id(), Coordinate(nodeCoordinates.y(), nodeCoordinates.x()))
        }.first()
    }

    private fun legFromRecord(record: Record): Leg {
        val leg = record["leg"].asRelationship()
        val startNode = record["begin"].asNode()
        val startCoordinates = startNode["coordinates"].asPoint()
        val endNode = record["end"].asNode()
        val endCoordinates = endNode["coordinates"].asPoint()
        val start = Node(startNode.id(), Coordinate(startCoordinates.y(), startCoordinates.x()))
        val end = Node(endNode.id(), Coordinate(endCoordinates.y(), endCoordinates.x()))
        val obstacles = ObstacleType.values().filter { !leg[it.name].isNull }.map {
            it to leg[it.name].asList() { v ->
                calculateIntermediatePoint(start.coordinates, end.coordinates, v.asDouble()) ?: start.coordinates
            }
        }.toMap()
        val qualityValue = round(leg["quality"].asDouble()).toInt()
        return Leg(start, end, qualityValue, obstacles)
    }

    fun getEvaluationWithinRadius(latitude: Double, longitude: Double, radius: Double): Flux<Leg> {
        return client.query("MATCH (begin: Node)-[leg:LEG]->(end: Node) \n" +
                "WHERE distance(point({latitude: $latitude, longitude: $longitude}), end.coordinates) <= $radius  \n" +
                "RETURN begin, leg, end").fetchAs<Leg>().mappedBy { _, record -> legFromRecord(record) }.all()
    }

    fun getEvaluationWithinBoundariesAlongUserDirection(latitude: Double,
                                                        longitude: Double,
                                                        radius: Double,
                                                        userNearestNodeId: Long): Flux<Leg> {
        return client.query("MATCH path = (a:Node)-[:LEG  *1..20]->(b: Node) \n" +
                "UNWIND NODES(path) AS n WITH path, size(collect(DISTINCT n)) AS testLength " +
                "WHERE testLength = LENGTH(path) + 1 AND a.id = $userNearestNodeId AND " +
                "distance(a.coordinates, b.coordinates) <= radius \n" +
                "WITH relationships(path) as rel_arr \n" +
                "UNWIND rel_arr as rel \n" +
                "RETURN DISTINCT startNode(rel), rel, endNode(rel)")
                .fetchAs<Leg>().mappedBy { _, record -> legFromRecord(record) }.all()
    }
}
