package it.unibo.yahm.server

import it.unibo.yahm.server.commons.ApplicationConfig
import org.neo4j.springframework.data.core.ReactiveNeo4jClient
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter


@SpringBootApplication
@EnableConfigurationProperties(ApplicationConfig::class)
class ServerApplication(client: ReactiveNeo4jClient) {

    init {
        println("Creating constraints..")
        client.query("CREATE CONSTRAINT unique_id ON (n:Node) ASSERT n.id IS UNIQUE").run().subscribe({
            println("Added ${it.counters().constraintsAdded()} constraints")
        }, {
            println("Constraints already exists")
        })
    }

    @Bean
    fun corsFilter(): CorsFilter? {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration()
        config.allowCredentials = true
        config.addAllowedOrigin("*")
        config.addAllowedHeader("*")
        config.addAllowedMethod("OPTIONS")
        config.addAllowedMethod("GET")
        config.addAllowedMethod("POST")
        config.addAllowedMethod("PUT")
        config.addAllowedMethod("DELETE")
        source.registerCorsConfiguration("/**", config)
        return CorsFilter(source)
    }

}

fun main(args: Array<String>) {
    runApplication<ServerApplication>(*args)
}

