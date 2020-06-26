package it.unibo.yahm.server.handlers

import it.unibo.yahm.server.entities.ObstacleType
import it.unibo.yahm.server.utils.DBQueries
import it.unibo.yahm.server.utils.TestUtils
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.neo4j.springframework.boot.test.autoconfigure.data.ReactiveDataNeo4jTest
import org.neo4j.springframework.data.core.ReactiveNeo4jClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.Neo4jContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers


@Testcontainers
@ReactiveDataNeo4jTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestDeleteObstacleHandler {

    companion object {
        @Container
        private val neo4jContainer: Neo4jContainer<*> = Neo4jContainer<Nothing>("neo4j:4.0")

        @DynamicPropertySource
        fun neo4jProperties(registry: DynamicPropertyRegistry) {
            TestUtils.neo4jProperties(registry, neo4jContainer)
        }
    }

    @BeforeAll
    fun setup(@Autowired client: ReactiveNeo4jClient) {
        // setup database
    }

    @Test
    fun testDeleteObstacles(@Autowired client: ReactiveNeo4jClient) {
        val handler = DeleteObstacleHandler(DBQueries(client))

        handler.deleteObstacle(0.0, 0.0, ObstacleType.POTHOLE, 10, 11)
    }

}
