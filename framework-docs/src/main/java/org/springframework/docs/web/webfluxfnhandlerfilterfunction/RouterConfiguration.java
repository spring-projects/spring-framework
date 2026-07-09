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
