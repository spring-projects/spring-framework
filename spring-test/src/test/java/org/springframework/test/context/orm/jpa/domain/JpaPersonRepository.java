/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.orm.jpa.domain;

import java.util.List;

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
	public List<Person> findAll() {
		return this.entityManager.createQuery("from Person", Person.class).getResultList();
	}

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
