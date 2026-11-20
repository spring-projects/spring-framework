/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.orm.hibernate.service;

import org.springframework.stereotype.Service;
import org.springframework.test.context.orm.hibernate.domain.Person;
import org.springframework.test.context.orm.hibernate.repository.PersonRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Standard implementation of the {@link PersonService} API.
 *
 * @author Sam Brannen
 * @since 3.0
 */
@Service
@Transactional(readOnly = true)
class StandardPersonService implements PersonService {

	private final PersonRepository personRepository;


	public StandardPersonService(PersonRepository personRepository) {
		this.personRepository = personRepository;
	}

	@Override
	public Person findByName(String name) {
		return this.personRepository.findByName(name);
	}

	@Override
	@Transactional(readOnly = false)
	public Person save(Person person) {
		return this.personRepository.save(person);
	}

}
