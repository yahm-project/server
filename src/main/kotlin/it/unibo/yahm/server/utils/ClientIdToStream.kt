package it.unibo.yahm.server.utils

import it.unibo.yahm.server.controllers.StreamSegmentController
import it.unibo.yahm.server.entities.Leg
import reactor.core.publisher.EmitterProcessor

class ClientIdToStream() {
    companion object {
        private val clientIdToStream: MutableMap<String, EmitterProcessor<Leg>> = HashMap()
        @Synchronized
        fun getStreamForClient(clientId: String): EmitterProcessor<Leg> {
            return clientIdToStream.getOrElse(clientId,
                    {
                        val newStreamForClient: EmitterProcessor<Leg> = EmitterProcessor.create()
                        StreamSegmentController(newStreamForClient).observe()
                        clientIdToStream[clientId] = newStreamForClient
                        newStreamForClient
                    })
        }
    }
}