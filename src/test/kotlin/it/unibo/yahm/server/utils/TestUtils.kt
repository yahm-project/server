package it.unibo.yahm.server.utils

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.Neo4jContainer

object TestUtils {

    fun neo4jProperties(registry: DynamicPropertyRegistry, neo4jContainer: Neo4jContainer<*>) {
        registry.add("org.neo4j.driver.uri") { neo4jContainer.getBoltUrl() }
        registry.add("org.neo4j.driver.authentication.username") { "neo4j" }
        registry.add("org.neo4j.driver.authentication.password") { neo4jContainer.getAdminPassword() }
    }

}
