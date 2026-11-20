/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.orm.hibernate.repository;

import org.springframework.test.context.orm.hibernate.domain.Person;

/**
 * Person Repository API.
 *
 * @author Sam Brannen
 * @since 3.0
 */
public interface PersonRepository {

	Person findByName(String name);

	Person save(Person person);

}
