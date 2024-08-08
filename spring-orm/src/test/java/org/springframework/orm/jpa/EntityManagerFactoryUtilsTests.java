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

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TransactionRequiredException;
import org.junit.jupiter.api.Test;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Costin Leau
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Phillip Webb
 */
class EntityManagerFactoryUtilsTests {

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.EntityManagerFactoryUtils.doGetEntityManager(EntityManagerFactory)'
	 */
	@Test
	void testDoGetEntityManager() {
		// test null assertion
		assertThatIllegalArgumentException().isThrownBy(() ->
				EntityManagerFactoryUtils.doGetTransactionalEntityManager(null, null));
		EntityManagerFactory factory = mock();

		// no tx active
		assertThat(EntityManagerFactoryUtils.doGetTransactionalEntityManager(factory, null)).isNull();
		assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
	}

	@Test
	void testDoGetEntityManagerWithTx() {
		try {
			EntityManagerFactory factory = mock();
			EntityManager manager = mock();

			TransactionSynchronizationManager.initSynchronization();
			given(factory.createEntityManager()).willReturn(manager);

			// no tx active
			assertThat(EntityManagerFactoryUtils.doGetTransactionalEntityManager(factory, null)).isSameAs(manager);
			assertThat(((EntityManagerHolder) TransactionSynchronizationManager.unbindResource(factory)).getEntityManager()).isSameAs(manager);
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}

		assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
	}

	@Test
	void testTranslatesIllegalStateException() {
		IllegalStateException ise = new IllegalStateException();
		DataAccessException dex = EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ise);
		assertThat(dex.getCause()).isSameAs(ise);
		boolean condition = dex instanceof InvalidDataAccessApiUsageException;
		assertThat(condition).isTrue();
	}

	@Test
	void testTranslatesIllegalArgumentException() {
		IllegalArgumentException iae = new IllegalArgumentException();
		DataAccessException dex = EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(iae);
		assertThat(dex.getCause()).isSameAs(iae);
		boolean condition = dex instanceof InvalidDataAccessApiUsageException;
		assertThat(condition).isTrue();
	}

	/**
	 * We do not convert unknown exceptions. They may result from user code.
	 */
	@Test
	void testDoesNotTranslateUnfamiliarException() {
		UnsupportedOperationException userRuntimeException = new UnsupportedOperationException();
		assertThat(EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(userRuntimeException)).as("Exception should not be wrapped").isNull();
	}

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.EntityManagerFactoryUtils.convertJpaAccessException(PersistenceException)'
	 */
	@Test
	@SuppressWarnings("serial")
	public void testConvertJpaPersistenceException() {
		EntityNotFoundException entityNotFound = new EntityNotFoundException();
		assertThat(EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(entityNotFound).getClass()).isSameAs(JpaObjectRetrievalFailureException.class);

		NoResultException noResult = new NoResultException();
		assertThat(EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(noResult).getClass()).isSameAs(EmptyResultDataAccessException.class);

		NonUniqueResultException nonUniqueResult = new NonUniqueResultException();
		assertThat(EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(nonUniqueResult).getClass()).isSameAs(IncorrectResultSizeDataAccessException.class);

		OptimisticLockException optimisticLock = new OptimisticLockException();
		assertThat(EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(optimisticLock).getClass()).isSameAs(JpaOptimisticLockingFailureException.class);

		EntityExistsException entityExists = new EntityExistsException("foo");
		assertThat(EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(entityExists).getClass()).isSameAs(DataIntegrityViolationException.class);

		TransactionRequiredException transactionRequired = new TransactionRequiredException("foo");
		assertThat(EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(transactionRequired).getClass()).isSameAs(InvalidDataAccessApiUsageException.class);

		PersistenceException unknown = new PersistenceException() {
		};
		assertThat(EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(unknown).getClass()).isSameAs(JpaSystemException.class);
	}

}
