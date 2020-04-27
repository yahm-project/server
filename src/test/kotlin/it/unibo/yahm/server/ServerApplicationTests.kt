package it.unibo.yahm.server

import it.unibo.yahm.server.entities.GeographicPoint
import it.unibo.yahm.server.entities.GeographicPoint2
import it.unibo.yahm.server.entities.Waypoint
import it.unibo.yahm.server.repositories.WaypointRepository
import it.unibo.yahm.server.utils.GeographicPointGenerator
import org.junit.jupiter.api.Test
import org.neo4j.driver.types.Point
import org.neo4j.springframework.data.types.GeographicPoint2d
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
class ServerApplicationTests {
	@Autowired
	private val repository: WaypointRepository? = null

	@Test
	fun contextLoads() {
		/*val initialPoint = GeographicPoint(44.136805, 12.2386659)
		val newPoint = GeographicPointGenerator.generatePoint(initialPoint, 10, 0.0)
		println(newPoint)
		println(initialPoint.distanceTo(newPoint))*/
		val point = GeographicPoint2d(0.0,0.0)
		val entity = Waypoint(null, coordinates = point)
		println("HEREEEEEEEEEEEEEEEEEEEEEEEEE " + point)
		val createdNode = repository!!.save(entity).block()
		println(createdNode)
	}

}
