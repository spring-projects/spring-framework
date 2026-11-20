/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webfluxfnhandlervalidation

import org.springframework.docs.web.webfluxfnhandlerclasses.Person
import org.springframework.docs.web.webfluxfnhandlerclasses.PersonRepository
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.server.ServerWebInputException

// tag::snippet[]
class PersonHandler(private val repository: PersonRepository) {

	// Create Validator instance
	private val validator = PersonValidator()

	suspend fun createPerson(request: ServerRequest): ServerResponse {
		val person = request.awaitBody<Person>()
		// Apply validation
		validate(person)
		repository.savePerson(person)
		return ServerResponse.ok().buildAndAwait()
	}

	private fun validate(person: Person) {
		val errors: Errors = BeanPropertyBindingResult(person, "person")
		validator.validate(person, errors)
		if (errors.hasErrors()) {
			// Raise exception for a 400 response
			throw ServerWebInputException(errors.toString())
		}
	}
}
// end::snippet[]

