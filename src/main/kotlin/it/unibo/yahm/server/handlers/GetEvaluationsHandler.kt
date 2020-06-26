package it.unibo.yahm.server.handlers

import it.unibo.yahm.server.entities.Coordinate
import it.unibo.yahm.server.entities.Leg
import it.unibo.yahm.server.entities.ObstacleType
import it.unibo.yahm.server.maps.MapServices
import it.unibo.yahm.server.utils.DBQueries
import reactor.core.publisher.Flux


class GetEvaluationsHandler(private val queriesManager: DBQueries, private val mapServices: MapServices) {

    fun getEvaluationWithinRadius(latitude: Double, longitude: Double, radius: Double): Flux<Leg> {
        return queriesManager.getEvaluationWithinRadius(latitude, longitude, radius).buffer().flatMap { legs ->
            Flux.fromIterable(
                legs.groupBy {
                    if (it.from.id!! < it.to.id!!) Pair(it.from.id, it.to.id)
                    else Pair(it.to.id, it.from.id)
                }.values.map {
                    val firstLeg = it[0]
                    if (it.size == 2) {
                        val secondLeg = it[1]
                        firstLeg.quality = (firstLeg.quality + secondLeg.quality) / 2
                        firstLeg.obstacles = mergeObstaclesMaps(firstLeg.obstacles, secondLeg.obstacles)
                    }
                    firstLeg
                }
            )
        }
    }

    fun getEvaluationsWithinBoundariesAlongUserDirection(latitude: Double, longitude: Double, radius: Double): Flux<Leg> {
        val userNearestNodeId = mapServices.findNearestNode(Coordinate(latitude, longitude))
        return if (userNearestNodeId != null) {
            queriesManager.getEvaluationsWithinBoundariesAlongUserDirection(radius, userNearestNodeId)
        } else return Flux.empty()
    }

    private fun mergeObstaclesMaps(first: Map<ObstacleType, List<Coordinate>>,
                                   second: Map<ObstacleType, List<Coordinate>>): Map<ObstacleType, List<Coordinate>> {
        return (first.asSequence() + second.asSequence())
                .distinct()
                .groupBy({ it.key }, { it.value })
                .mapValues { (_, values) -> values.flatten() }
    }

}
