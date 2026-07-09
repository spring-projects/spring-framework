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

package org.springframework.docs.web.webfluxfnroutes;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.docs.web.webfluxfnhandlerclasses.Person;
import org.springframework.docs.web.webfluxfnhandlerclasses.PersonHandler;
import org.springframework.docs.web.webfluxfnhandlerclasses.PersonRepository;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

public class RouterConfiguration {

	public RouterFunction<ServerResponse> routes() {
		// tag::snippet[]
		PersonRepository repository = getPersonRepository();
		PersonHandler handler = new PersonHandler(repository);

		RouterFunction<ServerResponse> otherRoute = getOtherRoute();

		RouterFunction<ServerResponse> route = route()
			// GET /person/{id} with an Accept header that matches JSON is routed to PersonHandler.getPerson
			.GET("/person/{id}", accept(APPLICATION_JSON), handler::getPerson)
			// GET /person with an Accept header that matches JSON is routed to PersonHandler.listPeople
			.GET("/person", accept(APPLICATION_JSON), handler::listPeople)
			// POST /person with no additional predicates is mapped to PersonHandler.createPerson
			.POST("/person", handler::createPerson)
			// otherRoute is a router function that is created elsewhere and added to the route built
			.add(otherRoute)
			.build();
		// end::snippet[]
		return route;
	}

	PersonRepository getPersonRepository() {
		return new PersonRepository() {
			@Override
			public Flux<Person> allPeople() {
				return null;
			}

			@Override
			public Mono<Void> savePerson(Mono<Person> person) {
				return null;
			}

			@Override
			public Mono<Person> getPerson(int id) {
				return null;
			}
		};
	}

	RouterFunction<ServerResponse> getOtherRoute() {
		return RouterFunctions.route().build();
	}
}
