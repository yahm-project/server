package it.unibo.yahm.server.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import it.unibo.yahm.server.entities.Coordinate
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

fun calculateIntermediatePoint(point1: Coordinate, point2: Coordinate, perc: Double): Coordinate? { //const φ1 = this.lat.toRadians(), λ1 = this.lon.toRadians();
    //const φ2 = point.lat.toRadians(), λ2 = point.lon.toRadians();
    val lat1: Double =  Math.toRadians(point1.latitude)
    val lng1: Double = Math.toRadians(point1.longitude)
    val lat2: Double = Math.toRadians(point2.latitude)
    val lng2: Double = Math.toRadians(point2.longitude)
    //const Δφ = φ2 - φ1;
    //const Δλ = λ2 - λ1;
    val deltaLat = lat2 - lat1
    val deltaLng = lng2 - lng1
    //const a = Math.sin(Δφ/2) * Math.sin(Δφ/2) + Math.cos(φ1) * Math.cos(φ2) * Math.sin(Δλ/2) * Math.sin(Δλ/2);
    //const δ = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    val calcA = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(lat1) * cos(lat2) * sin(deltaLng / 2) * sin(deltaLng / 2)
    val calcB = 2 * atan2(sqrt(calcA), sqrt(1 - calcA))
    //const A = Math.sin((1-fraction)*δ) / Math.sin(δ);
    //const B = Math.sin(fraction*δ) / Math.sin(δ);
    val A = sin((1 - perc) * calcB) / sin(calcB)
    val B = sin(perc * calcB) / sin(calcB)
    //const x = A * Math.cos(φ1) * Math.cos(λ1) + B * Math.cos(φ2) * Math.cos(λ2);
    //const y = A * Math.cos(φ1) * Math.sin(λ1) + B * Math.cos(φ2) * Math.sin(λ2);
    //const z = A * Math.sin(φ1) + B * Math.sin(φ2);
    val x = A * cos(lat1) * cos(lng1) + B * cos(lat2) * cos(lng2)
    val y = A * cos(lat1) * sin(lng1) + B * cos(lat2) * sin(lng2)
    val z = A * sin(lat1) + B * sin(lat2)
    //const φ3 = Math.atan2(z, Math.sqrt(x*x + y*y));
    //const λ3 = Math.atan2(y, x);
    val lat3 = atan2(z, sqrt(x * x + y * y))
    val lng3 = atan2(y, x)
    //const lat = φ3.toDegrees();
    //const lon = λ3.toDegrees();
    return Coordinate(Math.toDegrees(lat3), Math.toDegrees(lng3))
}