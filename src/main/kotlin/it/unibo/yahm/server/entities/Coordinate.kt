package it.unibo.yahm.server.entities
import kotlin.math.*

object Constants {
    const val EARTH_RADIUS = 6371000.0
}

class Coordinate(
        val latitude: Double,
        val longitude: Double){

        fun distanceTo(toPoint: Coordinate): Double {
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

    override fun toString(): String {
        return "Coordinate(latitude=$latitude, longitude=$longitude)"
    }


}
