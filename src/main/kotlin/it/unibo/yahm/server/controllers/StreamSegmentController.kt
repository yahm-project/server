package it.unibo.yahm.server.controllers


import it.unibo.yahm.server.entities.Leg
import it.unibo.yahm.server.repositories.WaypointRepository
import it.unibo.yahm.server.utils.distanceTo
import org.neo4j.springframework.data.types.GeographicPoint2d
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.EmitterProcessor
import java.util.*

class StreamSegmentController(private val streamToObserve: EmitterProcessor<Leg>) {

    @Autowired
    private val repository: WaypointRepository? = null

    fun observe() {
       val toSnapPoint: MutableList<GeographicPoint2d> = mutableListOf()
        var toRelateToPoint = Optional.empty<GeographicPoint2d>()
        streamToObserve.subscribe {
            val fromPointCoordinates = it.from.coordinates
            val nearestWaypoint = repository!!.findClosestPoints(fromPointCoordinates.longitude, fromPointCoordinates.latitude)
                    .next()
                    .filter { nearestWayPoint -> fromPointCoordinates.distanceTo(nearestWayPoint.coordinates) <= NEAR_POINT_DISTANCE }
                    .blockOptional()
            if(nearestWaypoint.isEmpty) {
                toSnapPoint.add(fromPointCoordinates)
            } else {
                if(toSnapPoint.size > 0) {
                    //snap each point
                    if(toRelateToPoint.isEmpty) {
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
        }
    }

    companion object {
        const val NEAR_POINT_DISTANCE = 20
    }
}