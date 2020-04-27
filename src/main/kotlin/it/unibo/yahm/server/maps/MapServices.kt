package it.unibo.yahm.server.maps

import it.unibo.yahm.server.commons.ApplicationConfig
import it.unibo.yahm.server.maps.MatchService.MatchOptions
import it.unibo.yahm.server.maps.MatchService.RoadSnaps
import org.neo4j.springframework.data.types.GeographicPoint2d
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject

@Component
class MapServices(private val applicationConfig: ApplicationConfig) {

    private val restTemplate = RestTemplate()
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    fun snapToRoadCoordinates(coordinates: List<GeographicPoint2d>): List<GeographicPoint2d>? =
            snapToRoadRaw(coordinates)?.matchings?.flatMap { matching ->
                PolylineUtils.decode(matching.geometry!!).map {
                    GeographicPoint2d(it.latitude, it.longitude)
                }
            }

    fun snapToRoadRaw(coordinates: List<GeographicPoint2d>, timestamps: List<Long> = emptyList(),
                      radius: List<Double> = emptyList(), waypoints: List<Long> = emptyList(),
                      hints: List<String> = emptyList(), matchOptions: MatchOptions = MatchOptions()): RoadSnaps? {
        val sb = StringBuilder()
        sb.append(applicationConfig.OsrmBackend)
        if (!applicationConfig.OsrmBackend.endsWith("/")) {
            sb.append("/")
        }
        sb.append("match/v1/car/")
        sb.append(coordinates.joinToString(";") { it.latitude.toString() + "," + it.longitude.toString() })
        sb.append(".json?geometries=polyline6&")
        for (coll in listOf(timestamps, radius, waypoints, hints)) {
            if (coll.isNotEmpty()) {
                sb.append(coll.joinToString(";", postfix = "&"))
            }
        }
        sb.append(matchOptions.toUrlOptions())

        return try {
            return restTemplate.getForObject<RoadSnaps>(sb.toString())
        } catch (badRequest: HttpClientErrorException.BadRequest) {
            logger.error("Match service invalid query: ${badRequest.message}")
            null
        } catch (e: Exception) {
            logger.error("Match service error: ${e.message} ")
            null
        }
    }

}
