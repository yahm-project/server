package it.unibo.yahm.server

import org.neo4j.springframework.data.repository.config.EnableNeo4jRepositories
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableNeo4jRepositories(repositoryImplementationPostfix = "Impl")
class ServerApplication

fun main(args: Array<String>) {
    runApplication<ServerApplication>(*args)
}

