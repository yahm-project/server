package it.unibo.yahm.server.entities

import kotlin.math.*


data class Coordinate(val latitude: Double, val longitude: Double) {

    fun distanceTo(toPoint: Coordinate): Double {
        val fromLongitude = Math.toRadians(this.longitude)
        val toLongitude = Math.toRadians(toPoint.longitude)
        val fromLatitude = Math.toRadians(this.latitude)
        val toLatitude = Math.toRadians(toPoint.latitude)

        val longitudeDistance = toLongitude - fromLongitude
        val latitudeDistance = toLatitude - fromLatitude

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
