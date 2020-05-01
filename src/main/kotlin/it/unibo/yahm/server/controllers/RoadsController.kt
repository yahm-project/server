package it.unibo.yahm.server.controllers

import it.unibo.yahm.server.entities.Leg
import it.unibo.yahm.server.entities.Node
import it.unibo.yahm.server.entities.ObstacleType
import it.unibo.yahm.server.entities.Quality
import it.unibo.yahm.server.maps.MapServices
import it.unibo.yahm.server.utils.ClientIdToStream
import org.neo4j.springframework.data.core.ReactiveNeo4jClient
import org.neo4j.springframework.data.core.fetchAs
import org.neo4j.springframework.data.types.GeographicPoint2d
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/roads")
class RoadsController(val service: MapServices, val client: ReactiveNeo4jClient) {

    data class PositionSpeedAndRadius(
            val coordinates: GeographicPoint2d,
            val speed: Int,
            val radius: Double
    )

    data class PositionAndObstacleType(
            val coordinates: GeographicPoint2d,
            val obstacleType: ObstacleType
    )

    data class ClientIdAndEvaluations(
            val id: String,
            val coordinates: List<GeographicPoint2d>,
            val timestamps: List<Long>,
            val radiuses: List<Double>,
            val obstacles: List<PositionAndObstacleType>,
            val quality: List<Quality>
    )

    data class Boundaries(
            val userPosition: GeographicPoint2d,
            val upperLeftBound: GeographicPoint2d,
            val bottomRightBound: GeographicPoint2d
    )

    //data class

    @PostMapping("/evaluations")
    fun addEvaluations(@RequestBody clientIdAndEvaluations: ClientIdAndEvaluations) {
        val clientStream = ClientIdToStream.getStreamForClient(clientIdAndEvaluations.id, service, client)
        //clientIdAndEvaluations.evaluations.forEach { clientStream.onNext(it) }
    }


    @GetMapping("/obstacles")
    fun getEvaluationWithinBoundaries(@RequestBody boundaries: Boundaries): Flux<Leg> {
       return client.query("MATCH p =(begin: Node)-[leg:LEG]->(end: Node) \n" +
                "WHERE begin.coordinates.latitude < ${boundaries.upperLeftBound.latitude} AND " +
                "begin.coordinates.latitude > ${boundaries.bottomRightBound.latitude} AND " +
                "begin.coordinates.longitude < ${boundaries.upperLeftBound.longitude} AND " +
                "begin.coordinates.longitude > ${boundaries.bottomRightBound.longitude} \n" +
            "RETURN begin, leg, end").fetchAs<Leg>().mappedBy { _, record ->
            val leg = record["leg"].asRelationship()
            val startNode = record["begin"].asNode()
            val startCoordinates = startNode["coordinates"].asPoint()
            val endNode = record["end"].asNode()
            val endCoordinates = endNode["coordinates"].asPoint()
            val start = Node(startNode.id(),  GeographicPoint2d(startCoordinates.x(), startCoordinates.y()))
            val end = Node(endNode.id(),  GeographicPoint2d(endCoordinates.x(), endCoordinates.y()))
            Leg(start, end, leg["quality"].asInt(), emptyMap())
        }.all()
    }

    @GetMapping("/obstacles/relative")
    fun getEvaluationWithinBoundariesAlongUserDirection(@RequestBody boundaries: Boundaries): List<Leg> {
        val userNearestNodeId = service.findNearestNode(boundaries.userPosition)
        return if(userNearestNodeId != null) {
            client.query("MATCH path = (a:Node)-[:LEG  *1..20]->(b: Node) \n" +
                    "WHERE a.id = $userNearestNodeId AND " +
                    "b.coordinates.latitude < ${boundaries.upperLeftBound.latitude} AND " +
                    "b.coordinates.latitude > ${boundaries.bottomRightBound.latitude} AND " +
                    "b.coordinates.longitude < ${boundaries.upperLeftBound.longitude} AND " +
                    "b.coordinates.longitude > ${boundaries.bottomRightBound.longitude} \n" +
                    "WITH relationships(path) as r \n" +
                    "UNWIND r as t \n" +
                    "RETURN DISTINCT startNode(t), t, endNode(t)")
            emptyList()
        } else {
            emptyList()
        }
    }



}
