/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
