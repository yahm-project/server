package it.unibo.yahm.server.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import org.neo4j.springframework.data.types.GeographicPoint2d
import kotlin.math.*

object Constants {
    const val EARTH_RADIUS = 6371000.0
}
class GeographicPointDeserializer() : JsonDeserializer<GeographicPoint2d>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): GeographicPoint2d {
        val node: JsonNode = p.codec.readTree(p)
        return GeographicPoint2d(node.get("latitude").asDouble(), node.get("longitude").asDouble())
    }
}
fun GeographicPoint2d.distanceTo(toPoint: GeographicPoint2d): Double {
    val fromLongitude = Math.toRadians(this.longitude)
    val toLongitude = Math.toRadians(toPoint.longitude)
    val fromLatitude = Math.toRadians(this.latitude)
    val toLatitude = Math.toRadians(toPoint.latitude)

    val longitudeDistance: Double = toLongitude - fromLongitude
    val latitudeDistance: Double = toLatitude - fromLatitude

    val a = (sin(latitudeDistance / 2).pow(2.0)
            + (cos(fromLatitude) * cos(toLatitude)
            * sin(longitudeDistance / 2).pow(2.0)))

    val c = 2 * asin(sqrt(a))

    return c * Constants.EARTH_RADIUS
}