/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.context.orm.jpa.domain;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA based implementation of the {@link PersonRepository} API.
 *
 * @author Sam Brannen
 * @since 5.3.18
 */
@Transactional
@Repository
public class JpaPersonRepository implements PersonRepository {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public Person findById(Long id) {
		return this.entityManager.find(Person.class, id);
	}

	@Override
	public Person findByName(String name) {
		return this.entityManager.createQuery("from Person where name = :name", Person.class)
				.setParameter("name", name)
				.getSingleResult();
	}

	@Override
	public Person save(Person person) {
		this.entityManager.persist(person);
		return person;
	}

	@Override
	public void remove(Person person) {
		this.entityManager.remove(person);
	}

}
