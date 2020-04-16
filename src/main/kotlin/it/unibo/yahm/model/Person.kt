package it.unibo.yahm.model

import org.neo4j.springframework.data.core.schema.GeneratedValue
import org.neo4j.springframework.data.core.schema.Id
import org.neo4j.springframework.data.core.schema.Node

@Node("Person")
class Person {
    @Id
    @GeneratedValue
    private val id: Long? = null
    var firstName: String? = null
    var lastName: String? = null
}