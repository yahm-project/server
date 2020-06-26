package it.unibo.yahm.server.handlers

import it.unibo.yahm.server.commons.ApplicationConfig
import it.unibo.yahm.server.controllers.RoadsController
import it.unibo.yahm.server.entities.*
import it.unibo.yahm.server.maps.MapServices
import it.unibo.yahm.server.utils.DBQueries
import it.unibo.yahm.server.utils.TestUtils
import org.junit.jupiter.api.BeforeAll
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
import reactor.core.publisher.EmitterProcessor
import java.time.Duration


@Testcontainers
@ReactiveDataNeo4jTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestAddEvaluationsObservableHandler {

    companion object {
        @Container
        private val neo4jContainer: Neo4jContainer<*> = Neo4jContainer<Nothing>("neo4j:4.0")
        val firstNodeToInsertCoordinate = Coordinate(44.1428248, 12.2696954)
        val secondNodeToInsertCoordinate = Coordinate(44.142871, 12.2705644)
        val obstacleToInsertCoordinate = Coordinate(44.142829, 12.270093)
        val firstSnappedNodeCoordinate = Coordinate(44.1428248, 12.2696431)
        val secondSnappedNodeCoordinate = Coordinate(44.1428626, 12.2705635)
        val thirdSnappedNodeCoordinate = Coordinate(44.1429709, 12.2721559)
        val SnappedObstacleCoordinate = Coordinate(44.14284323939116, 12.270092061915902)
        @DynamicPropertySource
        fun neo4jProperties(registry: DynamicPropertyRegistry) {
            TestUtils.neo4jProperties(registry, neo4jContainer)
        }
    }

    @BeforeAll
    fun setup(@Autowired client: ReactiveNeo4jClient) {
    }

    private fun generateEvaluation(): Evaluations{
        return  Evaluations(
                listOf(firstNodeToInsertCoordinate,
                        secondNodeToInsertCoordinate),
                listOf(1, 2),
                listOf(50.0,50.0),
                listOf(
                        RoadsController.PositionAndObstacleType(
                                obstacleToInsertCoordinate, ObstacleType.POTHOLE
                        )
                ),
                listOf(Quality.OK)
        )
    }

    private fun getStoredEvaluations(client: ReactiveNeo4jClient):List<Leg>? {
        return client.query("""
            MATCH path = (a:Node)-[:LEG *2]->(b:Node)
            UNWIND NODES(path) AS n WITH path, size(collect(DISTINCT n)) AS testLength WHERE testLength = LENGTH(path) + 1
            WITH relationships(path) as rel_arr
            UNWIND rel_arr as rel
            RETURN DISTINCT startNode(rel) as begin, rel as leg, endNode(rel) as end
        """.trimIndent())
                .fetchAs<Leg>()
                .mappedBy { _, record -> DBQueries.legFromRecord(record) }
                .all()
                .collectList()
                .block(Duration.ofSeconds(2))
    }

    @Test
    fun testSimpleRoadSegmentInsertion(@Autowired client: ReactiveNeo4jClient,
                                       @Autowired applicationConfig: ApplicationConfig) {
        val inputRequestStream = EmitterProcessor.create<Evaluations>()
        val evaluationToInsert = generateEvaluation()
        AddEvaluationsObservableHandler(inputRequestStream, MapServices(applicationConfig), DBQueries(client)).observe()
        inputRequestStream.onNext(evaluationToInsert)
        Thread.sleep(2000)
        val firstLeg = getStoredEvaluations(client)?.get(0)
        val secondLeg = getStoredEvaluations(client)?.get(1)
        assert(firstLeg!!.from.coordinates == firstSnappedNodeCoordinate
        ) { "The start coordinates of the first street segment do not correspond to the correct ones [obtained: ${firstLeg.from.coordinates}, expected: $firstSnappedNodeCoordinate]" }
        assert(firstLeg.to.coordinates == secondSnappedNodeCoordinate)
        { "The end coordinates of the first street segment do not correspond to the correct ones" }
        assert(secondLeg!!.from.coordinates == secondSnappedNodeCoordinate)
        { "The start coordinates of the second street segment do not correspond to the correct ones" }
        assert(secondLeg.to.coordinates == thirdSnappedNodeCoordinate)
        { "The end coordinates of the second street segment do not correspond to the correct ones" }
        assert(firstLeg.quality == 2)
        { "The quality of the first street segment does not correspond to the correct one" }
        assert(secondLeg.quality == 2)
        { "The quality of the second street segment does not correspond to the correct one" }
        assert(firstLeg.obstacles[ObstacleType.POTHOLE]?.get(0) == SnappedObstacleCoordinate)
        { "The obstacle of the first street segment does not correspond to the correct one" }
    }
}
