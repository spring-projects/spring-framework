/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webfluxfnhandlerclasses

import kotlinx.coroutines.flow.Flow
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyAndAwait
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait

// tag::snippet[]
class PersonHandler(private val repository: PersonRepository) {

	// listPeople is a handler function that returns all Person objects found
	// in the repository as JSON
	suspend fun listPeople(request: ServerRequest): ServerResponse {
		val people: Flow<Person> = repository.allPeople()
		return ServerResponse.ok().contentType(APPLICATION_JSON).bodyAndAwait(people)
	}

	// createPerson is a handler function that stores a new Person contained
	// in the request body.
	// Note that PersonRepository.savePerson(Person) returns Mono<Void>: an empty
	// Mono that emits a completion signal when the person has been read from the
	// request and stored. So we use the build(Publisher<Void>) method to send a
	// response when that completion signal is received (that is, when the Person
	// has been saved)
	suspend fun createPerson(request: ServerRequest): ServerResponse {
		val person = request.awaitBody<Person>()
		repository.savePerson(person)
		return ServerResponse.ok().buildAndAwait()
	}

	// getPerson is a handler function that returns a single person, identified by
	// the id path variable. We retrieve that Person from the repository and create
	// a JSON response, if it is found. If it is not found, we use switchIfEmpty(Mono<T>)
	// to return a 404 Not Found response.
	suspend fun getPerson(request: ServerRequest): ServerResponse {
		val personId = request.pathVariable("id").toInt()
		return repository.getPerson(personId)?.let { ServerResponse.ok().contentType(APPLICATION_JSON).bodyValueAndAwait(it) }
				?: ServerResponse.notFound().buildAndAwait()

	}
}
// end::snippet[]
