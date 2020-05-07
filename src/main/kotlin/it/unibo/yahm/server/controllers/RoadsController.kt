package it.unibo.yahm.server.controllers

import it.unibo.yahm.server.entities.Coordinate
import it.unibo.yahm.server.entities.Leg
import it.unibo.yahm.server.entities.Node
import it.unibo.yahm.server.entities.ObstacleType
import it.unibo.yahm.server.entities.Quality
import it.unibo.yahm.server.maps.MapServices
import org.neo4j.springframework.data.core.ReactiveNeo4jClient
import org.neo4j.springframework.data.core.fetchAs
import org.neo4j.springframework.data.types.GeographicPoint2d
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/roads")
class RoadsController(private val service: MapServices, private val client: ReactiveNeo4jClient) {

    private val inputRequestStream: EmitterProcessor<ClientIdAndEvaluations> = EmitterProcessor.create()

    init {
        InputStreamLegController(inputRequestStream, service, client).observe()
    }

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
            val obstacles: List<PositionAndObstacleType>,
            val qualities: List<Quality>
    )

    data class Boundaries(
            val userPosition: Coordinate,
            val upperLeftBound: Coordinate,
            val bottomRightBound: Coordinate
    )


    @PostMapping("/evaluations")
    fun addEvaluations(@RequestBody clientIdAndEvaluations: ClientIdAndEvaluations) {
        inputRequestStream.onNext(clientIdAndEvaluations)
    }

    @GetMapping("/evaluations")
    fun getEvaluations(@RequestBody positionAndSpeed: PositionSpeedAndRadius): List<Leg> {
        return emptyList()
    }

    @GetMapping("/obstacles")
    fun getEvaluationWithinBoundaries(@RequestBody boundaries: Boundaries): Flux<Leg> {

       return client.query("MATCH p =(begin: Node)-[l:LEG]->(end: Node) \n" +
                "WHERE begin.coordinates.latitude < ${boundaries.upperLeftBound.latitude} AND " +
                "begin.coordinates.latitude > ${boundaries.bottomRightBound.latitude} AND " +
                "begin.coordinates.longitude < ${boundaries.upperLeftBound.longitude} AND " +
                "begin.coordinates.longitude > ${boundaries.bottomRightBound.longitude} \n" +
            "RETURN l").fetchAs<Leg>().mappedBy { _, record ->
            val leg = record["l"].asRelationship()
            val startNode = record["begin"].asNode()
            val startCoordinates = startNode["coordinates"].asPoint()
            val endNode = record["end"].asNode()
            val endCoordinates = endNode["coordinates"].asPoint()
            val start = Node(startNode.id(),  Coordinate(startCoordinates.x(), startCoordinates.y()))
            val end = Node(endNode.id(),  Coordinate(endCoordinates.x(), endCoordinates.y()))
            Leg(start, end, leg["quality"].asInt(), emptyMap())
        }.all()
    }

    @GetMapping("/obstacles/userdir")
    fun getEvaluationWithinBoundariesAlongUserDirection(@RequestBody boundaries: Boundaries): List<Leg> {
        val userNearestNodeId = service.findNearestNode(boundaries.userPosition)
        if(userNearestNodeId != null) {
            client.query("MATCH (p:Leg) WHERE p.id = $userNearestNodeId \n" +
                    "MATCH p =(begin)-[*]->(END)\n" +
                    "WHERE end.coordinates.getLatitude() < ${boundaries.upperLeftBound.latitude} AND end.coordinates.getLatitude > ${boundaries.bottomRightBound.latitude} AND end.coordinates.getLongitude < ${boundaries.upperLeftBound.longitude} AND end.coordinates.getLongitude > ${boundaries.bottomRightBound.longitude}\n" +
                    "FOREACH (n IN nodes(p))")
        }
        return emptyList()
    }



}
