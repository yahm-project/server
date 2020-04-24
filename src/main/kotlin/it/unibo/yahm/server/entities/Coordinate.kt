package it.unibo.yahm.server.entities

import org.neo4j.driver.types.Point

data class Coordinate(val latitude: Double, val longitude: Double): Point {
    override fun z(): Double = Double.NaN
    override fun y(): Double = longitude
    override fun x(): Double = latitude
    override fun srid(): Int = 4326
}
