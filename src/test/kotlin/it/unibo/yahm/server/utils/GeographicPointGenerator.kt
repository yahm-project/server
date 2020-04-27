package it.unibo.yahm.server.utils

import it.unibo.yahm.server.entities.GeographicPoint
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class GeographicPointGenerator {
    companion object {
        fun generatePoint(initialPoint: GeographicPoint, distanceInMeters: Int, bearing: Double): GeographicPoint {
            val initialLongitude = Math.toRadians(initialPoint.longitude)
            val initialLatitude = Math.toRadians(initialPoint.latitude)
            val newLatitude = asin( sin(initialLatitude)*cos(distanceInMeters/GeographicPoint.EARTH_RADIUS) +
                    cos(initialLatitude)*sin(distanceInMeters/GeographicPoint.EARTH_RADIUS)*cos(bearing))
            val newLongitude = initialLongitude + atan2(sin(bearing)*sin(distanceInMeters/GeographicPoint.EARTH_RADIUS)*cos(initialLatitude),
                    cos(distanceInMeters/GeographicPoint.EARTH_RADIUS)-sin(initialLatitude)*sin(newLatitude))
            return  GeographicPoint(Math.toDegrees(newLatitude), Math.toDegrees(newLongitude))
        }
    }
}