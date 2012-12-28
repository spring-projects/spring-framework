/*
 * Copyright 2002-2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.orm.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.OptimisticLockException;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 *
 * @author Costin Leau
 *
 */
public class DefaultJpaDialectTests extends TestCase {
	JpaDialect dialect;

	@Override
	protected void setUp() throws Exception {
		dialect = new DefaultJpaDialect();
	}

	@Override
	protected void tearDown() throws Exception {
		dialect = null;
	}

	public void testDefaultTransactionDefinition() throws Exception {
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		try {
			dialect.beginTransaction(null, definition);
			fail("expected exception");
		}
		catch (TransactionException e) {
			// ok
		}
	}

	public void testDefaultBeginTransaction() throws Exception {
		TransactionDefinition definition = new DefaultTransactionDefinition();
		MockControl entityControl = MockControl.createControl(EntityManager.class);
		EntityManager entityManager = (EntityManager) entityControl.getMock();

		MockControl txControl = MockControl.createControl(EntityTransaction.class);
		EntityTransaction entityTx = (EntityTransaction) txControl.getMock();

		entityControl.expectAndReturn(entityManager.getTransaction(), entityTx);
		entityTx.begin();

		entityControl.replay();
		txControl.replay();

		dialect.beginTransaction(entityManager, definition);

		entityControl.verify();
		txControl.verify();
	}

	public void testTranslateException() {
		OptimisticLockException ex = new OptimisticLockException();
		assertEquals(
				EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ex).getCause(),
				dialect.translateExceptionIfPossible(ex).getCause());
	}
}
