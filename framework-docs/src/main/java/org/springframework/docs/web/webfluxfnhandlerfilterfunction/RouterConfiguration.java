/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webfluxfnhandlerfilterfunction;

import org.springframework.docs.web.webfluxfnhandlerclasses.PersonHandler;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

public class RouterConfiguration {

	public RouterFunction<ServerResponse> route(PersonHandler handler) {
		// tag::snippet[]
		SecurityManager securityManager = getSecurityManager();

		RouterFunction<ServerResponse> route = RouterFunctions.route()
			.path("/person", b1 -> b1
				.nest(accept(APPLICATION_JSON), b2 -> b2
					.GET("/{id}", handler::getPerson)
					.GET(handler::listPeople))
				.POST(handler::createPerson))
			.filter((request, next) -> {
				if (securityManager.allowAccessTo(request.path())) {
					return next.handle(request);
				}
				else {
					return ServerResponse.status(UNAUTHORIZED).build();
				}
			}).build();
		// end::snippet[]
		return route;
	}

	SecurityManager getSecurityManager() {
		return path -> false;
	}
}
