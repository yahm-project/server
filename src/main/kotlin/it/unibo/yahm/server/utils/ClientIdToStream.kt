package it.unibo.yahm.server.utils

import it.unibo.yahm.server.controllers.InputStreamLegController
import it.unibo.yahm.server.entities.Leg
import it.unibo.yahm.server.maps.MapServices
import org.neo4j.springframework.data.core.ReactiveNeo4jClient
import reactor.core.publisher.EmitterProcessor

class ClientIdToStream() {
    companion object {
        private val clientIdToStream: MutableMap<String, EmitterProcessor<Leg>> = HashMap()
        @Synchronized
        fun getStreamForClient(clientId: String, mapServices: MapServices, client: ReactiveNeo4jClient): EmitterProcessor<Leg> {
            return clientIdToStream.getOrElse(clientId,
                    {
                        val newStreamForClient: EmitterProcessor<Leg> = EmitterProcessor.create()
                        InputStreamLegController(newStreamForClient, mapServices, client).observe()
                        clientIdToStream[clientId] = newStreamForClient
                        newStreamForClient
                    })
        }
    }
}