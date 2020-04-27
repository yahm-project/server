package it.unibo.yahm.server.entities
import org.neo4j.driver.types.Point
import org.neo4j.springframework.data.types.Neo4jPoint
import kotlin.math.*

data class GeographicPoint(val latitude: Double, val longitude: Double): Point {
    override fun z(): Double = Double.NaN
    override fun y(): Double = longitude
    override fun x(): Double = latitude
    override fun srid(): Int = 4326
    fun distanceTo(toPoint: GeographicPoint): Double {
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

        return c * EARTH_RADIUS
    }
    companion object {
        const val EARTH_RADIUS = 6371000.0
    }
}

data class GeographicPoint2(val latitude: Double, val longitude: Double): Neo4jPoint {
    override fun getSrid(): Int {
        return 4326
    }
    override fun toString(): String {
        return "GeographicPoint2d{" +
                "longitude=" + longitude +
                ", latitude=" + latitude +
                ", srid=" + srid +
                '}'
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GeographicPoint2

        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false

        return true
    }

    override fun hashCode(): Int {
        var result = latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        return result
    }
}
