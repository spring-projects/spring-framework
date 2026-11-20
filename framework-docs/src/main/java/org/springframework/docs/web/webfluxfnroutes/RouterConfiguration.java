/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
