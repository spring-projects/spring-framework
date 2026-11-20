/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webfluxfnrequest

import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.bindAndAwait

class RequestHandler {

	suspend fun bind(request: ServerRequest) {
		// tag::snippet[]
		val pet: Pet? = request.bindAndAwait<Pet>{ dataBinder -> dataBinder.setAllowedFields("name") }
		// end::snippet[]
	}

	data class Pet(val name: String)
}
