/*
 * Copyright 2002-2007 the original author or authors.
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

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import javax.persistence.TransactionRequiredException;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Costin Leau
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class EntityManagerFactoryUtilsTests extends TestCase {

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.EntityManagerFactoryUtils.doGetEntityManager(EntityManagerFactory)'
	 */
	public void testDoGetEntityManager() {
		// test null assertion
		try {
			EntityManagerFactoryUtils.doGetTransactionalEntityManager(null, null);
			fail("expected exception");
		}
		catch (IllegalArgumentException ex) {
			// it's okay
		}
		MockControl mockControl = MockControl.createControl(EntityManagerFactory.class);
		EntityManagerFactory factory = (EntityManagerFactory) mockControl.getMock();

		mockControl.replay();
		// no tx active
		assertNull(EntityManagerFactoryUtils.doGetTransactionalEntityManager(factory, null));
		mockControl.verify();

		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
	}

	public void testDoGetEntityManagerWithTx() throws Exception {
		try {
			MockControl mockControl = MockControl.createControl(EntityManagerFactory.class);
			EntityManagerFactory factory = (EntityManagerFactory) mockControl.getMock();

			MockControl managerControl = MockControl.createControl(EntityManager.class);
			EntityManager manager = (EntityManager) managerControl.getMock();

			TransactionSynchronizationManager.initSynchronization();
			mockControl.expectAndReturn(factory.createEntityManager(), manager);

			mockControl.replay();
			// no tx active
			assertSame(manager, EntityManagerFactoryUtils.doGetTransactionalEntityManager(factory, null));
			assertSame(manager, ((EntityManagerHolder)TransactionSynchronizationManager.unbindResource(factory)).getEntityManager());
			
			mockControl.verify();
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}

		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
	}
	
	public void testTranslatesIllegalStateException() {
		IllegalStateException ise = new IllegalStateException();
		DataAccessException dex = EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ise);
		assertSame(ise, dex.getCause());
		assertTrue(dex instanceof InvalidDataAccessApiUsageException);
	}
	
	public void testTranslatesIllegalArgumentException() {
		IllegalArgumentException iae = new IllegalArgumentException();
		DataAccessException dex = EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(iae);
		assertSame(iae, dex.getCause());
		assertTrue(dex instanceof InvalidDataAccessApiUsageException);
	}
	
	/**
	 * We do not convert unknown exceptions. They may result from user code.
	 */
	public void testDoesNotTranslateUnfamiliarException() {
		UnsupportedOperationException userRuntimeException = new UnsupportedOperationException();
		assertNull(
				"Exception should not be wrapped",
				EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(userRuntimeException));
	}

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.EntityManagerFactoryUtils.convertJpaAccessException(PersistenceException)'
	 */
	public void testConvertJpaPersistenceException() {
		EntityNotFoundException entityNotFound = new EntityNotFoundException();
		assertSame(JpaObjectRetrievalFailureException.class,
				EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(entityNotFound).getClass());

		NoResultException noResult = new NoResultException();
		assertSame(EmptyResultDataAccessException.class,
				EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(noResult).getClass());

		NonUniqueResultException nonUniqueResult = new NonUniqueResultException();
		assertSame(IncorrectResultSizeDataAccessException.class,
				EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(nonUniqueResult).getClass());

		OptimisticLockException optimisticLock = new OptimisticLockException();
		assertSame(JpaOptimisticLockingFailureException.class,
				EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(optimisticLock).getClass());

		EntityExistsException entityExists = new EntityExistsException("foo");
		assertSame(DataIntegrityViolationException.class,
				EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(entityExists).getClass());

		TransactionRequiredException transactionRequired = new TransactionRequiredException("foo");
		assertSame(InvalidDataAccessApiUsageException.class,
				EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(transactionRequired).getClass());

		PersistenceException unknown = new PersistenceException() {
		};
		assertSame(JpaSystemException.class,
				EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(unknown).getClass());
	}

}
