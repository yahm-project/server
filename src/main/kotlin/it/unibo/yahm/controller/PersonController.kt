package it.unibo.yahm.controller

import it.unibo.yahm.model.Person
import it.unibo.yahm.repository.PersonRepository
import org.neo4j.driver.internal.shaded.reactor.core.publisher.Flux
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/people")
class PersonController(personRepository: PersonRepository) {
    val personRepository = personRepository

    @GetMapping("/byLastName")
    fun findByLastName(@RequestParam lastName: String): Flux<Person> {
        return personRepository.findByLastName(lastName)
    }
}