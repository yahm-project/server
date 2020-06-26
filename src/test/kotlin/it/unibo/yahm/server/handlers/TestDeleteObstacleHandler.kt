package it.unibo.yahm.server.handlers

import it.unibo.yahm.server.entities.Coordinate
import it.unibo.yahm.server.entities.Node
import it.unibo.yahm.server.entities.ObstacleType
import it.unibo.yahm.server.utils.DBQueries
import it.unibo.yahm.server.utils.TestUtils
import it.unibo.yahm.server.utils.calculateIntermediatePoint
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.neo4j.springframework.boot.test.autoconfigure.data.ReactiveDataNeo4jTest
import org.neo4j.springframework.data.core.ReactiveNeo4jClient
import org.neo4j.springframework.data.core.fetchAs
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.Neo4jContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Duration


@Testcontainers
@ReactiveDataNeo4jTest
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class TestDeleteObstacleHandler {

    companion object {
        @Container
        private val neo4jContainer: Neo4jContainer<*> = Neo4jContainer<Nothing>("neo4j:4.0")

        @DynamicPropertySource
        fun neo4jProperties(registry: DynamicPropertyRegistry) {
            TestUtils.neo4jProperties(registry, neo4jContainer)
        }

        private val TIMEOUT = Duration.ofSeconds(10)

        private val TEST_START_NODE = Node(1, Coordinate(44.054153, 12.582221))
        private val TEST_END_NODE = Node(2, Coordinate(44.057816, 12.578627))
        private val TEST_OBSTACLES = mapOf(ObstacleType.POTHOLE to listOf(0.2, 0.6), ObstacleType.SPEED_BUMP to listOf(0.3))
    }

    @BeforeEach
    fun setup(@Autowired client: ReactiveNeo4jClient) {
        client.query("""
            CREATE (:Node{id:1,coordinates:point({
                longitude:${TEST_START_NODE.coordinates.longitude},
                latitude:${TEST_START_NODE.coordinates.latitude}
            })})
            -[:LEG{quality:3.0,POTHOLE:[0.2,0.6],SPEED_BUMP:[0.3]}]->
            (:Node{id:2,coordinates:point({
                longitude:${TEST_END_NODE.coordinates.longitude},
                latitude:${TEST_END_NODE.coordinates.latitude}
            })})
        """.trimIndent()).run().block(TIMEOUT)
    }

    @Test
    fun testDeleteObstacles(@Autowired client: ReactiveNeo4jClient) {
        val handler = DeleteObstacleHandler(DBQueries(client))

        assertFalse(handler.deleteObstacle(0.0, 0.0, ObstacleType.POTHOLE, 0, 0).block(TIMEOUT) ?: false)
        checkObstacles(client, TEST_OBSTACLES)
        assertFalse(handler.deleteObstacle(0.0, 0.0, ObstacleType.POTHOLE, 1, 0).block(TIMEOUT) ?: false)
        checkObstacles(client, TEST_OBSTACLES)

        val pothole1 = calculateIntermediatePoint(TEST_START_NODE.coordinates, TEST_END_NODE.coordinates, 0.2)
        assertTrue(handler.deleteObstacle(pothole1.latitude, pothole1.longitude, ObstacleType.POTHOLE, 0, 1).block(TIMEOUT) ?: false)
        checkObstacles(client, mapOf(ObstacleType.POTHOLE to listOf(0.6), ObstacleType.SPEED_BUMP to listOf(0.3)))

        val pothole2 = calculateIntermediatePoint(TEST_START_NODE.coordinates, TEST_END_NODE.coordinates, 0.6)
        assertTrue(handler.deleteObstacle(pothole2.latitude, pothole2.longitude, ObstacleType.POTHOLE, 0, 1).block(TIMEOUT) ?: false)
        checkObstacles(client, mapOf(ObstacleType.POTHOLE to emptyList(), ObstacleType.SPEED_BUMP to listOf(0.3)))

        val speedBump = calculateIntermediatePoint(TEST_START_NODE.coordinates, TEST_END_NODE.coordinates, 0.3)
        assertTrue(handler.deleteObstacle(speedBump.latitude, speedBump.longitude, ObstacleType.SPEED_BUMP, 0, 1).block(TIMEOUT) ?: false)
        checkObstacles(client, mapOf(ObstacleType.POTHOLE to emptyList(), ObstacleType.SPEED_BUMP to emptyList()))
    }

    private fun checkObstacles(client: ReactiveNeo4jClient, expected: Map<ObstacleType, List<Double>>) {
        val result = client.query("""
            MATCH (a:Node)-[r:LEG]->(b:Node) WHERE a.id = ${TEST_START_NODE.id} AND b.id = ${TEST_END_NODE.id} RETURN r
        """.trimIndent())
            .fetchAs<Map<ObstacleType, List<Double>>>()
            .mappedBy { _, record ->
                val leg = record["r"].asRelationship()
                expected.keys.map {
                    it to if (leg.containsKey(it.toString())) leg[it.toString()].asList { v -> v.asDouble()} else emptyList()
                }.toMap()
            }.first().block(TIMEOUT)

        assertNotNull(result)
        assertEquals(expected, result)
    }

}
