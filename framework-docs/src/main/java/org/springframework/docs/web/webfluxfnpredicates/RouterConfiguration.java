/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webfluxfnpredicates;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

public class RouterConfiguration {

	public RouterFunction<ServerResponse> route() {
		// tag::snippet[]
		RouterFunction<ServerResponse> route = RouterFunctions.route()
			.GET("/hello-world", accept(MediaType.TEXT_PLAIN),
				request -> ServerResponse.ok().bodyValue("Hello World")).build();
		// end::snippet[]
		return route;
	}
}
