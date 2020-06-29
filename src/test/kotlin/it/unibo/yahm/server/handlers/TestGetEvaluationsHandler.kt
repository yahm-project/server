package it.unibo.yahm.server.handlers

import it.unibo.yahm.server.commons.ApplicationConfig
import it.unibo.yahm.server.entities.Coordinate
import it.unibo.yahm.server.entities.ObstacleType
import it.unibo.yahm.server.maps.MapServices
import it.unibo.yahm.server.utils.DBQueries
import it.unibo.yahm.server.utils.TestUtils
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.neo4j.springframework.boot.test.autoconfigure.data.ReactiveDataNeo4jTest
import org.neo4j.springframework.data.core.ReactiveNeo4jClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.Neo4jContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@ReactiveDataNeo4jTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableConfigurationProperties(ApplicationConfig::class)
class TestGetObstacleHandler {

    companion object {
        @Container
        private val neo4jContainer: Neo4jContainer<*> = Neo4jContainer<Nothing>("neo4j:4.0")

        @DynamicPropertySource
        fun neo4jProperties(registry: DynamicPropertyRegistry) {
            TestUtils.neo4jProperties(registry, neo4jContainer)
        }

        private val EXTERNAL_POINT = Coordinate(43.136783, 12.224645)
        private val POINT_A = Coordinate(44.138849, 12.239079)
        private val POINT_B = Coordinate(44.138245, 12.242387)
        private const val POINT_B_NEAREST_ID = 245953123
        private val POINT_C = Coordinate(44.138053, 12.243385)
        private val POINT_D = Coordinate(44.137321, 12.245273)
        private const val AREA_RADIUS = 1000.0

        private val POT_HOLE = Coordinate(latitude = 44.138547011931415, longitude = 12.240733008459769)
        private val SPEED_BUMP = Coordinate(latitude = 44.138547011931415, longitude = 12.240733008459769)
        private val OBSTACLES_ABSOLUTE = mapOf(ObstacleType.POTHOLE to listOf(POT_HOLE), ObstacleType.SPEED_BUMP to listOf(SPEED_BUMP))
        private val OBSTACLES_RELATIVE = mapOf(ObstacleType.POTHOLE to listOf(0.5), ObstacleType.SPEED_BUMP to listOf(0.5))
    }


    @BeforeEach
    fun setup(@Autowired client: ReactiveNeo4jClient) {
        // setup database
        client.query("MATCH (a: Node)-[r: LEG]->(b: Node) DELETE r, a, b").run().block()
        client.query("MATCH (a: Node) DELETE a").run().block()
    }

    @Test
    fun testDataRetrievalWithinArea(@Autowired client: ReactiveNeo4jClient, @Autowired applicationConfig: ApplicationConfig) {
        val handler = GetEvaluationsHandler(DBQueries(client), MapServices(applicationConfig))

        //external_point is outside the area
        client.query("CREATE " +
                "(p:Node{coordinates: point({ latitude: ${EXTERNAL_POINT.latitude}, longitude: ${EXTERNAL_POINT.longitude}})})," +
                "(a:Node{coordinates: point({ latitude: ${POINT_A.latitude}, longitude: ${POINT_A.longitude}})}), " +
                "(b:Node{coordinates: point({ latitude: ${POINT_B.latitude}, longitude: ${POINT_B.longitude}})})," +
                "(p)-[:LEG {quality: 0.0}]->(a), " +
                "(a)-[:LEG {quality: 0.0}]->(b)").run().block()

        val result = handler.getEvaluationWithinRadius(POINT_A.latitude, POINT_A.longitude, AREA_RADIUS).collectList().block()?.map { Pair(it.from.coordinates, it.to.coordinates) }
        assertNotNull(result)
        assertFalse(result!!.any { it.first == EXTERNAL_POINT || it.second == EXTERNAL_POINT })
        assert(result.contains(Pair(POINT_A, POINT_B)))
    }

    @Test
    fun testDataRetrievalAlongUserDirection(@Autowired client: ReactiveNeo4jClient, @Autowired applicationConfig: ApplicationConfig) {
        val handler = GetEvaluationsHandler(DBQueries(client), MapServices(applicationConfig))

        client.query("CREATE " +
                "(a:Node{coordinates: point({ latitude: ${POINT_A.latitude}, longitude: ${POINT_A.longitude}})}), " +
                "(b:Node{id: $POINT_B_NEAREST_ID, coordinates: point({ latitude: ${POINT_B.latitude}, longitude: ${POINT_B.longitude}})})," +
                "(c:Node{coordinates: point({ latitude: ${POINT_C.latitude}, longitude: ${POINT_C.longitude}})}), " +
                "(d:Node{coordinates: point({ latitude: ${POINT_D.latitude}, longitude: ${POINT_D.longitude}})})," +
                "(a)-[:LEG {quality: 0.0}]->(b), " +
                "(b)-[:LEG {quality: 0.0}]->(c), " +
                "(c)-[:LEG {quality: 0.0}]->(d), " +
                "(a)<-[:LEG {quality: 0.0}]-(b), " +
                "(b)<-[:LEG {quality: 0.0}]-(c), " +
                "(c)<-[:LEG {quality: 0.0}]-(d)").run().block()

        //user supposed to be in point b.
        val result = handler.getEvaluationsWithinBoundariesAlongUserDirection(POINT_B.latitude, POINT_B.longitude, AREA_RADIUS).collectList().block()?.map { Pair(it.from.coordinates, it.to.coordinates) }
        assertNotNull(result)
        assert(result!!.containsAll(listOf(Pair(POINT_B, POINT_C), Pair(POINT_C, POINT_D))))
        assertFalse(result.contains(Pair(POINT_A, POINT_B)))
        assertFalse(result.contains(Pair(POINT_C, POINT_B)))
        assertFalse(result.contains(Pair(POINT_D, POINT_C)))
    }

    @Test
    fun testDataRetrievalWithObstacles(@Autowired client: ReactiveNeo4jClient, @Autowired applicationConfig: ApplicationConfig) {
        val handler = GetEvaluationsHandler(DBQueries(client), MapServices(applicationConfig))

        client.query("CREATE " +
                "(a:Node{id: 1, coordinates: point({ latitude: ${POINT_A.latitude}, longitude: ${POINT_A.longitude}})}), " +
                "(b:Node{id: 2, coordinates: point({ latitude: ${POINT_B.latitude}, longitude: ${POINT_B.longitude}})})," +
                "(c:Node{coordinates: point({ latitude: ${POINT_C.latitude}, longitude: ${POINT_C.longitude}})}), " +
                "(a)-[:LEG {quality: 0.0, ${OBSTACLES_RELATIVE.map { "${it.key}:${it.value}" }.joinToString()}}]->(b), " +
                "(b)-[:LEG {quality: 0.0}]->(c)").run().block()


        val result = handler.getEvaluationWithinRadius(POINT_A.latitude, POINT_A.longitude, AREA_RADIUS).collectList().block()
        assertNotNull(result)

        assert(result!!.any { it.from.coordinates == POINT_A && it.to.coordinates == POINT_B && it.obstacles == OBSTACLES_ABSOLUTE })
        assert(result.any { it.from.coordinates == POINT_B && it.to.coordinates == POINT_C && it.obstacles.isEmpty() })
    }


}