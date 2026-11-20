/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.orm.jpa;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.orm.jpa.domain.Person;
import org.springframework.test.context.orm.jpa.domain.PersonRepository;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Transactional tests for JPA support with {@link Nested @Nested} test classes.
 *
 * @author Sam Brannen
 * @since 6.2.13
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/34576">issue gh-34576</a>
 */
@SpringJUnitConfig(JpaConfig.class)
@Transactional
@Sql(statements = "insert into person(id, name) values(0, 'Jane')")
class JpaPersonRepositoryTests {

	@Autowired
	EntityManager em;

	@Autowired
	PersonRepository repo;


	@BeforeEach
	void setup() {
		em.persist(new Person("John"));
		em.flush();
	}

	@Test
	void findAll() {
		assertThat(repo.findAll()).map(Person::getName).containsExactlyInAnyOrder("Jane", "John");
	}


	@Nested
	// Declare a random test property to ensure we get a different ApplicationContext.
	@TestPropertySource(properties = "nested = true")
	class NestedTests {

		@Test
		void findAll() {
			assertThat(repo.findAll()).map(Person::getName).containsExactlyInAnyOrder("Jane", "John");
		}
	}

}
