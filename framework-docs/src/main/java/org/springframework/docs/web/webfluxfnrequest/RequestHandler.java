/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webfluxfnrequest;

import reactor.core.publisher.Mono;

import org.springframework.web.reactive.function.server.ServerRequest;

public class RequestHandler {

	public void bind(ServerRequest request) {
		// tag::snippet[]
		Mono<Pet> pet = request.bind(Pet.class, dataBinder -> dataBinder.setAllowedFields("name"));
		// end::snippet[]
	}

	record Pet(String name) { }

}
