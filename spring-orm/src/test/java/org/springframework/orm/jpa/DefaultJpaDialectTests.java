/*
 * Copyright 2002-2013 the original author or authors.
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

import org.junit.Test;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Costin Leau
 * @author Phillip Webb
 */
public class DefaultJpaDialectTests {

	private JpaDialect dialect = new DefaultJpaDialect();

	@Test
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

	@Test
	public void testDefaultBeginTransaction() throws Exception {
		TransactionDefinition definition = new DefaultTransactionDefinition();
		EntityManager entityManager = mock(EntityManager.class);
		EntityTransaction entityTx = mock(EntityTransaction.class);

		given(entityManager.getTransaction()).willReturn(entityTx);

		dialect.beginTransaction(entityManager, definition);
	}

	@Test
	public void testTranslateException() {
		OptimisticLockException ex = new OptimisticLockException();
		assertEquals(
				EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ex).getCause(),
				dialect.translateExceptionIfPossible(ex).getCause());
	}
}
