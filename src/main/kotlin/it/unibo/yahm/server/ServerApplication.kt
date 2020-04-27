package it.unibo.yahm.server

import it.unibo.yahm.server.commons.ApplicationConfig
import org.neo4j.springframework.data.repository.config.EnableNeo4jRepositories
import org.neo4j.springframework.data.types.GeographicPoint2d
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableNeo4jRepositories(repositoryImplementationPostfix = "Impl")
@EnableConfigurationProperties(ApplicationConfig::class)
class ServerApplication

fun main(args: Array<String>) {
    runApplication<ServerApplication>(*args)
}


