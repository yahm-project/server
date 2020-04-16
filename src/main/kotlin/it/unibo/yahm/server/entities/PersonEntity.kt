package it.unibo.yahm.server.entities

import org.neo4j.springframework.data.core.schema.Id
import org.neo4j.springframework.data.core.schema.Node


@Node("Person")
data class PersonEntity(@Id val name: String, val born: Int)
