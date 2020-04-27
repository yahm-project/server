package it.unibo.yahm.server.entities

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.neo4j.springframework.data.core.schema.GeneratedValue
import org.neo4j.springframework.data.core.schema.Id
import org.neo4j.springframework.data.core.schema.Node
import org.neo4j.springframework.data.core.schema.Property
import org.neo4j.springframework.data.types.GeographicPoint2d

@Node("waypoint")
data class Waypoint(
        @Id @GeneratedValue val id: Long?,
        @Property("coordinates")
        @JsonDeserialize(using = WaypointDeserializer::class)
        val coordinates: GeographicPoint2d
)

class WaypointDeserializer() : JsonDeserializer<GeographicPoint2d>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): GeographicPoint2d {
        val node: JsonNode = p.codec.readTree(p)
        return GeographicPoint2d(node.get("latitude").asDouble(), node.get("longitude").asDouble())
    }
}
