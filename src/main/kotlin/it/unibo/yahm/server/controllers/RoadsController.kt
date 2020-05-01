package it.unibo.yahm.server.controllers

import it.unibo.yahm.server.entities.Coordinate
import it.unibo.yahm.server.entities.Leg
import it.unibo.yahm.server.entities.Node
import it.unibo.yahm.server.entities.ObstacleType
import it.unibo.yahm.server.entities.Quality
import it.unibo.yahm.server.maps.MapServices
import it.unibo.yahm.server.utils.ClientIdToStream
import org.neo4j.driver.Record
import org.neo4j.springframework.data.core.ReactiveNeo4jClient
import org.neo4j.springframework.data.core.fetchAs
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/roads")
class RoadsController(val service: MapServices, val client: ReactiveNeo4jClient) {

    data class PositionSpeedAndRadius(
            val coordinates: Coordinate,
            val speed: Int,
            val radius: Double
    )

    data class PositionAndObstacleType(
            val coordinates: Coordinate,
            val obstacleType: ObstacleType
    )

    data class ClientIdAndEvaluations(
            val id: String,
            val coordinates: List<Coordinate>,
            //val timestamps: List<Long>,
            val radiuses: List<Double>,
            //val obstacles: List<PositionAndObstacleType>,
            val qualities: List<Quality>
    )

    data class ClientLegInfo(
            val coordinate: Coordinate,
            //val timestamp: Long,
            val radius: Double,
            //val obstacle: PositionAndObstacleType,
            val quality: Quality?
    )
    
    data class Boundaries(
            val userPosition: Coordinate,
            val upperLeftBound: Coordinate,
            val bottomRightBound: Coordinate
    )

    //data class

    @PostMapping("/evaluations")
    fun addEvaluations(@RequestBody clientIdAndEvaluations: ClientIdAndEvaluations) {
        val clientStream = ClientIdToStream.getStreamForClient(clientIdAndEvaluations.id, service, client)
        clientIdAndEvaluations.coordinates.forEachIndexed { index, coordinate ->
            //if(index < clientIdAndEvaluations.coordinates.size - 1){
                clientStream.onNext(
                        ClientLegInfo(coordinate,
                                //clientIdAndEvaluations.timestamps[index],
                                clientIdAndEvaluations.radiuses[index],
                                //clientIdAndEvaluations.obstacles[index],
                                if(index < clientIdAndEvaluations.coordinates.size - 1)  clientIdAndEvaluations.qualities[index] else null
                        )
                )
           //}
        }
    }

    @GetMapping("/evaluations")
    fun getEvaluationWithinBoundaries(@RequestBody boundaries: Boundaries): Flux<Leg> {
       return client.query("MATCH p =(begin: Node)-[leg:LEG]->(end: Node) \n" +
                "WHERE begin.coordinates.x > ${boundaries.upperLeftBound.latitude} AND " +
                "begin.coordinates.x < ${boundaries.bottomRightBound.latitude} AND " +
                "begin.coordinates.y > ${boundaries.upperLeftBound.longitude} AND " +
                "begin.coordinates.y < ${boundaries.bottomRightBound.longitude} \n" +
            "RETURN begin, leg, end").fetchAs<Leg>().mappedBy { _, record -> legFromRecord(record) }.all()
    }

    @GetMapping("/evaluations/relative")
    fun getEvaluationWithinBoundariesAlongUserDirection(@RequestBody boundaries: Boundaries): Flux<Leg> {
        val userNearestNodeId = service.findNearestNode(boundaries.userPosition)
        return if(userNearestNodeId != null) {
            client.query("MATCH path = (a:Node)-[:LEG  *1..20]->(b: Node) \n" +
                    "UNWIND NODES(path) AS n WITH path, size(collect(DISTINCT n)) AS testLength " +
                    "WHERE testLength = LENGTH(path) + 1 AND a.id = $userNearestNodeId AND " +
                    "b.coordinates.latitude > ${boundaries.upperLeftBound.latitude} AND " +
                    "b.coordinates.latitude < ${boundaries.bottomRightBound.latitude} AND " +
                    "b.coordinates.longitude > ${boundaries.upperLeftBound.longitude} AND " +
                    "b.coordinates.longitude < ${boundaries.bottomRightBound.longitude} \n" +
                    "WITH relationships(path) as rel_arr \n" +
                    "UNWIND rel_arr as rel \n" +
                    "RETURN DISTINCT startNode(rel), rel, endNode(rel)")
            .fetchAs<Leg>().mappedBy { _, record -> legFromRecord(record)}.all()
        } else return Flux.empty()
    }

    fun legFromRecord(record: Record): Leg {
        val leg = record["leg"].asRelationship()
        val startNode = record["begin"].asNode()
        val startCoordinates = startNode["coordinates"].asPoint()
        val endNode = record["end"].asNode()
        val endCoordinates = endNode["coordinates"].asPoint()
        val start = Node(startNode.id(),  Coordinate(startCoordinates.x(), startCoordinates.y()))
        val end = Node(endNode.id(),  Coordinate(endCoordinates.x(), endCoordinates.y()))
        return Leg(start, end, leg["quality"].asInt(), emptyMap())
    }
}
