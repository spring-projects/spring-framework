/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webfluxfnhandlerfilterfunction

import org.springframework.docs.web.webfluxfnhandlerclasses.PersonHandler
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.coRouter

class RouterConfiguration {

	fun route(handler: PersonHandler): RouterFunction<ServerResponse> {
		// tag::snippet[]
		val securityManager: SecurityManager = getSecurityManager()

		val route = coRouter {
			("/person" and accept(APPLICATION_JSON)).nest {
				GET("/{id}", handler::getPerson)
				GET("/", handler::listPeople)
				POST("/", handler::createPerson)
				filter { request, next ->
					if (securityManager.allowAccessTo(request.path())) {
						next(request)
					}
					else {
						ServerResponse.status(UNAUTHORIZED).buildAndAwait()
					}
				}
			}
		}
		// end::snippet[]
		return route
	}

}

fun getSecurityManager() = object : SecurityManager {
	override fun allowAccessTo(path: String): Boolean {
		TODO("Not yet implemented")
	}
}
