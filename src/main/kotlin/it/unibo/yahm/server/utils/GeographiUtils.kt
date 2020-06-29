package it.unibo.yahm.server.utils

import it.unibo.yahm.server.entities.Coordinate
import kotlin.math.*

object  GeographiUtils {
    /**
     * Aggregates the obstacles relative distances on the DB with the distances to be inserted.
     *
     * @property onDBDistances a map from each obstacle type to relative distances. This data are stored on DB.
     * @property toBeInsertedDistances a map from each obstacle type to relative distances. This data are going to be inserted.
     * @property segmentLength the leg, that contains obstacles, length.
     */
    fun aggregateDistances(onDBDistances: Map<String, List<Double>>,
                           toBeInsertedDistances: Map<String, List<Double>>,
                           segmentLength: Double,
                           minimumDistanceBetweenObstacles: Int): Map<String, List<Double>> {
        return if (toBeInsertedDistances.isNotEmpty()) {
            val toReturnMap = (onDBDistances.asSequence() + toBeInsertedDistances.asSequence())
                    .groupBy({ it.key }, { it.value })
                    .mapValues { (_, values) ->
                        values
                                .flatten()
                                .sorted()
                                .fold(mutableListOf<Double>(), { accumulator, value ->
                                    if (accumulator.isEmpty() ||
                                            (value - accumulator.last()) * segmentLength > minimumDistanceBetweenObstacles) {
                                        accumulator.add(value)
                                    }
                                    accumulator
                                })
                    }
            toReturnMap
        } else {
            onDBDistances
        }
    }

    fun calculateIntermediatePoint(point1: Coordinate, point2: Coordinate, perc: Double): Coordinate {
        val lat1: Double = Math.toRadians(point1.latitude)
        val lng1: Double = Math.toRadians(point1.longitude)
        val lat2: Double = Math.toRadians(point2.latitude)
        val lng2: Double = Math.toRadians(point2.longitude)
        val deltaLat = lat2 - lat1
        val deltaLng = lng2 - lng1
        val calcA = sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(lat1) * cos(lat2) * sin(deltaLng / 2) * sin(deltaLng / 2)
        val calcB = 2 * atan2(sqrt(calcA), sqrt(1 - calcA))
        val A = sin((1 - perc) * calcB) / sin(calcB)
        val B = sin(perc * calcB) / sin(calcB)
        val x = A * cos(lat1) * cos(lng1) + B * cos(lat2) * cos(lng2)
        val y = A * cos(lat1) * sin(lng1) + B * cos(lat2) * sin(lng2)
        val z = A * sin(lat1) + B * sin(lat2)
        val lat3 = atan2(z, sqrt(x * x + y * y))
        val lng3 = atan2(y, x)
        return Coordinate(Math.toDegrees(lat3), Math.toDegrees(lng3))
    }
}
