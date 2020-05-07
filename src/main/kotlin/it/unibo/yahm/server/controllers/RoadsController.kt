package it.unibo.yahm.server.controllers

import it.unibo.yahm.server.entities.Coordinate
import it.unibo.yahm.server.entities.Leg
import it.unibo.yahm.server.entities.Node
import it.unibo.yahm.server.entities.ObstacleType
import it.unibo.yahm.server.entities.Quality
import it.unibo.yahm.server.maps.MapServices
import org.neo4j.driver.Record
import org.neo4j.springframework.data.core.ReactiveNeo4jClient
import org.neo4j.springframework.data.core.fetchAs
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

    data class ClientLegInfo(
            val coordinate: Coordinate,
            //val timestamp: Long,
            val radius: Double,
            //val obstacle: PositionAndObstacleType,
            val quality: Quality?
    )

    //data class

    @PostMapping("/evaluations")
    fun addEvaluations(@RequestBody clientIdAndEvaluations: ClientIdAndEvaluations) {
        inputRequestStream.onNext(clientIdAndEvaluations)
    }

    @GetMapping("/evaluations")
    fun getEvaluationWithinRadius(@RequestParam latitude: Double,
                                      @RequestParam longitude: Double,
                                      @RequestParam radius: Double): Flux<Leg> {
       return client.query("MATCH p =(begin: Node)-[leg:LEG]->(end: Node) \n" +
                "WHERE distance(point({latitude: $latitude, longitude: $longitude}), end.coordinates) <= $radius  \n" +
            "RETURN begin, leg, end").fetchAs<Leg>().mappedBy { _, record -> legFromRecord(record) }.all()
    }

    @GetMapping("/evaluations/relative")
    fun getEvaluationWithinBoundariesAlongUserDirection(@RequestParam latitude: Double,
                                                        @RequestParam longitude: Double,
                                                        @RequestParam radius: Double): Flux<Leg> {
        val userPosition = Coordinate(latitude, longitude)
        val userNearestNodeId = service.findNearestNode(userPosition)
        return if(userNearestNodeId != null) {
            client.query("MATCH path = (a:Node)-[:LEG  *1..20]->(b: Node) \n" +
                    "UNWIND NODES(path) AS n WITH path, size(collect(DISTINCT n)) AS testLength " +
                    "WHERE testLength = LENGTH(path) + 1 AND a.id = $userNearestNodeId AND " +
                    "distance(a.coordinates, b.coordinates) <= radius \n" +
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
        val obstacles = emptyMap<ObstacleType, List<Double>>() //leg["obstacles"].asMap{v -> v.asList{dist -> dist.asDouble()}}.mapKeys { (k,v) -> ObstacleType.valueOf(k) }
        return Leg(start, end, leg["quality"].asInt(), obstacles)
    }
}
