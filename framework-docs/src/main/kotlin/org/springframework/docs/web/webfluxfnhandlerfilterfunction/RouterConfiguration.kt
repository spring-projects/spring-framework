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
