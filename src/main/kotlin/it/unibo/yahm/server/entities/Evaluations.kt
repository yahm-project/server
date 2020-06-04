package it.unibo.yahm.server.entities

import it.unibo.yahm.server.controllers.RoadsController


data class Evaluations(
        val coordinates: List<Coordinate>,
        val timestamps: List<Long>,
        val radiuses: List<Double>,
        val obstacles: List<RoadsController.PositionAndObstacleType>,
        val qualities: List<Quality>
)
