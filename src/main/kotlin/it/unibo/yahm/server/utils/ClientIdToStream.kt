package it.unibo.yahm.server.utils

import it.unibo.yahm.server.controllers.InputStreamLegController
import it.unibo.yahm.server.controllers.RoadsController.ClientLegInfo
import it.unibo.yahm.server.maps.MapServices
import org.neo4j.springframework.data.core.ReactiveNeo4jClient
import reactor.core.publisher.EmitterProcessor

class ClientIdToStream() {
    companion object {
        private val clientIdToStream: MutableMap<String, EmitterProcessor<ClientLegInfo>> = HashMap()
        @Synchronized
        fun getStreamForClient(clientId: String, mapServices: MapServices, client: ReactiveNeo4jClient): EmitterProcessor<ClientLegInfo> {
            return clientIdToStream.getOrElse(clientId,
                    {
                        val newStreamForClient: EmitterProcessor<ClientLegInfo> = EmitterProcessor.create()
                        InputStreamLegController(newStreamForClient, mapServices, client).observe()
                        clientIdToStream[clientId] = newStreamForClient
                        newStreamForClient
                    })
        }
    }
}