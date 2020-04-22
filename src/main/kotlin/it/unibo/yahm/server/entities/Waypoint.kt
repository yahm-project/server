package it.unibo.yahm.server.entities

import org.neo4j.springframework.data.core.schema.GeneratedValue
import org.neo4j.springframework.data.core.schema.Id
import org.neo4j.springframework.data.core.schema.Node
import org.neo4j.springframework.data.core.schema.Property
import org.neo4j.springframework.data.types.GeographicPoint2d

@Node("waypoint")
data class Waypoint(
        @Id @GeneratedValue val id: Long?,
        @Property("coordinates") val coordinates: GeographicPoint2d
)
