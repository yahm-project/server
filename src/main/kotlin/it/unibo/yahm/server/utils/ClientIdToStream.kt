package it.unibo.yahm.server.utils

import it.unibo.yahm.server.controllers.StreamSegmentController
import it.unibo.yahm.server.entities.Segment
import reactor.core.publisher.EmitterProcessor

class ClientIdToStream() {
    companion object {
        private val clientIdToStream: MutableMap<String, EmitterProcessor<Segment>> = HashMap()
        @Synchronized
        fun getStreamForClient(clientId: String): EmitterProcessor<Segment> {
            return clientIdToStream.getOrElse(clientId,
                    {
                        val newStreamForClient: EmitterProcessor<Segment> = EmitterProcessor.create()
                        StreamSegmentController(newStreamForClient).observe()
                        clientIdToStream[clientId] = newStreamForClient
                        newStreamForClient
                    })
        }
    }
}