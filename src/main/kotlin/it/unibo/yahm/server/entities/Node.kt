package it.unibo.yahm.server.entities

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import it.unibo.yahm.server.utils.GeographicPointDeserializer
import org.neo4j.springframework.data.core.schema.GeneratedValue
import org.neo4j.springframework.data.core.schema.Id
import org.neo4j.springframework.data.core.schema.Node
import org.neo4j.springframework.data.core.schema.Property
import org.neo4j.springframework.data.types.GeographicPoint2d

@Node("node")
data class Node(
        @Id @GeneratedValue val id: Long?,
        @Property("coordinates")
        @JsonDeserialize(using = GeographicPointDeserializer::class)
        val coordinates: GeographicPoint2d
)
