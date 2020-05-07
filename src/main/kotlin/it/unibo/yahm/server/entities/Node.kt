package it.unibo.yahm.server.entities

import org.neo4j.springframework.data.core.schema.GeneratedValue
import org.neo4j.springframework.data.core.schema.Id
import org.neo4j.springframework.data.core.schema.Node
import org.neo4j.springframework.data.core.schema.Property

@Node("node")
data class Node(
        @Id @GeneratedValue val id: Long?,
        @Property("coordinates")
        val coordinates: Coordinate
)
