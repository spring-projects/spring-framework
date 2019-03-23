/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.test.context.transaction.ejb.dao;

import javax.ejb.TransactionAttribute;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

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
