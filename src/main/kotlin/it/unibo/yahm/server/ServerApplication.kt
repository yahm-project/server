package it.unibo.yahm.server

import org.neo4j.springframework.data.repository.config.EnableNeo4jRepositories
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan("it.unibo.yahm.*")
@EnableNeo4jRepositories("it.unibo.yahm.repository.*")
class ServerApplication

fun main(args: Array<String>) {
    runApplication<ServerApplication>(*args)
}
