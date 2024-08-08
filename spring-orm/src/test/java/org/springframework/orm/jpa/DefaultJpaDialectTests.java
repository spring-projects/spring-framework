/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.orm.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Costin Leau
 * @author Phillip Webb
 */
class DefaultJpaDialectTests {

	private JpaDialect dialect = new DefaultJpaDialect();

	@Test
	void testDefaultTransactionDefinition() {
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		assertThatExceptionOfType(TransactionException.class).isThrownBy(() ->
				dialect.beginTransaction(null, definition));
	}

	@Test
	void testDefaultBeginTransaction() throws Exception {
		TransactionDefinition definition = new DefaultTransactionDefinition();
		EntityManager entityManager = mock();
		EntityTransaction entityTx = mock();

		given(entityManager.getTransaction()).willReturn(entityTx);

		dialect.beginTransaction(entityManager, definition);
	}

	@Test
	void testTranslateException() {
		OptimisticLockException ex = new OptimisticLockException();
		assertThat(dialect.translateExceptionIfPossible(ex).getCause()).isEqualTo(EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ex).getCause());
	}
}
