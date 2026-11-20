/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webfluxfnresponse;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public class ResponseHandler {

	public Mono<ServerResponse> createResponse() {
		// tag::snippet[]
		Mono<Person> person = getPerson();
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(person, Person.class);
		// end::snippet[]
	}

	private Mono<Person> getPerson() {
		return Mono.just(new Person("foo"));
	}

	record Person(String name) { }
}
