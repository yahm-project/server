package it.unibo.yahm.server

import it.unibo.yahm.server.repositories.WaypointRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ServerApplicationTests {
	@Autowired
	private val repository: WaypointRepository? = null

	@Test
	fun contextLoads() {
	}

}
