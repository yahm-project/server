package it.unibo.yahm.server.controllers

import it.unibo.yahm.server.utils.aggregateDistances
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ControllersTests {

    @Test
    fun aggregateObstacles() {
        val segmentLength = 50.0
        val onDBDistances = mapOf(Pair("POTHOLE", listOf(0.1,0.3,0.7)))
        val toInsertDistances = mapOf(Pair("POTHOLE", listOf(0.12,0.36,0.82)), Pair("SPEED_BUMP", listOf(0.9)))
        Assertions.assertIterableEquals(mapOf(Pair("POTHOLE", listOf(0.1,0.3,0.7, 0.82)), Pair("SPEED_BUMP", listOf(0.9))).entries,
                aggregateDistances(onDBDistances, toInsertDistances, segmentLength, InputStreamLegController.MINIMUM_DISTANCE_BETWEEN_OBSTACLES_IN_METERS).entries)
    }
}