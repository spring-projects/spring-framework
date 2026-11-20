/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
