/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webfluxfnhandlerclasses

import kotlinx.coroutines.flow.Flow

interface PersonRepository {

	fun allPeople(): Flow<Person>

	suspend fun savePerson(person: Person)

	suspend fun getPerson(id: Int): Person?
}
