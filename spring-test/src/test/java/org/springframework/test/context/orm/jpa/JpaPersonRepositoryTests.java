/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.orm.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

	@PersistenceContext
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
