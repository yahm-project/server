package it.unibo.yahm.server.handlers

import it.unibo.yahm.server.commons.ApplicationConfig
import it.unibo.yahm.server.entities.Coordinate
import it.unibo.yahm.server.maps.MapServices
import it.unibo.yahm.server.utils.DBQueries
import it.unibo.yahm.server.utils.TestUtils
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeAll
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
    }

    private val p = Coordinate( 43.136783, 12.224645)
    private val a = Coordinate(44.138849, 12.239079)
    private val b = Coordinate(44.138284, 12.242297)
    private val c = Coordinate(44.138053, 12.243385)
    private val d = Coordinate(44.137321, 12.245273)

    @BeforeAll
    fun setup(@Autowired client: ReactiveNeo4jClient) {
        // setup database
        client.query("MATCH (a: Node)-[r: LEG]->(b: Node) DELETE r, a, b").run().block()
        client.query("MATCH (a: Node) DELETE a").run().block()
    }

    @Test
    fun testDataRetrievalWithinArea(@Autowired client: ReactiveNeo4jClient, @Autowired applicationConfig: ApplicationConfig) {
        val handler = GetEvaluationsHandler(DBQueries(client), MapServices(applicationConfig))

        client.query("CREATE " +
                "(p:Node{coordinates: point({ latitude: ${p.latitude}, longitude: ${p.longitude}})})," +
                "(a:Node{coordinates: point({ latitude: ${a.latitude}, longitude: ${a.longitude}})}), " +
                "(b:Node{coordinates: point({ latitude: ${b.latitude}, longitude: ${b.longitude}})})," +
                "(c:Node{coordinates: point({ latitude: ${c.latitude}, longitude: ${c.longitude}})}), " +
                "(d:Node{coordinates: point({ latitude: ${d.latitude}, longitude: ${d.longitude}})})," +
                "(p)-[:LEG {quality: 0.0}]->(a), " +
                "(a)-[:LEG {quality: 0.0}]->(b), " +
                "(b)-[:LEG {quality: 0.0}]->(c), " +
                "(c)-[:LEG {quality: 0.0}]->(d), " +
                "(p)<-[:LEG {quality: 0.0}]-(a), " +
                "(a)<-[:LEG {quality: 0.0}]-(b), " +
                "(b)<-[:LEG {quality: 0.0}]-(c), " +
                "(c)<-[:LEG {quality: 0.0}]-(d)").run().block()

        handler.getEvaluationWithinRadius( a.latitude, a.longitude,  1000.0).collectList().block()?.any{ leg -> leg.from.coordinates == p || leg.to.coordinates == p  }?.let { assertFalse(it) }
    }

}