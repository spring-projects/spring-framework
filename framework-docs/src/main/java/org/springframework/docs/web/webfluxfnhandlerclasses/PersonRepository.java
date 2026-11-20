/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webfluxfnhandlerclasses;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PersonRepository {

	Flux<Person> allPeople();

	Mono<Void> savePerson(Mono<Person> person);

	Mono<Person> getPerson(int id);
}
