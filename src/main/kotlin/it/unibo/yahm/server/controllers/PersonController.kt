package it.unibo.yahm.server.controllers

import it.unibo.yahm.server.entities.PersonEntity
import it.unibo.yahm.server.repositories.PersonRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/people")
class PersonController(val repository: PersonRepository) {

    @GetMapping("/by-name")
    fun byName(@RequestParam name: String): Flux<PersonEntity> {
        return repository.findAllByName(name)
    }
}
