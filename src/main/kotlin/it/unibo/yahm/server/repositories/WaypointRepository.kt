package it.unibo.yahm.server.repositories

import it.unibo.yahm.server.entities.Node
import org.neo4j.springframework.data.core.ReactiveNeo4jClient
import org.neo4j.springframework.data.core.fetchAs
import org.neo4j.springframework.data.repository.ReactiveNeo4jRepository
import org.neo4j.springframework.data.types.GeographicPoint2d
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux


interface ClosestPointsRepository {
    fun findClosestPoints(longitude: Double, latitude: Double): Flux<Node>
}

@Component("ClosestPointsRepositoryImpl")
class ClosestPointsRepositoryImpl(val client: ReactiveNeo4jClient) : ClosestPointsRepository  {

    override fun findClosestPoints(longitude: Double, latitude: Double): Flux<Node> {
        val params = mapOf("longitude" to longitude, "latitude" to latitude)

        return client.query("MATCH (w:Waypoint) WITH w, distance(w.coordinates, " +
                "point({longitude: $longitude, latitude: $latitude})) as d ORDER BY d ASC RETURN w")
                .bindAll(params).fetchAs<Node>().mappedBy { _, record ->
                    val waypoint = record["w"].asNode()
                    val coordinates = waypoint["coordinates"].asPoint()
                    Node(waypoint.id(), GeographicPoint2d(coordinates.x(), coordinates.y()))
                }.all()
    }

}

interface WaypointRepository : ClosestPointsRepository, ReactiveNeo4jRepository<Node, Long>
