package it.unibo.yahm.server.repositories

import it.unibo.yahm.server.entities.PersonEntity
import org.neo4j.springframework.data.repository.ReactiveNeo4jRepository
import reactor.core.publisher.Flux

interface PersonRepository: ReactiveNeo4jRepository<PersonEntity, String> {
    fun findAllByName(name: String): Flux<PersonEntity>
}
