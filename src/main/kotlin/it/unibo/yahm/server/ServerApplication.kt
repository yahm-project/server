package it.unibo.yahm.server

import it.unibo.yahm.server.commons.ApplicationConfig
import org.neo4j.springframework.data.repository.config.EnableNeo4jRepositories
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

import org.springframework.web.filter.CorsFilter


@SpringBootApplication
@EnableNeo4jRepositories(repositoryImplementationPostfix = "Impl")
@EnableConfigurationProperties(ApplicationConfig::class)
class ServerApplication {
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

