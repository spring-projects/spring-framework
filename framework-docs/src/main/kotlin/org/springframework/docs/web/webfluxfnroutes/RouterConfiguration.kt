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

package org.springframework.docs.web.webfluxfnroutes

import kotlinx.coroutines.flow.Flow
import org.springframework.docs.web.webfluxfnhandlerclasses.Person
import org.springframework.docs.web.webfluxfnhandlerclasses.PersonHandler
import org.springframework.docs.web.webfluxfnhandlerclasses.PersonRepository
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.coRouter

class RouterConfiguration {

	fun routes(): RouterFunction<ServerResponse> {
		// tag::snippet[]
		val repository: PersonRepository = getPersonRepository()
		val handler = PersonHandler(repository)

		val otherRoute: RouterFunction<ServerResponse> = getOtherRoute()

		val route = coRouter {
			// GET /person/{id} with an Accept header that matches JSON is routed to PersonHandler.getPerson
			GET("/person/{id}", accept(APPLICATION_JSON), handler::getPerson)
			// GET /person with an Accept header that matches JSON is routed to PersonHandler.listPeople
			GET("/person", accept(APPLICATION_JSON), handler::listPeople)
			// POST /person with no additional predicates is mapped to PersonHandler.createPerson
			POST("/person", handler::createPerson)
		// otherRoute is a router function that is created elsewhere and added to the route built
		}.and(otherRoute)
		// end::snippet[]
		return route
	}
}

fun getOtherRoute() = coRouter {  }

fun getPersonRepository() = object: PersonRepository {
	override fun allPeople(): Flow<Person> {
		TODO("Not yet implemented")
	}

	override suspend fun savePerson(person: Person) {
		TODO("Not yet implemented")
	}

	override suspend fun getPerson(id: Int): Person? {
		TODO("Not yet implemented")
	}
}
