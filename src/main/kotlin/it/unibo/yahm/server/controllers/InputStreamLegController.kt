package it.unibo.yahm.server.controllers

import it.unibo.yahm.server.entities.Node
import it.unibo.yahm.server.entities.Quality
import it.unibo.yahm.server.maps.MapServices
import org.neo4j.springframework.data.core.ReactiveNeo4jClient
import reactor.core.publisher.EmitterProcessor
import it.unibo.yahm.server.controllers.RoadsController.ClientLegInfo
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration

class InputStreamLegController(private val streamToObserve: EmitterProcessor<ClientLegInfo>, private val mapServices: MapServices, private val client: ReactiveNeo4jClient) {

    fun observe() {
        fun getFromNodeToNodePair(nodes: List<Node>): Flux<Pair<Node, Node>> {
            val fromToNodesPair: MutableList<Pair<Node, Node>> = mutableListOf()
            nodes.forEachIndexed { index, node ->
                if (index < nodes.size - 1) {
                    fromToNodesPair.add(Pair(node, nodes[index+1]))
                }
            }
            return Flux.fromIterable(fromToNodesPair)
        }

        streamToObserve
                .bufferTimeout(MAX_BUFFERED_ELEMENT, Duration.ofSeconds(MAX_BUFFERED_SECONDS))
                .subscribeOn(Schedulers.single())
                .flatMap { it ->
                    val snappedNodesForOriginalNodes = Flux.fromIterable(mapServices
                            .snapToRoadNodes(
                                    coordinates = it.map { it.coordinate },
                                    /*it.map { it.timestamp },*/
                                    radiuses = it.map { it.radius }
                            )!!)
                    snappedNodesForOriginalNodes.index().flatMap { snappedNodesWithIndex ->
                        val quality = it[snappedNodesWithIndex.t1.toInt()].quality!!
                        val fromNodeToNodePair = getFromNodeToNodePair(snappedNodesWithIndex.t2)
                        fromNodeToNodePair.flatMap {
                            createOrUpdateQuality(it.first, it.second, quality)
                        }
                    }
                }.subscribe {
                    println(it)
                }
    }

    private fun createOrUpdateQuality(firstNode: Node, secondNode: Node, quality: Quality): Mono<Boolean> {
        val qualityValue = quality.value
        println(firstNode.toString() + " "+ secondNode)
        return client.query("MERGE (a:Node{id:${firstNode.id}, coordinates: point({ longitude: ${firstNode.coordinates.longitude}, latitude:${firstNode.coordinates.latitude}})}) \n" +
                "MERGE (b:Node{id:${secondNode.id}, coordinates: point({ longitude: ${secondNode.coordinates.longitude}, latitude:${secondNode.coordinates.latitude}})}) \n" +
                "MERGE (a)-[s:Leg]->(b)\n" +
                "ON CREATE SET s.quality = $qualityValue\n" +
                "ON MATCH SET s.quality = s.quality * (1 - $NEW_QUALITY_WEIGHT) + $qualityValue * $NEW_QUALITY_WEIGHT").run().map { true }
    }

    companion object {
        const val NEAR_POINT_DISTANCE = 20
        const val NEW_QUALITY_WEIGHT = 0.7
        const val MAX_BUFFERED_ELEMENT = 20
        const val MAX_BUFFERED_SECONDS: Long = 5
    }
}