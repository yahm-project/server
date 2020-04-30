package it.unibo.yahm.server.utils

import org.neo4j.springframework.data.types.GeographicPoint2d
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class GeographicPointGenerator {
    /*companion object {
        fun generatePoint(initialPoint: GeographicPoint2d, distanceInMeters: Int, bearing: Double): GeographicPoint2d {
            val initialLongitude = Math.toRadians(initialPoint.longitude)
            val initialLatitude = Math.toRadians(initialPoint.latitude)
            val newLatitude = asin( sin(initialLatitude)*cos(distanceInMeters/GeographicPoint2d.EARTH_RADIUS) +
                    cos(initialLatitude)*sin(distanceInMeters/GeographicPoint2d.EARTH_RADIUS)*cos(bearing))
            val newLongitude = initialLongitude + atan2(sin(bearing)*sin(distanceInMeters/GeographicPoint2d.EARTH_RADIUS)*cos(initialLatitude),
                    cos(distanceInMeters/GeographicPoint2d.EARTH_RADIUS)-sin(initialLatitude)*sin(newLatitude))
            return  GeographicPoint2d(Math.toDegrees(newLatitude), Math.toDegrees(newLongitude))
        }
    }*/
}