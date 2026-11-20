/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.transaction.ejb.dao;

import jakarta.ejb.TransactionAttribute;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.test.context.transaction.ejb.model.TestEntity;

/**
 * Abstract base class for EJB implementations of {@link TestEntityDao} which
 * declare transaction semantics for {@link #incrementCount(String)} via
 * {@link TransactionAttribute}.
 *
 * @author Sam Brannen
 * @author Xavier Detant
 * @since 4.0.1
 * @see RequiredEjbTxTestEntityDao
 * @see RequiresNewEjbTxTestEntityDao
 */
public abstract class AbstractEjbTxTestEntityDao implements TestEntityDao {

	@PersistenceContext
	protected EntityManager entityManager;


	protected final TestEntity getTestEntity(String name) {
		TestEntity te = entityManager.find(TestEntity.class, name);
		if (te == null) {
			te = new TestEntity(name, 0);
			entityManager.persist(te);
		}
		return te;
	}

	protected final int getCountInternal(String name) {
		return getTestEntity(name).getCount();
	}

	protected final int incrementCountInternal(String name) {
		TestEntity te = getTestEntity(name);
		int count = te.getCount();
		count++;
		te.setCount(count);
		return count;
	}

}
