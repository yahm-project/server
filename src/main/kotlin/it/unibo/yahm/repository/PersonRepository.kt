package it.unibo.yahm.repository

import it.unibo.yahm.model.Person
import org.neo4j.driver.internal.shaded.reactor.core.publisher.Flux
import org.neo4j.springframework.data.repository.ReactiveNeo4jRepository
import org.springframework.data.repository.query.Param;

interface PersonRepository: ReactiveNeo4jRepository<Person, Long> {
    fun findByLastName(@Param("name") name: String): Flux<Person>
    fun findByFirstName(@Param("name") name: String): Flux<Person>
}