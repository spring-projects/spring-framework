/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webfluxfnresponse

import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueWithTypeAndAwait

class ResponseHandler {

	suspend fun createResponse(): ServerResponse {
		// tag::snippet[]
		val person: Person = getPerson()
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValueWithTypeAndAwait<Person>(person)
		// end::snippet[]
	}

	fun getPerson() = Person("foo")

	data class Person(val name: String)
}
