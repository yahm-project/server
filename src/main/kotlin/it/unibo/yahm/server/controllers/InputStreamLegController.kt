package it.unibo.yahm.server.controllers


import it.unibo.yahm.server.entities.Leg
import it.unibo.yahm.server.entities.Node
import it.unibo.yahm.server.entities.Quality
import it.unibo.yahm.server.maps.MapServices
import org.neo4j.springframework.data.core.ReactiveNeo4jClient
import reactor.core.publisher.EmitterProcessor

class InputStreamLegController(private val streamToObserve: EmitterProcessor<Leg>, private val mapServices: MapServices, private val client: ReactiveNeo4jClient) {

    fun observe() {
       /* val toSnapPoint: MutableList<GeographicPoint2d> = mutableListOf()
        var toRelateToPoint = Optional.empty<GeographicPoint2d>()
        streamToObserve.subscribe {
            val fromPointCoordinates = it.from.coordinates
            val nearestWaypoint = repository!!.findClosestPoints(fromPointCoordinates.longitude, fromPointCoordinates.latitude)
                    .next()
                    .filter { nearestWayPoint -> fromPointCoordinates.distanceTo(nearestWayPoint.coordinates) <= NEAR_POINT_DISTANCE }
                    .blockOptional()
            if (nearestWaypoint.isEmpty) {
                toSnapPoint.add(fromPointCoordinates)
            } else {
                if (toSnapPoint.size > 0) {
                    val snappedPoints = mapServices!!.snapToRoadCoordinates(toSnapPoint)
                    if (toRelateToPoint.isEmpty) {
                        val toRelateFromPoint = nearestWaypoint.get()
                        //relate last snapped with toRelateFromPoint
                    } else {
                        //relate last snapped with toRelateToPoint.get()
                        toRelateToPoint = Optional.empty()
                    }
                    toSnapPoint.clear()
                } else {
                    toRelateToPoint = Optional.of(nearestWaypoint.get().coordinates)
                }
            }
        } */
    }

    fun createOrUpdateQuality(firstNode: Node, secondNode: Node, quality: Quality) {

    }

    companion object {
        const val NEAR_POINT_DISTANCE = 20
    }
}