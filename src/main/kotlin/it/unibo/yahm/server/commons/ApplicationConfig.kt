package it.unibo.yahm.server.commons

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("it.unibo.yahm")
class ApplicationConfig {

    var OsrmBackend: String = "http://localhost"
    var OverpassAPI: String = "http://localhost"

}