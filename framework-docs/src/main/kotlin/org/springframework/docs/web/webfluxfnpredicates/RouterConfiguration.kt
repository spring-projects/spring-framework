/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webfluxfnpredicates

import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.coRouter

class RouterConfiguration {

	fun route(): RouterFunction<ServerResponse> {
		// tag::snippet[]
		val route = coRouter {
			GET("/hello-world", accept(MediaType.TEXT_PLAIN)) {
				ServerResponse.ok().bodyValueAndAwait("Hello World")
			}
		}
		// end::snippet[]
		return route
	}
}
