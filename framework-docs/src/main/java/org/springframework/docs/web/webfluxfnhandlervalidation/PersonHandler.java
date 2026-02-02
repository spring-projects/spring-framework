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

package org.springframework.docs.web.webfluxfnhandlervalidation;

import org.springframework.docs.web.webfluxfnhandlerclasses.Person;
import org.springframework.docs.web.webfluxfnhandlerclasses.PersonRepository;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;

// tag::snippet[]
public class PersonHandler {

	// Create Validator instance
	private final Validator validator = new PersonValidator();

	private final PersonRepository repository;

	public PersonHandler(PersonRepository repository) {
		this.repository = repository;
	}

	public Mono<ServerResponse> createPerson(ServerRequest request) {
		// Apply validation
		Mono<Person> person = request.bodyToMono(Person.class).doOnNext(this::validate);
		return ok().build(repository.savePerson(person));
	}

	private void validate(Person person) {
		Errors errors = new BeanPropertyBindingResult(person, "person");
		validator.validate(person, errors);
		if (errors.hasErrors()) {
			// Raise exception for a 400 response
			throw new ServerWebInputException(errors.toString());
		}
	}
}
// end::snippet[]
