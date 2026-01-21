/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.docs.web.webfluxfnhandlerclasses;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

// tag::snippet[]
public class PersonHandler {

	private final PersonRepository repository;

	public PersonHandler(PersonRepository repository) {
		this.repository = repository;
	}

	// listPeople is a handler function that returns all Person objects found
	// in the repository as JSON
	public Mono<ServerResponse> listPeople(ServerRequest request) {
		Flux<Person> people = repository.allPeople();
		return ok().contentType(APPLICATION_JSON).body(people, Person.class);
	}

	// createPerson is a handler function that stores a new Person contained
	// in the request body.
	// Note that PersonRepository.savePerson(Person) returns Mono<Void>: an empty
	// Mono that emits a completion signal when the person has been read from the
	// request and stored. So we use the build(Publisher<Void>) method to send a
	// response when that completion signal is received (that is, when the Person
	// has been saved)
	public Mono<ServerResponse> createPerson(ServerRequest request) {
		Mono<Person> person = request.bodyToMono(Person.class);
		return ok().build(repository.savePerson(person));
	}

	// getPerson is a handler function that returns a single person, identified by
	// the id path variable. We retrieve that Person from the repository and create
	// a JSON response, if it is found. If it is not found, we use switchIfEmpty(Mono<T>)
	// to return a 404 Not Found response.
	public Mono<ServerResponse> getPerson(ServerRequest request) {
		int personId = Integer.valueOf(request.pathVariable("id"));
		return repository.getPerson(personId)
			.flatMap(person -> ok().contentType(APPLICATION_JSON).bodyValue(person))
			.switchIfEmpty(ServerResponse.notFound().build());
	}
}
// end::snippet[]

