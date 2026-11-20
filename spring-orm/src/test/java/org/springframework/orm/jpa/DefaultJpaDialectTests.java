/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
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
 * @author Juergen Hoeller
 */
class DefaultJpaDialectTests {

	private final JpaDialect dialect = new DefaultJpaDialect();


	@Test
	void defaultBeginTransaction() throws Exception {
		TransactionDefinition definition = new DefaultTransactionDefinition();
		EntityManager entityManager = mock();
		EntityTransaction entityTx = mock();
		given(entityManager.getTransaction()).willReturn(entityTx);

		dialect.beginTransaction(entityManager, definition);
	}

	@Test
	void customIsolationLevel() {
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		assertThatExceptionOfType(TransactionException.class).isThrownBy(() ->
				dialect.beginTransaction(null, definition));
	}

	@Test
	void translateException() {
		PersistenceException ex = new OptimisticLockException();
		assertThat(dialect.translateExceptionIfPossible(ex))
				.isInstanceOf(JpaOptimisticLockingFailureException.class).hasCause(ex);
	}

}
