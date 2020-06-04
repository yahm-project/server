package it.unibo.yahm.server.maps

import org.neo4j.springframework.data.types.GeographicPoint2d
import kotlin.math.pow


object PolylineUtils {

    private const val RoundPrecision = 6.0

    /**
     * Decodes a polyline that has been encoded using Google's algorithm
     * (http://code.google.com/apis/maps/documentation/polylinealgorithm.html)
     *
     * code derived from : https://gist.github.com/signed0/2031157
     *
     * @param polyline-string
     * @return (long,lat)-Coordinates
     */
    fun decode(polyline: String): List<GeographicPoint2d> {
        val coordinateChunks: MutableList<MutableList<Int>> = mutableListOf()
        coordinateChunks.add(mutableListOf())

        for (char in polyline.toCharArray()) {
            // convert each character to decimal from ascii
            var value = char.toInt() - 63

            // values that have a chunk following have an extra 1 on the left
            val isLastOfChunk = (value and 0x20) == 0
            value = value and (0x1F)

            coordinateChunks.last().add(value)

            if (isLastOfChunk)
                coordinateChunks.add(mutableListOf())
        }

        coordinateChunks.removeAt(coordinateChunks.lastIndex)

        val coordinates: MutableList<Double> = mutableListOf()

        for (coordinateChunk in coordinateChunks) {
            var coordinate = coordinateChunk.mapIndexed { i, chunk -> chunk shl (i * 5) }.reduce { i, j -> i or j }

            // there is a 1 on the right if the coordinate is negative
            if (coordinate and 0x1 > 0)
                coordinate = (coordinate).inv()

            coordinate = coordinate shr 1
            coordinates.add((coordinate).toDouble() / 1000000.0) // add 0 for polyline6
        }

        val points: MutableList<GeographicPoint2d> = mutableListOf()
        var previousX = 0.0
        var previousY = 0.0

        for (i in 0 until coordinates.size step 2) {
            if (coordinates[i] == 0.0 && coordinates[i + 1] == 0.0)
                continue

            previousX += coordinates[i + 1]
            previousY += coordinates[i]

            points.add(GeographicPoint2d(round(previousX), round(previousY)))
        }
        return points
    }

    private fun round(value: Double) =
            (value * 10.0.pow(RoundPrecision)).toInt().toDouble() / 10.0.pow(RoundPrecision)

}
