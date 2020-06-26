package it.unibo.yahm.server.simulation

import it.unibo.yahm.server.entities.Coordinate
import it.unibo.yahm.server.entities.Evaluations
import it.unibo.yahm.server.entities.Quality
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForObject
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.io.File
import java.time.Duration
import kotlin.system.exitProcess

class FakeClientSimulator(inputSource: File, private val bufferSize: Int = 15,
                          private val delayBetweenRequests: Long = 5) {

    private val url = "http://localhost:8080/roads/evaluations"
    private val reader = inputSource.bufferedReader()
    private val restTemplate = RestTemplate()

    fun doSimulation() {
        Flux.fromStream(reader.lines())
            .subscribeOn(Schedulers.newSingle("it/unibo/yahm/server/simulation"))
            .skip(1) // skip header
            .map { it.split(",") }
            .buffer(bufferSize, bufferSize - 1)
            .delayElements(Duration.ofSeconds(delayBetweenRequests))
            .subscribe({buf ->
                println("Sending elements..")
                restTemplate.postForObject<Void?>(url, Evaluations(
                        buf.map { Coordinate(it[0].toDouble(), it[1].toDouble()) },
                        emptyList(),
                        buf.map { it[2].toDouble() },
                        emptyList(),
                        buf.take(bufferSize - 1).map { Quality.valueOf(it[4]) }
                ))
            }, {
                it.printStackTrace()
            }, {
                reader.close()
            })
    }

}

fun main(args: Array<String>) {
    if (args.size != 1) {
        System.err.println("Provide as first argument the file of stretches")
        exitProcess(1)
    }
    FakeClientSimulator(File(args[0])).doSimulation()
}
