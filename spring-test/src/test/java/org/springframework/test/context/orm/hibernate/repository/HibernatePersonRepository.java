/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.test.context.orm.hibernate.repository;

import org.hibernate.SessionFactory;

import org.springframework.stereotype.Repository;
import org.springframework.test.context.orm.hibernate.domain.Person;

/**
 * Hibernate implementation of the {@link PersonRepository} API.
 *
 * @author Sam Brannen
 * @since 3.0
 */
@Repository
class HibernatePersonRepository implements PersonRepository {

	private final SessionFactory sessionFactory;


	public HibernatePersonRepository(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public Person save(Person person) {
		this.sessionFactory.getCurrentSession().persist(person);
		return person;
	}

	@Override
	public Person findByName(String name) {
		return this.sessionFactory.getCurrentSession()
				.createQuery("from Person person where person.name = :name", Person.class)
				.setParameter("name", name)
				.getSingleResult();
	}

}
