package it.unibo.yahm.server.maps

import it.unibo.yahm.server.commons.ApplicationConfig
import it.unibo.yahm.server.entities.Coordinate
import it.unibo.yahm.server.entities.Node
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import org.springframework.web.client.postForObject


@Component
class MapServices(private val applicationConfig: ApplicationConfig) {

    private val version = "v1"
    private val profile = "car"

    private val restTemplate = RestTemplate()
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    fun snapToRoadNodes(coordinates: List<Coordinate>, timestamps: List<Long> = emptyList(),
                        radiuses: List<Double> = emptyList()): List<List<Node>>? {
        val options = MatchService.Options(annotations = MatchService.Annotations.NODES)
        val legsNodes = snapToRoad(coordinates, timestamps, radiuses, options = options)?.matchings?.flatMap { matching ->
            matching.legs.map { it.annotation!!.nodes!! }
        } ?: return null

        val nodes = findNodesCoordinates(legsNodes.flatten().toSet()) ?: return null
        return legsNodes.map { leg ->
            leg.map { nodeId -> nodes.find { it.id == nodeId }!! }
        }
    }

    fun snapToRoad(coordinates: List<Coordinate>, timestamps: List<Long> = emptyList(),
                   radiuses: List<Double> = emptyList(), waypoints: List<Long> = emptyList(),
                   options: MatchService.Options = MatchService.Options()): MatchService.Result? {
        val sb = osrmUrl("match", coordinates, radiuses)
        if (timestamps.isNotEmpty()) {
            sb.append(timestamps.joinToString(";", prefix = "&timestamps="))
        }
        if (waypoints.isNotEmpty()) {
            sb.append(timestamps.joinToString(";", prefix = "&waypoints="))
        }
        sb.append(options.toUrlOptions())

        return try {
            return restTemplate.getForObject<MatchService.Result>(sb.toString())
        } catch (badRequest: HttpClientErrorException.BadRequest) {
            if (badRequest.responseBodyAsString != "{\"code\":\"NoSegment\"}") {
                logger.error("Match service invalid query: ${badRequest.responseBodyAsString}")
                logger.info("Query: $sb")
            }
            null
        } catch (e: Exception) {
            logger.error("Match service error: ${e.message} ")
            null
        }
    }

    fun findNodesCoordinates(ids: Set<Long>): Set<Node>? {
        val query = "[out:json]; node(id:${ids.joinToString(", ")});out;"
        return try {
            val result = restTemplate.postForObject<OverpassService.Result>(applicationConfig.OverpassAPI, query)
            result.elements.filter { it.type == "node" }.map { Node(it.id, Coordinate(it.lat, it.lon)) }.toSet()
        } catch (e: Exception) {
            logger.error("Overpass API error: ${e.message} ")
            null
        }
    }

    fun findNearestNode(coordinate: Coordinate, radius: Double? = null): Long? {
        return findNearestNodes(coordinate, radius)?.waypoints?.filter { it.nodes.isNotEmpty() }?.map { it.nodes[0] }
                ?.firstOrNull()
    }

    fun findNearestNodes(coordinate: Coordinate, radius: Double? = null, number: Int = 1): NearestService.Result? {
        val sb = osrmUrl("nearest", listOf(coordinate), if (radius != null) listOf(radius) else emptyList())
        sb.append("&number=").append(number)
        return try {
            return restTemplate.getForObject<NearestService.Result>(sb.toString())
        } catch (badRequest: HttpClientErrorException.BadRequest) {
            if (badRequest.responseBodyAsString != "{\"code\":\"NoSegment\"}") {
                logger.error("Match service invalid query: ${badRequest.responseBodyAsString}")
            }

            null
        } catch (e: Exception) {
            logger.error("Match service error: ${e.message} ")
            null
        }
    }

    private fun osrmUrl(serviceName: String, coordinates: List<Coordinate>,
                        radiuses: List<Double> = emptyList()): StringBuilder {
        val sb = StringBuilder()
        sb.append(applicationConfig.OsrmBackend)
        if (!applicationConfig.OsrmBackend.endsWith("/")) {
            sb.append("/")
        }
        sb.append("${serviceName}/${version}/${profile}/")
        sb.append(coordinates.joinToString(";") { it.longitude.toString() + "," + it.latitude.toString() })
        sb.append(".json?generate_hints=false")
        if (radiuses.isNotEmpty()) {
            sb.append("&radiuses=")
            sb.append(radiuses.joinToString(";"))
        }

        return sb
    }

}
