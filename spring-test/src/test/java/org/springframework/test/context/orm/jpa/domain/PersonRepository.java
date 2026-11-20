/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.orm.jpa.domain;

import java.util.List;

/**
 * Person repository API.
 *
 * @author Sam Brannen
 * @since 5.3.18
 */
public interface PersonRepository {

	List<Person> findAll();

	Person findById(Long id);

	Person findByName(String name);

	Person save(Person person);

	void remove(Person person);

}
